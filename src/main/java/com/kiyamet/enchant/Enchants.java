package com.kiyamet.enchant;

import com.kiyamet.Kiyamet;
import com.kiyamet.skill.Skill;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * TWD 1.4 enchanting. Every enchantment gets a higher cap (Sharpness to X, Protection to X, Unbreaking to X...)
 * and each level is unlocked one by one along the Enchanting road (e.g. "Lv 13: Fire Protection III").
 * Gun enchantments (blue tomes) unlock along the Firearms road from level 100.
 */
public final class Enchants {
    /** id, vanilla max, new max. Single-level enchantments (Mending, Silk Touch...) are left alone. */
    static final Object[][] VANILLA = {
            {"protection", 4, 10}, {"fire_protection", 4, 8}, {"feather_falling", 4, 10}, {"blast_protection", 4, 8}, {"projectile_protection", 4, 8},
            {"respiration", 3, 6}, {"thorns", 3, 6}, {"depth_strider", 3, 3}, {"frost_walker", 2, 4}, {"soul_speed", 3, 6}, {"swift_sneak", 3, 5},
            {"sharpness", 5, 10}, {"smite", 5, 10}, {"bane_of_arthropods", 5, 8}, {"knockback", 2, 4}, {"fire_aspect", 2, 4}, {"looting", 3, 6},
            {"sweeping_edge", 3, 6}, {"efficiency", 5, 10}, {"unbreaking", 3, 10}, {"fortune", 3, 6}, {"power", 5, 10}, {"punch", 2, 4},
            {"luck_of_the_sea", 3, 6}, {"lure", 3, 5}, {"loyalty", 3, 5}, {"impaling", 5, 8}, {"riptide", 3, 5}, {"quick_charge", 3, 5},
            {"piercing", 4, 8}, {"density", 5, 8}, {"breach", 4, 6}, {"wind_burst", 3, 5},
            {"aqua_affinity", 1, 1}, {"silk_touch", 1, 1}, {"mending", 1, 1}, {"infinity", 1, 1}, {"flame", 1, 1}, {"channeling", 1, 1}, {"multishot", 1, 1}};

    /** The gun enchantments (data/kiyamet/enchantment/*.json): id, title, max level, what it does. */
    public static final String[][] GUN = {
            {"rapid_fire", "Rapid Fire", "5", "-10% cooldown per level"},
            {"high_caliber", "High Caliber", "5", "+12% damage per level"},
            {"long_barrel", "Long Barrel", "4", "+15% range per level"},
            {"ammo_saver", "Ammo Saver", "3", "15% chance per level to keep the arrow"},
            {"incendiary_rounds", "Incendiary Rounds", "3", "sets targets on fire, 3 s per level"},
            {"frost_rounds", "Frost Rounds", "3", "slows and freezes targets"},
            {"vampiric_rounds", "Vampiric Rounds", "3", "heals you 6% of damage per level"},
            {"explosive_tips", "Explosive Tips", "2", "small blast on hit (no block damage)"}};

    public record Unlock(String id, int level, int at, boolean gun) {}

    /** Every enchantment level above I, ordered along the road: which skill level unlocks it. */
    public static final List<Unlock> ROAD = new ArrayList<>();
    public static final List<Unlock> GUN_ROAD = new ArrayList<>();
    private static final Map<String, Integer> CAP = new HashMap<>();

    static {
        record P(String id, int lv, double key) {}
        List<P> ps = new ArrayList<>();
        for (Object[] v : VANILLA) {
            String id = (String) v[0];
            int max = (Integer) v[2];
            CAP.put("minecraft:" + id, max);
            for (int l = 2; l <= max; l++) ps.add(new P(id, l, l / (double) max + l * 0.04 + (l > (Integer) v[1] ? 0.35 : 0)));
        }
        ps.sort(Comparator.comparingDouble(P::key).thenComparing(P::id));
        for (int i = 0; i < ps.size(); i++) ROAD.add(new Unlock("minecraft:" + ps.get(i).id, ps.get(i).lv, (int) Math.round(3 + i * 996.0 / (ps.size() - 1)), false));
        List<P> gs = new ArrayList<>();
        for (String[] g : GUN) {
            int max = Integer.parseInt(g[2]);
            CAP.put("kiyamet:" + g[0], max);
            for (int l = 1; l <= max; l++) gs.add(new P(g[0], l, l / (double) max + l * 0.05));
        }
        gs.sort(Comparator.comparingDouble(P::key).thenComparing(P::id));
        for (int i = 0; i < gs.size(); i++) GUN_ROAD.add(new Unlock("kiyamet:" + gs.get(i).id, gs.get(i).lv, (int) Math.round(100 + i * 899.0 / (gs.size() - 1)), true));
    }

    public static boolean isGun(String id) { return id.startsWith("kiyamet:"); }

    /** Highest level of this enchantment you may put on with your current skills (0 = not yet). */
    public static int cap(String id, int enchanting, int archery) {
        if (!CAP.containsKey(id)) return 1;                       // unknown / modded: leave at 1
        boolean gun = isGun(id);
        int best = gun ? 0 : 1;
        for (Unlock u : gun ? GUN_ROAD : ROAD) if (u.id.equals(id) && u.at <= (gun ? archery : enchanting)) best = Math.max(best, u.level);
        return best;
    }

    public static int maxCap(String id) { return CAP.getOrDefault(id, 1); }

    /** Skill level at which the next level of this enchantment unlocks, or -1. */
    public static int nextUnlock(String id, int have) {
        for (Unlock u : isGun(id) ? GUN_ROAD : ROAD) if (u.id.equals(id) && u.level == have + 1) return u.at;
        return -1;
    }

    public static List<Unlock> unlocksBetween(Skill s, int from, int to) {
        List<Unlock> out = new ArrayList<>();
        if (s == Skill.ENCHANTING) for (Unlock u : ROAD) if (u.at > from && u.at <= to) out.add(u);
        if (s == Skill.ARCHERY) for (Unlock u : GUN_ROAD) if (u.at > from && u.at <= to) out.add(u);
        return out;
    }

    public static String gunTitle(String id) {
        for (String[] g : GUN) if (("kiyamet:" + g[0]).equals(id)) return g[1];
        return id;
    }

    public static String title(String id) {
        if (isGun(id)) return gunTitle(id);
        String s = id.substring(id.indexOf(':') + 1).replace('_', ' ');
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) { if (b.length() > 0) b.append(' '); b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)); }
        return b.toString();
    }

    public static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
        return n < r.length ? r[n] : String.valueOf(n);
    }

    public static Optional<Holder.Reference<Enchantment>> holder(Level level, String id) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, net.minecraft.resources.Identifier.parse(id)));
    }

    /** Level of one of our gun enchantments on a stack. */
    public static int gunLevel(Level level, ItemStack stack, String name) {
        return holder(level, "kiyamet:" + name).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
    }

    public static String id(Holder<Enchantment> h) { return h.unwrapKey().map(k -> k.identifier().toString()).orElse("?"); }

    private Enchants() {}
}
