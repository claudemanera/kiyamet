package com.kiyamet.skill;

import com.kiyamet.boss.BossRewards;
import com.kiyamet.ui.Ui;
import com.kiyamet.world.Difficulty;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** /skills panel: 20 skill icons with level, progress bar and next milestone, plus Total Power and threat level. */
public class SkillsMenu extends ChestMenu {
    private static final int[] SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42};

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SkillsMenu(id, inv, p), Component.literal("Skills")));
    }

    private SkillsMenu(int id, Inventory inv, ServerPlayer p) {
        this(id, inv, new SimpleContainer(54), p);
    }

    private SkillsMenu(int id, Inventory inv, SimpleContainer c, ServerPlayer p) {
        super(MenuType.GENERIC_9x6, id, inv, c, 6);
        fill(c, p);
    }

    private static Item icon(Skill s) {
        return switch (s) {
            case HEALTH -> Items.GOLDEN_APPLE; case ATHLETICS -> Items.LEATHER_BOOTS; case MINING -> Items.IRON_PICKAXE;
            case SWORDS -> Items.IRON_SWORD; case ARCHERY -> Items.BOW; case DEFENSE -> Items.SHIELD; case ACROBATICS -> Items.FEATHER;
            case SWIMMING -> Items.PRISMARINE_SHARD; case FARMING -> Items.WHEAT; case WOODCUTTING -> Items.IRON_AXE;
            case HUNTING -> Items.COOKED_BEEF; case ENCHANTING -> Items.ENCHANTING_TABLE; case ALCHEMY -> Items.BREWING_STAND;
            case EXPLORATION -> Items.COMPASS; case ENGINEERING -> Items.REDSTONE; case SURVIVAL -> Items.CAMPFIRE;
            case STEALTH -> Items.ENDER_EYE; case MEDICINE -> Ui.item("kiyamet:medkit"); case HEAVY_WEAPONS -> Ui.item("kiyamet:arrow_gatling");
            case BUILDING -> Items.BRICKS;
        };
    }

    private static int nextMilestone(int lv) {
        for (int m : new int[]{250, 500, 750, 999}) if (lv < m && m - lv <= 50) return m;
        return Math.min(Skill.MAX_LEVEL, (lv / 50 + 1) * 50);
    }

    private static void fill(SimpleContainer c, ServerPlayer p) {
        SkillData d = SkillManager.get(p);
        for (int i = 0; i < 54; i++) c.setItem(i, i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8 ? Ui.dark() : Ui.frame());
        int power = d.totalPower();
        int threat = (int) Math.round(100.0 * Math.min(power, Difficulty.CAP) / Difficulty.CAP);
        c.setItem(4, Ui.glint(Ui.icon(Items.NETHER_STAR, Ui.text("Total Power: " + power, ChatFormatting.GOLD, ChatFormatting.BOLD),
                Ui.text("Sum of all 20 skill levels (max " + 20 * Skill.MAX_LEVEL + ")", ChatFormatting.GRAY),
                Ui.bar(power / (20.0 * Skill.MAX_LEVEL), 20),
                Ui.text("Zombie threat: " + threat + "%", threat > 66 ? ChatFormatting.RED : threat > 33 ? ChatFormatting.YELLOW : ChatFormatting.GREEN),
                Ui.text("(zombies stop getting stronger at " + Difficulty.CAP + ")", ChatFormatting.DARK_GRAY)), true));
        int bosses = p.getAttachedOrElse(BossRewards.KILLED, List.of()).size();
        c.setItem(2, Ui.icon(Items.WITHER_SKELETON_SKULL, Ui.text("Bosses defeated: " + bosses + " / 15", ChatFormatting.DARK_RED, ChatFormatting.BOLD), Ui.bar(bosses / 15.0, 15)));
        boolean rested = d.restTicks() < SkillManager.RESTED_TICKS;
        c.setItem(6, Ui.icon(rested ? Items.GLOW_BERRIES : Items.SWEET_BERRIES, Ui.text(rested ? "Rested: +50% XP" : "Rested bonus used up today", rested ? ChatFormatting.AQUA : ChatFormatting.GRAY),
                Ui.text("First 2 hours of play each day give +50% XP", ChatFormatting.GRAY)));
        Skill[] all = Skill.values();
        for (int i = 0; i < all.length; i++) {
            Skill s = all[i];
            int lv = d.level(s);
            boolean max = lv >= Skill.MAX_LEVEL;
            double frac = max ? 1 : d.xp(s) / (double) XpCurve.toNext(lv);
            List<Component> lore = new ArrayList<>();
            lore.add(Ui.text("Level " + lv + " / " + Skill.MAX_LEVEL, ChatFormatting.YELLOW));
            lore.add(Ui.bar(frac, 20).copy().append(Component.literal(" " + (int) (frac * 100) + "%").withStyle(ChatFormatting.GRAY)));
            if (!max) {
                lore.add(Ui.text(d.xp(s) + " / " + XpCurve.toNext(lv) + " XP", ChatFormatting.DARK_GRAY));
                int m = nextMilestone(lv);
                boolean big = m == 250 || m == 500 || m == 750 || m == 999;
                lore.add(Ui.text((big ? "★ Big power at level " : "Next perk at level ") + m, big ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
            } else lore.add(Ui.text("★ MASTERED", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            int steps = Perks.steps(lv);
            for (Perks.Perk pk : Perks.of(s))
                lore.add(Ui.text("✦ " + (steps == 0 ? pk.label + " (from level 50)" : pk.describe(steps)), steps == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN));
            ItemStack st = Ui.icon(icon(s), Component.translatable(s.key()).withStyle(max ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE, ChatFormatting.BOLD));
            Ui.lore(st, lore);
            st.setCount(Math.max(1, Math.min(64, lv / 16 + 1)));
            c.setItem(SLOTS[i], Ui.glint(st, lv >= 250));
        }
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
        long un = java.util.Arrays.stream(Powers.Power.values()).filter(pw -> pw.unlocked(d)).count();
        c.setItem(47, Ui.glint(Ui.icon(Items.BEACON, Ui.text("Super Powers", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Ui.text(un + " / " + Powers.Power.values().length + " unlocked", ChatFormatting.GRAY), Ui.text("Click to open", ChatFormatting.YELLOW)), true));
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot == 49 && player instanceof ServerPlayer sp) sp.closeContainer();
        if (slot == 47 && player instanceof ServerPlayer sp) { PowersMenu.open(sp); return; }
        if (slot < 0 || slot >= 54) { if (type != ClickType.QUICK_MOVE) super.clicked(slot, button, type, player); }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
