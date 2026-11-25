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
        ModEntities.ZOMBIES.forEach((kind, type) -> EntityRendererRegistry.register(type, ctx -> new KZombieRenderer<>(ctx, kind)));
        com.kiyamet.boss.ModBosses.TYPES.forEach((kind, type) -> EntityRendererRegistry.register(type, ctx -> new BossRenderer<>(ctx, kind)));
        EntityRendererRegistry.register(com.kiyamet.ally.ModAllies.TURRET, ctx -> new SimpleGeoRenderer<>(ctx, "scout_drone", 1.0f));
        com.kiyamet.robot.ModRobots.TYPES.forEach((kind, type) -> EntityRendererRegistry.register(type, ctx -> new RobotRenderer<>(ctx, kind)));
    }
}
