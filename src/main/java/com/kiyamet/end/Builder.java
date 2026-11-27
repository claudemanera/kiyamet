package com.kiyamet.end;

import com.kiyamet.boss.BossEntity;
import com.kiyamet.boss.BossKind;
import com.kiyamet.boss.ModBosses;
import com.kiyamet.element.Temples;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Plans a big structure as a list of block changes, then places it a slice per tick. Coordinates are relative to the origin. */
public class Builder {
    public final ServerLevel l;
    public final BlockPos o;
    public final Random r;
    final List<long[]> ops = new ArrayList<>();
    final List<BlockState> states = new ArrayList<>();
    final List<Runnable> after = new ArrayList<>();
    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public Builder(ServerLevel l, BlockPos origin, long seed) { this.l = l; this.o = origin; this.r = new Random(seed); }

    public static BlockState b(String id) { return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)).defaultBlockState(); }

    public void set(int x, int y, int z, BlockState s) { ops.add(new long[]{o.getX() + x, o.getY() + y, o.getZ() + z}); states.add(s); }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    /** a hollow box: walls only */
    public void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
            if (x == x0 || x == x1 || z == z0 || z == z1) set(x, y, z, s);
    }

    public void disc(int cx, int y, int cz, double rad, BlockState s) {
        int R = (int) Math.ceil(rad);
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) if (x * x + z * z <= rad * rad) set(cx + x, y, cz + z, s);
    }

    public void ring(int cx, int y, int cz, double rOut, double rIn, BlockState s) {
        int R = (int) Math.ceil(rOut);
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) { double d = x * x + z * z; if (d <= rOut * rOut && d >= rIn * rIn) set(cx + x, y, cz + z, s); }
    }

    /** a cylinder; hollow keeps only a 1-thick wall */
    public void cyl(int cx, int y0, int cz, double rad, int h, BlockState s, boolean hollow) {
        for (int y = y0; y < y0 + h; y++) { if (hollow) ring(cx, y, cz, rad, rad - 1.2, s); else disc(cx, y, cz, rad, s); }
    }

    /** sphere; shell > 0 makes it hollow with that wall thickness, air inside */
    public void sphere(int cx, int cy, int cz, double rad, BlockState s, double shell) {
        int R = (int) Math.ceil(rad);
        for (int x = -R; x <= R; x++) for (int y = -R; y <= R; y++) for (int z = -R; z <= R; z++) {
            double d = Math.sqrt(x * x + y * y + z * z);
            if (d > rad) continue;
            if (shell > 0 && d < rad - shell) set(cx + x, cy + y, cz + z, AIR);
            else set(cx + x, cy + y, cz + z, s);
        }
    }

    /** a floating rock: flat top at y, an uneven point hanging down */
    public void island(int cx, int y, int cz, double rad, int depth, BlockState top, BlockState under) {
        int R = (int) Math.ceil(rad);
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > rad) continue;
            double k = 1 - d / rad;
            int dn = (int) Math.round(depth * Math.pow(k, 0.8) + Math.sin(x * 0.7 + z * 0.4) * 1.5 + Math.cos(z * 0.9) * 1.2);
            set(cx + x, y, cz + z, top);
            for (int dy = 1; dy <= Math.max(1, dn); dy++) set(cx + x, y - dy, cz + z, under);
        }
    }

    public void chest(int x, int y, int z, String loot) {
        set(x, y, z, Blocks.CHEST.defaultBlockState());
        long seed = r.nextLong();
        after.add(() -> {
            if (l.getBlockEntity(new BlockPos(o.getX() + x, o.getY() + y, o.getZ() + z)) instanceof ChestBlockEntity ch)
                ch.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(loot)), seed);
        });
    }

    public void mob(int x, int y, int z, EntityType<? extends Mob> type) {
        float yaw = r.nextFloat() * 360;
        after.add(() -> {
            Mob m = type.create(l, EntitySpawnReason.STRUCTURE);
            if (m == null) return;
            m.snapTo(o.getX() + x + 0.5, o.getY() + y, o.getZ() + z + 0.5, yaw, 0);
            m.setPersistenceRequired();
            l.addFreshEntity(m);
        });
    }

    public void boss(int x, int y, int z, BossKind k) {
        after.add(() -> {
            BossEntity boss = ModBosses.TYPES.get(k).create(l, EntitySpawnReason.STRUCTURE);
            if (boss == null) return;
            boss.snapTo(o.getX() + x + 0.5, o.getY() + y, o.getZ() + z + 0.5, 0, 0);
            boss.setPersistenceRequired();
            l.addFreshEntity(boss);
        });
    }

    public int size() { return ops.size(); }

    public Temples.Job job() {
        return new Temples.Job() {
            int at = 0;
            @Override
            public boolean step(ServerLevel ignored, int budget) {            // always builds in its own level
                int end = Math.min(ops.size(), at + budget);
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
                for (; at < end; at++) {
                    long[] q = ops.get(at);
                    m.set((int) q[0], (int) q[1], (int) q[2]);
                    if (!l.isInWorldBounds(m)) continue;
                    l.setBlock(m, states.get(at), 2 | 16);
                }
                if (at < ops.size()) return false;
                for (Runnable a : after) a.run();
                return true;
            }
        };
    }

    /** load every chunk the structure touches before building */
    public void loadChunks(int radius) {
        for (int dx = -radius; dx <= radius; dx += 16) for (int dz = -radius; dz <= radius; dz += 16) l.getChunk((o.getX() + dx) >> 4, (o.getZ() + dz) >> 4);
        l.getChunk((o.getX() + radius) >> 4, (o.getZ() + radius) >> 4);
    }
}
