package com.kiyamet.gunsmith;

import com.kiyamet.item.GunItem;
import com.kiyamet.npc.Shop;
import com.kiyamet.registry.ModItems;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.9: the Gunsmith Bench builds guns and ammo. Stronger guns cost more metal and need more Engineering. */
public class GunCraftMenu extends PanelMenu {
    public record Build(String id, int count, int eng, String... cost) {}
    static final List<Build> BUILDS = new ArrayList<>();
    static {
        for (String id : ModItems.WEAPONS) {
            GunItem.Stats st = GunItem.statsFor(id);
            int d = Math.round(st.damage());
            boolean arrow = id.contains("arrow") || id.equals("harpoon_gun") || id.equals("apocalypse_cannon");
            boolean energy = st.mode() == GunItem.Mode.TESLA || st.mode() == GunItem.Mode.VOID || st.mode() == GunItem.Mode.HOMING || st.mode() == GunItem.Mode.CANNON || id.contains("railgun") || id.contains("gatling");
            List<String> c = new ArrayList<>();
            if (arrow) { add(c, "minecraft:bow", 1); add(c, "minecraft:string", 2 + d / 10); add(c, "minecraft:iron_ingot", 2 + d / 4); }
            else { add(c, "minecraft:iron_ingot", 5 + d / 2); add(c, "minecraft:oak_planks", 2); }
            add(c, "kiyamet:metal_plate", 1 + d / 8);
            if (id.startsWith("golden")) add(c, "minecraft:gold_ingot", 10);
            if (energy) { add(c, "kiyamet:circuit", 2); add(c, "kiyamet:energy_core", 1); }
            else if (d >= 25) add(c, "minecraft:diamond", 1 + d / 30);
            if (id.contains("sniper") || id.contains("marksman") || id.contains("longshot") || id.contains("hunting")) add(c, "kiyamet:lens", 1);
            BUILDS.add(new Build(id, 1, Math.min(400, d * 6), c.toArray(new String[0])));
        }
        BUILDS.add(new Build("kiyamet:pistol_ammo", 16, 0, "minecraft:iron_nugget", "4", "minecraft:gunpowder", "1"));
        BUILDS.add(new Build("kiyamet:rifle_ammo", 16, 0, "minecraft:iron_nugget", "6", "minecraft:gunpowder", "2"));
        BUILDS.add(new Build("kiyamet:shotgun_shell", 8, 0, "minecraft:iron_nugget", "3", "minecraft:gunpowder", "2", "minecraft:paper", "1"));
        BUILDS.add(new Build("kiyamet:sniper_ammo", 8, 10, "minecraft:iron_ingot", "1", "minecraft:gunpowder", "2"));
        BUILDS.add(new Build("kiyamet:rocket_ammo", 2, 40, "minecraft:iron_ingot", "2", "minecraft:gunpowder", "4"));
        BUILDS.add(new Build("kiyamet:plasma_cell", 4, 60, "kiyamet:energy_cell", "1", "minecraft:redstone", "4"));
        BUILDS.add(new Build("minecraft:arrow", 16, 0, "minecraft:flint", "2", "minecraft:stick", "2", "minecraft:feather", "2"));
    }
    static void add(List<String> c, String id, int n) { c.add(id); c.add(String.valueOf(n)); }

    static final int[] SLOTS = new int[36];
    static { for (int i = 0; i < 36; i++) SLOTS[i] = 9 + i; }
    private final int page;

    public static void open(ServerPlayer p, int page) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GunCraftMenu(id, inv, p, page), Component.literal("Gunsmith Bench - Build")));
    }

    private GunCraftMenu(int id, Inventory inv, ServerPlayer p, int page) { super(id, inv, p); this.page = page; redraw(); }

    static String id(Build b) { return b.id().contains(":") ? b.id() : "kiyamet:" + b.id(); }
    int eng() { return SkillManager.get(player).level(Skill.ENGINEERING); }
    boolean can(Build b) {
        if (player.isCreative()) return true;
        if (eng() < b.eng()) return false;
        for (int i = 0; i < b.cost().length; i += 2) if (Shop.count(player, b.cost()[i]) < Integer.parseInt(b.cost()[i + 1])) return false;
        return true;
    }

    @Override
    protected void draw() {
        c.setItem(4, Ui.icon(Ui.item("kiyamet:gun_bench"), Ui.text("Build Guns & Ammo", ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text("Page " + (page + 1) + " / " + pages(), ChatFormatting.GRAY)));
        for (int i = 0; i < 36; i++) {
            int n = page * 36 + i;
            if (n >= BUILDS.size()) break;
            Build b = BUILDS.get(n);
            List<Component> lore = new ArrayList<>();
            if (!b.id().contains(":")) { GunItem.Stats st = GunItem.statsFor(b.id()); lore.add(Ui.text("Damage " + Math.round(st.damage()) + " · " + st.mode().name().toLowerCase(), ChatFormatting.AQUA)); }
            if (b.eng() > 0) lore.add(Ui.text("Needs Engineering " + b.eng(), eng() >= b.eng() || player.isCreative() ? ChatFormatting.GRAY : ChatFormatting.RED));
            for (int j = 0; j < b.cost().length; j += 2) {
                int need = Integer.parseInt(b.cost()[j + 1]), have = Shop.count(player, b.cost()[j]);
                lore.add(Component.literal((have >= need ? "✔ " : "✘ ") + need + "× ").append(new ItemStack(Ui.item(b.cost()[j])).getHoverName()).withStyle(have >= need ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
            boolean ok = can(b);
            lore.add(Ui.text(ok ? "Click to build" + (b.count() > 1 ? " (" + b.count() + ")" : "") : "Not yet", ok ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
            c.setItem(SLOTS[i], Ui.glint(Ui.lore(new ItemStack(Ui.item(id(b)), b.count()), lore), ok));
        }
        if (page > 0) c.setItem(45, Ui.icon(Items.ARROW, Ui.text("◀ Previous page", ChatFormatting.WHITE)));
        if (page < pages() - 1) c.setItem(53, Ui.icon(Items.ARROW, Ui.text("Next page ▶", ChatFormatting.WHITE)));
        c.setItem(48, Ui.icon(Items.ANVIL, Ui.text("◀ Back to parts & repair", ChatFormatting.AQUA)));
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    static int pages() { return (BUILDS.size() + 35) / 36; }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (slot == 49) { player.closeContainer(); return false; }
        if (slot == 48) { GunBenchMenu.open(player); return false; }
        if (slot == 45 && page > 0) { open(player, page - 1); return false; }
        if (slot == 53 && page < pages() - 1) { open(player, page + 1); return false; }
        if (slot < 9 || slot >= 45) return false;
        int n = page * 36 + slot - 9;
        if (n >= BUILDS.size()) return false;
        Build b = BUILDS.get(n);
        if (!can(b)) { player.displayClientMessage(Component.literal(eng() < b.eng() ? "Needs Engineering " + b.eng() : "Missing materials").withStyle(ChatFormatting.RED), true); return false; }
        if (!player.isCreative()) for (int i = 0; i < b.cost().length; i += 2) Shop.take(player, b.cost()[i], Integer.parseInt(b.cost()[i + 1]));
        Shop.give(player, new ItemStack(Ui.item(id(b)), b.count()));
        SkillManager.addXp(player, Skill.ENGINEERING, b.id().contains(":") ? 4 : 30 + b.eng() / 5);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1f, 0.9f);
        return true;
    }
}
