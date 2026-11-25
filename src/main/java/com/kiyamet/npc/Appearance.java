package com.kiyamet.npc;

import net.minecraft.util.RandomSource;

/**
 * A human's look packed in one int: skin tone, hair style + colour, face (eyes + beard), detail (scar, glasses...),
 * outfit + colour and the walker-skin mask. The client paints a 64x64 skin from it.
 */
public record Appearance(int skin, int hair, int hairColor, int face, int detail, int outfit, int outfitColor, boolean mask, boolean female) {
    public Appearance(int skin, int hair, int hairColor, int face, int detail, int outfit, int outfitColor, boolean mask) {
        this(skin, hair, hairColor, face, detail, outfit, outfitColor, mask, false);
    }

    public Appearance withFemale(boolean f) { return new Appearance(skin, hair, hairColor, f ? face & 3 : face, detail, outfit, outfitColor, mask, f); }

    public static final int SKINS = 12, HAIRS = 16, HAIR_COLORS = 12, FACES = 16, DETAILS = 8, OUTFITS = 32, OUTFIT_COLORS = 16;

    public static final String[] SKIN_NAMES = {"Porcelain", "Fair", "Light", "Light tan", "Medium", "Olive", "Tan", "Bronze", "Brown", "Dark brown", "Deep", "Ebony"};
    public static final String[] HAIR_NAMES = {"Bald", "Buzz cut", "Short", "Side part", "Long", "Ponytail", "Mohawk", "Curly", "Bun", "Afro",
            "Dreadlocks", "Slicked back", "Mullet", "Bob", "Beanie", "Cap"};
    public static final String[] HAIR_COLOR_NAMES = {"Black", "Dark brown", "Brown", "Light brown", "Blonde", "Platinum", "Ginger", "Auburn", "Gray", "White", "Dyed red", "Dyed blue"};
    public static final String[] EYE_NAMES = {"Brown", "Blue", "Green", "Gray"};
    public static final String[] BEARD_NAMES = {"Clean shaven", "Stubble", "Full beard", "Mustache"};
    public static final String[] DETAIL_NAMES = {"None", "Scar", "Eye patch", "Glasses", "Bandana", "Face paint", "Bandaged head", "Freckles"};
    public static final String[] OUTFIT_NAMES = {"T-shirt & jeans", "Flannel shirt", "Leather jacket", "Hoodie", "Sheriff uniform", "Police uniform",
            "Army camo", "Farmer overalls", "Prison jumpsuit", "Doctor coat", "Biker vest", "Business suit", "Tracksuit", "Tank top",
            "Poncho", "Scrap armor", "Hunting jacket", "Mechanic overalls", "Puffer jacket", "Rags", "Riot gear", "Denim jacket", "Sweater", "Chef whites",
            "Firefighter gear", "Royal guard armor", "Trucker flannel", "Teacher cardigan", "Hazmat suit", "Ghillie suit", "Nurse scrubs", "Prison stripes"};
    public static final String[] OUTFIT_COLOR_NAMES = {"Red", "Navy", "Forest green", "Black", "White", "Gray", "Brown", "Tan", "Olive",
            "Maroon", "Teal", "Orange", "Purple", "Yellow", "Sky blue", "Pink"};

    public Appearance withOutfit(int o, int oc) { return new Appearance(skin, hair, hairColor, face, detail, o, oc, mask, female); }
    public Appearance withDetail(int d) { return new Appearance(skin, hair, hairColor, face, d, outfit, outfitColor, mask, female); }
    public Appearance withHair(int h) { return new Appearance(skin, h, hairColor, face, detail, outfit, outfitColor, mask, female); }

    public int pack() {
        return (skin & 15) | (hair & 15) << 4 | (hairColor & 15) << 8 | (face & 15) << 12 | (detail & 7) << 16
                | (outfit & 31) << 19 | (outfitColor & 15) << 24 | (mask ? 1 : 0) << 28 | (female ? 1 : 0) << 29;
    }

    public static Appearance unpack(int v) {
        return new Appearance(v & 15, v >> 4 & 15, v >> 8 & 15, v >> 12 & 15, v >> 16 & 7, v >> 19 & 31, v >> 24 & 15, (v >> 28 & 1) == 1, (v >> 29 & 1) == 1);
    }

    public int eyes() { return face & 3; }
    public int beard() { return face >> 2 & 3; }

    private static int pick(RandomSource r, int... opts) { return opts[r.nextInt(opts.length)]; }

    /** Mostly light skin tones, some medium, a few dark. */
    public static int randomSkin(RandomSource r) {
        float x = r.nextFloat();
        return x < 0.6f ? r.nextInt(5) : x < 0.85f ? 5 + r.nextInt(3) : 8 + r.nextInt(4);
    }

    public static Appearance random(RandomSource r, Faction f, Role role) {
        int skin = randomSkin(r), hair = r.nextInt(HAIRS), hc = r.nextInt(10), face = r.nextInt(FACES);
        int detail = r.nextFloat() < 0.65f ? 0 : 1 + r.nextInt(DETAILS - 1);
        int oc = r.nextInt(OUTFIT_COLORS);
        int outfit = switch (f) {
            case RAIDER -> role == Role.COMMANDER ? 2 : pick(r, 2, 10, 15, 2, 10, 16, 20);
            case WHISPERER -> 19;
            case COMMUNITY -> switch (role) {
                case GUARD -> pick(r, 4, 5, 6, 20, 16);
                case TRADER -> pick(r, 7, 17, 22, 23, 1);
                case LEADER -> pick(r, 4, 11, 21, 1);
                default -> r.nextInt(OUTFITS);
            };
            default -> pick(r, 0, 1, 2, 3, 7, 12, 13, 14, 16, 17, 18, 21, 22);
        };
        if (f == Faction.RAIDER) oc = pick(r, 3, 6, 5, 9, 0);
        if (f == Faction.RAIDER && role == Role.COMMANDER) { hair = pick(r, 0, 1, 11); face = (face & 3) | 2 << 2; }
        return new Appearance(skin, hair, hc, face, detail, outfit, oc, f == Faction.WHISPERER);
    }

    private static final int[] FEMALE_HAIR = {4, 5, 8, 13, 7, 2, 3, 9, 10, 14, 15, 4, 5, 13};

    public static Appearance random(RandomSource r, Faction f, Role role, boolean female) {
        Appearance a = random(r, f, role);
        if (!female) return a;
        int hair = FEMALE_HAIR[r.nextInt(FEMALE_HAIR.length)];
        return new Appearance(a.skin, hair, a.hairColor, a.face & 3, a.detail == 1 || a.detail == 7 || a.detail == 0 ? a.detail : 0, a.outfit, a.outfitColor, a.mask, true);
    }
}
