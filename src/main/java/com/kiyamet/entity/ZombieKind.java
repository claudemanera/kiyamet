package com.kiyamet.entity;

/**
* The 16 zombie types (incl. female walkers). Speed tiers (Walking Dead style): most are slow walkers (0.14-0.18),
 * police/screamer/alpha walk at vanilla zombie speed (0.23-0.24), only runner and hunter are fast (0.33-0.34).
 * The 14 zombie types. minPower = Total Power needed before this kind starts to spawn (zombie evolution).
 * Base stats are for Total Power 0; Difficulty scales them up to x3 at Total Power 8000, then stays flat.
 */
public enum ZombieKind {
    //          id                 hp    speed  dmg   armor  w     h     scale  weight minPower
    RUNNER   ("runner_zombie",    18,   0.34,  3.0,  0,    0.6f, 1.95f, 1.0f, 12, 0),
    TANK     ("tank_zombie",      70,   0.15,  9.0,  6,    1.1f, 2.6f,  1.4f, 8,  0),
    SCREAMER ("screamer_zombie",  16,   0.23,  2.0,  0,    0.6f, 1.95f, 1.0f, 10, 0),
    TOXIC    ("toxic_zombie",     22,   0.17,  3.0,  0,    0.6f, 1.95f, 1.0f, 16, 0),
    ARMORED  ("armored_zombie",   30,   0.16,  5.0,  10,   0.6f, 1.95f, 1.0f, 10, 0),
    LISTENER ("listener_zombie",  26,   0.18,  6.0,  2,    0.6f, 1.95f, 1.0f, 8,  0),
    EXPLODER ("exploder_zombie",  20,   0.17,  2.0,  0,    0.7f, 1.95f, 1.0f, 8,  0),
    FARMER   ("farmer_zombie",    20,   0.17,  3.0,  0,    0.6f, 1.95f, 1.0f, 40, 0),
    DOCTOR   ("doctor_zombie",    20,   0.17,  3.0,  0,    0.6f, 1.95f, 1.0f, 18, 0),
    POLICE   ("police_zombie",    24,   0.23,  4.0,  4,    0.6f, 1.95f, 1.0f, 14, 0),
    HUNTER   ("hunter_zombie",    24,   0.33,  5.0,  0,    0.6f, 1.95f, 1.0f, 12, 4000),
    CRAWLER  ("crawler_zombie",   16,   0.14,  4.0,  0,    0.8f, 0.7f,  1.0f, 12, 4000),
    ALPHA    ("alpha_zombie",     60,   0.24,  8.0,  6,    0.8f, 2.3f,  1.2f, 4,  8000),
    MUTANT   ("mutant_zombie",    90,   0.18,  12.0, 4,    1.2f, 2.8f,  1.5f, 3,  8000),
    WOMAN    ("woman_zombie",     20,   0.17,  3.0,  0,    0.6f, 1.95f, 1.0f, 34, 0),
    NURSE    ("nurse_zombie",     20,   0.17,  3.0,  0,    0.6f, 1.95f, 1.0f, 12, 0);

    public final String id;
    public final double health, speed, damage, armor;
    public final float width, height, scale;
    public final int weight, minPower;

    ZombieKind(String id, double hp, double speed, double dmg, double armor, float w, float h, float scale, int weight, int minPower) {
        this.id = id; this.health = hp; this.speed = speed; this.damage = dmg; this.armor = armor;
        this.width = w; this.height = h; this.scale = scale; this.weight = weight; this.minPower = minPower;
    }
}
