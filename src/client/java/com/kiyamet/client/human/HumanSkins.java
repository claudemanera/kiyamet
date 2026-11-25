package com.kiyamet.client.human;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.Appearance;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Paints a 64x64 Minecraft-layout skin for every {@link Appearance} code and caches it as a dynamic texture.
 * Base layer = body, outer layer (hat / jacket / sleeves / pants) = hair volume, hats, coats and armour.
 */
public final class HumanSkins {
    private static final Map<Integer, Identifier> CACHE = new HashMap<>();

    public static Identifier texture(int code) {
        return CACHE.computeIfAbsent(code, c -> {
            NativeImage img = new NativeImage(64, 64, true);
            new Painter(img, Appearance.unpack(c), new Random(c * 31L + 7)).paint();
            Identifier id = Kiyamet.id("dynamic/human_" + Integer.toHexString(c));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "kiyamet human " + c, img));
            return id;
        });
    }

    // ------------------------------------------------------------------ palette

    static final int[] SKIN = {0xFFF6DDCB, 0xFFF0CDB4, 0xFFE8BC98, 0xFFD9A77F, 0xFFC99068, 0xFFB98458, 0xFFA8714A, 0xFF94603C,
            0xFF7C4E30, 0xFF654027, 0xFF4E311E, 0xFF3A2416};
    static final int[] HAIR = {0xFF1A1512, 0xFF3B2618, 0xFF5A3A22, 0xFF7E5A38, 0xFFD8B65E, 0xFFEDE3C2, 0xFFB5531E, 0xFF7E2E1C,
            0xFF7D7A78, 0xFFE4E2DE, 0xFFB01E2A, 0xFF2A4FB0};
    static final int[] EYES = {0xFF4A2E1A, 0xFF3A72C8, 0xFF3D8A3D, 0xFF7A8088};
    static final int[] CLOTH = {0xFFA82828, 0xFF1F2E58, 0xFF2F5A2C, 0xFF1E1E22, 0xFFE8E8E4, 0xFF7A7C80, 0xFF5E4026, 0xFFC2A878,
            0xFF5C6234, 0xFF6A1E2A, 0xFF1E7A78, 0xFFD06A1E, 0xFF5E2E86, 0xFFD8B42A, 0xFF7AB4E0, 0xFFE08AB0};

    /** A box-UV part: texture origin (u,v) and size (w,h,d). */
    record Part(int u, int v, int w, int h, int d) {}

    static final Part HEAD = new Part(0, 0, 8, 8, 8), HAT = new Part(32, 0, 8, 8, 8);
    static final Part BODY = new Part(16, 16, 8, 12, 4), JACKET = new Part(16, 32, 8, 12, 4);
    static final Part RARM = new Part(40, 16, 4, 12, 4), RSLEEVE = new Part(40, 32, 4, 12, 4);
    static final Part LARM = new Part(32, 48, 4, 12, 4), LSLEEVE = new Part(48, 48, 4, 12, 4);
    static final Part RLEG = new Part(0, 16, 4, 12, 4), RPANTS = new Part(0, 32, 4, 12, 4);
    static final Part LLEG = new Part(16, 48, 4, 12, 4), LPANTS = new Part(0, 48, 4, 12, 4);

    static final class Painter {
        final NativeImage img;
        final Appearance a;
        final Random r;
        final int skin, hair, cloth;

        Painter(NativeImage img, Appearance a, Random r) {
            this.img = img;
            this.a = a;
            this.r = r;
            this.skin = SKIN[Math.min(a.skin(), SKIN.length - 1)];
            this.hair = HAIR[Math.min(a.hairColor(), HAIR.length - 1)];
            this.cloth = CLOTH[Math.min(a.outfitColor(), CLOTH.length - 1)];
        }

        // ---------- colour helpers
        static int shade(int c, int d) {
            int rr = clamp((c >> 16 & 255) + d), gg = clamp((c >> 8 & 255) + d), bb = clamp((c & 255) + d);
            return 0xFF000000 | rr << 16 | gg << 8 | bb;
        }
        static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
        static int mix(int a, int b, float t) {
            int rr = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t), gg = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t), bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
            return 0xFF000000 | rr << 16 | gg << 8 | bb;
        }
        int jit(int c, int n) { return n == 0 ? c : shade(c, r.nextInt(n * 2 + 1) - n); }

        void px(int x, int y, int c) { if (x >= 0 && y >= 0 && x < 64 && y < 64) img.setPixel(x, y, c); }

        // ---------- part helpers (coordinates inside a face: x left->right, y top->bottom)
        void top(Part p, int c, int n) { for (int x = 0; x < p.w; x++) for (int y = 0; y < p.d; y++) px(p.u + p.d + x, p.v + y, jit(c, n)); }
        void bottom(Part p, int c, int n) { for (int x = 0; x < p.w; x++) for (int y = 0; y < p.d; y++) px(p.u + p.d + p.w + x, p.v + y, jit(c, n)); }
        /** Rows y0..y1 on all four side faces. */
        void sides(Part p, int y0, int y1, int c, int n) {
            for (int y = Math.max(0, y0); y <= Math.min(p.h - 1, y1); y++)
                for (int x = 0; x < 2 * p.d + 2 * p.w; x++) px(p.u + x, p.v + p.d + y, jit(c, n));
        }
        void all(Part p, int c, int n) { top(p, c, n); bottom(p, c, n); sides(p, 0, p.h - 1, c, n); }
        void front(Part p, int x, int y, int c) { px(p.u + p.d + x, p.v + p.d + y, c); }
        void back(Part p, int x, int y, int c) { px(p.u + 2 * p.d + p.w + x, p.v + p.d + y, c); }
        void right(Part p, int x, int y, int c) { px(p.u + x, p.v + p.d + y, c); }
        void left(Part p, int x, int y, int c) { px(p.u + p.d + p.w + x, p.v + p.d + y, c); }
        void frontRect(Part p, int x0, int y0, int x1, int y1, int c, int n) { for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) front(p, x, y, jit(c, n)); }
        void backRect(Part p, int x0, int y0, int x1, int y1, int c, int n) { for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) back(p, x, y, jit(c, n)); }
        /** Row band on the left/right faces only (side stripes). */
        void flanks(Part p, int y0, int y1, int c) { for (int y = y0; y <= y1; y++) for (int x = 0; x < p.d; x++) { right(p, x, y, c); left(p, x, y, c); } }

        void arms(int y0, int y1, int c, int n, boolean outer) {
            sides(outer ? RSLEEVE : RARM, y0, y1, c, n); sides(outer ? LSLEEVE : LARM, y0, y1, c, n);
            if (y0 == 0) { top(outer ? RSLEEVE : RARM, c, n); top(outer ? LSLEEVE : LARM, c, n); }
        }
        void legs(int y0, int y1, int c, int n, boolean outer) {
            sides(outer ? RPANTS : RLEG, y0, y1, c, n); sides(outer ? LPANTS : LLEG, y0, y1, c, n);
            if (y1 == 11) { bottom(outer ? RPANTS : RLEG, c, n); bottom(outer ? LPANTS : LLEG, c, n); }
        }
        void torso(int y0, int y1, int c, int n, boolean outer) {
            Part p = outer ? JACKET : BODY;
            sides(p, y0, y1, c, n);
            if (y0 == 0) top(p, c, n);
            if (y1 == 11) bottom(p, c, n);
        }

        // ---------- paint
        void paint() {
            for (Part p : new Part[]{HEAD, BODY, RARM, LARM, RLEG, LLEG}) all(p, skin, 4);
            if (a.mask()) { mask(); outfit(19); return; }
            face();
            outfit(a.outfit());
            detail();
            hairStyle();
            if (a.female()) feminine();
        }

        void face() {
            int eye = EYES[a.eyes()], brow = shade(hair, -10);
            front(HEAD, 1, 4, 0xFFF2F2F2); front(HEAD, 2, 4, eye); front(HEAD, 5, 4, eye); front(HEAD, 6, 4, 0xFFF2F2F2);
            front(HEAD, 1, 3, brow); front(HEAD, 2, 3, brow); front(HEAD, 5, 3, brow); front(HEAD, 6, 3, brow);
            front(HEAD, 3, 5, shade(skin, -28)); front(HEAD, 4, 5, shade(skin, -28));
            int mouth = mix(shade(skin, -45), 0xFF8A3A30, 0.5f);
            front(HEAD, 3, 6, mouth); front(HEAD, 4, 6, mouth);
            switch (a.beard()) {
                case 1 -> { int s = mix(skin, hair, 0.35f); for (int y = 5; y <= 7; y++) for (int x = 0; x < 8; x++) if ((y > 5 || x < 2 || x > 5) && !(y == 6 && (x == 3 || x == 4)) && r.nextFloat() < 0.7f) front(HEAD, x, y, s); }
                case 2 -> {
                    for (int x = 0; x < 8; x++) { front(HEAD, x, 7, jit(hair, 6)); if (x != 3 && x != 4) front(HEAD, x, 6, jit(hair, 6)); }
                    front(HEAD, 0, 5, hair); front(HEAD, 7, 5, hair); front(HEAD, 2, 5, hair); front(HEAD, 5, 5, hair);
                    for (int y = 4; y < 8; y++) { right(HEAD, 6, y, hair); right(HEAD, 7, y, hair); left(HEAD, 0, y, hair); left(HEAD, 1, y, hair); }
                }
                case 3 -> { for (int x = 2; x <= 5; x++) front(HEAD, x, 5, shade(hair, 4)); front(HEAD, 2, 6, hair); front(HEAD, 5, 6, hair); }
                default -> {}
            }
        }

        void feminine() {
            int lash = 0xFF151010;
            front(HEAD, 0, 4, lash); front(HEAD, 7, 4, lash); front(HEAD, 1, 3, lash); front(HEAD, 6, 3, lash);
            front(HEAD, 2, 3, shade(hair, -10)); front(HEAD, 5, 3, shade(hair, -10));
            int lip = mix(skin, 0xFFC0485A, 0.65f);
            front(HEAD, 3, 6, lip); front(HEAD, 4, 6, lip); front(HEAD, 2, 6, mix(skin, lip, 0.4f)); front(HEAD, 5, 6, mix(skin, lip, 0.4f));
            front(HEAD, 3, 5, shade(skin, -14)); front(HEAD, 4, 5, shade(skin, -14));
            front(HEAD, 1, 5, mix(skin, 0xFFE08080, 0.25f)); front(HEAD, 6, 5, mix(skin, 0xFFE08080, 0.25f));
            // a little shape on the shirt
            for (int x = 1; x <= 6; x++) { int c = img.getPixel(BODY.u + BODY.d + x, BODY.v + BODY.d + 3); if ((c >>> 24) != 0) front(BODY, x, 3, shade(c, -18)); }
            front(BODY, 3, 2, shade(img.getPixel(BODY.u + BODY.d + 3, BODY.v + BODY.d + 2), -12)); front(BODY, 4, 2, shade(img.getPixel(BODY.u + BODY.d + 4, BODY.v + BODY.d + 2), -12));
        }

        void detail() {
            switch (a.detail()) {
                case 1 -> { front(HEAD, 6, 2, 0xFF9A3A34); front(HEAD, 5, 3, 0xFFB04A40); front(HEAD, 6, 5, 0xFF9A3A34); front(HEAD, 7, 6, 0xFF9A3A34); }
                case 2 -> { front(HEAD, 5, 4, 0xFF111111); front(HEAD, 6, 4, 0xFF111111); front(HEAD, 5, 5, 0xFF1A1A1A);
                    for (int x = 0; x < 8; x++) if (x < 5 || x > 6) front(HEAD, x, 3, 0xFF2A2A2A);
                    for (int x = 0; x < 8; x++) { right(HEAD, x, 3, 0xFF2A2A2A); left(HEAD, x, 3, 0xFF2A2A2A); back(HEAD, x, 3, 0xFF2A2A2A); } }
                case 3 -> { int g = 0xFF202020; front(HEAD, 0, 4, g); front(HEAD, 3, 4, g); front(HEAD, 4, 4, g); front(HEAD, 7, 4, g);
                    for (int x = 1; x <= 6; x++) if (x != 3 && x != 4) front(HEAD, x, 3, g); }
                case 4 -> { int c = cloth == CLOTH[0] ? CLOTH[1] : CLOTH[0]; for (int y = 5; y < 8; y++) for (int x = 0; x < 8; x++) front(HEAD, x, y, jit(c, 6));
                    for (int y = 5; y < 8; y++) for (int x = 0; x < 8; x++) { right(HEAD, x, y, jit(c, 6)); left(HEAD, x, y, jit(c, 6)); } }
                case 5 -> { int c = 0xFF2A2A2A; front(HEAD, 1, 5, c); front(HEAD, 1, 6, c); front(HEAD, 6, 5, c); front(HEAD, 6, 6, c); front(HEAD, 0, 5, 0xFF8A1E1E); front(HEAD, 7, 5, 0xFF8A1E1E); }
                case 6 -> { for (int x = 0; x < 8; x++) for (int y = 1; y <= 2; y++) { front(HAT, x, y, jit(0xFFE8E4DA, 8)); back(HAT, x, y, jit(0xFFE8E4DA, 8)); right(HAT, x, y, jit(0xFFE8E4DA, 8)); left(HAT, x, y, jit(0xFFE8E4DA, 8)); }
                    front(HAT, 5, 2, 0xFFA03030); }
                case 7 -> { for (int i = 0; i < 6; i++) front(HEAD, r.nextInt(8), 4 + r.nextInt(3), shade(skin, -35)); }
                default -> {}
            }
        }

        // ------------------------------------------------------------------ hair
        void hairStyle() {
            int h = hair, h2 = shade(hair, -18);
            switch (a.hair()) {
                case 0 -> top(HEAD, shade(skin, -6), 3);
                case 1 -> { int c = mix(skin, h, 0.55f); top(HEAD, c, 6); sides(HEAD, 0, 1, c, 6); keepFace(); }
                case 2 -> { shortHair(h, h2); }
                case 3 -> { shortHair(h, h2); for (int x = 0; x < 5; x++) front(HEAD, x, 2, h); front(HEAD, 5, 1, shade(skin, -10)); }
                case 4 -> { shortHair(h, h2); longBack(h, 8); sidesDown(h, 7); hatBack(h, 0, 7); }
                case 5 -> { shortHair(h, h2); backRect(HAT, 3, 2, 4, 7, h, 8); backRect(HEAD, 3, 0, 4, 7, h2, 4); }
                case 6 -> { top(HEAD, shade(skin, -6), 3); sides(HEAD, 0, 0, mix(skin, h, 0.4f), 4); keepFace();
                    for (int y = 0; y < 8; y++) { px(HAT.u + HAT.d + 3, HAT.v + y, jit(h, 10)); px(HAT.u + HAT.d + 4, HAT.v + y, jit(h, 10)); }
                    front(HAT, 3, 0, h); front(HAT, 4, 0, h); for (int y = 0; y < 6; y++) { back(HAT, 3, y, h); back(HAT, 4, y, h); } }
                case 7 -> { shortHair(h, h2); curls(HAT, h, 0, 2); }
                case 8 -> { shortHair(h, h2); for (int x = 2; x <= 5; x++) for (int y = 2; y <= 5; y++) px(HAT.u + HAT.d + x, HAT.v + y, jit(h, 10)); backRect(HAT, 2, 0, 5, 1, h, 8); }
                case 9 -> { shortHair(h, h2); curls(HAT, h, 0, 5); top(HAT, h, 14); hatBack(h, 0, 6); }
                case 10 -> { shortHair(h, h2); longBack(h, 8); sidesDown(h, 6); for (int x = 0; x < 8; x += 2) for (int y = 0; y < 8; y++) back(HAT, x, y, jit(h2, 6)); }
                case 11 -> { top(HEAD, h, 4); for (int x = 0; x < 8; x += 3) for (int y = 0; y < 8; y++) px(HEAD.u + HEAD.d + x, HEAD.v + y, shade(h, 22));
                    sides(HEAD, 0, 1, h, 4); backRect(HEAD, 0, 0, 7, 4, h, 4); keepFace(); for (int x = 0; x < 8; x++) front(HEAD, x, 0, h); }
                case 12 -> { shortHair(h, h2); longBack(h, 8); }
                case 13 -> { shortHair(h, h2); sidesDown(h, 5); backRect(HEAD, 0, 0, 7, 5, h, 6); for (int y = 2; y <= 4; y++) { front(HEAD, 0, y, h); front(HEAD, 7, y, h); } }
                case 14 -> { shortHair(h, h2); int b = cloth == CLOTH[3] ? CLOTH[0] : shade(cloth, -10); top(HAT, b, 8); sides(HAT, 0, 2, b, 8);
                    for (int x = 0; x < 2 * 8 + 2 * 8; x++) px(HAT.u + x, HAT.v + HAT.d + 2, shade(b, -25)); }
                case 15 -> { shortHair(h, h2); int b = cloth; top(HAT, b, 6); sides(HAT, 0, 1, b, 6); for (int x = 0; x < 8; x++) front(HAT, x, 2, shade(b, -30));
                    front(HAT, 3, 0, 0xFFF0F0F0); front(HAT, 4, 0, 0xFFF0F0F0); }
                default -> shortHair(h, h2);
            }
        }

        void keepFace() { /* face rows 2..7 stay visible on purpose */ }
        void shortHair(int h, int h2) {
            top(HEAD, h, 8);
            for (int x = 0; x < 8; x++) { front(HEAD, x, 0, jit(h, 6)); front(HEAD, x, 1, jit(x % 3 == 0 ? h2 : h, 6)); }
            front(HEAD, 0, 2, h); front(HEAD, 7, 2, h);
            for (int y = 0; y < 3; y++) for (int x = 0; x < 8; x++) { right(HEAD, x, y, jit(h, 6)); left(HEAD, x, y, jit(h, 6)); }
            for (int x = 2; x < 6; x++) { right(HEAD, x, 3, h2); left(HEAD, x, 3, h2); }
            backRect(HEAD, 0, 0, 7, 4, h, 6);
        }
        void longBack(int h, int rows) { backRect(HEAD, 0, 0, 7, rows - 1, h, 6); }
        void sidesDown(int h, int rows) { for (int y = 0; y < rows; y++) for (int x = 0; x < 8; x++) if (y < 3 || x < 2 || x > 5) { right(HEAD, x, y, jit(h, 6)); left(HEAD, x, y, jit(h, 6)); } }
        void hatBack(int h, int y0, int y1) { backRect(HAT, 0, y0, 7, y1, h, 8); for (int y = y0; y <= y1; y++) { right(HAT, 7, y, h); left(HAT, 0, y, h); } }
        void curls(Part p, int h, int y0, int y1) {
            for (int x = 0; x < 2 * p.d + 2 * p.w; x++) for (int y = y0; y <= y1; y++) if (r.nextFloat() < 0.65f) px(p.u + x, p.v + p.d + y, jit(r.nextBoolean() ? h : shade(h, 20), 8));
            for (int x = 0; x < p.w; x++) for (int y = 0; y < p.d; y++) if (r.nextFloat() < 0.8f) px(p.u + p.d + x, p.v + y, jit(h, 12));
        }

        void mask() {
            int rot = 0xFF7E8572, rot2 = 0xFF5F6656;
            all(HEAD, rot, 10);
            for (int i = 0; i < 14; i++) front(HEAD, r.nextInt(8), r.nextInt(8), rot2);
            front(HEAD, 1, 3, 0xFF15130F); front(HEAD, 2, 3, 0xFF15130F); front(HEAD, 1, 4, 0xFF15130F); front(HEAD, 2, 4, 0xFF2A2018);
            front(HEAD, 5, 3, 0xFF15130F); front(HEAD, 6, 3, 0xFF15130F); front(HEAD, 5, 4, 0xFF2A2018); front(HEAD, 6, 4, 0xFF15130F);
            for (int x = 2; x <= 5; x++) front(HEAD, x, 6, x % 2 == 0 ? 0xFF3A2A1E : 0xFFCFC6A8);
            for (int y = 0; y < 8; y++) front(HEAD, 4, y, y % 2 == 0 ? 0xFF3A3020 : rot2);
            int stringy = 0xFF9A9384;
            top(HAT, stringy, 14);
            for (int x = 0; x < 32; x++) for (int y = 0; y < 8; y++) if (r.nextFloat() < (y < 3 ? 0.8f : 0.35f)) px(HAT.u + x, HAT.v + 8 + y, jit(stringy, 14));
            for (int x = 0; x < 8; x++) for (int y = 2; y < 8; y++) if (y > 1) px(HAT.u + 8 + x, HAT.v + 8 + y, 0);    // keep the mask face clear
        }

        // ------------------------------------------------------------------ outfits
        void basic(int shirt, int sleeves, int pants, int shoes) {
            torso(0, 11, shirt, 5, false);
            if (sleeves > 0) arms(0, sleeves - 1, shirt, 5, false);
            legs(0, 9, pants, 5, false);
            legs(10, 11, shoes, 3, false);
        }
        void belt(int c) { sides(BODY, 10, 11, c, 3); front(BODY, 3, 10, 0xFFB8A040); front(BODY, 4, 10, 0xFFB8A040); }
        static final int DENIM = 0xFF3A5A8C, DENIM_DARK = 0xFF26385A, BLACK = 0xFF1C1C1E, BOOT = 0xFF3A2A1C, WHITE = 0xFFE6E6E2;

        void outfit(int o) {
            switch (o) {
                case 0 -> { basic(cloth, 4, DENIM, 0xFFD8D8D4); }
                case 1 -> { basic(cloth, 12, DENIM_DARK, BOOT);
                    for (Part p : new Part[]{BODY, RARM, LARM}) for (int x = 0; x < 2 * p.d + 2 * p.w; x += 3) for (int y = 0; y < p.h; y++) px(p.u + x, p.v + p.d + y, shade(cloth, -30));
                    for (Part p : new Part[]{BODY, RARM, LARM}) for (int y = 1; y < p.h; y += 3) for (int x = 0; x < 2 * p.d + 2 * p.w; x++) px(p.u + x, p.v + p.d + y, shade(cloth, -22));
                    for (int y = 0; y < 12; y++) front(BODY, 4, y, shade(cloth, -40)); }
                case 2 -> { int lea = cloth == CLOTH[6] ? 0xFF4A3020 : BLACK; basic(0xFF9A9A9A, 0, BLACK, BLACK);
                    torso(0, 11, lea, 4, true); arms(0, 11, lea, 4, true); arms(0, 11, lea, 4, false);
                    for (int y = 0; y < 11; y++) { front(JACKET, 3, y, 0); front(JACKET, 4, y, 0); }
                    front(JACKET, 2, 0, shade(lea, 30)); front(JACKET, 5, 0, shade(lea, 30)); for (int y = 1; y < 10; y += 2) front(JACKET, 2, y, 0xFFB0B0B0); }
                case 3 -> { basic(cloth, 12, 0xFF6A6A70, 0xFFD8D8D4); frontRect(BODY, 1, 7, 6, 9, shade(cloth, -20), 3);
                    front(BODY, 3, 0, WHITE); front(BODY, 4, 0, WHITE); front(BODY, 3, 1, WHITE); front(BODY, 4, 2, WHITE);
                    hatBack(cloth, 0, 7); for (int y = 4; y < 8; y++) for (int x = 0; x < 8; x++) { right(HAT, x, y, jit(cloth, 4)); left(HAT, x, y, jit(cloth, 4)); } }
                case 4 -> { basic(0xFFB8A070, 12, 0xFF5A4630, BOOT); belt(BLACK); front(BODY, 5, 2, 0xFFE0C040); front(BODY, 2, 2, shade(0xFFB8A070, -25));
                    if (a.hair() < 14) { int hat = 0xFF6A4A2A; top(HAT, hat, 6); sides(HAT, 0, 1, hat, 6); for (int x = 0; x < 32; x++) px(HAT.u + x, HAT.v + 8 + 1, shade(hat, -30)); } }
                case 5 -> { basic(0xFF22305A, 12, BLACK, BLACK); belt(BLACK); front(BODY, 5, 2, 0xFFC8C8D0); front(BODY, 2, 1, 0xFFC8C8D0);
                    flanks(RARM, 1, 2, 0xFFC8B040); }
                case 6 -> { camo(); legs(10, 11, BLACK, 2, false); torso(1, 9, 0xFF4A4E2A, 4, true); for (int x = 1; x < 7; x += 2) front(JACKET, x, 6, 0xFF3A3E20); }
                case 7 -> { basic(cloth, 6, DENIM, BOOT);
                    for (Part p : new Part[]{BODY}) for (int x = 0; x < 2 * p.d + 2 * p.w; x += 2) for (int y = 0; y < 6; y++) if ((x + y) % 4 == 0) px(p.u + x, p.v + p.d + y, shade(cloth, -30));
                    torso(5, 11, DENIM, 4, false); for (int y = 0; y < 5; y++) { front(BODY, 1, y, DENIM); front(BODY, 6, y, DENIM); back(BODY, 1, y, DENIM); back(BODY, 6, y, DENIM); }
                    frontRect(BODY, 2, 6, 5, 8, shade(DENIM, -15), 2); front(BODY, 1, 5, 0xFFC0A040); front(BODY, 6, 5, 0xFFC0A040); }
                case 8 -> { int or = 0xFFE07020; basic(or, 4, or, WHITE); backRect(BODY, 2, 3, 5, 5, 0xFF1A1A1A, 0); backRect(BODY, 3, 4, 4, 4, WHITE, 0); }
                case 9 -> { basic(0xFF5AA0A0, 4, 0xFF5AA0A0, WHITE); torso(0, 11, WHITE, 3, true); arms(0, 11, WHITE, 3, true); legs(0, 4, WHITE, 3, true);
                    for (int y = 0; y < 11; y++) { front(JACKET, 3, y, 0); front(JACKET, 4, y, 0); } front(JACKET, 5, 3, 0xFF2A5AA0); front(JACKET, 5, 4, 0xFFC03030); }
                case 10 -> { basic(WHITE, 0, DENIM_DARK, BLACK); int lea = BLACK; torso(0, 10, lea, 4, true);
                    for (int y = 0; y < 10; y++) { front(JACKET, 3, y, 0); front(JACKET, 4, y, 0); }
                    backRect(JACKET, 1, 2, 6, 6, 0xFF8A1E1E, 6); backRect(JACKET, 3, 3, 4, 5, 0xFFE0E0E0, 0); flanks(JACKET, 0, 1, 0xFF6A6A6A); }
                case 11 -> { int suit = cloth == CLOTH[4] ? CLOTH[3] : cloth == CLOTH[0] ? CLOTH[1] : cloth; basic(suit, 12, suit, BLACK);
                    for (int y = 0; y < 8; y++) { front(BODY, 3, y, WHITE); front(BODY, 4, y, WHITE); } for (int y = 1; y < 8; y++) front(BODY, y % 2 == 0 ? 3 : 4, y, 0xFFA02020);
                    front(BODY, 3, 1, 0xFFA02020); front(BODY, 4, 1, 0xFFA02020); arms(11, 11, WHITE, 0, false); }
                case 12 -> { basic(cloth, 12, cloth, WHITE); flanks(RARM, 0, 11, WHITE); flanks(LARM, 0, 11, WHITE); flanks(RLEG, 0, 9, WHITE); flanks(LLEG, 0, 9, WHITE);
                    for (int y = 0; y < 12; y++) front(BODY, 4, y, shade(cloth, -30)); }
                case 13 -> { basic(cloth, 0, 0xFFA89870, BOOT); torso(0, 0, skin, 2, false); front(BODY, 1, 0, cloth); front(BODY, 6, 0, cloth); back(BODY, 1, 0, cloth); back(BODY, 6, 0, cloth);
                    frontRect(RLEG, 0, 4, 1, 6, shade(0xFFA89870, -20), 2); frontRect(LLEG, 2, 4, 3, 6, shade(0xFFA89870, -20), 2); }
                case 14 -> { basic(0xFF8A8A80, 12, 0xFF5A4630, BOOT); torso(0, 11, cloth, 5, true); arms(0, 7, cloth, 5, true);
                    for (int x = 0; x < 24; x += 3) for (int y = 0; y < 12; y++) if (y % 4 == 1) px(JACKET.u + x, JACKET.v + 4 + y, shade(cloth, 30)); }
                case 15 -> { basic(0xFF3A3A3A, 12, 0xFF2A2A2A, BLACK); int m = 0xFF8A8C90; torso(1, 9, m, 10, true); arms(0, 3, m, 10, true);
                    for (int x = 0; x < 8; x += 3) for (int y = 2; y < 9; y += 3) front(JACKET, x, y, 0xFFC8C8C8); legs(3, 5, m, 8, true); frontRect(JACKET, 0, 4, 7, 4, 0xFF6A4A2A, 3); }
                case 16 -> { basic(0xFF5A5030, 12, 0xFF5A4630, BOOT); torso(0, 9, 0xFFE06A10, 6, true); for (int y = 0; y < 10; y++) { front(JACKET, 3, y, 0); front(JACKET, 4, y, 0); } }
                case 17 -> { basic(cloth, 12, cloth, BLACK); frontRect(BODY, 5, 2, 6, 2, WHITE, 0); for (int i = 0; i < 8; i++) { int px = r.nextInt(24); px(BODY.u + px, BODY.v + 4 + r.nextInt(12), 0xFF2A2420); }
                    belt(shade(cloth, -30)); }
                case 18 -> { basic(cloth, 12, BLACK, 0xFFD8D8D4); torso(0, 10, cloth, 3, true); arms(0, 11, cloth, 3, true);
                    for (Part p : new Part[]{JACKET, RSLEEVE, LSLEEVE}) for (int y = 2; y < p.h; y += 3) for (int x = 0; x < 2 * p.d + 2 * p.w; x++) px(p.u + x, p.v + p.d + y, shade(cloth, -28));
                    for (int y = 0; y < 11; y++) front(JACKET, 4, y, 0xFF9A9A9A); }
                case 19 -> { int rag = 0xFF6A5A46, rag2 = 0xFF4E4236; torso(0, 11, rag, 10, false); arms(0, 2, rag2, 10, false); legs(0, 8, rag2, 10, false);
                    for (int i = 0; i < 18; i++) { Part p = new Part[]{BODY, RLEG, LLEG}[r.nextInt(3)]; px(p.u + r.nextInt(2 * p.d + 2 * p.w), p.v + p.d + r.nextInt(p.h), skin); }
                    torso(0, 7, shade(rag, -10), 12, true); for (int x = 0; x < 24; x++) if (r.nextFloat() < 0.4f) px(JACKET.u + x, JACKET.v + 4 + 7 + r.nextInt(3), 0); }
                case 20 -> { int bl = 0xFF16161A, pl = 0xFF3C3E44; basic(bl, 12, bl, bl); torso(0, 9, pl, 4, true); arms(0, 4, pl, 4, true); legs(2, 6, pl, 4, true);
                    top(HAT, bl, 3); sides(HAT, 0, 2, bl, 3); for (int y = 3; y <= 4; y++) for (int x = 0; x < 8; x++) front(HAT, x, y, 0x88202830); front(JACKET, 3, 2, WHITE); front(JACKET, 4, 2, WHITE); }
                case 21 -> { basic(cloth, 12, DENIM_DARK, BOOT); int dn = 0xFF4A6EA0; torso(0, 10, dn, 5, true); arms(0, 11, dn, 5, true); arms(0, 11, dn, 5, false);
                    for (int y = 0; y < 10; y++) { front(JACKET, 3, y, 0); front(JACKET, 4, y, 0); } front(JACKET, 1, 3, 0xFFC09040); front(JACKET, 6, 3, 0xFFC09040); }
                case 22 -> { basic(cloth, 12, 0xFF5A4E40, BOOT);
                    for (Part p : new Part[]{BODY, RARM, LARM}) for (int x = 0; x < 2 * p.d + 2 * p.w; x++) for (int y = 0; y < p.h; y++) if ((x + y) % 2 == 0) px(p.u + x, p.v + p.d + y, shade(cloth, -14));
                    sides(BODY, 0, 0, shade(cloth, 25), 2); sides(BODY, 11, 11, shade(cloth, -30), 2); }
                case 23 -> { basic(WHITE, 12, 0xFF2A2A2A, BLACK); for (int x = 0; x < 4; x++) for (int y = 0; y < 10; y++) if ((x + y) % 2 == 0) { front(RLEG, x, y, WHITE); front(LLEG, x, y, WHITE); }
                    for (int y = 1; y < 10; y += 3) { front(BODY, 2, y, 0xFF8A8A8A); front(BODY, 5, y, 0xFF8A8A8A); } }
                default -> basic(cloth, 4, DENIM, BOOT);
            }
        }

        void camo() {
            int[] cs = {0xFF4E5A2E, 0xFF3A4422, 0xFF6A6040, 0xFF2E3420};
            for (Part p : new Part[]{BODY, RARM, LARM, RLEG, LLEG}) {
                for (int x = 0; x < 2 * p.d + 2 * p.w; x++) for (int y = 0; y < p.h; y++) px(p.u + x, p.v + p.d + y, cs[(int) ((Math.sin(x * 1.3 + p.u) + Math.cos(y * 0.9 + p.v) + 2) * 1.0) % 4]);
                top(p, cs[0], 6);
            }
        }
    }

    private HumanSkins() {}
}
