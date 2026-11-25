package com.kiyamet.client.human;

import com.kiyamet.npc.HumanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;

public class HumanRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<HumanEntity, R> {
    public HumanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanModel());
        withRenderLayer(new ItemInHandGeoLayer<>(this, "item_l", "item_r"));
        this.shadowRadius = 0.5f;
    }
}
