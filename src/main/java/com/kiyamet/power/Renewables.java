package com.kiyamet.power;

import com.kiyamet.Kiyamet;
import com.kiyamet.registry.ModItems;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.9 clean power. Solar panels power everything within 8 blocks while the sun is up (and they can see the sky);
 * wind turbines turn day and night - faster up high and in storms. Both count as power for turrets, electric fences,
 * lights... exactly like a running generator, but need no fuel. Electric tools charge while you stand in power:
 * the drill mines 3x3, the chainsaw fells a whole tree.
 */
public final class Renewables {
    public static Block SOLAR, TURBINE;
    public static Item DRILL, CHAINSAW;
    public static final int MAX_CHARGE = 400;

    public static class Source extends Block {
        final boolean solar;
        final VoxelShape shape;
        public Source(Properties p, boolean solar) { super(p); this.solar = solar; shape = solar ? Block.box(0, 0, 0, 16, 6, 16) : Block.box(5, 0, 5, 11, 16, 11); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return shape; }
        public boolean producing(Level level, BlockPos pos) {
            if (solar) return level.isBrightOutside() && level.canSeeSky(pos.above());
            return level.canSeeSky(pos.above(2)) || pos.getY() > 90;
        }
    }

    static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kiyamet.id(name));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
        ModItems.custom(name, ip -> new BlockItem(b, ip.useBlockDescriptionPrefix()));
        return b;
    }

    /** is a solar panel or turbine producing power within range of pos? (used by Power.isPowered) */
    public static boolean near(ServerLevel level, BlockPos pos, int range) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-range, -range, -range), pos.offset(range, range, range))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof Source src && src.producing(level, p)) return true;
        }
        return false;
    }

    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static int charge(ItemStack s) { return tag(s).getIntOr("Charge", MAX_CHARGE / 2); }
    static void setCharge(ItemStack s, int c) { CompoundTag t = tag(s); t.putInt("Charge", Math.max(0, Math.min(MAX_CHARGE, c))); s.set(DataComponents.CUSTOM_DATA, CustomData.of(t)); }

    public static void init() {
        SOLAR = block("solar_panel", p -> new Source(p, true), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2f).noOcclusion());
        TURBINE = block("wind_turbine", p -> new Source(p, false), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2f).noOcclusion());
        DRILL = ModItems.custom("electric_drill", p -> new Item(p.pickaxe(ToolMaterial.DIAMOND, 1f, -2.6f).durability(2400)));
        CHAINSAW = ModItems.custom("chainsaw", p -> new Item(p.axe(ToolMaterial.DIAMOND, 7f, -3.0f).durability(2400)));
        // charging: every second, tools in your inventory charge while you stand in power
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                boolean powered = false; boolean checked = false;
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack s = p.getInventory().getItem(i);
                    if (!(s.is(DRILL) || s.is(CHAINSAW)) || charge(s) >= MAX_CHARGE) continue;
                    if (!checked) { powered = com.kiyamet.defense.Power.isPowered(p.level(), p.blockPosition()); checked = true; }
                    if (!powered) break;
                    setCharge(s, charge(s) + 10);
                }
            }
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (!(player instanceof ServerPlayer p) || !(level instanceof ServerLevel sl) || p.isShiftKeyDown()) return;
            ItemStack tool = p.getMainHandItem();
            if (tool.is(DRILL) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) drill(sl, p, tool, pos);
            else if (tool.is(CHAINSAW) && state.is(BlockTags.LOGS)) fell(sl, p, tool, pos, state);
        });
    }

    static boolean busy = false;

    static void drill(ServerLevel l, ServerPlayer p, ItemStack tool, BlockPos pos) {
        if (busy) return;
        if (charge(tool) <= 0) { p.displayClientMessage(Component.literal("Drill battery empty - stand near a generator, solar panel or turbine.").withStyle(ChatFormatting.RED), true); return; }
        Direction face = p.getDirection(); boolean vertical = Math.abs(p.getXRot()) > 50;
        busy = true;
        int n = 0;
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            if (a == 0 && b == 0) continue;
            BlockPos q = vertical ? pos.offset(a, 0, b) : face.getAxis() == Direction.Axis.X ? pos.offset(0, a, b) : pos.offset(a, b, 0);
            BlockState s = l.getBlockState(q);
            if (s.isAir() || !s.is(BlockTags.MINEABLE_WITH_PICKAXE) || s.getDestroySpeed(l, q) < 0 || s.getDestroySpeed(l, q) > 50) continue;
            if (p.gameMode.destroyBlock(q)) n++;
        }
        busy = false;
        setCharge(tool, charge(tool) - 1 - n / 3);
        if (n > 0) l.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
        p.displayClientMessage(Component.literal("Drill: " + charge(tool) + "/" + MAX_CHARGE).withStyle(ChatFormatting.AQUA), true);
    }

    static void fell(ServerLevel l, ServerPlayer p, ItemStack tool, BlockPos start, BlockState st) {
        if (busy) return;
        if (charge(tool) <= 0) { p.displayClientMessage(Component.literal("Chainsaw battery empty - stand near a generator, solar panel or turbine.").withStyle(ChatFormatting.RED), true); return; }
        Set<Long> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start); seen.add(start.asLong());
        List<BlockPos> logs = new java.util.ArrayList<>();
        while (!q.isEmpty() && logs.size() < 96) {
            BlockPos c = q.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = c.offset(dx, dy, dz);
                if (seen.add(n.asLong()) && l.getBlockState(n).is(BlockTags.LOGS)) { logs.add(n); q.add(n); }
            }
        }
        busy = true;
        for (BlockPos b : logs) if (p.gameMode.destroyBlock(b)) {}
        busy = false;
        setCharge(tool, charge(tool) - 1 - logs.size() / 4);
        l.playSound(null, start, SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 0.8f, 1.8f);
        p.displayClientMessage(Component.literal("Chainsaw: " + charge(tool) + "/" + MAX_CHARGE + " · felled " + (logs.size() + 1) + " logs").withStyle(ChatFormatting.AQUA), true);
    }

    private Renewables() {}
}
