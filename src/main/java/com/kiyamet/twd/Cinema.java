package com.kiyamet.twd;

import com.kiyamet.npc.Talk;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Story cut-scenes, made from what the game can do without a camera mod: fade to black, title cards,
 * a slow-down "freeze", the camera turning toward where you have to go, sound and particles.
 */
public final class Cinema {
    public static final String[] BOOKS = {"BOOK I: THE LONG ROAD", "BOOK II: ACROSS THE WATER", "BOOK III: THE BURNING CITIES", "BOOK IV: LEGACY"};
    static final int[] BOOK_START = {1, 60, 101, 151};

    static void title(ServerPlayer p, int in, int stay, int out, Component t, Component sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(t));
    }

    static void freeze(ServerPlayer p, int ticks) {
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 4, true, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, 4, true, false, false));
    }

    static void fadeBlack(ServerPlayer p, int ticks) { p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, true, false, false)); }

    /** Opening of a chapter: black screen, (book card), chapter card, then the camera turns toward the objective. */
    public static void chapter(ServerPlayer p, int ch, String name, String tagline, BlockPos target) {
        ServerLevel level = p.level();
        int book = -1;
        for (int i = 0; i < BOOK_START.length; i++) if (BOOK_START[i] == ch) book = i;
        int d = 0;
        fadeBlack(p, book >= 0 ? 120 : 60);
        freeze(p, book >= 0 ? 160 : 100);
        level.playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.MASTER, 0.8f, 0.6f);
        if (book >= 0) {
            final int b = book;
            Talk.later(p, 10, () -> {
                title(p, 30, 60, 20, Component.literal(BOOKS[b]).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.literal("The Long Road").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                level.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.MASTER, 0.35f, 0.7f);
            });
            d = 100;
        }
        Talk.later(p, d + 20, () -> {
            title(p, 15, 70, 25, Component.literal("CHAPTER " + ch).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                    Component.literal(name + " - " + tagline).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
        });
        if (target != null) Talk.later(p, d + 55, () -> {
            p.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(target).add(0, 1, 0));
            Vec3 dir = Vec3.atCenterOf(target).subtract(p.position()).normalize();
            for (int i = 2; i < 14; i++) {
                Vec3 q = p.getEyePosition().add(dir.scale(i));
                level.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 1f, 0.8f);
        });
    }

    /** A boss or named enemy comes into view. */
    public static void boss(ServerPlayer p, Entity e, String name) {
        freeze(p, 50);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, e.getEyePosition());
        title(p, 5, 50, 20, Component.literal("★ " + name.toUpperCase() + " ★").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), Component.literal("Story enemy").withStyle(ChatFormatting.GRAY));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 0.8f);
    }

    public static void passed(ServerPlayer p, int reward, boolean finale) {
        ServerLevel level = p.level();
        freeze(p, 40);
        title(p, 10, 60, 20, Component.literal(finale ? "THE END" : "MISSION PASSED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("+" + reward + " rubies").withStyle(ChatFormatting.GREEN));
        level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1f, 1f);
        for (int i = 0; i < (finale ? 6 : 2); i++) {
            final int k = i;
            Talk.later(p, 10 + i * 12, () -> {
                double a = k * 1.3;
                level.sendParticles(ParticleTypes.FIREWORK, p.getX() + Math.cos(a) * 3, p.getY() + 4, p.getZ() + Math.sin(a) * 3, 60, 0.4, 0.4, 0.4, 0.15);
                level.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.MASTER, 0.8f, 1f);
            });
        }
    }

    public static void failed(ServerPlayer p, String why) {
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false, false));
        title(p, 5, 60, 25, Component.literal("MISSION FAILED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), Component.literal(why).withStyle(ChatFormatting.GRAY));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1f, 0.6f);
    }

    private Cinema() {}
}
