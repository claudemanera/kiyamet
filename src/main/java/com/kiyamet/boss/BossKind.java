package com.kiyamet.boss;

import net.minecraft.world.BossEvent;

import static com.kiyamet.boss.Ability.*;

/** The 15 bosses. Sizes are model size x scale (bosses are giants). */
public enum BossKind {
    PATIENT_ZERO   ("patient_zero",    "Patient Zero",    400, 12, 6,  0.26, 2.0f, 0.88f, 2.44f, BossEvent.BossBarColor.GREEN,  false, new Ability[]{INFECT_AURA, SUMMON_DOCTORS, TOXIC_BURST}, "vaccine:4,ruby:6,tech_core:1"),
    THE_BUTCHER    ("the_butcher",     "The Butcher",     450, 18, 8,  0.27, 2.0f, 1.2f,  2.12f, BossEvent.BossBarColor.RED,    false, new Ability[]{CHARGE, BLEED, SLAM}, "ruby:8,metal_plate:6,medkit:2"),
    EXPERIMENT_47  ("experiment_47",   "Experiment 47",   600, 16, 10, 0.28, 2.0f, 0.88f, 2.5f,  BossEvent.BossBarColor.PURPLE, false, new Ability[]{LEAP_SLAM, REGEN, CHARGE}, "energy_core:2,tech_core:2,ruby:8"),
    FORGOTTEN_KING ("forgotten_king",  "The Forgotten King", 700, 16, 12, 0.24, 2.0f, 1.2f, 2.69f, BossEvent.BossBarColor.YELLOW, false, new Ability[]{SUMMON_GUARDS, SOUL_DRAIN, DARKNESS}, "ruby:12,void_shard:2,energy_core:1"),
    HIVE_MOTHER    ("hive_mother",     "Hive Mother",     800, 14, 8,  0.22, 2.5f, 1.12f, 1.56f, BossEvent.BossBarColor.GREEN,  false, new Ability[]{SUMMON_CRAWLERS, TOXIC_BURST, SPIT}, "ruby:10,vaccine:3,energy_core:1"),
    LAVA_GOLEM     ("lava_golem",      "Lava Golem",      750, 20, 14, 0.22, 2.0f, 1.4f,  1.62f, BossEvent.BossBarColor.RED,    true,  new Ability[]{FIRE_RING, SLAM, FIREBALL}, "ruby:12,metal_plate:10,energy_core:1"),
    TIME_KEEPER    ("time_keeper",     "The Time Keeper", 650, 15, 10, 0.27, 2.0f, 1.2f,  2.19f, BossEvent.BossBarColor.BLUE,   false, new Ability[]{TIME_SLOW, TELEPORT_BEHIND, BEAM}, "energy_core:2,lens:4,ruby:10"),
    CORE_WARDEN    ("core_warden",     "Core Warden",     1000, 22, 16, 0.24, 2.0f, 1.4f, 2.44f, BossEvent.BossBarColor.PURPLE, true,  new Ability[]{SHOCKWAVE, LEVITATE, BEAM}, "void_shard:4,energy_core:2,ruby:14"),
    STORM_LORD     ("storm_lord",      "Storm Lord",      700, 16, 10, 0.27, 2.0f, 1.06f, 2.56f, BossEvent.BossBarColor.BLUE,   false, new Ability[]{LIGHTNING, LEVITATE, TELEPORT_BEHIND}, "energy_cell:6,energy_core:1,ruby:10"),
    SWAMP_WITCH    ("swamp_witch",     "Swamp Witch",     500, 12, 6,  0.26, 2.0f, 0.94f, 2.69f, BossEvent.BossBarColor.GREEN,  false, new Ability[]{CURSE, SUMMON_TOXIC, REGEN}, "vaccine:2,ruby:8,lens:2"),
    SHADOW_STALKER ("shadow_stalker",  "Shadow Stalker",  550, 18, 6,  0.32, 2.0f, 0.75f, 2.5f,  BossEvent.BossBarColor.WHITE,  false, new Ability[]{SHADOW_CLOAK, TELEPORT_BEHIND, DARKNESS}, "void_shard:2,ruby:10"),
    THE_GENERAL    ("the_general",     "The General",     900, 18, 18, 0.25, 2.0f, 1.19f, 2.31f, BossEvent.BossBarColor.RED,    false, new Ability[]{SUMMON_SOLDIERS, ARROW_BARRAGE, CHARGE}, "metal_plate:12,tech_core:3,ruby:14"),
    DEEP_HORROR    ("deep_horror",     "The Deep Horror", 1100, 20, 12, 0.24, 2.5f, 0.9f, 2.38f, BossEvent.BossBarColor.BLUE,   false, new Ability[]{PULL, SLAM, TOXIC_BURST}, "void_shard:3,energy_core:2,ruby:16"),
    ASSEMBLY_MOTHER("assembly_mother", "Assembly Mother", 1000, 16, 18, 0.22, 2.0f, 1.12f, 2.06f, BossEvent.BossBarColor.YELLOW, false, new Ability[]{SUMMON_ROBOTS, BEAM, SHOCKWAVE}, "circuit:12,tech_core:4,energy_core:2"),
    FROST_GIANT    ("frost_giant",     "Frost Giant",     1000, 22, 14, 0.24, 2.0f, 1.38f, 2.5f, BossEvent.BossBarColor.WHITE,  false, new Ability[]{FREEZE, SLAM, LEAP_SLAM}, "ruby:16,metal_plate:8,energy_core:1");

    public final String id, title, drops;
    public final double health, damage, armor, speed;
    public final float scale, width, height;
    public final BossEvent.BossBarColor color;
    public final boolean fireImmune;
    public final Ability[] abilities;

    BossKind(String id, String title, double health, double damage, double armor, double speed, float scale, float w, float h,
             BossEvent.BossBarColor color, boolean fireImmune, Ability[] abilities, String drops) {
        this.id = id; this.title = title; this.health = health; this.damage = damage; this.armor = armor; this.speed = speed;
        this.scale = scale; this.width = Math.min(w, 1.3f) * scale; this.height = h * scale;
        this.color = color; this.fireImmune = fireImmune; this.abilities = abilities; this.drops = drops;
    }

    /** XP granted to combat skills on a player's first kill of this boss. */
    public int firstKillXp() { return (int) (health * 40); }
}
