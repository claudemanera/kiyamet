package com.kiyamet.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;

/**
 * XP sources tuned for ~3,140 XP per hour per skill while actively doing that thing.
 * Engineering, alchemy, enchanting, medicine, building and exploration get their sources in later phases
 * (lab table crafting, brewing, healing items, block placing, structure discovery).
 */
public final class SkillEvents {
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    private static int tick;

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (!(player instanceof ServerPlayer p)) return;
            if (level instanceof net.minecraft.server.level.ServerLevel sl) com.kiyamet.world.NoiseSystem.make(sl, pos, 10, 4);
            if (state.is(BlockTags.LOGS)) SkillManager.addXp(p, Skill.WOODCUTTING, 7);
            else if (state.getBlock() instanceof CropBlock crop) { if (crop.isMaxAge(state)) SkillManager.addXp(p, Skill.FARMING, 9); }
            else if (state.is(ConventionalBlockTags.ORES)) SkillManager.addXp(p, Skill.MINING, 14);
            else if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) SkillManager.addXp(p, Skill.MINING, 2);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer p) || entity == p) return;
            double base = Math.max(4, entity.getMaxHealth() * 1.5);
            Entity direct = source.getDirectEntity();
            if ((direct != null && direct.getType().is(EntityTypeTags.ARROWS)) || p.getMainHandItem().getItem() instanceof com.kiyamet.item.GunItem) SkillManager.addXp(p, Skill.ARCHERY, base);
            else if (p.getMainHandItem().is(ItemTags.SWORDS)) SkillManager.addXp(p, Skill.SWORDS, base);
            else if (p.getMainHandItem().is(ItemTags.AXES)) SkillManager.addXp(p, Skill.HEAVY_WEAPONS, base);
            SkillManager.addXp(p, Skill.HUNTING, base * 0.6);
            if (p.isShiftKeyDown()) SkillManager.addXp(p, Skill.STEALTH, base * 0.5);
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseTaken, taken, blocked) -> {
            if (!(entity instanceof ServerPlayer p)) return;
            if (source.is(DamageTypeTags.IS_FALL)) SkillManager.addXp(p, Skill.ACROBATICS, taken * 6);
            else if (blocked) SkillManager.addXp(p, Skill.DEFENSE, baseTaken * 4);
            else {
                if (taken > 0) SkillManager.addXp(p, Skill.HEALTH, taken * 4);
                if (baseTaken > taken) SkillManager.addXp(p, Skill.DEFENSE, (baseTaken - taken) * 4);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> SkillManager.applyHealth(newP));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SkillManager.applyHealth(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tick % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isSpectator() || p.isCreative() || !p.isAlive()) continue;
                SkillManager.tickRest(p);
                com.kiyamet.world.Infection.tick(p);
                Vec3 now = p.position(), last = LAST_POS.put(p.getUUID(), now);
                boolean moved = last != null && last.distanceToSqr(now) > 1.0;
                if (p.isSprinting() && moved) SkillManager.addXp(p, Skill.ATHLETICS, 1.2);
                if ((p.isSwimming() || p.isUnderWater()) && moved) SkillManager.addXp(p, Skill.SWIMMING, 1.5);
                if (p.isShiftKeyDown() && moved && p.onGround()) SkillManager.addXp(p, Skill.STEALTH, 1.0);
                if (tick % 1200 == 0) SkillManager.addXp(p, Skill.SURVIVAL, 25);
            }
        });
    }

    private SkillEvents() {}
}
