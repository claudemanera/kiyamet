package com.kiyamet.skill;

import com.kiyamet.Kiyamet;
import java.time.LocalDate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SkillManager {
    /** First 2 real hours of play each day give +50% XP. */
    public static final int RESTED_TICKS = 2 * 60 * 60 * 20;
    private static final Identifier HEALTH_MOD = Kiyamet.id("skill_health");

    public static SkillData get(ServerPlayer p) {
        return p.getAttachedOrSet(ModAttachments.SKILLS, SkillData.empty());
    }

    public static void set(ServerPlayer p, SkillData d) {
        p.setAttached(ModAttachments.SKILLS, d);
    }

    /** Adds XP to one skill; handles rested bonus, level ups, milestone messages and the health bonus. */
    public static void addXp(ServerPlayer p, Skill skill, double base) {
        if (base <= 0 || p.isCreative() || p.isSpectator()) return;
        SkillData d = get(p);
        int level = d.level(skill);
        if (level >= Skill.MAX_LEVEL) return;
        boolean rested = d.restTicks() < RESTED_TICKS && d.restDay() == LocalDate.now().toEpochDay();
        long gain = Math.max(1, Math.round(base * (rested ? 1.5 : 1.0)));
        long xp = d.xp(skill) + gain;
        int startLevel = level;
        while (level < Skill.MAX_LEVEL && xp >= XpCurve.toNext(level)) {
            xp -= XpCurve.toNext(level);
            level++;
        }
        if (level >= Skill.MAX_LEVEL) xp = 0;
        set(p, d.with(skill, level, xp));

        Component bar = Component.literal("+" + gain + " ").withStyle(rested ? ChatFormatting.AQUA : ChatFormatting.GREEN)
                .append(Component.translatable(skill.key()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  " + level + "  ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(xp + "/" + XpCurve.toNext(level)).withStyle(ChatFormatting.GRAY));
        p.connection.send(new ClientboundSetActionBarTextPacket(bar));

        if (level > startLevel) onLevelUp(p, skill, startLevel, level);
    }

    private static void onLevelUp(ServerPlayer p, Skill skill, int from, int to) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.3f);
        p.sendSystemMessage(Component.translatable("kiyamet.msg.level_up", Component.translatable(skill.key()), to).withStyle(ChatFormatting.GOLD));
        for (int m = from + 1; m <= to; m++) {
            if (m == 250 || m == 500 || m == 750 || m == 999)
                p.sendSystemMessage(Component.translatable("kiyamet.msg.big_perk", Component.translatable(skill.key()), m).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            else if (m % 50 == 0)
                p.sendSystemMessage(Component.translatable("kiyamet.msg.perk", Component.translatable(skill.key()), m).withStyle(ChatFormatting.YELLOW));
        }
        if (skill == Skill.HEALTH) applyHealth(p);
    }

    /** Health skill: +1 HP (half heart) every 33 levels → +30 HP at 999 = 25 hearts total. */
    public static void applyHealth(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        int bonus = Math.min(30, get(p).level(Skill.HEALTH) / 33);
        inst.removeModifier(HEALTH_MOD);
        if (bonus > 0) inst.addPermanentModifier(new AttributeModifier(HEALTH_MOD, bonus, AttributeModifier.Operation.ADD_VALUE));
    }

    /** Called every second for every online player: counts the daily rested window. */
    public static void tickRest(ServerPlayer p) {
        SkillData d = get(p);
        long today = LocalDate.now().toEpochDay();
        if (d.restDay() != today) set(p, d.withRest(today, 20));
        else if (d.restTicks() < RESTED_TICKS) set(p, d.withRest(today, d.restTicks() + 20));
    }

    private SkillManager() {}
}
