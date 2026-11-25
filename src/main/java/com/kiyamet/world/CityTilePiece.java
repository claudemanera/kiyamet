package com.kiyamet.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class CityTilePiece extends TemplateStructurePiece {
    public CityTilePiece(StructureTemplateManager manager, Identifier id, BlockPos pos) {
        super(ModStructures.CITY_TILE, 0, manager, id, id.toString(), settings(), pos);
    }

    public CityTilePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.CITY_TILE, tag, ctx.structureTemplateManager(), id -> settings());
    }

    private static StructurePlaceSettings settings() {
        return new StructurePlaceSettings()
                .setIgnoreEntities(false)
                .setFinalizeEntities(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }
}
