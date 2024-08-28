package me.sirreal.carpetdragonutils.utils;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class EnderDragon extends AbstractEntity{
    public final double[][] segmentCircularBuffer = new double[64][3];
    public int latestSegment = -1;
    public final EnderDragonBodyPart[] parts;
    public final EnderDragonBodyPart head;
    public boolean slowedDownByBlock;
    public float yawAcceleration;
    public float maxYAcceleration = 0.6F;
    public ServerPlayerEntity target;
    public Vec3d pathTarget;
    public int seenTargetTimes = 0;

    public EnderDragon(EnderDragonEntity originalDragon, ServerPlayerEntity player) {
        super(originalDragon.getPos(), originalDragon.getPitch(),  originalDragon.getYaw(), originalDragon.getVelocity());
//        this.copyFrom(originalDragon);

//        Mojunk redundant code never used
//        int k = player.getBlockX();
//        int l = player.getBlockZ();
//        double d0 = (double)k - originalDragon.getX();
//        double d1 = (double)l - originalDragon.getZ();
//        double d2 = Math.sqrt(d0 * d0 + d1 * d1);
//        double d3 = Math.min((double)0.4F + d2 / 80.0D - 1.0D, 10.0D);
//        int i1 = MathHelper.floor(player.getY() + d3);
//        this.pathTarget = new Vec3d(k, i1, l);

        this.yawAcceleration = originalDragon.yawAcceleration;
        this.setHeadYaw(originalDragon.getHeadYaw());
        this.setBodyYaw(originalDragon.getBodyYaw());
        this.head = new EnderDragonBodyPart(originalDragon, this, "head", 1.0F, 1.0F);
        this.parts = new EnderDragonBodyPart[]{this.head};
        this.target = player;
    }
    public double[] getSegmentProperties(int segmentNumber, float tickDelta) {
        tickDelta = 1.0F - tickDelta;
        int i = this.latestSegment - segmentNumber & 63;
        int j = this.latestSegment - segmentNumber - 1 & 63;
        double[] ds = new double[3];
        double d = this.segmentCircularBuffer[i][0];
        double e = MathHelper.wrapDegrees(this.segmentCircularBuffer[j][0] - d);
        ds[0] = d + e * (double)tickDelta;
        d = this.segmentCircularBuffer[i][1];
        e = this.segmentCircularBuffer[j][1] - d;
        ds[1] = d + e * (double)tickDelta;
        ds[2] = MathHelper.lerp((double)tickDelta, this.segmentCircularBuffer[i][2], this.segmentCircularBuffer[j][2]);
        return ds;
    }
    public int getMaxHeadRotation() {
        return 75;
    }

    public void tickMovement() {
        float g;

        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();

        Vec3d vec3d = this.getVelocity();
        g = 0.2F / ((float)vec3d.horizontalLength() * 10.0F + 1.0F);
        g *= (float)Math.pow(2.0, vec3d.y);

        this.setYaw(MathHelper.wrapDegrees(this.getYaw()));

        if (this.latestSegment < 0) {
            for(int i = 0; i < this.segmentCircularBuffer.length; ++i) {
                this.segmentCircularBuffer[i][0] = (double)this.getYaw();
                this.segmentCircularBuffer[i][1] = this.getY();
            }
        }

        if (++this.latestSegment == this.segmentCircularBuffer.length) {
            this.latestSegment = 0;
        }

        this.segmentCircularBuffer[this.latestSegment][0] = (double)this.getYaw();
        this.segmentCircularBuffer[this.latestSegment][1] = this.getY();
        float n;
        float o;
        float p;

        phaseTick();

        Vec3d vec3d2 = this.getPathTarget();
        if (vec3d2 != null) {
            double d = vec3d2.x - this.getX();
            double e = vec3d2.y - this.getY();
            double j = vec3d2.z - this.getZ();
            double k = d * d + e * e + j * j;
            float l = this.getMaxYAcceleration();
            double m = Math.sqrt(d * d + j * j);
            if (m > 0.0) {
                e = MathHelper.clamp(e / m, (double)(-l), (double)l);
            }

            this.setVelocity(this.getVelocity().add(0.0, e * 0.01, 0.0));
            this.setYaw(MathHelper.wrapDegrees(this.getYaw()));
            Vec3d vec3d3 = vec3d2.subtract(this.getX(), this.getY(), this.getZ()).normalize();
            Vec3d vec3d4 = (new Vec3d((double)MathHelper.sin(this.getYaw() * ((float)Math.PI / 180F)), this.getVelocity().y, (double)(-MathHelper.cos(this.getYaw() * ((float)Math.PI / 180F))))).normalize();
            n = Math.max(((float)vec3d4.dotProduct(vec3d3) + 0.5F) / 1.5F, 0.0F);
            if (Math.abs(d) > (double)1.0E-5F || Math.abs(j) > (double)1.0E-5F) {
                o = MathHelper.clamp(MathHelper.wrapDegrees(180.0F - (float)MathHelper.atan2(d, j) * (180F / (float)Math.PI) - this.getYaw()), -50.0F, 50.0F);
                this.yawAcceleration *= 0.8F;
                this.yawAcceleration += o * this.getDefaultYawAcceleration();
                this.setYaw(this.getYaw() + this.yawAcceleration * 0.1F);
            }

            o = (float)(2.0 / (k + 1.0));
            p = 0.06F;
            this.updateVelocity(0.06F * (n * o + (1.0F - o)), new Vec3d(0.0, 0.0, -1.0));

            this.move(MovementType.SELF, this.getVelocity());
//            if (this.slowedDownByBlock) {
//                this.move(MovementType.SELF, this.getVelocity().multiply(0.800000011920929));
//            } else {
//                this.move(MovementType.SELF, this.getVelocity());
//            }

            Vec3d vec3d5 = this.getVelocity().normalize();
            double q = 0.8 + 0.15 * (vec3d5.dotProduct(vec3d4) + 1.0) / 2.0;
            this.setVelocity(this.getVelocity().multiply(q, 0.91, q));
        }


        this.setBodyYaw(this.getYaw());
        Vec3d[] vec3ds = new Vec3d[this.parts.length];

        for(int r = 0; r < this.parts.length; ++r) {
            vec3ds[r] = new Vec3d(this.parts[r].getX(), this.parts[r].getY(), this.parts[r].getZ());
        }

        float s = (float)(this.getSegmentProperties(5, 1.0F)[1] - this.getSegmentProperties(10, 1.0F)[1]) * 10.0F * ((float)Math.PI / 180F);
        float t = MathHelper.cos(s);
        float u = MathHelper.sin(s);
        float v = this.getYaw() * ((float)Math.PI / 180F);
        float w = MathHelper.sin(v);
        float x = MathHelper.cos(v);

        float y = MathHelper.sin(this.getYaw() * ((float)Math.PI / 180F) - this.yawAcceleration * 0.01F);
        float z = MathHelper.cos(this.getYaw() * ((float)Math.PI / 180F) - this.yawAcceleration * 0.01F);
        float aa = this.getHeadVerticalMovement();
        this.movePart(this.head, (double)(y * 6.5F * t), (double)(aa + u * 6.5F), (double)(-z * 6.5F * t));
        double[] ds = this.getSegmentProperties(5, 1.0F);

        int ab;
        for(ab = 0; ab < 3; ++ab) {
            EnderDragonBodyPart enderDragonPart = null;

            double[] es = this.getSegmentProperties(12 + ab * 2, 1.0F);
            float ac = this.getYaw() * ((float)Math.PI / 180F) + this.wrapYawChange(es[0] - ds[0]) * ((float)Math.PI / 180F);
            n = MathHelper.sin(ac);
            o = MathHelper.cos(ac);
            p = 1.5F;
            float ad = (float)(ab + 1) * 2.0F;
        }

        for(ab = 0; ab < this.parts.length; ++ab) {
            this.parts[ab].setPrevX(vec3ds[ab].x);
            this.parts[ab].setPrevY(vec3ds[ab].y);
            this.parts[ab].setPrevZ(vec3ds[ab].z);
        }
        this.setBodyYaw(this.getYaw());
        this.setHeadYaw(MathHelper.clampAngle(this.getHeadYaw(), this.getBodyYaw(), (float) this.getMaxHeadRotation()));
        this.setPrevHeadYaw(this.getHeadYaw());
    }

    public Vec3d getPathTarget() {
        return this.pathTarget;
    }

    public void phaseTick() {
        this.setYaw(MathHelper.wrapDegrees(this.getYaw()));
        double d;
        double e;
        double h;
        d = this.target.getX(); //d = playerX
        e = this.target.getZ(); //e = playerZ
        double f = d - this.getX(); //f = diffX
        double g = e - this.getZ(); //g = diffZ
        h = Math.sqrt(f * f + g * g); //h = diffAbsolute
        double i = Math.min(0.4 + h / 80.0 - 1.0, 10.0); //i=yOffset
        this.pathTarget = new Vec3d(d, this.target.getY() + i, e);
        d = this.pathTarget == null ? 0.0 : this.pathTarget.squaredDistanceTo(this.getX(), this.getY(), this.getZ()); //d = disSquared

        if (d < 100.0D && this.seenTargetTimes < 4) {
            this.target.sendMessage(Text.literal(String.format("Too close on gt %d", this.seenTargetTimes + 1)),false);
            return;
        }
        else if (d >= 4096.0D) {
            this.target.sendMessage(Text.literal(String.format("Too far on gt %d", this.seenTargetTimes + 1)),false);
            return;
        }

        e = 64.0;
        if (squaredDistanceTo(this.target) < 4096.0) {

            ++this.seenTargetTimes;
            Vec3d vec3d = (new Vec3d(this.target.getX() - this.getX(), 0.0, this.target.getZ() - this.getZ())).normalize();
            Vec3d vec3d2 = (new Vec3d((double) MathHelper.sin(this.getYaw() * ((float)Math.PI / 180F)), 0.0, (double) (-MathHelper.cos(this.getYaw() * ((float)Math.PI / 180F))))).normalize();
            float j = (float) vec3d2.dotProduct(vec3d);
            float k = (float) (Math.acos((double) j) * (double)(180F / (float)Math.PI));
            k += 0.5F;
            if (this.seenTargetTimes >= 5 && k >= 0.0F && k < 10.0F) {
                h = 1.0;
                Vec3d vec3d3 = this.getRotationVec(1.0F);
                double l = this.head.getX() - vec3d3.x * 1.0;
                double m = this.head.getBodyY(0.5) + 0.5;
                double n = this.head.getZ() - vec3d3.z * 1.0;
                double o = this.target.getX() - l;
                double p = this.target.getBodyY(0.5) - m;
                double q = this.target.getZ() - n;

                this.target.sendMessage(Text.literal(String.format("X: %.16G", l)),false);
                this.target.sendMessage(Text.literal(String.format("Y: %.16G", m)),false);
                this.target.sendMessage(Text.literal(String.format("Z: %.16G", n)),false);

                double powerX = 0;
                double powerY = 0;
                double powerZ = 0;
                double d0 = Math.sqrt(o * o + p * p + q * q);
                if (d0 != 0.0D) {
                    powerX = o / d0 * 0.1D;
                    powerY = p / d0 * 0.1D;
                    powerZ = q / d0 * 0.1D;
                }

                this.target.sendMessage(Text.literal(String.format("Power X: %.5f", powerX)),false);
                this.target.sendMessage(Text.literal(String.format("Power Y: %.5f", powerY)),false);
                this.target.sendMessage(Text.literal(String.format("Power Z: %.5f", powerZ)),false);

                Vec3d pos = getMinecartPositionForY(m);
                this.target.sendMessage(Text.literal(String.format("\nZ: %.16G", pos.z)),false);

                this.seenTargetTimes = 0;
            }
        }
    }

    public Vec3d getMinecartPositionForY(double targetY) {
        Vec3i vec3i = new Vec3i(0, -1, -1);
        Vec3i vec3i2 = new Vec3i(0, 0, 1);
        double d = (double) 0.5 + (double) vec3i.getX() * 0.5;
        double e = (double) 0.0625 + (double) vec3i.getY() * 0.5;
        double f = (double) 0.5 + (double) vec3i.getZ() * 0.5;
        double g = (double) 0.5 + (double) vec3i2.getX() * 0.5;
        double h = (double) 0.0625 + (double) vec3i2.getY() * 0.5;
        double l = (double) 0.5 + (double) vec3i2.getZ() * 0.5;
        double m = g - d;
        double n = (h - e) * 2.0;
        double o = l - f;

        double x = d + m;
        double y = targetY;
        double z = f + o;

        return new Vec3d(x, y, z);
    }

    public float getDefaultYawAcceleration() {
        float f = (float)this.getVelocity().horizontalLength() + 1.0F;
        float g = Math.min(f, 40.0F);
        return 0.7F / g / f;
    }

    public float getMaxYAcceleration () {
        return this.maxYAcceleration;
    }

    public void movePart (EnderDragonBodyPart enderDragonPart,double dx, double dy, double dz){
        enderDragonPart.setPosition(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
    }


    public float getHeadVerticalMovement () {
        double[] ds = this.getSegmentProperties(5, 1.0F);
        double[] es = this.getSegmentProperties(0, 1.0F);
        return (float) (ds[1] - es[1]);
    }
    public float wrapYawChange ( double yawDegrees){
        return (float) MathHelper.wrapDegrees(yawDegrees);
    }

    public EnderDragonBodyPart[] getBodyParts() {
        return this.parts;
    }

    public class EnderDragonBodyPart extends AbstractEntity {
        public final EnderDragon owner;
        public final String name;
        public final EntityDimensions partDimensions;

        public EnderDragonBodyPart(EnderDragonEntity originalDragon, EnderDragon owner, String name, float width, float height) {
            super(originalDragon.head.getPos(), originalDragon.head.getPitch(), originalDragon.head.getYaw(), originalDragon.head.getVelocity());
            this.partDimensions = EntityDimensions.changing(width, height);
            this.setDimensions(EntityDimensions.changing(width, height));
            this.owner = owner;
            this.name = name;
        }
        public EntityDimensions getDimensions(EntityPose pose) {
            return this.partDimensions;
        }
    }
}
