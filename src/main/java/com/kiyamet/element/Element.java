package com.kiyamet.element;

import net.minecraft.ChatFormatting;

/** TWD 2.0: the 14 elements, their ancient weapons, temples, bosses, guardians and sages. */
public enum Element {
    //      id           title        weapon id               weapon title               dmg   speed  color     fmt                     power
    FLAME   ("flame",    "Flame",     "flame_sword",          "Flame Sword",              10, -2.4f, 0xFF5A1E, ChatFormatting.RED,          "Flamethrower: a stream of fire from the blade"),
    LIGHTNING("lightning","Lightning","lightning_mace",       "Lightning Mace",           13, -3.0f, 0xFFE24A, ChatFormatting.YELLOW,       "Calls lightning down on whatever you look at"),
    EARTH   ("earth",    "Earth",     "earth_axe",            "Earth Axe",                14, -3.0f, 0x7BA23F, ChatFormatting.DARK_GREEN,   "Shatters a 6x6 area where you look"),
    SHADOW  ("shadow",   "Shadow",    "shadow_katana",        "Shadow Katana",            10, -2.0f, 0x6B2FB5, ChatFormatting.DARK_PURPLE,  "You turn invisible - enemies lose you"),
    SMOKE   ("smoke",    "Smoke",     "smoke_sai",            "Smoke Sai",                 8, -1.6f, 0x9A9A9A, ChatFormatting.GRAY,         "A choking cloud: everything around you suffocates"),
    SPIRIT  ("spirit",   "Spirit",    "spirit_spear",         "Spirit Spear",             11, -2.6f, 0x7FF0E8, ChatFormatting.AQUA,         "Fly at four times flying speed"),
    ELECTRIC("electric", "Electric",  "electric_scythe",      "Electric Scythe",          12, -2.8f, 0x35C8FF, ChatFormatting.BLUE,         "Exploding lightning where you look"),
    THUNDER ("thunder",  "Thunder",   "thunder_hammer",       "Thunder Hammer",           15, -3.2f, 0x4D6BFF, ChatFormatting.DARK_BLUE,    "Super speed while time runs at half speed"),
    ICE     ("ice",      "Ice",       "ice_nunchaku",         "Ice Nunchaku",              8, -1.6f, 0xA8E8FF, ChatFormatting.AQUA,         "Freezes time - only you can move"),
    FIRE    ("fire",     "Fire",      "fire_club",            "Fire Club",                12, -2.8f, 0xFF9A1A, ChatFormatting.GOLD,         "Throws fireballs where you look"),
    WATER   ("water",    "Water",     "water_shuriken",       "Water Shuriken",            7, -1.2f, 0x2E7BFF, ChatFormatting.BLUE,         "Super jumps, and full speed in water"),
    AIR     ("air",      "Air",       "air_kunai",            "Air Kunai",                 7, -1.4f, 0xE8F6FF, ChatFormatting.WHITE,        "Rise into the sky and fly"),
    WIND    ("wind",     "Wind",      "wind_triple_scythe",   "Wind Triple Scythe",       12, -2.8f, 0x8EF0A0, ChatFormatting.GREEN,        "Time runs twice as fast"),
    METAL   ("metal",    "Metal",     "metal_claws",          "Metal Claws",               9, -1.8f, 0xB8C2CC, ChatFormatting.WHITE,        "You take no damage at all");

    public final String id, title, weapon, weaponTitle, power;
    public final int damage, color;
    public final float speed;
    public final ChatFormatting fmt;

    Element(String id, String title, String weapon, String weaponTitle, int damage, float speed, int color, ChatFormatting fmt, String power) {
        this.id = id; this.title = title; this.weapon = weapon; this.weaponTitle = weaponTitle; this.damage = damage; this.speed = speed; this.color = color; this.fmt = fmt; this.power = power;
    }

    public String core() { return id + "_core"; }
    public String boss() { return id + "_lord"; }
    public String guardian() { return id + "_guardian"; }
    public String sage() { return id + "_sage"; }

    public static Element of(String id) { for (Element e : values()) if (e.id.equals(id)) return e; return null; }
}
