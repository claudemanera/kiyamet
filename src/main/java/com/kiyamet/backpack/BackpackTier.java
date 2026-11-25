package com.kiyamet.backpack;

/** The 10 backpacks, each bigger than the last. Upgrade by crafting the previous one with the upgrade material (contents are kept). */
public enum BackpackTier {
    CANVAS_SATCHEL("canvas_satchel", "Canvas Satchel", 6, 0xFF8A6A44, 0xFF5A4028, "minecraft:leather"),
    SCOUT_PACK("scout_pack", "Scout Pack", 9, 0xFF4E8A3A, 0xFF2E5A22, "minecraft:leather"),
    HIKER_PACK("hiker_pack", "Hiker's Pack", 12, 0xFF2E64B0, 0xFF1C3E72, "minecraft:iron_ingot"),
    SURVIVOR_PACK("survivor_pack", "Survivor Pack", 15, 0xFFE07A1E, 0xFF8E4A10, "minecraft:gold_ingot"),
    RANGER_PACK("ranger_pack", "Ranger Pack", 18, 0xFF6A7434, 0xFF3E4420, "kiyamet:metal_plate"),
    MEDIC_PACK("medic_pack", "Medic Pack", 24, 0xFFE8E8E4, 0xFFC02828, "kiyamet:medkit"),
    TACTICAL_PACK("tactical_pack", "Tactical Pack", 27, 0xFF2A2A2E, 0xFF14141A, "kiyamet:circuit"),
    MILITARY_RUCKSACK("military_rucksack", "Military Rucksack", 33, 0xFF5A6234, 0xFF3A3E20, "minecraft:diamond"),
    EXPEDITION_PACK("expedition_pack", "Expedition Pack", 39, 0xFF7A3AB0, 0xFF4A1E72, "kiyamet:energy_core"),
    APOCALYPSE_PACK("apocalypse_pack", "Apocalypse Pack", 45, 0xFFB01E2A, 0xFFE8C040, "minecraft:netherite_ingot");

    public final String id, title, upgrade;
    public final int capacity, color, accent;

    BackpackTier(String id, String title, int capacity, int color, int accent, String upgrade) {
        this.id = id; this.title = title; this.capacity = capacity; this.color = color; this.accent = accent; this.upgrade = upgrade;
    }

    public int rows() { return (capacity + 8) / 9; }

    public static BackpackTier byIndex(int i) { return i < 0 || i >= values().length ? null : values()[i]; }
}
