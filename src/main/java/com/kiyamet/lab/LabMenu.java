package com.kiyamet.lab;

import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.ItemLore;

/** A 6-row chest screen listing lab recipes. Clicking a recipe crafts it if you have the level and the parts. */
public class LabMenu extends ChestMenu {
    public static final int PER_PAGE = 45;
    private final SimpleContainer shown;
    private final Player owner;
    private int page;

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

    private void refresh() {
        shown.clearContent();
        int eng = engineering();
        List<LabRecipes.Recipe> all = LabRecipes.ALL;
        for (int i = 0; i < PER_PAGE && page * PER_PAGE + i < all.size(); i++) {
            LabRecipes.Recipe r = all.get(page * PER_PAGE + i);
            ItemStack s = new ItemStack(item(r.result()), r.count());
            List<Component> lore = new ArrayList<>();
            boolean ok = eng >= r.level();
            lore.add(Component.translatable("kiyamet.lab.level", r.level()).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
            for (Map.Entry<String, Integer> e : r.parts().entrySet()) {
                int have = count(e.getKey());
                lore.add(Component.literal(" " + e.getValue() + "x ").append(item(e.getKey()).getName()).append(Component.literal("  (" + have + ")"))
                        .withStyle(have >= e.getValue() ? ChatFormatting.GRAY : ChatFormatting.DARK_RED));
            }
            s.set(DataComponents.LORE, new ItemLore(lore));
            shown.setItem(i, s);
        }
        ItemStack prev = new ItemStack(item("minecraft:arrow"));
        prev.set(DataComponents.CUSTOM_NAME, Component.translatable("kiyamet.lab.prev"));
        ItemStack next = new ItemStack(item("minecraft:spectral_arrow"));
        next.set(DataComponents.CUSTOM_NAME, Component.translatable("kiyamet.lab.next"));
        if (page > 0) shown.setItem(45, prev);
        if ((page + 1) * PER_PAGE < all.size()) shown.setItem(53, next);
        ItemStack info = new ItemStack(item("minecraft:book"));
        info.set(DataComponents.CUSTOM_NAME, Component.translatable("kiyamet.lab.info", eng));
        shown.setItem(49, info);
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
        if (slot == 45 && page > 0) { page--; refresh(); return; }
        if (slot == 53 && (page + 1) * PER_PAGE < LabRecipes.ALL.size()) { page++; refresh(); return; }
        int idx = page * PER_PAGE + slot;
        if (slot >= PER_PAGE || idx >= LabRecipes.ALL.size()) return;
        LabRecipes.Recipe r = LabRecipes.ALL.get(idx);
        if (engineering() < r.level()) { sp.sendSystemMessage(Component.translatable("kiyamet.lab.need_level", r.level()).withStyle(ChatFormatting.RED)); return; }
        for (Map.Entry<String, Integer> e : r.parts().entrySet())
            if (count(e.getKey()) < e.getValue()) { sp.sendSystemMessage(Component.translatable("kiyamet.lab.need_parts").withStyle(ChatFormatting.RED)); return; }
        r.parts().forEach(this::take);
        ItemStack out = new ItemStack(item(r.result()), r.count());
        if (!sp.getInventory().add(out)) sp.drop(out, false);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1f, 1.1f);
        SkillManager.addXp(sp, Skill.ENGINEERING, 15 + r.level() * 0.6);
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
