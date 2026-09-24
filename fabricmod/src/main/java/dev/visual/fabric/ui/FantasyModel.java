package dev.visual.fabric.ui;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

/** Original cosmetic geometry using Minecraft's model and material renderer. */
public final class FantasyModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart parts;
    private final boolean dragon;
    private final int kind;
    public static final class Pose extends LivingEntityRenderState {
        boolean seated;
        float step, movement;
    }
    public final Pose pose = new Pose();
    public void capture(LivingEntityRenderState source) {
        pose.age = source.age;
        pose.seated = dev.visual.fabric.MiniMe.sitzt();
        pose.step = dev.visual.fabric.MiniMe.schritt();
        pose.movement = dev.visual.fabric.MiniMe.bewegung();
    }
    public final Identifier texture;

    public FantasyModel(int kind) {
        super(kind <= 8 ? shape(kind == 7) : cuteShape(kind));
        this.kind = kind;
        this.dragon = kind == 7;
        this.parts = getRootPart();
        texture = kind >= 9 ? Identifier.of("visualsfabric", "textures/cosmetic_palette.png") : Identifier.of("minecraft", dragon
                ? "textures/block/amethyst_block.png" : "textures/block/sea_lantern.png");
    }

    private static ModelPart shape(boolean dragon) {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        if (dragon) {
            root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-4, -10, -5, 8, 6, 12), ModelTransform.NONE);
            root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4, -16, -10, 8, 7, 7)
                    .cuboid(-3, -12, -14, 6, 3, 4).cuboid(-4, -20, -6, 2, 5, 2)
                    .cuboid(2, -20, -6, 2, 5, 2), ModelTransform.NONE);
            root.addChild("wingLeft", ModelPartBuilder.create().uv(0, 0).cuboid(0, -1, -2, 13, 1, 10), ModelTransform.of(3, -9, 0, 0, 0, 0));
            root.addChild("wingRight", ModelPartBuilder.create().uv(0, 0).cuboid(-13, -1, -2, 13, 1, 10), ModelTransform.of(-3, -9, 0, 0, 0, 0));
            root.addChild("tail", ModelPartBuilder.create().uv(0, 0).cuboid(-1, -1, 0, 2, 2, 12), ModelTransform.of(0, -7, 6, 0, 0, 0));
            for (int side : new int[]{-1, 1}) for (int z : new int[]{-3, 5})
                root.addChild("leg" + side + z, ModelPartBuilder.create().uv(0, 0)
                        .cuboid(side * 3 - 1.5f, -5, z - 1.5f, 3, 5, 3), ModelTransform.NONE);
        } else {
            root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-5, -14, -4, 10, 10, 8)
                    .cuboid(-3, -5, -2, 6, 3, 4).cuboid(-1, -2, -1, 2, 2, 2), ModelTransform.NONE);
            root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-5, -20, -4, 10, 8, 8)
                    .cuboid(-5, -25, -1, 2, 6, 2).cuboid(3, -25, -1, 2, 6, 2), ModelTransform.NONE);
            root.addChild("wingLeft", ModelPartBuilder.create().uv(0, 0).cuboid(0, -1, -1, 7, 2, 2), ModelTransform.of(4, -10, 0, 0, 0, 0));
            root.addChild("wingRight", ModelPartBuilder.create().uv(0, 0).cuboid(-7, -1, -1, 7, 2, 2), ModelTransform.of(-4, -10, 0, 0, 0, 0));
            root.addChild("tail", ModelPartBuilder.create(), ModelTransform.NONE);
        }
        return TexturedModelData.of(data, 16, 16).createModel();
    }

    private static ModelPartBuilder color(int index) {
        return ModelPartBuilder.create().uv((index % 4) * 64, (index / 4) * 64);
    }

    private static ModelPart cuteShape(int kind) {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        int fur = kind == 10 ? 3 : kind == 11 ? 0 : 1;
        int limbs = kind == 9 || kind == 10 || kind == 11 ? 0 : 2;
        root.addChild("body", color(fur).cuboid(-5,-12,-3.5f,10,10,7), ModelTransform.NONE);
        root.addChild("belly", color(1).cuboid(-3.5f,-10,-3.7f,7,7,1), ModelTransform.NONE);
        ModelPartData head = root.addChild("head", color(fur).cuboid(-5,-7,-5,10,8,8),
                ModelTransform.of(0,-13,-.5f,0,0,0));
        head.addChild("muzzle", color(1).cuboid(-3,-3,-5.4f,6,3,1), ModelTransform.NONE);
        head.addChild("nose", color(kind == 11 ? 4 : 0).cuboid(-1,-2.5f,-6,2,1.4f,1), ModelTransform.NONE);
        ModelPartBuilder eyes = color(0), blink = color(0);
        for (int side : new int[]{-1,1}) {
            if (kind == 9) head.addChild("patch"+side, color(0).cuboid(side*2.7f-1.6f,-5.5f,-5.15f,3.2f,3.5f,.3f), ModelTransform.NONE);
            float ex = side*2.7f;
            eyes.cuboid(ex-.7f,-4.6f,-5.55f,1.4f,1.7f,.4f);
            blink.cuboid(ex-1,-3.7f,-5.6f,2,.35f,.4f);
            head.addChild("shine"+side, color(1).cuboid(ex-.5f,-4.5f,-5.8f,.45f,.65f,.3f), ModelTransform.NONE);
            head.addChild("cheek"+side, color(2).cuboid(side*3.5f-.6f,-2.8f,-5.2f,1.2f,.6f,.3f), ModelTransform.NONE);
            head.addChild("ear"+side, color(limbs).cuboid(side*3.8f-1.5f,kind==12?-14:-10,-1,3,kind==12?8:4,2.5f), ModelTransform.NONE);
            root.addChild("leg"+side, color(limbs).cuboid(-2,0,-2.5f,4,4,5), ModelTransform.of(side*2.8f,-4,0,0,0,0));
            root.addChild("arm"+side, color(limbs).cuboid(-1.5f,-1,-1.5f,3,6,3), ModelTransform.of(side*5,-10,0,0,0,side*-.15f));
        }
        head.addChild("eyes", eyes, ModelTransform.NONE);
        head.addChild("blink", blink, ModelTransform.NONE);
        root.addChild("tail", color(fur).cuboid(-2,-2,0,4,4,kind==10?9:3), ModelTransform.of(0,-4,3,0,0,0));
        return TexturedModelData.of(data,256,256).createModel();
    }

    @Override public void setAngles(LivingEntityRenderState state) {
        super.setAngles(state);
        if (kind >= 9) {
            ModelPart head = parts.getChild("head");
            head.pitch = dev.visual.fabric.MiniAnimation.headPitch(state.age);
            head.yaw = dev.visual.fabric.MiniAnimation.headYaw(state.age);
            head.roll = dev.visual.fabric.MiniAnimation.headRoll(state.age);
            float blink = state.age % 83;
            boolean open = blink > 4;
            head.getChild("eyes").visible = open;
            head.getChild("blink").visible = !open;
            head.getChild("shine-1").visible = open;
            head.getChild("shine1").visible = open;
            float walking = (float) Math.sin(pose.step * .6662f) * pose.movement;
            for (int side : new int[]{-1, 1}) {
                parts.getChild("leg" + side).pitch = pose.seated ? -1.35f + (float) Math.sin(state.age * .12) * .08f * side : walking * side;
                parts.getChild("arm" + side).pitch = -walking * side;
            }
            parts.getChild("arm-1").roll = dev.visual.fabric.MiniAnimation.greeting(state.age) * -1.8f;
            parts.getChild("tail").yaw = (float) Math.sin(state.age * .1) * .25f;
            return;
        }
        float flap = (float) Math.sin(state.age * (dragon ? 0.22f : 0.08f));
        parts.getChild("wingLeft").roll = -0.25f + flap * 0.35f;
        parts.getChild("wingRight").roll = 0.25f - flap * 0.35f;
        parts.getChild("tail").yaw = (float) Math.sin(state.age * 0.1f) * 0.22f;
        parts.getChild("head").pitch = dev.visual.fabric.MiniAnimation.headPitch(state.age);
        parts.getChild("head").roll = dev.visual.fabric.MiniAnimation.headRoll(state.age);
        parts.getChild("head").yaw = dev.visual.fabric.MiniAnimation.headYaw(state.age);
        if (!dragon) {
            float wave = dev.visual.fabric.MiniAnimation.greeting(state.age);
            parts.getChild("wingRight").roll += wave * (1.0f + (float) Math.sin(state.age * 0.6f) * 0.25f);
        }
    }
}
