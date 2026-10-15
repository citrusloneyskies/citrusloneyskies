package com.lastbastion.client.model;

import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.entity.hero.HellspawnEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** Renderers and animation for the Devils' lesser demons: imp, hellhound and fiend. */
public final class HellspawnModels {
    private static final Map<HellspawnEntity.Variant, ModelSpec.Baked> BAKED = new EnumMap<>(HellspawnEntity.Variant.class);

    private HellspawnModels() {
    }

    public static ModelSpec.Baked baked(HellspawnEntity.Variant v) {
        return BAKED.computeIfAbsent(v, k -> {
            Consumer<ModelSpec> b = switch (k) {
                case IMP -> HellspawnShapes::imp;
                case HELLHOUND -> HellspawnShapes::hellhound;
                case FIEND -> HellspawnShapes::fiend;
            };
            ModelSpec s = new ModelSpec("hellspawn_" + k.id, k == HellspawnEntity.Variant.FIEND ? 4 : 5);
            b.accept(s);
            return s.bake();
        });
    }

    public static ProcRenderer<HellspawnEntity> renderer(EntityRendererProvider.Context ctx, HellspawnEntity.Variant v) {
        float shadow = switch (v) {
            case IMP -> 0.35f;
            case HELLHOUND -> 0.6f;
            case FIEND -> 0.9f;
        };
        ProcModel.Animator<HellspawnEntity> anim = switch (v) {
            case IMP -> HellspawnModels::imp;
            case HELLHOUND -> HellspawnModels::hound;
            case FIEND -> HellspawnModels::fiend;
        };
        ProcRenderer<HellspawnEntity> r = new ProcRenderer<>(ctx, baked(v), anim, shadow);
        r.glowAlpha(e -> 0.8f + 0.2f * Mth.sin(e.tickCount * 0.2f));
        // they burn away when their time is up
        r.tint((e, c) -> {
            float fade = e.fade();
            c[0] = 1f;
            c[1] = c[2] = 0.4f + 0.6f * fade;
        });
        return r;
    }

    private static float time(HellspawnEntity e, float pt) {
        return e.anim.time(pt);
    }

    // ------------------------------------------------------------------ imp

    static void imp(ProcModel<HellspawnEntity> m, HellspawnEntity e, float ls, float la, float age, float hy, float hp, float pt) {
        Poser p = Poser.of(m);
        p.idle(age, 1.3f);
        p.walk(ls, la, 1.1f, 0.6f, 1.4f);
        p.look(hy, hp);
        // a restless little hop and constant wing-beat
        float flap = Mth.sin(age * 0.9f);
        m.p("wing_r").yRot += 0.5f + flap * 0.35f;
        m.p("wing_l").yRot -= 0.5f + flap * 0.35f;
        m.p("wing_r").zRot += flap * 0.15f;
        m.p("wing_l").zRot -= flap * 0.15f;
        p.lift(-Math.max(0, Mth.sin(age * 0.45f)) * 0.8f);
        for (int i = 0; i < HellspawnShapes.IMP_TAIL; i++) {
            ModelPart t = m.p("tail" + i);
            t.xRot += (i == 0 ? 0.5f : -0.25f) + Mth.sin(age * 0.15f - i) * 0.12f;
            t.yRot += Mth.sin(age * 0.11f - i * 0.8f) * 0.3f;
        }
        ModelPart orb = m.p("orb");
        float pulse = 1f + 0.15f * Mth.sin(age * 0.6f);
        orb.xScale = orb.yScale = orb.zScale = pulse;
        float t = time(e, pt);
        switch (e.anim.id()) {
            case HellspawnEntity.ANIM_ATTACK -> {
                float k = Anim.swing(t, 5, 2, 5);
                p.armL(-100 * Math.max(0, k) - 40 * Math.max(0, -k), 30 * Math.max(0, k) - 40 * Math.max(0, -k), 0, 30);
                p.spine(0, 20 * Math.max(0, k) - 30 * Math.max(0, -k), 0);
            }
            case HellspawnEntity.ANIM_SPECIAL -> {
                float k = Anim.swing(t, 7, 3, 6);
                p.armR(-160 * Math.max(0, k) - 60 * Math.max(0, -k), 0, 20 * Math.max(0, k), 30);
                p.spine(-15 * Math.max(0, k) + 20 * Math.max(0, -k), 0, 0);
                orb.visible = t < 8 || t > 20;
            }
            case HellspawnEntity.ANIM_ARRIVE -> {
                float k = 1 - Anim.easeOut(Anim.win(t, 0, 16));
                p.crouch(k, 3);
                p.armR(-120 * k, 0, 30 * k, 20);
                p.armL(-120 * k, 0, -30 * k, 20);
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ hellhound

    static void hound(ProcModel<HellspawnEntity> m, HellspawnEntity e, float ls, float la, float age, float hy, float hp, float pt) {
        float amt = Math.min(1, la);
        float ph = ls * 0.9f;
        String[] legs = {"leg_fr", "leg_fl", "leg_br", "leg_bl"};
        float[] phase = {0, Mth.PI * 0.5f, Mth.PI, Mth.PI * 1.5f};
        for (int i = 0; i < 4; i++) {
            float s = Mth.sin(ph + phase[i]);
            m.p(legs[i]).xRot += s * 0.75f * amt;
            m.p(legs[i] + "_lo").xRot += Math.max(0, -Mth.cos(ph + phase[i])) * (i < 2 ? -0.6f : 0.6f) * amt;
        }
        ModelPart body = m.p("body"), head = m.p("head"), neck = m.p("neck"), jaw = m.p("jaw");
        body.y -= Math.abs(Mth.sin(ph)) * 0.8f * amt;
        body.xRot += Mth.sin(ph * 2) * 0.04f * amt;
        neck.yRot += Mth.clamp(hy, -50, 50) * Mth.DEG_TO_RAD * 0.5f;
        head.yRot += Mth.clamp(hy, -50, 50) * Mth.DEG_TO_RAD * 0.4f;
        head.xRot += Mth.clamp(hp, -40, 40) * Mth.DEG_TO_RAD * 0.6f;
        // panting
        jaw.xRot += 0.15f + 0.08f * Mth.sin(age * 0.5f);
        for (int i = 0; i < HellspawnShapes.HOUND_TAIL; i++) {
            m.p("tail" + i).zRot += Mth.sin(age * 0.3f - i * 0.6f) * 0.15f * (i + 1) * 0.5f;
        }
        float t = time(e, pt);
        switch (e.anim.id()) {
            case HellspawnEntity.ANIM_ATTACK -> {
                float k = Anim.swing(t, 5, 2, 5);
                float wind = Math.max(0, k), snap = Math.max(0, -k);
                neck.xRot += -0.3f * wind + 0.5f * snap;
                head.xRot += 0.2f * snap;
                jaw.xRot += 0.7f * wind - 0.15f * snap;
                body.xRot -= 0.1f * wind;
            }
            case HellspawnEntity.ANIM_SPECIAL -> {
                float k = Anim.hold(t, 4, 22, 3);
                neck.xRot += 0.35f * k;
                head.xRot -= 0.2f * k;
                jaw.xRot += 0.75f * k;
                body.xRot += 0.08f * k;
                m.p("leg_fr").xRot -= 0.3f * k;
                m.p("leg_fl").xRot -= 0.3f * k;
            }
            case HellspawnEntity.ANIM_ARRIVE -> {
                float k = 1 - Anim.easeOut(Anim.win(t, 0, 16));
                body.y += 3 * k;
                neck.xRot -= 0.6f * k;
                jaw.xRot += 0.8f * Anim.pulse(t, 4, 14);
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ fiend

    static void fiend(ProcModel<HellspawnEntity> m, HellspawnEntity e, float ls, float la, float age, float hy, float hp, float pt) {
        Poser p = Poser.of(m);
        p.idle(age, 1f);
        p.walk(ls, la, 0.7f, 0.5f, 1.2f);
        p.look(hy, hp);
        // knuckles dragging, shoulders rolling
        p.armR(-10, 0, 8, 25);
        p.armL(-10, 0, -8, 25);
        p.spine(0, 0, Mth.sin(ls * 0.66f) * 4 * Math.min(1, la));
        m.p("tail0").yRot += Mth.sin(age * 0.08f) * 0.2f;
        float t = time(e, pt);
        switch (e.anim.id()) {
            case HellspawnEntity.ANIM_ATTACK -> {
                float k = Anim.swing(t, 8, 3, 6);
                float wind = Math.max(0, k), strike = Math.max(0, -k);
                p.armR(-60 * wind - 90 * strike, 60 * wind - 60 * strike, 30 * wind, 70 * wind);
                p.spine(5, 30 * wind - 40 * strike, 0);
            }
            case HellspawnEntity.ANIM_SPECIAL -> {
                float up = Anim.hold(t, 2, 10, 3);
                float slam = Anim.hold(t, 12, 18, 3);
                p.armR(-170 * up - 60 * slam, 0, 10 * up, 20);
                p.armL(-170 * up - 60 * slam, 0, -10 * up, 20);
                p.spine(-20 * up + 40 * slam, 0, 0);
                p.crouch(slam * 0.7f, 4);
            }
            case HellspawnEntity.ANIM_ARRIVE -> {
                float k = 1 - Anim.easeOut(Anim.win(t, 0, 20));
                p.crouch(k, 6);
                float roar = Anim.pulse(t, 8, 20);
                p.armR(-60 * roar, 0, 60 * roar, 40);
                p.armL(-60 * roar, 0, -60 * roar, 40);
                p.spine(-20 * roar, 0, 0);
                p.head.xRot -= 0.4f * roar;
            }
            default -> {
            }
        }
    }
}
