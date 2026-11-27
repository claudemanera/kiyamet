package com.kiyamet.creation;

import com.kiyamet.element.Element;
import com.kiyamet.element.ElementPowers;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/**
 * TWD 2.2: the Creation Axe, a double-bladed battle axe that holds all fourteen element powers.
 * Every power has its own key (Options > Controls > The Walking Dead), right-click switches the selected power,
 * sneak + right-click picks the next power. Powers that fight over the same thing (time, flight) replace each other.
 */
public class CreationAxe extends Item {
    public CreationAxe(Properties p) { super(p); }

    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static int mask(ItemStack s) { return tag(s).getIntOr("On", 0); }
    public static int selected(ItemStack s) { return Math.floorMod(tag(s).getIntOr("Sel", 0), Element.values().length); }

    static void write(ItemStack s, int mask, int sel) {
        CompoundTag t = tag(s);
        t.putInt("On", mask);
        t.putInt("Sel", sel);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(mask != 0), List.of(), List.of()));
    }

    public static boolean isOn(ItemStack s, Element e) { return (mask(s) & (1 << e.ordinal())) != 0; }

    /** switch one power of the axe in this player's main hand */
    public static void toggle(ServerPlayer p, ItemStack s, Element e) {
        int m = mask(s);
        boolean on = (m & (1 << e.ordinal())) == 0;
        if (on) {
            for (Element o : Element.values())                                          // the clock and the flight have one owner
                if (o != e && (m & (1 << o.ordinal())) != 0 && clash(o, e)) { m &= ~(1 << o.ordinal()); ElementPowers.stop(p, o); }
            m |= 1 << e.ordinal();
            ElementPowers.start(p, e);
        } else {
            m &= ~(1 << e.ordinal());
            ElementPowers.stop(p, e);
        }
        write(s, m, e.ordinal());
        p.displayClientMessage(Component.literal("✦ CREATION · ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                .append(Component.literal(e.title.toUpperCase() + (on ? " ON" : " OFF")).withStyle(e.fmt, ChatFormatting.BOLD))
                .append(Component.literal("   (" + Integer.bitCount(m) + " active)").withStyle(ChatFormatting.GRAY)), true);
        p.level().playSound(null, p.blockPosition(), on ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1f, on ? 1.4f : 0.8f);
    }

    static boolean clash(Element a, Element b) {
        boolean ca = a == Element.ICE || a == Element.THUNDER || a == Element.WIND, cb = b == Element.ICE || b == Element.THUNDER || b == Element.WIND;
        boolean fa = a == Element.SPIRIT || a == Element.AIR, fb = b == Element.SPIRIT || b == Element.AIR;
        return ca && cb || fa && fb;
    }

    public static void allOff(ServerPlayer p, ItemStack s) {
        int m = mask(s);
        for (Element e : Element.values()) if ((m & (1 << e.ordinal())) != 0) ElementPowers.stop(p, e);
        write(s, 0, selected(s));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) {
                int sel = (selected(s) + 1) % Element.values().length;
                write(s, mask(s), sel);
                Element e = Element.values()[sel];
                sp.displayClientMessage(Component.literal("✦ Selected power: ").withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(e.title + " - " + e.power).withStyle(e.fmt)), true);
                sp.level().playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.6f);
            } else toggle(sp, s, Element.values()[selected(s)]);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack s, ServerLevel level, Entity ent, EquipmentSlot slot) {
        if (!(ent instanceof ServerPlayer p)) return;
        boolean held = p.getMainHandItem() == s || p.getOffhandItem() == s;
        int m = mask(s);
        if (!held) { if (m != 0) allOff(p, s); return; }
        // the axe shines white: a slow ring of light around you, brighter when powers are on
        int t = p.tickCount;
        double a = t * 0.18;
        for (int k = 0; k < 2; k++) {
            double ang = a + k * Math.PI;
            level.sendParticles(ParticleTypes.END_ROD, p.getX() + Math.cos(ang) * 0.9, p.getY() + 0.4 + (t % 40) / 40.0 * 1.6, p.getZ() + Math.sin(ang) * 0.9, 1, 0, 0, 0, 0);
        }
        if (t % 3 == 0) level.sendParticles(new DustParticleOptions(0xFFFFFF, 1.4f), p.getX(), p.getY() + 1.1, p.getZ(), 1, 0.4, 0.5, 0.4, 0);
        if (m == 0) return;
        if (t % 10 == 0) level.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 1, p.getZ(), 6, 0.6, 0.8, 0.6, 0.02);
        for (Element e : Element.values()) if ((m & (1 << e.ordinal())) != 0) ElementPowers.tick(p, e, s);
    }

    @Override
    public void hurtEnemy(ItemStack s, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(s, target, attacker);
        int m = mask(s);
        if (target.level() instanceof ServerLevel l) {
            l.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            l.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.5, 0.3, 0.15);
            target.hurtServer(l, l.damageSources().magic(), 6f);                          // creation itself cuts
        }
        if (m == 0) { ElementPowers.onHit(Element.values()[attacker.getRandom().nextInt(14)], target, attacker); return; }
        for (Element e : Element.values()) if ((m & (1 << e.ordinal())) != 0) ElementPowers.onHit(e, target, attacker);
    }
}
