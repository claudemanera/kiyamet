package com.kiyamet.npc;

public enum Faction {
    SURVIVOR("Survivor"),      // lone wanderer, can be recruited
    COMMUNITY("Community"),    // lives in a settlement; friendly or hostile per player
    RAIDER("Raider"),          // armed gang, always hostile
    WHISPERER("Masked One"),   // wears walker skin, walks with the herds, always hostile
    COMPANION("Companion");    // follows a player

    public final String title;

    Faction(String title) { this.title = title; }

    public static Faction byId(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }
}
