package com.kiyamet.npc;

import net.minecraft.util.RandomSource;

public final class Names {
    private static final String[] FIRST = {"Aaron", "Abby", "Alden", "Amira", "Ben", "Bianca", "Boyd", "Carla", "Cole", "Dana", "Dex", "Duke",
            "Elena", "Eli", "Faye", "Finn", "Gabe", "Gina", "Hank", "Hazel", "Ike", "Iris", "Jace", "Jade", "Kane", "Kira", "Leon", "Lila",
            "Mack", "Maya", "Nash", "Nora", "Owen", "Opal", "Pete", "Quinn", "Rex", "Rosa", "Sam", "Sky", "Theo", "Tess", "Ugo", "Vera",
            "Wade", "Wren", "Xander", "Yara", "Zeke", "Zoe", "Murphy", "Rook", "Sasha", "Tobin", "Vince", "Marlow", "June", "Silas", "Ash", "Cruz"};
    private static final String[] RAIDER_TAGS = {"Spike", "Crow", "Knuckles", "Rat", "Viper", "Chainsaw", "Bones", "Tank", "Razor", "Hound",
            "Brick", "Snake", "Mad Dog", "Hatchet", "Jinx", "Fang"};

    public static String random(RandomSource r, Faction f, Role role) {
        if (f == Faction.WHISPERER) return role == Role.COMMANDER ? "The Alpha Mask" : null;
        if (f == Faction.RAIDER) return role == Role.COMMANDER ? "Commander " + FIRST[r.nextInt(FIRST.length)] : role == Role.LEADER ? RAIDER_TAGS[r.nextInt(RAIDER_TAGS.length)] : null;
        return FIRST[r.nextInt(FIRST.length)];
    }

    private Names() {}
}
