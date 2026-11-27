package com.kiyamet.stone;

public enum StoneKind {
    FIRE("fire_stone", 300), FROST("frost_stone", 300), STORM("storm_stone", 240), NATURE("nature_stone", 400), BLOOD("blood_stone", 300),
    SHADOW("shadow_stone", 600), SOUL("soul_stone", 1200), TIME("time_stone", 900), VOID("void_stone", 900), EARTH("earth_stone", 300),
    // 2.2: found only in the End
    STAR("star_stone", 600), DRAGON("dragon_stone", 900);

    public final String id;
    public final int cooldown;
    StoneKind(String id, int cooldown) { this.id = id; this.cooldown = cooldown; }

    public static StoneKind byId(String id) {
        for (StoneKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
