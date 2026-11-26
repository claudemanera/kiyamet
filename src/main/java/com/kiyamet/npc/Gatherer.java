package com.kiyamet.npc;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Companion gathering (1.6). Orders: wood, stone, coal, iron, copper, gold, diamond, sand, dirt, "the block in my hand",
 * or DEVELOP: chops wood, makes wooden tools, mines stone, makes stone tools, mines iron and coal, smelts it,
 * makes itself iron armor and an iron pickaxe, then keeps mining ores for you. Everything goes into its inventory.
 */
public class Gatherer extends Goal {
    public static final String[][] KINDS = {{"develop", "Develop: tools → armor → ores"}, {"wood", "Wood"}, {"stone", "Stone"}, {"coal", "Coal"}, {"iron", "Iron"},
            {"copper", "Copper"}, {"gold", "Gold"}, {"diamond", "Diamonds"}, {"sand", "Sand"}, {"dirt", "Dirt"}};
    private final HumanEntity h;
    private BlockPos target;
    private int mining, search, stuck;
    private double lastDist;

    public Gatherer(HumanEntity h) { this.h = h; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

    @Override public boolean canUse() { return h.order() == HumanEntity.Order.GATHER && h.getTarget() == null && h.level() instanceof ServerLevel; }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void stop() { target = null; mining = 0; h.level().destroyBlockProgress(h.getId(), BlockPos.ZERO, -1); }

    // ------------------------------------------------------------------ what to look for
    static boolean wants(String kind, BlockState s) {
        if (s.isAir()) return false;
        return switch (kind) {
            case "wood" -> s.is(BlockTags.LOGS);
            case "stone" -> s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE) || s.is(Blocks.TUFF);
            case "coal" -> s.is(BlockTags.COAL_ORES);
            case "iron" -> s.is(BlockTags.IRON_ORES);
            case "copper" -> s.is(BlockTags.COPPER_ORES);
            case "gold" -> s.is(BlockTags.GOLD_ORES);
            case "diamond" -> s.is(BlockTags.DIAMOND_ORES);
            case "sand" -> s.is(BlockTags.SAND);
            case "dirt" -> s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.COARSE_DIRT);
            default -> kind.startsWith("block:") && BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString().equals(kind.substring(6));
        };
    }

    int count(Item it) { int n = 0; SimpleContainer c = h.inventory(); for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).is(it)) n += c.getItem(i).getCount(); return n; }
    int countTag(net.minecraft.tags.TagKey<Item> t) { int n = 0; SimpleContainer c = h.inventory(); for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).is(t)) n += c.getItem(i).getCount(); return n; }
    boolean take(Item it, int n) { SimpleContainer c = h.inventory(); if (count(it) < n) return false; for (int i = 0; i < c.getContainerSize() && n > 0; i++) if (c.getItem(i).is(it)) { int k = Math.min(n, c.getItem(i).getCount()); c.getItem(i).shrink(k); n -= k; } return true; }
    boolean takeTag(net.minecraft.tags.TagKey<Item> t, int n) { SimpleContainer c = h.inventory(); if (countTag(t) < n) return false; for (int i = 0; i < c.getContainerSize() && n > 0; i++) if (c.getItem(i).is(t)) { int k = Math.min(n, c.getItem(i).getCount()); c.getItem(i).shrink(k); n -= k; } return true; }
    boolean has(Item it) { return count(it) > 0 || h.getMainHandItem().is(it); }
    void give(ItemStack s) { ItemStack left = h.inventory().addItem(s); if (!left.isEmpty()) h.spawnAtLocation((ServerLevel) h.level(), left); }

    int pickTier() {
        if (has(Items.DIAMOND_PICKAXE)) return 4; if (has(Items.IRON_PICKAXE)) return 3; if (has(Items.STONE_PICKAXE)) return 2; if (has(Items.WOODEN_PICKAXE)) return 1; return 0;
    }

    void say(String s) {
        if (h.ownerPlayer() instanceof ServerPlayer p && p.distanceTo(h) < 64)
            p.sendSystemMessage(Component.literal("<" + h.getName().getString() + "> ").withStyle(ChatFormatting.AQUA).append(Component.literal(s).withStyle(ChatFormatting.WHITE)));
    }

    /** Develop mode: craft and decide what to gather next. */
    String develop() {
        int logs = countTag(net.minecraft.tags.ItemTags.LOGS);
        int tier = pickTier();
        if (tier == 0) {
            if (logs >= 5) { takeTag(net.minecraft.tags.ItemTags.LOGS, 5); give(new ItemStack(Items.WOODEN_PICKAXE)); give(new ItemStack(Items.WOODEN_AXE)); give(new ItemStack(Items.STICK, 4));
                say("Made a wooden pickaxe and axe. Going for stone."); return "stone"; }
            return "wood";
        }
        if (tier == 1) {
            if (count(Items.COBBLESTONE) + count(Items.COBBLED_DEEPSLATE) >= 8 && (logs >= 1 || count(Items.STICK) >= 3)) {
                if (!take(Items.COBBLESTONE, 8)) { int c = count(Items.COBBLESTONE); take(Items.COBBLESTONE, c); take(Items.COBBLED_DEEPSLATE, 8 - c); }
                give(new ItemStack(Items.STONE_PICKAXE)); give(new ItemStack(Items.STONE_AXE)); give(new ItemStack(Items.STONE_SWORD));
                say("Stone tools done. Now I need iron and coal."); return "iron";
            }
            return logs < 1 && count(Items.STICK) < 3 ? "wood" : "stone";
        }
        boolean armored = h.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE);
        if (tier == 2 || !armored) {
            int raw = count(Items.RAW_IRON) + count(Items.IRON_INGOT), coal = count(Items.COAL) + count(Items.CHARCOAL);
            if (raw >= 27 && coal >= 4) {
                take(Items.RAW_IRON, Math.min(count(Items.RAW_IRON), 27)); int ci = 27 - 0; if (!take(Items.COAL, 4)) take(Items.CHARCOAL, 4);
                h.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)); h.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                h.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS)); h.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                give(new ItemStack(Items.IRON_PICKAXE)); give(new ItemStack(Items.IRON_AXE));
                say("Smelted the iron. I've got full iron armor and an iron pickaxe now! I'll mine ores for you.");
                return "coal";
            }
            return coal < 4 ? (h.getRandom().nextBoolean() ? "coal" : "iron") : "iron";
        }
        // geared up: keep mining what's valuable
        int r = (h.tickCount / 2400) % 4;
        return r == 0 ? "diamond" : r == 1 ? "iron" : r == 2 ? "gold" : "coal";
    }

    void tool(BlockState s) {
        Item best = null;
        if (s.is(BlockTags.MINEABLE_WITH_AXE)) best = has(Items.IRON_AXE) ? Items.IRON_AXE : has(Items.STONE_AXE) ? Items.STONE_AXE : has(Items.WOODEN_AXE) ? Items.WOODEN_AXE : null;
        else if (s.is(BlockTags.MINEABLE_WITH_PICKAXE)) best = has(Items.DIAMOND_PICKAXE) ? Items.DIAMOND_PICKAXE : has(Items.IRON_PICKAXE) ? Items.IRON_PICKAXE : has(Items.STONE_PICKAXE) ? Items.STONE_PICKAXE : has(Items.WOODEN_PICKAXE) ? Items.WOODEN_PICKAXE : null;
        else if (s.is(BlockTags.MINEABLE_WITH_SHOVEL)) best = null;
        if (best == null || h.getMainHandItem().is(best)) return;
        // swap: hand item into the bag, tool out of the bag
        SimpleContainer c = h.inventory();
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).is(best)) {
            ItemStack t = c.getItem(i).copy(); c.setItem(i, h.getMainHandItem().copy()); h.setItemSlot(EquipmentSlot.MAINHAND, t); return;
        }
    }

    boolean canHarvest(BlockState s) {
        if (!s.requiresCorrectToolForDrops()) return true;
        int need = s.is(BlockTags.NEEDS_DIAMOND_TOOL) ? 3 : s.is(BlockTags.NEEDS_IRON_TOOL) ? 2 : s.is(BlockTags.NEEDS_STONE_TOOL) ? 1 : 1;
        return pickTier() >= need + (need == 1 ? 0 : 0) || (need == 1 && pickTier() >= 1);
    }

    BlockPos find(ServerLevel level, String kind) {
        BlockPos c = h.blockPosition(); BlockPos best = null; double bd = Double.MAX_VALUE;
        int r = kind.equals("wood") || kind.equals("sand") || kind.equals("dirt") ? 18 : 14;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -10, -r), c.offset(r, 8, r))) {
            BlockState s = level.getBlockState(p);
            if (!wants(kind, s) || !canHarvest(s)) continue;
            double d = p.distSqr(c) + Math.abs(p.getY() - c.getY()) * 6;
            if (d < bd) { bd = d; best = p.immutable(); }
        }
        return best;
    }

    @Override
    public void tick() {
        ServerLevel level = (ServerLevel) h.level();
        String kind = h.gather.equals("develop") ? develop() : h.gather;
        if (h.inventory().getItem(h.inventory().getContainerSize() - 1).isEmpty() == false && full()) {
            say("My bag is full - coming back to you."); h.setOrder(HumanEntity.Order.FOLLOW); return;
        }
        if (target == null || !wants(kind, level.getBlockState(target)) && !level.getBlockState(target).isAir() && mining == 0) {
            if (--search > 0) return;
            search = 30;
            target = find(level, kind);
            mining = 0; stuck = 0; lastDist = 999;
            if (target == null) {
                if (h.tickCount % 400 < 30) say(switch (kind) { case "wood" -> "No trees around here."; case "stone" -> "Can't find stone nearby."; default -> "No " + kind + " nearby. Take me somewhere else?"; });
                if (h.ownerPlayer() instanceof ServerPlayer p && h.distanceTo(p) > 10) h.getNavigation().moveTo(p, 1.0);
                return;
            }
        }
        if (level.getBlockState(target).isAir()) { target = null; return; }
        Vec3 c = Vec3.atCenterOf(target);
        double d = h.getEyePosition().distanceTo(c);
        h.getLookControl().setLookAt(c.x, c.y, c.z);
        if (d > 4.2) {
            if (h.tickCount % 10 == 0) {
                h.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
                if (d > lastDist - 0.3) stuck++; else stuck = 0;
                lastDist = d;
            }
            if (stuck > 6) tunnel(level, c);        // can't get there: dig a way through
            return;
        }
        h.getNavigation().stop();
        mine(level, target);
    }

    boolean full() { SimpleContainer c = h.inventory(); for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).isEmpty()) return false; return true; }

    void tunnel(ServerLevel level, Vec3 to) {
        Vec3 dir = to.subtract(h.position()).normalize();
        BlockPos feet = h.blockPosition();
        BlockPos step = BlockPos.containing(h.getX() + dir.x * 1.2, h.getY() + (dir.y < -0.5 ? -1 : 0), h.getZ() + dir.z * 1.2);
        for (BlockPos p : new BlockPos[]{step, step.above()}) {
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && s.getDestroySpeed(level, p) >= 0 && s.getDestroySpeed(level, p) < 30 && s.getFluidState().isEmpty() && !p.equals(feet.below())) { mine(level, p); return; }
        }
        stuck = 0;
    }

    void mine(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        tool(s);
        float hard = s.getDestroySpeed(level, p);
        float speed = Math.max(1f, h.getMainHandItem().getDestroySpeed(s));
        int need = Math.max(4, (int) (hard * 30 / speed));
        mining++;
        if (mining % 5 == 0) h.swing(InteractionHand.MAIN_HAND);
        level.destroyBlockProgress(h.getId(), p, Math.min(9, mining * 10 / need));
        if (mining < need) return;
        mining = 0;
        level.destroyBlockProgress(h.getId(), p, -1);
        List<ItemStack> drops = Block.getDrops(s, level, p, level.getBlockEntity(p), h, h.getMainHandItem());
        level.destroyBlock(p, false, h);
        for (ItemStack it : drops) give(it);
        if (p.equals(target)) target = null;
    }
}
