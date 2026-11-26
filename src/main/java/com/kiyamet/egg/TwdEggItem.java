package com.kiyamet.egg;

import com.kiyamet.npc.Faction;
import com.kiyamet.npc.HumanEntity;
import com.kiyamet.npc.ModHumans;
import com.kiyamet.npc.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** TWD spawn eggs (and car kits): use on a block to spawn the thing. */
public class TwdEggItem extends Item {
    final String what;   // "entity:<id>", "human:<FACTION>:<ROLE>", "vehicle:<KIND>"

    public TwdEggItem(Properties p, String what) { super(p); this.what = what; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        String[] w = what.split(":");
        Entity made = null;
        switch (w[0]) {
            case "entity" -> {
                EntityType<?> t = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(com.kiyamet.Kiyamet.id(w[1]));
                Entity e = t.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
                if (e == null) return InteractionResult.FAIL;
                e.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5); e.setYRot(level.random.nextFloat() * 360);
                if (e instanceof Mob m) m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.SPAWN_ITEM_USE, null);
                level.addFreshEntity(e);
                made = e;
            }
            case "human" -> {
                Faction f = Faction.valueOf(w[1]);
                int lv = 20 + level.random.nextInt(60);
                HumanEntity h = ModHumans.spawn(level, pos, f == Faction.COMPANION ? Faction.SURVIVOR : f, Role.valueOf(w[2]), lv, f == Faction.COMMUNITY ? "Alexandria" : "");
                if (h != null && f == Faction.COMPANION && ctx.getPlayer() instanceof ServerPlayer sp) h.setOwner(sp);
                made = h;
            }
            case "vehicle" -> {
                float yaw = ctx.getPlayer() == null ? 0 : ctx.getPlayer().getYRot();
                made = com.kiyamet.vehicle.ModVehicles.spawn(level, com.kiyamet.vehicle.VehicleKind.valueOf(w[1]), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 1f, 1f);
            }
        }
        if (made == null) return InteractionResult.FAIL;
        level.playSound(null, pos, w[0].equals("vehicle") ? SoundEvents.ANVIL_PLACE : SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.7f, 1f);
        if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
