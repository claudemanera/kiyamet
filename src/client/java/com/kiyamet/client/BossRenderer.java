package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import com.kiyamet.boss.BossEntity;
import com.kiyamet.boss.BossKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class BossRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<BossEntity, R> {
    public BossRenderer(EntityRendererProvider.Context ctx, BossKind kind) {
        super(ctx, new DefaultedEntityGeoModel<>(Kiyamet.id(kind.id)));
        withScale(kind.scale);
        this.shadowRadius = 0.7f * kind.scale;
    }
}
