package com.kiyamet.skill;

/** The 20 skills, levels 1..999. Order is the save order: never reorder, only append. */
public enum Skill {
    HEALTH, ATHLETICS, MINING, SWORDS, ARCHERY, DEFENSE, ACROBATICS, SWIMMING, FARMING, WOODCUTTING,
    HUNTING, ENCHANTING, ALCHEMY, EXPLORATION, ENGINEERING, SURVIVAL, STEALTH, MEDICINE, HEAVY_WEAPONS, BUILDING;

    public static final int MAX_LEVEL = 999;

    public String key() { return "kiyamet.skill." + name().toLowerCase(); }
}
