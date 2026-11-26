package com.kiyamet.season;

import com.kiyamet.Kiyamet;
import com.mojang.serialization.Codec;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.level.Level;

/**
 * 1.7 calendar: the apocalypse starts on 1 March 2027 and every Minecraft day is one calendar day.
 * Seasons follow the months: Mar-May spring, Jun-Aug summer, Sep-Nov autumn, Dec-Feb winter.
 */
public final class Calendar {
    public static final LocalDate START = LocalDate.of(2027, 3, 1);
    /** days added on top of the world clock (/twd date set) */
    public static final AttachmentType<Integer> OFFSET = AttachmentRegistry.create(Kiyamet.id("date_offset"), b -> b.initializer(() -> 0).persistent(Codec.INT));

    public static long dayIndex(Level level) {
        var ow = level.getServer() == null ? level : level.getServer().overworld();
        return ow.getDayTime() / 24000 + ow.getAttachedOrElse(OFFSET, 0);
    }

    public static LocalDate date(Level level) { return START.plusDays(dayIndex(level)); }
    public static LocalDate dateAt(Level level, long gameTime) {
        var ow = level.getServer() == null ? level : level.getServer().overworld();
        long dayTimeThen = ow.getDayTime() + (gameTime - ow.getGameTime());
        return START.plusDays(dayTimeThen / 24000 + ow.getAttachedOrElse(OFFSET, 0));
    }

    public static Seasons.Season seasonOf(LocalDate d) {
        return switch (d.getMonthValue()) { case 3, 4, 5 -> Seasons.Season.SPRING; case 6, 7, 8 -> Seasons.Season.SUMMER; case 9, 10, 11 -> Seasons.Season.AUTUMN; default -> Seasons.Season.WINTER; };
    }

    public static String format(LocalDate d) {
        return d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", " + d.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + d.getDayOfMonth() + ", " + d.getYear();
    }

    public static String shortFormat(LocalDate d) {
        return d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + d.getDayOfMonth() + ", " + d.getYear();
    }

    public static void init() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(net.minecraft.commands.Commands.literal("twd")
                .then(net.minecraft.commands.Commands.literal("date").executes(c -> {
                    var p = c.getSource().getPlayerOrException();
                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal("📅 " + format(date(p.level())) + " · " + Seasons.season(p.level()).title
                            + (SeasonEvents.today != SeasonEvents.Event.NONE ? " · " + SeasonEvents.today.title : "")).withStyle(net.minecraft.ChatFormatting.WHITE));
                    return 1;
                }).then(net.minecraft.commands.Commands.literal("set")
                        .then(net.minecraft.commands.Commands.argument("yyyy-mm-dd", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> {
                            var p = c.getSource().getPlayerOrException();
                            if (!p.isCreative()) return 0;
                            LocalDate want = LocalDate.parse(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "yyyy-mm-dd"));
                            var ow = p.level().getServer().overworld();
                            long now = ow.getDayTime() / 24000;
                            ow.setAttached(OFFSET, (int) (java.time.temporal.ChronoUnit.DAYS.between(START, want) - now));
                            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Date set: " + format(date(p.level()))));
                            return 1;
                        }))))));
    }

    private Calendar() {}
}
