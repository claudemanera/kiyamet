package com.kiyamet.net;

import com.kiyamet.npc.HumanEntity;
import com.kiyamet.skill.Powers;
import com.kiyamet.skill.PowersMenu;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.skill.SkillsMenu;
import com.kiyamet.stone.GauntletData;
import com.kiyamet.stone.GauntletItem;
import com.kiyamet.stone.StoneKind;
import com.kiyamet.stone.StonePowers;
import com.kiyamet.ui.Ui;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Server side of the key bindings (Options > Controls > The Walking Dead). */
public final class KeyActions {
    public static void init() {
        PayloadTypeRegistry.playC2S().register(KeyAction.TYPE, KeyAction.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KeyAction.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            ctx.server().execute(() -> handle(p, payload.action()));
        });
    }

    private static ItemStack gauntlet(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            if (p.getInventory().getItem(i).getItem() instanceof GauntletItem) return p.getInventory().getItem(i);
        return ItemStack.EMPTY;
    }

    private static void msg(ServerPlayer p, String s, ChatFormatting c) { p.displayClientMessage(Component.literal(s).withStyle(c), true); }

    static void handle(ServerPlayer p, int action) {
        if (!p.isAlive()) return;
        switch (action) {
            case KeyAction.SKILLS -> SkillsMenu.open(p);
            case KeyAction.POWERS -> PowersMenu.open(p);
            case KeyAction.COMPANIONS -> {
                List<HumanEntity> mine = ((ServerLevel) p.level()).getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(128), h -> h.isOwnedBy(p));
                if (mine.isEmpty()) { msg(p, "You have no companions nearby.", ChatFormatting.GRAY); return; }
                boolean anyFollow = mine.stream().anyMatch(h -> h.order() == HumanEntity.Order.FOLLOW);
                for (HumanEntity h : mine) h.setOrder(anyFollow ? HumanEntity.Order.STAY : HumanEntity.Order.FOLLOW);
                msg(p, anyFollow ? "Companions: hold position!" : "Companions: follow me!", ChatFormatting.AQUA);
            }
            case KeyAction.GAUNTLET_USE, KeyAction.GAUNTLET_CYCLE -> {
                ItemStack g = gauntlet(p);
                if (g.isEmpty()) { msg(p, "You don't carry a Stone Gauntlet.", ChatFormatting.GRAY); return; }
                GauntletData d = g.getOrDefault(GauntletData.TYPE, GauntletData.EMPTY);
                if (action == KeyAction.GAUNTLET_CYCLE) {
                    GauntletData c = d.cycle();
                    g.set(GauntletData.TYPE, c);
                    if (c.current() != null) p.displayClientMessage(Component.literal("Stone: ").append(Component.translatable("item.kiyamet." + c.current().id)).withStyle(ChatFormatting.AQUA), true);
                    return;
                }
                StoneKind k = d.current();
                if (k == null) { msg(p, "The gauntlet is empty.", ChatFormatting.GRAY); return; }
                if (p.getCooldowns().isOnCooldown(g)) { msg(p, "Stone power is recharging...", ChatFormatting.GRAY); return; }
                if (StonePowers.activate(p, k)) p.getCooldowns().addCooldown(g, StonePowers.cooldown(p, k));
            }
            case KeyAction.CAMOUFLAGE -> {
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack s = p.getInventory().getItem(i);
                    if (s.is(Ui.item("kiyamet:walker_guts"))) { s.getItem().use(p.level(), p, InteractionHand.MAIN_HAND); return; }
                }
                msg(p, "You have no walker guts.", ChatFormatting.GRAY);
            }
            case KeyAction.NIGHT_VISION -> toggle(p, Powers.Power.NIGHT_VISION);
            case KeyAction.SHADOW_STEP -> toggle(p, Powers.Power.INVISIBLE);
            case KeyAction.SQUAD -> com.kiyamet.npc.SquadMenu.open(p);
            case KeyAction.BACKPACK -> com.kiyamet.backpack.BackpackMenu.openWorn(p);
            default -> {}
        }
    }

    private static void toggle(ServerPlayer p, Powers.Power pw) {
        if (!pw.unlocked(SkillManager.get(p))) { msg(p, pw.title + " is locked (" + pw.skill.name().toLowerCase() + " " + pw.level + ")", ChatFormatting.RED); return; }
        Powers.toggle(p, pw);
        msg(p, pw.title + (Powers.enabled(p, pw) ? ": ON" : ": OFF"), Powers.enabled(p, pw) ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private KeyActions() {}
}
