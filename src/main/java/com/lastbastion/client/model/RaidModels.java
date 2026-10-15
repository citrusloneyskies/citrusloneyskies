package com.lastbastion.client.model;

import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.client.render.SimpleProcRenderer;
import com.lastbastion.entity.misc.SupplyCrateEntity;
import com.lastbastion.entity.zombie.RiftEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** War-event models: the parachuted supply crate and the Corruption Rift. */
public final class RaidModels {
    private static ModelSpec.Baked crate, rift;
    static final int RIFT_SHARDS = RaidShapes.RIFT_SHARDS, RIFT_DEBRIS = RaidShapes.RIFT_DEBRIS;

    private RaidModels() {
    }

    // ------------------------------------------------------------------ supply crate

    public static ModelSpec.Baked crate() {
        if (crate == null) {
            ModelSpec s = new ModelSpec("supply_crate", 6);
            RaidShapes.crate(s);
            crate = s.bake();
        }
        return crate;
    }

    public static SimpleProcRenderer<SupplyCrateEntity> crateRenderer(EntityRendererProvider.Context ctx) {
        return new SimpleProcRenderer<>(ctx, crate(), (m, e, ls, la, age, hy, hp, pt) -> {
            boolean falling = !e.landed();
            ModelPart chute = m.p("chute");
            chute.visible = falling;
            ModelPart c = m.p("crate");
            if (falling) {
                c.zRot = Mth.sin(age * 0.08f) * 0.12f;
                c.xRot = Mth.cos(age * 0.06f) * 0.08f;
                m.p("canopy").yRot = age * 0.02f;
            }
            // locator lamp strobes, faster as the scuttle charge counts down
            int life = e.life();
            float rate = life < 200 ? 0.9f : 0.25f;
            float blink = 0.6f + 0.4f * Mth.sin(age * rate);
            ModelPart lamp = m.p("lamp");
            lamp.xScale = lamp.zScale = 0.8f + 0.25f * blink;
            lamp.yScale = 0.8f + 0.3f * blink;
        }, (e, pose, pt) -> pose.scale(1.1f, 1.1f, 1.1f), false);
    }

    // ------------------------------------------------------------------ corruption rift

    public static ModelSpec.Baked rift() {
        if (rift == null) {
            ModelSpec s = new ModelSpec("corruption_rift", 5);
            RaidShapes.rift(s);
            rift = s.bake();
        }
        return rift;
    }

    public static ProcRenderer<RiftEntity> riftRenderer(EntityRendererProvider.Context ctx) {
        ProcRenderer<RiftEntity> r = new ProcRenderer<>(ctx, rift(), (m, e, ls, la, age, hy, hp, pt) -> {
            float since = e.sinceBurst(pt);
            float flare = since >= 0 && since < 16 ? Mth.sin(since / 16f * Mth.PI) : 0;
            float charge = e.charge();
            float breathe = 1 + 0.06f * Mth.sin(age * 0.12f) + 0.25f * flare + 0.18f * charge;
            ModelPart tear = m.p("tear");
            tear.xScale = breathe * (1 + 0.04f * Mth.sin(age * 0.53f));
            tear.yScale = 1 + 0.03f * Mth.sin(age * 0.09f) + 0.08f * flare;
            tear.zScale = 1;
            tear.yRot = Mth.sin(age * 0.02f) * 0.15f;
            ModelPart voidCore = m.p("tear_void");
            voidCore.xScale = 1 - 0.15f * flare;
            voidCore.yScale = 1;
            voidCore.zScale = 1;
            ModelPart ring = m.p("shard_ring");
            ring.yRot = age * (0.03f + 0.12f * charge);
            for (int i = 0; i < RIFT_SHARDS; i++) {
                ModelPart sh = m.p("shard" + i);
                sh.y += Mth.sin(age * 0.07f + i * 1.3f) * 1.5f;
                sh.z += 2.5f * flare - 4f * charge;
                sh.zRot = Mth.sin(age * 0.05f + i) * 0.2f;
            }
            ModelPart orbit = m.p("debris_orbit");
            orbit.yRot = -age * 0.05f;
            for (int i = 0; i < RIFT_DEBRIS; i++) {
                ModelPart d = m.p("debris" + i);
                d.xRot += age * 0.04f * (i % 2 == 0 ? 1 : -1);
                d.yRot += age * 0.06f;
                d.y += Mth.sin(age * 0.1f + i) * 1.2f;
            }
            ModelPart tendrils = m.p("tendrils");
            tendrils.yRot = age * 0.1f;
            tendrils.xScale = tendrils.zScale = 1 + 0.2f * Mth.sin(age * 0.3f);
            tendrils.yScale = 1;
        }, 0f);
        r.glowAlpha(e -> 0.8f + 0.2f * Mth.sin(e.tickCount * 0.2f));
        return r;
    }
}
