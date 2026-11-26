package com.kiyamet.npc;

import com.kiyamet.skill.SkillManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import static com.kiyamet.npc.Talk.*;

/** All chat conversations: community first contact (three questions), survivors, leaders, traders, envoys, companion chatter. */
public final class Dialogues {

    // ------------------------------------------------------------------ community first contact

    /** A friendly community stops you at the gate and asks the three questions. The answers decide your standing. */
    static void threeQuestions(ServerLevel level, ServerPlayer p, HumanEntity h) {
        String key = h.community(), name = Communities.displayName(h.communityType());
        h.getNavigation().moveTo(p, 0.8);
        h.getLookControl().setLookAt(p);
        int[] score = {20};
        var timeout = (java.util.function.Consumer<ServerPlayer>) pl -> finish(level, pl, h, key, name, 8, true);
        say(p, h, pick(level.random, "Stop right there! Keep your hands where I can see them.", "Hold it! That's close enough.", "Easy now. Don't make me use this."));
        ask(p, h, 30, "Three questions. Answer honest and you might walk in. How many walkers have you killed?", timeout,
                new Option("Too many to count.", pl -> { score[0] += 5; q2(level, pl, h, key, name, score, timeout); }),
                new Option("A few. Only when I had to.", pl -> { score[0] += 5; q2(level, pl, h, key, name, score, timeout); }),
                new Option("None. I hide from them.", pl -> { score[0] -= 5; sayLater(pl, h, 10, "Hiding only works so long."); q2(level, pl, h, key, name, score, timeout); }));
    }

    private static void q2(ServerLevel level, ServerPlayer p, HumanEntity h, String key, String name, int[] score, java.util.function.Consumer<ServerPlayer> timeout) {
        ask(p, h, 30, "How many people have you killed?", timeout,
                new Option("None.", pl -> q3(level, pl, h, key, name, score, timeout, false)),
                new Option("Some. Raiders, mostly.", pl -> { score[0] += 3; q3(level, pl, h, key, name, score, timeout, true); }),
                new Option("That's none of your business.", pl -> { score[0] -= 10; sayLater(pl, h, 10, "Everything is our business at this gate."); q3(level, pl, h, key, name, score, timeout, true); }));
    }

    private static void q3(ServerLevel level, ServerPlayer p, HumanEntity h, String key, String name, int[] score, java.util.function.Consumer<ServerPlayer> timeout, boolean killed) {
        if (killed) ask(p, h, 30, "Why?", timeout,
                new Option("They tried to kill me first.", pl -> { score[0] += 5; finish(level, pl, h, key, name, score[0], false); }),
                new Option("To protect my people.", pl -> { score[0] += 10; finish(level, pl, h, key, name, score[0], false); }),
                new Option("They had what I needed.", pl -> { score[0] -= 25; finish(level, pl, h, key, name, score[0], false); }));
        else ask(p, h, 30, "Then why should we trust you?", timeout,
                new Option("I can work: build, fight, farm.", pl -> { score[0] += 5; finish(level, pl, h, key, name, score[0], false); }),
                new Option("I'll trade fairly.", pl -> { score[0] += 5; finish(level, pl, h, key, name, score[0], false); }),
                new Option("You shouldn't.", pl -> { score[0] -= 10; finish(level, pl, h, key, name, score[0], false); }));
    }

    private static void finish(ServerLevel level, ServerPlayer p, HumanEntity h, String key, String name, int score, boolean silent) {
        h.getNavigation().stop();
        if (silent) {
            say(p, h, "No answer? Fine. You can trade, but keep your distance.");
            Communities.set(p, key, score);
            return;
        }
        if (score >= 20) {
            sayLater(p, h, 25, "Alright. Welcome to " + name + ". Talk to our leader, and check the mission board by the gate.");
            later(p, 30, () -> {
                p.sendSystemMessage(Component.literal("» " + name + " lets you in. (Reputation " + score + ")").withStyle(ChatFormatting.GREEN));
                level.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 1f, 1.2f);
            });
            Communities.set(p, key, score);
        } else if (score >= 0) {
            sayLater(p, h, 25, "You can come in and trade. But we're watching you.");
            later(p, 30, () -> p.sendSystemMessage(Component.literal("» " + name + " lets you in, warily. (Reputation " + score + ")").withStyle(ChatFormatting.YELLOW)));
            Communities.set(p, key, score);
        } else {
            sayLater(p, h, 20, "Wrong answers. Get out of here - NOW!");
            later(p, 40, () -> {
                Communities.set(p, key, -50);
                p.sendSystemMessage(Component.literal("» " + name + " sees you as a threat! They open fire!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1f, 1.4f);
                for (HumanEntity ally : h.allies(level, 40)) if (ally.getTarget() == null) ally.setTarget(p);
                h.setTarget(p);
            });
        }
    }

    // ------------------------------------------------------------------ lone survivors

    static void survivor(ServerPlayer p, HumanEntity h) {
        RandomSource r = h.getRandom();
        h.getNavigation().stop();
        say(p, h, pick(r, "Stay back! ...You're not one of them, are you?", "You're alive. Good. Me too. Barely.", "Don't shoot! I'm not looking for trouble.",
                "Another living face... it's been weeks.", "If you're here to rob me, I've got nothing."));
        ask(p, h, 25, "What do you want?", null,
                new Option("Join my group. Strength in numbers.", pl -> recruit(pl, h)),
                new Option("Seen anything out there?", pl -> { say(pl, h, pick(r, RUMORS)); sayLater(pl, h, 40, "That's all I know. Watch your back."); }),
                new Option("Nothing. Good luck out there.", pl -> say(pl, h, pick(r, "You too. Don't get bit.", "Yeah... luck. Haven't seen any of that lately.", "Stay safe."))));
    }

    private static final String[] RUMORS = {
            "A herd passed by the river two days ago. Hundreds. They follow noise.",
            "Raiders patrol the highway in pickup trucks. If you hear engines, hide.",
            "There's a prison up north. People live there now. Walls and everything.",
            "When the moon turns red, the dead get faster. Don't be outside.",
            "I heard about people who wear the dead's skin. They walk with herds. Stay away from them.",
            "There's fuel at the gas stations if you're brave enough to go in.",
            "Acid rain eats through clothes. Find a roof or wear a helmet.",
            "Communities ask three questions at the gate. Answer honest.",
            "Pharmacies still have medkits in the back rooms.",
            "Their Commander drives an armored truck with a gun on top. Run if you see it."};

    private static void recruit(ServerPlayer p, HumanEntity h) {
        if (RecruitMenu.companions(p) >= RecruitMenu.MAX_COMPANIONS) { say(p, h, "Looks like you've got enough mouths to feed already."); return; }
        int cost = RecruitMenu.cost(h);
        boolean hurt = h.getHealth() < h.getMaxHealth() * 0.7f;
        ask(p, h, 25, hurt ? "I'm hurt... and you want me to follow you? What's in it for me?" : "And what's in it for me?", null,
                new Option("Rubies: " + cost + ". Yours now.", pl -> {
                    if (Shop.take(pl, Shop.RUBY, cost)) RecruitMenu.join(pl, h, "...Alright. You've got yourself a partner.");
                    else say(pl, h, "You don't even have " + cost + " rubies. Come back when you do.");
                }),
                new Option(hurt ? "A medkit. You need it." : "A medkit, for when you need it.", pl -> {
                    if (Shop.take(pl, "kiyamet:medkit", 1)) RecruitMenu.join(pl, h, "You'd give that up for me? ...Okay. I'm with you.");
                    else say(pl, h, "Where? I don't see a medkit.");
                }),
                new Option("Food. Three loaves of bread.", pl -> {
                    if (!Shop.take(pl, "minecraft:bread", 3)) { say(pl, h, "Bread you don't have doesn't fill my stomach."); return; }
                    if (h.getRandom().nextBoolean()) RecruitMenu.join(pl, h, "*eats* ...Okay. Okay, I'll come with you.");
                    else say(pl, h, "Thanks... really. But I'm not ready to trust you yet.");
                }),
                new Option("Just my word. I'll keep you alive.", pl -> {
                    float chance = 0.12f + Math.min(0.35f, SkillManager.get(pl).totalPower() / 8000f);
                    if (h.getRandom().nextFloat() < chance) RecruitMenu.join(pl, h, "Something about you... fine. I believe you.");
                    else say(pl, h, pick(h.getRandom(), "Words are cheap these days.", "Everyone says that. Most of them are dead.", "Prove it first."));
                }));
    }

    // ------------------------------------------------------------------ community leader & trader

    static void leader(ServerPlayer p, HumanEntity h) {
        RandomSource r = h.getRandom();
        int rep = Communities.reputation(p, h.community());
        say(p, h, rep >= 60 ? pick(r, "Good to see you again, friend.", "Our people speak well of you.") : pick(r, Communities.greeting(r, h.communityType()), "You're the one everyone's talking about."));
        ask(p, h, 25, "What brings you to me?", null,
                new Option("Any work for me?", pl -> say(pl, h, "Check the mission board by the gate. Three jobs a day - we pay in rubies.")),
                new Option("I want to help your people. (donate / volunteers)", pl -> LeaderMenu.open(pl, h)),
                new Option("Can someone from here join me?", pl -> {
                    if (Communities.reputation(pl, h.community()) >= 40) { say(pl, h, "Some of ours would follow you. Let's talk."); LeaderMenu.open(pl, h); }
                    else say(pl, h, "Earn our trust first. (Reputation " + Communities.reputation(pl, h.community()) + " / 40)");
                }),
                new Option("Just passing through.", pl -> say(pl, h, pick(r, "Then pass safely.", "Mind the walkers on the road.", "Door's open if you need us."))));
    }

    static void trader(ServerPlayer p, HumanEntity h) {
        RandomSource r = h.getRandom();
        say(p, h, pick(r, "Got goods, if you've got rubies.", "Take a look. No haggling, no stealing.", "Everything here was paid for in blood. Prices reflect that.",
                "Fresh stock from the last supply run."));
        TradeMenu.open(p, h);
    }

    // ------------------------------------------------------------------ raider envoy

    static void envoy(ServerPlayer p, HumanEntity h, int demand, java.util.function.IntConsumer setDemand, Runnable pay, Runnable refuse) {
        RandomSource r = h.getRandom();
        int have = Shop.count(p, Shop.RUBY);
        say(p, h, pick(r, "Nice place you got. Shame if something happened to it.", "Relax. I'm just here to talk business.", "The Commander sends his regards."));
        ask(p, h, 30, demand + " rubies. Pay, and nobody bothers you for a while. (You have " + have + ")", pl -> { },
                new Option("Fine. Take them. (" + demand + " rubies)", pl -> {
                    if (Shop.count(pl, Shop.RUBY) >= demand) pay.run();
                    else say(pl, h, "You're short. Don't waste my time - get the rest before I leave.");
                }),
                new Option("Half. Take it or leave it.", pl -> {
                    float chance = 0.25f + Math.min(0.35f, SkillManager.get(pl).totalPower() / 10000f);
                    if (r.nextFloat() < chance) { int half = Math.max(1, demand / 2); setDemand.accept(half); say(pl, h, "...You've got guts. " + half + " then. Right now."); }
                    else { int more = demand * 3 / 2; setDemand.accept(more); say(pl, h, "Funny. Now it's " + more + "."); }
                    sayLater(pl, h, 30, "Right-click me when you're ready to pay.");
                }),
                new Option("Get off my land.", pl -> { say(pl, h, "Bad choice. See you tonight."); refuse.run(); }));
    }

    // ------------------------------------------------------------------ companion chatter

    private static final String[] IDLE = {
            "Quiet day. I don't trust quiet days.", "You ever think about what it was like before?", "My feet are killing me.",
            "I used to complain about traffic. Can you believe that?", "We should find more medkits.", "I heard something over there... probably nothing.",
            "Thanks for keeping me around.", "When this is over, I'm sleeping for a week.", "Remember: aim for the head.", "Smell that? Something's rotting nearby.",
            "I miss coffee. Real coffee.", "We make a good team, you know.", "Don't go running off without me.", "If I get bit... you know what to do."};

    /** A random line from a companion now and then, fitting what's going on. */
    static void chatter(ServerPlayer owner, HumanEntity h) {
        RandomSource r = h.getRandom();
        String line;
        if (owner.getHealth() < owner.getMaxHealth() * 0.35f) line = pick(r, "You're bleeding bad - patch yourself up!", "Hey! You look terrible. Use a medkit!");
        else if (h.getHealth() < h.getMaxHealth() * 0.35f) line = pick(r, "I need a medkit... or a bandage... anything.", "I'm hurt. Bad.");
        else if (BaseRaids.bloodMoon()) line = pick(r, "That moon... I don't like it. Stay close.", "They're everywhere tonight!");
        else if (WorldEvents.acidRain() && owner.level().isRainingAt(owner.blockPosition().above())) line = pick(r, "This rain burns! Get under something!", "My skin is on fire - find a roof!");
        else if (!owner.level().isBrightOutside()) line = pick(r, "Can't see a thing out here.", "Night's when they come. Keep your eyes open.", "Should we find somewhere to hole up?");
        else line = pick(r, IDLE);
        say(owner, h, line);
    }

    private Dialogues() {}
}
