package com.tokyoghoul.move;

/**
 * Data for one technique. Jujutsu Craft hand-writes a procedure per technique and finds it with a
 * {@code skill == 206} chain; here every move is one of a few dozen behaviours ({@link Kind}) tuned with
 * parameters, so the whole roster is plain data that players and NPCs share.
 */
public final class MoveDef {
    public enum Kind {
        /** One wind-up and one hit in front, with an optional lunge. */
        STRIKE,
        /** A chain of quick hits; the last one knocks back. */
        COMBO,
        /** Rapid hits for as long as Z is held (NPCs: a fixed time). */
        BARRAGE,
        /** Kagune/lance thrust that hits everything along a line. */
        PIERCE,
        /** Hits all around the user; repeats {@code count} times. */
        SWEEP,
        /** Fires projectiles forward over time. */
        VOLLEY,
        /** Bursts projectiles in every direction. */
        STORM,
        /** Charges forward, hitting everything in the path. */
        DASH,
        /** Jumps at the target and crashes down with a shockwave. */
        LEAP_SLAM,
        /** Grabs the target in front, lifts it and slams it down. */
        GRAB,
        /** Spikes erupt from the ground in a line toward the target. */
        SPIKES,
        /** Lightning bolts that chain between enemies (Narukami, Ukaku electricity). */
        LIGHTNING,
        /** A cone of fire breath (Tatara). */
        FIRE,
        /** Guard stance that counter-attacks when struck. */
        GUARD_COUNTER,
        /** Transformation: kakuja, Arata armour, Shinigami. Big buffs for a time. */
        FORM,
        /** Self-buff without transformation. */
        BUFF,
        /** Bite the target: damage that heals the user and feeds Rc. */
        DEVOUR,
        /** Teleport-speed step behind the target followed by a strike. */
        BLINK
    }

    /** Where the attack comes from; decides visuals and which damage type it deals. */
    public enum Source {
        BODY, KAGUNE, QUINQUE, FIRE, LIGHTNING
    }

    /** Projectile looks for {@link Kind#VOLLEY} / {@link Kind#STORM}. */
    public enum Shot {
        SHARD, KNIFE, SPEAR, LIGHTNING, FLAME, BULLET
    }

    public final String id;
    public final String name;
    public final Kind kind;
    public Source source = Source.BODY;
    public Shot shot = Shot.SHARD;
    public float damage = 4, range = 2.6f, radius = 1.3f, knockback = 0.5f, speed = 1.4f, lift = 0.15f;
    public int count = 1, interval = 4, duration = 20, windup = 6;
    public int cost, cooldown = 20, minRank = 1, anim = Anims.HEAVY;
    /** FX colour; -1 means "use the character's kagune colour". */
    public int color = -1;
    /** Ultimate moves need kakuja progress (ghouls) and sit in the domain slot of the rotation. */
    public boolean ultimate, needsKakuja, hold, lunge;
    /** FORM: model scale while transformed, damage multiplier and damage taken multiplier. */
    public float formScale = 1.15f, formDamage = 1.5f, formArmor = 0.6f;
    /** AI: preferred distance band and how often it picks this move. */
    public float aiMin = 0, aiMax = 4, aiWeight = 1;

    private MoveDef(String id, String name, Kind kind) {
        this.id = id;
        this.name = name;
        this.kind = kind;
    }

    public static MoveDef of(String id, String name, Kind kind) {
        MoveDef d = new MoveDef(id, name, kind);
        switch (kind) {
            case STRIKE -> d.anim(Anims.HEAVY).windup(6).cd(30);
            case COMBO -> d.anim(Anims.JAB).count(3).interval(5).windup(2).cd(20);
            case BARRAGE -> d.anim(Anims.BARRAGE).duration(24).interval(2).windup(4).cd(60).hold();
            case PIERCE -> d.anim(Anims.K_PIERCE).range(8).radius(1.1f).windup(7).cd(50).ai(2, 9);
            case SWEEP -> d.anim(Anims.K_SWEEP).range(4).windup(6).kb(1.2f).cd(60).ai(0, 4);
            case VOLLEY -> d.anim(Anims.K_VOLLEY).count(8).interval(2).windup(6).speed(1.8f).cd(60).ai(5, 24);
            case STORM -> d.anim(Anims.K_VOLLEY).count(24).windup(12).speed(1.4f).cd(200).ai(0, 10);
            case DASH -> d.anim(Anims.DASH).duration(10).speed(1.25f).windup(4).cd(60).ai(4, 14);
            case LEAP_SLAM -> d.anim(Anims.LEAP).duration(30).radius(3.5f).windup(4).kb(1.4f).cd(80).ai(5, 16);
            case GRAB -> d.anim(Anims.K_GRAB).range(6).duration(28).windup(8).cd(120).ai(1, 6);
            case SPIKES -> d.anim(Anims.K_SPIKES).count(8).interval(2).range(1.6f).windup(8).cd(100).ai(3, 14);
            case LIGHTNING -> d.anim(Anims.CAST).count(5).interval(4).windup(8).speed(2.6f).cd(100).ai(4, 26);
            case FIRE -> d.anim(Anims.BREATH).duration(40).range(7).windup(8).cd(160).ai(1, 7);
            case GUARD_COUNTER -> d.anim(Anims.GUARD).duration(40).windup(0).cd(100).ai(0, 4);
            case FORM -> d.anim(Anims.TRANSFORM).duration(600).windup(30).cd(1800).ai(0, 30);
            case BUFF -> d.anim(Anims.CHARGE).duration(400).windup(20).cd(1200).ai(0, 30);
            case DEVOUR -> d.anim(Anims.BITE).range(2.6f).windup(6).cd(160).ai(0, 3);
            case BLINK -> d.anim(Anims.SLASH).range(14).windup(3).cd(80).ai(4, 14);
        }
        return d;
    }

    // ------------------------------------------------------------------ fluent setters

    public MoveDef src(Source s) {
        this.source = s;
        return this;
    }

    public MoveDef shot(Shot s) {
        this.shot = s;
        return this;
    }

    public MoveDef dmg(float d) {
        this.damage = d;
        return this;
    }

    public MoveDef range(float r) {
        this.range = r;
        return this;
    }

    public MoveDef radius(float r) {
        this.radius = r;
        return this;
    }

    public MoveDef kb(float k) {
        this.knockback = k;
        return this;
    }

    public MoveDef speed(float s) {
        this.speed = s;
        return this;
    }

    public MoveDef lift(float l) {
        this.lift = l;
        return this;
    }

    public MoveDef count(int c) {
        this.count = c;
        return this;
    }

    public MoveDef interval(int i) {
        this.interval = Math.max(1, i);
        return this;
    }

    public MoveDef duration(int d) {
        this.duration = d;
        return this;
    }

    public MoveDef windup(int w) {
        this.windup = w;
        return this;
    }

    public MoveDef cost(int c) {
        this.cost = c;
        return this;
    }

    public MoveDef cd(int c) {
        this.cooldown = c;
        return this;
    }

    public MoveDef rank(int r) {
        this.minRank = r;
        return this;
    }

    public MoveDef anim(int a) {
        this.anim = a;
        return this;
    }

    public MoveDef color(int c) {
        this.color = c;
        return this;
    }

    public MoveDef hold() {
        this.hold = true;
        return this;
    }

    public MoveDef lunge() {
        this.lunge = true;
        return this;
    }

    /** Mark as the character's ultimate (kakuja, full release...). */
    public MoveDef ultimate(boolean needsKakuja) {
        this.ultimate = true;
        this.needsKakuja = needsKakuja;
        return this;
    }

    public MoveDef form(float scale, float damage, float armor) {
        this.formScale = scale;
        this.formDamage = damage;
        this.formArmor = armor;
        return this;
    }

    public MoveDef ai(float min, float max) {
        this.aiMin = min;
        this.aiMax = max;
        return this;
    }

    public MoveDef weight(float w) {
        this.aiWeight = w;
        return this;
    }

    /** Copy with a new id/name (for sharing tuned templates between characters). */
    public MoveDef copy(String newId, String newName) {
        MoveDef d = new MoveDef(newId, newName, kind);
        d.source = source;
        d.shot = shot;
        d.damage = damage;
        d.range = range;
        d.radius = radius;
        d.knockback = knockback;
        d.speed = speed;
        d.lift = lift;
        d.count = count;
        d.interval = interval;
        d.duration = duration;
        d.windup = windup;
        d.cost = cost;
        d.cooldown = cooldown;
        d.minRank = minRank;
        d.anim = anim;
        d.color = color;
        d.ultimate = ultimate;
        d.needsKakuja = needsKakuja;
        d.hold = hold;
        d.lunge = lunge;
        d.formScale = formScale;
        d.formDamage = formDamage;
        d.formArmor = formArmor;
        d.aiMin = aiMin;
        d.aiMax = aiMax;
        d.aiWeight = aiWeight;
        return d;
    }

    /** Ticks the move occupies the user before they can act again (players and NPCs). */
    public int busyTicks() {
        return switch (kind) {
            case STRIKE, DEVOUR -> windup + 8;
            case COMBO -> windup + count * interval + 4;
            case BARRAGE, FIRE, GUARD_COUNTER -> windup + duration;
            case PIERCE, SWEEP, SPIKES, VOLLEY, LIGHTNING -> windup + count * interval + 6;
            case STORM -> windup + 10;
            case DASH -> windup + duration + 4;
            case LEAP_SLAM -> duration + 6;
            case GRAB -> windup + duration + 6;
            case FORM, BUFF -> windup + 6;
            case BLINK -> windup + 10;
        };
    }

    public String key() {
        return "move.tokyoghoul." + id;
    }

    /** Display name; translatable, falling back to the canon name. */
    public net.minecraft.network.chat.MutableComponent title() {
        return net.minecraft.network.chat.Component.translatableWithFallback(key(), name);
    }
}
