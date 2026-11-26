package com.kiyamet.vehicle;

import com.kiyamet.Kiyamet;
import com.kiyamet.entity.KZombie;
import com.kiyamet.npc.HumanEntity;
import com.kiyamet.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModVehicles {
    public static final Map<VehicleKind, EntityType<VehicleEntity>> TYPES = new EnumMap<>(VehicleKind.class);

    static {
        for (VehicleKind k : VehicleKind.values()) {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id(k.id));
            EntityType<VehicleEntity> type = EntityType.Builder.<VehicleEntity>of((t, l) -> new VehicleEntity(t, l, k), MobCategory.MISC)
                    .sized(k.width, k.height).eyeHeight(k.height * 0.8f).clientTrackingRange(10).fireImmune().build(key);
            TYPES.put(k, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type));
        }
        for (VehicleKind k : VehicleKind.values())
            ModItems.custom(k.kitId(), p -> new KeyItem(p.stacksTo(1).rarity(k.armored ? net.minecraft.world.item.Rarity.EPIC : net.minecraft.world.item.Rarity.RARE), k));
    }

    public static void init() {
        TYPES.forEach((k, type) -> FabricDefaultAttributeRegistry.register(type, Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, k.health)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.ARMOR_TOUGHNESS, k.armored ? 8 : 0)
                .add(Attributes.STEP_HEIGHT, k == VehicleKind.MOTORCYCLE ? 1.0 : 1.1)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 96)
                .add(Attributes.SAFE_FALL_DISTANCE, 6)));

        // inside a car the dead can't reach you as easily; inside armored vehicles not at all
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amount) -> {
            if (!(e.getVehicle() instanceof VehicleEntity v) || !(src.getEntity() instanceof KZombie)) return true;
            if (v.kind.armored) return false;
            if (v.kind == VehicleKind.ATV || v.kind == VehicleKind.MOTORCYCLE) return true;
            return e.getRandom().nextFloat() < 0.4f;
        });
    }

    public static VehicleEntity spawn(ServerLevel level, VehicleKind k, double x, double y, double z, float yaw, float fuelFrac, float hpFrac) {
        VehicleEntity v = TYPES.get(k).create(level, EntitySpawnReason.EVENT);
        if (v == null) return null;
        v.setPos(x, y, z);
        v.setYRot(yaw); v.yBodyRot = yaw; v.yHeadRot = yaw;
        v.setFuel((int) (k.fuel * fuelFrac));
        v.setHealth((float) (k.health * hpFrac));
        level.addFreshEntity(v);
        return v;
    }

    private static final VehicleKind[] WRECKS = {VehicleKind.SEDAN, VehicleKind.SEDAN, VehicleKind.PICKUP, VehicleKind.POLICE, VehicleKind.AMBULANCE, VehicleKind.ATV, VehicleKind.MOTORCYCLE, VehicleKind.ARMORED_SUV};

    /** An abandoned vehicle by the road: little fuel, banged up, sometimes something in the trunk. */
    public static void abandoned(ServerLevel level, BlockPos at) {
        VehicleKind k = WRECKS[level.random.nextInt(WRECKS.length)];
        VehicleEntity v = spawn(level, k, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0.05f + level.random.nextFloat() * 0.3f, 0.4f + level.random.nextFloat() * 0.4f);
        if (v == null || k.trunk == 0) return;
        String[][] loot = {{"minecraft:bread", "4"}, {"kiyamet:bandage", "2"}, {"minecraft:arrow", "16"}, {"kiyamet:fuel_canister", "1"}, {"kiyamet:circuit", "1"}, {"kiyamet:medkit", "1"}, {"kiyamet:ruby", "2"}, {"minecraft:iron_ingot", "3"}};
        if (level.random.nextFloat() < 0.2f) v.trunk().setItem(0, com.kiyamet.enchant.EnchantLoot.weapon(level, 0));
        if (level.random.nextFloat() < 0.15f) v.trunk().setItem(1, com.kiyamet.enchant.GunTomeItem.random(level.random));
        int n = level.random.nextInt(4);
        for (int i = 0; i < n; i++) {
            String[] l = loot[level.random.nextInt(loot.length)];
            v.trunk().setItem(level.random.nextInt(v.trunk().getContainerSize()), new ItemStack(com.kiyamet.ui.Ui.item(l[0]), Integer.parseInt(l[1])));
        }
    }

    /** Puts a raider group into a vehicle (rest walk). */
    public static VehicleEntity raiderRide(ServerLevel level, BlockPos at, List<HumanEntity> raiders, boolean commander, int power) {
        VehicleKind k = commander ? VehicleKind.ARMORED_TRUCK : power >= 2500 && level.random.nextBoolean() ? VehicleKind.ARMORED_SUV
                : new VehicleKind[]{VehicleKind.PICKUP, VehicleKind.PICKUP, VehicleKind.SEDAN, VehicleKind.MOTORCYCLE, VehicleKind.ATV}[level.random.nextInt(5)];
        VehicleEntity v = spawn(level, k, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0.3f + level.random.nextFloat() * 0.4f, 1f);
        if (v == null) return null;
        int seat = 0;
        for (HumanEntity h : raiders) {
            if (seat >= k.seats.length) break;
            if (h.startRiding(v, true, true)) seat++;
        }
        return v;
    }

    /** Car keys: use on the ground to bring out the vehicle (half a tank). */
    public static class KeyItem extends Item {
        final VehicleKind kind;
        public KeyItem(Properties p, VehicleKind kind) { super(p); this.kind = kind; }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
            BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
            if (!level.noCollision(TYPES.get(kind).getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5))) {
                p.displayClientMessage(Component.literal("Not enough room here.").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            spawn(level, kind, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), 0.5f, 1f);
            p.sendSystemMessage(Component.literal(kind.title + " ready. Right-click: get in · W/S: gas/brake · A/D: steer · sneak: get out · sneak + right-click: trunk · Fuel Canister / coal: refuel · iron / metal plate / repair kit: repair").withStyle(ChatFormatting.GOLD));
            if (!p.isCreative()) ctx.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS;
        }
    }

    private ModVehicles() {}
}
