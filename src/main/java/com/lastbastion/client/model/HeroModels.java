package com.lastbastion.client.model;

import com.lastbastion.client.model.hero.*;
import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.HeroKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Hero models. Each hero has its own {@link Def} (in {@code client.model.hero}) that builds a detailed
 * jointed model and animates every move in its kit. This class wires them to renderers and provides the
 * animation context and shared motion (gait, breathing, look, flinch, dodge, arrival, level-up).
 */
public final class HeroModels {
    /** A hero's model definition. */
    public interface Def {
        void build(ModelSpec s);

        void animate(ProcModel<HeroEntity> m, HeroEntity h, Ctx c);

        default float scale() {
            return 1f;
        }

        default float shadow() {
            return 0.5f;
        }

        default int density() {
            return 4;
        }
    }

    /** Per-frame animation inputs. */
    public static final class Ctx {
        public float ls, la, age, headYaw, headPitch, pt;
        /** Current one-shot animation id and its time in ticks. */
        public int id;
        public float t;
        public boolean combat;
        /** 0..1 hurt flash. */
        public float hurt;
        public int level;

        /** True while animation {@code anim} is playing and younger than {@code len} ticks. */
        public boolean is(int anim, float len) {
            return id == anim && t < len;
        }
    }

    private static final Map<HeroKind, Def> DEFS = new EnumMap<>(HeroKind.class);
    private static final Map<HeroKind, ModelSpec.Baked> CACHE = new EnumMap<>(HeroKind.class);

    static {
        DEFS.put(HeroKind.ASHFANG, new AshfangModel());
        DEFS.put(HeroKind.SYLVA, new SylvaModel());
        DEFS.put(HeroKind.BASTION, new BastionModel());
        DEFS.put(HeroKind.ALDRIC, new AldricModel());
        DEFS.put(HeroKind.MORGANA, new MorganaModel());
        DEFS.put(HeroKind.KAGE, new KageModel());
        DEFS.put(HeroKind.AEGIS7, new Aegis7Model());
        DEFS.put(HeroKind.ASTERION, new AsterionModel());
        DEFS.put(HeroKind.ANUBIS, new AnubisModel());
        DEFS.put(HeroKind.ZEUS, new ZeusModel());
    }

    private HeroModels() {
    }

    public static ModelSpec.Baked baked(HeroKind k) {
        return CACHE.computeIfAbsent(k, kind -> {
            Def d = DEFS.get(kind);
            ModelSpec s = new ModelSpec("hero_" + kind.id, d.density());
            d.build(s);
            return s.bake();
        });
    }

    private static final Ctx CTX = new Ctx();

    public static void animate(ProcModel<HeroEntity> m, HeroEntity h, float ls, float la, float age, float hy, float hp, float pt) {
        Ctx c = CTX;
        c.ls = ls;
        c.la = la;
        c.age = age;
        c.headYaw = hy;
        c.headPitch = hp;
        c.pt = pt;
        c.id = h.anim.id();
        c.t = h.anim.time(pt);
        if (c.t > 400) c.id = 0;
        c.combat = h.inCombat();
        c.hurt = h.hurtTime > 0 ? (h.hurtTime - pt) / 10f : 0;
        c.level = h.heroLevel();
        DEFS.get(h.kind).animate(m, h, c);
    }

    /**
     * Shared humanoid motion: breathing, gait, head look, flinch, fighting stance, and the generic
     * dodge / arrival / level-up animations. Heroes layer their own attack poses on top.
     */
    public static void humanoid(Poser p, HeroEntity h, Ctx c, float stride, float armSwing) {
        p.idle(c.age, c.combat ? 0.6f : 1f);
        float move = Math.min(1, c.la);
        boolean run = h.getDeltaMovement().horizontalDistanceSqr() > 0.02;
        p.walk(c.ls, c.la, stride * (run ? 1.2f : 1f), armSwing, run ? 1.3f : 1f);
        if (run) p.spine(10 * move, 0, 0);
        p.look(c.headYaw, c.headPitch);
        p.flinch(c.hurt);
        if (c.combat && move < 0.3f && c.id == 0) p.stance(0.6f * (1 - move));
        switch (c.id) {
            case HeroEntity.ANIM_DODGE -> {
                float k = Anim.pulse(c.t, 0, 12);
                p.crouch(k, 3);
                p.spine(-15 * k, 0, 0);
                p.armR(-40 * k, 0, 30 * k, 40 * k);
                p.armL(-40 * k, 0, -30 * k, 40 * k);
            }
            case HeroEntity.ANIM_ARRIVE -> {
                float k = 1 - Anim.easeOut(Anim.win(c.t, 0, 30));
                p.crouch(k, 6);
                p.spine(25 * k, 0, 0);
                p.legR(0, 0, 20 * k);
                p.armR(-20 * k, 0, 10 * k, 30 * k);
            }
            case HeroEntity.ANIM_LEVELUP -> {
                float k = Anim.pulse(c.t, 0, 30);
                p.armR(-160 * k, 0, 20 * k, 10 * k);
                p.armL(-160 * k, 0, -20 * k, 10 * k);
                p.spine(-10 * k, 0, 0);
                p.head.xRot -= 0.4f * k;
            }
            default -> {
            }
        }
    }

    public static ProcRenderer<HeroEntity> renderer(EntityRendererProvider.Context ctx, HeroKind kind) {
        Def d = DEFS.get(kind);
        ProcRenderer<HeroEntity> r = new ProcRenderer<>(ctx, baked(kind), HeroModels::animate, d.shadow());
        r.scale(d.scale()).level(HeroEntity::heroLevel);
        r.glowAlpha(h -> 0.85f + 0.15f * Mth.sin(h.tickCount * 0.12f));
        if (kind == HeroKind.AEGIS7) {
            r.tint((h, c) -> {
                if (h instanceof com.lastbastion.entity.hero.Aegis7Hero a && a.overdriven()) {
                    c[0] = 1f;
                    c[1] = 0.82f;
                    c[2] = 0.78f;
                }
            });
        }
        if (kind == HeroKind.ASTERION) {
            r.scaleBy(h -> h instanceof com.lastbastion.entity.hero.AsterionHero a && a.giant() ? 1.2f : 1f);
        }
        return r;
    }

    /** Kage's shadow clones: the same model rendered as translucent living shadow. */
    public static ProcRenderer<HeroEntity> cloneRenderer(EntityRendererProvider.Context ctx) {
        Def d = DEFS.get(HeroKind.KAGE);
        ProcRenderer<HeroEntity> r = new ProcRenderer<>(ctx, baked(HeroKind.KAGE), HeroModels::animate, 0.3f, true);
        r.scale(d.scale()).level(h -> 1).tint((h, c) -> {
            c[0] = 0.25f;
            c[1] = 0.15f;
            c[2] = 0.35f;
            c[3] = 0.6f;
        });
        return r;
    }
}
