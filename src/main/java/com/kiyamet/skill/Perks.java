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
 * Kıyamet 1.2 perks: every 50 levels of a skill is one "step" (max 19 at level 950+, 20 at 999) and each step
 * permanently raises one or two attributes. Bonuses are refreshed every 5 seconds.
 */
public final class Perks {
    public record Perk(Skill skill, Holder<Attribute> attribute, double perStep, AttributeModifier.Operation op, String label, boolean percent) {
        public double value(int steps) { return perStep * steps; }
        public String describe(int steps) {
            double v = value(steps);
            return label + " +" + (percent ? String.format("%.0f%%", v * 100) : String.format(v < 1 ? "%.2f" : "%.1f", v));
        }
    }

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE, PCT = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    public static final List<Perk> ALL = new ArrayList<>();

    static {
        p(Skill.ATHLETICS, Attributes.MOVEMENT_SPEED, 0.006, PCT, "Move speed", true);
        p(Skill.MINING, Attributes.BLOCK_BREAK_SPEED, 0.025, PCT, "Mining speed", true);
        p(Skill.MINING, Attributes.SUBMERGED_MINING_SPEED, 0.05, PCT, "Underwater mining", true);
        p(Skill.SWORDS, Attributes.ATTACK_DAMAGE, 0.3, ADD, "Melee damage", false);
        p(Skill.SWORDS, Attributes.ATTACK_SPEED, 0.01, PCT, "Attack speed", true);
        p(Skill.ARCHERY, Attributes.LUCK, 0.05, ADD, "Luck", false);
        p(Skill.DEFENSE, Attributes.ARMOR, 0.5, ADD, "Armor", false);
        p(Skill.DEFENSE, Attributes.ARMOR_TOUGHNESS, 0.25, ADD, "Armor toughness", false);
        p(Skill.DEFENSE, Attributes.KNOCKBACK_RESISTANCE, 0.02, ADD, "Knockback resistance", true);
        p(Skill.ACROBATICS, Attributes.SAFE_FALL_DISTANCE, 0.6, ADD, "Safe fall (blocks)", false);
        p(Skill.ACROBATICS, Attributes.JUMP_STRENGTH, 0.012, PCT, "Jump height", true);
        p(Skill.SWIMMING, Attributes.WATER_MOVEMENT_EFFICIENCY, 0.04, ADD, "Swim speed", true);
        p(Skill.SWIMMING, Attributes.OXYGEN_BONUS, 0.5, ADD, "Breath", false);
        p(Skill.FARMING, Attributes.LUCK, 0.05, ADD, "Luck", false);
        p(Skill.WOODCUTTING, Attributes.BLOCK_BREAK_SPEED, 0.015, PCT, "Chopping/mining speed", true);
        p(Skill.HUNTING, Attributes.ATTACK_DAMAGE, 0.1, ADD, "Melee damage", false);
        p(Skill.HUNTING, Attributes.FOLLOW_RANGE, 0.0, ADD, "", false);
        p(Skill.ENCHANTING, Attributes.LUCK, 0.08, ADD, "Luck", false);
        p(Skill.ALCHEMY, Attributes.BURNING_TIME, -0.03, PCT, "Burn time", true);
        p(Skill.EXPLORATION, Attributes.STEP_HEIGHT, 0.025, ADD, "Step height", false);
        p(Skill.EXPLORATION, Attributes.MOVEMENT_SPEED, 0.003, PCT, "Move speed", true);
        p(Skill.ENGINEERING, Attributes.BLOCK_INTERACTION_RANGE, 0.1, ADD, "Reach (blocks)", false);
        p(Skill.SURVIVAL, Attributes.BURNING_TIME, -0.02, PCT, "Burn time", true);
        p(Skill.SURVIVAL, Attributes.KNOCKBACK_RESISTANCE, 0.01, ADD, "Knockback resistance", true);
        p(Skill.STEALTH, Attributes.SNEAKING_SPEED, 0.025, ADD, "Sneak speed", true);
        p(Skill.MEDICINE, Attributes.ARMOR_TOUGHNESS, 0.1, ADD, "Armor toughness", false);
        p(Skill.HEAVY_WEAPONS, Attributes.ATTACK_KNOCKBACK, 0.06, ADD, "Knockback", false);
        p(Skill.HEAVY_WEAPONS, Attributes.ATTACK_DAMAGE, 0.15, ADD, "Melee damage", false);
        p(Skill.BUILDING, Attributes.BLOCK_INTERACTION_RANGE, 0.12, ADD, "Reach (blocks)", false);
        p(Skill.BUILDING, Attributes.ENTITY_INTERACTION_RANGE, 0.05, ADD, "Attack reach", false);
        ALL.removeIf(x -> x.perStep == 0);
    }

    private static void p(Skill s, Holder<Attribute> a, double v, AttributeModifier.Operation op, String label, boolean pct) {
        ALL.add(new Perk(s, a, v, op, label, pct));
    }

    public static int steps(int level) { return level >= Skill.MAX_LEVEL ? 20 : level / 50; }

    public static List<Perk> of(Skill s) { return ALL.stream().filter(x -> x.skill == s).toList(); }

    private static Identifier id(Perk p) {
        return Kiyamet.id("perk/" + p.skill.name().toLowerCase() + "/" + p.attribute.unwrapKey().map(k -> k.identifier().getPath()).orElse("x"));
    }

    public static void apply(ServerPlayer p) {
        SkillData d = SkillManager.get(p);
        for (Perk perk : ALL) {
            AttributeInstance inst = p.getAttribute(perk.attribute);
            if (inst == null) continue;
            int steps = steps(d.level(perk.skill));
            Identifier id = id(perk);
            if (steps == 0) { inst.removeModifier(id); continue; }
            AttributeModifier cur = inst.getModifier(id);
            double v = perk.value(steps);
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
