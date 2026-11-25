package com.kiyamet.npc;

/** The four women-only survivor types. Each brings her own skill to your group. */
public enum Specialty {
    NONE("", ""),
    NURSE("Nurse", "Heals you and your companions every 10 s, clears poison; from Lv 120 cures your infection every 5 min"),
    HUNTRESS("Huntress", "Bow/crossbow expert: +30% ranged damage, shoots 30% faster, never misses at close range"),
    SCAVENGER("Scavenger", "Finds supplies every 3 min (food, arrows, medicine, parts, sometimes rubies) and puts them in her inventory"),
    ENGINEER("Engineer", "Repairs damaged walls within 10 blocks every 15 s and fixes your robots and turrets; from Lv 150 she repairs twice as much");

    public final String title, desc;

    Specialty(String title, String desc) { this.title = title; this.desc = desc; }

    public static Specialty byName(String s) {
        try { return valueOf(s); } catch (IllegalArgumentException e) { return NONE; }
    }
}
