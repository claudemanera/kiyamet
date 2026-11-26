package com.kiyamet.element;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * An ancient elemental weapon. Left click: attack (every hit carries the element). Right click: switch the element's
 * power on, right click again: off. No cooldown. The power only works while the weapon is in your hand.
 */
public class ElementWeapon extends Item {
    public final Element element;

    public ElementWeapon(Properties p, Element e) { super(p); this.element = e; }

    public static boolean active(ItemStack s) {
        CustomModelData d = s.get(DataComponents.CUSTOM_MODEL_DATA);
        return d != null && !d.flags().isEmpty() && d.flags().get(0);
    }

    public static void setActive(ItemStack s, boolean on) {
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(on), List.of(), List.of()));
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, on);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            boolean on = !active(s);
            setActive(s, on);
            if (on) ElementPowers.start(sp, element); else ElementPowers.stop(sp, element);
            sp.displayClientMessage(Component.literal(element.title.toUpperCase() + (on ? " POWER: ON" : " POWER: OFF")).withStyle(element.fmt, ChatFormatting.BOLD), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack s, ServerLevel level, Entity e, EquipmentSlot slot) {
        if (!(e instanceof ServerPlayer p) || !active(s)) return;
        boolean held = p.getMainHandItem() == s || p.getOffhandItem() == s;
        if (!held) { setActive(s, false); ElementPowers.stop(p, element); return; }
        ElementPowers.tick(p, element, s);
    }

    @Override
    public void hurtEnemy(ItemStack s, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(s, target, attacker);
        ElementPowers.onHit(element, target, attacker);
    }
}
