package com.kiyamet.furniture;

import com.kiyamet.npc.Shop;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Furniture Table: pick a piece, pay the materials, get the furniture. */
public class FurnitureTableBlock extends Block {
    public FurnitureTableBlock(Properties p) { super(p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Menu.open(sp);
        return InteractionResult.SUCCESS;
    }

    public static class Menu extends PanelMenu {
        static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

        static void open(ServerPlayer p) { p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, p), Component.literal("Furniture Table"))); }

        private Menu(int id, Inventory inv, ServerPlayer p) { super(id, inv, p); redraw(); }

        boolean has(Furniture.Def d) {
            Object[] c = d.cost();
            for (int i = 0; i < c.length; i += 2) if (Shop.count(player, (String) c[i]) < (Integer) c[i + 1]) return false;
            return true;
        }

        @Override
        protected void draw() {
            c.setItem(4, Ui.icon(Furniture.TABLE.asItem(), Ui.text("Furniture Table", ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text("Click a piece to build it", ChatFormatting.GRAY)));
            for (int i = 0; i < Furniture.DEFS.size(); i++) {
                Furniture.Def d = Furniture.DEFS.get(i);
                boolean ok = has(d);
                List<Component> lore = new ArrayList<>();
                lore.add(Ui.text(switch (d.kind()) { case BED -> "Bed: sleep and set spawn"; case SEAT -> "Sit on it"; case STORAGE -> d.slots() + " slots of storage";
                    case FRIDGE -> "Freezer (18) stops food spoiling, fridge (36) slows it 4x"; case LAMP -> "Gives light"; default -> "Decoration"; }, ChatFormatting.AQUA));
                Object[] c2 = d.cost();
                for (int j = 0; j < c2.length; j += 2) {
                    String id = (String) c2[j]; int n = (Integer) c2[j + 1], have = Shop.count(player, id);
                    lore.add(Component.literal((have >= n ? "✔ " : "✘ ") + n + "× ").append(new ItemStack(Ui.item(id)).getHoverName()).withStyle(have >= n ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                }
                lore.add(Ui.text(ok ? "Click to build!" : "Missing materials", ok ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
                ItemStack icon = new ItemStack(Furniture.BLOCKS.get(d.id()).asItem());
                c.setItem(SLOTS[i], Ui.glint(Ui.lore(icon, lore), ok));
            }
            c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
        }

        @Override
        protected boolean click(int slot, ClickType type) {
            if (slot == 49) { player.closeContainer(); return false; }
            for (int i = 0; i < Furniture.DEFS.size(); i++) {
                if (SLOTS[i] != slot) continue;
                Furniture.Def d = Furniture.DEFS.get(i);
                if (!player.isCreative() && !has(d)) return false;
                Object[] c2 = d.cost();
                if (!player.isCreative()) for (int j = 0; j < c2.length; j += 2) Shop.take(player, (String) c2[j], (Integer) c2[j + 1]);
                Shop.give(player, new ItemStack(Furniture.BLOCKS.get(d.id()).asItem()));
                player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 0.8f, 1f);
                return true;
            }
            return false;
        }
    }
}
