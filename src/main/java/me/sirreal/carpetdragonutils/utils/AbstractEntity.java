package me.sirreal.carpetdragonutils.utils;

import net.minecraft.entity.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

abstract class AbstractEntity {
    private double prevX;
    private double prevY;
    private double prevZ;
    private float headYaw;
    private float prevHeadYaw;
    private float bodyYaw;
    private Vec3d pos;
    private Vec3d velocity;
    private float yaw;
    private float pitch;
    public float prevYaw;
    public float prevPitch;
    private Box boundingBox;
    private Vec3d movementMultiplier;
    private EntityDimensions dimensions;
    public AbstractEntity(Vec3d pos, float pitch, float yaw, Vec3d velocity) {

        this.velocity = velocity;
        this.movementMultiplier = Vec3d.ZERO;
        this.dimensions = EntityType.ENDER_DRAGON.getDimensions();
        this.pos = pos;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    protected void setRotation(float yaw, float pitch) {
        this.setYaw(yaw % 360.0F);
        this.setPitch(pitch % 360.0F);
    }

    private final void setPosition(Vec3d pos) {
        this.setPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    void setPosition(double x, double y, double z) {
        this.setPos(x, y, z);
        this.setBoundingBox(this.calculateBoundingBox());
    }
    void move(MovementType movementType, Vec3d movement) {
        this.setPosition(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
    }
    void updateVelocity(float speed, Vec3d movementInput) {
        Vec3d vec3d = movementInputToVelocity(movementInput, speed, this.getYaw());
        this.setVelocity(this.getVelocity().add(vec3d));
    }
    protected Box calculateBoundingBox() {
        return this.dimensions.getBoxAt(this.pos);
    }
    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        } else {
            Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
            float f = MathHelper.sin(yaw * 0.017453292F);
            float g = MathHelper.cos(yaw * 0.017453292F);
            return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
        }
    }
    private final void resetPosition() {
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        this.prevX = d;
        this.prevY = e;
        this.prevZ = f;
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }
    private void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
        this.setPos(x, y, z);
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.resetPosition();
        this.refreshPosition();
    }
    protected void refreshPosition() {
        this.setPosition(this.pos.x, this.pos.y, this.pos.z);
    }
    double squaredDistanceTo(Entity entity) {
        return this.squaredDistanceTo(entity.getPos());
    }

    private double squaredDistanceTo(Vec3d vector) {
        double d = this.getX() - vector.x;
        double e = this.getY() - vector.y;
        double f = this.getZ() - vector.z;
        return d * d + e * e + f * f;
    }
    final Vec3d getRotationVec(float tickDelta) {
        return this.getRotationVector(this.getPitch(tickDelta), this.getYaw(tickDelta));
    }
    private float getPitch(float tickDelta) {
        return tickDelta == 1.0F ? this.getPitch() : MathHelper.lerp(tickDelta, this.prevPitch, this.getPitch());
    }
    private float getYaw(float tickDelta) {
        return tickDelta == 1.0F ? this.getHeadYaw() : MathHelper.lerp(tickDelta, this.prevHeadYaw, this.getHeadYaw());
    }
    protected final Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180F);
        float g = -yaw * ((float)Math.PI / 180F);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
    }
    private void setVelocity(double x, double y, double z) {
        this.setVelocity(new Vec3d(x, y, z));
    }
    private void readNbt(NbtCompound nbt) {
        try {
            NbtList nbtList = nbt.getList("Pos", 6);
            NbtList nbtList2 = nbt.getList("Motion", 6);
            NbtList nbtList3 = nbt.getList("Rotation", 5);
            double d = nbtList2.getDouble(0);
            double e = nbtList2.getDouble(1);
            double f = nbtList2.getDouble(2);
            this.setVelocity(Math.abs(d) > 10.0 ? 0.0 : d, Math.abs(e) > 10.0 ? 0.0 : e, Math.abs(f) > 10.0 ? 0.0 : f);
            double g = 3.0000512E7;
            this.setPos(MathHelper.clamp(nbtList.getDouble(0), -3.0000512E7, 3.0000512E7), MathHelper.clamp(nbtList.getDouble(1), -2.0E7, 2.0E7), MathHelper.clamp(nbtList.getDouble(2), -3.0000512E7, 3.0000512E7));
            this.setYaw(nbtList3.getFloat(0));
            this.setPitch(nbtList3.getFloat(1));
            this.resetPosition();

            if (Double.isFinite(this.getX()) && Double.isFinite(this.getY()) && Double.isFinite(this.getZ())) {
                if (Double.isFinite((double)this.getYaw()) && Double.isFinite((double)this.getPitch())) {
                    this.refreshPosition();
                    this.setRotation(this.getYaw(), this.getPitch());

                } else {
                    throw new IllegalStateException("Entity has invalid rotation");
                }
            } else {
                throw new IllegalStateException("Entity has invalid position");
            }
        } catch (Throwable var17) {
            CrashReport crashReport = CrashReport.create(var17, "Loading entity NBT");
            CrashReportSection crashReportSection = crashReport.addElement("Entity being loaded");
            throw new CrashException(crashReport);
        }
    }
    private final void setBoundingBox(Box boundingBox) {
        this.boundingBox = boundingBox;
    }
    private final float getHeight() {
        return this.dimensions.height;
    }
    public void setDimensions(EntityDimensions dimensions) {
        this.dimensions = dimensions;
    }
    Vec3d getVelocity() {
        return this.velocity;
    }

    void setVelocity(Vec3d velocity) {
        this.velocity = velocity;
    }
    final double getX() {
        return this.pos.x;
    }
    final double getY() {
        return this.pos.y;
    }
    double getBodyY(double heightScale) {
        return this.pos.y + (double)this.getHeight() * heightScale;
    }
    public final double getZ() {
        return this.pos.z;
    }
    private void setPos(double x, double y, double z) {
        if (this.pos.x != x || this.pos.y != y || this.pos.z != z) {
            this.pos = new Vec3d(x, y, z);
        }
    }
    public float getYaw() {
        return this.yaw;
    }
    public void setYaw(float yaw) {
        if (!Float.isFinite(yaw)) {
            Util.error("Invalid entity rotation: " + yaw + ", discarding.");
        } else {
            this.yaw = yaw;
        }
    }
    public float getPitch() {
        return this.pitch;
    }
    private void setPitch(float pitch) {
        if (!Float.isFinite(pitch)) {
            Util.error("Invalid entity rotation: " + pitch + ", discarding.");
        } else {
            this.pitch = pitch;
        }
    }

    public double getPrevX() {
        return prevX;
    }

    public void setPrevX(double prevX) {
        this.prevX = prevX;
    }

    public double getPrevY() {
        return prevY;
    }

    public void setPrevY(double prevY) {
        this.prevY = prevY;
    }

    public double getPrevZ() {
        return prevZ;
    }

    public void setPrevZ(double prevZ) {
        this.prevZ = prevZ;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public void setBodyYaw(float bodyYaw) {
        this.bodyYaw = bodyYaw;
    }

    public float getHeadYaw() {
        return headYaw;
    }

    public void setHeadYaw(float headYaw) {
        this.headYaw = headYaw;
    }

    public float getPrevHeadYaw() {
        return prevHeadYaw;
    }

    public void setPrevHeadYaw(float lastHeadYaw) {
        this.prevHeadYaw = lastHeadYaw;
    }
}
