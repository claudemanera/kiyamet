package com.kiyamet.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Hitscan helpers for guns and gadgets. */
public final class Raycast {
    public static BlockHitResult block(Player p, double range) {
        Vec3 from = p.getEyePosition(), to = from.add(p.getViewVector(1f).scale(range));
        return p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
    }

    /** Living entities hit by the ray before it reaches a wall, nearest first. */
    public static List<LivingEntity> entities(Player p, double range, double grow) {
        Level level = p.level();
        Vec3 from = p.getEyePosition();
        BlockHitResult wall = block(p, range);
        Vec3 to = wall.getType() == HitResult.Type.MISS ? from.add(p.getViewVector(1f).scale(range)) : wall.getLocation();
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : level.getEntities(p, new AABB(from, to).inflate(grow + 1))) {
            if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
            Optional<Vec3> hit = e.getBoundingBox().inflate(grow).clip(from, to);
            if (hit.isPresent()) out.add(le);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return out;
    }

    /** Point where the shot lands: first entity, else wall, else max range. */
    public static Vec3 impact(Player p, double range) {
        List<LivingEntity> es = entities(p, range, 0.3);
        if (!es.isEmpty()) return es.get(0).position().add(0, es.get(0).getBbHeight() / 2, 0);
        BlockHitResult b = block(p, range);
        return b.getType() == HitResult.Type.MISS ? p.getEyePosition().add(p.getViewVector(1f).scale(range)) : b.getLocation();
    }

    private Raycast() {}
}
