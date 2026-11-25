package com.kiyamet.entity;

/**
 * The 14 zombie types. minPower = Total Power needed before this kind starts to spawn (zombie evolution).
 * Base stats are for Total Power 0; Difficulty scales them up to x3 at Total Power 8000, then stays flat.
 */
public enum ZombieKind {
    //          id                 hp    speed  dmg   armor  w     h     scale  weight minPower
    RUNNER   ("runner_zombie",    18,   0.33,  3.0,  0,    0.6f, 1.95f, 1.0f, 40, 0),
    TANK     ("tank_zombie",      70,   0.18,  9.0,  6,    1.1f, 2.6f,  1.4f, 8,  0),
    SCREAMER ("screamer_zombie",  16,   0.26,  2.0,  0,    0.6f, 1.95f, 1.0f, 10, 0),
    TOXIC    ("toxic_zombie",     22,   0.22,  3.0,  0,    0.6f, 1.95f, 1.0f, 14, 0),
    ARMORED  ("armored_zombie",   30,   0.21,  5.0,  10,   0.6f, 1.95f, 1.0f, 10, 0),
    LISTENER ("listener_zombie",  26,   0.25,  6.0,  2,    0.6f, 1.95f, 1.0f, 8,  0),
    EXPLODER ("exploder_zombie",  20,   0.24,  2.0,  0,    0.7f, 1.95f, 1.0f, 8,  0),
    FARMER   ("farmer_zombie",    20,   0.23,  3.0,  0,    0.6f, 1.95f, 1.0f, 30, 0),
    DOCTOR   ("doctor_zombie",    20,   0.23,  3.0,  0,    0.6f, 1.95f, 1.0f, 14, 0),
    POLICE   ("police_zombie",    24,   0.24,  4.0,  4,    0.6f, 1.95f, 1.0f, 14, 0),
    HUNTER   ("hunter_zombie",    24,   0.30,  5.0,  0,    0.6f, 1.95f, 1.0f, 12, 4000),
    CRAWLER  ("crawler_zombie",   16,   0.20,  4.0,  0,    0.8f, 0.7f,  1.0f, 12, 4000),
    ALPHA    ("alpha_zombie",     60,   0.27,  8.0,  6,    0.8f, 2.3f,  1.2f, 4,  8000),
    MUTANT   ("mutant_zombie",    90,   0.22,  12.0, 4,    1.2f, 2.8f,  1.5f, 3,  8000);

    public final String id;
    public final double health, speed, damage, armor;
    public final float width, height, scale;
    public final int weight, minPower;

    ZombieKind(String id, double hp, double speed, double dmg, double armor, float w, float h, float scale, int weight, int minPower) {
        this.id = id; this.health = hp; this.speed = speed; this.damage = dmg; this.armor = armor;
        this.width = w; this.height = h; this.scale = scale; this.weight = weight; this.minPower = minPower;
    }
}
