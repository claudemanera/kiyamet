package com.kiyamet.boss;

import com.kiyamet.Kiyamet;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Big XP on each player's first kill of a boss, small XP afterwards. */
public final class BossRewards {
    public static final AttachmentType<List<String>> KILLED = AttachmentRegistry.create(Kiyamet.id("bosses_killed"),
            b -> b.initializer(List::of).persistent(Codec.STRING.listOf()).copyOnDeath());

    private static final Skill[] COMBAT = {Skill.SWORDS, Skill.ARCHERY, Skill.DEFENSE, Skill.HEAVY_WEAPONS, Skill.SURVIVAL};

    public static void onBossKilled(ServerLevel level, BossEntity boss) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(48))) {
            List<String> done = p.getAttachedOrElse(KILLED, List.of());
            boolean first = !done.contains(boss.kind.id);
            if (first) {
                List<String> next = new ArrayList<>(done);
                next.add(boss.kind.id);
                p.setAttached(KILLED, List.copyOf(next));
                for (Skill s : COMBAT) SkillManager.addXp(p, s, boss.kind.firstKillXp() / (double) COMBAT.length);
                p.sendSystemMessage(Component.literal("BOSS DEFEATED: " + boss.kind.title + " (first kill! " + next.size() + "/15)")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            } else {
                for (Skill s : COMBAT) SkillManager.addXp(p, s, 150);
                p.sendSystemMessage(Component.literal("Boss defeated: " + boss.kind.title).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    public static void init() {}

    private BossRewards() {}
}
