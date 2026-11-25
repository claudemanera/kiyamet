package com.kiyamet.defense;

import com.kiyamet.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/** Zombies chip away "durability points" of the blocks in their way. Damage slowly heals if left alone. */
public final class BlockDurability {
    private record Hit(float damage, long lastTick) {}
    private static final Map<ResourceKey<Level>, Map<Long, Hit>> DAMAGE = new HashMap<>();
    private static final Map<Block, Float> HP = new HashMap<>();

    static {
        HP.put(ModBlocks.REINFORCED_PLANKS, 60f);
        HP.put(ModBlocks.STEEL_BLOCK, 150f);
        HP.put(ModBlocks.STEEL_GRATE, 120f);
        HP.put(ModBlocks.RUBY_BLOCK, 260f);
        HP.put(ModBlocks.TITANIUM_BLOCK, 400f);
        HP.put(ModBlocks.ENERGY_WALL, 300f);
        HP.put(ModBlocks.STEEL_GATE, 220f);
        HP.put(Blocks.OBSIDIAN, 500f);
        HP.put(Blocks.IRON_BLOCK, 120f);
        HP.put(Blocks.IRON_DOOR, 80f);
        HP.put(Blocks.IRON_BARS, 60f);
    }

    public static float maxHp(BlockState state) {
        Float hp = HP.get(state.getBlock());
        if (hp != null) return hp;
        float hardness = state.getBlock().defaultDestroyTime();
        if (hardness < 0) return -1;                 // unbreakable
        return Math.max(4f, hardness * 14f);
    }

    public static boolean breakable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty() && maxHp(state) > 0;
    }

    /** Applies damage; returns true if the block broke. */
    public static boolean hit(ServerLevel level, BlockPos pos, float amount, Entity breaker) {
        BlockState state = level.getBlockState(pos);
        if (!breakable(level, pos, state)) return false;
        if (state.is(ModBlocks.ENERGY_WALL) && Power.isPowered(level, pos)) return false;   // powered energy walls hold
        float max = maxHp(state);
        Map<Long, Hit> map = DAMAGE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        long key = pos.asLong();
        long now = level.getGameTime();
        Hit old = map.get(key);
        float dmg = (old == null ? 0 : healed(old, now)) + amount;
        if (dmg >= max) {
            map.remove(key);
            level.destroyBlockProgress(breaker.getId(), pos, -1);
            level.destroyBlock(pos, false, breaker);
            return true;
        }
        map.put(key, new Hit(dmg, now));
        level.destroyBlockProgress(breaker.getId(), pos, Math.min(9, (int) (dmg / max * 10)));
        return false;
    }

    private static float healed(Hit h, long now) {
        long idle = now - h.lastTick;
        return idle > 1200 ? Math.max(0, h.damage - (idle - 1200) / 200f) : h.damage;   // after 1 min, 1 point per 10 s
    }

    public static float damage(ServerLevel level, BlockPos pos) {
        Hit h = DAMAGE.getOrDefault(level.dimension(), Map.of()).get(pos.asLong());
        return h == null ? 0 : healed(h, level.getGameTime());
    }

    /** Repairs a fraction (0..1) of the block's max durability. Returns false if there was nothing to repair. */
    public static boolean repair(ServerLevel level, BlockPos pos, float fraction) {
        Map<Long, Hit> map = DAMAGE.get(level.dimension());
        if (map == null) return false;
        Hit h = map.get(pos.asLong());
        if (h == null) return false;
        float left = h.damage - maxHp(level.getBlockState(pos)) * fraction;
        if (left <= 0) map.remove(pos.asLong());
        else map.put(pos.asLong(), new Hit(left, h.lastTick));
        for (int i = 0; i < 1; i++) level.destroyBlockProgress(pos.hashCode(), pos, -1);
        return true;
    }

    /** Damaged blocks within r blocks of a point. */
    public static java.util.List<BlockPos> damagedNear(ServerLevel level, BlockPos c, int r) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        for (long l : DAMAGE.getOrDefault(level.dimension(), Map.of()).keySet()) {
            BlockPos p = BlockPos.of(l);
            if (p.distSqr(c) <= r * r) out.add(p);
        }
        return out;
    }

    private BlockDurability() {}
}
