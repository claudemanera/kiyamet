package com.kiyamet.twd;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.Shop;
import com.kiyamet.registry.ModItems;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.9 Main Table: the one table that builds all the other TWD stations - furniture, cooking, guns, lab, vehicles,
 * power, the mission board and everything for your own settlement. Those stations can't be crafted anywhere else.
 */
public class MainTable extends Block {
    public static Block BLOCK;
    public static Item MISSION_BOARD_ITEM;

    /** item id, short description, cost (item, n, ...) */
    public record Station(String id, String what, Object... cost) {}
    public static final List<Station> STATIONS = new ArrayList<>(List.of(
            new Station("kiyamet:furniture_table", "Build beds, sofas, wardrobes, fridges...", "minecraft:oak_planks", 12, "minecraft:shears", 1, "minecraft:crafting_table", 1),
            new Station("kiyamet:cooking_table", "Cook all 30 meals", "minecraft:oak_planks", 8, "minecraft:iron_ingot", 2, "minecraft:furnace", 1, "minecraft:bowl", 1),
            new Station("kiyamet:gun_bench", "Build guns, fit parts, repair", "minecraft:iron_ingot", 6, "minecraft:oak_planks", 6, "minecraft:crafting_table", 1),
            new Station("kiyamet:lab_table", "Research and the vaccine", "minecraft:iron_ingot", 6, "minecraft:copper_ingot", 2, "minecraft:redstone", 4, "minecraft:crafting_table", 1),
            new Station("kiyamet:vehicle_workshop", "Build cars and bikes", "minecraft:iron_block", 6, "minecraft:anvil", 1, "minecraft:crafting_table", 1),
            new Station("kiyamet:generator", "Powers turrets, fences and electric tools", "minecraft:iron_ingot", 8, "minecraft:redstone", 6, "minecraft:furnace", 1),
            new Station("kiyamet:mission_board", "Your own mission board: 3 new jobs every day", "minecraft:oak_planks", 10, "minecraft:paper", 6, "minecraft:stick", 4)));

    public MainTable(Properties p) { super(p); }

    public static void init() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kiyamet.id("main_table"));
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new MainTable(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(3f).setId(key)));
        ModItems.custom("main_table", ip -> new BlockItem(BLOCK, ip.useBlockDescriptionPrefix()));
        MISSION_BOARD_ITEM = ModItems.custom("mission_board", p -> new BoardItem(p.stacksTo(4)));
    }

    /** places a mission board where you click, facing you */
    public static class BoardItem extends Item {
        public BoardItem(Properties p) { super(p); }
        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            if (!(ctx.getLevel() instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
            MissionBoard b = ModTwd.MISSION_BOARD.create(sl, EntitySpawnReason.SPAWN_ITEM_USE);
            if (b == null) return InteractionResult.FAIL;
            float yaw = ctx.getPlayer() == null ? 0 : ctx.getPlayer().getYRot() + 180;
            b.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
            b.setYHeadRot(yaw); b.setYBodyRot(yaw);
            sl.addFreshEntity(b);
            ctx.getItemInHand().consume(1, ctx.getPlayer());
            sl.playSound(null, at, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 1f);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Menu.open(sp);
        return InteractionResult.SUCCESS;
    }

    static boolean has(ServerPlayer p, Station s) {
        if (p.isCreative()) return true;
        for (int i = 0; i < s.cost().length; i += 2) if (Shop.count(p, (String) s.cost()[i]) < (Integer) s.cost()[i + 1]) return false;
        return true;
    }

    public static class Menu extends PanelMenu {
        static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

        static void open(ServerPlayer p) { p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, p), Component.literal("Main Table"))); }

        private Menu(int id, Inventory inv, ServerPlayer p) { super(id, inv, p); redraw(); }

        @Override
        protected void draw() {
            c.setItem(4, Ui.icon(BLOCK.asItem(), Ui.text("Main Table", ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text("Every TWD station and settlement block is built here", ChatFormatting.GRAY)));
            for (int i = 0; i < STATIONS.size() && i < SLOTS.length; i++) {
                Station s = STATIONS.get(i);
                boolean ok = has(player, s);
                List<Component> lore = new ArrayList<>();
                lore.add(Ui.text(s.what(), ChatFormatting.AQUA));
                for (int j = 0; j < s.cost().length; j += 2) {
                    String id = (String) s.cost()[j]; int n = (Integer) s.cost()[j + 1], have = Shop.count(player, id);
                    lore.add(Component.literal((have >= n ? "✔ " : "✘ ") + n + "× ").append(new ItemStack(Ui.item(id)).getHoverName()).withStyle(have >= n ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                }
                lore.add(Ui.text(ok ? "Click to build!" : "Missing materials", ok ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
                c.setItem(SLOTS[i], Ui.glint(Ui.lore(new ItemStack(Ui.item(s.id())), lore), ok));
            }
            c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
        }

        @Override
        protected boolean click(int slot, ClickType type) {
            if (slot == 49) { player.closeContainer(); return false; }
            for (int i = 0; i < STATIONS.size() && i < SLOTS.length; i++) {
                if (SLOTS[i] != slot) continue;
                Station s = STATIONS.get(i);
                if (!has(player, s)) return false;
                if (!player.isCreative()) for (int j = 0; j < s.cost().length; j += 2) Shop.take(player, (String) s.cost()[j], (Integer) s.cost()[j + 1]);
                Shop.give(player, new ItemStack(Ui.item(s.id())));
                player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1f);
                return true;
            }
            return false;
        }
    }
}
