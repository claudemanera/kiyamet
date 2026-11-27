package com.kiyamet.food;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.Shop;
import com.kiyamet.registry.ModItems;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** 1.9 Cooking Table: every meal is cooked here. Pick a dish, and if you carry the ingredients it's made on the spot. */
public class CookingTable extends Block {
    public static Block BLOCK;
    static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public CookingTable(Properties p) { super(p); }

    public static void init() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kiyamet.id("cooking_table"));
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new CookingTable(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(2.5f).noOcclusion().setId(key)));
        ModItems.custom("cooking_table", ip -> new BlockItem(BLOCK, ip.useBlockDescriptionPrefix()));
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) Menu.open(sp, pos);
        return InteractionResult.SUCCESS;
    }

    static Map<String, Integer> need(CookList.Cook c) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String i : c.ingredients()) m.merge(i, 1, Integer::sum);
        return m;
    }

    static boolean has(ServerPlayer p, CookList.Cook c) {
        if (p.isCreative()) return true;
        for (var e : need(c).entrySet()) if (Shop.count(p, e.getKey()) < e.getValue()) return false;
        return true;
    }

    public static class Menu extends PanelMenu {
        static final int[] SLOTS = new int[36];
        static { for (int i = 0; i < 36; i++) SLOTS[i] = 9 + i; }
        final BlockPos pos;

        static void open(ServerPlayer p, BlockPos pos) { p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, p, pos), Component.literal("Cooking Table"))); }

        private Menu(int id, Inventory inv, ServerPlayer p, BlockPos pos) { super(id, inv, p); this.pos = pos; redraw(); }

        @Override
        protected void draw() {
            c.setItem(4, Ui.icon(BLOCK.asItem(), Ui.text("Cooking Table", ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text("Click a dish to cook it (shift-click: cook as many as you can)", ChatFormatting.GRAY)));
            List<CookList.Cook> all = CookList.ALL;
            for (int i = 0; i < all.size() && i < SLOTS.length; i++) {
                CookList.Cook ck = all.get(i);
                boolean ok = has(player, ck);
                Recipes.Meal m = Recipes.meal(ck.meal());
                List<Component> lore = new ArrayList<>();
                if (m != null) lore.add(Ui.text("Hunger +" + m.food() + "  ·  Well Fed " + m.fedMinutes() + " min  ·  keeps " + m.days() + " days", ChatFormatting.AQUA));
                for (var e : need(ck).entrySet()) {
                    int have = Shop.count(player, e.getKey());
                    lore.add(Component.literal((have >= e.getValue() ? "✔ " : "✘ ") + e.getValue() + "× ").append(new ItemStack(Ui.item(e.getKey())).getHoverName())
                            .withStyle(have >= e.getValue() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                }
                lore.add(Ui.text(ok ? "Click to cook (makes " + ck.count() + ")" : "Missing ingredients", ok ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
                ItemStack icon = new ItemStack(ModItems.get(ck.meal()), ck.count());
                c.setItem(SLOTS[i], Ui.glint(Ui.lore(icon, lore), ok));
            }
            c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
        }

        @Override
        protected boolean click(int slot, ClickType type) {
            if (slot == 49) { player.closeContainer(); return false; }
            List<CookList.Cook> all = CookList.ALL;
            for (int i = 0; i < all.size() && i < SLOTS.length; i++) {
                if (SLOTS[i] != slot) continue;
                CookList.Cook ck = all.get(i);
                int times = 0;
                do {
                    if (!has(player, ck)) break;
                    if (!player.isCreative()) for (var e : need(ck).entrySet()) {
                        Shop.take(player, e.getKey(), e.getValue());
                        if (e.getKey().equals("minecraft:milk_bucket")) Shop.give(player, new ItemStack(Items.BUCKET, e.getValue()));
                        if (e.getKey().equals("minecraft:honey_bottle")) Shop.give(player, new ItemStack(Items.GLASS_BOTTLE, e.getValue()));
                    }
                    Shop.give(player, new ItemStack(ModItems.get(ck.meal()), ck.count()));
                    times++;
                } while (type == ClickType.QUICK_MOVE && times < 16);
                if (times == 0) return false;
                com.kiyamet.skill.SkillManager.addXp(player, com.kiyamet.skill.Skill.SURVIVAL, 10 * times);
                player.level().playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1f);
                player.level().playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.4f, 1.4f);
                player.level().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.3, pos.getY() + 1.2, pos.getZ() + 0.3, 3, 0.05, 0.1, 0.05, 0.01);
                return true;
            }
            return false;
        }
    }
}
