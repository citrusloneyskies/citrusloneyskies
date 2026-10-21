package com.tokyoghoul.data;

import com.tokyoghoul.move.Anims;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.move.MoveDef.Kind;
import com.tokyoghoul.move.MoveDef.Shot;
import com.tokyoghoul.move.MoveDef.Source;

import java.util.List;

/**
 * Universal attacks every fighter has (JJK's Attack / Knockback Attack / Barrage Attack slots 0-2) plus
 * the extra melee options the AI mixes in (Jump Attack, Run Attack), and shorthand builders for the
 * character moves in {@link Characters}.
 */
public final class Moves {
    public static final MoveDef ATTACK = MoveDef.of("attack", "Attack", Kind.COMBO).dmg(3).range(2.4f).radius(1.2f).kb(0.3f).count(3)
            .interval(5).cd(10).anim(Anims.JAB).ai(0, 3).weight(3);
    public static final MoveDef KNOCKBACK = MoveDef.of("knockback_attack", "Knockback Attack", Kind.STRIKE).dmg(6).range(2.6f).radius(1.4f)
            .kb(1.9f).lift(0.35f).windup(7).cd(24).anim(Anims.HEAVY).lunge().ai(0, 3.5f).weight(2);
    public static final MoveDef BARRAGE = MoveDef.of("barrage", "Barrage Attack", Kind.BARRAGE).dmg(1.1f).range(2.4f).radius(1.4f)
            .kb(0.08f).duration(26).interval(2).cd(50).anim(Anims.BARRAGE).ai(0, 3).weight(2);
    public static final List<MoveDef> UNIVERSAL = List.of(ATTACK, KNOCKBACK, BARRAGE);

    /** Extra melee the AI uses to close distance (JJK's Jump Attack and Run Attack). */
    public static final MoveDef JUMP_ATTACK = MoveDef.of("jump_attack", "Jump Attack", Kind.LEAP_SLAM).dmg(5).radius(2.6f).kb(1.0f)
            .duration(26).cd(70).ai(5, 18).weight(1);
    public static final MoveDef RUN_ATTACK = MoveDef.of("run_attack", "Run Attack", Kind.DASH).dmg(5).radius(1.4f).kb(1.2f).duration(12)
            .speed(1.1f).cd(60).anim(Anims.DASH).ai(4, 14).weight(1);
    public static final List<MoveDef> AI_MELEE = List.of(ATTACK, KNOCKBACK, BARRAGE, JUMP_ATTACK, RUN_ATTACK);

    private Moves() {
    }

    // ------------------------------------------------------------------ shorthand

    static MoveDef k(String id, String name, Kind kind) {
        return MoveDef.of(id, name, kind).src(Source.KAGUNE);
    }

    static MoveDef q(String id, String name, Kind kind) {
        return MoveDef.of(id, name, kind).src(Source.QUINQUE);
    }

    static MoveDef b(String id, String name, Kind kind) {
        return MoveDef.of(id, name, kind).src(Source.BODY);
    }

    /** Standard Rinkaku tentacle stab. */
    static MoveDef rinkakuPierce(String id, String name) {
        return k(id, name, Kind.PIERCE).dmg(8).range(9).radius(1.1f).count(3).interval(5).kb(0.4f).cost(25).cd(40);
    }

    /** Standard Ukaku crystal shard volley. */
    static MoveDef ukakuVolley(String id, String name) {
        return k(id, name, Kind.VOLLEY).shot(Shot.SHARD).dmg(3.5f).count(10).interval(2).speed(2.0f).cost(20).cd(50).anim(Anims.K_VOLLEY);
    }

    /** Standard Bikaku tail strike. */
    static MoveDef tailStrike(String id, String name) {
        return k(id, name, Kind.PIERCE).dmg(9).range(6).radius(1.4f).count(1).windup(6).kb(1.2f).cost(15).cd(30).ai(1, 6);
    }

    /** Standard kagune spin around the user. */
    static MoveDef kaguneSweep(String id, String name) {
        return k(id, name, Kind.SWEEP).dmg(7).range(4.5f).count(2).interval(6).kb(1.3f).cost(25).cd(60);
    }

    static MoveDef kakuja(String id, String name, float scale) {
        return k(id, name, Kind.FORM).ultimate(true).form(scale, 1.7f, 0.5f).duration(900).cost(150).cd(2400).rank(5).weight(4);
    }
}
