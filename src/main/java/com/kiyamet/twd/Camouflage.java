package com.kiyamet.twd;

import com.kiyamet.Kiyamet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Walker-blood camouflage: zombies ignore you until you attack them, get wet, or it wears off. */
public final class Camouflage {
    public static class Effect extends MobEffect {
        public Effect() { super(MobEffectCategory.BENEFICIAL, 0x5a2a1a); }
    }

    public static final Holder<MobEffect> EFFECT = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Kiyamet.id("walker_camouflage"), new Effect());

    public static boolean hides(LivingEntity target, Mob zombie) {
        return target.hasEffect(EFFECT) && zombie.getLastHurtByMob() != target;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.hasEffect(EFFECT) && p.isInWaterOrRain()) {
                    p.removeEffect(EFFECT);
                    p.sendSystemMessage(Component.literal("The water washed the walker blood off you!").withStyle(ChatFormatting.RED));
                }
            }
        });
    }

    private Camouflage() {}
}
