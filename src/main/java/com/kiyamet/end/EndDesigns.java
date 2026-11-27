package com.kiyamet.end;

import com.kiyamet.boss.BossKind;
import com.kiyamet.entity.ModEntities;
import com.kiyamet.entity.ZombieKind;
import net.minecraft.world.level.block.state.BlockState;

import static com.kiyamet.end.Builder.AIR;
import static com.kiyamet.end.Builder.b;

/** The five great End structures of TWD 2.2. Every one floats in the void, and every one is big. */
public final class EndDesigns {
    static final String RUIN = "kiyamet:chests/end_ruin", VAULT = "kiyamet:chests/end_vault";
    static BlockState ES, ESB, PUR, PIL, OBS, CRY, ROD, MAG, PGL, BLK, PCON, CHF, AME, AMC, QZ, SQZ, WCON, LBG, COP, CUTC, FROG, VFROG, MWOOL, PWOOL, DOAK, DLOG, BARS, GLAZED, BGLASS, PBB, BARREL, CALC;

    static void init() {
        if (ES != null) return;
        ES = b("end_stone"); ESB = b("end_stone_bricks"); PUR = b("purpur_block"); PIL = b("purpur_pillar"); OBS = b("obsidian"); CRY = b("crying_obsidian");
        ROD = b("end_rod"); MAG = b("magenta_stained_glass"); PGL = b("purple_stained_glass"); BLK = b("black_concrete"); PCON = b("purple_concrete");
        CHF = b("chorus_flower"); AME = b("amethyst_block"); AMC = b("amethyst_cluster"); QZ = b("quartz_bricks"); SQZ = b("smooth_quartz"); WCON = b("white_concrete");
        LBG = b("light_blue_stained_glass"); COP = b("waxed_oxidized_copper"); CUTC = b("waxed_oxidized_cut_copper"); FROG = b("pearlescent_froglight");
        VFROG = b("verdant_froglight"); MWOOL = b("magenta_wool"); PWOOL = b("purple_wool"); DOAK = b("dark_oak_planks"); DLOG = b("dark_oak_log");
        BARS = b("iron_bars"); GLAZED = b("magenta_glazed_terracotta"); BGLASS = b("black_stained_glass"); PBB = b("polished_blackstone_bricks");
        BARREL = b("barrel"); CALC = b("calcite");
    }

    static void chorus(Builder s, int x, int y, int z) { s.set(x, y, z, ES); s.set(x, y + 1, z, CHF); }
    static void ze(Builder s, int x, int y, int z) { s.mob(x, y, z, EndMobs.ZOMBIE_ENDERMAN); }
    static void walker(Builder s, int x, int y, int z) { s.mob(x, y, z, ModEntities.ZOMBIES.get(ZombieKind.ARMORED)); }

    // ------------------------------------------------------------------ 1. the Void Citadel
    static void citadel(Builder s) {
        s.island(0, 0, 0, 36, 24, ES, ES);
        for (int i = 0; i < 40; i++) { int x = s.r.nextInt(60) - 30, z = s.r.nextInt(60) - 30; if (x * x + z * z < 900) s.set(x, -2 - s.r.nextInt(8), z, s.r.nextBoolean() ? OBS : CRY); }
        s.fill(-24, 0, -24, 24, 0, 24, ESB);
        for (int k = -24; k <= 24; k += 6) { s.fill(k, 0, -24, k, 0, 24, PUR); s.fill(-24, 0, k, 24, 0, k, PUR); }
        // outer walls with bands, slit windows and battlements
        s.walls(-24, 1, -24, 24, 12, 24, PUR);
        s.walls(-24, 1, -24, 24, 1, 24, OBS);
        s.walls(-24, 12, -24, 24, 12, 24, OBS);
        for (int k = -24; k <= 24; k += 2) for (int[] q : new int[][]{{k, -24}, {k, 24}, {-24, k}, {24, k}}) s.set(q[0], 13, q[1], PIL);
        for (int k = -20; k <= 20; k += 6) for (int y = 6; y <= 9; y++) { s.set(k, y, -24, MAG); s.set(k, y, 24, MAG); s.set(-24, y, k, MAG); s.set(24, y, k, MAG); }
        s.walls(-23, 12, -23, 23, 12, 23, ESB); s.walls(-22, 12, -22, 22, 12, 22, ESB);              // the wall walk
        // the gate
        s.fill(-3, 1, 24, 3, 8, 24, AIR);
        for (int x : new int[]{-5, 5}) { s.fill(x, 1, 23, x, 16, 26, OBS); s.set(x, 17, 24, FROG); s.set(x, 18, 24, ROD); }
        s.fill(-4, 9, 24, 4, 10, 24, OBS); s.fill(-2, 9, 25, 2, 9, 25, CRY);
        // four corner towers
        for (int[] c : new int[][]{{-24, -24}, {24, -24}, {-24, 24}, {24, 24}}) {
            s.cyl(c[0], 1, c[1], 5.5, 26, PUR, false);
            s.cyl(c[0], 2, c[1], 4.3, 24, AIR, false);
            for (int y = 1; y <= 26; y += 6) s.ring(c[0], y, c[1], 5.5, 4.4, OBS);
            for (int y = 2; y <= 25; y++) { double a = y * 0.5; s.set(c[0] + (int) Math.round(Math.cos(a) * 3), y, c[1] + (int) Math.round(Math.sin(a) * 3), PIL); }   // a spiral of steps
            s.disc(c[0], 13, c[1], 4.3, ESB); s.set(c[0] + 3, 13, c[1], AIR); s.set(c[0] + 3, 13, c[1] + 1, AIR);
            s.disc(c[0], 26, c[1], 5.5, ESB);
            for (int h = 0; h < 9; h++) s.disc(c[0], 27 + h, c[1], 5.5 - h * 0.62, h % 3 == 2 ? OBS : PIL);
            s.set(c[0], 36, c[1], FROG); s.set(c[0], 37, c[1], ROD);
            for (int y : new int[]{8, 18}) { s.set(c[0] + 5, y, c[1], MAG); s.set(c[0] - 5, y, c[1], MAG); s.set(c[0], y, c[1] + 5, MAG); s.set(c[0], y, c[1] - 5, MAG); }
            s.chest(c[0], 14, c[1] - 2, RUIN);
            ze(s, c[0] + 1, 14, c[1] + 1);
            s.fill(c[0] - (c[0] > 0 ? 6 : -6), 12, c[1], c[0] - (c[0] > 0 ? 4 : -4), 14, c[1], AIR);             // door onto the wall walk
        }
        // the keep
        s.fill(-11, 1, -18, 11, 25, 4, OBS);
        s.fill(-10, 1, -17, 10, 11, 3, AIR);
        s.fill(-10, 13, -17, 10, 24, 3, AIR);
        s.fill(-10, 12, -17, 10, 12, 3, PUR);
        for (int y = 2; y <= 24; y += 4) for (int x = -11; x <= 11; x += 4) { s.set(x, y, 4, CRY); s.set(x, y, -18, CRY); }
        for (int z = -16; z <= 2; z += 4) for (int y = 5; y <= 8; y++) { s.set(-11, y, z, PGL); s.set(11, y, z, PGL); }
        s.fill(-2, 1, 4, 2, 7, 4, AIR);
        s.fill(-1, 0, -16, 1, 0, 3, PCON);                                                             // the long carpet to the throne
        for (int x : new int[]{-7, 7}) for (int z = -14; z <= 0; z += 5) { s.fill(x, 1, z, x, 11, z, PIL); s.set(x, 10, z + (z < 0 ? 1 : -1), FROG); }
        s.fill(-3, 1, -17, 3, 2, -15, PUR); s.fill(-1, 3, -17, 1, 6, -17, PIL); s.set(0, 7, -17, CRY); s.set(0, 3, -16, GLAZED);   // the throne
        s.chest(-4, 1, -16, VAULT); s.chest(4, 1, -16, VAULT);
        s.set(10, 11, 2, AIR); s.fill(9, 1, 2, 9, 11, 2, PIL);                                          // a pillar to climb, a hole to the upper hall
        s.chest(-9, 13, -16, RUIN);
        ze(s, -5, 13, -8); ze(s, 5, 13, -4);
        // the black spire on the keep
        s.cyl(0, 26, -7, 3.5, 16, PUR, true);
        for (int y = 26; y < 42; y += 4) s.ring(0, y, -7, 3.5, 2.3, OBS);
        for (int h = 0; h < 6; h++) s.disc(0, 42 + h, -7, 3.5 - h * 0.6, CRY);
        s.set(0, 48, -7, FROG); s.set(0, 49, -7, ROD); s.set(0, 50, -7, ROD);
        // courtyard: chorus gardens and lights
        for (int i = 0; i < 26; i++) { int x = s.r.nextInt(40) - 20, z = s.r.nextInt(18) + 6; chorus(s, x, 0, z); }
        for (int[] q : new int[][]{{-16, 10}, {16, 10}, {-16, -10}, {16, -10}, {-16, 18}, {16, 18}}) { s.fill(q[0], 1, q[1], q[0], 4, q[1], PIL); s.set(q[0], 5, q[1], FROG); s.set(q[0], 6, q[1], ROD); }
        // floating debris around
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60 + 20);
            int x = (int) (Math.cos(a) * 48), z = (int) (Math.sin(a) * 48), y = -4 + s.r.nextInt(12);
            s.island(x, y, z, 4 + s.r.nextInt(3), 6, ES, ES);
            if (i % 2 == 0) { s.chest(x, y + 1, z, RUIN); ze(s, x + 1, y + 1, z); }
            else { s.fill(x, y + 1, z, x, y + 4, z, OBS); s.set(x, y + 5, z, ROD); }
        }
        for (int i = 0; i < 10; i++) ze(s, s.r.nextInt(36) - 18, 1, s.r.nextInt(16) + 6);
        for (int i = 0; i < 4; i++) walker(s, s.r.nextInt(30) - 15, 1, s.r.nextInt(14) + 7);
        s.boss(0, 1, -10, BossKind.VOID_TYRANT);
    }

    // ------------------------------------------------------------------ 2. the Sky Spire
    static void spire(Builder s) {
        int H = 96;
        s.island(0, 0, 0, 17, 20, ES, ES);
        s.disc(0, 0, 0, 13, ESB);
        s.cyl(0, 1, 0, 8, H, ESB, true);
        s.cyl(0, 1, 0, 1.5, H, PIL, false);                                                           // the core
        for (int y = 12; y < H; y += 12) { s.ring(0, y, 0, 8.6, 7.4, PUR); s.ring(0, y + 1, 0, 8.6, 7.4, OBS); }
        for (int y = 4; y < H; y += 12) for (int k = 0; k < 4; k++) {                                   // windows
            int dx = k == 0 ? 8 : k == 1 ? -8 : 0, dz = k == 2 ? 8 : k == 3 ? -8 : 0;
            for (int h = 0; h < 4; h++) for (int w = -1; w <= 1; w++) s.set(dx == 0 ? w : dx, y + h, dz == 0 ? w : dz, MAG);
        }
        // the spiral stair
        for (int y = 1; y < H; y++) {
            double a = Math.toRadians(y * 22.5);
            for (double rr = 2.4; rr <= 6.6; rr += 0.7) s.set((int) Math.round(Math.cos(a) * rr), y, (int) Math.round(Math.sin(a) * rr), PUR);
        }
        // floors every 16 blocks, open where the stair passes
        for (int y = 16; y < H; y += 16) {
            for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > 7.2 || d < 2.0) continue;
                double ang = Math.toDegrees(Math.atan2(z, x)), sa = (y * 22.5) % 360;
                double diff = Math.abs(((ang - sa) % 360 + 540) % 360 - 180);
                if (diff < 60) continue;
                s.set(x, y, z, ESB);
            }
            s.set(5, y + 1, 0, FROG);
            s.chest(-5, y + 1, 1, RUIN);
            ze(s, 0, y + 1, -5); if (y % 32 == 0) walker(s, -4, y + 1, -3);
        }
        // the crown
        s.disc(0, H, 0, 12, PUR);
        s.ring(0, H, 0, 12, 10.5, OBS);
        for (int a = 0; a < 360; a += 15) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 11.5), z = (int) Math.round(Math.sin(Math.toRadians(a)) * 11.5);
            s.fill(x, H + 1, z, x, H + (a % 30 == 0 ? 3 : 1), z, PIL);
        }
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + k * 90);
            int x = (int) Math.round(Math.cos(a) * 8), z = (int) Math.round(Math.sin(a) * 8);
            s.fill(x, H + 1, z, x, H + 9, z, OBS); s.set(x, H + 10, z, FROG); s.set(x, H + 11, z, ROD);
        }
        for (int y = H + 1; y < H + 14; y++) s.set(0, y, 0, y % 3 == 0 ? FROG : PGL);
        s.set(0, H + 14, 0, ROD);
        s.set(0, H, 3, AIR); s.set(1, H, 3, AIR); s.set(0, H, 4, AIR); s.set(1, H, 4, AIR);           // the stair comes out here
        s.boss(0, H + 1, -5, BossKind.SKY_HERALD);
        ze(s, 6, H + 1, 2); ze(s, -6, H + 1, 2);
        // four sky platforms, each lower than the last, joined to the spire by bridges
        int[] hs = {20, 36, 52, 68};
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + k * 90);
            int px = (int) Math.round(Math.cos(a) * 32), pz = (int) Math.round(Math.sin(a) * 32), y = hs[k];
            s.island(px, y, pz, 7, 7, PUR, ES);
            for (int[] q : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) { s.fill(px + q[0], y + 1, pz + q[1], px + q[0], y + 5, pz + q[1], PIL); s.set(px + q[0], y + 6, pz + q[1], FROG); }
            s.disc(px, y + 6, pz, 4.4, PUR); s.disc(px, y + 7, pz, 3, OBS); s.set(px, y + 8, pz, ROD);
            s.chest(px, y + 1, pz, k == 3 ? VAULT : RUIN);
            ze(s, px + 2, y + 1, pz); ze(s, px - 2, y + 1, pz - 1);
            // bridge: from the spire wall to the platform
            for (double t = 8; t <= 25; t += 0.5) {
                int bx = (int) Math.round(Math.cos(a) * t), bz = (int) Math.round(Math.sin(a) * t);
                s.set(bx, y, bz, PUR); s.set(bx + 1, y, bz, PUR); s.set(bx, y, bz + 1, PUR);
                if (((int) t) % 4 == 0) { s.set(bx + (Math.cos(a) > 0 ? -1 : 2), y + 1, bz, ROD); }
            }
            int dx = (int) Math.round(Math.cos(a) * 8), dz = (int) Math.round(Math.sin(a) * 8);
            s.fill(dx - 1, y + 1, dz - 1, dx + 1, y + 3, dz + 1, AIR);                                  // door in the wall
            s.set(dx, y, dz, PUR);
        }
    }

    // ------------------------------------------------------------------ 3. the Fallen Sky Ship
    static int shipDrop(int x) { return x < -4 ? (int) Math.floor((x + 4) * 0.32) : x > 22 ? (x - 22) / 4 : 0; }
    static double shipHalf(int x) { return x > 18 ? Math.max(1.2, 7 - (x - 18) * 0.34) : x < -26 ? 6 : 7; }

    static void ship(Builder s) {
        s.island(-6, -2, 0, 24, 18, ES, ES);
        for (int i = 0; i < 18; i++) chorus(s, s.r.nextInt(40) - 26, -2, s.r.nextInt(36) - 18);
        for (int x = -38; x <= 38; x++) {
            if (x >= -4 && x <= -1) continue;                                                           // the break
            int yo = shipDrop(x);
            double hw = shipHalf(x);
            for (int y = 1; y <= 9; y++) {
                double wy = hw * (0.45 + 0.55 * (y - 1) / 8.0);
                int W = (int) Math.round(wy);
                for (int z = -W; z <= W; z++) {
                    boolean shell = Math.abs(z) == W || y == 1;
                    if (shell) s.set(x, y + yo, z, y == 6 ? PUR : y == 1 ? PBB : DOAK);
                    else s.set(x, y + yo, z, AIR);
                }
            }
            int W = (int) Math.round(hw);
            for (int z = -W; z <= W; z++) s.set(x, 9 + yo, z, (x + z) % 7 == 0 ? DLOG : DOAK);                // the deck
            s.set(x, 10 + yo, -W, BARS); s.set(x, 10 + yo, W, BARS);
            if (x % 6 == 0) { s.set(x, 4 + yo, -W, MAG); s.set(x, 4 + yo, W, MAG); }                       // portholes
            if (x == -5 || x == 0) for (int z = -W; z <= W; z++) for (int y = 1; y <= 10; y++) if (s.r.nextFloat() < 0.4f) s.set(x, y + yo, z, AIR);   // torn edges
        }
        // splinters in the gap
        for (int i = 0; i < 20; i++) s.set(-4 + s.r.nextInt(4), 1 + s.r.nextInt(8) - s.r.nextInt(3), s.r.nextInt(13) - 6, s.r.nextBoolean() ? DOAK : DLOG);
        // the bow: a dragon head of purpur and obsidian
        int by = shipDrop(38);
        s.fill(38, 7 + by, -1, 42, 9 + by, 1, PUR); s.fill(42, 8 + by, -1, 44, 9 + by, 1, OBS);
        s.set(43, 10 + by, -1, MAG); s.set(43, 10 + by, 1, MAG); s.fill(39, 10 + by, 0, 41, 12 + by, 0, OBS);
        // stern castle with the captain's cabin
        int sy = shipDrop(-30);
        s.fill(-38, 10 + sy, -6, -24, 17 + sy, 6, PUR);
        s.fill(-37, 10 + sy, -5, -25, 16 + sy, 5, AIR);
        s.fill(-38, 17 + sy, -6, -24, 17 + sy, 6, OBS);
        for (int x = -36; x <= -26; x += 3) { s.set(x, 13 + sy, -6, MAG); s.set(x, 13 + sy, 6, MAG); s.set(x, 14 + sy, -6, MAG); s.set(x, 14 + sy, 6, MAG); }
        s.fill(-24, 10 + sy, -1, -24, 13 + sy, 1, AIR);
        s.set(-36, 10 + sy, 0, GLAZED); s.set(-36, 11 + sy, 0, PIL);
        s.set(-31, 16 + sy, 0, FROG); s.set(-28, 16 + sy, 3, FROG);
        s.chest(-36, 10 + sy, -4, VAULT); s.chest(-36, 10 + sy, 4, RUIN);
        s.boss(-30, 10 + sy, 0, BossKind.DEAD_CAPTAIN);
        // masts and torn sails
        int my = shipDrop(10);
        s.fill(10, 10 + my, 0, 10, 34 + my, 0, DLOG);
        for (int[] yard : new int[][]{{18, 9}, {28, 7}}) {
            s.fill(10, yard[0] + my, -yard[1], 10, yard[0] + my, yard[1], DLOG);
            for (int y = yard[0] - 7; y < yard[0]; y++) for (int z = -yard[1] + 1; z < yard[1]; z++) if (s.r.nextFloat() > 0.22f) s.set(11, y + my, z, (y + z) % 5 == 0 ? PWOOL : MWOOL);
        }
        s.set(10, 35 + my, 0, ROD);
        int m2 = shipDrop(-14);
        s.fill(-14, 10 + m2, 0, -14, 20 + m2, 0, DLOG);
        for (int k = 1; k < 6; k++) s.set(-14 - k, 20 + m2 - k / 2, k / 3, DLOG);                         // the snapped top
        // cargo in the hold
        for (int x = 6; x <= 30; x += 8) { s.chest(x, 2, -3, RUIN); s.set(x + 1, 2, 3, BARREL); s.set(x + 2, 2, 3, BARREL); s.set(x + 1, 3, 3, BARREL); }
        s.set(14, 9, 0, AIR); s.set(15, 9, 0, AIR); s.fill(14, 2, 0, 14, 8, 0, DLOG);                    // a hatch and a post to climb
        s.set(20, 5, 0, FROG); s.set(-15, 5 + m2, 0, FROG);
        // the crew
        for (int i = 0; i < 8; i++) ze(s, s.r.nextInt(50) - 20, 10 + shipDrop(5), s.r.nextInt(7) - 3);
        for (int i = 0; i < 4; i++) walker(s, 8 + s.r.nextInt(20), 2, s.r.nextInt(5) - 2);
        // debris on the rock
        for (int i = 0; i < 30; i++) s.set(s.r.nextInt(44) - 28, -1, s.r.nextInt(36) - 18, s.r.nextFloat() < 0.5 ? DOAK : PUR);
    }

    // ------------------------------------------------------------------ 4. the Ender Hive
    static void hive(Builder s) {
        int C = 26;
        s.sphere(0, C, 0, 27, ES, 3.2);
        for (int i = 0; i < 46; i++) {                                                                  // warts and lumps
            double th = s.r.nextDouble() * Math.PI * 2, ph = Math.acos(2 * s.r.nextDouble() - 1);
            int x = (int) (Math.sin(ph) * Math.cos(th) * 26), y = (int) (Math.cos(ph) * 26), z = (int) (Math.sin(ph) * Math.sin(th) * 26);
            s.sphere(x, C + y, z, 2 + s.r.nextInt(3), s.r.nextInt(4) == 0 ? OBS : ES, 0);
        }
        // the inner hive wall: cells of purple, black and light
        for (int x = -24; x <= 24; x++) for (int y = -24; y <= 24; y++) for (int z = -24; z <= 24; z++) {
            double d = Math.sqrt(x * x + y * y + z * z);
            if (d < 22.8 || d > 23.8) continue;
            int h = Math.floorMod(x * 7 + y * 13 + z * 5, 11);
            s.set(x, C + y, z, h == 0 ? FROG : h < 4 ? BLK : h < 6 ? CRY : PCON);
        }
        // six ways in
        for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {1, 1, 1}, {-1, 1, -1}}) {
            double n = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            for (double t = 20; t <= 30; t += 1) s.sphere((int) (d[0] / n * t), C + (int) (d[1] / n * t * 0.6), (int) (d[2] / n * t), 3, AIR, 0);
        }
        // floor of the nest and ledges inside
        s.disc(0, C - 20, 0, 15, ES);
        for (int[] lv : new int[][]{{-10, 20}, {0, 22}, {10, 19}}) {
            int y = C + lv[0];
            for (int x = -lv[1]; x <= lv[1]; x++) for (int z = -lv[1]; z <= lv[1]; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > lv[1] || d < lv[1] - 6) continue;
                double a = Math.toDegrees(Math.atan2(z, x)) + 180;
                if (((int) a / 30) % 3 == 2) continue;                                                // gaps
                s.set(x, y, z, (x + z) % 5 == 0 ? OBS : ESB);
            }
        }
        // the central column with steps around it
        s.cyl(0, C - 19, 0, 3.5, 38, OBS, false);
        for (int y = C - 19; y < C + 18; y++) {
            double a = Math.toRadians(y * 24);
            for (double rr = 4; rr <= 6.5; rr += 0.8) s.set((int) Math.round(Math.cos(a) * rr), y, (int) Math.round(Math.sin(a) * rr), ESB);
        }
        for (int y = C - 18; y < C + 18; y += 5) for (int k = 0; k < 4; k++) s.set(k == 0 ? 4 : k == 1 ? -4 : 0, y, k == 2 ? 4 : k == 3 ? -4 : 0, FROG);
        // eggs of the nest
        for (int i = 0; i < 14; i++) {
            double a = Math.toRadians(i * 360.0 / 14);
            int x = (int) (Math.cos(a) * 10), z = (int) (Math.sin(a) * 10);
            s.sphere(x, C - 18, z, 1.6, i % 3 == 0 ? GLAZED : i % 3 == 1 ? MAG : AME, 0);
        }
        for (int i = 0; i < 20; i++) { int x = s.r.nextInt(26) - 13, z = s.r.nextInt(26) - 13; if (x * x + z * z > 30) chorus(s, x, C - 20, z); }
        s.chest(0, C - 19, 8, VAULT);
        s.boss(0, C - 19, -8, BossKind.HIVE_MATRIARCH);
        for (int[] q : new int[][]{{18, -10, 4}, {-18, 0, 6}, {5, 10, -16}, {-6, 0, -20}}) s.chest(q[0], C + q[1] + 1, q[2], RUIN);
        for (int i = 0; i < 16; i++) {
            int[] lv = new int[][]{{-10, 17}, {0, 19}, {10, 16}}[i % 3];
            double a = Math.toRadians(s.r.nextInt(360));
            ze(s, (int) (Math.cos(a) * lv[1]), C + lv[0] + 1, (int) (Math.sin(a) * lv[1]));
        }
        for (int i = 0; i < 4; i++) ze(s, s.r.nextInt(16) - 8, C - 19, s.r.nextInt(16) - 8);
    }

    // ------------------------------------------------------------------ 5. the Starfall Observatory
    static void observatory(Builder s) {
        s.island(0, 0, 0, 17, 16, SQZ, ES);
        // the outer ring, floating, with its own rim of light
        s.ring(0, 0, 0, 34, 27.5, QZ);
        s.ring(0, -1, 0, 33, 28.5, ESB);
        s.ring(0, -2, 0, 31.5, 29.5, ESB);
        for (int a = 0; a < 360; a += 10) { int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 30.7), z = (int) Math.round(Math.sin(Math.toRadians(a)) * 30.7); s.set(x, 0, z, a % 30 == 0 ? FROG : SQZ); }
        // four spokes
        for (int k = 0; k < 4; k++) for (int t = 15; t <= 28; t++) for (int w = -1; w <= 1; w++) {
            int x = k == 0 ? t : k == 1 ? -t : w, z = k == 2 ? t : k == 3 ? -t : w;
            s.set(x, 0, z, w == 0 ? SQZ : ESB);
            if (w != 0 && t % 4 == 0) s.set(x, 1, z, BARS);
        }
        // eight pylons on the ring
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45);
            int x = (int) Math.round(Math.cos(a) * 31), z = (int) Math.round(Math.sin(a) * 31);
            s.fill(x, 1, z, x, 6, z, PIL); s.set(x, 7, z, FROG); s.set(x, 8, z, ROD);
        }
        // fallen stars: craters full of amethyst
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(k * 60 + 8);
            int x = (int) Math.round(Math.cos(a) * 30.5), z = (int) Math.round(Math.sin(a) * 30.5);
            s.disc(x, 0, z, 2.2, CRY); s.set(x, 0, z, AME); s.set(x, 1, z, AME); s.set(x, 2, z, AMC);
            s.set(x + 1, 1, z, AMC); s.set(x - 1, 1, z, AMC);
            if (k % 2 == 0) s.chest(x, 1, z + 2, RUIN);
        }
        // the dome
        s.disc(0, 0, 0, 13, SQZ);
        for (int a = 0; a < 360; a += 30) for (int rr = 3; rr <= 11; rr += 4) s.set((int) Math.round(Math.cos(Math.toRadians(a + rr * 7)) * rr), 0, (int) Math.round(Math.sin(Math.toRadians(a + rr * 7)) * rr), FROG);   // a star map in the floor
        s.cyl(0, 1, 0, 12.5, 8, WCON, true);
        for (int a = 0; a < 360; a += 30) for (int y = 3; y <= 6; y++) s.set((int) Math.round(Math.cos(Math.toRadians(a)) * 12), y, (int) Math.round(Math.sin(Math.toRadians(a)) * 12), LBG);
        for (int x = -13; x <= 13; x++) for (int y = 0; y <= 13; y++) for (int z = -13; z <= 13; z++) {
            double d = Math.sqrt(x * x + y * y + z * z);
            if (d > 12.5 || d < 11.4) continue;
            s.set(x, 9 + y, z, Math.abs(z) <= 1 && x > -2 ? AIR : (y + x * 3 + z * 5) % 13 == 0 ? LBG : WCON);
        }
        s.fill(-2, 1, 12, 2, 5, 13, AIR);                                                               // the door
        s.fill(-2, 6, 13, 2, 6, 13, CALC);
        // the great telescope, aimed at the sky through the slot
        s.cyl(0, 1, 0, 3, 4, CUTC, false);
        double ang = Math.toRadians(38);
        for (double t = 0; t <= 34; t += 0.5) {
            double cx = Math.cos(ang) * t, cy = 6 + Math.sin(ang) * t;
            double rad = t > 30 ? 3.0 : t > 14 ? 2.6 : 2.2;
            for (int dy = -3; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
                if (dy * dy + dz * dz > rad * rad) continue;
                s.set((int) Math.round(cx - Math.sin(ang) * dy * 0.3), (int) Math.round(cy + dy), dz, t > 33 ? LBG : t > 30 ? CUTC : (int) (t * 2) % 9 == 0 ? CUTC : COP);
            }
        }
        s.chest(-6, 1, -6, VAULT); s.chest(6, 1, -6, RUIN);
        s.set(-9, 1, 0, FROG); s.set(-9, 2, 0, ROD);
        s.boss(0, 1, 7, BossKind.STAR_WARDEN);
        ze(s, -6, 1, 4); ze(s, 6, 1, 4);
        for (int i = 0; i < 8; i++) { double a = Math.toRadians(i * 45 + 10); ze(s, (int) (Math.cos(a) * 30.5), 1, (int) (Math.sin(a) * 30.5)); }
        // little meteors still drifting overhead
        for (int i = 0; i < 6; i++) s.sphere(s.r.nextInt(60) - 30, 26 + s.r.nextInt(14), s.r.nextInt(60) - 30, 1.5 + s.r.nextInt(2), s.r.nextBoolean() ? AME : CRY, 0);
    }

    public static void plan(Builder s, EndStructures.Type t) {
        init();
        switch (t) {
            case VOID_CITADEL -> citadel(s);
            case SKY_SPIRE -> spire(s);
            case FALLEN_SKYSHIP -> ship(s);
            case ENDER_HIVE -> hive(s);
            case STARFALL_OBSERVATORY -> observatory(s);
        }
    }

    private EndDesigns() {}
}
