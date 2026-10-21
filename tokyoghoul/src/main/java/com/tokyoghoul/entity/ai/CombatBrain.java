package com.tokyoghoul.entity.ai;

import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Moves;
import com.tokyoghoul.entity.TGCharacterEntity;
import com.tokyoghoul.move.Anims;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.move.MoveRunner;
import com.tokyoghoul.network.Sync;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Anime-style fight decisions, rebuilt from Jujutsu Craft's per-character AI procedures
 * (AIGojoProcedure & co.), CalculateAttackProcedure and AIActiveProcedure:
 * <ul>
 * <li>every few ticks (faster at higher level) pick a technique valid for the current distance and off
 * cooldown, with a random retry loop like JJK's {@code rnd = 4 + random*16} search;</li>
 * <li>otherwise choose a basic attack by distance band: jab / knockback / barrage up close, run and jump
 * attacks to close the gap;</li>
 * <li>desperation ultimates below half health (JJK's Hollow Purple at 30% health);</li>
 * <li>reactions: guard or side-dodge when the enemy winds up a move in range or a projectile flies in.</li>
 * </ul>
 */
public final class CombatBrain {
    private final TGCharacterEntity mob;
    private int decide = 10;
    private int reactCooldown;
    private int noTarget;
    private boolean guardAnim;
    public boolean ultimateUsed;
    /** Ranged characters prefer to keep some distance. */
    public final boolean ranged;

    public CombatBrain(TGCharacterEntity mob) {
        this.mob = mob;
        boolean r = false;
        CharacterDef d = mob.fighterDef();
        if (d != null) {
            for (MoveDef m : d.moves()) {
                if (m.kind == MoveDef.Kind.VOLLEY || m.kind == MoveDef.Kind.LIGHTNING) r = true;
            }
        }
        this.ranged = r;
    }

    public float preferredRange() {
        return ranged ? 7f : 2.2f;
    }

    /** Rank the NPC fights at, for move unlocks (named bosses know their whole kit). */
    private int rank() {
        int lv = mob.fighterLevel();
        return lv >= 8 ? 5 : lv >= 6 ? 4 : lv >= 4 ? 3 : lv >= 2 ? 2 : 1;
    }

    public void tick() {
        FighterState s = mob.fighterState();
        LivingEntity t = mob.getTarget();
        if (guardAnim && s.guardTicks <= 0) {
            guardAnim = false;
            Sync.anim(mob, Anims.NONE);
        }
        if (t == null || !t.isAlive()) {
            if (++noTarget > 200 && s.kaguneTicks > 1 && !s.transformed()) s.kaguneTicks = 1;
            return;
        }
        noTarget = 0;
        CharacterDef def = mob.fighterDef();
        double dist = mob.distanceTo(t);
        if (def.hasKagune() && dist < 24) MoveRunner.kaguneOut(mob, s, 80);
        if (s.stun > 0 || s.busy()) return;
        if (react(t, s, dist)) return;
        if (--decide > 0) return;
        decide = Math.max(4, 12 - mob.fighterLevel()) + mob.getRandom().nextInt(5);
        choose(t, s, def, dist);
    }

    // ------------------------------------------------------------------ reactions

    private boolean react(LivingEntity t, FighterState s, double dist) {
        if (reactCooldown > 0) {
            reactCooldown--;
            return false;
        }
        float skill = Math.min(0.7f, 0.12f + 0.06f * mob.fighterLevel());
        FighterState ts = Fighters.state(t);
        if (ts != null && ts.move != null && ts.moveTick <= ts.move.windup) {
            MoveDef m = ts.move;
            boolean threat = m.kind != MoveDef.Kind.FORM && m.kind != MoveDef.Kind.BUFF && dist < reach(m) + 1.5;
            if (threat && mob.getRandom().nextFloat() < skill) {
                reactCooldown = 30;
                boolean ranged = m.kind == MoveDef.Kind.VOLLEY || m.kind == MoveDef.Kind.LIGHTNING || m.kind == MoveDef.Kind.SPIKES;
                if (ranged || m.kind == MoveDef.Kind.GRAB || mob.getRandom().nextBoolean()) dodge(t, s);
                else guard(s, m.windup - ts.moveTick + 10);
                return true;
            }
        }
        if (mob.tickCount % 3 == 0) {
            for (Projectile p : mob.level().getEntitiesOfClass(Projectile.class, mob.getBoundingBox().inflate(7), p -> p.getOwner() != mob)) {
                Vec3 v = p.getDeltaMovement();
                Vec3 to = mob.getBoundingBox().getCenter().subtract(p.position());
                if (v.lengthSqr() > 0.2 && v.normalize().dot(to.normalize()) > 0.85 && mob.getRandom().nextFloat() < skill) {
                    reactCooldown = 25;
                    dodge(t, s);
                    return true;
                }
            }
        }
        return false;
    }

    private static double reach(MoveDef m) {
        return switch (m.kind) {
            case PIERCE, GRAB, FIRE -> m.range;
            case SWEEP -> m.range + 1;
            case DASH, LEAP_SLAM, BLINK -> 12;
            case VOLLEY, LIGHTNING, SPIKES, STORM -> 16;
            default -> m.range + 1.5;
        };
    }

    private void dodge(LivingEntity t, FighterState s) {
        Vec3 to = t.position().subtract(mob.position());
        Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(mob.getRandom().nextBoolean() ? 1 : -1);
        Vec3 v = side.scale(0.95).add(to.normalize().scale(-0.25));
        mob.setDeltaMovement(v.x, 0.28, v.z);
        mob.hurtMarked = true;
        s.dodgeTicks = 8;
        Sync.anim(mob, Anims.DODGE);
        Fx.particles(mob.level(), ParticleTypes.CLOUD, mob.position(), 6, 0.3, 0.02);
    }

    private void guard(FighterState s, int ticks) {
        s.guardTicks = Math.max(8, ticks);
        guardAnim = true;
        CharacterDef d = mob.fighterDef();
        Sync.anim(mob, d.kagune == com.tokyoghoul.data.KaguneType.KOUKAKU ? Anims.K_SHIELD : Anims.GUARD);
    }

    // ------------------------------------------------------------------ decisions

    private void choose(LivingEntity t, FighterState s, CharacterDef def, double dist) {
        float hp = mob.getHealth() / mob.getMaxHealth();
        var rnd = mob.getRandom();
        MoveDef ult = def.ultimate();
        if (ult != null && mob.fighterLevel() >= 5 && (hp < 0.5f || ultimateUsed && hp < 0.3f) && rnd.nextFloat() < 0.6f
                && dist >= ult.aiMin && dist <= ult.aiMax + 6) {
            if (MoveRunner.start(mob, ult, t) == MoveRunner.Fail.NONE) {
                ultimateUsed = true;
                return;
            }
        }
        List<MoveDef> techniques = new ArrayList<>();
        float total = 0;
        int rank = rank();
        for (MoveDef m : def.moves()) {
            if (m.minRank <= rank) {
                techniques.add(m);
                total += m.aiWeight;
            }
        }
        if (!techniques.isEmpty() && rnd.nextFloat() < 0.62f) {
            for (int tries = 0; tries < 12; tries++) {
                float r = rnd.nextFloat() * total;
                MoveDef pick = techniques.get(techniques.size() - 1);
                for (MoveDef m : techniques) {
                    r -= m.aiWeight;
                    if (r <= 0) {
                        pick = m;
                        break;
                    }
                }
                if (dist < pick.aiMin || dist > pick.aiMax) continue;
                if (pick.kind == MoveDef.Kind.DEVOUR && hp > 0.7f) continue;
                if ((pick.kind == MoveDef.Kind.BUFF || pick.kind == MoveDef.Kind.FORM) && s.transformed()) continue;
                if (MoveRunner.start(mob, pick, t) == MoveRunner.Fail.NONE) return;
            }
        }
        melee(t, dist);
    }

    /** CalculateAttackProcedure: basic attacks by distance band. */
    private void melee(LivingEntity t, double dist) {
        var rnd = mob.getRandom();
        MoveDef pick;
        if (dist > 10) pick = rnd.nextBoolean() ? Moves.RUN_ATTACK : Moves.JUMP_ATTACK;
        else if (dist > 5) pick = rnd.nextFloat() < 0.6f ? Moves.JUMP_ATTACK : Moves.RUN_ATTACK;
        else if (dist > 3.2) pick = rnd.nextBoolean() ? Moves.KNOCKBACK : Moves.RUN_ATTACK;
        else {
            float r = rnd.nextFloat();
            pick = r < 0.5f ? Moves.ATTACK : r < 0.75f ? Moves.KNOCKBACK : Moves.BARRAGE;
        }
        if (dist > pick.aiMax + 1) return;
        if (MoveRunner.start(mob, pick, t) != MoveRunner.Fail.NONE && dist < 3.2) {
            MoveRunner.start(mob, Moves.ATTACK, t);
        }
    }
}
