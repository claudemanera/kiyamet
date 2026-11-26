package com.kiyamet.water;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Canteens (1.9): right-click water to fill (river water is dirty - boil it by right-clicking a lit campfire or furnace),
 * hold right-click to drink a sip. In winter you can fill them from snow; in the cold they can freeze - warm them by a fire.
 */
public class CanteenItem extends Item {
    public final int sips;
    public CanteenItem(Properties p, int sips) { super(p); this.sips = sips; }

    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static int water(ItemStack s) { return tag(s).getIntOr("Water", 0); }
    public static boolean dirty(ItemStack s) { return tag(s).getBooleanOr("Dirty", false); }
    public static boolean frozen(ItemStack s) { return tag(s).getBooleanOr("Frozen", false); }

    public static void set(ItemStack s, int water, boolean dirty, boolean frozen) {
        CompoundTag t = tag(s);
        t.putInt("Water", water); t.putBoolean("Dirty", dirty && water > 0); t.putBoolean("Frozen", frozen && water > 0);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        int max = s.getItem() instanceof CanteenItem c ? c.sips : 0;
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Water: " + water + " / " + max + " sips" + (water == 0 ? "" : frozen ? " · FROZEN" : dirty ? " · dirty" : " · clean"))
                        .withStyle(st -> st.withItalic(false).withColor(water == 0 ? ChatFormatting.GRAY : frozen ? ChatFormatting.WHITE : dirty ? ChatFormatting.DARK_GREEN : ChatFormatting.AQUA)),
                Component.literal("Right-click water/snow: fill · campfire: boil · hold: drink").withStyle(st -> st.withItalic(false).withColor(ChatFormatting.DARK_GRAY)))));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            var st = level.getBlockState(pos);
            boolean water = level.getFluidState(pos).is(FluidTags.WATER);
            boolean snow = st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.SNOW) || st.is(Blocks.POWDER_SNOW) || st.is(Blocks.ICE);
            boolean fire = st.is(Blocks.CAMPFIRE) && st.getValue(net.minecraft.world.level.block.CampfireBlock.LIT) || st.is(Blocks.FURNACE) && st.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT);
            if (!level.isClientSide()) {
                if (fire && water(s) > 0) {
                    set(s, water(s), false, false);
                    level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1f, 1f);
                    player.displayClientMessage(Component.literal("Boiled: the water is clean.").withStyle(ChatFormatting.AQUA), true);
                    return InteractionResult.CONSUME;
                }
                if ((water || snow) && water(s) < sips) {
                    boolean dirtyNow = water && !(level.isRaining() && level.canSeeSky(player.blockPosition()));
                    set(s, sips, water(s) > 0 ? dirty(s) || dirtyNow : dirtyNow, false);
                    level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1f, snow ? 0.7f : 1f);
                    player.displayClientMessage(Component.literal(snow ? "Packed with snow - it melts into clean water." : dirtyNow ? "Filled - river water, boil it first!" : "Filled.").withStyle(ChatFormatting.AQUA), true);
                    return InteractionResult.CONSUME;
                }
            } else if (fire || water || snow) return InteractionResult.SUCCESS;
        }
        if (water(s) <= 0) return InteractionResult.FAIL;
        if (frozen(s)) { if (!level.isClientSide()) player.displayClientMessage(Component.literal("Frozen solid. Warm it near a fire.").withStyle(ChatFormatting.WHITE), true); return InteractionResult.FAIL; }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override public int getUseDuration(ItemStack s, LivingEntity e) { return 24; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack s) { return ItemUseAnimation.DRINK; }

    @Override
    public ItemStack finishUsingItem(ItemStack s, Level level, LivingEntity e) {
        if (e instanceof ServerPlayer p && water(s) > 0) {
            Thirst.add(p, 4);
            if (dirty(s) && p.getRandom().nextFloat() < 0.35f) { p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)); p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0)); }
            set(s, water(s) - 1, dirty(s), false);
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.8f, 1f);
        }
        return s;
    }

    public static final String[][] TIERS = {{"waterskin", "Leather Waterskin", "4"}, {"tin_canteen", "Tin Canteen", "6"}, {"army_canteen", "Army Canteen", "9"},
            {"steel_thermos", "Steel Thermos", "13"}, {"hydration_pack", "Tactical Hydration Pack", "20"}};

    /** seasons: rain fills canteens outside, frost freezes them, fire thaws them */
    public static void seasonTick(ServerPlayer p) {
        boolean rainOut = p.level().isRaining() && p.level().canSeeSky(p.blockPosition());
        int bodyState = com.kiyamet.season.Seasons.state(p.getAttachedOrElse(com.kiyamet.season.Seasons.BODY, 20f));
        boolean warm = com.kiyamet.season.Seasons.warmNear(p.level(), p.blockPosition(), 4);
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!(s.getItem() instanceof CanteenItem c)) continue;
            if (rainOut && i < 9 && water(s) < c.sips && p.getRandom().nextInt(3) == 0) set(s, water(s) + 1, dirty(s) && water(s) > 0, frozen(s));
            if (water(s) > 0 && !frozen(s) && bodyState <= -3 && !warm && p.getRandom().nextInt(4) == 0) { set(s, water(s), dirty(s), true); p.displayClientMessage(Component.literal("Your water froze!").withStyle(ChatFormatting.WHITE), true); }
            if (frozen(s) && (warm || bodyState >= 0)) set(s, water(s), dirty(s), false);
        }
    }
}
