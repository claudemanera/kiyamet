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
    FROST_GIANT    ("frost_giant",     "Frost Giant",     1000, 22, 14, 0.24, 2.0f, 1.38f, 2.5f, BossEvent.BossBarColor.WHITE,  false, new Ability[]{FREEZE, SLAM, LEAP_SLAM}, "ruby:16,metal_plate:8,energy_core:1"),
    // 2.0: the fourteen element lords, each waiting at the bottom of its temple
    FLAME_LORD("flame_lord", "Flame Lord", 900, 22, 12, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.RED, true, new Ability[]{FIRE_RING, FIREBALL, CHARGE}, "ancient_rod:1,ruby:12,energy_core:2"),
    LIGHTNING_LORD("lightning_lord", "Lightning Lord", 900, 20, 12, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.YELLOW, false, new Ability[]{LIGHTNING, LEVITATE, BEAM}, "ancient_rod:1,ruby:12,energy_core:2"),
    EARTH_LORD("earth_lord", "Earth Lord", 1100, 24, 20, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.GREEN, false, new Ability[]{SLAM, SHOCKWAVE, LEAP_SLAM}, "ancient_rod:1,ruby:12,energy_core:2"),
    SHADOW_LORD("shadow_lord", "Shadow Lord", 850, 24, 10, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.PURPLE, false, new Ability[]{SHADOW_CLOAK, TELEPORT_BEHIND, DARKNESS}, "ancient_rod:1,ruby:12,energy_core:2"),
    SMOKE_LORD("smoke_lord", "Smoke Lord", 850, 20, 10, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.WHITE, false, new Ability[]{TOXIC_BURST, DARKNESS, CURSE}, "ancient_rod:1,ruby:12,energy_core:2"),
    SPIRIT_LORD("spirit_lord", "Spirit Lord", 900, 20, 12, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.BLUE, false, new Ability[]{SOUL_DRAIN, LEVITATE, SUMMON_GUARDS}, "ancient_rod:1,ruby:12,energy_core:2"),
    ELECTRIC_LORD("electric_lord", "Electric Lord", 950, 22, 14, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.BLUE, false, new Ability[]{BEAM, LIGHTNING, SHOCKWAVE}, "ancient_rod:1,ruby:12,energy_core:2"),
    THUNDER_LORD("thunder_lord", "Thunder Lord", 1000, 26, 16, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.BLUE, false, new Ability[]{CHARGE, LIGHTNING, TIME_SLOW}, "ancient_rod:1,ruby:12,energy_core:2"),
    ICE_LORD("ice_lord", "Ice Lord", 1000, 22, 16, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.WHITE, false, new Ability[]{FREEZE, TIME_SLOW, SLAM}, "ancient_rod:1,ruby:12,energy_core:2"),
    FIRE_LORD("fire_lord", "Fire Lord", 950, 24, 14, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.RED, true, new Ability[]{FIREBALL, FIRE_RING, SLAM}, "ancient_rod:1,ruby:12,energy_core:2"),
    WATER_LORD("water_lord", "Water Lord", 950, 20, 12, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.BLUE, false, new Ability[]{PULL, SPIT, REGEN}, "ancient_rod:1,ruby:12,energy_core:2"),
    AIR_LORD("air_lord", "Air Lord", 850, 20, 10, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.WHITE, false, new Ability[]{LEVITATE, TELEPORT_BEHIND, ARROW_BARRAGE}, "ancient_rod:1,ruby:12,energy_core:2"),
    WIND_LORD("wind_lord", "Wind Lord", 900, 22, 12, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.GREEN, false, new Ability[]{PULL, SHOCKWAVE, CHARGE}, "ancient_rod:1,ruby:12,energy_core:2"),
    METAL_LORD("metal_lord", "Metal Lord", 1200, 24, 24, 0.27, 2.0f, 1.0f, 2.3f, BossEvent.BossBarColor.WHITE, false, new Ability[]{SLAM, REGEN, SHOCKWAVE}, "ancient_rod:1,ruby:12,energy_core:2"),
    // 2.2: the five masters of the End structures
    VOID_TYRANT("void_tyrant", "The Void Tyrant", 1500, 26, 16, 0.3, 2.0f, 0.9f, 3.0f, BossEvent.BossBarColor.PURPLE, false, new Ability[]{TELEPORT_BEHIND, DARKNESS, SHOCKWAVE}, "dragon_stone:1,void_shard:6,ruby:20,energy_core:2"),
    SKY_HERALD("sky_herald", "The Sky Herald", 1300, 22, 12, 0.3, 2.0f, 0.9f, 2.5f, BossEvent.BossBarColor.WHITE, false, new Ability[]{LEVITATE, LIGHTNING, ARROW_BARRAGE}, "void_shard:5,ruby:18,energy_core:2"),
    DEAD_CAPTAIN("dead_captain", "The Dead Captain", 1400, 24, 14, 0.28, 2.0f, 1.0f, 2.4f, BossEvent.BossBarColor.RED, false, new Ability[]{CHARGE, SUMMON_GUARDS, BLEED}, "void_shard:5,ruby:24,energy_core:2"),
    HIVE_MATRIARCH("hive_matriarch", "The Hive Matriarch", 1600, 22, 14, 0.26, 2.0f, 1.2f, 2.8f, BossEvent.BossBarColor.PINK, false, new Ability[]{SUMMON_CRAWLERS, TOXIC_BURST, TELEPORT_BEHIND}, "void_shard:6,ruby:20,energy_core:3"),
    STAR_WARDEN("star_warden", "The Star Warden", 1700, 26, 20, 0.27, 2.0f, 1.0f, 2.6f, BossEvent.BossBarColor.BLUE, false, new Ability[]{BEAM, LEVITATE, SHOCKWAVE}, "star_stone:1,void_shard:6,ruby:20,energy_core:3");

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
