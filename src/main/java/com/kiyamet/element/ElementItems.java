package com.kiyamet.element;

import com.kiyamet.registry.ModItems;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.AnvilBlock;

/** Weapons, element cores, the ancient rod; and forging: core + rod on an anvil. */
public final class ElementItems {
    public static final Map<Element, Item> WEAPONS = new EnumMap<>(Element.class), CORES = new EnumMap<>(Element.class);
    public static Item ROD, SAGE_MAP;

    public static void init() {
        for (Element e : Element.values()) {
            WEAPONS.put(e, ModItems.custom(e.weapon, p -> new ElementWeapon(p.sword(ToolMaterial.NETHERITE, e.damage - 5f, e.speed).fireResistant().rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE), e)));
            CORES.put(e, ModItems.custom(e.core(), p -> new Item(p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))));
        }
        ROD = ModItems.custom("ancient_rod", p -> new Item(p.sword(ToolMaterial.IRON, 4f, -2.4f).fireResistant().rarity(Rarity.RARE).component(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE)));
        SAGE_MAP = ModItems.custom("sage_map", p -> new SageMapItem(p.stacksTo(1).rarity(Rarity.UNCOMMON)));

        // walkers felled with a sage's rod count for that sage's quest
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((dead, src) -> {
            if (!(src.getEntity() instanceof ServerPlayer p) || !(dead instanceof net.minecraft.world.entity.monster.Enemy)) return;
            Element e = Sage.rodElement(p.getMainHandItem());
            if (e != null) Sage.addKill(p, e);
        });
        // forging: hold the element core, have an ancient rod in your inventory, right-click an anvil
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer p) || !(level instanceof ServerLevel l)) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            if (!(l.getBlockState(pos).getBlock() instanceof AnvilBlock)) return InteractionResult.PASS;
            ItemStack held = p.getMainHandItem();
            Element e = null;
            for (var en : CORES.entrySet()) if (held.is(en.getValue())) e = en.getKey();
            if (e == null) return InteractionResult.PASS;
            int rod = -1;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(ROD)) { rod = i; break; }
            if (rod < 0) { p.displayClientMessage(Component.literal("You need an Ancient Rod to forge the " + e.weaponTitle + ".").withStyle(ChatFormatting.RED), true); return InteractionResult.FAIL; }
            p.getInventory().getItem(rod).shrink(1);
            held.shrink(1);
            ItemStack w = new ItemStack(WEAPONS.get(e));
            if (!p.getInventory().add(w)) p.drop(w, false);
            for (int i = 0; i < 5; i++) l.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 0.6f + i * 0.15f);
            l.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6f, 1.6f);
            l.sendParticles(new DustParticleOptions(e.color, 2f), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 80, 0.6, 0.6, 0.6, 0.1);
            l.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.3, 1, 0.3, 0.1);
            l.getServer().getPlayerList().broadcastSystemMessage(Component.literal("⚔ " + p.getName().getString() + " forged the " + e.weaponTitle + "!").withStyle(e.fmt, ChatFormatting.BOLD), false);
            return InteractionResult.SUCCESS;
        });
    }

    private ElementItems() {}
}
