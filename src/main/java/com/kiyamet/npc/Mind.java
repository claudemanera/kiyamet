package com.kiyamet.npc;

import com.kiyamet.Kiyamet;
import com.kiyamet.entity.KZombie;
import com.kiyamet.season.Seasons;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * TWD 1.5 mental health and morale.
 * Your mind (0-100) wears down near walkers, alone in the dark, under a Blood Moon, when friends die or come back as
 * walkers, when you kill people, freeze or starve. It recovers with sleep, food, music, a fire, your shelter and
 * your people around you. Low: whispers, footsteps, things that aren't there - and at the bottom, walkers that vanish
 * when you swing at them.
 * Companions have morale (0-100): long fights, wounds, hunger, the night and losing friends drag it down; rest,
 * a warm fire, food from your hand and a safe base lift it. High morale = harder hits; low = weaker, grumbling,
 * and at the very bottom they may walk away.
 */
public final class Mind {
    public static final AttachmentType<Float> SANITY = AttachmentRegistry.create(Kiyamet.id("sanity"), b -> b.initializer(() -> 80f).persistent(Codec.FLOAT).copyOnDeath());
    private static final Map<UUID, Integer> STATE = new HashMap<>();
    private static final Map<UUID, Integer> FOOD = new HashMap<>();
    private static final Set<UUID> SEEN_FRIENDS = new HashSet<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) if (!p.isSpectator() && !p.isCreative()) tick(p);
        });
        EntitySleepEvents.STOP_SLEEPING.register((e, pos) -> { if (e instanceof ServerPlayer p) change(p, 30, "A night's sleep clears your head."); });
        ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> {
            if (src.getEntity() instanceof ServerPlayer p && e instanceof HumanEntity h && h.faction() != Faction.RAIDER && h.faction() != Faction.WHISPERER)
                change(p, -6, "You killed someone who wasn't a raider. It weighs on you.");
            else if (src.getEntity() instanceof ServerPlayer p && e instanceof HumanEntity) change(p, -1.5f, null);
            if (e instanceof HumanEntity h && h.faction() == Faction.COMPANION && h.ownerPlayer() != null) {
                change(h.ownerPlayer(), -15, "Losing " + h.getName().getString() + " hits you hard.");
                for (HumanEntity o : ((ServerLevel) e.level()).getEntitiesOfClass(HumanEntity.class, e.getBoundingBox().inflate(48), o -> o != h && o.isOwnedBy(h.ownerPlayer())))
                    o.morale = Math.max(0, o.morale - 20);
            }
        });
        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(Commands.literal("twd")
                .then(Commands.literal("mind").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    float s = p.getAttachedOrElse(SANITY, 80f);
                    p.sendSystemMessage(Component.literal("🧠 Mind: " + Math.round(s) + "/100 - " + label(s)).withStyle(color(s), ChatFormatting.BOLD));
                    return 1;
                }))));
    }

    public static float sanity(ServerPlayer p) { return p.getAttachedOrElse(SANITY, 80f); }

    public static void change(ServerPlayer p, float d, String why) {
        if (d < 0) return;                                    // 1.9: your own mind stays steady
        float s = Mth.clamp(sanity(p) + d, 0, 100);
        p.setAttached(SANITY, s);
        if (why != null) p.sendSystemMessage(Component.literal("🧠 " + why + " (" + (d > 0 ? "+" : "") + Math.round(d) + ")").withStyle(d > 0 ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_PURPLE));
    }

    static String label(float s) { return s >= 70 ? "Steady" : s >= 50 ? "Uneasy" : s >= 30 ? "Shaken" : s >= 12 ? "Breaking" : "Lost"; }
    static ChatFormatting color(float s) { return s >= 70 ? ChatFormatting.GREEN : s >= 50 ? ChatFormatting.YELLOW : s >= 30 ? ChatFormatting.GOLD : s >= 12 ? ChatFormatting.RED : ChatFormatting.DARK_RED; }
    static int bucket(float s) { return s >= 70 ? 4 : s >= 50 ? 3 : s >= 30 ? 2 : s >= 12 ? 1 : 0; }

    private static void tick(ServerPlayer p) {
        if (sanity(p) < 100) p.setAttached(SANITY, 100f);    // 1.9: the player's mind no longer breaks - only companions' minds do
        if (true) return;
        ServerLevel level = p.level();
        RandomSource r = level.random;
        BlockPos pos = p.blockPosition();
        float d = 0;
        List<KZombie> walkers = level.getEntitiesOfClass(KZombie.class, p.getBoundingBox().inflate(12), z -> !z.captives.captive);
        d -= Math.min(1.2f, walkers.size() * 0.08f);
        boolean night = !level.isBrightOutside(), sky = level.canSeeSky(pos.above());
        long friends = level.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(10), h -> h.isOwnedBy(p)).size();
        if (night && sky) d -= 0.06f;
        if (night && friends == 0) d -= 0.05f;
        if (BaseRaids.bloodMoon()) d -= 0.15f;
        int st = Seasons.state(p.getAttachedOrElse(Seasons.BODY, 20f));
        if (st <= -2 || st >= 3) d -= 0.06f;
        if (p.getFoodData().getFoodLevel() < 6) d -= 0.06f;
        d += Math.min(3, friends) * 0.03f;
        boolean safe = Seasons.warmNear(level, pos, 5) || com.kiyamet.twd.ShelterCore.protects(level, pos) || com.kiyamet.world.ModStructures.inSanctuary(level, pos);
        if (safe && walkers.isEmpty()) d += 0.15f;
        if (music(level, pos)) d += 0.3f;
        int food = p.getFoodData().getFoodLevel();
        Integer lastFood = FOOD.put(p.getUUID(), food);
        if (lastFood != null && food > lastFood) d += (food - lastFood) * 0.6f;
        // seeing a friend who turned
        for (KZombie z : walkers) if (z.formerName != null && z.distanceTo(p) < 10 && SEEN_FRIENDS.add(z.getUUID()))
            change(p, -6, "That walker... it was " + z.formerName + ".");
        float s = Mth.clamp(sanity(p) + d, 0, 100);
        p.setAttached(SANITY, s);
        int b = bucket(s);
        Integer was = STATE.put(p.getUUID(), b);
        if (was != null && was != b) p.sendSystemMessage(Component.literal("🧠 Your mind: " + label(s) + (b < was ? " - rest, eat, find your people and a fire." : ".")).withStyle(color(s)));
        symptoms(p, level, s, r);
    }

    static boolean music(ServerLevel level, BlockPos c) {
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-8, -3, -8), c.offset(8, 3, 8))) {
            BlockState b = level.getBlockState(q);
            if (b.is(Blocks.JUKEBOX) && b.getValue(JukeboxBlock.HAS_RECORD)) return true;
        }
        return false;
    }

    private static final String[] WHISPERS = {"Did something move behind you?", "You hear footsteps... then nothing.", "Someone whispers your name.",
            "For a second the trees looked like people.", "You hear a radio crackle. There's no radio.", "Something is breathing right behind you.",
            "You smell rot. Close. Very close.", "A child laughs somewhere in the dark."};

    private static void symptoms(ServerPlayer p, ServerLevel level, float s, RandomSource r) {
        if (s < 50 && r.nextInt(40) == 0) whisper(p, r.nextBoolean() ? SoundEvents.AMBIENT_CAVE : BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ZOMBIE_AMBIENT), 0.6f);
        if (s < 30) {
            if (r.nextInt(30) == 0) p.sendSystemMessage(Component.literal(WHISPERS[r.nextInt(WHISPERS.length)]).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            if (r.nextInt(25) == 0) p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0, true, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, false));
        }
        if (s < 12) {
            if (r.nextInt(15) == 0) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false, false));
            if (r.nextInt(45) == 0) hallucination(p, level);
        }
    }

    static void whisper(ServerPlayer p, Holder<SoundEvent> sound, float vol) {
        Vec3 behind = p.position().subtract(p.getLookAngle().scale(3));
        p.connection.send(new ClientboundSoundPacket(sound, SoundSource.AMBIENT, behind.x, behind.y + 1, behind.z, vol, 0.7f + p.getRandom().nextFloat() * 0.3f, p.getRandom().nextLong()));
    }

    /** A walker that isn't really there: it charges, does no harm, and vanishes when hit or after a few seconds. */
    private static void hallucination(ServerPlayer p, ServerLevel level) {
        Vec3 at = p.position().subtract(p.getLookAngle().multiply(1, 0, 1).normalize().scale(-8));
        KZombie z = com.kiyamet.entity.ModEntities.ZOMBIES.get(com.kiyamet.entity.ZombieKind.FARMER).create(level, EntitySpawnReason.EVENT);
        if (z == null) return;
        z.setPos(at.x, level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at)).getY(), at.z);
        z.hallucination = level.getGameTime() + 120;
        z.setTarget(p);
        level.addFreshEntity(z);
    }

    // ------------------------------------------------------------------ companion morale

    private static final net.minecraft.resources.Identifier MORALE_DMG = Kiyamet.id("morale_damage"), MORALE_SPD = Kiyamet.id("morale_speed");

    /** Called every second for owned companions. */
    public static void companion(ServerLevel level, HumanEntity h, ServerPlayer owner) {
        float m = h.morale;
        boolean fighting = h.getTarget() != null;
        if (fighting) m -= 0.12f;
        if (h.getHealth() < h.getMaxHealth() * 0.5f) m -= 0.1f;
        if (!level.isBrightOutside()) m -= 0.03f;
        if (BaseRaids.bloodMoon()) m -= 0.08f;
        if (!fighting && (Seasons.warmNear(level, h.blockPosition(), 5) || com.kiyamet.twd.ShelterCore.protects(level, h.blockPosition()))) m += 0.25f;
        else if (!fighting && h.order() == HumanEntity.Order.STAY) m += 0.06f;
        else if (!fighting && level.isBrightOutside()) m += 0.02f;
        if (music(level, h.blockPosition())) m += 0.2f;
        h.morale = Mth.clamp(m, 0, 100);
        // effects
        double dmg = h.morale >= 70 ? 0.15 : h.morale < 30 ? -0.15 : 0, spd = h.morale >= 70 ? 0.05 : h.morale < 30 ? -0.05 : 0;
        apply(h.getAttribute(Attributes.ATTACK_DAMAGE), MORALE_DMG, dmg);
        apply(h.getAttribute(Attributes.MOVEMENT_SPEED), MORALE_SPD, spd);
        RandomSource r = h.getRandom();
        if (h.morale < 25 && r.nextInt(240) == 0 && !Talk.busy(owner))
            Talk.say(owner, h, Talk.pick(r, "I can't keep doing this...", "How long until it ends? Ever?", "I'm so tired. Can we stop somewhere safe?", "I keep seeing their faces."));
        if (h.morale < 8 && r.nextInt(4000) == 0 && !Talk.busy(owner)) {      // very rare: the broken mind turns on you
            Talk.say(owner, h, Talk.pick(r, "You did this to us. All of it.", "I'm taking what I'm owed.", "They were right about you."));
            owner.sendSystemMessage(Component.literal("⚠ " + h.getName().getString() + "'s mind broke - they've turned against you!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            h.betray(owner);
            return;
        }
        if (h.morale < 10 && r.nextInt(600) == 0) {
            Talk.say(owner, h, "I'm sorry. I can't. I'm leaving.");
            h.setOwner(null);
            owner.sendSystemMessage(Component.literal(h.getName().getString() + " has left your group. (Morale broke)").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
    }

    private static void apply(AttributeInstance a, net.minecraft.resources.Identifier id, double v) {
        if (a == null) return;
        AttributeModifier cur = a.getModifier(id);
        if (v == 0) { if (cur != null) a.removeModifier(id); return; }
        if (cur == null || cur.amount() != v) a.addOrUpdateTransientModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    public static String moraleLabel(float m) { return m >= 70 ? "High" : m >= 45 ? "Okay" : m >= 25 ? "Low" : "Breaking"; }

    private Mind() {}
}
