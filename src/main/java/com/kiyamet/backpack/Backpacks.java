package com.kiyamet.backpack;

import com.kiyamet.Kiyamet;
import com.kiyamet.registry.ModItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.EnumMap;
import java.util.Map;

/** Back slot (extra equipment slot just for backpacks) + registration. */
public final class Backpacks {
    public static final Map<BackpackTier, Item> ITEMS = new EnumMap<>(BackpackTier.class);

    /** The backpack you wear (server only, saved with the player). */
    public static final AttachmentType<ItemStack> WORN = AttachmentRegistry.create(Kiyamet.id("worn_backpack"),
            b -> b.initializer(() -> ItemStack.EMPTY).persistent(ItemStack.OPTIONAL_CODEC));
    /** Which backpack to draw on your back (-1 = none), synced to every client. */
    public static final AttachmentType<Integer> WORN_TIER = AttachmentRegistry.create(Kiyamet.id("worn_backpack_tier"),
            b -> b.initializer(() -> -1).persistent(Codec.INT).syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

    public static ItemStack worn(ServerPlayer p) { return p.getAttachedOrElse(WORN, ItemStack.EMPTY); }

    public static void setWorn(ServerPlayer p, ItemStack s) {
        p.setAttached(WORN, s);
        p.setAttached(WORN_TIER, s.getItem() instanceof BackpackItem b ? b.tier.ordinal() : -1);
    }

    public static void init() {
        for (BackpackTier t : BackpackTier.values())
            ITEMS.put(t, ModItems.custom(t.id, p -> new BackpackItem(p.stacksTo(1).rarity(t.ordinal() >= 8 ? Rarity.EPIC : t.ordinal() >= 5 ? Rarity.RARE : Rarity.COMMON), t)));
        // the worn backpack drops where you die (with everything inside)
        ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> {
            if (e instanceof ServerPlayer p && !worn(p).isEmpty()) {
                p.level().addFreshEntity(new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), worn(p)));
                setWorn(p, ItemStack.EMPTY);
            }
        });
    }

    private Backpacks() {}
}
