package com.kiyamet.element;

import com.kiyamet.boss.BossEntity;
import com.kiyamet.boss.BossKind;
import com.kiyamet.boss.ModBosses;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds one huge element temple: a five-tier ziggurat above ground with a pillared hall, then three levels underground -
 * the guardian hall, the labyrinth (with the element's hazards), and the round arena of the element lord. Behind the arena
 * lies the sanctum with the element core.
 */
public class TempleBuilder {
    final ServerLevel l;
    final Element e;
    final BlockPos c;
    final Pal p;
    final List<long[]> ops = new ArrayList<>();
    final List<BlockState> states = new ArrayList<>();
    final List<Runnable> after = new ArrayList<>();
    final Random r;
    int gy;

    record Pal(BlockState wall, BlockState accent, BlockState floor, BlockState light, BlockState hazard, BlockState glass) {}

    static BlockState b(String id) { return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)).defaultBlockState(); }

    static Pal pal(Element e) {
        return switch (e) {
            case FLAME -> new Pal(b("red_nether_bricks"), b("nether_bricks"), b("polished_blackstone"), b("shroomlight"), b("magma_block"), b("red_stained_glass"));
            case LIGHTNING -> new Pal(b("cut_sandstone"), b("yellow_glazed_terracotta"), b("smooth_sandstone"), b("glowstone"), b("lightning_rod"), b("yellow_stained_glass"));
            case EARTH -> new Pal(b("mud_bricks"), b("mossy_stone_bricks"), b("packed_mud"), b("ochre_froglight"), b("pointed_dripstone"), b("green_stained_glass"));
            case SHADOW -> new Pal(b("deepslate_bricks"), b("obsidian"), b("polished_deepslate"), b("crying_obsidian"), b("sculk"), b("purple_stained_glass"));
            case SMOKE -> new Pal(b("tuff_bricks"), b("polished_basalt"), b("smooth_basalt"), b("lantern"), b("campfire"), b("gray_stained_glass"));
            case SPIRIT -> new Pal(b("purpur_block"), b("end_stone_bricks"), b("purpur_pillar"), b("end_rod"), b("soul_sand"), b("cyan_stained_glass"));
            case ELECTRIC -> new Pal(b("oxidized_cut_copper"), b("weathered_copper"), b("cut_copper"), b("sea_lantern"), b("lightning_rod"), b("light_blue_stained_glass"));
            case THUNDER -> new Pal(b("deepslate_tiles"), b("blue_glazed_terracotta"), b("polished_deepslate"), b("glowstone"), b("lightning_rod"), b("blue_stained_glass"));
            case ICE -> new Pal(b("packed_ice"), b("blue_ice"), b("snow_block"), b("sea_lantern"), b("powder_snow"), b("light_blue_stained_glass"));
            case FIRE -> new Pal(b("polished_blackstone_bricks"), b("orange_terracotta"), b("blackstone"), b("shroomlight"), b("lava"), b("orange_stained_glass"));
            case WATER -> new Pal(b("prismarine_bricks"), b("dark_prismarine"), b("prismarine"), b("sea_lantern"), b("water"), b("blue_stained_glass"));
            case AIR -> new Pal(b("smooth_quartz"), b("calcite"), b("quartz_bricks"), b("sea_lantern"), b("cobweb"), b("white_stained_glass"));
            case WIND -> new Pal(b("polished_diorite"), b("white_terracotta"), b("smooth_stone"), b("pearlescent_froglight"), b("cobweb"), b("lime_stained_glass"));
            case METAL -> new Pal(b("polished_tuff"), b("chiseled_tuff_bricks"), b("smooth_stone"), b("lantern"), b("iron_bars"), b("light_gray_stained_glass"));
        };
    }

    static BlockState wool(Element e) {
        return b(switch (e) {
            case FLAME, FIRE -> "red_wool"; case LIGHTNING -> "yellow_wool"; case EARTH -> "green_wool"; case SHADOW -> "purple_wool"; case SMOKE -> "gray_wool";
            case SPIRIT -> "cyan_wool"; case ELECTRIC -> "light_blue_wool"; case THUNDER -> "blue_wool"; case ICE -> "white_wool"; case WATER -> "blue_wool";
            case AIR -> "white_wool"; case WIND -> "lime_wool"; case METAL -> "light_gray_wool";
        });
    }

    public TempleBuilder(ServerLevel l, Element e, BlockPos center) {
        this.l = l; this.e = e; this.c = center; this.p = pal(e); this.r = new Random(center.asLong());
    }

    void set(int x, int y, int z, BlockState s) { ops.add(new long[]{c.getX() + x, gy + y, c.getZ() + z}); states.add(s); }
    void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }
    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** spiral stair around a 3x3 core going down 'depth' blocks from floor y (top) */
    void spiral(int cx, int cz, int top, int depth) {
        fill(cx - 2, top - depth + 1, cz - 2, cx + 2, top + 4, cz + 2, AIR);
        fill(cx - 1, top - depth + 1, cz - 1, cx + 1, top + 4, cz + 1, p.accent);
        int[][] ring = new int[16][];
        int i = 0;
        for (int x = -2; x <= 2; x++) ring[i++] = new int[]{x, -2};
        for (int z = -1; z <= 2; z++) ring[i++] = new int[]{2, z};
        for (int x = 1; x >= -2; x--) ring[i++] = new int[]{x, 2};
        for (int z = 1; z >= -1; z--) ring[i++] = new int[]{-2, z};
        for (int k = 1; k <= depth; k++) {
            int[] q = ring[k % 16];
            set(cx + q[0], top - k - 1 + 1, cz + q[1], p.floor);     // step surface at top-k
        }
    }

    Runnable spawnGuard(int x, int y, int z) {
        return () -> {
            Mob g = ElementMobs.GUARDIANS.get(e).create(l, EntitySpawnReason.STRUCTURE);
            if (g == null) return;
            g.snapTo(c.getX() + x + 0.5, gy + y, c.getZ() + z + 0.5, r.nextFloat() * 360, 0);
            g.setPersistenceRequired();
            l.addFreshEntity(g);
        };
    }

    void chest(int x, int y, int z, String loot) {
        set(x, y, z, Blocks.CHEST.defaultBlockState());
        after.add(() -> {
            if (l.getBlockEntity(new BlockPos(c.getX() + x, gy + y, c.getZ() + z)) instanceof ChestBlockEntity ch)
                ch.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(loot)), r.nextLong());
        });
    }

    public Temples.Job job() {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) l.getChunk((c.getX() >> 4) + dx, (c.getZ() >> 4) + dz);
        gy = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
        gy = Math.max(gy, l.getSeaLevel());
        plan();
        return new Temples.Job() {
            int at = 0;
            @Override
            public boolean step(ServerLevel lv, int budget) {
                int end = Math.min(ops.size(), at + budget);
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
                for (; at < end; at++) {
                    long[] o = ops.get(at);
                    m.set((int) o[0], (int) o[1], (int) o[2]);
                    lv.setBlock(m, states.get(at), 2 | 16);
                }
                if (at < ops.size()) return false;
                for (Runnable a : after) a.run();
                return true;
            }
        };
    }

    void plan() {
        // clear the site and lay a solid base all the way down to the arena
        fill(-27, 1, -27, 27, 40, 27, AIR);
        fill(-26, -44, -26, 26, -1, 26, p.wall);
        fill(-27, 0, -27, 27, 0, 27, p.floor);
        // ---- the ziggurat
        fill(-24, 1, -24, 24, 10, 24, p.wall);
        fill(-23, 1, -23, 23, 9, 23, AIR);                                   // the great hall
        for (int t = 1; t <= 4; t++) {
            int h = 24 - 4 * t, y0 = 11 + (t - 1) * 4;
            fill(-h, y0, -h, h, y0 + 3, h, p.wall);
            for (int k = -h; k <= h; k += 2) { set(k, y0 + 3, -h, p.accent); set(k, y0 + 3, h, p.accent); set(-h, y0 + 3, k, p.accent); set(h, y0 + 3, k, p.accent); }
        }
        for (int k = -24; k <= 24; k += 2) { set(k, 10, -24, p.accent); set(k, 10, 24, p.accent); set(-24, 10, k, p.accent); set(24, 10, k, p.accent); }
        // shrine on top
        for (int[] q : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) fill(q[0], 27, q[1], q[0], 31, q[1], p.accent);
        fill(-3, 32, -3, 3, 32, 3, p.wall);
        fill(-1, 27, -1, 1, 27, 1, p.glass);
        set(0, 28, 0, p.light); set(0, 33, 0, p.light); set(0, 34, 0, p.accent);
        // the south gate with columns and steps
        fill(-3, 1, 24, 3, 7, 24, AIR);
        for (int x : new int[]{-5, 5}) fill(x, 1, 24, x, 9, 25, p.accent);
        for (int x : new int[]{-4, 4}) { fill(x, 1, 25, x, 9, 25, p.accent); set(x, 10, 25, p.light); }
        for (int s = 0; s < 4; s++) fill(-4, -s, 25 + s, 4, -s, 25 + s, p.floor);
        // great hall: floor pattern, pillars with lights, the spiral down in the middle
        for (int x = -22; x <= 22; x++) for (int z = -22; z <= 22; z++) if ((Math.abs(x) + Math.abs(z)) % 6 == 0) set(x, 0, z, p.accent);
        for (int x = -18; x <= 18; x += 9) for (int z = -18; z <= 18; z += 9) {
            if (Math.abs(x) < 5 && Math.abs(z) < 5) continue;
            fill(x, 1, z, x, 9, z, p.accent); set(x, 6, z - 1, p.light); set(x, 6, z + 1, p.light);
        }
        for (int x = -20; x <= 20; x += 10) set(x, 9, 0, p.light);
        for (int x : new int[]{-23, 23}) for (int z = -18; z <= 18; z += 6) fill(x, 3, z, x, 6, z, p.glass);
        spiral(0, 0, 0, 13);
        fill(-3, -1, -3, 3, -1, 3, AIR);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) if (Math.abs(x) == 3 || Math.abs(z) == 3) set(x, 1, z, p.accent);
        set(0, 1, 3, AIR); set(1, 1, 3, AIR); set(-1, 1, 3, AIR);
        // ---- level 1: the guardian hall (floor -14)
        fill(-21, -13, -21, 21, -5, 21, AIR);
        fill(-21, -14, -21, 21, -14, 21, p.floor);
        for (int x = -21; x <= 21; x++) for (int z = -21; z <= 21; z++) if ((x * x + z * z) % 37 == 0) set(x, -14, z, p.accent);
        for (int x = -15; x <= 15; x += 6) for (int z = -15; z <= 15; z += 6) {
            if (Math.abs(x) < 5 && Math.abs(z) < 5) continue;
            if (Math.abs(x - 10) < 4 && Math.abs(z + 10) < 4) continue;
            fill(x, -13, z, x, -5, z, p.accent); set(x, -6, z, p.light);
        }
        for (int[] q : new int[][]{{-17, -17}, {17, 17}, {-17, 17}}) { fill(q[0] - 1, -14, q[1] - 1, q[0] + 1, -14, q[1] + 1, p.hazard.is(Blocks.LAVA) || p.hazard.is(Blocks.WATER) || p.hazard.is(Blocks.POWDER_SNOW) ? p.hazard : p.floor); if (!p.hazard.is(Blocks.LAVA) && !p.hazard.is(Blocks.WATER)) set(q[0], -13, q[1], p.hazard); }
        chest(-20, -13, 0, "kiyamet:chests/ancient_underground_city");
        chest(20, -13, 0, "kiyamet:chests/ancient_underground_city");
        for (int[] q : new int[][]{{-12, 6}, {12, 6}, {-6, -12}, {6, 12}, {-14, -6}, {14, 14}}) after.add(spawnGuard(q[0], -13, q[1]));
        spiral(10, -10, -14, 13);
        // ---- level 2: the labyrinth (floor -27): nine rooms, corridors as a random spanning tree + a few loops
        int[] cs = {-14, 0, 14};
        for (int ix = 0; ix < 3; ix++) for (int iz = 0; iz < 3; iz++) {
            int x = cs[ix], z = cs[iz];
            fill(x - 5, -26, z - 5, x + 5, -20, z + 5, AIR);
            fill(x - 5, -27, z - 5, x + 5, -27, z + 5, p.floor);
            set(x, -20, z, p.light); set(x - 4, -26, z - 4, p.light); set(x + 4, -26, z + 4, p.light);
            if (!((ix == 2 && iz == 0) || (ix == 0 && iz == 2) || (ix == 1 && iz == 1))) after.add(spawnGuard(x + 2, -26, z - 2));
            if ((ix + iz) % 2 == 1) chest(x - 4, -26, z + 4, "kiyamet:chests/ancient_underground_city_treasury");
            // hazards in the room corners
            if (!p.hazard.is(Blocks.LAVA) && !p.hazard.is(Blocks.WATER)) { set(x + 4, -26, z - 4, p.hazard); set(x - 4, -26, z + 3, p.hazard); }
            else { set(x + 4, -27, z - 4, p.hazard); set(x + 3, -27, z - 4, p.hazard); }
        }
        boolean[][] linkH = new boolean[2][3], linkV = new boolean[3][2];
        // spanning tree by random walk
        boolean[][] seen = new boolean[3][3];
        java.util.ArrayDeque<int[]> st = new java.util.ArrayDeque<>();
        st.push(new int[]{0, 0}); seen[0][0] = true;
        while (!st.isEmpty()) {
            int[] cur = st.peek();
            List<int[]> nb = new ArrayList<>();
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) { int nx = cur[0] + d[0], nz = cur[1] + d[1]; if (nx >= 0 && nx < 3 && nz >= 0 && nz < 3 && !seen[nx][nz]) nb.add(new int[]{nx, nz}); }
            if (nb.isEmpty()) { st.pop(); continue; }
            int[] n = nb.get(r.nextInt(nb.size()));
            if (n[0] != cur[0]) linkH[Math.min(n[0], cur[0])][cur[1]] = true; else linkV[cur[0]][Math.min(n[1], cur[1])] = true;
            seen[n[0]][n[1]] = true; st.push(n);
        }
        linkH[r.nextInt(2)][r.nextInt(3)] = true;
        for (int ix = 0; ix < 2; ix++) for (int iz = 0; iz < 3; iz++) if (linkH[ix][iz]) { fill(cs[ix] + 5, -26, cs[iz] - 1, cs[ix + 1] - 5, -23, cs[iz] + 1, AIR); fill(cs[ix] + 5, -27, cs[iz] - 1, cs[ix + 1] - 5, -27, cs[iz] + 1, p.floor); }
        for (int ix = 0; ix < 3; ix++) for (int iz = 0; iz < 2; iz++) if (linkV[ix][iz]) { fill(cs[ix] - 1, -26, cs[iz] + 5, cs[ix] + 1, -23, cs[iz + 1] - 5, AIR); fill(cs[ix] - 1, -27, cs[iz] + 5, cs[ix] + 1, -27, cs[iz + 1] - 5, p.floor); }
        spiral(-10, 10, -27, 14);
        // ---- level 3: the lord's arena (floor -41), round, tall, pillared
        for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d <= 15.5) { fill(x, -40, z, x, -30, z, AIR); set(x, -41, z, Math.abs(d - 10) < 0.6 || Math.abs(d - 5) < 0.6 ? p.accent : p.floor); }
        }
        for (int a = 0; a < 360; a += 45) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 13), z = (int) Math.round(Math.sin(Math.toRadians(a)) * 13);
            if (Math.abs(x + 10) < 4 && Math.abs(z - 10) < 4) continue;
            fill(x, -40, z, x, -30, z, p.accent); set(x, -34, z, p.light);
        }
        set(0, -30, 0, p.light); set(5, -30, 5, p.light); set(-5, -30, -5, p.light); set(5, -30, -5, p.light); set(-5, -30, 5, p.light);
        // the sanctum behind the arena (north)
        fill(-2, -40, -21, 2, -37, -15, AIR);
        fill(-5, -40, -25, 5, -35, -21, AIR);
        fill(-5, -41, -25, 5, -41, -21, p.accent);
        set(-4, -38, -24, p.light); set(4, -38, -24, p.light);
        fill(-1, -40, -24, 1, -40, -22, p.accent);
        set(0, -39, -23, Blocks.CHEST.defaultBlockState());
        set(-5, -40, -25, p.glass); set(5, -40, -25, p.glass);
        after.add(() -> {
            if (l.getBlockEntity(new BlockPos(c.getX(), gy - 39, c.getZ() - 23)) instanceof ChestBlockEntity ch) {
                ch.setItem(13, new ItemStack(ElementItems.CORES.get(e)));
                ch.setItem(11, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 8));
                ch.setItem(15, new ItemStack(com.kiyamet.registry.ModItems.get("ruby"), 16));
            }
        });
        after.add(spawnGuard(-8, -40, 8));
        after.add(spawnGuard(8, -40, 8));
        after.add(() -> {
            BossKind k = BossKind.valueOf(e.name() + "_LORD");
            BossEntity boss = ModBosses.TYPES.get(k).create(l, EntitySpawnReason.STRUCTURE);
            if (boss == null) return;
            boss.snapTo(c.getX() + 0.5, gy - 40, c.getZ() - 4.5, 0, 0);
            boss.setPersistenceRequired();
            l.addFreshEntity(boss);
        });
    }
}
