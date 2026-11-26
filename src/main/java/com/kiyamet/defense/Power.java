package com.kiyamet.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;

/**
 * Generators power blocks around them. Generators placed touching each other (side by side or stacked)
 * form a cluster of up to 65: every extra running generator adds +1 block of range (8 alone, 72 with 65)
 * and the whole cluster burns fuel more slowly. Results are cached for 5 seconds.
 */
public final class Power {
    public static final int RANGE = 8, PER_EXTRA = 1, MAX_CLUSTER = 65;
    private record Cached(boolean on, long until) {}
    private static final Map<Long, Cached> CACHE = new HashMap<>();
    /** Known generator positions per dimension (filled as generators are placed, fuelled and tick). */
    private static final Map<ResourceKey<Level>, Set<Long>> GENS = new HashMap<>();
    private static final Map<Long, Integer> CLUSTER = new HashMap<>();

    public static void track(ServerLevel level, BlockPos pos) { GENS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.asLong()); }

    static boolean running(BlockState s) { return s.getBlock() instanceof GeneratorBlock && s.getValue(BlockStateProperties.LIT) && s.getValue(GeneratorBlock.ON); }

    /** Generators connected to this one (touching faces), counting running ones, max 20. */
    public static List<BlockPos> cluster(ServerLevel level, BlockPos start) {
        List<BlockPos> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start.immutable()); seen.add(start.asLong());
        while (!q.isEmpty() && out.size() < 128) {
            BlockPos c = q.poll();
            if (!(level.getBlockState(c).getBlock() instanceof GeneratorBlock)) continue;
            out.add(c);
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (seen.add(n.asLong()) && level.getBlockState(n).getBlock() instanceof GeneratorBlock) q.add(n);
            }
        }
        return out;
    }

    public static int runningInCluster(ServerLevel level, BlockPos pos) {
        Integer c = CLUSTER.get(pos.asLong());
        if (c != null) return c;
        int n = 0;
        for (BlockPos p : cluster(level, pos)) if (running(level.getBlockState(p))) n++;
        n = Math.min(MAX_CLUSTER, n);
        CLUSTER.put(pos.asLong(), n);
        return n;
    }

    public static int range(ServerLevel level, BlockPos gen) {
        int n = runningInCluster(level, gen);
        return RANGE + PER_EXTRA * Math.max(0, n - 1);
    }

    public static boolean isPowered(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        Cached c = CACHE.get(pos.asLong());
        if (c != null && c.until > now) return c.on;
        if (DeviceSwitch.isOff(level, pos)) { CACHE.put(pos.asLong(), new Cached(false, now + 100)); return false; }
        boolean on = false;
        // nearby generators (also finds ones not tracked yet, e.g. right after a restart)
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RANGE, -RANGE, -RANGE), pos.offset(RANGE, RANGE, RANGE))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof GeneratorBlock) { track(level, p); if (running(s)) { on = true; break; } }
        }
        if (!on) on = com.kiyamet.power.Renewables.near(level, pos, RANGE);   // 1.9 solar panels and wind turbines
        if (!on) {
            Set<Long> gens = GENS.getOrDefault(level.dimension(), Set.of());
            for (Iterator<Long> it = gens.iterator(); it.hasNext(); ) {
                BlockPos g = BlockPos.of(it.next());
                int max = RANGE + PER_EXTRA * (MAX_CLUSTER - 1);
                if (Math.abs(g.getX() - pos.getX()) > max || Math.abs(g.getY() - pos.getY()) > max || Math.abs(g.getZ() - pos.getZ()) > max) continue;
                if (!level.isLoaded(g)) continue;
                BlockState s = level.getBlockState(g);
                if (!(s.getBlock() instanceof GeneratorBlock)) { it.remove(); continue; }
                if (!running(s)) continue;
                int r = range(level, g);
                if (Math.abs(g.getX() - pos.getX()) <= r && Math.abs(g.getY() - pos.getY()) <= r && Math.abs(g.getZ() - pos.getZ()) <= r) { on = true; break; }
            }
        }
        if (CACHE.size() > 20000) CACHE.clear();
        CACHE.put(pos.asLong(), new Cached(on, now + 100));
        return on;
    }

    public static void invalidate() { CACHE.clear(); CLUSTER.clear(); }

    private Power() {}
}
