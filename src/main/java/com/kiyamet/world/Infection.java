package com.kiyamet.world;

import com.kiyamet.Kiyamet;
import com.kiyamet.entity.ZombieKind;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Zombie hits can infect (base 8%, Survival skill lowers it, immune at Survival 999).
 * Infection counts up in seconds: 5 min hunger, 10 min weakness + nausea, 15 min wither until cured.
 * Cured by a Vaccine (or Medicine perks later).
 */
public final class Infection {
    public static final AttachmentType<Integer> INFECTION = AttachmentRegistry.create(Kiyamet.id("infection"),
            b -> b.initializer(() -> 0).persistent(Codec.INT));

    public static void init() {}

    public static int get(ServerPlayer p) { return p.getAttachedOrElse(INFECTION, 0); }

    public static void onZombieHit(ServerPlayer p, ZombieKind kind) {
        if (get(p) > 0 || p.isCreative()) return;
        int survival = SkillManager.get(p).level(Skill.SURVIVAL);
        if (survival >= 999) return;
        double chance = 0.08 * (1.0 - survival / 1100.0) * (kind == ZombieKind.TOXIC ? 2 : 1);
        if (p.getRandom().nextDouble() < chance) {
            p.setAttached(INFECTION, 1);
            p.sendSystemMessage(Component.translatable("kiyamet.msg.infected").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        }
    }

    public static void cure(ServerPlayer p) {
        p.removeAttached(INFECTION);
        p.removeEffect(MobEffects.WITHER);
        p.removeEffect(MobEffects.NAUSEA);
        p.removeEffect(MobEffects.WEAKNESS);
        p.removeEffect(MobEffects.HUNGER);
        p.sendSystemMessage(Component.translatable("kiyamet.msg.cured").withStyle(ChatFormatting.GREEN));
    }

    /** Called once per second per player. */
    public static void tick(ServerPlayer p) {
        int t = get(p);
        if (t <= 0) return;
        p.setAttached(INFECTION, t + 1);
        if (t >= 300) p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 0, true, false));
        if (t >= 600) { p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false)); if (t % 30 == 0) p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0, true, false)); }
        if (t >= 900) p.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, true, false));
        if (t % 60 == 0) SkillManager.addXp(p, Skill.SURVIVAL, 10);
    }

    private Infection() {}
}
