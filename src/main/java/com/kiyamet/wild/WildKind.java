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
    // walker animals: rare, attack the moment they see you, and hit very hard
    ZOMBIE_LION ("zombie_lion",  "Walker Lion",  60,  15,  0.36, 1.3f, 1.4f, true,  false, true,  "",              "lion_pelt"),
    ZOMBIE_TIGER("zombie_tiger", "Walker Tiger", 64,  16,  0.37, 1.3f, 1.3f, true,  false, true,  "",              "tiger_pelt"),
    ZOMBIE_BEAR ("zombie_bear",  "Walker Bear",  95,  19,  0.32, 1.5f, 1.6f, true,  false, true,  "",              "bear_pelt"),
    ZOMBIE_CROC ("zombie_crocodile", "Walker Crocodile", 70, 17, 0.30, 1.6f, 0.7f, true, false, true, "",           ""),
    ZOMBIE_DEER ("zombie_deer",  "Walker Deer",  34,  10,  0.38, 0.9f, 1.5f, true,  false, true,  "",              ""),
    ZOMBIE_WOLF ("zombie_wolf",  "Walker Wolf",  30,  9,   0.38, 0.6f, 0.85f, true, false, true,  "",              ""),
    // pets (1.9): tame them and they stay with you
    SHEPHERD  ("german_shepherd", "German Shepherd", 30, 6, 0.33, 0.7f, 0.95f, false, true, false, "",             ""),
    HUSKY     ("husky",        "Husky",          28,  5,   0.35, 0.7f, 0.95f, false, true,  false, "",              ""),
    ROTTWEILER("rottweiler",   "Rottweiler",     36,  8,   0.31, 0.75f, 0.95f, false, true, false, "",              ""),
    PITBULL   ("pitbull",      "Black Pitbull",  34, 9,   0.34, 0.7f, 0.85f, false, true, false, "",              ""),
    CAT       ("stray_cat",    "Stray Cat",      12,  3,   0.34, 0.5f, 0.6f, false, true,  false, "",              "");

    public final String id, title, meat, pelt;
    public final double hp, damage, speed;
    public final float width, height;
    public final boolean predator, tameable, zombie;

    WildKind(String id, String title, double hp, double damage, double speed, float w, float h, boolean predator, boolean tameable, boolean zombie, String meat, String pelt) {
        this.id = id; this.title = title; this.hp = hp; this.damage = damage; this.speed = speed; this.width = w; this.height = h;
        this.predator = predator; this.tameable = tameable; this.zombie = zombie; this.meat = meat; this.pelt = pelt;
    }

    public boolean pet() { return this == SHEPHERD || this == HUSKY || this == ROTTWEILER || this == PITBULL || this == CAT; }
    public boolean dog() { return this == SHEPHERD || this == HUSKY || this == ROTTWEILER || this == PITBULL; }

    /** what this animal rises as after it dies (null = it stays dead) */
    public WildKind risesAs() { return switch (this) { case LION -> ZOMBIE_LION; case TIGER -> ZOMBIE_TIGER; case BROWN_BEAR, ICE_BEAR -> ZOMBIE_BEAR; case CROCODILE -> ZOMBIE_CROC; case DEER -> ZOMBIE_DEER; default -> null; }; }
}
