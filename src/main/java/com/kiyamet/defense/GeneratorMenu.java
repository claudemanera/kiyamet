package com.kiyamet.defense;

import com.kiyamet.registry.ModBlocks;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generator control panel. Shows fuel, a master switch and every powered device group (connected electric fences /
 * energy walls) within range; click a group to switch it on or off.
 */
public class GeneratorMenu extends ChestMenu {
    private static final int DEVICE_START = 9, DEVICE_SLOTS = 27;
    private static final int FUEL = 2, STATUS = 4, MASTER = 6, ALL_ON = 47, ALL_OFF = 51, FEED = 49;

    record Group(Block block, List<BlockPos> blocks) {
        BlockPos anchor() { return blocks.get(0); }
    }

    private final SimpleContainer shown;
    private final ServerLevel level;
    private final BlockPos pos;
    private List<Group> groups = List.of();

    public static void open(Player player, ServerLevel level, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new GeneratorMenu(id, inv, level, pos), Component.literal("Generator Control Panel")));
    }

    private GeneratorMenu(int id, Inventory inv, ServerLevel level, BlockPos pos) {
        this(id, inv, new SimpleContainer(54), level, pos);
    }

    private GeneratorMenu(int id, Inventory inv, SimpleContainer c, ServerLevel level, BlockPos pos) {
        super(MenuType.GENERIC_9x6, id, inv, c, 6);
        this.shown = c;
        this.level = level;
        this.pos = pos;
        scan();
        refresh();
    }

    private static boolean powered(Block b) { return b == ModBlocks.ELECTRIC_FENCE || b == ModBlocks.ENERGY_WALL; }

    private void scan() {
        Set<BlockPos> seen = new HashSet<>();
        List<Group> out = new ArrayList<>();
        int r = Math.min(40, Power.range(level, pos));
        for (BlockPos p0 : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
            Block b = level.getBlockState(p0).getBlock();
            if (!powered(b) || seen.contains(p0)) continue;
            List<BlockPos> blocks = new ArrayList<>();
            ArrayDeque<BlockPos> q = new ArrayDeque<>();
            q.add(p0.immutable());
            seen.add(p0.immutable());
            while (!q.isEmpty()) {
                BlockPos c = q.poll();
                blocks.add(c);
                for (BlockPos n : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) {
                    if (Math.abs(n.getX() - pos.getX()) > r || Math.abs(n.getY() - pos.getY()) > r || Math.abs(n.getZ() - pos.getZ()) > r) continue;
                    if (!seen.contains(n) && level.getBlockState(n).getBlock() == b) { BlockPos im = n.immutable(); seen.add(im); q.add(im); }
                }
            }
            out.add(new Group(b, blocks));
            if (out.size() >= DEVICE_SLOTS) break;
        }
        groups = out;
    }

    private boolean isOn(Group g) { return !DeviceSwitch.isOff(level, g.anchor()); }

    private void refresh() {
        shown.clearContent();
        for (int i = 0; i < 54; i++) shown.setItem(i, i < 9 || i >= 45 ? Ui.dark() : ItemStack.EMPTY);
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof GeneratorBlock)) return;
        int fuel = st.getValue(GeneratorBlock.FUEL);
        boolean lit = st.getValue(GeneratorBlock.LIT), on = st.getValue(GeneratorBlock.ON);

        ItemStack gauge = Ui.icon(Items.COAL, Ui.text("Fuel: " + fuel + " / 15 min", ChatFormatting.GOLD),
                Ui.bar(fuel / 15.0, 15), Ui.text("Fuel canister = 4 min, coal = 1 min", ChatFormatting.GRAY),
                Ui.text("Coal block = 9 min, lava bucket = 10 min", ChatFormatting.GRAY));
        gauge.setCount(Math.max(1, fuel));
        shown.setItem(FUEL, gauge);
        shown.setItem(STATUS, Ui.glint(Ui.icon(lit && on ? Items.BLAST_FURNACE : Items.FURNACE,
                Ui.text("Generator", ChatFormatting.AQUA, ChatFormatting.BOLD),
                Ui.text(lit && on ? "● Running" : on ? "● Out of fuel" : "● Paused", lit && on ? ChatFormatting.GREEN : ChatFormatting.RED),
                Ui.text("Range: " + Power.range(level, pos) + " blocks", ChatFormatting.GRAY),
                Ui.text("Cluster: " + Power.cluster(level, pos).size() + " generators (" + Power.runningInCluster(level, pos) + " running, max " + Power.MAX_CLUSTER + ")", ChatFormatting.GRAY),
                Ui.text("Place generators touching each other: +" + Power.PER_EXTRA + " range each, slower fuel use", ChatFormatting.DARK_GRAY),
                Ui.text("Devices in range: " + groups.size() + " groups", ChatFormatting.GRAY)), lit && on));
        shown.setItem(MASTER, Ui.icon(on ? Items.LIME_DYE : Items.RED_DYE,
                Ui.text(on ? "Master switch: ON" : "Master switch: OFF", on ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD),
                Ui.text("Click to " + (on ? "pause (keeps fuel)" : "resume"), ChatFormatting.GRAY)));

        if (groups.isEmpty()) {
            shown.setItem(22, Ui.icon(Items.BARRIER, Ui.text("No devices in range", ChatFormatting.RED),
                    Ui.text("Place electric fences or energy walls", ChatFormatting.GRAY), Ui.text("within " + Power.RANGE + " blocks of the generator.", ChatFormatting.GRAY)));
        }
        int fence = 0, wall = 0;
        for (int i = 0; i < groups.size(); i++) {
            Group g = groups.get(i);
            boolean gOn = isOn(g);
            boolean isFence = g.block() == ModBlocks.ELECTRIC_FENCE;
            String name = (isFence ? "Electric Fence #" + (++fence) : "Energy Wall #" + (++wall));
            BlockPos a = g.anchor();
            ItemStack s = Ui.icon(g.block().asItem(), Ui.text(name, gOn ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD),
                    Ui.text(gOn ? "● Active" : "● Switched off", gOn ? ChatFormatting.GREEN : ChatFormatting.DARK_RED),
                    Ui.text(g.blocks().size() + " blocks", ChatFormatting.GRAY),
                    Ui.text("At " + a.getX() + ", " + a.getY() + ", " + a.getZ() + "  (" + (int) Math.sqrt(a.distSqr(pos)) + " m)", ChatFormatting.DARK_GRAY),
                    Ui.text(isFence ? "Shocks zombies: 6 dmg + stun" : "Unbreakable while powered, knocks zombies back", ChatFormatting.GRAY),
                    Ui.text("Click to switch " + (gOn ? "off" : "on"), ChatFormatting.YELLOW));
            s.setCount(Math.min(64, g.blocks().size()));
            shown.setItem(DEVICE_START + i, Ui.glint(s, gOn && lit && on));
        }
        shown.setItem(ALL_ON, Ui.icon(Items.LIME_CONCRETE, Ui.text("Switch all ON", ChatFormatting.GREEN)));
        shown.setItem(FEED, Ui.icon(Items.HOPPER, Ui.text("Add fuel", ChatFormatting.GOLD),
                Ui.text("Click here holding fuel on the cursor", ChatFormatting.GRAY)));
        shown.setItem(ALL_OFF, Ui.icon(Items.GRAY_CONCRETE, Ui.text("Switch all OFF", ChatFormatting.RED)));
    }

    private void setGroup(Group g, boolean on) {
        for (BlockPos p : g.blocks()) DeviceSwitch.set(level, p, on);
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot < 0 || slot >= 54) { if (type != ClickType.QUICK_MOVE) super.clicked(slot, button, type, player); return; }
        if (!(player instanceof ServerPlayer sp)) return;
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof GeneratorBlock)) { sp.closeContainer(); return; }
        boolean changed = true;
        if (slot == MASTER) {
            boolean on = !st.getValue(GeneratorBlock.ON);
            level.setBlock(pos, st.setValue(GeneratorBlock.ON, on), 3);
            if (on && st.getValue(GeneratorBlock.LIT)) level.scheduleTick(pos, st.getBlock(), 1200);
        } else if (slot == FEED || slot == FUEL) {
            ItemStack carried = getCarried();
            changed = !carried.isEmpty() && GeneratorBlock.addFuel(level, pos, carried);
            if (changed && !sp.isCreative()) carried.shrink(1);
        } else if (slot == ALL_ON || slot == ALL_OFF) {
            for (Group g : groups) setGroup(g, slot == ALL_ON);
        } else if (slot >= DEVICE_START && slot < DEVICE_START + groups.size()) {
            Group g = groups.get(slot - DEVICE_START);
            setGroup(g, !isOn(g));
        } else changed = false;
        if (changed) {
            Power.invalidate();
            level.playSound(null, sp.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7f, 1.3f);
            refresh();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public void removed(Player player) {
        shown.clearContent();
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) { return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64; }
}
