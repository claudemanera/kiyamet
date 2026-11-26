package com.kiyamet.element;

import com.kiyamet.npc.Shop;
import com.kiyamet.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The sage of an element. Bring what they ask for and they hand you the true map of their element's temple.
 */
public class Sage extends PathfinderMob implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    public final Element element;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<String> helped = new ArrayList<>();

    public Sage(EntityType<? extends PathfinderMob> t, Level l, Element e) { super(t, l); this.element = e; setPersistenceRequired(); setInvulnerable(true); }

    /** item id, amount (pairs) */
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

    static final String[] LORE = {
            "Long before the dead walked, fourteen temples held the elements of the world.",
            "Each temple sleeps far from here, guarded by its lord and his soldiers.",
            "At the very bottom lies the element itself. Join it with an ancient rod on an anvil, and a weapon is born."};

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND || element == null) return InteractionResult.SUCCESS;
        String who = p.getUUID().toString();
        if (helped.contains(who)) {
            give(p);
            say(p, "You have the map. The " + element.title + " Temple waits. Go, and do not die there.");
            return InteractionResult.SUCCESS;
        }
        String[] w = wants(element);
        boolean all = true;
        for (int i = 0; i < w.length; i += 2) if (Shop.count(p, w[i]) < Integer.parseInt(w[i + 1])) all = false;
        if (!all && !p.isCreative()) {
            say(p, LORE[p.getRandom().nextInt(LORE.length)]);
            StringBuilder b = new StringBuilder("Bring me ");
            for (int i = 0; i < w.length; i += 2) {
                int have = Shop.count(p, w[i]);
                b.append(w[i + 1]).append("× ").append(new ItemStack(Ui.item(w[i])).getHoverName().getString()).append(" (").append(Math.min(have, Integer.parseInt(w[i + 1]))).append(")");
                b.append(i + 2 < w.length ? ", " : "");
            }
            say(p, b + " - and the way to the " + element.title + " Temple is yours.");
            return InteractionResult.SUCCESS;
        }
        if (!p.isCreative()) for (int i = 0; i < w.length; i += 2) Shop.take(p, w[i], Integer.parseInt(w[i + 1]));
        helped.add(who);
        ServerLevel l = (ServerLevel) level();
        l.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 0.8f);
        l.sendParticles(new DustParticleOptions(element.color, 1.8f), getX(), getY() + 1.6, getZ(), 50, 0.6, 0.8, 0.6, 0.05);
        say(p, "You kept your word. Here is the true map of the " + element.title + " Temple.");
        give(p);
        return InteractionResult.SUCCESS;
    }

    void give(ServerPlayer p) {
        ItemStack map = Temples.templeMap(p.level(), element);
        if (!p.getInventory().add(map)) p.drop(map, false);
    }

    void say(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal("[" + element.title + " Sage] ").withStyle(element.fmt, ChatFormatting.BOLD).append(Component.literal(text).withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (level() instanceof ServerLevel sl && element != null && tickCount % 8 == 0) {
            sl.sendParticles(new DustParticleOptions(element.color, 0.9f), getX(), getY() + 2.1, getZ(), 2, 0.25, 0.1, 0.25, 0);
            if (tickCount % 40 == 0) sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.5, getZ(), 12, 0.6, 0.6, 0.6, 0.5);
        }
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean hurtServer(ServerLevel l, DamageSource src, float amount) { return false; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putString("Helped", String.join(",", helped)); }
    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        helped.clear();
        for (String s : in.getStringOr("Helped", "").split(",")) if (!s.isEmpty()) helped.add(s);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(new AnimationController<Sage>("main", 4, t -> t.setAndContinue(IDLE))); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
