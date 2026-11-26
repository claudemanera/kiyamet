package com.kiyamet.npc;

import com.kiyamet.entity.KZombie;
import com.kiyamet.registry.ModItems;
import com.kiyamet.twd.Camouflage;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Prisoners. Put a lead (kayış) on any ordinary walker - or on a raider who's badly hurt (under 40% health) and they
 * surrender. Captives don't fight. Lead them somewhere closed and tie them: to a fence (a normal leash knot) or to
 * iron bars (they're chained there). Talk to them (right-click): raiders can be interrogated, fed, recruited or let go;
 * walkers can be milked for guts or put down. Two or more walkers on leads beside you hide you from other walkers,
 * like the pets with no jaws and no arms.
 */
public final class Captives {
    /** Shared captive state, stored on each walker / human. */
    public static final class Data {
        public boolean captive;
        public BlockPos post;
        public long since;
        public int fed;
        public long cooldown;
        public boolean talked;
    }

    public static void init() {
        // tie captives to iron bars
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            if (!level.getBlockState(pos).is(Blocks.IRON_BARS)) return InteractionResult.PASS;
            int n = 0;
            for (Leashable l : Leashable.leashableLeashedTo(sp)) {
                Data d = data(l);
                if (d == null || !d.captive) continue;
                d.post = pos.immutable();
                l.removeLeash();
                n++;
            }
            if (n == 0) return InteractionResult.PASS;
            sl.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
            sp.displayClientMessage(Component.literal(n + " captive" + (n > 1 ? "s" : "") + " chained to the bars.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        });

        // walker pets hide you
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                int pets = 0;
                for (Leashable l : Leashable.leashableLeashedTo(p)) if (l instanceof KZombie z && z.captives.captive && z.distanceTo(p) < 8) pets++;
                if (pets >= 2) p.addEffect(new MobEffectInstance(Camouflage.EFFECT, 60, 0, true, false, true));
            }
        });
    }

    static Data data(Object o) {
        if (o instanceof KZombie z) return z.captives;
        if (o instanceof HumanEntity h) return h.captives;
        return null;
    }

    /** Can a lead be put on this right now? */
    public static boolean canCapture(Mob m) {
        Data d = data(m);
        if (d == null) return false;
        if (d.captive) return true;
        if (m instanceof KZombie z) return z.kind.capturable();
        if (m instanceof HumanEntity h) return (h.faction() == Faction.RAIDER || h.faction() == Faction.WHISPERER) && h.role() != Role.COMMANDER && !h.envoy && h.getHealth() < h.getMaxHealth() * 0.4f;
        return false;
    }

    /** Every tick on the server for walkers and humans. */
    public static void tick(ServerLevel level, Mob m, Data d) {
        if (!d.captive) {
            if (m instanceof Leashable l && l.isLeashed()) capture(level, m, d, l.getLeashHolder());
            return;
        }
        m.setTarget(null);
        m.setPersistenceRequired();
        if (d.post != null && !(m instanceof Leashable l && l.isLeashed())) {
            if (!level.getBlockState(d.post).is(Blocks.IRON_BARS)) { d.post = null; return; }
            double dist = m.position().distanceTo(Vec3.atCenterOf(d.post));
            if (dist > 8) m.teleportTo(d.post.getX() + 0.5, d.post.getY(), d.post.getZ() + 0.5);
            else if (dist > 2.5) m.getNavigation().moveTo(d.post.getX() + 0.5, d.post.getY(), d.post.getZ() + 0.5, 1.0);
            if (m.tickCount % 40 == 0) {   // the chain
                Vec3 a = Vec3.atCenterOf(d.post), b = m.position().add(0, m.getBbHeight() * 0.6, 0);
                for (int i = 1; i < 6; i++) { Vec3 q = a.lerp(b, i / 6.0); level.sendParticles(ParticleTypes.WHITE_ASH, q.x, q.y, q.z, 1, 0, 0, 0, 0); }
            }
        }
    }

    static void capture(ServerLevel level, Mob m, Data d, Entity holder) {
        d.captive = true;
        d.since = level.getGameTime();
        m.setTarget(null);
        if (holder instanceof ServerPlayer p) {
            if (m instanceof HumanEntity h) {
                Talk.say(p, h, Talk.pick(h.getRandom(), "Okay! Okay! I give up! Don't kill me!", "Fine... you got me.", "Alright, alright. I surrender."));
            } else if (m instanceof KZombie z) {
                String who = z.formerName != null ? z.formerName : "The walker";
                p.sendSystemMessage(Component.literal(who + " snaps at the rope, but it holds. " + (z.formerName != null ? "Its eyes... it was " + z.formerName + "." : "You pull its teeth and bind its arms.")).withStyle(ChatFormatting.DARK_GREEN));
            }
            p.sendSystemMessage(Component.literal("Captured! Tie them to a fence or iron bars (right-click the bars). Right-click them to talk.").withStyle(ChatFormatting.GRAY));
        }
        level.playSound(null, m.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1f, 0.8f);
    }

    static int days(ServerLevel level, Data d) { return (int) ((level.getGameTime() - d.since) / 24000); }

    // ------------------------------------------------------------------ talking to a captive raider

    public static void talk(ServerPlayer p, HumanEntity h) {
        ServerLevel level = (ServerLevel) p.level();
        Data d = h.captives;
        int days = days(level, d);
        Talk.say(p, h, days == 0 ? Talk.pick(h.getRandom(), "What do you want?", "Come to gloat?", "Untie me and we'll talk.")
                : Talk.pick(h.getRandom(), "Day " + (days + 1) + "... you gonna keep me here forever?", "My arms are numb.", "...", "You again."));
        Talk.ask(p, h, 25, "", null,
                new Talk.Option("Where's your crew's stash?", pl -> interrogate(pl, h, level)),
                new Talk.Option("Here, eat something. (1 food in hand)", pl -> feed(pl, h)),
                new Talk.Option("Join us. Start over.", pl -> {
                    float chance = 0.08f + days * 0.15f + d.fed * 0.1f;
                    if (h.getRandom().nextFloat() < chance) {
                        d.captive = false; d.post = null;
                        if (h.isLeashed()) h.removeLeash();
                        Talk.say(pl, h, "...You fed me. You didn't kill me. Nobody in my crew would've done that. I'm in.");
                        h.setOwner(pl);
                        pl.sendSystemMessage(Component.literal(h.getName().getString() + " joins you!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    } else Talk.say(pl, h, Talk.pick(h.getRandom(), "Join you? Ha. Ask me again in a few days.", "Not a chance.", "I'm thinking about it. Maybe. Bring food."));
                }),
                new Talk.Option("You're free. Go.", pl -> {
                    Talk.say(pl, h, "...Seriously? I won't forget this.");
                    Talk.later(pl, 40, () -> { if (h.isAlive()) { level.sendParticles(ParticleTypes.POOF, h.getX(), h.getY() + 1, h.getZ(), 10, 0.3, 0.5, 0.3, 0.02); h.discard(); } });
                }),
                new Talk.Option("Nothing.", pl -> {}));
    }

    private static void feed(ServerPlayer p, HumanEntity h) {
        ItemStack held = p.getMainHandItem();
        if (!held.has(net.minecraft.core.component.DataComponents.FOOD)) { Talk.say(p, h, "That's not food."); return; }
        held.shrink(1);
        h.captives.fed++;
        h.heal(4);
        Talk.say(p, h, Talk.pick(h.getRandom(), "*eats fast* ...Thanks.", "Why are you doing this?", "...I didn't think you'd feed me."));
    }

    private static void interrogate(ServerPlayer p, HumanEntity h, ServerLevel level) {
        Data d = h.captives;
        if (d.talked) { Talk.say(p, h, "I already told you everything!"); return; }
        if (level.getGameTime() < d.cooldown) { Talk.say(p, h, "I said I ain't talking. Leave me alone."); return; }
        float chance = 0.35f + days(level, d) * 0.2f + d.fed * 0.05f;
        if (h.getRandom().nextFloat() >= chance) { d.cooldown = level.getGameTime() + 2400; Talk.say(p, h, Talk.pick(h.getRandom(), "I ain't telling you nothing.", "Go to hell.", "You'll have to do better than that.")); return; }
        d.talked = true;
        double a = level.random.nextDouble() * Math.PI * 2;
        int dist = 80 + level.random.nextInt(80);
        BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                h.blockPosition().offset((int) (Math.cos(a) * dist), 0, (int) (Math.sin(a) * dist)));
        level.setBlock(at, Blocks.BARREL.defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity b) {
            b.setItem(4, com.kiyamet.money.Money.stack((6 + level.random.nextInt(10)) * 10));
            b.setItem(10, new ItemStack(Items.ARROW, 32));
            b.setItem(13, new ItemStack(ModItems.get("medkit"), 1 + level.random.nextInt(2)));
            b.setItem(16, new ItemStack(ModItems.get("circuit"), 1 + level.random.nextInt(3)));
            if (level.random.nextBoolean()) b.setItem(22, new ItemStack(ModItems.get("fuel_canister"), 1));
        }
        String dir = WorldEvents.dir(at, h.blockPosition());
        Talk.say(p, h, "Fine! There's a barrel buried at our old drop point. " + dist + " steps " + dir + " of here. Happy?");
        p.sendSystemMessage(Component.literal("Stash: " + at.getX() + " " + at.getY() + " " + at.getZ() + " (" + dir + ")").withStyle(ChatFormatting.GOLD));
    }

    // ------------------------------------------------------------------ captive walkers

    public static void walker(ServerPlayer p, KZombie z) {
        ServerLevel level = (ServerLevel) p.level();
        Data d = z.captives;
        boolean friend = z.formerOwner != null && z.formerOwner.equals(p.getUUID());
        String name = z.formerName != null ? z.formerName : "Walker";
        p.sendSystemMessage(Component.literal(name + ": ").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD)
                .append(Component.literal(friend ? "*growls... the face is still " + name + "'s*" : "*snarls and snaps its teeth*").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        Talk.askAs(p, z, name, ChatFormatting.DARK_GREEN, 15, "", null,
                new Talk.Option("Collect walker guts (camouflage)", pl -> {
                    if (level.getGameTime() < d.cooldown) { pl.displayClientMessage(Component.literal("Nothing left to take. Wait a few minutes.").withStyle(ChatFormatting.GRAY), true); return; }
                    d.cooldown = level.getGameTime() + 20 * 180;
                    com.kiyamet.npc.Shop.give(pl, new ItemStack(ModItems.get("walker_guts"), 2));
                    level.playSound(null, z.blockPosition(), SoundEvents.HONEY_BLOCK_BREAK, SoundSource.NEUTRAL, 1f, 0.6f);
                }),
                new Talk.Option(friend ? "Rest now, " + name + "." : "Put it down.", pl -> {
                    if (friend) pl.sendSystemMessage(Component.literal("You close " + name + "'s eyes one last time.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                    z.kill(level);
                }),
                new Talk.Option("Leave it.", pl -> {}));
    }

    public static void save(net.minecraft.world.level.storage.ValueOutput out, Data d) {
        out.putBoolean("Captive", d.captive);
        if (d.post != null) out.putLong("CaptivePost", d.post.asLong());
        out.putLong("CaptiveSince", d.since);
        out.putInt("CaptiveFed", d.fed);
        out.putBoolean("CaptiveTalked", d.talked);
    }

    public static void load(net.minecraft.world.level.storage.ValueInput in, Data d) {
        d.captive = in.getBooleanOr("Captive", false);
        long post = in.getLongOr("CaptivePost", Long.MIN_VALUE);
        d.post = post == Long.MIN_VALUE ? null : BlockPos.of(post);
        d.since = in.getLongOr("CaptiveSince", 0);
        d.fed = in.getIntOr("CaptiveFed", 0);
        d.talked = in.getBooleanOr("CaptiveTalked", false);
    }

    private Captives() {}
}
