package com.kiyamet.npc;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Walkie-talkie screen: your companions sorted into 3 groups (Alpha, Bravo, Charlie). Click a person to call them,
 * right-click to move them to the next group, or call a whole group / everyone. Calls arrive in 20 seconds.
 */
public class RadioMenu extends PanelMenu {
    static final String[] TEAMS = {"Alpha", "Bravo", "Charlie"};
    static final Item[] TEAM_ICON = {Items.RED_BANNER, Items.BLUE_BANNER, Items.LIME_BANNER};
    static final ChatFormatting[] TEAM_COLOR = {ChatFormatting.RED, ChatFormatting.BLUE, ChatFormatting.GREEN};
    private final int[] rowSlot = {9, 18, 27};

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new RadioMenu(id, inv, p), Component.literal("Walkie-Talkie")));
    }

    private RadioMenu(int id, Inventory inv, ServerPlayer p) {
        super(id, inv, p);
        redraw();
    }

    private HumanEntity live(UUID u) {
        for (ServerLevel sl : player.level().getServer().getAllLevels()) {
            Entity e = sl.getEntity(u);
            if (e instanceof HumanEntity h) return h;
        }
        return null;
    }

    @Override
    protected void draw() {
        Map<UUID, Radio.Last> known = Radio.known(player);
        long now = player.level().getGameTime();
        boolean calling = Radio.calling(player);
        c.setItem(4, Ui.glint(Ui.icon(Ui.item("kiyamet:radio"), Ui.text("Walkie-Talkie", ChatFormatting.AQUA, ChatFormatting.BOLD),
                Ui.text(calling ? "Calling... arriving in " + Math.max(1, (Radio.callEnds(player) - now) / 20) + " s" : "Channel open. " + known.size() + " companion(s) on the radio", calling ? ChatFormatting.YELLOW : ChatFormatting.GREEN),
                Ui.text("Left-click a person: call them", ChatFormatting.GRAY), Ui.text("Right-click a person: move to next group", ChatFormatting.GRAY)), true));
        for (int t = 0; t < 3; t++) {
            int count = 0;
            for (Radio.Last l : known.values()) if (l.team() == t) count++;
            c.setItem(rowSlot[t], Ui.icon(TEAM_ICON[t], Ui.text("Group " + TEAMS[t] + " (" + count + ")", TEAM_COLOR[t], ChatFormatting.BOLD), Ui.text("Click: call the whole group", ChatFormatting.YELLOW)));
            int col = 1;
            for (Map.Entry<UUID, Radio.Last> e : known.entrySet()) {
                Radio.Last l = e.getValue();
                if (l.team() != t || col > 8) continue;
                int dist = l.dim() == player.level().dimension() ? (int) Math.sqrt(l.pos().distSqr(player.blockPosition())) : -1;
                HumanEntity h = live(e.getKey());
                ItemStack s = Ui.icon(h != null && h.isAway() ? Items.CLOCK : Items.PLAYER_HEAD, Component.literal(l.name()).withStyle(TEAM_COLOR[t], ChatFormatting.BOLD),
                        Ui.text("Lv " + l.level() + " · " + l.job(), ChatFormatting.GRAY),
                        Ui.text(dist < 0 ? "In another dimension" : dist + " blocks away", ChatFormatting.WHITE),
                        Ui.text(h == null ? "Out of range (will be reached)" : h.isAway() ? "On a supply run" : h.order() == HumanEntity.Order.FOLLOW ? "Following you" : "Holding position", ChatFormatting.DARK_GRAY),
                        Ui.text("Left: call · Right: change group", ChatFormatting.YELLOW));
                c.setItem(rowSlot[t] + col, s);
                col++;
            }
        }
        c.setItem(37, Ui.glint(Ui.icon(Items.BELL, Ui.text("Call EVERYONE", ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text("All companions reach you in 20 s", ChatFormatting.GRAY)), true));
        c.setItem(39, Ui.icon(Items.LEAD, Ui.text("Everyone nearby: follow me", ChatFormatting.AQUA)));
        c.setItem(41, Ui.icon(Items.OAK_SIGN, Ui.text("Everyone nearby: hold position", ChatFormatting.AQUA)));
        c.setItem(43, Ui.icon(Items.BELL, Ui.text("Open group panel", ChatFormatting.LIGHT_PURPLE), Ui.text("Supply runs, glow, picks", ChatFormatting.GRAY)));
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    private UUID at(int slot) {
        for (int t = 0; t < 3; t++) {
            int col = slot - rowSlot[t];
            if (col >= 1 && col <= 8) {
                int n = 0;
                for (Map.Entry<UUID, Radio.Last> e : Radio.known(player).entrySet()) {
                    if (e.getValue().team() != t) continue;
                    if (++n == col) return e.getKey();
                }
            }
        }
        return null;
    }

    private void callMsg(int n, String who) {
        if (n == 0) player.displayClientMessage(Component.literal("Static... nobody answers.").withStyle(ChatFormatting.GRAY), true);
        else player.sendSystemMessage(Component.literal("[Radio] \"" + who + ", come to me!\" — arriving in 20 seconds.").withStyle(ChatFormatting.AQUA));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        Map<UUID, Radio.Last> known = Radio.known(player);
        for (int t = 0; t < 3; t++) if (slot == rowSlot[t]) {
            List<UUID> ids = new ArrayList<>();
            int tt = t;
            known.forEach((u, l) -> { if (l.team() == tt) ids.add(u); });
            callMsg(Radio.call(player, ids), "Group " + TEAMS[t]);
            return true;
        }
        UUID u = at(slot);
        if (u != null) {
            HumanEntity h = live(u);
            if (type == ClickType.PICKUP && lastButton == 1) {
                if (h != null) { h.setTeam(h.team() + 1); Radio.report(h); }
                else player.displayClientMessage(Component.literal("Out of range: call them first to change their group.").withStyle(ChatFormatting.GRAY), true);
            } else callMsg(Radio.call(player, List.of(u)), known.get(u).name());
            return true;
        }
        switch (slot) {
            case 37 -> callMsg(Radio.call(player, known.keySet()), "Everyone");
            case 39 -> forNear(h -> h.setOrder(HumanEntity.Order.FOLLOW));
            case 41 -> forNear(h -> h.setOrder(HumanEntity.Order.STAY));
            case 43 -> { SquadMenu.open(player); return false; }
            case 49 -> { player.closeContainer(); return false; }
            default -> { return false; }
        }
        return true;
    }

    private void forNear(java.util.function.Consumer<HumanEntity> f) {
        ((ServerLevel) player.level()).getEntitiesOfClass(HumanEntity.class, player.getBoundingBox().inflate(128), h -> h.isOwnedBy(player) && !h.isAway()).forEach(f);
    }

    private int lastButton;

    @Override
    public void clicked(int slot, int button, ClickType type, net.minecraft.world.entity.player.Player p) {
        lastButton = button;
        super.clicked(slot, button, type, p);
    }
}
