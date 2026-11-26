package com.kiyamet.wild;

/** The 8 wild animals of 1.9 and their rare walker forms. */
public enum WildKind {
    //         id              title             hp   dmg  speed  w     h     predator tame  zombie meat              pelt
    LION      ("lion",         "Lion",           40,  9,   0.34, 1.3f, 1.4f, true,  true,  false, "big_cat_meat",  "lion_pelt"),
    TIGER     ("tiger",        "Tiger",          44,  10,  0.35, 1.3f, 1.3f, true,  true,  false, "big_cat_meat",  "tiger_pelt"),
    BROWN_BEAR("brown_bear",   "Brown Bear",     60,  12,  0.30, 1.5f, 1.6f, true,  false, false, "bear_meat",     "bear_pelt"),
    ICE_BEAR  ("ice_bear",     "Ice Bear",       75,  14,  0.31, 1.6f, 1.8f, true,  false, false, "bear_meat",     "ice_bear_pelt"),
    CROCODILE ("crocodile",    "Crocodile",      45,  11,  0.26, 1.6f, 0.7f, true,  false, false, "croc_meat",     ""),
    WILD_BOAR ("wild_boar",    "Wild Boar",      22,  6,   0.30, 1.0f, 0.9f, false, false, false, "boar_meat",     ""),
    DEER      ("deer",         "Deer",           16,  3,   0.33, 0.9f, 1.5f, false, false, false, "venison",       ""),
    MOOSE     ("moose",        "Moose",          50,  9,   0.28, 1.5f, 2.2f, false, false, false, "moose_meat",    ""),
    ZOMBIE_LION ("zombie_lion",  "Walker Lion",  50,  10,  0.30, 1.3f, 1.4f, true,  false, true,  "",              "lion_pelt"),
    ZOMBIE_TIGER("zombie_tiger", "Walker Tiger", 54,  11,  0.31, 1.3f, 1.3f, true,  false, true,  "",              "tiger_pelt"),
    ZOMBIE_BEAR ("zombie_bear",  "Walker Bear",  80,  13,  0.27, 1.5f, 1.6f, true,  false, true,  "",              "bear_pelt"),
    ZOMBIE_DEER ("zombie_deer",  "Walker Deer",  26,  6,   0.30, 0.9f, 1.5f, true,  false, true,  "",              "");

    public final String id, title, meat, pelt;
    public final double hp, damage, speed;
    public final float width, height;
    public final boolean predator, tameable, zombie;

    WildKind(String id, String title, double hp, double damage, double speed, float w, float h, boolean predator, boolean tameable, boolean zombie, String meat, String pelt) {
        this.id = id; this.title = title; this.hp = hp; this.damage = damage; this.speed = speed; this.width = w; this.height = h;
        this.predator = predator; this.tameable = tameable; this.zombie = zombie; this.meat = meat; this.pelt = pelt;
    }

    /** the living animal a walker form came from (for the model) */
    public String model() { return switch (this) { case ZOMBIE_LION -> "lion"; case ZOMBIE_TIGER -> "tiger"; case ZOMBIE_BEAR -> "brown_bear"; case ZOMBIE_DEER -> "deer"; default -> id; }; }
}
