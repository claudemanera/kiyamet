package com.kiyamet.world;

import com.kiyamet.skill.SkillManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Zombie strength follows the nearest player's Total Power and stops growing at 8000 (x3 health and damage). */
public final class Difficulty {
    public static final int CAP = 8000;

    public static int nearestPower(ServerLevel level, BlockPos pos) {
        ServerPlayer best = null;
        double bd = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            double d = p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            if (d < bd) { bd = d; best = p; }
        }
        return best == null ? 0 : SkillManager.get(best).totalPower();
    }

    public static double multiplierNear(ServerLevel level, BlockPos pos) {
        return 1.0 + 2.0 * Math.min(nearestPower(level, pos), CAP) / CAP;
    }

    private Difficulty() {}
}
