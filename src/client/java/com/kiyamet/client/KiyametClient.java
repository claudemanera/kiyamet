package com.kiyamet.client;

import com.kiyamet.registry.ModBlocks;
import com.kiyamet.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class KiyametClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, ModBlocks.STEEL_GRATE, ModBlocks.STEEL_GATE, ModBlocks.BARBED_WIRE,
                ModBlocks.RAZOR_WIRE, ModBlocks.ELECTRIC_FENCE, ModBlocks.SPIKE_TRAP);
        BlockRenderLayerMap.putBlock(ModBlocks.ENERGY_WALL, ChunkSectionLayer.TRANSLUCENT);
        com.kiyamet.farm.ModFarm.CUTOUT.forEach(b -> BlockRenderLayerMap.putBlock(b, ChunkSectionLayer.CUTOUT));
        ModEntities.ZOMBIES.forEach((kind, type) -> EntityRendererRegistry.register(type, ctx -> new KZombieRenderer<>(ctx, kind)));
        com.kiyamet.boss.ModBosses.TYPES.forEach((kind, type) -> EntityRendererRegistry.register(type, ctx -> new BossRenderer<>(ctx, kind)));
        com.kiyamet.vehicle.ModVehicles.TYPES.forEach((k, t) -> EntityRendererRegistry.register(t, ctx -> new SimpleGeoRenderer<>(ctx, k.id, 1.0f)));
        EntityRendererRegistry.register(com.kiyamet.twd.ModTwd.SHELTER_CORE, ctx -> new SimpleGeoRenderer<>(ctx, "shelter_core", 1.0f));
        EntityRendererRegistry.register(com.kiyamet.twd.ModTwd.MISSION_BOARD, ctx -> new SimpleGeoRenderer<>(ctx, "mission_board", 1.0f));
        EntityRendererRegistry.register(com.kiyamet.ally.ModAllies.TURRET, ctx -> new SimpleGeoRenderer<>(ctx, "auto_turret", 1.0f));
        EntityRendererRegistry.register(com.kiyamet.item.BombProjectile.TYPE, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        ModKeys.init();
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> ar)
                helper.register(new BackpackLayer((net.minecraft.client.renderer.entity.RenderLayerParent<net.minecraft.client.renderer.entity.state.AvatarRenderState, net.minecraft.client.model.player.PlayerModel>) (Object) ar));
        });
        EntityRendererRegistry.register(com.kiyamet.npc.ModHumans.HUMAN, ctx -> new com.kiyamet.client.human.HumanRenderer<>(ctx));
        com.kiyamet.robot.ModRobots.TYPES.forEach((kind, type) -> EntityRendererRegistry.register(type, ctx -> new RobotRenderer<>(ctx, kind)));
    }
}
