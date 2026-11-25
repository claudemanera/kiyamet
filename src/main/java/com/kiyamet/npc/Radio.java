package com.kiyamet.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Walkie-talkie: call your whole group. 20 seconds later every companion teleports to you, like tamed wolves —
 * even ones left far away (their chunks are loaded for the call). Sneak + use opens the group panel.
 */
public class Radio extends Item {
    record Last(ResourceKey<Level> dim, BlockPos pos) {}
    record Call(long at, UUID player) {}
    private static final Map<UUID, Map<UUID, Last>> KNOWN = new HashMap<>();
    private static final Map<UUID, Call> CALLS = new HashMap<>();
    public static final int DELAY = 400;

    public Radio(Properties p) { super(p); }

    /** Companions report where they are every few seconds. */
    public static void report(HumanEntity h) {
        if (h.owner() == null || h.isAway()) return;
        KNOWN.computeIfAbsent(h.owner(), k -> new HashMap<>()).put(h.getUUID(), new Last(h.level().dimension(), h.blockPosition()));
    }

    public static void forget(HumanEntity h) {
        if (h.owner() != null && KNOWN.containsKey(h.owner())) KNOWN.get(h.owner()).remove(h.getUUID());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        if (p.isShiftKeyDown()) { SquadMenu.open(p); return InteractionResult.CONSUME; }
        Map<UUID, Last> mine = KNOWN.getOrDefault(p.getUUID(), Map.of());
        if (mine.isEmpty()) { p.displayClientMessage(Component.literal("Static... nobody answers. (No companions yet)").withStyle(ChatFormatting.GRAY), true); return InteractionResult.FAIL; }
        MinecraftServer server = p.level().getServer();
        for (Last l : mine.values()) {
            ServerLevel sl = server.getLevel(l.dim);
            if (sl != null) sl.setChunkForced(l.pos.getX() >> 4, l.pos.getZ() >> 4, true);
        }
        CALLS.put(p.getUUID(), new Call(p.level().getGameTime() + DELAY, p.getUUID()));
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 1f, 0.6f);
        p.sendSystemMessage(Component.literal("[Radio] \"Everyone, come to me!\" — your group will reach you in 20 seconds.").withStyle(ChatFormatting.AQUA));
        p.getCooldowns().addCooldown(p.getItemInHand(hand), 600);
        return InteractionResult.CONSUME;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (CALLS.isEmpty()) return;
            CALLS.entrySet().removeIf(e -> {
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null) return true;
                if (p.level().getGameTime() < e.getValue().at) return false;
                int arrived = 0;
                Map<UUID, Last> mine = KNOWN.getOrDefault(p.getUUID(), Map.of());
                for (Map.Entry<UUID, Last> m : new HashMap<>(mine).entrySet()) {
                    ServerLevel sl = server.getLevel(m.getValue().dim);
                    if (sl == null) continue;
                    Entity ent = sl.getEntity(m.getKey());
                    if (ent instanceof HumanEntity h && h.isAlive() && h.isOwnedBy(p) && !h.isAway()) {
                        double x = p.getX() + p.getRandom().nextInt(5) - 2, z = p.getZ() + p.getRandom().nextInt(5) - 2;
                        if (sl != p.level()) h.teleport(new net.minecraft.world.entity.TeleportTransition(p.level(), new net.minecraft.world.phys.Vec3(x, p.getY(), z),
                                net.minecraft.world.phys.Vec3.ZERO, h.getYRot(), h.getXRot(), net.minecraft.world.entity.TeleportTransition.DO_NOTHING));
                        else h.teleportTo(x, p.getY(), z);
                        h.setOrder(HumanEntity.Order.FOLLOW);
                        p.level().sendParticles(ParticleTypes.PORTAL, x, p.getY() + 1, z, 20, 0.3, 0.6, 0.3, 0.2);
                        arrived++;
                    }
                    sl.setChunkForced(m.getValue().pos.getX() >> 4, m.getValue().pos.getZ() >> 4, false);
                }
                p.sendSystemMessage(Component.literal("[Radio] " + arrived + " companion(s) arrived.").withStyle(ChatFormatting.AQUA));
                return true;
            });
        });
    }
}
