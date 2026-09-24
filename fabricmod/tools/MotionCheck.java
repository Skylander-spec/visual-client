import dev.visual.fabric.CompanionMotion;

/** Deterministic terrain regressions, independent of a running Minecraft client. */
public final class MotionCheck {
    static CompanionMotion.Terrain terrain(java.util.function.DoubleBinaryOperator height) {
        return new CompanionMotion.Terrain() {
            public double floor(double x, double z, double near, double up, double down) {
                double h = height.applyAsDouble(x,z);
                return h <= near+up && h >= near-down ? h : Double.NaN;
            }
            public boolean clear(double x,double y,double z) { return y >= height.applyAsDouble(x,z)-.001; }
        };
    }
    static void require(boolean pass, String why) { if (!pass) throw new AssertionError(why); }
    public static void main(String[] args) {
        var flat=terrain((x,z)->0);
        var m=new CompanionMotion();
        m.tick(flat,0,0,0,0,false);
        for(int i=0;i<100;i++) m.tick(flat,0,0,0,180,false);
        require(m.z>1.05,"Turning must move the pet to the new spot");
        var ledge=terrain((x,z)-> z < -.5 ? 1 : 0);
        var l=new CompanionMotion();
        for(int i=0;i<60;i++) l.tick(ledge,0,0,0,0,false);
        require(l.y==1,"Pet must stand on the block above its owner");
        var wall=terrain((x,z)->x>.4 && x<1.6 && z>-.5 && z<.5 ? 3 : 0);
        var w=new CompanionMotion(); w.tick(wall,0,0,1.15f,0,false);
        for(int i=0;i<200;i++) {
            w.tick(wall,3,0,1.15f,0,false);
            require(wall.clear(w.x,w.y,w.z),"Walking must not enter a wall");
        }
        require(w.x>2.8,"Pet must walk around a wall");
        var f=new CompanionMotion(); f.tick(flat,0,0,0,0,true);
        for(int i=0;i<80;i++) f.tick(flat,2,0,0,0,true);
        require(f.y>1 && f.x>1.9,"Flying follows above the ground");
        for(int i=0;i<80;i++) f.tick(flat,2,0,0,0,false);
        require(f.y<.01,"Changing to walking lands on the ground");
        var digging=new CompanionMotion(); digging.tick(flat,0,0,0,0,false);
        var shaft=terrain((x,z)->-10);
        for(int i=0;i<8;i++) digging.tick(shaft,0,-2,0,0,false);
        require(digging.y < -1,"Removed ground must make the pet fall, including deep shafts");
        var unsupported=new CompanionMotion();
        unsupported.tick(terrain((x,z)->-100),0,0,0,0,false);
        require(!unsupported.ready(),"Walking pets must not spawn floating over a deep void");
        System.out.println("PASS: turning, raised ground, obstacle routing, flying, landing, digging and unsupported spawn");
    }
}
