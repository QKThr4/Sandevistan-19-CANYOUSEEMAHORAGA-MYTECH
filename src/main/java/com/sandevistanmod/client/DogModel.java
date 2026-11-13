package com.sandevistanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sandevistanmod.robot.RoboDog;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Modelo do Cao Robo: corpo de chapas, cabeca com focinho e antenas, quatro pernas articuladas e rabo-antena. Textura 64x64. */
public class DogModel extends EntityModel<RoboDog> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart legFr;
    private final ModelPart legFl;
    private final ModelPart legBr;
    private final ModelPart legBl;

    public DogModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.legFr = root.getChild("leg_fr");
        this.legFl = root.getChild("leg_fl");
        this.legBr = root.getChild("leg_br");
        this.legBl = root.getChild("leg_bl");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3f, -3f, -6f, 6f, 6f, 12f), PartPose.offset(0f, 15f, 0f));
        PartDefinition h = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20).addBox(-2.5f, -3f, -5f, 5f, 5f, 5f)
                .texOffs(24, 20).addBox(-1.5f, 0f, -8f, 3f, 2f, 3f), PartPose.offset(0f, 13f, -6f));
        h.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(40, 20).addBox(-2.5f, -6f, -2f, 1f, 3f, 1f), PartPose.ZERO);
        h.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(40, 20).mirror().addBox(1.5f, -6f, -2f, 1f, 3f, 1f), PartPose.ZERO);
        r.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 32).addBox(-0.5f, -0.5f, 0f, 1f, 1f, 5f), PartPose.offsetAndRotation(0f, 13f, 6f, -0.8f, 0f, 0f));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 32).addBox(-1f, 0f, -1f, 2f, 6f, 2f);
        r.addOrReplaceChild("leg_fr", leg, PartPose.offset(-2f, 18f, -4.5f));
        r.addOrReplaceChild("leg_fl", leg, PartPose.offset(2f, 18f, -4.5f));
        r.addOrReplaceChild("leg_br", leg, PartPose.offset(-2f, 18f, 4.5f));
        r.addOrReplaceChild("leg_bl", leg, PartPose.offset(2f, 18f, 4.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(RoboDog e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * ((float) Math.PI / 180f);
        head.xRot = headPitch * ((float) Math.PI / 180f);
        float a = Mth.cos(limbSwing * 0.6662f) * 1.3f * limbSwingAmount;
        float b = Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.3f * limbSwingAmount;
        legFr.xRot = a;
        legBl.xRot = a;
        legFl.xRot = b;
        legBr.xRot = b;
        tail.yRot = Mth.cos(ageInTicks * 0.25f) * 0.35f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
