package com.kiyamet.npc;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/** Community leader: reputation, donations, recruit volunteers (needs trust). */
public class LeaderMenu extends PanelMenu {
    private final HumanEntity h;

    public static void open(ServerPlayer p, HumanEntity h) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new LeaderMenu(id, inv, p, h),
                Component.literal(Communities.displayName(h.communityType()) + " - " + h.getName().getString())));
    }

    private LeaderMenu(int id, Inventory inv, ServerPlayer p, HumanEntity h) {
        super(id, inv, p);
        this.h = h;
        redraw();
    }

    private int rep() { return Communities.reputation(player, h.community()); }

    @Override
    protected void draw() {
        int rep = rep();
        int members = h.allies((ServerLevel) player.level(), 64).size() + 1;
        String standing = rep >= 80 ? "Hero of the community" : rep >= 50 ? "Trusted friend" : rep >= 20 ? "Welcome guest" : "Stranger";
        c.setItem(4, Ui.glint(Ui.icon(Items.BELL, Component.literal(Communities.displayName(h.communityType())).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Ui.text("Leader: " + h.getName().getString() + " (Lv " + h.level() + ")", ChatFormatting.YELLOW),
                Ui.text("Members nearby: " + members, ChatFormatting.GRAY),
                Ui.text("Your standing: " + standing + " (" + rep + ")", ChatFormatting.GREEN), Ui.bar((rep + 100) / 200.0, 20)), true));
        c.setItem(20, Ui.icon(Ui.item(Shop.RUBY), Ui.text("Donate 5 rubies", ChatFormatting.RED), Ui.text("+10 reputation", ChatFormatting.GREEN)));
        c.setItem(22, Ui.icon(Items.WHEAT, Ui.text("Donate 32 wheat", ChatFormatting.GOLD), Ui.text("+6 reputation", ChatFormatting.GREEN)));
        boolean canRecruit = rep >= 40;
        c.setItem(24, Ui.glint(Ui.icon(Items.IRON_SWORD, Ui.text("Ask for a volunteer (12 rubies)", canRecruit ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY, ChatFormatting.BOLD),
                Ui.text(canRecruit ? "A skilled member joins you as a companion" : "Needs reputation 40", canRecruit ? ChatFormatting.GRAY : ChatFormatting.RED)), canRecruit));
        c.setItem(31, Ui.icon(Items.MAP, Ui.text("\"" + Communities.greeting(h.getRandom(), h.communityType()) + "\"", ChatFormatting.ITALIC, ChatFormatting.GRAY)));
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (!h.isAlive()) { player.closeContainer(); return false; }
        switch (slot) {
            case 49 -> { player.closeContainer(); return false; }
            case 20 -> { if (!Shop.take(player, Shop.RUBY, 5)) return false; Communities.change(player, h.community(), 10); }
            case 22 -> { if (!Shop.take(player, "minecraft:wheat", 32)) return false; Communities.change(player, h.community(), 6); }
            case 24 -> {
                if (rep() < 40 || RecruitMenu.companions(player) >= RecruitMenu.MAX_COMPANIONS) return false;
                if (!Shop.take(player, Shop.RUBY, 12)) return false;
                ServerLevel sl = (ServerLevel) player.level();
                HumanEntity v = ModHumans.HUMAN.create(sl, EntitySpawnReason.MOB_SUMMONED);
                if (v == null) return false;
                v.setPos(h.getX() + 1, h.getY(), h.getZ() + 1);
                v.setup(Faction.SURVIVOR, Role.MEMBER, Math.max(20, h.level() / 2 + h.getRandom().nextInt(30)), null);
                sl.addFreshEntity(v);
                v.setOwner(player);
                player.sendSystemMessage(Component.literal(v.getName().getString() + " volunteers to join you!").withStyle(ChatFormatting.AQUA));
                player.closeContainer();
                return false;
            }
            default -> { return false; }
        }
        return true;
    }
}
