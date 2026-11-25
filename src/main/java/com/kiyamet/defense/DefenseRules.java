package com.kiyamet.defense;

import com.kiyamet.entity.KZombie;
import com.kiyamet.entity.ZombieKind;
import com.kiyamet.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Traps that hurt zombies, and zombies breaking through walls. Called from {@link KZombie#tick()}. */
public final class DefenseRules {
    private static final Map<ResourceKey<Level>, Map<Long, Integer>> WEAR = new HashMap<>();

    public static void tickZombie(ServerLevel level, KZombie z) {
        if (z.tickCount % 20 == 0) traps(level, z);
        if (z.tickCount % 20 == 10) breakWalls(level, z);
    }

    private static void traps(ServerLevel level, KZombie z) {
        BlockPos pos = z.blockPosition();
        for (BlockPos p : new BlockPos[]{pos, pos.above()}) {
            BlockState s = level.getBlockState(p);
            Block b = s.getBlock();
            if (b == ModBlocks.BARBED_WIRE) { hurt(level, z, 2); wear(level, p, 60); }
            else if (b == ModBlocks.RAZOR_WIRE) { hurt(level, z, 3); z.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0)); wear(level, p, 90); }
            else if (b == ModBlocks.SPIKE_TRAP) { hurt(level, z, 10); wear(level, p, 20); }
            else if (b == ModBlocks.ELECTRIC_FENCE && Power.isPowered(level, p)) {
                hurt(level, z, 6);
                z.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 5));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, z.getX(), z.getY() + 1, z.getZ(), 15, 0.3, 0.5, 0.3, 0.2);
                level.playSound(null, p, SoundEvents.BEE_STING, SoundSource.BLOCKS, 0.8f, 0.5f);
            }
        }
    }

    private static void hurt(ServerLevel level, LivingEntity e, float amount) {
        e.hurtServer(level, level.damageSources().cactus(), amount);
    }

    private static void wear(ServerLevel level, BlockPos p, int uses) {
        Map<Long, Integer> m = WEAR.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        int n = m.merge(p.asLong(), 1, Integer::sum);
        if (n >= uses) { m.remove(p.asLong()); level.destroyBlock(p, false); }
    }

    private static void breakWalls(ServerLevel level, KZombie z) {
        LivingEntity target = z.getTarget();
        if (target == null || !z.horizontalCollision || z.distanceTo(target) > 32) return;
        Vec3 dir = target.position().subtract(z.position()).multiply(1, 0, 1).normalize();
        BlockPos front = BlockPos.containing(z.getX() + dir.x * (z.getBbWidth() / 2 + 0.6), z.getY() + 0.5, z.getZ() + dir.z * (z.getBbWidth() / 2 + 0.6));
        for (BlockPos p : new BlockPos[]{front, front.above()}) {
            BlockState s = level.getBlockState(p);
            if (!BlockDurability.breakable(level, p, s)) continue;
            if (s.is(ModBlocks.ENERGY_WALL) && Power.isPowered(level, p)) {
                z.knockback(1.0, dir.x, dir.z);
                z.hurtServer(level, level.damageSources().lightningBolt(), 2);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.3);
                return;
            }
            float mult = switch (z.kind) {
                case TANK, ALPHA, MUTANT -> 3f;
                case EXPLODER -> s.is(ModBlocks.TITANIUM_BLOCK) ? 1f : 5f;
                default -> 1f;
            };
            float dmg = (float) (z.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * mult * 0.5);
            z.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            z.triggerAnim("attack", "attack");
            level.playSound(null, p, s.getSoundType().getHitSound(), SoundSource.HOSTILE, 1f, 0.7f);
            BlockDurability.hit(level, p, dmg, z);
            return;
        }
    }

    private DefenseRules() {}
}
