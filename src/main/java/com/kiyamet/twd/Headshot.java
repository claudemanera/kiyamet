package com.kiyamet.twd;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Walking Dead rule: aim for the head. Hits in the top ~22% of the body deal double damage. */
public final class Headshot {
    public static final float MULT = 2.0f;

    /** Returns the damage multiplier for this hit (2.0 for a headshot) and plays the effect. */
    public static float multiplier(ServerLevel level, LivingEntity target, DamageSource src) {
        Entity direct = src.getDirectEntity(), attacker = src.getEntity();
        double hitY;
        if (direct != null && direct != attacker) hitY = direct.getY() + direct.getBbHeight() / 2;     // arrow / projectile
        else if (attacker instanceof LivingEntity a) {                                                 // melee or hitscan gun
            Vec3 eye = a.getEyePosition(), end = eye.add(a.getLookAngle().scale(64));
            AABB box = target.getBoundingBox().inflate(0.15);
            Optional<Vec3> hit = box.clip(eye, end);
            if (box.contains(eye)) return 1f;
            if (hit.isEmpty()) return 1f;
            hitY = hit.get().y;
        } else return 1f;
        double headStart = target.getY() + target.getBbHeight() * 0.78;
        if (hitY < headStart) return 1f;
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY() + 0.1, target.getZ(), 12, 0.2, 0.15, 0.2, 0.2);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9f, 0.7f);
        return MULT;
    }

    private Headshot() {}
}
