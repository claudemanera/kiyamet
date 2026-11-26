package com.kiyamet.npc;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.function.Consumer;

/**
 * Conversations in the normal game chat. The game never pauses: people say their lines in chat with a short
 * "typing" delay, and your answers appear as clickable [1] [2] [3] options (or just type the number).
 * Walking away (20 blocks) or waiting 60 seconds ends the talk.
 */
public final class Talk {
    public record Option(String text, Consumer<ServerPlayer> then) {}

    private static final class Convo {
        UUID npc;
        List<Option> options = List.of();
        long expires;
        Consumer<ServerPlayer> onTimeout;
    }

    private record Later(long at, UUID player, Runnable run) {}

    private static final Map<UUID, Convo> ACTIVE = new HashMap<>();
    private static final List<Later> QUEUE = new ArrayList<>();
    private static MinecraftServer server;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            server = s;
            long now = s.getTickCount();
            if (!QUEUE.isEmpty()) {
                List<Later> due = new ArrayList<>();
                for (Iterator<Later> it = QUEUE.iterator(); it.hasNext(); ) { Later l = it.next(); if (l.at <= now) { due.add(l); it.remove(); } }
                for (Later l : due) if (s.getPlayerList().getPlayer(l.player) != null) l.run.run();
            }
            if (now % 20 != 0) return;
            for (Iterator<Map.Entry<UUID, Convo>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Convo> e = it.next();
                ServerPlayer p = s.getPlayerList().getPlayer(e.getKey());
                Convo c = e.getValue();
                Entity npc = p == null ? null : ((ServerLevel) p.level()).getEntity(c.npc);
                boolean gone = p == null || npc == null || !npc.isAlive() || npc.distanceTo(p) > 28;
                if (gone || now > c.expires) {
                    it.remove();
                    if (p != null && c.onTimeout != null) c.onTimeout.accept(p);
                }
            }
        });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            Convo c = ACTIVE.get(sender.getUUID());
            if (c == null) return true;
            String t = message.signedContent().trim();
            if (t.length() == 1 && Character.isDigit(t.charAt(0))) { choose(sender, t.charAt(0) - '0'); return false; }
            return true;
        });

        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(Commands.literal("twd")
                .then(Commands.literal("talk").then(Commands.argument("n", IntegerArgumentType.integer(1, 9)).executes(c -> {
                    choose(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
                    return 1;
                })))));
    }

    public static boolean busy(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    private static long now() { return server == null ? 0 : server.getTickCount(); }

    public static void later(ServerPlayer p, int ticks, Runnable r) {
        if (ticks <= 0) r.run(); else QUEUE.add(new Later(now() + ticks, p.getUUID(), r));
    }

    static ChatFormatting color(HumanEntity h) {
        return switch (h.faction()) {
            case COMMUNITY -> ChatFormatting.GREEN;
            case SURVIVOR -> ChatFormatting.YELLOW;
            case RAIDER -> ChatFormatting.RED;
            case COMPANION -> ChatFormatting.AQUA;
            default -> ChatFormatting.WHITE;
        };
    }

    /** Typing time for a line, in ticks. */
    static int typing(String line) { return Math.min(50, 12 + line.length() / 2); }

    /** A line in chat from this person: "Name: text". */
    public static void say(ServerPlayer p, HumanEntity h, String line) {
        p.sendSystemMessage(Component.literal(h.getName().getString() + ": ").withStyle(color(h), ChatFormatting.BOLD)
                .append(Component.literal(line).withStyle(ChatFormatting.WHITE)));
        p.level().playSound(null, h.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.35f, h.appearance().female() ? 1.35f : 0.9f);
    }

    /** Say a line after a delay. */
    public static void sayLater(ServerPlayer p, HumanEntity h, int delay, String line) {
        later(p, delay, () -> { if (h.isAlive()) say(p, h, line); });
    }

    /** A line from a named character that isn't (or needn't be) a visible person. */
    public static void sayAs(ServerPlayer p, String name, ChatFormatting col, String line) {
        p.sendSystemMessage(Component.literal(name + ": ").withStyle(col, ChatFormatting.BOLD).append(Component.literal(line).withStyle(ChatFormatting.WHITE)));
    }

    public static void sayAsLater(ServerPlayer p, int delay, String name, ChatFormatting col, String line) { later(p, delay, () -> sayAs(p, name, col, line)); }

    /** Say a question after a delay, then show the options and wait for the answer. */
    public static void ask(ServerPlayer p, HumanEntity h, int delay, String line, Consumer<ServerPlayer> onTimeout, Option... options) {
        askAs(p, h, h.getName().getString(), color(h), delay, line, onTimeout, options);
    }

    /** Like ask, for any speaker; the talk ends if you walk 28 blocks from {@code anchor}. */
    public static void askAs(ServerPlayer p, Entity anchor, String name, ChatFormatting col, int delay, String line, Consumer<ServerPlayer> onTimeout, Option... options) {
        Convo c = new Convo();
        c.npc = anchor.getUUID();
        c.options = List.of(options);
        c.onTimeout = onTimeout;
        c.expires = now() + delay + 20 * 60;
        ACTIVE.put(p.getUUID(), c);
        later(p, delay, () -> {
            if (!anchor.isAlive() || ACTIVE.get(p.getUUID()) != c) return;
            if (!line.isEmpty()) sayAs(p, name, col, line);
            for (int i = 0; i < options.length; i++) {
                int n = i + 1;
                MutableComponent opt = Component.literal("   [" + n + "] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .append(Component.literal(options[i].text).withStyle(ChatFormatting.GRAY));
                opt.withStyle(st -> st.withClickEvent(new ClickEvent.RunCommand("/twd talk " + n))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click, or type " + n + " in chat"))));
                p.sendSystemMessage(opt);
            }
        });
    }

    static void choose(ServerPlayer p, int n) {
        Convo c = ACTIVE.get(p.getUUID());
        if (c == null || n < 1 || n > c.options.size()) return;
        ACTIVE.remove(p.getUUID());
        Option o = c.options.get(n - 1);
        p.sendSystemMessage(Component.literal("You: ").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD)
                .append(Component.literal(o.text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        o.then.accept(p);
    }

    public static void end(ServerPlayer p) { ACTIVE.remove(p.getUUID()); }

    static String pick(net.minecraft.util.RandomSource r, String... lines) { return lines[r.nextInt(lines.length)]; }

    private Talk() {}
}
