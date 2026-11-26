package com.kiyamet.food;

import com.kiyamet.registry.ModItems;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/** 1.7: 10 hearty meals from the new crops - they fill you up for a long time and keep for weeks. */
public final class Recipes {
    /** id, title, nutrition, saturation modifier, keeps (days), well-fed minutes */
    public record Meal(String id, String title, int food, float sat, int days, int fedMinutes, boolean bowl) {}

    public static final List<Meal> MEALS = List.of(
            new Meal("vegetable_stew", "Vegetable Stew", 12, 1.2f, 10, 6, true),
            new Meal("tomato_soup", "Tomato Soup", 10, 1.1f, 10, 5, true),
            new Meal("stuffed_peppers", "Stuffed Peppers", 12, 1.3f, 12, 6, false),
            new Meal("ratatouille", "Ratatouille", 14, 1.3f, 12, 8, true),
            new Meal("cabbage_rolls", "Cabbage Rolls", 12, 1.2f, 14, 6, false),
            new Meal("cornbread", "Cornbread", 10, 1.2f, 20, 4, false),
            new Meal("fruit_salad", "Fruit Salad", 9, 1.0f, 7, 4, true),
            new Meal("peach_pie", "Peach Pie", 12, 1.2f, 15, 6, false),
            new Meal("strawberry_jam", "Strawberry Jam", 8, 1.0f, 120, 4, false),
            new Meal("survivor_ration", "Survivor's Ration", 16, 1.5f, 365, 10, false),
            new Meal("hunters_breakfast", "Hunter's Breakfast", 14, 1.3f, 6, 7, false),
            new Meal("fish_and_chips", "Fish and Chips", 12, 1.2f, 5, 5, false),
            new Meal("mushroom_risotto", "Mushroom Risotto", 12, 1.3f, 6, 6, true),
            new Meal("honey_glazed_ham", "Honey Glazed Ham", 14, 1.4f, 9, 8, false),
            new Meal("berry_pancakes", "Berry Pancakes", 10, 1.2f, 6, 5, false));

    public static Meal meal(String id) { for (Meal m : MEALS) if (m.id().equals(id)) return m; return null; }

    public static void init() {
        for (Meal m : MEALS) {
            Consumable c = Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.SATURATION, 30, 0, true, false), new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 0, true, true),
                    new MobEffectInstance(com.kiyamet.food.Recipes.WELL_FED, m.fedMinutes() * 1200, 0, true, true)))).build();
            ModItems.custom(m.id(), p -> {
                var pp = p.food(new FoodProperties(m.food(), m.sat(), false), c).stacksTo(16);
                if (m.bowl()) pp = pp.usingConvertsTo(Items.BOWL);
                return new net.minecraft.world.item.Item(pp);
            });
        }
        ModItems.custom("spoiled_food", p -> new net.minecraft.world.item.Item(p.food(new FoodProperties(1, 0.1f, false),
                Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 1), 0.8f)).build())));
    }

    /** Well Fed: hunger slowly refills while it lasts - you stay full for minutes. */
    public static class WellFed extends net.minecraft.world.effect.MobEffect {
        public WellFed() { super(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xE8A040); }
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amp) { return duration % 160 == 0; }
        @Override public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity e, int amp) {
            if (e instanceof net.minecraft.world.entity.player.Player p) p.getFoodData().eat(1, 0.4f);
            return true;
        }
    }
    public static final net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> WELL_FED =
            net.minecraft.core.Registry.registerForHolder(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT, com.kiyamet.Kiyamet.id("well_fed"), new WellFed());

    private Recipes() {}
}
