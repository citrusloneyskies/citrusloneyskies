package com.lastbastion.client.model;

import com.lastbastion.client.model.zombie.Hulks;
import com.lastbastion.client.model.zombie.Specials;
import com.lastbastion.client.model.zombie.Walkers;
import com.lastbastion.client.render.ProcRenderer;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Zombie models. Each archetype has a {@link Def} (in {@code client.model.zombie}) built on the jointed
 * {@link Body} rig with rank mutations gated by renderer level (rank + 1) and per-zombie outfit
 * variants, so a horde never looks like copies of one corpse.
 */
public final class ZombieModels {
    public interface Def {
        void build(ModelSpec s);

        void animate(ProcModel<SiegeZombie> m, SiegeZombie z, Ctx c);

        default float shadow() {
            return 0.5f;
        }

        /** Model size relative to the hitbox (models are authored at their natural size). */
        default float scale() {
            return 1f;
        }

        default boolean translucent() {
            return false;
        }

        default int density() {
            return 3;
        }
    }

    public static final class Ctx {
        public float ls, la, age, headYaw, headPitch, pt;
        public int id;
        public float t;
        public float hurt;
        /** Rank ordinal 0..4. */
        public int rank;
        /** Stable per-zombie random 0..999 for outfit variants and gait offsets. */
        public int variant;
        public int windup;
        /** 0 = still underground, 1 = emerged. */
        public float rise;

        public boolean is(int anim, float len) {
            return id == anim && t < len;
        }
    }

    private static final Map<ZombieKind, Def> DEFS = new EnumMap<>(ZombieKind.class);
    private static final Map<ZombieKind, ModelSpec.Baked> CACHE = new EnumMap<>(ZombieKind.class);

    static {
        DEFS.put(ZombieKind.SHAMBLER, new Walkers.Shambler());
        DEFS.put(ZombieKind.RUNNER, new Walkers.Runner());
        DEFS.put(ZombieKind.SPITTER, new Walkers.Spitter());
        DEFS.put(ZombieKind.LEAPER, new Walkers.Leaper());
        DEFS.put(ZombieKind.SHRIEKER, new Walkers.Shrieker());
        DEFS.put(ZombieKind.CRAWLER, new Walkers.Crawler());
        DEFS.put(ZombieKind.BRUTE, new Hulks.Brute());
        DEFS.put(ZombieKind.BLOATER, new Hulks.Bloater());
        DEFS.put(ZombieKind.BULWARK, new Hulks.Bulwark());
        DEFS.put(ZombieKind.WRECKER, new Hulks.Wrecker());
        DEFS.put(ZombieKind.ABOMINATION, new Hulks.Abomination());
        DEFS.put(ZombieKind.BURROWER, new Specials.Burrower());
        DEFS.put(ZombieKind.WRAITH, new Specials.Wraith());
        DEFS.put(ZombieKind.NECROMANCER, new Specials.Necromancer());
        DEFS.put(ZombieKind.INCINERATOR, new Specials.Incinerator());
    }

    private ZombieModels() {
    }

    public static ModelSpec.Baked baked(ZombieKind k) {
        return CACHE.computeIfAbsent(k, kind -> {
            Def d = DEFS.get(kind);
            ModelSpec s = new ModelSpec("zombie_" + kind.id, d.density());
            d.build(s);
            return s.bake();
        });
    }

    private static final Ctx CTX = new Ctx();

    public static void animate(ProcModel<SiegeZombie> m, SiegeZombie z, float ls, float la, float age, float hy, float hp, float pt) {
        Ctx c = CTX;
        c.ls = ls;
        c.la = la;
        c.age = age;
        c.headYaw = hy;
        c.headPitch = hp;
        c.pt = pt;
        c.id = z.anim.id();
        c.t = z.anim.time(pt);
        if (c.t > 300) c.id = 0;
        c.hurt = z.hurtTime > 0 ? (z.hurtTime - pt) / 10f : 0;
        c.rank = z.rank().ordinal();
        c.variant = Math.floorMod(z.getUUID().hashCode(), 1000);
        c.windup = z.attackWindup();
        c.rise = z.riseProgress(pt);
        DEFS.get(z.kind).animate(m, z, c);
    }

    public static ProcRenderer<SiegeZombie> renderer(EntityRendererProvider.Context ctx, ZombieKind kind) {
        Def d = DEFS.get(kind);
        ProcRenderer<SiegeZombie> r = new ProcRenderer<>(ctx, baked(kind), ZombieModels::animate, d.shadow(), d.translucent());
        r.level(z -> z.rank().ordinal() + 1)
                .scaleBy(z -> z.renderScale() * d.scale())
                .tint((z, c) -> {
                    switch (z.rank()) {
                        case FERAL -> {
                            c[0] = 0.95f;
                            c[1] = 1.04f;
                            c[2] = 0.92f;
                        }
                        case CORRUPTED -> {
                            c[0] = 0.95f;
                            c[1] = 0.85f;
                            c[2] = 1.06f;
                        }
                        case ABYSSAL -> {
                            c[0] = 1.04f;
                            c[1] = 0.78f;
                            c[2] = 0.76f;
                        }
                        default -> {
                        }
                    }
                    if (z.isChampion()) {
                        c[0] = Math.min(1.2f, c[0] * 1.08f);
                        c[2] *= 0.85f;
                    }
                    if (kind == ZombieKind.WRAITH) c[3] = 0.6f + Mth.sin(z.tickCount * 0.1f) * 0.1f;
                })
                .pre((z, pose, pt) -> {
                    float rise = z.riseProgress(pt);
                    if (rise < 1f) pose.translate(0, -(1f - Anim.easeOut(rise)) * z.getBbHeight() * 1.05f, 0);
                    if (kind == ZombieKind.BURROWER && z.state()) pose.scale(0.001f, 0.001f, 0.001f);
                });
        return r;
    }
}
