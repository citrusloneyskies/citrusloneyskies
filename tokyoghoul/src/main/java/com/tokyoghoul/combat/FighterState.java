package com.tokyoghoul.combat;

import com.tokyoghoul.move.MoveDef;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transient combat state for any fighter, player or NPC. It plays the role of Jujutsu Craft's
 * persistent-data counters ({@code skill}, {@code cnt1..cnt6}, {@code PRESS_Z}) but as typed fields:
 * the running move and its tick, cooldowns, hit-stun, guard, transformation and grab state.
 */
public final class FighterState {
    @Nullable
    public MoveDef move;
    public int moveTick;
    /** Z (or the AI's equivalent) is still held, which extends barrages and breath attacks. */
    public boolean holding;
    /** Locked aim for the current move (players aim with the camera, NPCs at their target). */
    public Vec3 aim = new Vec3(0, 0, 1);
    @Nullable
    public LivingEntity moveTarget;
    /** Entities already struck by the current dash, so a dash hits each once. */
    public final Set<Integer> struck = new HashSet<>();
    @Nullable
    public LivingEntity grabbed;

    /** Move id -> game time at which it is ready again. */
    public final Map<String, Long> cooldowns = new HashMap<>();
    /** Short lockout after any move so moves cannot be chained frame-perfectly. */
    public long globalReady;

    /** Hit-stun: no moves, no AI decisions, slowed. */
    public int stun;
    /** Guard held by the player (G) or chosen by the AI. */
    public boolean guarding;
    public int guardTicks;
    /** GUARD_COUNTER stance: the move that strikes back if hit during the window. */
    @Nullable
    public MoveDef counter;
    public int counterTicks;
    /** Dodge invulnerability frames. */
    public int dodgeTicks;

    /** Active transformation (kakuja, Arata, White Reaper) and its remaining ticks. */
    @Nullable
    public MoveDef form;
    public int formTicks;
    /** Active self-buff. */
    @Nullable
    public MoveDef buff;
    public int buffTicks;
    /** Ticks the kagune stays out after its last use (auto-retracts). */
    public int kaguneTicks;
    /** The player toggled the kagune out with C (stays until toggled again). */
    public boolean kaguneToggled;

    /** Current one-shot animation, mirrored to clients. */
    public int anim;

    public boolean busy() {
        return move != null;
    }

    public boolean onCooldown(MoveDef d, long now) {
        Long t = cooldowns.get(d.id);
        return t != null && t > now;
    }

    public int cooldownLeft(MoveDef d, long now) {
        Long t = cooldowns.get(d.id);
        return t == null ? 0 : (int) Math.max(0, t - now);
    }

    public boolean guardingNow() {
        return guarding || guardTicks > 0 || counterTicks > 0;
    }

    public boolean transformed() {
        return form != null && formTicks > 0;
    }

    public void clearMove() {
        move = null;
        moveTick = 0;
        holding = false;
        moveTarget = null;
        struck.clear();
        grabbed = null;
    }
}
