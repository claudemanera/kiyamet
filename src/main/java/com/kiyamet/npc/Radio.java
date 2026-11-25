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
    public record Last(ResourceKey<Level> dim, BlockPos pos, String name, int level, int team, String job) {}
    record Call(long at, java.util.Set<UUID> who) {}
    private static final Map<UUID, Map<UUID, Last>> KNOWN = new HashMap<>();
    private static final Map<UUID, Call> CALLS = new HashMap<>();
    public static final int DELAY = 400;

    public Radio(Properties p) { super(p); }

    /** Companions report where they are every few seconds. */
    public static void report(HumanEntity h) {
        if (h.owner() == null || h.isAway()) return;
        KNOWN.computeIfAbsent(h.owner(), k -> new HashMap<>()).put(h.getUUID(), new Last(h.level().dimension(), h.blockPosition(), h.getName().getString(), h.lvl(), h.team(), h.job().title));
    }

    public static void forget(HumanEntity h) {
        if (h.owner() != null && KNOWN.containsKey(h.owner())) KNOWN.get(h.owner()).remove(h.getUUID());
    }

    public static Map<UUID, Last> known(ServerPlayer p) { return KNOWN.getOrDefault(p.getUUID(), Map.of()); }

    public static boolean calling(ServerPlayer p) { return CALLS.containsKey(p.getUUID()); }

    public static long callEnds(ServerPlayer p) { Call c = CALLS.get(p.getUUID()); return c == null ? 0 : c.at; }

    /** Starts a call for the given companions; they arrive after 20 seconds. */
    public static int call(ServerPlayer p, java.util.Collection<UUID> who) {
        Map<UUID, Last> mine = known(p);
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        MinecraftServer server = p.level().getServer();
        for (UUID u : who) {
            Last l = mine.get(u);
            if (l == null) continue;
            ids.add(u);
            ServerLevel sl = server.getLevel(l.dim);
            if (sl != null) sl.setChunkForced(l.pos.getX() >> 4, l.pos.getZ() >> 4, true);
        }
        if (ids.isEmpty()) return 0;
        Call old = CALLS.get(p.getUUID());
        if (old != null) ids.addAll(old.who);
        CALLS.put(p.getUUID(), new Call(p.level().getGameTime() + DELAY, ids));
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 1f, 0.6f);
        return ids.size();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        RadioMenu.open(p);
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
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
                    if (!e.getValue().who.contains(m.getKey())) continue;
                    ServerLevel sl = server.getLevel(m.getValue().dim);
                    if (sl == null) continue;
                    Entity ent = sl.getEntity(m.getKey());
                    if (ent instanceof HumanEntity h && h.isAlive() && h.isOwnedBy(p) && !h.isAway()) {
                        double x = p.getX() + p.getRandom().nextInt(5) - 2, z = p.getZ() + p.getRandom().nextInt(5) - 2;
                        if (sl != p.level()) continue;                       // other dimension: can't reach
                        h.teleportTo(x, p.getY(), z);
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
