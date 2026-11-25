package com.kiyamet.npc;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Your whole group in one place: pick people, send them on a supply run together, or give group orders. */
public class SquadMenu extends PanelMenu {
    private static final int[] SLOTS = {10, 11, 12, 14, 15, 16};
    private final Set<UUID> picked = new HashSet<>();
    private List<HumanEntity> squad = List.of();

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SquadMenu(id, inv, p), Component.literal("Your group")));
    }

    private SquadMenu(int id, Inventory inv, ServerPlayer p) {
        super(id, inv, p);
        redraw();
    }

    private List<HumanEntity> members() {
        return new ArrayList<>(((ServerLevel) player.level()).getEntitiesOfClass(HumanEntity.class, player.getBoundingBox().inflate(512), h -> h.isOwnedBy(player)));
    }

    @Override
    protected void draw() {
        squad = members();
        long now = player.level().getGameTime();
        c.setItem(4, Ui.glint(Ui.icon(Items.BELL, Ui.text("Group: " + squad.size() + " / " + RecruitMenu.MAX_COMPANIONS, ChatFormatting.GOLD, ChatFormatting.BOLD),
                Ui.text("Click people to pick them, then send them out.", ChatFormatting.GRAY),
                Ui.text("Longer runs = more loot but more danger. Bigger groups are safer.", ChatFormatting.GRAY)), true));
        for (int i = 0; i < SLOTS.length; i++) {
            if (i >= squad.size()) { c.setItem(SLOTS[i], Ui.icon(Items.GRAY_STAINED_GLASS_PANE, Ui.text("Empty place", ChatFormatting.DARK_GRAY))); continue; }
            HumanEntity h = squad.get(i);
            boolean away = h.isAway(), sel = picked.contains(h.getUUID());
            ItemStack s = Ui.icon(away ? Items.CLOCK : h.job().icon, Component.literal(h.getName().getString()).withStyle(sel ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD),
                    Ui.text("Lv " + h.lvl() + " · " + h.profession().title + " · " + h.job().title, ChatFormatting.GRAY),
                    Ui.text("Health " + (int) h.getHealth() + " / " + (int) h.getMaxHealth(), ChatFormatting.RED),
                    away ? Ui.text("On a supply run: back in " + Math.max(1, (h.awayUntil() - now) / 1200 + 1) + " min", ChatFormatting.YELLOW)
                         : Ui.text(sel ? "✔ Picked (click to unpick)" : "Click to pick", sel ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            c.setItem(SLOTS[i], Ui.glint(s, sel));
        }
        c.setItem(28, Ui.icon(Items.LEAD, Ui.text("Everyone: follow me", ChatFormatting.AQUA)));
        c.setItem(30, Ui.icon(Items.OAK_SIGN, Ui.text("Everyone: stay here", ChatFormatting.AQUA)));
        c.setItem(32, Ui.icon(Items.ENDER_PEARL, Ui.text("Call everyone to me", ChatFormatting.AQUA)));
        c.setItem(34, Ui.icon(Items.GLOWSTONE_DUST, Ui.text("Toggle glow for everyone", ChatFormatting.YELLOW)));
        c.setItem(37, Ui.icon(Items.WHITE_BANNER, Ui.text("Pick everyone available", ChatFormatting.WHITE)));
        c.setItem(39, Ui.icon(Items.CHEST, Ui.text("Supply run: 3 minutes", ChatFormatting.GREEN), Ui.text("Safe, small haul", ChatFormatting.GRAY)));
        c.setItem(40, Ui.icon(Items.CHEST, Ui.text("Supply run: 6 minutes", ChatFormatting.GOLD), Ui.text("Some risk, good haul", ChatFormatting.GRAY)));
        c.setItem(41, Ui.icon(Items.CHEST, Ui.text("Supply run: 10 minutes", ChatFormatting.RED), Ui.text("Dangerous, big haul", ChatFormatting.GRAY)));
        c.setItem(43, Ui.icon(Items.BARRIER, Ui.text("Clear picks", ChatFormatting.GRAY)));
        c.setItem(49, Ui.icon(Items.ARROW, Ui.text("Close", ChatFormatting.RED)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        for (int i = 0; i < SLOTS.length; i++) if (slot == SLOTS[i] && i < squad.size()) {
            HumanEntity h = squad.get(i);
            if (h.isAway()) return false;
            if (!picked.remove(h.getUUID())) picked.add(h.getUUID());
            return true;
        }
        switch (slot) {
            case 28 -> squad.forEach(h -> { if (!h.isAway()) h.setOrder(HumanEntity.Order.FOLLOW); });
            case 30 -> squad.forEach(h -> { if (!h.isAway()) h.setOrder(HumanEntity.Order.STAY); });
            case 32 -> squad.forEach(h -> { if (!h.isAway()) { h.teleportTo(player.getX(), player.getY(), player.getZ()); h.setOrder(HumanEntity.Order.FOLLOW); } });
            case 34 -> { boolean on = squad.stream().noneMatch(HumanEntity::hasGlowingTag); squad.forEach(h -> h.setGlowingTag(on)); }
            case 37 -> squad.forEach(h -> { if (!h.isAway()) picked.add(h.getUUID()); });
            case 43 -> picked.clear();
            case 39, 40, 41 -> {
                int minutes = slot == 39 ? 3 : slot == 40 ? 6 : 10;
                List<HumanEntity> go = squad.stream().filter(h -> picked.contains(h.getUUID()) && !h.isAway()).toList();
                if (go.isEmpty()) { player.displayClientMessage(Component.literal("Pick someone first.").withStyle(ChatFormatting.RED), true); return false; }
                for (HumanEntity h : go) h.sendAway(minutes, go.size());
                picked.clear();
                player.sendSystemMessage(Component.literal(go.size() + " companion(s) left on a " + minutes + "-minute supply run.").withStyle(ChatFormatting.GOLD));
            }
            case 49 -> { player.closeContainer(); return false; }
            default -> { return false; }
        }
        return true;
    }
}
