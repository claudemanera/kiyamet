package com.kiyamet.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Map;

/** A block is powered when a burning generator is within 8 blocks. Results are cached for 5 seconds. */
public final class Power {
    public static final int RANGE = 8;
    private record Cached(boolean on, long until) {}
    private static final Map<Long, Cached> CACHE = new HashMap<>();

    public static boolean isPowered(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        Cached c = CACHE.get(pos.asLong());
        if (c != null && c.until > now) return c.on;
        boolean on = false;
        search:
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RANGE, -RANGE, -RANGE), pos.offset(RANGE, RANGE, RANGE))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof GeneratorBlock && s.getValue(BlockStateProperties.LIT)) { on = true; break search; }
        }
        if (CACHE.size() > 20000) CACHE.clear();
        CACHE.put(pos.asLong(), new Cached(on, now + 100));
        return on;
    }

    public static void invalidate() { CACHE.clear(); }

    private Power() {}
}
