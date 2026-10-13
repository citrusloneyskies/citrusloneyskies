package com.lastbastion.client.model;

import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.boss.*;
import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.boss.PhylacteryEntity;
import com.lastbastion.entity.boss.SiegeBoss;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Boss models. Each boss has a {@link Def} in {@code client.model.boss} with a detailed model, phase-gated
 * parts (renderer level = phase), and animations for its intro, every attack, stagger and death.
 */
public final class BossModels {
    public interface Def {
        void build(ModelSpec s);

        void animate(ProcModel<SiegeBoss> m, SiegeBoss b, Ctx c);

        default float scale() {
            return 1f;
        }

        default float shadow() {
            return 1.5f;
        }

        default int density() {
            return 3;
        }
    }

    public static final class Ctx {
        public float ls, la, age, headYaw, headPitch, pt;
        public int id;
        public float t;
        public int phase;
        public float hurt;
        /** Ticks since death (0 while alive). */
        public float death;
        public boolean staggered;
    }

    private static final Map<BossKind, Def> DEFS = new EnumMap<>(BossKind.class);
    private static final Map<BossKind, ModelSpec.Baked> CACHE = new EnumMap<>(BossKind.class);
    private static ModelSpec.Baked crystal;

    static {
        DEFS.put(BossKind.GOREHOUND, new GorehoundModel());
        DEFS.put(BossKind.PLAGUE_MOTHER, new PlagueMotherModel());
        DEFS.put(BossKind.MALGRAVE, new MalgraveModel());
        DEFS.put(BossKind.OSSUARY, new OssuaryModel());
        DEFS.put(BossKind.VARKHUL, new VarkhulModel());
        DEFS.put(BossKind.MORVATHRAX, new MorvathraxModel());
    }

    private BossModels() {
    }

    public static ModelSpec.Baked baked(BossKind k) {
        return CACHE.computeIfAbsent(k, kind -> {
            Def d = DEFS.get(kind);
            ModelSpec s = new ModelSpec("boss_" + kind.id, d.density());
            d.build(s);
            return s.bake();
        });
    }

    private static final Ctx CTX = new Ctx();

    public static void animate(ProcModel<SiegeBoss> m, SiegeBoss b, float ls, float la, float age, float hy, float hp, float pt) {
        Ctx c = CTX;
        c.ls = ls;
        c.la = la;
        c.age = age;
        c.headYaw = hy;
        c.headPitch = hp;
        c.pt = pt;
        c.id = b.anim.id();
        c.t = b.anim.time(pt);
        if (c.t > 600) c.id = 0;
        c.phase = b.phase();
        c.hurt = b.hurtTime > 0 ? (b.hurtTime - pt) / 10f : 0;
        c.death = b.deathTime > 0 ? b.deathTime + pt : 0;
        c.staggered = c.id == SiegeBoss.ANIM_STAGGER && c.t < 70;
        DEFS.get(b.kind).animate(m, b, c);
    }

    public static ProcRenderer<SiegeBoss> renderer(EntityRendererProvider.Context ctx, BossKind kind) {
        Def d = DEFS.get(kind);
        ProcRenderer<SiegeBoss> r = new ProcRenderer<>(ctx, baked(kind), BossModels::animate, d.shadow());
        r.scale(d.scale()).level(SiegeBoss::phase).noDeathFlip();
        r.glowAlpha(b -> b.deathTime > 0 ? Math.max(0, 1 - b.deathTime / 50f) : 0.85f + 0.15f * Mth.sin(b.tickCount * 0.15f));
        return r;
    }

    // ------------------------------------------------------------------ phylactery crystal

    public static ModelSpec.Baked crystal() {
        if (crystal == null) {
            ModelSpec s = new ModelSpec("phylactery", 4);
            Part c = s.root.child("crystal", 0, 12, 0);
            Paint glow = Paint.of(Paint.Mat.CRYSTAL, 0x5AD8FF).accent(0xE0FFFF).glow().pattern(Pattern.RUNES, 0xFFFFFF);
            c.taper(0, 0, 0, 6, 6, 0.6f, 0.6f, 10, 5, glow);
            c.sub(0, 0, 0).rot(180, 0, 0).taper(0, 0, 0, 6, 6, 0.6f, 0.6f, 8, 4, glow);
            c.child("shard1", -4.5f, 2, 0).rot(0, 0, 25).taper(0, 0, 0, 2, 2, 0.3f, 0.3f, 6, 3, glow);
            c.child("shard2", 4.5f, 2, 0).rot(0, 0, -25).taper(0, 0, 0, 2, 2, 0.3f, 0.3f, 6, 3, glow);
            Part ring = s.root.child("ring", 0, 14, 0);
            ring.octoRing(-0.5f, 13, 0.8f, 1, Paint.of(Paint.Mat.METAL, 0x5A6A8A).pattern(Pattern.RUNES, 0x5AD8FF).accentGlow());
            crystal = s.bake();
        }
        return crystal;
    }

    public static ProcRenderer<PhylacteryEntity> crystalRenderer(EntityRendererProvider.Context ctx) {
        return new ProcRenderer<PhylacteryEntity>(ctx, crystal(), (m, e, ls, la, age, hy, hp, pt) -> {
            m.p("crystal").yRot = age * 0.05f;
            m.p("crystal").y += Mth.sin(age * 0.1f) * 1.5f;
            m.p("ring").yRot = -age * 0.08f;
            m.p("ring").zRot = Mth.sin(age * 0.05f) * 0.3f;
        }, 0.3f);
    }
}
