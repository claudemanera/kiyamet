package com.kiyamet.lab;

import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Laboratory Table screen: category tabs on top, 36 recipes in the middle (glowing = craftable now), page buttons below.
 */
public class LabMenu extends ChestMenu {
    public static final int PER_PAGE = 36, FIRST = 9;
    enum Cat {
        ALL("All recipes", "minecraft:crafting_table"), PARTS("Parts", "kiyamet:circuit"), DEFENSE("Base defense", "kiyamet:steel_block"),
        GADGETS("Gadgets", "kiyamet:flashlight"), WEAPONS("Arrow weapons", "kiyamet:arrow_rifle"), READY("Craftable now", "minecraft:emerald");
        final String title, icon;
        Cat(String title, String icon) { this.title = title; this.icon = icon; }
    }
    private static final int[] TAB_SLOTS = {1, 2, 3, 5, 6, 7};

    private final SimpleContainer shown;
    private final Player owner;
    private int page;
    private Cat cat = Cat.ALL;
    private List<LabRecipes.Recipe> list = List.of();

    public LabMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(54));
    }

    private LabMenu(int id, Inventory inv, SimpleContainer c) {
        super(MenuType.GENERIC_9x6, id, inv, c, 6);
        this.shown = c;
        this.owner = inv.player;
        refresh();
    }

    private static Item item(String id) { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); }

    private int engineering() { return owner instanceof ServerPlayer sp ? SkillManager.get(sp).level(Skill.ENGINEERING) : 1; }

    private static boolean in(String[] arr, String id) {
        for (String a : arr) if (id.equals("kiyamet:" + a)) return true;
        return false;
    }

    private boolean canCraft(LabRecipes.Recipe r, int eng) {
        if (eng < r.level()) return false;
        for (Map.Entry<String, Integer> e : r.parts().entrySet()) if (count(e.getKey()) < e.getValue()) return false;
        return true;
    }

    private boolean matches(LabRecipes.Recipe r, int eng) {
        String id = r.result();
        return switch (cat) {
            case ALL -> true;
            case PARTS -> in(com.kiyamet.registry.ModItems.PARTS, id) || id.equals("kiyamet:repair_kit") || id.equals("kiyamet:medkit");
            case DEFENSE -> item(id) instanceof net.minecraft.world.item.BlockItem;
            case GADGETS -> in(com.kiyamet.registry.ModItems.GADGETS, id);
            case WEAPONS -> in(com.kiyamet.registry.ModItems.WEAPONS, id);
            case READY -> canCraft(r, eng);
        };
    }

    private void refresh() {
        shown.clearContent();
        int eng = engineering();
        list = LabRecipes.ALL.stream().filter(r -> matches(r, eng)).toList();
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        for (int i = 0; i < 9; i++) shown.setItem(i, com.kiyamet.ui.Ui.dark());
        for (int i = 45; i < 54; i++) shown.setItem(i, com.kiyamet.ui.Ui.dark());
        Cat[] cats = Cat.values();
        for (int t = 0; t < cats.length; t++) {
            Cat c = cats[t];
            boolean sel = c == cat;
            ItemStack tab = com.kiyamet.ui.Ui.icon(item(c.icon), Component.literal(c.title).withStyle(sel ? ChatFormatting.GOLD : ChatFormatting.WHITE, ChatFormatting.BOLD),
                    Component.literal(sel ? "▶ Selected" : "Click to show").withStyle(sel ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
            shown.setItem(TAB_SLOTS[t], com.kiyamet.ui.Ui.glint(tab, sel));
        }
        for (int i = 0; i < PER_PAGE && page * PER_PAGE + i < list.size(); i++) {
            LabRecipes.Recipe r = list.get(page * PER_PAGE + i);
            ItemStack s = new ItemStack(item(r.result()), r.count());
            List<Component> lore = new ArrayList<>();
            boolean lvlOk = eng >= r.level();
            boolean ready = canCraft(r, eng);
            lore.add(Component.literal(ready ? "✔ Ready to craft" : lvlOk ? "✘ Missing parts" : "✘ Locked").withStyle(ready ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD));
            lore.add(Component.translatable("kiyamet.lab.level", r.level()).withStyle(lvlOk ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
            lore.add(Component.literal("Parts:").withStyle(ChatFormatting.GRAY));
            for (Map.Entry<String, Integer> e : r.parts().entrySet()) {
                int have = count(e.getKey());
                boolean enough = have >= e.getValue();
                lore.add(Component.literal((enough ? " ✔ " : " ✘ ") + e.getValue() + "x ").append(item(e.getKey()).getName())
                        .append(Component.literal("  (" + have + ")")).withStyle(enough ? ChatFormatting.GRAY : ChatFormatting.DARK_RED));
            }
            lore.add(Component.literal("+" + (int) (15 + r.level() * 0.6) + " Engineering XP").withStyle(ChatFormatting.AQUA));
            if (ready) lore.add(Component.literal("Click to craft").withStyle(ChatFormatting.YELLOW));
            com.kiyamet.ui.Ui.lore(s, lore);
            shown.setItem(FIRST + i, com.kiyamet.ui.Ui.glint(s, ready));
        }
        if (list.isEmpty()) shown.setItem(22, com.kiyamet.ui.Ui.icon(net.minecraft.world.item.Items.BARRIER, Component.literal("Nothing here yet").withStyle(ChatFormatting.RED),
                Component.literal(cat == Cat.READY ? "Gather more parts or level up Engineering." : "Level up Engineering to unlock more.").withStyle(ChatFormatting.GRAY)));
        if (page > 0) shown.setItem(45, com.kiyamet.ui.Ui.icon(item("minecraft:arrow"), Component.translatable("kiyamet.lab.prev").withStyle(ChatFormatting.YELLOW)));
        if (page + 1 < pages) shown.setItem(53, com.kiyamet.ui.Ui.icon(item("minecraft:spectral_arrow"), Component.translatable("kiyamet.lab.next").withStyle(ChatFormatting.YELLOW)));
        long unlocked = LabRecipes.ALL.stream().filter(r -> eng >= r.level()).count();
        shown.setItem(49, com.kiyamet.ui.Ui.icon(item("minecraft:book"), Component.translatable("kiyamet.lab.info", eng).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.literal("Page " + (page + 1) + " / " + pages).withStyle(ChatFormatting.GRAY),
                Component.literal("Unlocked recipes: " + unlocked + " / " + LabRecipes.ALL.size()).withStyle(ChatFormatting.GRAY),
                com.kiyamet.ui.Ui.bar(unlocked / (double) LabRecipes.ALL.size(), 20)));
    }

    private int count(String id) {
        Item it = item(id);
        int n = 0;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack s = owner.getInventory().getItem(i);
            if (s.is(it)) n += s.getCount();
        }
        return n;
    }

    private void take(String id, int amount) {
        Item it = item(id);
        for (int i = 0; i < owner.getInventory().getContainerSize() && amount > 0; i++) {
            ItemStack s = owner.getInventory().getItem(i);
            if (s.is(it)) { int t = Math.min(amount, s.getCount()); s.shrink(t); amount -= t; }
        }
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot < 0 || slot >= 54) { if (type != ClickType.QUICK_MOVE) super.clicked(slot, button, type, player); return; }
        if (!(player instanceof ServerPlayer sp)) return;
        for (int t = 0; t < TAB_SLOTS.length; t++) if (slot == TAB_SLOTS[t]) {
            cat = Cat.values()[t]; page = 0; refresh();
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.2f);
            return;
        }
        if (slot == 45 && page > 0) { page--; refresh(); return; }
        if (slot == 53 && (page + 1) * PER_PAGE < list.size()) { page++; refresh(); return; }
        int idx = page * PER_PAGE + slot - FIRST;
        if (slot < FIRST || slot >= FIRST + PER_PAGE || idx >= list.size()) return;
        LabRecipes.Recipe r = list.get(idx);
        if (engineering() < r.level()) { sp.sendSystemMessage(Component.translatable("kiyamet.lab.need_level", r.level()).withStyle(ChatFormatting.RED)); return; }
        for (Map.Entry<String, Integer> e : r.parts().entrySet())
            if (count(e.getKey()) < e.getValue()) { sp.sendSystemMessage(Component.translatable("kiyamet.lab.need_parts").withStyle(ChatFormatting.RED)); return; }
        int times = type == ClickType.QUICK_MOVE ? 8 : 1;          // shift-click crafts up to 8 at once
        for (int n = 0; n < times && canCraft(r, engineering()); n++) {
            r.parts().forEach(this::take);
            ItemStack out = new ItemStack(item(r.result()), r.count());
            if (!sp.getInventory().add(out)) sp.drop(out, false);
            SkillManager.addXp(sp, Skill.ENGINEERING, 15 + r.level() * 0.6);
        }
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1f, 1.1f);
        refresh();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public void removed(Player player) {
        shown.clearContent();
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}
