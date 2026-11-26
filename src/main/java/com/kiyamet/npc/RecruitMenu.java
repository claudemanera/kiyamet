package com.kiyamet.npc;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/** Talk to a lone survivor: recruit them for rubies (or a medkit if they are hurt). Max 6 companions. */
public class RecruitMenu extends PanelMenu {
    public static final int MAX_COMPANIONS = 6;
    private final HumanEntity h;

    public static void open(ServerPlayer p, HumanEntity h) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new RecruitMenu(id, inv, p, h), Component.literal("Survivor: " + h.getName().getString())));
    }

    private RecruitMenu(int id, Inventory inv, ServerPlayer p, HumanEntity h) {
        super(id, inv, p);
        this.h = h;
        redraw();
    }

    public static int cost(HumanEntity h) { return 3 + h.lvl() / 8; }

    public static long companions(ServerPlayer p) {
        return ((net.minecraft.server.level.ServerLevel) p.level()).getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(512), x -> x.isOwnedBy(p)).size();
    }

    @Override
    protected void draw() {
        c.setItem(4, Ui.icon(Items.PLAYER_HEAD, Component.literal(h.getName().getString()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                Ui.text("Level " + h.lvl(), ChatFormatting.YELLOW),
                Ui.text("Type: " + h.profession().title, ChatFormatting.WHITE),
                Ui.text(h.specialty() == Specialty.NONE ? "No specialty" : "★ " + h.specialty().title + ": " + h.specialty().desc, h.specialty() == Specialty.NONE ? ChatFormatting.GRAY : ChatFormatting.LIGHT_PURPLE),
                Ui.text("\"I could use some company out here.\"", ChatFormatting.GRAY)));
        int cost = cost(h), have = Shop.count(player, Shop.RUBY);
        c.setItem(20, Ui.glint(Ui.icon(Ui.item("kiyamet:dollar_100"), Ui.text("Recruit for " + com.kiyamet.money.Money.fmt(cost * com.kiyamet.money.Money.RATE), have >= cost ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD),
                Ui.text("You have " + have, ChatFormatting.GRAY), Ui.text("Companions: " + companions(player) + " / " + MAX_COMPANIONS, ChatFormatting.GRAY)), have >= cost));
        c.setItem(22, Ui.icon(Ui.item("kiyamet:medkit"), Ui.text("Give a medkit instead", ChatFormatting.AQUA), Ui.text("A kind deed: they join for free", ChatFormatting.GRAY)));
        c.setItem(24, Ui.icon(Items.BREAD, Ui.text("Share food (3 bread)", ChatFormatting.GOLD), Ui.text("50% chance they join", ChatFormatting.GRAY)));
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Leave", ChatFormatting.RED)));
    }

    public static void join(ServerPlayer player, HumanEntity h, String line) {
        Talk.say(player, h, line);
        h.setOwner(player);
        player.sendSystemMessage(Component.literal(h.getName().getString() + " joins you! Right-click them for orders.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.level().playSound(null, h.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_YES, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1f);
    }

    private void join() {
        h.setOwner(player);
        player.closeContainer();
        player.sendSystemMessage(Component.literal(h.getName().getString() + " joins you! Right-click them for orders.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.level().playSound(null, h.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_YES, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1f);
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (!h.isAlive() || h.faction() != Faction.SURVIVOR) { player.closeContainer(); return false; }
        if (slot == 49) { player.closeContainer(); return false; }
        if (slot != 20 && slot != 22 && slot != 24) return false;
        if (companions(player) >= MAX_COMPANIONS) { player.displayClientMessage(Component.literal("You already lead " + MAX_COMPANIONS + " companions.").withStyle(ChatFormatting.RED), true); return false; }
        if (slot == 20 && Shop.take(player, Shop.RUBY, cost(h))) join();
        else if (slot == 22 && Shop.take(player, "kiyamet:medkit", 1)) join();
        else if (slot == 24 && Shop.take(player, "minecraft:bread", 3)) {
            if (h.getRandom().nextBoolean()) join();
            else { player.displayClientMessage(Component.literal("\"Thanks... but I'm not ready to trust you yet.\"").withStyle(ChatFormatting.GRAY), true); return true; }
        } else player.displayClientMessage(Component.literal("You don't have that.").withStyle(ChatFormatting.RED), true);
        return true;
    }
}
