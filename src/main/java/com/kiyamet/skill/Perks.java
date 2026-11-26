package com.kiyamet.skill;

import com.kiyamet.Kiyamet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill roads: every skill raises several attributes in small steps all the way to level 999
 * (e.g. Athletics: +0.05% move speed every 3 levels = +17% at 999). Each line has its own step size,
 * so you gain something almost every level. Bonuses are refreshed every 5 seconds.
 */
public final class Perks {
    /** One road line: grows by max/steps every {@code every} levels, reaching {@code max} at 999. */
    public record Perk(Skill skill, Holder<Attribute> attribute, double max, int every, AttributeModifier.Operation op, String label, boolean percent) {
        public int totalSteps() { return Skill.MAX_LEVEL / every; }
        public double perStep() { return max / totalSteps(); }
        public double value(int level) { return perStep() * Math.min(totalSteps(), Math.max(0, level) / every); }
        public int nextLevel(int level) { return level >= Skill.MAX_LEVEL ? -1 : Math.min(Skill.MAX_LEVEL, (level / every + 1) * every); }
        public String fmt(double v) {
            if (percent) return (v >= 0 ? "+" : "") + (Math.abs(v * 100) < 1 ? String.format("%.2f%%", v * 100) : String.format("%.1f%%", v * 100));
            return (v >= 0 ? "+" : "") + (Math.abs(v) < 1 ? String.format("%.2f", v) : String.format("%.1f", v));
        }
        public String describe(int level) { return label + " " + fmt(value(level)); }
        public String stepText() { return fmt(perStep()) + " every " + every + " lv"; }
    }

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE, PCT = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    public static final List<Perk> ALL = new ArrayList<>();

    static {
        p(Skill.HEALTH, Attributes.ARMOR_TOUGHNESS, 2, 10, ADD, "Armor toughness", false);
        p(Skill.HEALTH, Attributes.MAX_ABSORPTION, 4, 20, ADD, "Max absorption", false);
        p(Skill.HEALTH, Attributes.KNOCKBACK_RESISTANCE, 0.1, 10, ADD, "Knockback resistance", true);
        p(Skill.ATHLETICS, Attributes.MOVEMENT_SPEED, 0.17, 3, PCT, "Move speed", true);
        p(Skill.ATHLETICS, Attributes.JUMP_STRENGTH, 0.10, 5, PCT, "Jump height", true);
        p(Skill.ATHLETICS, Attributes.MOVEMENT_EFFICIENCY, 0.4, 10, ADD, "Mud/soul sand speed", true);
        p(Skill.MINING, Attributes.BLOCK_BREAK_SPEED, 0.60, 3, PCT, "Mining speed", true);
        p(Skill.MINING, Attributes.MINING_EFFICIENCY, 10, 10, ADD, "Tool efficiency", false);
        p(Skill.MINING, Attributes.SUBMERGED_MINING_SPEED, 1.0, 10, PCT, "Underwater mining", true);
        p(Skill.MINING, Attributes.BLOCK_INTERACTION_RANGE, 1.0, 20, ADD, "Reach (blocks)", false);
        p(Skill.SWORDS, Attributes.ATTACK_DAMAGE, 6, 3, ADD, "Melee damage", false);
        p(Skill.SWORDS, Attributes.ATTACK_SPEED, 0.20, 5, PCT, "Attack speed", true);
        p(Skill.SWORDS, Attributes.SWEEPING_DAMAGE_RATIO, 0.5, 10, ADD, "Sweep damage", true);
        p(Skill.SWORDS, Attributes.ENTITY_INTERACTION_RANGE, 0.75, 25, ADD, "Attack reach", false);
        p(Skill.ARCHERY, Attributes.LUCK, 2, 10, ADD, "Luck", false);
        p(Skill.ARCHERY, Attributes.MOVEMENT_SPEED, 0.03, 10, PCT, "Move speed", true);
        p(Skill.ARCHERY, Attributes.SNEAKING_SPEED, 0.15, 20, ADD, "Sneak speed", true);
        p(Skill.DEFENSE, Attributes.ARMOR, 10, 3, ADD, "Armor", false);
        p(Skill.DEFENSE, Attributes.ARMOR_TOUGHNESS, 6, 5, ADD, "Armor toughness", false);
        p(Skill.DEFENSE, Attributes.KNOCKBACK_RESISTANCE, 0.4, 10, ADD, "Knockback resistance", true);
        p(Skill.DEFENSE, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.5, 20, ADD, "Blast knockback resistance", true);
        p(Skill.ACROBATICS, Attributes.SAFE_FALL_DISTANCE, 12, 3, ADD, "Safe fall (blocks)", false);
        p(Skill.ACROBATICS, Attributes.JUMP_STRENGTH, 0.25, 5, PCT, "Jump height", true);
        p(Skill.ACROBATICS, Attributes.FALL_DAMAGE_MULTIPLIER, -0.5, 10, ADD, "Fall damage", true);
        p(Skill.ACROBATICS, Attributes.MOVEMENT_SPEED, 0.03, 10, PCT, "Move speed", true);
        p(Skill.SWIMMING, Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0, 3, ADD, "Swim speed", true);
        p(Skill.SWIMMING, Attributes.OXYGEN_BONUS, 12, 5, ADD, "Breath", false);
        p(Skill.SWIMMING, Attributes.SUBMERGED_MINING_SPEED, 0.5, 10, PCT, "Underwater mining", true);
        p(Skill.FARMING, Attributes.LUCK, 2, 5, ADD, "Luck", false);
        p(Skill.FARMING, Attributes.BLOCK_BREAK_SPEED, 0.10, 10, PCT, "Harvest speed", true);
        p(Skill.FARMING, Attributes.MOVEMENT_SPEED, 0.02, 20, PCT, "Move speed", true);
        p(Skill.WOODCUTTING, Attributes.BLOCK_BREAK_SPEED, 0.30, 3, PCT, "Chopping speed", true);
        p(Skill.WOODCUTTING, Attributes.ATTACK_DAMAGE, 2, 10, ADD, "Melee damage", false);
        p(Skill.WOODCUTTING, Attributes.BLOCK_INTERACTION_RANGE, 0.5, 25, ADD, "Reach (blocks)", false);
        p(Skill.HUNTING, Attributes.ATTACK_DAMAGE, 3, 5, ADD, "Melee damage", false);
        p(Skill.HUNTING, Attributes.MOVEMENT_SPEED, 0.04, 10, PCT, "Move speed", true);
        p(Skill.HUNTING, Attributes.LUCK, 1, 20, ADD, "Luck", false);
        p(Skill.HUNTING, Attributes.SNEAKING_SPEED, 0.2, 20, ADD, "Sneak speed", true);
        p(Skill.ENCHANTING, Attributes.LUCK, 4, 5, ADD, "Luck", false);
        p(Skill.ENCHANTING, Attributes.ARMOR_TOUGHNESS, 1, 25, ADD, "Armor toughness", false);
        p(Skill.ALCHEMY, Attributes.BURNING_TIME, -0.6, 5, PCT, "Burn time", true);
        p(Skill.ALCHEMY, Attributes.OXYGEN_BONUS, 4, 20, ADD, "Breath", false);
        p(Skill.ALCHEMY, Attributes.MAX_ABSORPTION, 4, 25, ADD, "Max absorption", false);
        p(Skill.EXPLORATION, Attributes.MOVEMENT_SPEED, 0.06, 5, PCT, "Move speed", true);
        p(Skill.EXPLORATION, Attributes.STEP_HEIGHT, 0.5, 10, ADD, "Step height", false);
        p(Skill.EXPLORATION, Attributes.SAFE_FALL_DISTANCE, 3, 20, ADD, "Safe fall (blocks)", false);
        p(Skill.EXPLORATION, Attributes.LUCK, 1, 25, ADD, "Luck", false);
        p(Skill.ENGINEERING, Attributes.BLOCK_INTERACTION_RANGE, 2.5, 5, ADD, "Reach (blocks)", false);
        p(Skill.ENGINEERING, Attributes.BLOCK_BREAK_SPEED, 0.15, 10, PCT, "Mining speed", true);
        p(Skill.ENGINEERING, Attributes.ARMOR, 2, 25, ADD, "Armor", false);
        p(Skill.SURVIVAL, Attributes.BURNING_TIME, -0.4, 5, PCT, "Burn time", true);
        p(Skill.SURVIVAL, Attributes.KNOCKBACK_RESISTANCE, 0.2, 10, ADD, "Knockback resistance", true);
        p(Skill.SURVIVAL, Attributes.MOVEMENT_EFFICIENCY, 0.5, 10, ADD, "Mud/soul sand speed", true);
        p(Skill.SURVIVAL, Attributes.MAX_HEALTH, 4, 50, ADD, "Max health", false);
        p(Skill.STEALTH, Attributes.SNEAKING_SPEED, 0.6, 3, ADD, "Sneak speed", true);
        p(Skill.STEALTH, Attributes.MOVEMENT_SPEED, 0.03, 10, PCT, "Move speed", true);
        p(Skill.STEALTH, Attributes.SAFE_FALL_DISTANCE, 4, 20, ADD, "Safe fall (blocks)", false);
        p(Skill.MEDICINE, Attributes.ARMOR_TOUGHNESS, 3, 10, ADD, "Armor toughness", false);
        p(Skill.MEDICINE, Attributes.MAX_ABSORPTION, 6, 20, ADD, "Max absorption", false);
        p(Skill.MEDICINE, Attributes.MAX_HEALTH, 4, 50, ADD, "Max health", false);
        p(Skill.HEAVY_WEAPONS, Attributes.ATTACK_DAMAGE, 4, 5, ADD, "Melee damage", false);
        p(Skill.HEAVY_WEAPONS, Attributes.ATTACK_KNOCKBACK, 2, 5, ADD, "Knockback", false);
        p(Skill.HEAVY_WEAPONS, Attributes.KNOCKBACK_RESISTANCE, 0.2, 10, ADD, "Knockback resistance", true);
        p(Skill.BUILDING, Attributes.BLOCK_INTERACTION_RANGE, 3, 3, ADD, "Reach (blocks)", false);
        p(Skill.BUILDING, Attributes.BLOCK_BREAK_SPEED, 0.20, 5, PCT, "Breaking speed", true);
        p(Skill.BUILDING, Attributes.ENTITY_INTERACTION_RANGE, 1, 10, ADD, "Attack reach", false);
        p(Skill.BUILDING, Attributes.SAFE_FALL_DISTANCE, 6, 20, ADD, "Safe fall (blocks)", false);
    }

    private static void p(Skill s, Holder<Attribute> a, double max, int every, AttributeModifier.Operation op, String label, boolean pct) {
        ALL.add(new Perk(s, a, max, every, op, label, pct));
    }

    public static List<Perk> of(Skill s) { return ALL.stream().filter(x -> x.skill == s).toList(); }

    private static Identifier id(Perk p) {
        return Kiyamet.id("perk/" + p.skill.name().toLowerCase() + "/" + p.attribute.unwrapKey().map(k -> k.identifier().getPath()).orElse("x"));
    }

    public static void apply(ServerPlayer p) {
        SkillData d = SkillManager.get(p);
        for (Perk perk : ALL) {
            AttributeInstance inst = p.getAttribute(perk.attribute);
            if (inst == null) continue;
            Identifier id = id(perk);
            double v = perk.value(d.level(perk.skill));
            if (v == 0) { inst.removeModifier(id); continue; }
            AttributeModifier cur = inst.getModifier(id);
            if (cur == null || cur.amount() != v) inst.addOrUpdateTransientModifier(new AttributeModifier(id, v, perk.op));
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) apply(p);
        });
    }

    private Perks() {}
}
