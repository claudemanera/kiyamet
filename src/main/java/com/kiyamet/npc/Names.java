package com.kiyamet.npc;

import net.minecraft.util.RandomSource;

public final class Names {
    private static final String[] FIRST = {"Aaron", "Abby", "Alden", "Amira", "Ben", "Bianca", "Boyd", "Carla", "Cole", "Dana", "Dex", "Duke",
            "Elena", "Eli", "Faye", "Finn", "Gabe", "Gina", "Hank", "Hazel", "Ike", "Iris", "Jace", "Jade", "Kane", "Kira", "Leon", "Lila",
            "Mack", "Maya", "Nash", "Nora", "Owen", "Opal", "Pete", "Quinn", "Rex", "Rosa", "Sam", "Sky", "Theo", "Tess", "Ugo", "Vera",
            "Wade", "Wren", "Xander", "Yara", "Zeke", "Zoe", "Murphy", "Rook", "Sasha", "Tobin", "Vince", "Marlow", "June", "Silas", "Ash", "Cruz"};
    private static final String[] RAIDER_TAGS = {"Spike", "Crow", "Knuckles", "Rat", "Viper", "Chainsaw", "Bones", "Tank", "Razor", "Hound",
            "Brick", "Snake", "Mad Dog", "Hatchet", "Jinx", "Fang"};

    private static final String[] FEMALE = {"Abby", "Andrea", "Beth", "Carol", "Clara", "Dana", "Ella", "Emma", "Faye", "Grace", "Hazel", "Iris",
            "Jade", "Jess", "Kara", "Lena", "Lila", "Lori", "Maya", "Mira", "Nadia", "Nora", "Olivia", "Opal", "Paula", "Quinn", "Rosa", "Rita",
            "Sasha", "Sara", "Tara", "Tess", "Uma", "Vera", "Willa", "Wren", "Yara", "Zoe", "June", "Ruby", "Ivy", "Luna", "Elena", "Mae"};
    private static final String[] FEMALE_RAIDER = {"Viper", "Jinx", "Raven", "Blade", "Scar", "Hex", "Venom", "Fury"};

    public static String random(RandomSource r, Faction f, Role role, boolean female) {
        if (!female) return random(r, f, role);
        if (f == Faction.WHISPERER) return role == Role.COMMANDER ? "The Alpha Mask" : null;
        if (f == Faction.RAIDER) return role == Role.COMMANDER ? "Commander " + FEMALE[r.nextInt(FEMALE.length)] : role == Role.LEADER ? FEMALE_RAIDER[r.nextInt(FEMALE_RAIDER.length)] : null;
        return FEMALE[r.nextInt(FEMALE.length)];
    }

    public static String random(RandomSource r, Faction f, Role role) {
        if (f == Faction.WHISPERER) return role == Role.COMMANDER ? "The Alpha Mask" : null;
        if (f == Faction.RAIDER) return role == Role.COMMANDER ? "Commander " + FIRST[r.nextInt(FIRST.length)] : role == Role.LEADER ? RAIDER_TAGS[r.nextInt(RAIDER_TAGS.length)] : null;
        return FIRST[r.nextInt(FIRST.length)];
    }

    private Names() {}
}
