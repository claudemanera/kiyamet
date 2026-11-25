package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import com.kiyamet.robot.RobotEntity;
import com.kiyamet.robot.RobotKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class RobotRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<RobotEntity, R> {
    public RobotRenderer(EntityRendererProvider.Context ctx, RobotKind kind) {
        super(ctx, new DefaultedEntityGeoModel<>(Kiyamet.id(kind.id)));
        withScale(kind.scale);
        this.shadowRadius = 0.5f * kind.scale;
    }
}
