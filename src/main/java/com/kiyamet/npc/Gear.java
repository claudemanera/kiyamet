package com.kiyamet.npc;

/** Extra look layer (second int): headwear, mask, accessory and their colour. */
public record Gear(int headwear, int mask, int accessory, int color) {
    public static final Gear NONE = new Gear(0, 0, 0, 0);
    public static final int HEADWEAR = 16, MASKS = 16, ACCESSORIES = 8, COLORS = 16;

    public static final String[] HEADWEAR_NAMES = {"None", "Baseball cap", "Cowboy hat", "Army helmet", "Firefighter helmet", "Police cap", "Headband",
            "Hard hat", "Crown", "Hood", "Beret", "Goggles (forehead)", "Bucket hat", "Head wrap", "Riot helmet", "Headphones"};
    public static final String[] MASK_NAMES = {"None", "Gas mask", "Hockey mask", "Medical mask", "Skull bandana", "Balaclava", "Respirator",
            "Welding mask", "Bandit bandana", "Sunglasses", "Aviator goggles", "Skull face paint", "Leather mask", "Bone mask", "Desert wrap", "Cyber visor"};
    public static final String[] ACCESSORY_NAMES = {"None", "Scarf", "Bandolier", "Backpack straps", "Necklace", "Dog tags", "Holster belt", "Tool belt"};

    public int pack() { return (headwear & 15) | (mask & 15) << 4 | (accessory & 7) << 8 | (color & 15) << 11; }

    public static Gear unpack(int v) { return new Gear(v & 15, v >> 4 & 15, v >> 8 & 7, v >> 11 & 15); }

    public Gear with(int row, int value) {
        return switch (row) {
            case 0 -> new Gear(value, mask, accessory, color);
            case 1 -> new Gear(headwear, value, accessory, color);
            case 2 -> new Gear(headwear, mask, value, color);
            default -> new Gear(headwear, mask, accessory, value);
        };
    }
}
