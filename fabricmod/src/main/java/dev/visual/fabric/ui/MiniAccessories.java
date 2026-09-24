package dev.visual.fabric.ui;

import dev.visual.fabric.MiniAnimation;
import dev.visual.fabric.MiniMe;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

/** Accessories have their own model and pose, including on deferred renderers. */
public final class MiniAccessories extends EntityModel<MiniAccessories.Pose> {
    public static final Identifier TEXTURE = Identifier.of("visualsfabric", "textures/cosmetic_palette.png");
    public static final class Pose extends LivingEntityRenderState {
        int hat, scarf, shoes;
        boolean seated, wings;
        float step, movement;
    }
    public final Pose pose = new Pose();
    private final ModelPart head, rightLeg, leftLeg, body;
    public MiniAccessories() {
        super(shape());
        head=getRootPart().getChild("head"); body=getRootPart().getChild("body");
        rightLeg=getRootPart().getChild("rightLeg"); leftLeg=getRootPart().getChild("leftLeg");
    }
    public void capture(LivingEntityRenderState source) {
        var c=MiniMe.config();
        pose.age=source.age; pose.hat=c.miniMeHut; pose.scarf=c.miniMeScarf;
        pose.shoes=c.miniMeShoes; pose.wings=c.miniMeFluegel;
        pose.seated=MiniMe.sitzt(); pose.step=MiniMe.schritt(); pose.movement=MiniMe.bewegung();
    }
    private static ModelPartBuilder color(int i) { return ModelPartBuilder.create().uv(i%4*64,i/4*64); }
    private static ModelPart shape() {
        ModelData data=new ModelData(); var root=data.getRoot();
        var head=root.addChild("head",ModelPartBuilder.create(),ModelTransform.NONE);
        for(int i=1;i<=9;i++) {
            int color=switch(i){case 1->4;case 2->7;case 3->14;case 4->15;case 5->2;case 6->5;case 7->9;default->2;};
            var h=head.addChild("hat"+i, color(color).cuboid(-4.4f,-9,-4.4f,8.8f,1.5f,8.8f),ModelTransform.NONE);
            if(i==7) h.addChild("pom",color(1).cuboid(-1.5f,-12,-1.5f,3,3,3),ModelTransform.NONE);
            if(i==5) for(int x=-3;x<=3;x+=3) h.addChild("flower"+x,color(1).cuboid(x-1,-9.5f,-4.8f,2,2,1),ModelTransform.NONE);
            if(i==6||i==8||i==9) for(int side:new int[]{-1,1}) h.addChild("ear"+side,
                    color(color).cuboid(side*3-1,-(i==9?15:12),-1.5f,2,i==9?7:4,3),ModelTransform.NONE);
            if(i==4) h.addChild("icing",color(2).cuboid(-3.5f,-10,-3.5f,7,1,7),ModelTransform.NONE);
        }
        var body=root.addChild("body",ModelPartBuilder.create(),ModelTransform.NONE);
        for(int i=1;i<=3;i++) {
            int col=i==1?2:i==2?9:7;
            body.addChild("scarf"+i,color(col).cuboid(-4.5f,-.4f,-2.5f,9,2.5f,5)
                    .cuboid(1,2,-2.7f,2.3f,6,1),ModelTransform.NONE);
        }
        for(int side:new int[]{-1,1}) {
            var leg=root.addChild(side==-1?"rightLeg":"leftLeg",ModelPartBuilder.create(),ModelTransform.of(side*1.9f,12,0,0,0,0));
            for(int i=1;i<=3;i++) {
                var boot=leg.addChild("shoe"+i,color(i==1?2:i==2?9:1).cuboid(-2.2f,8.5f,-2.8f,4.4f,3.7f,5),ModelTransform.NONE);
                if(i==3) boot.addChild("ears",color(2).cuboid(-1.5f,6.5f,-2.2f,1,3,1).cuboid(.5f,6.5f,-2.2f,1,3,1),ModelTransform.NONE);
            }
            body.addChild("wing"+side,color(6).cuboid(side<0?-10:0,-3,0,10,13,1),ModelTransform.of(side*2,3,2.8f,0,0,0));
        }
        return TexturedModelData.of(data,256,256).createModel();
    }
    @Override public void setAngles(Pose s) {
        super.setAngles(s);
        head.pitch=MiniAnimation.headPitch(s.age)+(s.seated?.14f:0);
        head.yaw=MiniAnimation.headYaw(s.age); head.roll=MiniAnimation.headRoll(s.age);
        for(int i=1;i<=9;i++) head.getChild("hat"+i).visible=s.hat==i;
        for(int i=1;i<=3;i++) {
            body.getChild("scarf"+i).visible=s.scarf==i;
            rightLeg.getChild("shoe"+i).visible=s.shoes==i;
            leftLeg.getChild("shoe"+i).visible=s.shoes==i;
        }
        rightLeg.pitch=s.seated?-1.55f+MiniAnimation.legSwing(s.age):(float)Math.cos(s.step*.6662f)*1.4f*s.movement;
        leftLeg.pitch=s.seated?-1.55f-MiniAnimation.legSwing(s.age):(float)Math.cos(s.step*.6662f+Math.PI)*1.4f*s.movement;
        rightLeg.yaw=s.seated?.12f:0;leftLeg.yaw=s.seated?-.12f:0;
        for(int side:new int[]{-1,1}) {
            var wing=body.getChild("wing"+side);wing.visible=s.wings;
            wing.yaw=side*(.35f+(float)Math.sin(s.age*.22f)*.25f);
        }
    }
}
