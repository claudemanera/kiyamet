package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/** Renders any GeckoLib living entity with the model/texture/animation named {@code asset}. */
public class SimpleGeoRenderer<T extends LivingEntity & GeoEntity, R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<T, R> {
    public SimpleGeoRenderer(EntityRendererProvider.Context ctx, String asset, float scale) {
        super(ctx, new DefaultedEntityGeoModel<>(Kiyamet.id(asset)));
        withScale(scale);
        this.shadowRadius = 0.5f * scale;
    }
}
