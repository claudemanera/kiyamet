package com.kiyamet.element;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.Shop;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.ui.Ui;
import com.mojang.serialization.Codec;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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

/**
 * A sage (2.1). Each has a name and tells their element's story. On your first visit the sage entrusts you with an
 * Ancient Rod - fight with it while you gather what they ask (items + walkers felled with that rod). Fulfil it and they
 * give you the temple's map. Sages cannot die: they can be hurt, never killed, and never turn. At very high levels
 * (total skill power 3000) a sage you helped will join you - and can carry their element's weapon.
 */
public class Sage extends PathfinderMob implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle"), WALK = RawAnimation.begin().thenLoop("move.walk"), ATTACK = RawAnimation.begin().thenPlay("attack.swing");
    public static final int ROD_KILLS = 20, COMPANION_POWER = 3000;
    /** walkers felled with each sage's rod, per player: "flame" -> 12 */
    public static final AttachmentType<Map<String, Integer>> KILLS = AttachmentRegistry.create(Kiyamet.id("rod_kills"),
            b -> b.initializer(Map::of).persistent(Codec.unboundedMap(Codec.STRING, Codec.INT)).copyOnDeath());
    public final Element element;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<String> helped = new ArrayList<>(), given = new ArrayList<>();
    private final Map<String, Integer> talks = new HashMap<>();
    UUID owner;
    boolean stay;
    int powerCd;

    public Sage(EntityType<? extends PathfinderMob> t, Level l, Element e) { super(t, l); this.element = e; setPersistenceRequired(); }

    public static String[] wants(Element e) {
        return switch (e) {
            case FLAME -> new String[]{"minecraft:coal", "32", "minecraft:magma_block", "8", "minecraft:flint_and_steel", "1"};
            case LIGHTNING -> new String[]{"minecraft:copper_ingot", "32", "minecraft:lightning_rod", "4", "minecraft:glowstone_dust", "16"};
            case EARTH -> new String[]{"minecraft:mossy_cobblestone", "32", "minecraft:emerald", "4", "minecraft:oak_sapling", "8"};
            case SHADOW -> new String[]{"minecraft:obsidian", "8", "minecraft:ink_sac", "16", "minecraft:ender_pearl", "4"};
            case SMOKE -> new String[]{"minecraft:campfire", "4", "minecraft:charcoal", "32", "minecraft:gunpowder", "16"};
            case SPIRIT -> new String[]{"minecraft:rotten_flesh", "64", "minecraft:bone", "32", "minecraft:soul_sand", "16"};
            case ELECTRIC -> new String[]{"minecraft:redstone", "64", "minecraft:copper_ingot", "32", "kiyamet:circuit", "2"};
            case THUNDER -> new String[]{"minecraft:iron_ingot", "32", "minecraft:lightning_rod", "8", "minecraft:gold_ingot", "8"};
            case ICE -> new String[]{"minecraft:packed_ice", "32", "minecraft:snowball", "32", "minecraft:blue_ice", "4"};
            case FIRE -> new String[]{"minecraft:coal_block", "8", "minecraft:fire_charge", "8", "minecraft:netherrack", "32"};
            case WATER -> new String[]{"minecraft:prismarine_shard", "16", "minecraft:cod", "16", "minecraft:kelp", "32"};
            case AIR -> new String[]{"minecraft:feather", "32", "minecraft:phantom_membrane", "4", "minecraft:string", "32"};
            case WIND -> new String[]{"minecraft:feather", "32", "minecraft:white_wool", "16", "minecraft:wind_charge", "8"};
            case METAL -> new String[]{"minecraft:iron_block", "8", "minecraft:chain", "16", "minecraft:anvil", "1"};
        };
    }

    public static int kills(ServerPlayer p, Element e) { return p.getAttachedOrElse(KILLS, Map.of()).getOrDefault(e.id, 0); }
    public static void addKill(ServerPlayer p, Element e) {
        Map<String, Integer> m = new HashMap<>(p.getAttachedOrElse(KILLS, Map.of()));
        m.merge(e.id, 1, Integer::sum);
        p.setAttached(KILLS, Map.copyOf(m));
        int k = m.get(e.id);
        if (k <= ROD_KILLS) p.displayClientMessage(Component.literal("Ancient Rod (" + e.title + "): " + k + " / " + ROD_KILLS + " walkers").withStyle(e.fmt), true);
    }

    /** the rod a sage entrusts to you, marked with their element */
    public static ItemStack rod(Element e) {
        ItemStack s = new ItemStack(ElementItems.ROD);
        CompoundTag t = new CompoundTag(); t.putString("Sage", e.id);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.CUSTOM_NAME, Component.literal("Ancient Rod of " + SageLore.name(e)).withStyle(e.fmt).withStyle(st -> st.withItalic(false)));
        return s;
    }

    public static Element rodElement(ItemStack s) {
        if (!s.is(ElementItems.ROD)) return null;
        return Element.of(s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("Sage", ""));
    }

    ServerPlayer ownerPlayer() { return owner == null || !(level() instanceof ServerLevel sl) ? null : sl.getServer().getPlayerList().getPlayer(owner); }
    public boolean isCompanion() { return owner != null; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) { @Override public boolean canUse() { return isCompanion() && super.canUse(); } });
        goalSelector.addGoal(3, new Goal() {                    // follow the owner
            { setFlags(EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { ServerPlayer o = ownerPlayer(); return o != null && !stay && o.level() == level() && distanceToSqr(o) > 36; }
            @Override public void tick() {
                ServerPlayer o = ownerPlayer(); if (o == null) return;
                getLookControl().setLookAt(o, 30, 30);
                if (distanceToSqr(o) > 24 * 24 && o.onGround()) { snapTo(o.getX(), o.getY(), o.getZ(), getYRot(), 0); getNavigation().stop(); }
                else getNavigation().moveTo(o, 1.25);
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) { @Override public boolean canUse() { return isCompanion() && super.canUse() && !(getLastHurtByMob() instanceof Player); } });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, (e, l) -> isCompanion() && e instanceof Enemy));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND || element == null) return InteractionResult.SUCCESS;
        ServerLevel l = (ServerLevel) level();
        String who = p.getUUID().toString();
        int talk = talks.merge(who, 1, Integer::sum);
        // --- my companion
        if (p.getUUID().equals(owner)) {
            ItemStack held = p.getMainHandItem();
            if (p.isShiftKeyDown()) {
                if (held.getItem() instanceof ElementWeapon && getMainHandItem().isEmpty()) {
                    setItemSlot(EquipmentSlot.MAINHAND, held.split(1)); setDropChance(EquipmentSlot.MAINHAND, 0f);
                    say(p, "Ahh. " + ((ElementWeapon) getMainHandItem().getItem()).element.weaponTitle + ". I know this one. Let them come.");
                } else if (!getMainHandItem().isEmpty()) {
                    ItemStack w = getMainHandItem().copy(); setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    if (!p.getInventory().add(w)) p.drop(w, false);
                    say(p, "Take it back, then. My staff will do.");
                } else say(p, "Give me an element weapon (sneak + right-click) and I will fight with it.");
                return InteractionResult.SUCCESS;
            }
            stay = !stay; getNavigation().stop();
            say(p, stay ? "I'll wait here." : "Lead the way.");
            return InteractionResult.SUCCESS;
        }
        // --- first meeting: name, greeting, the rod
        if (!given.contains(who)) {
            given.add(who);
            say(p, "I am " + SageLore.name(element) + ". " + SageLore.greeting(element));
            say(p, "Take this Ancient Rod. Carry it, fight with it. Fell " + ROD_KILLS + " walkers with it, and bring me what I ask - then I will show you the way to the " + element.title + " Temple.");
            ItemStack r = rod(element);
            if (!p.getInventory().add(r)) p.drop(r, false);
            l.playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 1f, 0.8f);
            needs(p);
            return InteractionResult.SUCCESS;
        }
        String[] lore = SageLore.story(element);
        // --- helped already: stories, and at high levels - an offer
        if (helped.contains(who)) {
            if (owner == null && SkillManager.get(p).totalPower() >= COMPANION_POWER) {
                owner = p.getUUID(); stay = false;
                say(p, SageLore.companion(element));
                p.sendSystemMessage(Component.literal("★ " + SageLore.name(element) + " joined you! Right-click: follow / wait · sneak + right-click: give or take an element weapon.").withStyle(element.fmt, ChatFormatting.BOLD));
                l.sendParticles(new DustParticleOptions(element.color, 2f), getX(), getY() + 1.5, getZ(), 60, 0.6, 1, 0.6, 0.05);
                return InteractionResult.SUCCESS;
            }
            say(p, lore[talk % lore.length]);
            if (owner == null && talk % 3 == 0) say(p, "When you are much stronger (total skill power " + COMPANION_POWER + "), come back. I could walk with you. You have " + SkillManager.get(p).totalPower() + ".");
            if (talk % 4 == 0) give(p);
            return InteractionResult.SUCCESS;
        }
        // --- the quest
        String[] w = wants(element);
        boolean all = kills(p, element) >= ROD_KILLS;
        for (int i = 0; i < w.length; i += 2) if (Shop.count(p, w[i]) < Integer.parseInt(w[i + 1])) all = false;
        if (!all && !p.isCreative()) {
            say(p, lore[(talk - 1) % lore.length]);
            needs(p);
            return InteractionResult.SUCCESS;
        }
        if (!p.isCreative()) for (int i = 0; i < w.length; i += 2) Shop.take(p, w[i], Integer.parseInt(w[i + 1]));
        helped.add(who);
        l.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 0.8f);
        l.sendParticles(new DustParticleOptions(element.color, 1.8f), getX(), getY() + 1.6, getZ(), 50, 0.6, 0.8, 0.6, 0.05);
        say(p, "You kept your word. Here is the true map of the " + element.title + " Temple. Keep the rod - at the bottom of the temple lies the core. Join them on an anvil.");
        give(p);
        return InteractionResult.SUCCESS;
    }

    void needs(ServerPlayer p) {
        String[] w = wants(element);
        StringBuilder b = new StringBuilder("I need: ");
        for (int i = 0; i < w.length; i += 2)
            b.append(w[i + 1]).append("× ").append(new ItemStack(Ui.item(w[i])).getHoverName().getString()).append(" (").append(Math.min(Shop.count(p, w[i]), Integer.parseInt(w[i + 1]))).append("), ");
        b.append("and ").append(ROD_KILLS).append(" walkers felled with my rod (").append(Math.min(kills(p, element), ROD_KILLS)).append(").");
        say(p, b.toString());
    }

    void give(ServerPlayer p) {
        ItemStack map = Temples.templeMap(p.level(), element);
        if (!p.getInventory().add(map)) p.drop(map, false);
    }

    void say(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal("[" + SageLore.name(element) + "] ").withStyle(element.fmt, ChatFormatting.BOLD).append(Component.literal(text).withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || element == null) return;
        if (!isCompanion() || stay) setDeltaMovement(0, getDeltaMovement().y, 0);
        if (tickCount % 100 == 1 && (getCustomName() == null || !getCustomName().getString().equals(SageLore.name(element)))) {
            setCustomName(Component.literal(SageLore.name(element)).withStyle(element.fmt)); setCustomNameVisible(true);
        }
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(isCompanion() ? 2 : 4);
        if (tickCount % 8 == 0) sl.sendParticles(new DustParticleOptions(element.color, 0.9f), getX(), getY() + 2.1, getZ(), 2, 0.25, 0.1, 0.25, 0);
        if (tickCount % 40 == 0) sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.5, getZ(), 12, 0.6, 0.6, 0.6, 0.5);
        // a companion sage uses their element at range
        LivingEntity t = getTarget();
        if (isCompanion() && t != null && t.isAlive() && --powerCd <= 0 && distanceTo(t) < 20 && hasLineOfSight(t)) {
            powerCd = 50;
            Element use = getMainHandItem().getItem() instanceof ElementWeapon w ? w.element : element;
            float dmg = getMainHandItem().getItem() instanceof ElementWeapon ? 10f : 5f;
            switch (use) {
                case FLAME, FIRE -> { Vec3 d = t.position().add(0, t.getBbHeight() / 2, 0).subtract(getEyePosition()).normalize();
                    SmallFireball f = new SmallFireball(sl, this, d); f.setPos(getX() + d.x, getEyeY() + d.y, getZ() + d.z); sl.addFreshEntity(f); }
                case LIGHTNING, ELECTRIC, THUNDER -> { LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl, EntitySpawnReason.TRIGGERED);
                    if (b != null) { b.snapTo(t.getX(), t.getY(), t.getZ(), 0, 0); b.setVisualOnly(true); sl.addFreshEntity(b); } t.hurtServer(sl, damageSources().mobAttack(this), dmg); }
                default -> { t.hurtServer(sl, damageSources().indirectMagic(this, this), dmg);
                    sl.sendParticles(new DustParticleOptions(use.color, 1.6f), t.getX(), t.getY() + 1, t.getZ(), 30, 0.4, 0.6, 0.4, 0.05); }
            }
            ElementPowers.onHit(use, t, this);
            triggerAnim("main", "attack");
            sl.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.NEUTRAL, 0.8f, 1.2f);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity le) {
            ElementPowers.onHit(getMainHandItem().getItem() instanceof ElementWeapon w ? w.element : element, le, this);
            triggerAnim("main", "attack");
        }
        return hit;
    }

    /** sages can be hurt, never killed */
    @Override
    public boolean hurtServer(ServerLevel l, DamageSource src, float amount) {
        if (src.getEntity() instanceof Player p && p.getUUID().equals(owner)) return false;
        float cap = Math.max(0, getHealth() - 1f);
        if (amount >= cap) {
            setHealth(1f);
            l.sendParticles(new DustParticleOptions(element == null ? 0xFFFFFF : element.color, 1.5f), getX(), getY() + 1, getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            return super.hurtServer(l, src, 0.001f);
        }
        return super.hurtServer(l, src, amount);
    }
    @Override public void die(DamageSource src) { setHealth(1f); }
    @Override public boolean isPushable() { return isCompanion(); }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("Helped", String.join(",", helped));
        out.putString("Given", String.join(",", given));
        if (owner != null) out.putString("Owner", owner.toString());
        out.putBoolean("Stay", stay);
    }
    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        helped.clear(); given.clear();
        for (String s : in.getStringOr("Helped", "").split(",")) if (!s.isEmpty()) helped.add(s);
        for (String s : in.getStringOr("Given", "").split(",")) if (!s.isEmpty()) given.add(s);
        given.addAll(helped);
        String o = in.getStringOr("Owner", "");
        owner = o.isEmpty() ? null : UUID.fromString(o);
        stay = in.getBooleanOr("Stay", false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<Sage>("main", 4, t -> t.setAndContinue(t.isMoving() ? WALK : IDLE)).triggerableAnim("attack", ATTACK));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
