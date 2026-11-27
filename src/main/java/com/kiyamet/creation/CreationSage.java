package com.kiyamet.creation;

import com.kiyamet.Kiyamet;
import com.kiyamet.element.Sage;
import com.kiyamet.npc.HumanEntity;
import com.kiyamet.registry.ModBlocks;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillData;
import com.kiyamet.skill.SkillManager;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * TWD 2.2: the sages were always you, further down the road. When every one of your twenty skills reaches 999 you
 * become the Sage of Creation: you get the Altar of Creation, you build your own temple around it, and the companions
 * you choose become its guardians - they never die and never leave it.
 */
public final class CreationSage {
    public static final String TAG = "creation_sage", GUARD = "creation_guardian";
    /** where this player's altar stands (BlockPos as long), dimension id */
    public static final AttachmentType<Long> ALTAR = AttachmentRegistry.create(Kiyamet.id("creation_altar"), b -> b.persistent(Codec.LONG).copyOnDeath());
    public static final AttachmentType<String> ALTAR_DIM = AttachmentRegistry.create(Kiyamet.id("creation_altar_dim"), b -> b.persistent(Codec.STRING).copyOnDeath());

    public static boolean isSage(ServerPlayer p) { return p.getTags().contains(TAG); }

    public static boolean ready(SkillData d) {
        for (Skill s : Skill.values()) if (d.level(s) < Skill.MAX_LEVEL) return false;
        return true;
    }

    static final String[] STORY = {
            "The fourteen sages you met were never strangers.",
            "Each of them was you - the you that walked one element to its end.",
            "Fire, ice, thunder, shadow... every road you finished, a sage was waiting at the end of it.",
            "Now every road is finished. What is left is the element that made all the others:",
            "CREATION. You are its sage. There is no temple for you yet - you must build it yourself.",
            "Place the Altar of Creation where your temple will stand. Bring the companions you trust:",
            "sneak + right-click the altar, and they will guard it forever."};

    public static void ascend(ServerPlayer p) {
        if (isSage(p)) return;
        p.addTag(TAG);
        ServerLevel l = p.level();
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("SAGE OF CREATION").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("every element answers to you").withStyle(ChatFormatting.GRAY)));
        l.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        l.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1f, 1.3f);
        l.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 400, 2, 3, 2, 0.15);
        l.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), p.getX(), p.getY() + 1, p.getZ(), 4, 0, 0, 0, 0);
        for (String s : STORY) p.sendSystemMessage(Component.literal("✦ " + s).withStyle(ChatFormatting.WHITE));
        ItemStack altar = new ItemStack(ModBlocks.CREATION_ALTAR.asItem());
        if (!p.getInventory().add(altar)) p.drop(altar, false);
        l.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ " + p.getName().getString() + " has become the SAGE OF CREATION.").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), false);
    }

    static void founded(ServerPlayer p, BlockPos pos) {
        Long old = p.getAttached(ALTAR);
        p.setAttached(ALTAR, pos.asLong());
        p.setAttached(ALTAR_DIM, p.level().dimension().identifier().toString());
        p.sendSystemMessage(Component.literal("✦ Your Temple of Creation will stand here. Build it as you wish." + (old != null ? " (Your old altar is no longer your temple's heart.)" : "")).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(Component.literal("   Right-click the altar: your temple · sneak + right-click: nearby companions become its guardians (again, with none near: release them).").withStyle(ChatFormatting.GRAY));
        p.level().sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 80, 0.5, 2, 0.5, 0.1);
    }

    static boolean mine(ServerPlayer p, BlockPos pos) {
        Long at = p.getAttached(ALTAR);
        return at != null && at == pos.asLong() && p.level().dimension().identifier().toString().equals(p.getAttachedOrElse(ALTAR_DIM, ""));
    }

    static List<LivingEntity> guardians(ServerLevel l, ServerPlayer p, BlockPos pos, double r) {
        return l.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(r), e -> e.getTags().contains(GUARD) && owned(e, p));
    }

    static boolean owned(LivingEntity e, ServerPlayer p) {
        return e instanceof HumanEntity h && h.isOwnedBy(p) || e instanceof Sage s && s.isOwnedBy(p);
    }

    static void useAltar(ServerPlayer p, BlockPos pos) {
        ServerLevel l = p.level();
        if (!isSage(p) && !mine(p, pos)) { p.displayClientMessage(Component.literal("Only the Sage of Creation can wake this altar.").withStyle(ChatFormatting.GRAY), true); return; }
        if (!mine(p, pos)) founded(p, pos);
        BlockPos stand = pos.above();
        if (!p.isShiftKeyDown()) {
            List<LivingEntity> g = guardians(l, p, pos, 64);
            p.sendSystemMessage(Component.literal("✦ TEMPLE OF CREATION  ·  guardians: " + g.size()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            for (LivingEntity e : g) p.sendSystemMessage(Component.literal("   · " + e.getName().getString()).withStyle(ChatFormatting.AQUA));
            if (g.isEmpty()) p.sendSystemMessage(Component.literal("   Bring your companions here and sneak + right-click the altar.").withStyle(ChatFormatting.GRAY));
            l.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.3, 0.8, 0.3, 0.05);
            return;
        }
        List<LivingEntity> near = l.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(24), e -> owned(e, p) && !e.getTags().contains(GUARD));
        if (near.isEmpty()) {
            List<LivingEntity> g = guardians(l, p, pos, 64);
            for (LivingEntity e : g) {
                e.removeTag(GUARD);
                e.removeEffect(MobEffects.STRENGTH); e.removeEffect(MobEffects.RESISTANCE);
                if (e instanceof HumanEntity h) h.setOrder(HumanEntity.Order.FOLLOW);
                if (e instanceof Sage s) s.release();
            }
            p.sendSystemMessage(Component.literal(g.isEmpty() ? "No companions nearby to make guardians." : "✦ Released " + g.size() + " guardian(s). They follow you again.").withStyle(ChatFormatting.GRAY));
            return;
        }
        int i = 0;
        for (LivingEntity e : near) {
            e.addTag(GUARD);
            e.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 1, true, false));
            e.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, 1, true, false));
            e.setHealth(e.getMaxHealth());
            double a = Math.toRadians(i++ * (360.0 / near.size()));
            BlockPos post = stand.offset((int) Math.round(Math.cos(a) * 4), 0, (int) Math.round(Math.sin(a) * 4));
            if (e instanceof HumanEntity h) { h.setOrder(HumanEntity.Order.GUARD); h.setHome(post); }
            if (e instanceof Sage s) s.guardTemple(post);
            l.sendParticles(new DustParticleOptions(0xFFFFFF, 2f), e.getX(), e.getY() + 1, e.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            p.sendSystemMessage(Component.literal("✦ " + e.getName().getString() + " is now a guardian of your Temple of Creation. They will never fall.").withStyle(ChatFormatting.WHITE));
        }
        l.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 1.1f);
    }

    public static void init() {
        // guardians of a Temple of Creation cannot die: they drop to half health and keep standing
        ServerLivingEntityEvents.ALLOW_DEATH.register((e, src, amount) -> {
            if (!e.getTags().contains(GUARD)) return true;
            e.setHealth(e.getMaxHealth() * 0.5f);
            if (e.level() instanceof ServerLevel l) l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.8, 0.4, 0.3);
            return false;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((e, src, base, taken, blocked) -> {
            if (e.getTags().contains(GUARD)) e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, true, false));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!isSage(p)) { if (ready(SkillManager.get(p))) ascend(p); continue; }
                // the Sage of Creation: strength, regeneration and a quiet white light
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 0, true, false));
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 140, 0, true, false));
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 140, 0, true, false));
                p.level().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 8, 0.4, 0.8, 0.4, 0.02);
            }
        });
        // testing helper: /twd ascend (creative or console)
        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(Commands.literal("twd").then(Commands.literal("ascend")
                .executes(c -> {
                    if (c.getSource().getPlayer() != null && !c.getSource().getPlayer().isCreative()) return 0;
                    int n = 0;
                    for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) if (c.getSource().getPlayer() == null || c.getSource().getPlayer() == p) { ascend(p); n++; }
                    int fn = n;
                    c.getSource().sendSuccess(() -> Component.literal("Ascended " + fn + " player(s)."), true);
                    return 1;
                }))));
    }

    private CreationSage() {}
}
