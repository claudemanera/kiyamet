package com.kiyamet.skill;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** /skills — lists all 20 skills, their level and progress, plus Total Power. */
public final class SkillCommand {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
                dispatcher.register(Commands.literal("skills").executes(ctx -> { SkillsMenu.open(ctx.getSource().getPlayerOrException()); return 1; })
                        .then(Commands.literal("text").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    SkillData d = SkillManager.get(p);
                    MutableComponent out = Component.translatable("kiyamet.msg.skills_title", d.totalPower()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                    for (Skill s : Skill.values()) {
                        int lv = d.level(s);
                        int pct = lv >= Skill.MAX_LEVEL ? 100 : (int) (100 * d.xp(s) / XpCurve.toNext(lv));
                        out.append(Component.literal("\n "))
                                .append(Component.translatable(s.key()).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal("  " + lv).withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal("  (" + pct + "%)").withStyle(ChatFormatting.GRAY));
                    }
                    ctx.getSource().sendSuccess(() -> out, false);
                    return 1;
                }))));
    }
    private SkillCommand() {}
}
