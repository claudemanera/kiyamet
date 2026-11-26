package com.kiyamet.vehicle;

import com.kiyamet.entity.KZombie;
import com.kiyamet.npc.Faction;
import com.kiyamet.npc.HumanEntity;
import com.kiyamet.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * A drivable car / ATV / motorcycle / armored truck.
 * W/S throttle and brake, A/D steer, sneak to get out. Runs on fuel (Fuel Canister / coal), repaired with iron ingots
 * or a Repair Kit, runs over walkers, has a trunk (sneak + right-click). Raiders drive them straight at you.
 */
public class VehicleEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation DRIVE = RawAnimation.begin().thenLoop("move.drive");

    public final VehicleKind kind;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final SimpleContainer trunk;
    private float throttle;
    private int turretCooldown;

    public VehicleEntity(EntityType<? extends VehicleEntity> type, Level level, VehicleKind kind) {
        super(type, level);
        this.kind = kind;
        this.trunk = new SimpleContainer(Math.max(1, kind.trunk));
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FUEL, 0);
    }

    public int fuel() { return entityData.get(FUEL); }
    public void setFuel(int f) { entityData.set(FUEL, Mth.clamp(f, 0, kind.fuel)); }
    public SimpleContainer trunk() { return trunk; }

    // ------------------------------------------------------------------ riding

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity e) { return getPassengers().size() < kind.seats.length; }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity e, EntityDimensions dims, float scale) {
        int i = Math.max(0, getPassengers().indexOf(e));
        float[] s = kind.seats[Math.min(i, kind.seats.length - 1)];
        return new Vec3(s[0], s[1], s[2]).yRot(-getYRot() * Mth.DEG_TO_RAD);   // seats turn with the car
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 side = new Vec3(kind.width * 0.5 + 0.7, 0, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
        Vec3 at = position().add(side);
        return level().noCollision(passenger, passenger.getBoundingBox().move(at.subtract(passenger.position()))) ? at : position().add(0, kind.height + 0.2, 0);
    }

    @Override
    protected void tickRidden(Player p, Vec3 in) {
        super.tickRidden(p, in);
        float target = fuel() <= 0 ? 0 : p.zza > 0 ? 1f : p.zza < 0 ? -0.4f : 0f;
        throttle += (target - throttle) * (target == 0 ? 0.07f : 0.045f);
        if (Math.abs(throttle) < 0.01f) throttle = 0;
        float grip = Mth.clamp(Math.abs(throttle) * 2.5f, 0, 1) * Math.signum(throttle);
        float yaw = getYRot() - p.xxa * kind.turn * grip;
        setRot(yaw, 0);
        yBodyRot = yHeadRot = yaw;
    }

    @Override
    protected Vec3 getRiddenInput(Player p, Vec3 in) { return new Vec3(0, 0, throttle); }

    @Override
    protected float getRiddenSpeed(Player p) { return (float) kind.speed; }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        double speed = Math.sqrt(Mth.square(getX() - xo) + Mth.square(getZ() - zo));
        LivingEntity driver = getControllingPassenger();
        if (driver instanceof ServerPlayer sp) {
            if (speed > 0.02 && fuel() > 0 && !sp.isCreative()) setFuel(fuel() - 1);
            if (tickCount % 10 == 0) hud(sp, speed);
            if (speed > 0.22) ram(sl, speed);
            if (tickCount % 40 == 0 && speed > 0.05 && fuel() > 0) sl.playSound(null, blockPosition(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.25f, kind == VehicleKind.MOTORCYCLE ? 1.6f : 0.6f);
        }
        if (kind == VehicleKind.AMBULANCE && tickCount % 100 == 0)
            for (Entity e : getPassengers()) if (e instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 110, 0, true, false));
        if (getFirstPassenger() instanceof HumanEntity h && h.faction() == Faction.RAIDER) raiderDrive(sl);
        else if (getFirstPassenger() instanceof HumanEntity h && h.faction() == Faction.COMPANION) autopilot(sl, h, speed);
        if (getHealth() < getMaxHealth() * 0.3f && tickCount % 10 == 0)
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + kind.height, getZ(), 2, 0.3, 0.1, 0.3, 0.01);
    }

    private void hud(ServerPlayer p, double speed) {
        int pct = (int) Math.round(100.0 * fuel() / kind.fuel);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) bar.append(i < pct / 10 ? '|' : '.');
        p.displayClientMessage(Component.literal(kind.title + "   ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Fuel [" + bar + "] " + pct + "%   ").withStyle(pct < 15 ? ChatFormatting.RED : ChatFormatting.YELLOW))
                .append(Component.literal("HP " + (int) getHealth() + "/" + (int) getMaxHealth() + "   ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal((int) Math.round(speed * 20 * 3.6) + " km/h").withStyle(ChatFormatting.AQUA)), true);
    }

    /** Running over things: walkers fly, the car takes a little damage. */
    private void ram(ServerLevel sl, double speed) {
        Vec3 dir = new Vec3(getX() - xo, 0, getZ() - zo).normalize();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.4).move(dir.scale(0.6)),
                e -> e != this && !hasPassenger(e) && !(e instanceof VehicleEntity) && e.isAlive() && !(e instanceof Player pl && (pl.isCreative() || pl.isSpectator())))) {
            if (e instanceof HumanEntity h && h.faction() == Faction.COMPANION) continue;
            float dmg = (float) (speed * 20 * kind.ram * 1.4);
            if (e.hurtServer(sl, sl.damageSources().mobAttack(getControllingPassenger() != null ? getControllingPassenger() : this), dmg)) {
                e.knockback(1.2 + speed * 3, -dir.x, -dir.z);
                hurtServer(sl, sl.damageSources().generic(), kind.armored ? 0.2f : 1f);
                sl.playSound(null, e.blockPosition(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.NEUTRAL, 0.6f, 1.2f);
                if (e instanceof KZombie) sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 1, e.getZ(), 4, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    /** Raiders drive at the nearest player, jump out 12 blocks away; the armored truck's turret shoots while manned. */
    // ------------------------------------------------------------------ companion driver (1.6)
    public Vec3 autoDest;
    private int autoStuck;

    /** A companion takes the wheel and drives you to the spot (an Xaero's map waypoint, or coordinates). */
    public void driveTo(ServerPlayer p, HumanEntity driver, Vec3 dest) {
        List<Entity> others = new java.util.ArrayList<>(getPassengers());
        ejectPassengers();
        driver.startRiding(this, true, true);
        p.startRiding(this, true, true);
        for (Entity e : others) if (e != driver && e != p && canAddPassenger(e)) e.startRiding(this, true, true);
        autoDest = dest;
        p.sendSystemMessage(Component.literal("<" + driver.getName().getString() + "> ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("Hold on. Driving to " + (int) dest.x + ", " + (int) dest.z + " (" + (int) Math.sqrt(distanceToSqr(dest.x, getY(), dest.z)) + " blocks).").withStyle(ChatFormatting.WHITE)));
    }

    private void autopilot(ServerLevel sl, HumanEntity h, double speed) {
        if (autoDest == null) { getNavigation().stop(); return; }
        double dx = autoDest.x - getX(), dz = autoDest.z - getZ(), d = Math.sqrt(dx * dx + dz * dz);
        ServerPlayer owner = h.ownerPlayer();
        if (d < 6 || fuel() <= 0) {
            autoDest = null; getNavigation().stop();
            if (owner != null) owner.sendSystemMessage(Component.literal("<" + h.getName().getString() + "> ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(fuel() <= 0 ? "We're out of fuel." : "We're here.").withStyle(ChatFormatting.WHITE)));
            return;
        }
        if (speed > 0.02 && fuel() > 0) setFuel(fuel() - 1);
        if (speed > 0.22) ram(sl, speed);
        if (owner != null && tickCount % 10 == 0) hud(owner, speed);
        if (tickCount % 5 == 0) {
            double step = Math.min(d, 24);
            double tx = getX() + dx / d * step, tz = getZ() + dz / d * step;
            if (horizontalCollision && speed < 0.05) {               // blocked: steer around
                if (++autoStuck > 8) { float a = (random.nextBoolean() ? 70 : -70) * Mth.DEG_TO_RAD; double rx = dx / d * Math.cos(a) - dz / d * Math.sin(a), rz = dx / d * Math.sin(a) + dz / d * Math.cos(a); tx = getX() + rx * 10; tz = getZ() + rz * 10; }
            } else autoStuck = 0;
            int ty = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) tx, (int) tz);
            getMoveControl().setWantedPosition(tx, ty, tz, 1.0);
        }
    }

    private void raiderDrive(ServerLevel sl) {
        Player target = sl.getNearestPlayer(this, 96);
        if (target == null || target.isCreative() || target.isSpectator()) { getNavigation().stop(); return; }
        double d = distanceTo(target);
        if (kind == VehicleKind.ARMORED_TRUCK && d < 32 && --turretCooldown <= 0 && hasLineOfSight(target)) {
            turretCooldown = 25;
            target.hurtServer(sl, sl.damageSources().arrow(null, this), 5f);
            Vec3 from = position().add(0, kind.height + 0.4, 0), to = target.getEyePosition();
            for (int i = 0; i < 12; i++) { Vec3 p = from.lerp(to, i / 12.0); sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0); }
            sl.playSound(null, blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.2f, 0.6f);
        }
        if (d < 12 || getHealth() < getMaxHealth() * 0.4f) {
            boolean keepGunner = kind == VehicleKind.ARMORED_TRUCK && getHealth() >= getMaxHealth() * 0.4f;
            List<Entity> riders = List.copyOf(getPassengers());
            for (int i = 0; i < riders.size(); i++) if (!(keepGunner && i == 0)) riders.get(i).stopRiding();
            getNavigation().stop();
        } else if (tickCount % 10 == 0) {
            getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (getFirstPassenger() instanceof HumanEntity h && h.faction() == Faction.RAIDER) return InteractionResult.FAIL;
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        ServerPlayer sp = (ServerPlayer) player;
        if (held.is(ModItems.get("fuel_canister")) || held.is(Items.COAL) || held.is(Items.CHARCOAL) || held.is(Items.LAVA_BUCKET)) {
            if (fuel() >= kind.fuel) { sp.displayClientMessage(Component.literal("The tank is full.").withStyle(ChatFormatting.GRAY), true); return InteractionResult.SUCCESS; }
            int add = held.is(Items.LAVA_BUCKET) ? kind.fuel : held.is(ModItems.get("fuel_canister")) ? kind.fuel / 2 : kind.fuel / 10;
            setFuel(fuel() + add);
            if (!sp.isCreative()) { if (held.is(Items.LAVA_BUCKET)) sp.setItemInHand(hand, new ItemStack(Items.BUCKET)); else held.shrink(1); }
            level().playSound(null, blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.8f, 0.8f);
            sp.displayClientMessage(Component.literal("Fuel: " + (100 * fuel() / kind.fuel) + "%").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }
        if ((held.is(Items.IRON_INGOT) || held.is(ModItems.get("repair_kit")) || held.is(ModItems.get("metal_plate"))) && getHealth() < getMaxHealth()) {
            float frac = held.is(ModItems.get("repair_kit")) ? 0.5f : held.is(ModItems.get("metal_plate")) ? 0.25f : 0.1f;
            heal(getMaxHealth() * frac);
            if (!sp.isCreative()) held.shrink(1);
            level().playSound(null, blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 1.3f);
            sp.displayClientMessage(Component.literal("Repaired: " + (int) getHealth() + "/" + (int) getMaxHealth()).withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (kind.trunk == 0) { sp.displayClientMessage(Component.literal("No trunk on this one.").withStyle(ChatFormatting.GRAY), true); return InteractionResult.SUCCESS; }
            int rows = kind.trunk / 9;
            MenuType<ChestMenu> t = rows >= 3 ? MenuType.GENERIC_9x3 : rows == 2 ? MenuType.GENERIC_9x2 : MenuType.GENERIC_9x1;
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ChestMenu(t, id, inv, trunk, rows), Component.literal(kind.title + " - Trunk")));
            return InteractionResult.SUCCESS;
        }
        if (canAddPassenger(player)) {
            player.startRiding(this, false, true);
            if (fuel() <= 0) sp.displayClientMessage(Component.literal("Out of fuel! Use a Fuel Canister or coal on it.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity src = source.getEntity();
        if (src != null && hasPassenger(src)) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) amount *= 0.3f;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < trunk.getContainerSize(); i++) if (!trunk.getItem(i).isEmpty()) sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 1, getZ(), trunk.removeItemNoUpdate(i)));
            sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 1, getZ(), new ItemStack(ModItems.get("metal_plate"), 2 + random.nextInt(4))));
            if (random.nextBoolean()) sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 1, getZ(), new ItemStack(ModItems.get("circuit"), 1 + random.nextInt(2))));
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.5f, 0.9f);
            ejectPassengers();
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ misc

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith(Entity other) { return true; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override protected boolean shouldDropLoot(ServerLevel level) { return false; }
    @Override public boolean canBeLeashed() { return false; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Fuel", fuel());
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < trunk.getContainerSize(); i++) items.add(trunk.getItem(i));
        out.store("Trunk", ItemStack.OPTIONAL_CODEC.listOf(), items);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setFuel(in.getIntOr("Fuel", 0));
        in.read("Trunk", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> { for (int i = 0; i < l.size() && i < trunk.getContainerSize(); i++) trunk.setItem(i, l.get(i)); });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<VehicleEntity>("main", 3, t -> t.setAndContinue(t.isMoving() ? DRIVE : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
