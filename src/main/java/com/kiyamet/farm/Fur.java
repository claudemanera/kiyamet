package com.kiyamet.farm;

import com.kiyamet.Kiyamet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

/**
 * TWD 1.6 fur pelts: worn OVER armor. Right-click a pelt to line the next piece of armor you wear; the armor keeps
 * its look and gets a fur layer on top (an extra equipment-asset layer), and keeps you warm in the cold.
 * Sneak + right-click with shears takes the fur off again.
 */
public final class Fur {
    public enum Kind {
        WOLF("wolf_pelt", "Wolf Fur", 4f), FOX("fox_pelt", "Fox Fur", 4.5f), POLAR("polar_pelt", "Polar Bear Fur", 6f), SHEEP("sheepskin", "Sheepskin", 3.5f);
        public final String item, title; public final float warmth;
        Kind(String item, String title, float warmth) { this.item = item; this.title = title; this.warmth = warmth; }
        public String id() { return name().toLowerCase(); }
        static Kind byName(String n) { for (Kind k : values()) if (k.name().equals(n)) return k; return null; }
    }

    /** Vanilla armor looks we can put fur on (the equipment/fur_*_<base>.json files exist for these). */
    public static final String[] BASES = {"leather", "chainmail", "iron", "gold", "diamond", "netherite", "copper", "turtle_scute", "armadillo_scute"};

    public static class PeltItem extends Item {
        final Kind kind;
        public PeltItem(Properties p, Kind kind) { super(p); this.kind = kind; }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack armor = p.getItemBySlot(slot);
                if (armor.isEmpty() || kind(armor) != null) continue;
                String r = apply(armor, kind);
                if (r != null) { p.displayClientMessage(Component.literal(r).withStyle(ChatFormatting.RED), true); continue; }
                if (!p.isCreative()) player.getItemInHand(hand).shrink(1);
                level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 0.8f);
                p.displayClientMessage(Component.literal(armor.getHoverName().getString() + " lined with " + kind.title + " (+" + fmt(kind.warmth) + "°C in the cold)").withStyle(ChatFormatting.AQUA), true);
                return InteractionResult.CONSUME;
            }
            p.displayClientMessage(Component.literal("Wear armor first - the fur goes on top of it (helmet, chestplate, leggings or boots).").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
    }

    static String fmt(float f) { return f == (int) f ? String.valueOf((int) f) : String.valueOf(f); }

    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    public static Kind kind(ItemStack s) { return Kind.byName(tag(s).getStringOr("Fur", "")); }

    /** Lines the armor with fur. Returns an error message, or null when it worked. */
    static String apply(ItemStack armor, Kind k) {
        Equippable eq = armor.get(DataComponents.EQUIPPABLE);
        if (eq == null || eq.assetId().isEmpty()) return "This can't take fur.";
        ResourceKey<EquipmentAsset> base = eq.assetId().get();
        String path = base.identifier().getPath();
        boolean known = base.identifier().getNamespace().equals("minecraft") && java.util.Arrays.asList(BASES).contains(path);
        if (!known) return "Fur doesn't fit on " + armor.getHoverName().getString() + ".";
        CompoundTag t = tag(armor);
        t.putString("Fur", k.name());
        t.putString("FurBase", base.identifier().toString());
        armor.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        armor.set(DataComponents.EQUIPPABLE, with(eq, ResourceKey.create(EquipmentAssets.ROOT_ID, Kiyamet.id("fur_" + k.id() + "_" + path))));
        List<Component> lore = new ArrayList<>(armor.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        lore.add(Component.literal("❄ Lined with " + k.title + " (+" + fmt(k.warmth) + "°C in the cold)").withStyle(st -> st.withItalic(false).withColor(ChatFormatting.AQUA)));
        armor.set(DataComponents.LORE, new ItemLore(lore));
        return null;
    }

    /** Takes the fur off and gives the pelt back. */
    static boolean strip(ItemStack armor, Player p) {
        Kind k = kind(armor);
        if (k == null) return false;
        CompoundTag t = tag(armor);
        String base = t.getStringOr("FurBase", "minecraft:iron");
        t.remove("Fur"); t.remove("FurBase");
        if (t.isEmpty()) armor.remove(DataComponents.CUSTOM_DATA); else armor.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        Equippable eq = armor.get(DataComponents.EQUIPPABLE);
        if (eq != null) armor.set(DataComponents.EQUIPPABLE, with(eq, ResourceKey.create(EquipmentAssets.ROOT_ID, net.minecraft.resources.Identifier.parse(base))));
        List<Component> lore = new ArrayList<>();
        for (Component c : armor.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines()) if (!c.getString().startsWith("❄ Lined with")) lore.add(c);
        if (lore.isEmpty()) armor.remove(DataComponents.LORE); else armor.set(DataComponents.LORE, new ItemLore(lore));
        ItemStack pelt = new ItemStack(com.kiyamet.registry.ModItems.get(k.item));
        if (!p.getInventory().add(pelt)) p.drop(pelt, false);
        return true;
    }

    static Equippable with(Equippable e, ResourceKey<EquipmentAsset> asset) {
        return new Equippable(e.slot(), e.equipSound(), Optional.of(asset), e.cameraOverlay(), e.allowedEntities(), e.dispensable(), e.swappable(),
                e.damageOnHurt(), e.equipOnInteract(), e.canBeSheared(), e.shearingSound());
    }

    /** Extra warmth from fur-lined armor (used by Seasons). */
    public static float warmth(LivingEntity p) {
        float w = 0;
        for (EquipmentSlot es : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            Kind k = kind(p.getItemBySlot(es));
            if (k != null) w += k.warmth * (es == EquipmentSlot.CHEST ? 1.25f : 1f);
        }
        return w;
    }

    public static int pieces(LivingEntity p) {
        int n = 0;
        for (EquipmentSlot es : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) if (kind(p.getItemBySlot(es)) != null) n++;
        return n;
    }

    public static void init() {
        // shears: sneak + right-click takes all fur off your armor
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack s = player.getItemInHand(hand);
            if (level.isClientSide() || !s.is(Items.SHEARS) || !player.isShiftKeyDown()) return InteractionResult.PASS;
            boolean any = false;
            for (EquipmentSlot es : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) any |= strip(player.getItemBySlot(es), player);
            if (!any) return InteractionResult.PASS;
            level.playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1f, 1f);
            ((ServerPlayer) player).displayClientMessage(Component.literal("You took the fur off your armor.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        });
        // pelts from animals
        ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> {
            if (!(e.level() instanceof ServerLevel level) || !(src.getEntity() instanceof Player)) return;
            EntityType<?> t = e.getType();
            Kind k = t == EntityType.WOLF ? Kind.WOLF : t == EntityType.FOX ? Kind.FOX : t == EntityType.POLAR_BEAR ? Kind.POLAR : t == EntityType.SHEEP ? Kind.SHEEP : null;
            if (k == null) return;
            float chance = k == Kind.SHEEP ? 0.35f : 0.75f;
            if (level.random.nextFloat() < chance) e.spawnAtLocation(level, new ItemStack(com.kiyamet.registry.ModItems.get(k.item), k == Kind.POLAR ? 2 : 1));
        });
    }

    private Fur() {}
}
