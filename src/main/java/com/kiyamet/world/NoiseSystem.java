package com.kiyamet.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Loud actions (mining, explosions, guns, car horns, screams) leave short-lived noises that idle zombies walk to. */
public final class NoiseSystem {
    private record Noise(ServerLevel level, BlockPos pos, double radius, long until) {}
    private static final List<Noise> NOISES = new ArrayList<>();

    public static void make(ServerLevel level, BlockPos pos, double radius, int seconds) {
        if (NOISES.size() > 256) NOISES.remove(0);
        NOISES.add(new Noise(level, pos.immutable(), radius, level.getGameTime() + seconds * 20L));
    }

    public static BlockPos loudestNear(ServerLevel level, BlockPos at, double hearing) {
        long now = level.getGameTime();
        BlockPos best = null;
        double bestScore = 0;
        for (Iterator<Noise> it = NOISES.iterator(); it.hasNext(); ) {
            Noise n = it.next();
            if (n.until < now) { it.remove(); continue; }
            if (n.level != level) continue;
            double r = n.radius * hearing, d = Math.sqrt(n.pos.distSqr(at));
            if (d < r && r - d > bestScore) { bestScore = r - d; best = n.pos; }
        }
        return best;
    }

    private NoiseSystem() {}
}
