package com.kiyamet.creation;

import com.kiyamet.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.AnvilBlock;

/** TWD 2.2: the Creation Element, the Creation Rod and the Creation Axe, and forging the axe on an anvil. */
public final class CreationItems {
    public static Item CORE, ROD, AXE;

    public static void init() {
        CORE = ModItems.custom("creation_core", p -> new Item(p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
        ROD = ModItems.custom("creation_rod", p -> new Item(p.sword(ToolMaterial.NETHERITE, 6f, -2.4f).fireResistant().rarity(Rarity.EPIC)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
        AXE = ModItems.custom("creation_axe", p -> new CreationAxe(p.axe(ToolMaterial.NETHERITE, 17f, -2.8f).fireResistant().rarity(Rarity.EPIC)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

        // hold the Creation Element, carry the Creation Rod, right-click an anvil
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer p) || !(level instanceof ServerLevel l)) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            if (!(l.getBlockState(pos).getBlock() instanceof AnvilBlock) || !p.getMainHandItem().is(CORE)) return InteractionResult.PASS;
            int rod = -1;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(ROD)) { rod = i; break; }
            if (rod < 0) { p.displayClientMessage(Component.literal("You need the Creation Rod in your inventory.").withStyle(ChatFormatting.RED), true); return InteractionResult.FAIL; }
            p.getInventory().getItem(rod).shrink(1);
            p.getMainHandItem().shrink(1);
            ItemStack axe = new ItemStack(AXE);
            if (!p.getInventory().add(axe)) p.drop(axe, false);
            for (int i = 0; i < 7; i++) l.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 0.5f + i * 0.15f);
            l.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1f, 1.2f);
            l.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1f, 1.4f);
            l.sendParticles(new DustParticleOptions(0xFFFFFF, 2.5f), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 200, 0.8, 1.2, 0.8, 0.1);
            l.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 120, 0.4, 2, 0.4, 0.2);
            l.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0, 0, 0, 0);
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("CREATION").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("All fourteen powers answer to you now").withStyle(ChatFormatting.GRAY)));
            com.kiyamet.skill.SkillManager.addXp(p, com.kiyamet.skill.Skill.ENCHANTING, 20000);
            l.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ " + p.getName().getString() + " forged the CREATION AXE!").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), false);
            return InteractionResult.SUCCESS;
        });
    }

    private CreationItems() {}
}
