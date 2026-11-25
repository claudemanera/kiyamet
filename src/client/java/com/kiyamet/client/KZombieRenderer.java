package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import com.kiyamet.entity.KZombie;
import com.kiyamet.entity.ZombieKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class KZombieRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<KZombie, R> {
    public KZombieRenderer(EntityRendererProvider.Context ctx, ZombieKind kind) {
        super(ctx, new DefaultedEntityGeoModel<>(Kiyamet.id(kind.id)));
        withScale(kind.scale);
        this.shadowRadius = 0.5f * kind.scale;
    }
}
