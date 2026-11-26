package com.kiyamet.skill;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The road of one skill from level 1 to 999: 72 stops over two pages, laid out as a winding path.
 * Every stop shows what you have at that level and which super powers unlock there.
 */
public class SkillRoadMenu extends PanelMenu {
    private static final int NODES = 72, PER_PAGE = 36;
    private final Skill skill;
    private int page;

    public static void open(ServerPlayer p, Skill s, int page) {
        int lv = SkillManager.get(p).level(s);
        int pg = page >= 0 ? page : (nodeIndexFor(lv) >= PER_PAGE ? 1 : 0);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SkillRoadMenu(id, inv, p, s, pg),
                Component.translatable(s.key()).append(Component.literal(" - road to 999"))));
    }

    private SkillRoadMenu(int id, Inventory inv, ServerPlayer p, Skill s, int page) {
        super(id, inv, p);
        this.skill = s;
        this.page = page;
        redraw();
    }

    static int nodeLevel(int i) { return Math.max(1, (int) Math.round(Skill.MAX_LEVEL * (i + 1) / (double) NODES)); }

    static int nodeIndexFor(int lv) {
        for (int i = 0; i < NODES; i++) if (nodeLevel(i) > lv) return i;
        return NODES - 1;
    }

    /** Slot of the i-th stop on a page, snaking left-right / right-left across 4 rows. */
    private static int slot(int i) {
        int row = i / 9, col = i % 9;
        if (row % 2 == 1) col = 8 - col;
        return 9 + row * 9 + col;
    }

    @Override
    protected void draw() {
        int lv = SkillManager.get(player).level(skill);
        List<Perks.Perk> perks = Perks.of(skill);
        // header
        List<Component> head = new ArrayList<>();
        head.add(Ui.text("Level " + lv + " / " + Skill.MAX_LEVEL, ChatFormatting.YELLOW));
        head.add(Ui.bar(lv / (double) Skill.MAX_LEVEL, 30).copy().append(Component.literal(" " + (lv * 100 / Skill.MAX_LEVEL) + "%").withStyle(ChatFormatting.GRAY)));
        for (Perks.Perk pk : perks) {
            int nx = pk.nextLevel(lv);
            head.add(Ui.text("✦ " + pk.describe(lv) + (nx > 0 ? "  → next at lv " + nx + ": " + pk.fmt(pk.perStep()) : "  (max)"), ChatFormatting.GREEN));
        }
        for (Perks.Perk pk : perks) head.add(Ui.text("   at 999: " + pk.describe(Skill.MAX_LEVEL), ChatFormatting.DARK_AQUA));
        ItemStack h = Ui.logo("skill_" + skill.name().toLowerCase(), Component.translatable(skill.key()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        Ui.lore(h, head);
        c.setItem(4, Ui.glint(h, lv >= Skill.MAX_LEVEL));

        int cur = nodeIndexFor(lv);
        for (int k = 0; k < PER_PAGE; k++) {
            int i = page * PER_PAGE + k;
            int at = nodeLevel(i), prev = i == 0 ? 0 : nodeLevel(i - 1);
            boolean reached = lv >= at, current = i == cur && !reached;
            List<Powers.Power> unlocks = new ArrayList<>();
            for (Powers.Power pw : Powers.Power.values()) if (pw.skill == skill && pw.level > prev && pw.level <= at) unlocks.add(pw);
            List<Component> lore = new ArrayList<>();
            lore.add(Ui.text(reached ? "✔ Reached" : current ? "► You are here (" + lv + ")" : "Locked", reached ? ChatFormatting.GREEN : current ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
            ChatFormatting[] col = {ChatFormatting.AQUA, ChatFormatting.GREEN, ChatFormatting.GOLD, ChatFormatting.LIGHT_PURPLE};
            for (int L0 = prev + 1; L0 <= at; L0++) {
                final int L = L0;
                net.minecraft.network.chat.MutableComponent row = null;
                for (int pi = 0; pi < perks.size(); pi++) {
                    Perks.Perk pk = perks.get(pi);
                    if (L % pk.every() != 0 || L / pk.every() > pk.totalSteps()) continue;
                    Component part = Component.literal(pk.label() + " " + pk.fmt(pk.perStep())).withStyle(lv >= L ? col[pi % col.length] : ChatFormatting.DARK_GRAY);
                    if (row == null) row = Component.literal("Lv " + L + ": ").withStyle(st -> st.withItalic(false).withColor(lv >= L ? ChatFormatting.WHITE : ChatFormatting.GRAY));
                    else row.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
                    row.append(part);
                }
                if (row != null) lore.add(row);
            }
            lore.add(Ui.text("Total at " + at + ":", ChatFormatting.WHITE));
            for (int pi = 0; pi < perks.size(); pi++) lore.add(Ui.text("  " + perks.get(pi).describe(at), col[pi % col.length]));
            for (Powers.Power pw : unlocks) lore.add(Ui.text("★ Super power: " + pw.title + " (lv " + pw.level + ")", ChatFormatting.LIGHT_PURPLE));
            if (at == 250 || at == 500 || at == 750 || at == Skill.MAX_LEVEL) lore.add(Ui.text("★ Big milestone", ChatFormatting.GOLD));
            ItemStack node;
            Component name = Ui.text("Level " + at, reached ? ChatFormatting.GREEN : current ? ChatFormatting.YELLOW : ChatFormatting.WHITE, ChatFormatting.BOLD);
            if (!unlocks.isEmpty()) node = Ui.logo("power_" + unlocks.get(0).name().toLowerCase(), name);
            else if (current) node = Ui.icon(Items.EXPERIENCE_BOTTLE, name);
            else if (at % 100 < (Skill.MAX_LEVEL / NODES) && at >= 100 || at == Skill.MAX_LEVEL) node = Ui.icon(reached ? Items.GOLD_BLOCK : Items.YELLOW_TERRACOTTA, name);
            else node = Ui.icon(reached ? Items.LIME_CONCRETE : Items.GRAY_CONCRETE, name);
            Ui.lore(node, lore);
            node.setCount(Math.max(1, Math.min(64, at / 16)));
            c.setItem(slot(k), Ui.glint(node, current || reached && !unlocks.isEmpty()));
        }
        c.setItem(45, Ui.icon(Items.ARROW, Ui.text("Back to skills", ChatFormatting.YELLOW)));
        if (page > 0) c.setItem(48, Ui.icon(Items.SPECTRAL_ARROW, Ui.text("< Levels 1-" + nodeLevel(PER_PAGE - 1), ChatFormatting.AQUA)));
        if (page == 0) c.setItem(50, Ui.icon(Items.SPECTRAL_ARROW, Ui.text("Levels " + nodeLevel(PER_PAGE) + "-999 >", ChatFormatting.AQUA)));
        c.setItem(49, Ui.icon(Items.MAP, Ui.text("Page " + (page + 1) + " / 2", ChatFormatting.WHITE), Ui.text("Green = reached · bottle = you are here", ChatFormatting.GRAY),
                Ui.text("Icons = super powers · gold = every 100 levels", ChatFormatting.GRAY)));
        c.setItem(53, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (slot == 45) { SkillsMenu.open(player); return false; }
        if (slot == 53) { player.closeContainer(); return false; }
        if (slot == 48 && page > 0) { page = 0; return true; }
        if (slot == 50 && page == 0) { page = 1; return true; }
        return false;
    }
}
