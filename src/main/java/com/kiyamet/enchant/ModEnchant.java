package com.kiyamet.enchant;

import com.kiyamet.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;

public final class ModEnchant {
    public static final Item GUN_TOME = ModItems.custom("gun_tome", p -> new GunTomeItem(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer sp) || !player.isShiftKeyDown() || !level.getBlockState(hit.getBlockPos()).is(Blocks.ENCHANTING_TABLE)) return InteractionResult.PASS;
            if (player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
            ForgeMenu.open(sp);
            return InteractionResult.SUCCESS;
        });
    }

    private ModEnchant() {}
}
