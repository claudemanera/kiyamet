package com.kiyamet.water;

import com.kiyamet.Kiyamet;
import com.kiyamet.season.SeasonEvents;
import com.kiyamet.season.Seasons;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * 1.9 thirst: a water bar (0-20) next to hunger. Summer, heat waves, heat and sprinting dry you out faster; winter slower.
 * Drink from canteens, water bottles, milk, soups and juicy fruit. At zero you start to die of thirst.
 */
public final class Thirst {
    public static final AttachmentType<Float> WATER = AttachmentRegistry.create(Kiyamet.id("thirst"),
            b -> b.initializer(() -> 20f).persistent(Codec.FLOAT).syncWith(ByteBufCodecs.FLOAT, AttachmentSyncPredicate.targetOnly()));

    public static float get(ServerPlayer p) { return p.getAttachedOrElse(WATER, 20f); }
    public static void add(ServerPlayer p, float v) { p.setAttached(WATER, Math.max(0, Math.min(20, get(p) + v))); }

    /** how much water a drink / food gives back */
    static float drinkValue(ItemStack s) {
        if (s.is(Items.POTION)) { PotionContents pc = s.get(DataComponents.POTION_CONTENTS); return pc != null && pc.is(Potions.WATER) ? 6 : 3; }
        if (s.is(Items.MILK_BUCKET)) return 8;
        if (s.is(Items.HONEY_BOTTLE)) return 2;
        if (s.is(Items.MELON_SLICE)) return 3;
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return switch (id) {
            case "orange", "peach", "cucumber", "tomato", "strawberry", "apple", "sweet_berries", "glow_berries" -> 2;
            case "mushroom_stew", "beetroot_soup", "rabbit_stew", "vegetable_stew", "tomato_soup", "chicken_soup", "fruit_salad", "cabbage_salad" -> 5;
            default -> 0;
        };
    }

    static float drain(ServerPlayer p) {
        Seasons.Season s = Seasons.season(p.level());
        float d = 0.011f;
        d *= switch (s) { case SUMMER -> 1.6f; case SPRING -> 1.0f; case AUTUMN -> 0.9f; case WINTER -> 0.75f; };
        if (SeasonEvents.today == SeasonEvents.Event.HEAT_WAVE) d *= 1.6f;
        if (SeasonEvents.today == SeasonEvents.Event.DROUGHT) d *= 1.3f;
        int state = Seasons.state(p.getAttachedOrElse(Seasons.BODY, 20f));
        if (state >= 2) d *= state == 2 ? 1.3f : state == 3 ? 1.8f : 2.6f;
        if (p.isSprinting()) d *= 1.8f;
        if (p.level().getBiome(p.blockPosition()).value().getBaseTemperature() > 1.5f) d *= 1.3f;
        if (p.level().dimension() == net.minecraft.world.level.Level.NETHER) d *= 2.5f;
        d *= 1f - Math.min(0.5f, com.kiyamet.skill.SkillManager.get(p).level(com.kiyamet.skill.Skill.SURVIVAL) / 2000f);   // survivors need less
        return d;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTickCount();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
                // drinking anything that finishes this tick
                if (p.isUsingItem() && p.getUseItemRemainingTicks() == 1) {
                    float v = drinkValue(p.getUseItem());
                    if (v > 0) { add(p, v); com.kiyamet.skill.SkillManager.addXp(p, com.kiyamet.skill.Skill.SURVIVAL, 2); }
                }
                if (t % 20 != 0) continue;
                if (t % 600 == 0) CanteenItem.seasonTick(p);
                add(p, -drain(p));
                float w = get(p);
                if (w <= 6) p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, false, true));
                if (w <= 3) { p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true)); if (t % 200 == 0) p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0, true, false, true)); }
                if (w <= 0 && t % 80 == 0) p.hurtServer(p.level(), p.damageSources().dryOut(), 1.5f);
                if ((w <= 6 && t % 1200 == 0) || (w <= 3 && t % 400 == 0))
                    p.displayClientMessage(Component.literal(w <= 3 ? "You're dying of thirst - drink something!" : "You're thirsty.").withStyle(w <= 3 ? ChatFormatting.RED : ChatFormatting.AQUA), true);
            }
        });
        for (String[] c : CanteenItem.TIERS) { int n = Integer.parseInt(c[2]); com.kiyamet.registry.ModItems.custom(c[0], p -> new CanteenItem(p.stacksTo(1), n)); }
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> newP.setAttached(WATER, 20f));
    }

    private Thirst() {}
}
