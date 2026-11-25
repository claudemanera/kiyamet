package com.kiyamet.npc;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Character creator for a companion: each row cycles one feature (◀ / ▶). The companion updates live. */
public class AppearanceMenu extends PanelMenu {
    private final HumanEntity h;
    private static final String[] ROWS = {"Skin tone", "Hair style", "Hair colour", "Eyes", "Beard", "Face detail", "Outfit", "Outfit colour",
            "Headwear", "Mask", "Accessory", "Gear colour"};
    // rows 1-4 hold three features each (cols 0-2, 3-5, 6-8)
    private static final int[][] LAYOUT = {{9, 0}, {12, 1}, {15, 2}, {18, 3}, {21, 4}, {24, 5}, {27, 6}, {30, 7}, {33, 11}, {36, 8}, {39, 9}, {42, 10}};

    public static void open(ServerPlayer p, HumanEntity h) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new AppearanceMenu(id, inv, p, h), Component.literal("Appearance: " + h.getName().getString())));
    }

    private AppearanceMenu(int id, Inventory inv, ServerPlayer p, HumanEntity h) {
        super(id, inv, p);
        this.h = h;
        redraw();
    }

    private static int count(int row) {
        return switch (row) {
            case 0 -> Appearance.SKINS; case 1 -> Appearance.HAIRS; case 2 -> Appearance.HAIR_COLORS; case 3 -> 4; case 4 -> 4;
            case 5 -> Appearance.DETAILS; case 6 -> Appearance.OUTFITS; case 7 -> Appearance.OUTFIT_COLORS;
            case 8 -> Gear.HEADWEAR; case 9 -> Gear.MASKS; case 10 -> Gear.ACCESSORIES; default -> Gear.COLORS;
        };
    }

    private int value(Appearance a, int row) {
        Gear g = h.gear();
        return switch (row) {
            case 8 -> g.headwear(); case 9 -> g.mask(); case 10 -> g.accessory(); case 11 -> g.color();
            case 0 -> a.skin(); case 1 -> a.hair(); case 2 -> a.hairColor(); case 3 -> a.eyes(); case 4 -> a.beard();
            case 5 -> a.detail(); case 6 -> a.outfit(); default -> a.outfitColor();
        };
    }

    private static String label(int row, int v) {
        return switch (row) {
            case 0 -> Appearance.SKIN_NAMES[v]; case 1 -> Appearance.HAIR_NAMES[v]; case 2 -> Appearance.HAIR_COLOR_NAMES[v];
            case 3 -> Appearance.EYE_NAMES[v]; case 4 -> Appearance.BEARD_NAMES[v]; case 5 -> Appearance.DETAIL_NAMES[v];
            case 6 -> Appearance.OUTFIT_NAMES[v]; case 7 -> Appearance.OUTFIT_COLOR_NAMES[v];
            case 8 -> Gear.HEADWEAR_NAMES[v]; case 9 -> Gear.MASK_NAMES[v]; case 10 -> Gear.ACCESSORY_NAMES[v]; default -> Appearance.OUTFIT_COLOR_NAMES[v];
        };
    }

    private static Item icon(int row) {
        return switch (row) {
            case 0 -> Items.PLAYER_HEAD; case 1 -> Items.SHEARS; case 2 -> Items.BROWN_DYE; case 3 -> Items.ENDER_EYE; case 4 -> Items.STRING;
            case 5 -> Items.SPYGLASS; case 6 -> Items.LEATHER_CHESTPLATE; case 7 -> Items.RED_DYE;
            case 8 -> Items.LEATHER_HELMET; case 9 -> Items.CARVED_PUMPKIN; case 10 -> Items.LEAD; default -> Items.BLUE_DYE;
        };
    }

    private static Appearance with(Appearance a, int row, int v) {
        return with0(a, row, v).withFemale(a.female());
    }

    private static Appearance with0(Appearance a, int row, int v) {
        return switch (row) {
            case 0 -> new Appearance(v, a.hair(), a.hairColor(), a.face(), a.detail(), a.outfit(), a.outfitColor(), a.mask());
            case 1 -> new Appearance(a.skin(), v, a.hairColor(), a.face(), a.detail(), a.outfit(), a.outfitColor(), a.mask());
            case 2 -> new Appearance(a.skin(), a.hair(), v, a.face(), a.detail(), a.outfit(), a.outfitColor(), a.mask());
            case 3 -> new Appearance(a.skin(), a.hair(), a.hairColor(), (a.face() & 12) | v, a.detail(), a.outfit(), a.outfitColor(), a.mask());
            case 4 -> new Appearance(a.skin(), a.hair(), a.hairColor(), (a.face() & 3) | v << 2, a.detail(), a.outfit(), a.outfitColor(), a.mask());
            case 5 -> new Appearance(a.skin(), a.hair(), a.hairColor(), a.face(), v, a.outfit(), a.outfitColor(), a.mask());
            case 6 -> new Appearance(a.skin(), a.hair(), a.hairColor(), a.face(), a.detail(), v, a.outfitColor(), a.mask());
            default -> new Appearance(a.skin(), a.hair(), a.hairColor(), a.face(), a.detail(), a.outfit(), v, a.mask());
        };
    }

    @Override
    protected void draw() {
        Appearance a = h.appearance();
        c.setItem(4, Ui.glint(Ui.icon(Items.ARMOR_STAND, Ui.text("Character creator", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Ui.text("◀ left arrow / right arrow ▶ to change", ChatFormatting.GRAY), Ui.text("Look at your companion to see it live", ChatFormatting.GRAY)), true));
        for (int[] l : LAYOUT) {
            int slot = l[0], row = l[1], v = value(a, row), n = count(row);
            c.setItem(slot, Ui.icon(Items.ARROW, Ui.text("◀ Previous", ChatFormatting.YELLOW)));
            var ic = Ui.icon(icon(row), Ui.text(ROWS[row] + ": " + label(row, v), ChatFormatting.WHITE, ChatFormatting.BOLD),
                    Ui.text((v + 1) + " / " + n, ChatFormatting.GRAY), Ui.bar((v + 1) / (double) n, 12));
            c.setItem(slot + 1, ic);
            c.setItem(slot + 2, Ui.icon(Items.SPECTRAL_ARROW, Ui.text("Next ▶", ChatFormatting.YELLOW)));
        }
        c.setItem(45, Ui.icon(Items.ARROW, Ui.text("Back", ChatFormatting.YELLOW)));
        c.setItem(47, Ui.icon(a.female() ? Items.PINK_DYE : Items.LIGHT_BLUE_DYE, Ui.text("Body: " + (a.female() ? "Woman" : "Man"), ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), Ui.text("Click to switch", ChatFormatting.GRAY)));
        c.setItem(51, Ui.icon(Items.BUCKET, Ui.text("Remove all gear", ChatFormatting.GRAY)));
        c.setItem(49, Ui.icon(Items.ENCHANTED_BOOK, Ui.text("Randomize everything", ChatFormatting.AQUA)));
        c.setItem(53, Ui.icon(Items.LIME_CONCRETE, Ui.text("Done", ChatFormatting.GREEN)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (!h.isAlive() || !h.isOwnedBy(player)) { player.closeContainer(); return false; }
        if (slot == 45) { CompanionMenu.open(player, h); return false; }
        if (slot == 53) { player.closeContainer(); return false; }
        if (slot == 49) {
            h.setAppearance(Appearance.random(h.getRandom(), Faction.SURVIVOR, Role.MEMBER, h.appearance().female()));
            h.setGear(new Gear(h.getRandom().nextInt(3) == 0 ? 0 : h.getRandom().nextInt(Gear.HEADWEAR), h.getRandom().nextInt(3) == 0 ? h.getRandom().nextInt(Gear.MASKS) : 0,
                    h.getRandom().nextInt(Gear.ACCESSORIES), h.getRandom().nextInt(Gear.COLORS)));
            return true;
        }
        if (slot == 51) { h.setGear(Gear.NONE); return true; }
        if (slot == 47) { h.setAppearance(h.appearance().withFemale(!h.appearance().female())); return true; }
        for (int[] l : LAYOUT) {
            int row = l[1], n = count(row);
            if (slot == l[0] || slot == l[0] + 2) {
                Appearance a = h.appearance();
                int v = (value(a, row) + (slot == l[0] ? n - 1 : 1)) % n;
                if (row >= 8) h.setGear(h.gear().with(row - 8, v));
                else h.setAppearance(with(a, row, v));
                return true;
            }
        }
        return false;
    }
}
