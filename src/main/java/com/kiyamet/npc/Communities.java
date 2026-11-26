package com.kiyamet.npc;

import com.kiyamet.Kiyamet;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Settlements. Each has its own reputation with each player (-100..100). On first contact the community decides:
 * welcome you (rep +20) or treat you as a threat (rep -50). Attacking a member drops reputation and turns them hostile.
 */
public final class Communities {
    public static final AttachmentType<Map<String, Integer>> REPUTATION = AttachmentRegistry.create(Kiyamet.id("reputation"),
            b -> b.initializer(Map::of).persistent(Codec.unboundedMap(Codec.STRING, Codec.INT)).copyOnDeath());

    public record Type(String id, String title, float friendly, String[] greetings) {}

    public static final Map<String, Type> TYPES = new HashMap<>();

    static {
        add("prison", "Prison Survivor", 0.65f, "These walls kept the dead out. Mostly.", "Don't go near cell block C.", "We grow what we can in the yard.");
        add("farm", "Farmhand", 0.8f, "The barn? Don't open the barn.", "We've got eggs if you've got rubies.", "Wipe your boots, we're decent folk.");
        add("walled_town", "Townsfolk", 0.6f, "Welcome to the Safe Zone. Rules are rules.", "We have showers. Real ones.", "Keep the gate shut behind you.");
        add("hilltop", "Hilltop Settler", 0.7f, "We trade, we farm, we survive.", "The blacksmith could use more iron.", "Watch out for the raiders on the roads.");
        add("kingdom", "Kingdom Citizen", 0.75f, "Hail, traveller! The Kingdom welcomes you.", "We grow pomegranates. Well, apples.", "Our guards train every morning.");
        add("sanctuary", "Raider", 0f, "You're on our turf.");
        add("terminal", "Terminal Dweller", 0.35f, "Sanctuary for all. Community for all.", "Come in, we'll get you a plate.", "Don't mind the train cars.");
        add("camp", "Camper", 0.7f, "Keep your voice down, they hear everything.", "We move at dawn.", "Got any spare arrows?");
    }

    private static void add(String id, String title, float friendly, String... greetings) { TYPES.put(id, new Type(id, title, friendly, greetings)); }

    public static String title(String type) { Type t = TYPES.get(type); return t == null ? "Survivor" : t.title; }

    public static String greeting(RandomSource r, String type) {
        Type t = TYPES.get(type);
        return t == null ? "Stay safe out there." : t.greetings[r.nextInt(t.greetings.length)];
    }

    public static String displayName(String type) {
        return switch (type) {
            case "prison" -> "The Prison"; case "farm" -> "The Farm"; case "walled_town" -> "The Safe Zone"; case "hilltop" -> "Hilltop";
            case "kingdom" -> "The Kingdom"; case "sanctuary" -> "The Sanctuary"; case "terminal" -> "The Terminal"; default -> "The Camp";
        };
    }

    /** Finds the community key: shared with members already settled nearby, else a new one from this member's position. */
    public static String assign(ServerLevel level, HumanEntity h) {
        for (HumanEntity o : level.getEntitiesOfClass(HumanEntity.class, h.getBoundingBox().inflate(64),
                o -> o != h && !o.community().isEmpty() && o.communityType().equals(h.communityType())))
            return o.community();
        return h.communityType() + "@" + (h.blockPosition().getX() >> 4) + "," + (h.blockPosition().getZ() >> 4);
    }

    public static int reputation(Player p, String key) {
        if (key == null || key.isEmpty()) return 0;
        return p.getAttachedOrElse(REPUTATION, Map.of()).getOrDefault(key, 0);
    }

    public static boolean known(Player p, String key) { return p.getAttachedOrElse(REPUTATION, Map.of()).containsKey(key); }

    public static void change(ServerPlayer p, String key, int delta) {
        if (key.isEmpty()) return;
        Map<String, Integer> m = new HashMap<>(p.getAttachedOrElse(REPUTATION, Map.of()));
        int before = m.getOrDefault(key, 0), after = Math.max(-100, Math.min(100, before + delta));
        m.put(key, after);
        p.setAttached(REPUTATION, Map.copyOf(m));
        String type = key.contains("@") ? key.substring(0, key.indexOf('@')) : key;
        if (before >= 0 && after < 0) p.sendSystemMessage(Component.literal(displayName(type) + " now sees you as an enemy!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        if (before < 0 && after >= 0) p.sendSystemMessage(Component.literal(displayName(type) + " is willing to trust you again.").withStyle(ChatFormatting.GREEN));
    }

    /** First contact: called every 2 s for players near a community member they have never met. */
    public static void set(ServerPlayer p, String key, int value) {
        Map<String, Integer> m = new HashMap<>(p.getAttachedOrElse(REPUTATION, Map.of()));
        m.put(key, value);
        p.setAttached(REPUTATION, Map.copyOf(m));
    }

    public static void firstContact(ServerLevel level, ServerPlayer p, HumanEntity h) {
        String key = h.community();
        if (key.isEmpty() || known(p, key)) return;
        Type t = TYPES.get(h.communityType());
        float friendly = t == null ? 0.6f : t.friendly;
        friendly += Math.min(0.15f, com.kiyamet.skill.SkillManager.get(p).totalPower() / 40000f);   // famous survivors are trusted more
        boolean ok = level.random.nextFloat() < friendly;
        if (ok && !Talk.busy(p)) {                       // friendly communities stop you at the gate and ask
            set(p, key, 0);
            Dialogues.threeQuestions(level, p, h);
            return;
        }
        Map<String, Integer> m = new HashMap<>(p.getAttachedOrElse(REPUTATION, Map.of()));
        m.put(key, ok ? 20 : -50);
        p.setAttached(REPUTATION, Map.copyOf(m));
        String name = displayName(h.communityType());
        if (ok) {
            p.sendSystemMessage(Component.literal("» " + name + " lets you in. Find their leader or trader.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            level.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 1f, 1.2f);
        } else {
            p.sendSystemMessage(Component.literal("» " + name + " sees you as a threat! They open fire!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1f, 1.4f);
        }
    }

    public static void onMemberHurt(ServerLevel level, HumanEntity h, ServerPlayer p) {
        change(p, h.community(), reputation(p, h.community()) >= 0 ? -60 : -10);
        for (HumanEntity ally : h.allies(level, 40)) if (ally.getTarget() == null) ally.setTarget(p);
    }

    private Communities() {}
}
