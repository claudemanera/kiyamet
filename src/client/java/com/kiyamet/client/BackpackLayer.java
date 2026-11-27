package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import com.kiyamet.backpack.BackpackTier;
import com.kiyamet.backpack.Backpacks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Draws the worn backpack on the player's back (each of the 10 backpacks has its own colours). */
public class BackpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final ModelPart PACK;
    private static final Identifier[] TEX = new Identifier[BackpackTier.values().length];
    /** 2.2: a baby wrapped in white, riding beside the backpack (or on the back when there is none) */
    private static final ModelPart BABY_SIDE, BABY_BACK;
    private static final Identifier BABY_TEX = Kiyamet.id("textures/entity/baby_bundle.png");

    static {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("pack", CubeListBuilder.create().texOffs(0, 0).addBox(-4f, 1f, 2f, 8, 10, 4), PartPose.ZERO);
        PACK = LayerDefinition.create(mesh, 32, 32).bakeRoot().getChild("pack");
        MeshDefinition bm = new MeshDefinition();
        bm.getRoot().addOrReplaceChild("side", CubeListBuilder.create().texOffs(0, 0).addBox(4.4f, 2f, 3f, 3, 8, 3)
                .texOffs(0, 12).addBox(4.65f, -0.6f, 3.3f, 2.5f, 2.6f, 2.6f).texOffs(12, 12).addBox(4.3f, -1.2f, 2.9f, 3.2f, 2f, 2.7f), PartPose.rotation(0f, 0f, -0.12f));
        bm.getRoot().addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, 3f, 2.2f, 3, 8, 3)
                .texOffs(0, 12).addBox(-1.25f, 0.4f, 2.5f, 2.5f, 2.6f, 2.6f).texOffs(12, 12).addBox(-1.6f, -0.2f, 2.1f, 3.2f, 2f, 2.7f), PartPose.ZERO);
        ModelPart broot = LayerDefinition.create(bm, 32, 32).bakeRoot();
        BABY_SIDE = broot.getChild("side"); BABY_BACK = broot.getChild("back");
        for (BackpackTier t : BackpackTier.values()) TEX[t.ordinal()] = Kiyamet.id("textures/entity/backpack/" + t.id + ".png");
    }

    public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(state.id);
        if (e == null || e.isInvisible()) return;
        int tier = e.getAttachedOrElse(Backpacks.WORN_TIER, -1);
        boolean pack = tier >= 0 && tier < TEX.length;
        boolean baby = e.getAttachedOrElse(com.kiyamet.settle.Family.CARRIED, 0) > 0;
        if (!pack && !baby) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        if (pack) collector.submitModelPart(PACK, pose, RenderTypes.entityCutout(TEX[tier]), light, OverlayTexture.NO_OVERLAY, null);
        if (baby) collector.submitModelPart(pack ? BABY_SIDE : BABY_BACK, pose, RenderTypes.entityCutout(BABY_TEX), light, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
    }
}
