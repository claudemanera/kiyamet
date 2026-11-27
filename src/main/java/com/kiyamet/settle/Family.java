package com.kiyamet.settle;

import com.kiyamet.npc.Faction;
import com.kiyamet.npc.HumanEntity;
import com.kiyamet.npc.Role;
import com.kiyamet.season.Calendar;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * 1.9 family. A companion with high morale can become your partner. Sleep near your partner and one night she may
 * get pregnant; nine months later (calendar) your child is born. Children grow up with the calendar:
 * baby (first month) → toddler → child → teenager → adult (one year). Babies and children don't fight;
 * teenagers start to; adults are full companions.
 */
public final class Family {
    public static final int PREGNANCY_DAYS = 270;
    public enum Stage { BABY(0, 0.35f, "Baby"), TODDLER(30, 0.45f, "Toddler"), CHILD(120, 0.6f, "Child"), TEEN(240, 0.82f, "Teenager"), ADULT(365, 1f, "Adult");
        public final int from; public final float scale; public final String title;
        Stage(int from, float scale, String title) { this.from = from; this.scale = scale; this.title = title; }
        public static Stage of(long ageDays) { Stage s = BABY; for (Stage t : values()) if (ageDays >= t.from) s = t; return s; }
    }

    public static Stage stage(ServerLevel l, HumanEntity h) { return h.bornDay < 0 ? Stage.ADULT : Stage.of(Calendar.dayIndex(l) - h.bornDay); }

    public static boolean canPartner(HumanEntity h) { return h.appearance().female() && h.bornDay < 0 && !h.partner; }

    public static void propose(ServerPlayer p, HumanEntity h) {
        ServerLevel l = (ServerLevel) p.level();
        if (h.partner) {
            long due = h.pregnantDay >= 0 ? PREGNANCY_DAYS - (Calendar.dayIndex(l) - h.pregnantDay) : -1;
            p.displayClientMessage(Component.literal(h.pregnantDay >= 0 ? h.getName().getString() + " is expecting - " + Math.max(0, due) + " days to go."
                    : "You're together. Sleep in a bed near " + h.getName().getString() + " at night.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        if (!canPartner(h)) { p.displayClientMessage(Component.literal("That's not possible.").withStyle(ChatFormatting.GRAY), true); return; }
        boolean taken = !l.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(256), o -> o.partner && o.isOwnedBy(p)).isEmpty();
        if (taken) { p.displayClientMessage(Component.literal("You already have a partner.").withStyle(ChatFormatting.RED), true); return; }
        if (h.morale < 60) { p.sendSystemMessage(Component.literal(h.getName().getString() + ": \"Not now... I need to feel safe first.\" (morale 60+)").withStyle(ChatFormatting.GRAY)); return; }
        h.partner = true;
        l.sendParticles(ParticleTypes.HEART, h.getX(), h.getY() + 2, h.getZ(), 8, 0.4, 0.3, 0.4, 0);
        l.playSound(null, h.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 1f, 1.2f);
        p.sendSystemMessage(Component.literal("♥ " + h.getName().getString() + " is your partner now.").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }

    public static void init() {
        EntitySleepEvents.STOP_SLEEPING.register((e, pos) -> {
            if (!(e instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel l) || l.isDarkOutside()) return;
            for (HumanEntity h : l.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(12), o -> o.partner && o.isOwnedBy(p) && o.pregnantDay < 0))
                if (l.random.nextFloat() < 0.35f) {
                    h.pregnantDay = Calendar.dayIndex(l);
                    p.sendSystemMessage(Component.literal("♥ " + h.getName().getString() + " has news: you're going to be a parent! (due in 9 months)").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 == 0)
                for (ServerPlayer p : server.getPlayerList().getPlayers())
                    if (p.getAttachedOrElse(CARRIED, 0) > 0 && p.level().getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(4), o -> o.getTags().contains(CARRY_TAG) && o.isOwnedBy(p)).isEmpty())
                        p.setAttached(CARRIED, 0);
            if (server.getTickCount() % 200 != 0) return;
            for (ServerLevel l : server.getAllLevels())
                for (HumanEntity h : l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(HumanEntity.class), o -> o.pregnantDay >= 0))
                    if (Calendar.dayIndex(l) - h.pregnantDay >= PREGNANCY_DAYS) birth(l, h);
        });
        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(Commands.literal("twd").then(Commands.literal("family")
                .executes(c -> { status(c.getSource().getPlayerOrException()); return 1; })
                .then(Commands.literal("birth").executes(c -> {          // creative testing: skip the nine months
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!p.isCreative()) return 0;
                    for (HumanEntity h : p.level().getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(32), o -> o.partner && o.isOwnedBy(p))) { h.pregnantDay = Calendar.dayIndex(p.level()) - PREGNANCY_DAYS; }
                    return 1;
                })))));
    }

    static void status(ServerPlayer p) {
        ServerLevel l = (ServerLevel) p.level();
        var fam = l.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(512), o -> o.isOwnedBy(p) && (o.partner || o.bornDay >= 0));
        if (fam.isEmpty()) { p.sendSystemMessage(Component.literal("No family yet. A companion with high morale might become your partner (companion panel).").withStyle(ChatFormatting.GRAY)); return; }
        for (HumanEntity h : fam) {
            if (h.partner) p.sendSystemMessage(Component.literal("♥ Partner: " + h.getName().getString() + (h.pregnantDay >= 0 ? " - expecting, " + Math.max(0, PREGNANCY_DAYS - (Calendar.dayIndex(l) - h.pregnantDay)) + " days to go" : "")).withStyle(ChatFormatting.LIGHT_PURPLE));
            else p.sendSystemMessage(Component.literal("★ " + h.getName().getString() + " - " + stage(l, h).title + ", " + (Calendar.dayIndex(l) - h.bornDay) + " days old").withStyle(ChatFormatting.AQUA));
        }
    }

    static void birth(ServerLevel l, HumanEntity mother) {
        mother.pregnantDay = -1;
        ServerPlayer owner = mother.ownerPlayer();
        HumanEntity baby = com.kiyamet.npc.ModHumans.HUMAN.create(l, EntitySpawnReason.BREEDING);
        if (baby == null) return;
        baby.setup(Faction.COMPANION, Role.MEMBER, 1, null);
        baby.snapTo(mother.getX(), mother.getY(), mother.getZ(), mother.getYRot(), 0);
        baby.bornDay = Calendar.dayIndex(l);
        baby.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        l.addFreshEntity(baby);
        if (owner != null) {
            com.kiyamet.skill.SkillManager.addXp(owner, com.kiyamet.skill.Skill.SURVIVAL, 1500);
            baby.setOwner(owner);
            baby.setOrder(HumanEntity.Order.STAY);
            owner.sendSystemMessage(Component.literal("★ " + mother.getName().getString() + " gave birth! Say hello to " + baby.getName().getString() + ".").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        l.sendParticles(ParticleTypes.HEART, mother.getX(), mother.getY() + 1.5, mother.getZ(), 12, 0.5, 0.5, 0.5, 0);
        l.playSound(null, mother.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.4f);
        grow(l, baby);
    }

    /** 2.2: babies ride with their parent, wrapped in white, beside the backpack. Synced so every client can draw the bundle. */
    public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Integer> CARRIED = net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
            com.kiyamet.Kiyamet.id("carried_baby"), b -> b.initializer(() -> 0).persistent(com.mojang.serialization.Codec.INT)
                    .syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_INT, net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all()));
    public static final String CARRY_TAG = "carried_baby";

    static void carry(HumanEntity h, ServerPlayer o) {
        if (!h.getTags().contains(CARRY_TAG)) {
            h.addTag(CARRY_TAG);
            o.sendSystemMessage(Component.literal("★ " + h.getName().getString() + " is wrapped up and rides with you, beside your backpack.").withStyle(ChatFormatting.AQUA));
        }
        h.setInvisible(true); h.setInvulnerable(true); h.setNoAi(true); h.setNoGravity(true); h.setSilent(true);
        h.getNavigation().stop();
        if (o.getAttachedOrElse(CARRIED, 0) == 0) o.setAttached(CARRIED, 1);
    }

    static void release(ServerLevel l, HumanEntity h, String why) {
        if (!h.getTags().contains(CARRY_TAG)) return;
        h.removeTag(CARRY_TAG);
        h.setInvisible(false); h.setInvulnerable(false); h.setNoAi(false); h.setNoGravity(false); h.setSilent(false);
        if (h.ownerPlayer() instanceof ServerPlayer o) {
            o.setAttached(CARRIED, 0);
            if (why != null) o.sendSystemMessage(Component.literal("★ " + h.getName().getString() + " " + why).withStyle(ChatFormatting.AQUA));
        }
    }

    /** every tick while carried: stay right with the parent */
    public static void follow(ServerLevel l, HumanEntity h) {
        ServerPlayer o = h.ownerPlayer();
        if (o == null || o.level() != l || !o.isAlive()) { release(l, h, null); return; }
        h.snapTo(o.getX(), o.getY(), o.getZ(), o.getYRot(), 0);
        h.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        h.fallDistance = 0;
    }

    /** called every 2 seconds for children */
    public static void grow(ServerLevel l, HumanEntity h) {
        Stage s = stage(l, h);
        var scale = h.getAttribute(Attributes.SCALE);
        if (scale != null && scale.getBaseValue() != s.scale) {
            boolean first = scale.getBaseValue() != 1.0 || s != Stage.BABY;
            scale.setBaseValue(s.scale);
            if (first && h.ownerPlayer() instanceof ServerPlayer o && s != Stage.BABY)
                o.sendSystemMessage(Component.literal("★ " + h.getName().getString() + " is a " + s.title.toLowerCase() + " now!").withStyle(ChatFormatting.AQUA));
            if (s == Stage.ADULT) { h.bornDay = -1; h.applyStats(true); }
        }
        if (s.ordinal() <= Stage.CHILD.ordinal()) {
            h.setTarget(null);
            if (!h.getMainHandItem().isEmpty()) h.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            if (s == Stage.BABY && h.order() != HumanEntity.Order.STAY) h.setOrder(HumanEntity.Order.STAY);
        }
        if (s == Stage.BABY && h.ownerPlayer() instanceof ServerPlayer o && o.level() == l && o.isAlive()) carry(h, o);
        else if (s != Stage.BABY) release(l, h, "has grown - time to walk on their own!");
    }

    private Family() {}
}
