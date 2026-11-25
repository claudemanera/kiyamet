package com.kiyamet.npc;

import net.minecraft.util.RandomSource;

/**
 * The 25 human types. 20 friendly professions shape each person's clothes, gear and weapon; the last five are the
 * hand-made enemy designs (Raider, Brute, Raider Woman, Raider Commander, Masked One).
 */
public enum Profession {
    //            title               gender  outfits                 headwear              masks              accessories     weapons
    SURVIVOR     ("Survivor",          0, i(0, 1, 3, 12, 18, 21, 22),  i(0, 0, 1, 12, 9, 6),  i(0, 0, 0, 8, 14),  i(3, 1, 0),      "minecraft:stone_axe,minecraft:wooden_sword,minecraft:bow"),
    FARMER       ("Farmer",            0, i(7, 1, 26),                 i(2, 12, 1, 0),        i(0),              i(0, 7),         "minecraft:iron_axe,minecraft:stone_sword,minecraft:bow"),
    DOCTOR       ("Doctor",            0, i(9),                        i(0),                  i(3, 0),           i(4, 0),         "minecraft:iron_sword,kiyamet:silenced_arrow_pistol"),
    NURSE        ("Nurse",             2, i(30),                       i(0),                  i(3, 0, 0),        i(0, 4),         "minecraft:iron_sword"),
    HUNTRESS     ("Huntress",          2, i(16, 14, 29),               i(9, 13, 0),           i(0, 14),          i(2),            "minecraft:bow,minecraft:crossbow"),
    SCAVENGER    ("Scavenger",         2, i(3, 21, 18, 19),            i(9, 1, 6, 15),        i(4, 14, 9, 0),    i(3, 7),         "minecraft:stone_axe,minecraft:crossbow"),
    ENGINEER     ("Engineer",          2, i(17, 3, 21),                i(11, 7, 0),           i(0, 6),           i(7),            "kiyamet:arrow_pistol,minecraft:iron_axe"),
    POLICE       ("Police Officer",    0, i(5),                        i(5, 0),               i(0, 9),           i(6),            "kiyamet:arrow_pistol,minecraft:iron_sword"),
    SHERIFF      ("Sheriff",           1, i(4),                        i(2),                  i(0, 9),           i(6),            "kiyamet:arrow_pistol,kiyamet:arrow_rifle"),
    SOLDIER      ("Soldier",           0, i(6),                        i(3),                  i(0, 1, 9),        i(5, 2),         "kiyamet:arrow_rifle,kiyamet:arrow_repeater"),
    MECHANIC     ("Mechanic",          0, i(17),                       i(7, 11, 1),           i(0, 6),           i(7),            "minecraft:iron_axe,kiyamet:harpoon_gun"),
    COOK         ("Cook",              0, i(23),                       i(0),                  i(0),              i(0),            "minecraft:iron_sword,minecraft:iron_axe"),
    TEACHER      ("Teacher",           0, i(27, 22),                   i(0, 10),              i(0, 9),           i(1, 0),         "minecraft:wooden_sword,minecraft:bow"),
    PRISONER     ("Prisoner",          0, i(8, 31),                    i(0, 13),              i(0, 4),           i(0),            "minecraft:stone_axe,minecraft:stone_sword"),
    FIREFIGHTER  ("Firefighter",       0, i(24),                       i(4),                  i(6, 0),           i(0),            "minecraft:iron_axe"),
    TRUCKER      ("Trucker",           1, i(26, 1),                    i(1),                  i(0, 9),           i(0),            "minecraft:iron_axe,kiyamet:arrow_shotgun"),
    HUNTER       ("Hunter",            1, i(16, 29),                   i(12, 1, 9),           i(0, 14),          i(2),            "minecraft:bow,minecraft:crossbow,kiyamet:arrow_sniper"),
    TRADER       ("Trader",            0, i(14, 7, 17),                i(12, 10, 0),          i(0, 14),          i(3, 4),         "minecraft:crossbow,minecraft:iron_sword"),
    LEADER       ("Leader",            0, i(11, 4, 21),                i(0, 10, 2),           i(0, 9),           i(4, 6),         "kiyamet:arrow_rifle,minecraft:diamond_sword"),
    GUARD        ("Guard",             0, i(20, 6, 15),                i(14, 3),              i(5, 1, 0),        i(2, 5),         "kiyamet:arrow_rifle,kiyamet:arrow_shotgun,minecraft:iron_sword"),
    ROYAL_GUARD  ("Royal Guard",       0, i(25),                       i(3, 0),               i(0),              i(0),            "minecraft:iron_sword,minecraft:crossbow"),
    RAIDER       ("Raider",            1, i(), i(), i(), i(), ""),
    BRUTE        ("Brute",             1, i(), i(), i(), i(), ""),
    RAIDER_WOMAN ("Raider Woman",      2, i(), i(), i(), i(), ""),
    COMMANDER    ("Raider Commander",  1, i(), i(), i(), i(), ""),
    MASKED_ONE   ("Masked One",        0, i(), i(), i(), i(), "");

    public final String title, weapons;
    public final int gender;                    // 0 any, 1 man, 2 woman
    final int[] outfits, headwear, masks, accessories;

    Profession(String title, int gender, int[] outfits, int[] headwear, int[] masks, int[] accessories, String weapons) {
        this.title = title; this.gender = gender; this.outfits = outfits; this.headwear = headwear; this.masks = masks; this.accessories = accessories; this.weapons = weapons;
    }

    private static int[] i(int... v) { return v; }
    private static int pick(RandomSource r, int[] a, int fallback) { return a.length == 0 ? fallback : a[r.nextInt(a.length)]; }

    public boolean enemy() { return ordinal() >= RAIDER.ordinal(); }

    public Specialty specialty() {
        return switch (this) { case NURSE -> Specialty.NURSE; case HUNTRESS -> Specialty.HUNTRESS; case SCAVENGER -> Specialty.SCAVENGER; case ENGINEER -> Specialty.ENGINEER; default -> Specialty.NONE; };
    }

    public static Profession byName(String s) {
        try { return valueOf(s); } catch (IllegalArgumentException e) { return SURVIVOR; }
    }

    /** Picks a fitting profession for a new person. */
    public static Profession choose(RandomSource r, Faction f, Role role, String community, boolean female) {
        if (f == Faction.RAIDER) return role == Role.COMMANDER ? COMMANDER : female ? RAIDER_WOMAN : r.nextInt(4) == 0 ? BRUTE : RAIDER;
        if (f == Faction.WHISPERER) return MASKED_ONE;
        if (role == Role.LEADER) return LEADER;
        if (role == Role.TRADER) return TRADER;
        if (role == Role.GUARD) return switch (community == null ? "" : community) {
            case "kingdom" -> ROYAL_GUARD;
            case "prison" -> r.nextBoolean() ? GUARD : POLICE;
            case "farm" -> r.nextBoolean() ? SHERIFF : HUNTER;
            default -> new Profession[]{GUARD, SOLDIER, POLICE, SHERIFF, GUARD}[r.nextInt(5)];
        };
        if (r.nextFloat() < 0.25f) return new Profession[]{NURSE, HUNTRESS, SCAVENGER, ENGINEER}[r.nextInt(4)];
        Profession[] pool = switch (community == null ? "" : community) {
            case "prison" -> new Profession[]{PRISONER, PRISONER, COOK, MECHANIC, SURVIVOR, DOCTOR};
            case "farm" -> new Profession[]{FARMER, FARMER, COOK, HUNTER, SURVIVOR};
            case "walled_town" -> new Profession[]{TEACHER, DOCTOR, MECHANIC, COOK, SURVIVOR, POLICE};
            case "hilltop" -> new Profession[]{FARMER, MECHANIC, TRUCKER, COOK, SURVIVOR};
            case "kingdom" -> new Profession[]{FARMER, COOK, TEACHER, ROYAL_GUARD, MECHANIC};
            case "terminal" -> new Profession[]{COOK, COOK, SURVIVOR, TRUCKER, HUNTER};
            default -> new Profession[]{SURVIVOR, SURVIVOR, FARMER, DOCTOR, POLICE, SOLDIER, MECHANIC, COOK, TEACHER, PRISONER, FIREFIGHTER, TRUCKER, HUNTER};
        };
        Profession p = pool[r.nextInt(pool.length)];
        if (p.gender == 1 && female) p = SURVIVOR;
        return p;
    }

    /** Dresses an appearance and builds the gear for this profession. */
    public Appearance dress(RandomSource r, Appearance a) {
        if (enemy() || outfits.length == 0) return a;
        Appearance out = a.withOutfit(pick(r, outfits, a.outfit()), a.outfitColor());
        if (this == PRISONER || this == FIREFIGHTER || this == ROYAL_GUARD) out = out.withDetail(r.nextInt(4) == 0 ? 1 : 0);
        if (this == NURSE) out = out.withOutfit(out.outfit(), new int[]{14, 14, 10, 4, 15, 1}[r.nextInt(6)]);
        if (gender == 2 && !out.female()) out = out.withFemale(true);
        if (gender == 1 && out.female()) out = out.withFemale(false);
        return out;
    }

    public Gear gear(RandomSource r) {
        if (enemy() || outfits.length == 0) return Gear.NONE;
        return new Gear(pick(r, headwear, 0), pick(r, masks, 0), pick(r, accessories, 0), r.nextInt(Gear.COLORS));
    }

    public String randomWeapon(RandomSource r) {
        if (weapons.isEmpty()) return "";
        String[] w = weapons.split(",");
        return w[r.nextInt(w.length)];
    }
}
