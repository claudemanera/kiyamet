package com.kiyamet.end;

import com.kiyamet.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * TWD 2.2: the Temple of Wisdom - small, white, glowing, and unbreakable. It floats just south of the dragon's island,
 * a white bridge leads down to the island. In its middle stands the Table of Creation.
 */
public final class WisdomTemple {
    public static Builder plan(ServerLevel l, BlockPos c) {
        BlockState W = ModBlocks.WISDOM_STONE.defaultBlockState(), G = ModBlocks.WISDOM_GLASS.defaultBlockState(), L = ModBlocks.WISDOM_LIGHT.defaultBlockState();
        Builder s = new Builder(l, c, c.asLong());
        s.loadChunks(16);
        // the floating base
        s.island(0, 0, 0, 10.5, 8, W, W);
        s.fill(-11, 1, -11, 11, 14, 11, Builder.AIR);
        s.disc(0, 0, 0, 10.5, W);
        s.ring(0, 0, 0, 5.4, 4.6, G);
        for (int k = 0; k < 12; k++) { double a = Math.toRadians(k * 30); s.set((int) Math.round(Math.cos(a) * 3.2), 0, (int) Math.round(Math.sin(a) * 3.2), L); }       // the twelve stones
        for (int k = 0; k < 14; k++) { double a = Math.toRadians(k * 360.0 / 14); s.set((int) Math.round(Math.cos(a) * 7), 0, (int) Math.round(Math.sin(a) * 7), G); }  // the fourteen elements
        s.ring(0, 0, 0, 10.5, 9.6, L);
        // eight pillars and the ring beam
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45);
            int x = (int) Math.round(Math.cos(a) * 8), z = (int) Math.round(Math.sin(a) * 8);
            s.fill(x, 1, z, x, 6, z, W); s.set(x, 1, z, L); s.set(x, 6, z, L);
        }
        s.ring(0, 7, 0, 8.7, 7.3, W);
        // the glass dome with white ribs, a light at the top
        for (int x = -9; x <= 9; x++) for (int y = 0; y <= 9; y++) for (int z = -9; z <= 9; z++) {
            double d = Math.sqrt(x * x + y * y + z * z);
            if (d > 8.7 || d < 7.7) continue;
            boolean rib = x == 0 || z == 0 || Math.abs(x) == Math.abs(z);
            s.set(x, 7 + y, z, rib ? W : G);
        }
        s.set(0, 16, 0, L); s.set(0, 15, 0, L);
        // the Table of Creation on its step
        s.disc(0, 1, 0, 1.6, W);
        s.set(0, 1, 0, Builder.AIR);
        s.set(0, 0, 0, L);
        s.set(0, 1, 0, ModBlocks.CREATION_TABLE.defaultBlockState());
        // the white bridge north to the island, stepping down to meet the ground
        int y = 0;
        for (int z = -11; z >= -120; z--) {
            int wz = c.getZ() + z;
            l.getChunk(c.getX() >> 4, wz >> 4);
            int ground = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), wz) - 1 - c.getY();
            boolean land = ground > -40;
            if (land && y > ground && z % 2 == 0) y--;
            else if (land && y < ground && z % 2 == 0) y++;
            s.fill(-1, y, z, 1, y, z, W);
            s.fill(-1, y + 1, z, 1, y + 3, z, Builder.AIR);
            if (z % 6 == 0) { s.set(-2, y + 1, z, L); s.set(2, y + 1, z, L); s.set(-2, y, z, W); s.set(2, y, z, W); }
            if (land && y == ground) { s.fill(-2, y, z - 1, 2, y, z - 3, W); s.set(0, y + 1, z - 3, L); break; }
        }
        return s;
    }

    private WisdomTemple() {}
}
