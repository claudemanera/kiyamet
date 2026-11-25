package com.kiyamet.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Arrays;
import java.util.Optional;

/** A giant city assembled from a grid of 64x64 tile templates: cities/&lt;layout&gt;/t_i_j. */
public class TiledCityStructure extends Structure {
    public static final MapCodec<TiledCityStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("layout").forGetter(s -> s.layout),
            Codec.INT.fieldOf("cols").forGetter(s -> s.cols),
            Codec.INT.fieldOf("rows").forGetter(s -> s.rows)
    ).apply(i, TiledCityStructure::new));

    public static final int TILE = 64;
    private final String layout;
    private final int cols, rows;

    public TiledCityStructure(StructureSettings settings, String layout, int cols, int rows) {
        super(settings);
        this.layout = layout;
        this.cols = cols;
        this.rows = rows;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int w = cols * TILE, d = rows * TILE;
        // centre the city on the start chunk so every tile stays within reference range
        int x0 = ctx.chunkPos().getMinBlockX() - w / 2;
        int z0 = ctx.chunkPos().getMinBlockZ() - d / 2;
        int[] hs = new int[9];
        int k = 0;
        for (int a = 0; a < 3; a++)
            for (int b = 0; b < 3; b++)
                hs[k++] = ctx.chunkGenerator().getFirstOccupiedHeight(x0 + a * (w - 1) / 2, z0 + b * (d - 1) / 2,
                        Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        Arrays.sort(hs);
        if (hs[8] - hs[0] > 40) return Optional.empty();       // too rugged for a city
        int y = hs[4];
        if (y < ctx.chunkGenerator().getSeaLevel()) return Optional.empty();
        BlockPos origin = new BlockPos(x0, y, z0);
        String[] p = layout.split(":", 2);
        String ns = p.length == 2 ? p[0] : "kiyamet", city = p.length == 2 ? p[1] : p[0];
        return Optional.of(new GenerationStub(origin, (StructurePiecesBuilder b) -> {
            for (int i = 0; i < cols; i++)
                for (int j = 0; j < rows; j++) {
                    Identifier id = Identifier.fromNamespaceAndPath(ns, "cities/" + city + "/t_" + i + "_" + j);
                    if (ctx.structureTemplateManager().get(id).isEmpty()) continue;
                    b.addPiece(new CityTilePiece(ctx.structureTemplateManager(), id, origin.offset(i * TILE, 0, j * TILE)));
                }
        }));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TILED_CITY;
    }
}
