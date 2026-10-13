package com.lastbastion.client.fx;

import com.lastbastion.fx.FxParticleOptions;
import com.lastbastion.fx.FxStyle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The tintable {@code lastbastion:fx} particle; behaviour is chosen by its {@link FxStyle}. */
public class FxParticle extends TextureSheetParticle {
    private final FxStyle style;
    private final float baseSize;
    private final float spin;
    private final float phase;
    private final float r0, g0, b0;

    protected FxParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, FxParticleOptions o, SpriteSet sprites) {
        super(level, x, y, z);
        this.style = o.style();
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        int n = FxStyle.values().length;
        setSprite(sprites.get(style.ordinal(), n - 1));
        int c = o.color();
        r0 = ((c >> 16) & 255) / 255f;
        g0 = ((c >> 8) & 255) / 255f;
        b0 = (c & 255) / 255f;
        setColor(r0, g0, b0);
        float var = 0.8f + random.nextFloat() * 0.4f;
        this.lifetime = o.life() > 0 ? o.life() : Math.max(2, (int) (defaultLife(style) * var));
        this.baseSize = Math.max(0.01f, o.size()) * (0.85f + random.nextFloat() * 0.3f);
        this.quadSize = baseSize;
        this.gravity = style.gravity;
        this.friction = switch (style) {
            case SPARK -> 0.95f;
            case SMOKE, SOUL, SKULL, BUBBLE -> 0.94f;
            case LEAF, FEATHER, FLAKE -> 0.9f;
            case GLOW, RING, STAR, RUNE -> 0.86f;
            default -> 0.96f;
        };
        this.hasPhysics = switch (style) {
            case SPARK, SHARD, DROP, DEBRIS, LEAF, FEATHER -> true;
            default -> false;
        };
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        this.spin = switch (style) {
            case SHARD, DEBRIS -> (random.nextFloat() - 0.5f) * 0.6f;
            case LEAF, FEATHER, FLAKE -> (random.nextFloat() - 0.5f) * 0.2f;
            case RUNE, STAR -> (random.nextFloat() - 0.5f) * 0.15f;
            case SMOKE -> (random.nextFloat() - 0.5f) * 0.05f;
            default -> 0f;
        };
        this.phase = random.nextFloat() * Mth.TWO_PI;
    }

    private static int defaultLife(FxStyle s) {
        return switch (s) {
            case GLOW -> 14;
            case SPARK -> 16;
            case EMBER -> 30;
            case SMOKE -> 40;
            case SHARD -> 30;
            case RING -> 12;
            case SOUL, SKULL -> 34;
            case STAR -> 18;
            case DROP -> 24;
            case LEAF, FEATHER -> 60;
            case RUNE -> 30;
            case BOLT -> 5;
            case BUBBLE -> 26;
            case FLAKE -> 40;
            case DEBRIS -> 34;
            case FLAME -> 16;
        };
    }

    @Override
    public void tick() {
        oRoll = roll;
        roll += spin;
        super.tick();
        float t = (float) age / lifetime;
        switch (style) {
            case SOUL, SKULL -> {
                xd += Mth.sin(age * 0.35f + phase) * 0.006;
                zd += Mth.cos(age * 0.31f + phase) * 0.006;
            }
            case LEAF, FEATHER -> {
                xd += Mth.sin(age * 0.2f + phase) * 0.01;
                zd += Mth.cos(age * 0.17f + phase) * 0.01;
                if (yd < -0.06) yd = -0.06;
            }
            case FLAKE -> {
                xd += Mth.sin(age * 0.25f + phase) * 0.004;
                if (yd < -0.05) yd = -0.05;
            }
            case EMBER, FLAME -> xd += Mth.sin(age * 0.5f + phase) * 0.004;
            case BUBBLE -> xd += Mth.sin(age * 0.6f + phase) * 0.008;
            default -> {
            }
        }
        // colour ageing: embers and flames cool toward red, smoke greys out
        switch (style) {
            case EMBER, FLAME -> setColor(r0, g0 * (1 - t * 0.6f), b0 * (1 - t * 0.9f));
            case SPARK -> setColor(Mth.lerp(t, Math.min(1, r0 + 0.4f), r0), Mth.lerp(t, Math.min(1, g0 + 0.4f), g0 * 0.8f), Mth.lerp(t, Math.min(1, b0 + 0.4f), b0 * 0.6f));
            default -> {
            }
        }
        alpha = switch (style) {
            case SMOKE -> 0.55f * (1 - t) * Math.min(1, t * 6 + 0.2f);
            case EMBER -> (1 - t) * (0.6f + 0.4f * Mth.sin(age * 1.7f + phase) * Mth.sin(age * 0.9f));
            case STAR -> (1 - t) * (0.5f + 0.5f * Math.abs(Mth.sin(age * 0.6f + phase)));
            case BOLT -> random.nextFloat() < 0.3f ? 0.3f : 1f;
            case BUBBLE, DROP -> Math.min(1, (1 - t) * 3);
            case LEAF, FEATHER, SHARD, DEBRIS -> Math.min(1, (1 - t) * 4);
            default -> Math.min(1, (1 - t) * 1.6f);
        };
    }

    @Override
    public float getQuadSize(float pt) {
        float t = Mth.clamp((age + pt) / lifetime, 0, 1);
        float k = switch (style) {
            case GLOW, SPARK, FLAME -> 1 - t * t;
            case EMBER -> 1 - t * 0.6f;
            case SMOKE -> 0.5f + t * 1.4f;
            case RING -> 0.2f + t * 2.2f;
            case SOUL, SKULL -> t < 0.15f ? t / 0.15f : 1 - (t - 0.15f) * 0.5f;
            case STAR -> 0.6f + 0.4f * Mth.sin(t * Mth.PI);
            case RUNE -> t < 0.2f ? t / 0.2f : 1f;
            case BUBBLE -> t > 0.9f ? 1.3f : 1f;
            default -> 1f;
        };
        return baseSize * k;
    }

    @Override
    protected int getLightColor(float pt) {
        return style.fullBright ? 0xF000F0 : super.getLightColor(pt);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return style.additive ? VfxRenderTypes.PARTICLE_ADDITIVE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float pt) {
        if (style != FxStyle.SPARK) {
            super.render(vc, camera, pt);
            return;
        }
        // sparks are stretched along their velocity
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(pt, xo, this.x) - cam.x);
        float y = (float) (Mth.lerp(pt, yo, this.y) - cam.y);
        float z = (float) (Mth.lerp(pt, zo, this.z) - cam.z);
        Vector3f vel = new Vector3f((float) xd, (float) yd, (float) zd);
        float speed = vel.length();
        if (speed < 1e-3f) {
            super.render(vc, camera, pt);
            return;
        }
        float size = getQuadSize(pt);
        Vector3f dir = vel.div(speed);
        Vector3f toCam = new Vector3f(-x, -y, -z);
        if (toCam.lengthSquared() < 1e-6f) return;
        toCam.normalize();
        Vector3f side = dir.cross(toCam, new Vector3f());
        if (side.lengthSquared() < 1e-6f) side.set(0, 1, 0);
        side.normalize().mul(size * 0.35f);
        float len = Math.min(1.6f, size * 1.5f + speed * 2.4f);
        Vector3f head = new Vector3f(x, y, z).add(new Vector3f(dir).mul(size * 0.5f));
        Vector3f tail = new Vector3f(x, y, z).sub(new Vector3f(dir).mul(len));
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int light = getLightColor(pt);
        vc.vertex(tail.x - side.x, tail.y - side.y, tail.z - side.z).uv(u1, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        vc.vertex(tail.x + side.x, tail.y + side.y, tail.z + side.z).uv(u1, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        vc.vertex(head.x + side.x, head.y + side.y, head.z + side.z).uv(u0, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        vc.vertex(head.x - side.x, head.y - side.y, head.z - side.z).uv(u0, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
    }

    public static final class Provider implements ParticleProvider<FxParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(FxParticleOptions o, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new FxParticle(level, x, y, z, vx, vy, vz, o, sprites);
        }
    }
}
