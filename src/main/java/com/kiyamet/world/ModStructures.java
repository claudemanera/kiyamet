package com.kiyamet.world;

import com.kiyamet.Kiyamet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class ModStructures {
    public static final StructureType<TiledCityStructure> TILED_CITY =
            Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Kiyamet.id("tiled_city"), () -> TiledCityStructure.CODEC);
    public static final StructurePieceType CITY_TILE =
            Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Kiyamet.id("city_tile"), (StructurePieceType) CityTilePiece::new);

    public static final TagKey<Structure> SANCTUARY = TagKey.create(Registries.STRUCTURE, Kiyamet.id("sanctuary"));

    /** True inside mosques, churches and the peaceful camps/villages — zombies never spawn there. */
    public static boolean inSanctuary(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, SANCTUARY).isValid();
    }

    public static void init() {
    }

    private ModStructures() {
    }
}
