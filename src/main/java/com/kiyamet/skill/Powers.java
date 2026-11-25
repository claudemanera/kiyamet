package com.kiyamet.skill;

import com.kiyamet.Kiyamet;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Super powers: every useful /effect becomes a permanent power unlocked by skill levels. Each can be switched on/off
 * in the Powers panel (/skills -> Powers). Kıyamet 1.2 will add stronger tiers on top of these.
 */
public final class Powers {
    public enum Power {
        NIGHT_VISION("Night Vision", MobEffects.NIGHT_VISION, 0, Skill.EXPLORATION, 50, Items.GOLDEN_CARROT),
        SPEED_1("Speed I", MobEffects.SPEED, 0, Skill.ATHLETICS, 50, Items.SUGAR),
        SPEED_2("Speed II", MobEffects.SPEED, 1, Skill.ATHLETICS, 250, Items.SUGAR),
        SPEED_3("Speed III", MobEffects.SPEED, 2, Skill.ATHLETICS, 750, Items.SUGAR),
        JUMP_1("Jump Boost I", MobEffects.JUMP_BOOST, 0, Skill.ACROBATICS, 50, Items.RABBIT_FOOT),
        JUMP_2("Jump Boost II", MobEffects.JUMP_BOOST, 1, Skill.ACROBATICS, 250, Items.RABBIT_FOOT),
        SLOW_FALL("Slow Falling", MobEffects.SLOW_FALLING, 0, Skill.ACROBATICS, 500, Items.PHANTOM_MEMBRANE),
        HASTE_1("Haste I", MobEffects.HASTE, 0, Skill.MINING, 50, Items.GOLDEN_PICKAXE),
        HASTE_2("Haste II", MobEffects.HASTE, 1, Skill.MINING, 250, Items.GOLDEN_PICKAXE),
        HASTE_3("Haste III", MobEffects.HASTE, 2, Skill.MINING, 750, Items.NETHERITE_PICKAXE),
        BUILDER_HASTE("Builder's Haste", MobEffects.HASTE, 0, Skill.BUILDING, 100, Items.BRICKS),
        STRENGTH_1("Strength I", MobEffects.STRENGTH, 0, Skill.SWORDS, 100, Items.BLAZE_POWDER),
        STRENGTH_2("Strength II", MobEffects.STRENGTH, 1, Skill.SWORDS, 500, Items.BLAZE_POWDER),
        STRENGTH_3("Strength III", MobEffects.STRENGTH, 2, Skill.SWORDS, 999, Items.NETHERITE_SWORD),
        HEAVY_STRENGTH("Brute Force", MobEffects.STRENGTH, 0, Skill.HEAVY_WEAPONS, 250, Items.ANVIL),
        RESIST_1("Resistance I", MobEffects.RESISTANCE, 0, Skill.DEFENSE, 100, Items.IRON_CHESTPLATE),
        RESIST_2("Resistance II", MobEffects.RESISTANCE, 1, Skill.DEFENSE, 500, Items.DIAMOND_CHESTPLATE),
        RESIST_3("Resistance III", MobEffects.RESISTANCE, 2, Skill.DEFENSE, 999, Items.NETHERITE_CHESTPLATE),
        ABSORPTION("Absorption", MobEffects.ABSORPTION, 0, Skill.DEFENSE, 250, Items.GOLDEN_APPLE),
        REGEN_1("Regeneration I", MobEffects.REGENERATION, 0, Skill.HEALTH, 100, Items.GHAST_TEAR),
        REGEN_2("Regeneration II", MobEffects.REGENERATION, 1, Skill.HEALTH, 500, Items.GHAST_TEAR),
        MEDIC_REGEN("Field Medic", MobEffects.REGENERATION, 0, Skill.MEDICINE, 100, Items.GLISTERING_MELON_SLICE),
        FIRE_RES("Fire Resistance", MobEffects.FIRE_RESISTANCE, 0, Skill.SURVIVAL, 250, Items.MAGMA_CREAM),
        SATURATION("Saturation", MobEffects.SATURATION, 0, Skill.SURVIVAL, 750, Items.COOKED_BEEF),
        FARMER_SATURATION("Harvest Feast", MobEffects.SATURATION, 0, Skill.FARMING, 500, Items.BREAD),
        WATER_BREATH("Water Breathing", MobEffects.WATER_BREATHING, 0, Skill.SWIMMING, 50, Items.PUFFERFISH),
        DOLPHIN("Dolphin's Grace", MobEffects.DOLPHINS_GRACE, 0, Skill.SWIMMING, 250, Items.HEART_OF_THE_SEA),
        CONDUIT("Conduit Power", MobEffects.CONDUIT_POWER, 0, Skill.SWIMMING, 500, Items.CONDUIT),
        INVISIBLE("Shadow Step (invisible while sneaking)", MobEffects.INVISIBILITY, 0, Skill.STEALTH, 250, Items.FERMENTED_SPIDER_EYE),
        LUCK_1("Luck I", MobEffects.LUCK, 0, Skill.HUNTING, 100, Items.RABBIT_HIDE),
        LUCK_2("Luck II", MobEffects.LUCK, 1, Skill.HUNTING, 500, Items.EMERALD),
        TREASURE_LUCK("Treasure Sense", MobEffects.LUCK, 0, Skill.EXPLORATION, 250, Items.MAP),
        HERO("Hero of the Village", MobEffects.HERO_OF_THE_VILLAGE, 0, Skill.FARMING, 250, Items.EMERALD_BLOCK),
        ENCHANT_LUCK("Arcane Luck", MobEffects.LUCK, 0, Skill.ENCHANTING, 250, Items.LAPIS_LAZULI),
        ALCHEMY_RESIST("Iron Stomach", MobEffects.RESISTANCE, 0, Skill.ALCHEMY, 250, Items.BREWING_STAND),
        ENGINEER_HASTE("Tinkerer", MobEffects.HASTE, 0, Skill.ENGINEERING, 250, Items.REDSTONE),
        WOOD_HASTE("Lumberjack", MobEffects.HASTE, 0, Skill.WOODCUTTING, 100, Items.GOLDEN_AXE),
        ARCHER_SPEED("Ranger's Stride", MobEffects.SPEED, 0, Skill.ARCHERY, 250, Items.BOW),
        EMERGENCY_HEAL("Second Wind (instant heal at low HP, 2 min cooldown)", MobEffects.INSTANT_HEALTH, 1, Skill.MEDICINE, 250, Items.TOTEM_OF_UNDYING);

        public final String title;
        public final Holder<MobEffect> effect;
        public final int amp, level;
        public final Skill skill;
        public final Item icon;

        Power(String title, Holder<MobEffect> effect, int amp, Skill skill, int level, Item icon) {
            this.title = title; this.effect = effect; this.amp = amp; this.skill = skill; this.level = level; this.icon = icon;
        }

        public boolean unlocked(SkillData d) { return d.level(skill) >= level; }
    }

    public static final AttachmentType<List<String>> DISABLED = AttachmentRegistry.create(Kiyamet.id("powers_off"),
            b -> b.initializer(List::of).persistent(Codec.STRING.listOf()).copyOnDeath());
    public static final AttachmentType<Long> HEAL_COOLDOWN = AttachmentRegistry.create(Kiyamet.id("second_wind"),
            b -> b.initializer(() -> 0L).persistent(Codec.LONG));

    public static boolean enabled(ServerPlayer p, Power pw) {
        return !p.getAttachedOrElse(DISABLED, List.of()).contains(pw.name());
    }

    public static void toggle(ServerPlayer p, Power pw) {
        List<String> l = new ArrayList<>(p.getAttachedOrElse(DISABLED, List.of()));
        if (!l.remove(pw.name())) {
            l.add(pw.name());
            MobEffectInstance cur = p.getEffect(pw.effect);
            if (cur != null && cur.isAmbient()) p.removeEffect(pw.effect);
        }
        p.setAttached(DISABLED, List.copyOf(l));
    }

    private static void apply(ServerPlayer p) {
        SkillData d = SkillManager.get(p);
        Map<Holder<MobEffect>, Integer> best = new HashMap<>();
        for (Power pw : Power.values()) {
            if (!pw.unlocked(d) || !enabled(p, pw)) continue;
            if (pw == Power.EMERGENCY_HEAL) continue;
            if (pw == Power.INVISIBLE && !p.isShiftKeyDown()) continue;
            if ((pw == Power.SATURATION || pw == Power.FARMER_SATURATION) && p.getFoodData().getFoodLevel() >= 18) continue;
            best.merge(pw.effect, pw.amp, Math::max);
        }
        best.forEach((eff, amp) -> {
            MobEffectInstance cur = p.getEffect(eff);
            if (cur != null && !cur.isAmbient() && cur.getAmplifier() >= amp) return;           // a stronger potion is active
            if (eff == MobEffects.ABSORPTION && cur != null) return;                            // only refills once it ran out
            int dur = eff == MobEffects.ABSORPTION ? 2400 : eff == MobEffects.SATURATION ? 1 : eff == MobEffects.INVISIBILITY ? 30 : 300;
            p.addEffect(new MobEffectInstance(eff, dur, amp, true, false, true));
        });
        if (Power.EMERGENCY_HEAL.unlocked(d) && enabled(p, Power.EMERGENCY_HEAL) && p.getHealth() < p.getMaxHealth() * 0.3f) {
            long now = p.level().getGameTime();
            if (now >= p.getAttachedOrElse(HEAL_COOLDOWN, 0L)) {
                p.setAttached(HEAL_COOLDOWN, now + 2400);
                p.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                p.sendSystemMessage(Component.literal("Second Wind!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            }
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTickCount();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isSpectator()) continue;
                if (t % 20 == p.getId() % 20) apply(p);
            }
        });
    }

    private Powers() {}
}
