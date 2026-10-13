package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Malgrave the Dread Knight: a fallen paladin who still fights like a duellist. He singles out the
 * strongest defender with an <b>Oath of Ruin</b> (they are marked; everyone else's blows glance off his
 * plate a little), reads attacks with <b>Black Guard</b> and answers every blow that lands on it with a
 * lightning <b>Riposte</b>. His greatsword work is all telegraphed: the Ruin Combo (two sweeps and an
 * overhead lane), the Shadow Lunge, and Grave Rend, a travelling eruption of shadow blades.
 * At half health he kneels, and rises wreathed in <b>soulfire</b>: Soulfire Eruption rings and the
 * Execution Leap onto his chosen foe join the kit, and his footsteps burn.
 */
public class MalgraveBoss extends SiegeBoss {
    public static final int COMBO = 3, CHARGE = 4, WAVE = 5, SUMMON = 6, ERUPTION = 7, GUARD = 8, EXECUTE = 9, CHALLENGE = 10,
            RIPOSTE = 11, ASCEND = 12;
    public static final int SHADOW = 0x9A40E0, SOULFIRE = 0x40D8FF;
    private Vec3 dir = Vec3.ZERO, leapFrom = Vec3.ZERO, leapTo = Vec3.ZERO;
    @Nullable
    private LivingEntity foe;
    private int duelTicks;
    @Nullable
    private LivingEntity riposteAt;
    /** Damage absorbed recently: lots of pressure makes him raise his guard. */
    private float pressure;

    public MalgraveBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.MALGRAVE);
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.HOLY ? 1.5f : e == Element.PHYSICAL ? 0.85f : super.elementMultiplier(e);
    }

    @Override
    public int dangerColor() {
        return phase() >= 2 ? SOULFIRE : SHADOW;
    }

    @Override
    protected String epithet() {
        return "Oathbreaker of the Last Light";
    }

    @Override
    protected int introLength() {
        return 50;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"Kneel. The light abandoned you as surely as it abandoned me.",
                    "I swore once to guard these walls. Now I swear to break them."};
            case TAUNT -> new String[]{"Your blade wavers.", "Is this the best your bastion can muster?", "Every oath breaks. Yours will too."};
            case PHASE -> new String[]{"Soulfire... burn away what is left of me!", "You wanted the knight? Here is what remains of him!"};
            case STAGGER -> new String[]{"Hrk-! ...a worthy blow.", "You fight like... like we used to."};
            case KILL -> new String[]{"Another oath, broken.", "Rest. You fought with honour.", "Next."};
            case DEATH -> new String[]{"At last... the oath... is ended.", "Tell them... I held the line... once."};
            case CORE -> new String[]{"This light. I will snuff it out myself."};
        };
    }

    @Override
    protected void introTick(ServerLevel level, int t) {
        if (t == 2) Vfx.pillar(level, position(), SHADOW, 1.6f, 6, 40);
        if (t == 26) {
            Fx.sound(level, position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 2f, 0.5f);
            Vfx.spray(level, FxStyle.SOUL, position().add(0, 0.2, 0), SHADOW, 0x2A0A3A, 0.3f, 24, 0.25f, -0.6f);
        }
        if (t == 40) {
            Fx.sound(level, position(), SoundEvents.WITHER_SPAWN, 1.2f, 1.4f);
            Vfx.slash(level, position().add(0, getBbHeight() * 0.6, 0), getYRot(), 20, 200, 3.2f, SHADOW, 0xFFFFFF, 10, 0.3f);
        }
    }

    // ------------------------------------------------------------------ the duel

    @Nullable
    @Override
    protected LivingEntity chooseTarget(ServerLevel level) {
        if (foe != null && foe.isAlive() && duelTicks > 0 && distanceToSqr(foe) < 28 * 28) return foe;
        return super.chooseTarget(level);
    }

    @Nullable
    private LivingEntity strongestFoe(ServerLevel level) {
        LivingEntity best = null;
        double bestScore = 0;
        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 22)) {
            if (e instanceof TurretEntity) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            double score = e.getMaxHealth() + (e instanceof Player ? 30 : 0) - distanceTo(e);
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isDeadOrDying()) {
            Entity atk = source.getEntity();
            boolean guarding = skill == GUARD && skillTick >= 4 && skillTick <= 46;
            if (guarding && atk instanceof LivingEntity le && le != this) {
                amount *= 0.15f;
                if (riposteAt == null) riposteAt = le;
                if (level() instanceof ServerLevel sl) {
                    Vfx.flash(sl, position().add(0, getBbHeight() * 0.6, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(0.8)), SHADOW, 1.6f, 5);
                    Fx.sound(sl, position(), SoundEvents.SHIELD_BLOCK, 1.6f, 0.6f);
                }
            } else if (duelTicks > 0 && foe != null && foe.isAlive() && atk != foe) {
                amount *= 0.8f;
            }
            pressure += amount;
        }
        return super.hurt(source, amount);
    }

    // ------------------------------------------------------------------ skills

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (phase() >= 2 && t != null && dt > 5 && dt < 20 && ready(EXECUTE)) return EXECUTE;
        if (ready(CHALLENGE) && tickCount > 160 && (foe == null || !foe.isAlive() || duelTicks <= 0)) {
            LivingEntity f = strongestFoe(level);
            if (f != null) {
                foe = f;
                return CHALLENGE;
            }
        }
        if (ready(GUARD) && pressure > getMaxHealth() * 0.035f && t != null && dt < 9) return GUARD;
        if (ready(SUMMON) && tickCount > 300) return SUMMON;
        if (phase() >= 2 && ready(ERUPTION) && SiegeDamage.defendersAround(level, position(), 9).size() >= 2) return ERUPTION;
        if (t != null && dt < 4.6 && ready(COMBO)) return COMBO;
        if (t != null && dt > 6 && dt < 18 && ready(CHARGE)) return CHARGE;
        double d = t != null ? dt : dc;
        if (d > 4 && d < 16 && ready(WAVE)) return WAVE;
        if (t != null && random.nextInt(500) == 0) speak(Lines.TAUNT, false);
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case COMBO -> {
                if (target != null && t < 30) face(target.position());
                if (t == 2 || t == 14) teleCone(level, position(), forward(3), 4.4f, 75, 10);
                if (t == 12) sweep(level, 1f, 1);
                if (t == 24) sweep(level, 1f, -1);
                if (t == 28) {
                    dir = Vec3.directionFromRotation(0, getYRot());
                    teleLine(level, position(), position().add(dir.scale(8)), 2.2f, 14);
                }
                if (t == 42) overhead(level);
                if (t >= 56) {
                    cooldown(COMBO, 100);
                    return true;
                }
            }
            case CHARGE -> {
                if (t == 1) {
                    dir = aimPoint().subtract(position()).multiply(1, 0, 1).normalize();
                    face(position().add(dir));
                    leapFrom = position();
                    teleLine(level, position(), position().add(dir.scale(17)), 2.4f, 14);
                    Fx.sound(level, position(), SoundEvents.WITHER_SHOOT, 1.5f, 0.5f);
                }
                if (t > 14 && t < 30) {
                    setDeltaMovement(dir.x * 1.1, getDeltaMovement().y, dir.z * 1.1);
                    face(position().add(dir));
                    Vfx.spray(level, FxStyle.SOUL, position().add(0, 1.2, 0), dangerColor(), 0x1A0A2A, 0.35f, 4, 0.08f, 0.2f);
                    if (t % 5 == 0) {
                        HazardZone.Kind k = phase() >= 2 ? HazardZone.Kind.SOUL_FIRE : HazardZone.Kind.SHADOW_POOL;
                        HazardZone.spawn(level, k, position(), 1.3f, 70, damageScale * 0.6f, this, true);
                    }
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 2.3)) {
                        if (e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.2f), Element.SHADOW, false);
                        SiegeDamage.push(e, position(), 1.0, 0.5);
                    }
                    if (t % 3 == 0) crushBlocks(level);
                }
                if (t == 30) {
                    Vfx.dash(level, leapFrom.add(0, 1.2, 0), position().add(0, 1.2, 0), dangerColor(), 1.3f, 14);
                    sweep(level, 0.9f, 1);
                }
                if (t >= 40) {
                    cooldown(CHARGE, 160);
                    return true;
                }
            }
            case WAVE -> {
                if (t == 1) {
                    dir = aimPoint().subtract(position()).multiply(1, 0, 1).normalize();
                    teleLine(level, position(), position().add(dir.scale(16)), 2.4f, 20);
                }
                face(position().add(dir));
                if (t == 19) {
                    Fx.sound(level, position(), SoundEvents.ANVIL_LAND, 1.5f, 0.4f);
                    Vfx.shake(level, position(), 0.5f, 14, 6);
                    Vfx.wave(level, position().add(dir.scale(1)), position().add(dir.scale(16)), dangerColor(), 2.2f, 2.6f, 22);
                }
                if (t >= 20 && t < 40) {
                    double dist = (t - 19) * 0.8;
                    Vec3 at = position().add(dir.scale(dist));
                    Vfx.spray(level, phase() >= 2 ? FxStyle.FLAME : FxStyle.SOUL, at.add(0, 0.3, 0), dangerColor(), 0x1A0A2A, 0.35f, 6, 0.25f, 1f);
                    if (t % 2 == 0) Vfx.spray(level, FxStyle.SHARD, at.add(0, 0.2, 0), 0x2A2A34, 0x9A40E0, 0.3f, 4, 0.35f, 1f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, at, 1.8)) {
                        if (e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.1f), Element.SHADOW, false);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                        e.hurtMarked = true;
                    }
                    if (t % 4 == 0) Fx.sound(level, at, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.6f, 0.5f);
                }
                if (t >= 46) {
                    cooldown(WAVE, 140);
                    return true;
                }
            }
            case SUMMON -> {
                if (t == 2) say(random.nextBoolean() ? "Rise, brothers. One more oath to break." : "To me, my fallen order!");
                Vec3 a = position().add(Vec3.directionFromRotation(0, getYRot() + 90).scale(3));
                Vec3 b = position().add(Vec3.directionFromRotation(0, getYRot() - 90).scale(3));
                if (t == 4) {
                    Vfx.sigil(level, a.add(0, 0.05, 0), SHADOW, 1.6f, 36, 2);
                    Vfx.sigil(level, b.add(0, 0.05, 0), SHADOW, 1.6f, 36, -2);
                    Fx.sound(level, position(), SoundEvents.EVOKER_PREPARE_SUMMON, 2f, 0.5f);
                }
                if (t == 26) {
                    for (Vec3 p : new Vec3[]{a, b}) {
                        Vfx.pillar(level, p, SHADOW, 0.9f, 4, 16);
                        Vfx.spray(level, FxStyle.DEBRIS, p, 0x3A3028, 0x2A2420, 0.3f, 10, 0.3f, 1f);
                        SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.BULWARK, ZombieRank.byId(2 + phase()), 0, p, true, true);
                        if (z != null) z.setMinion(true);
                    }
                }
                if (t >= 40) {
                    cooldown(SUMMON, 560);
                    return true;
                }
            }
            case ERUPTION -> {
                for (int i = 0; i < 3; i++) {
                    float r = 3 + i * 3;
                    int tele = 1 + i * 9;
                    if (t == tele) teleDonut(level, position(), Math.max(0, r - 1.3f), r + 1.3f, 16);
                    if (t == tele + 16) {
                        Vfx.ring(level, position().add(0, 0.1, 0), SOULFIRE, r + 1.3f, 12, 1.4f);
                        Vfx.spray(level, FxStyle.FLAME, position().add(0, 0.3, 0), SOULFIRE, 0x1A60FF, 0.4f, 20 + i * 10, 0.3f + i * 0.15f, -0.3f);
                        Fx.sound(level, position(), SoundEvents.BLAZE_SHOOT, 1.5f, 0.4f + i * 0.1f);
                        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), r + 1.5)) {
                            double d = Math.sqrt(e.distanceToSqr(getX(), e.getY(), getZ()));
                            if (Math.abs(d - r) > 1.4) continue;
                            SiegeDamage.deal(e, this, this, dmg(1f), Element.FIRE, false);
                            SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 80, 2);
                            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.5, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
                if (t >= 50) {
                    cooldown(ERUPTION, 200);
                    return true;
                }
            }
            case GUARD -> {
                if (target != null) face(target.position());
                if (t == 2) {
                    Vfx.dome(level, position(), SHADOW, 1.7f, 46, this);
                    Fx.sound(level, position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 2f, 0.6f);
                    if (random.nextBoolean()) say(random.nextBoolean() ? "Strike me. I dare you." : "Come. Show me your form.");
                }
                if (riposteAt != null && t >= 4) {
                    startSkill(RIPOSTE);
                    return false;
                }
                if (t >= 48) {
                    pressure = 0;
                    cooldown(GUARD, 300);
                    return true;
                }
            }
            case RIPOSTE -> {
                LivingEntity v = riposteAt;
                if (t == 1) {
                    pressure = 0;
                    cooldown(GUARD, 300);
                    if (v != null && v.isAlive()) {
                        face(v.position());
                        double d = distanceTo(v);
                        if (d > 3.5 && d < 10) {
                            Vec3 to = v.position().add(position().subtract(v.position()).normalize().scale(2));
                            Vfx.dash(level, position().add(0, 1.2, 0), to.add(0, 1.2, 0), SHADOW, 1.2f, 10);
                            teleportTo(to.x, v.getY(), to.z);
                        }
                    }
                    Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.6f, 0.5f);
                }
                if (t == 5) {
                    if (v != null && v.isAlive() && distanceTo(v) < 4.5) {
                        SiegeDamage.deal(v, this, this, dmg(1.9f), Element.SHADOW, false);
                        SiegeDamage.push(v, position(), 1.6, 0.5);
                        SiegeDamage.apply(v, ModEffects.CONCUSSED.get(), 40, 0);
                        Vfx.flash(level, v.position().add(0, v.getBbHeight() * 0.5, 0), dangerColor(), 2f, 6);
                    } else if (v != null && v.isAlive()) {
                        // they hit from range: answer with a hurled shadow crescent
                        Vec3 from = position().add(0, getBbHeight() * 0.6, 0);
                        SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.SHADOW_ORB, this, from);
                        p.payload(dmg(1.2f), Element.SHADOW).homing(v, 0.2f).aim(v.position().add(0, 1, 0), 1.4f).scale(1.6f).fire(level);
                    }
                    Vfx.slash(level, position().add(0, getBbHeight() * 0.55, 0), getYRot(), 10, 140, 3.6f, dangerColor(), 0xFFFFFF, 8, 0.35f);
                    Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.6f, 0.6f);
                }
                if (t >= 18) {
                    riposteAt = null;
                    return true;
                }
            }
            case EXECUTE -> {
                if (t == 1) {
                    LivingEntity v = foe != null && foe.isAlive() && distanceTo(foe) < 20 ? foe : target;
                    leapFrom = position();
                    leapTo = v != null ? v.position() : forward(8);
                    face(leapTo);
                    teleCircle(level, leapTo, 3.6f, 26);
                    say(random.nextBoolean() ? "JUDGEMENT!" : "Your sentence is death!");
                }
                if (t >= 6 && t <= 26) {
                    float k = (t - 6) / 20f;
                    Vec3 p = leapFrom.lerp(leapTo, k).add(0, Mth.sin(k * Mth.PI) * 7, 0);
                    setPos(p.x, p.y, p.z);
                    setDeltaMovement(Vec3.ZERO);
                    if (t % 2 == 0) Vfx.spray(level, FxStyle.FLAME, p.add(0, 1, 0), SOULFIRE, 0x1A60FF, 0.35f, 4, 0.06f, 0);
                }
                if (t == 26) {
                    Vfx.ring(level, position().add(0, 0.1, 0), SOULFIRE, 4.5f, 14, 1.6f);
                    Vfx.pillar(level, position(), SOULFIRE, 1.4f, 7, 14);
                    Vfx.spray(level, FxStyle.DEBRIS, position(), 0x3A3A3A, 0x2A2A2A, 0.4f, 24, 0.5f, 0.8f);
                    Vfx.shake(level, position(), 1f, 20, 12);
                    Fx.sound(level, position(), SoundEvents.GENERIC_EXPLODE, 2f, 0.6f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 3.8)) {
                        SiegeDamage.deal(e, this, this, dmg(e == foe ? 2.6f : 2f), Element.FIRE, false);
                        SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 100, 3);
                        SiegeDamage.push(e, position(), 1.4, 0.7);
                    }
                    HazardZone.spawn(level, HazardZone.Kind.SOUL_FIRE, position(), 3f, 100, damageScale * 0.7f, this, true);
                    crushBlocks(level);
                }
                if (t >= 46) {
                    cooldown(EXECUTE, 260);
                    return true;
                }
            }
            case CHALLENGE -> {
                if (foe != null && foe.isAlive()) face(foe.position());
                if (t == 12 && foe != null && foe.isAlive()) {
                    duelTicks = 400;
                    SiegeDamage.apply(foe, ModEffects.MARKED.get(), 400, 1);
                    Vfx.tether(level, this, getBbHeight() * 0.7f, foe, SHADOW, 0.08f, 24);
                    Vfx.sigil(level, foe.position().add(0, 0.05, 0), SHADOW, 1.4f, 60, 3);
                    Fx.sound(level, position(), SoundEvents.WITHER_AMBIENT, 1.4f, 0.7f);
                    String who = foe.getDisplayName().getString();
                    say(foe instanceof Player ? who + ". Face me." : "You, " + who + ". Your oath against mine.");
                    for (ServerPlayer p : level.players()) {
                        if (p.distanceToSqr(this) < 64 * 64) {
                            p.displayClientMessage(Component.literal("Malgrave swears an Oath of Ruin against " + who + "! Others deal 20% less damage to him.")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                        }
                    }
                }
                if (t >= 26) {
                    cooldown(CHALLENGE, 700);
                    return true;
                }
            }
            case ASCEND -> {
                if (t == 4) Vfx.sigil(level, position().add(0, 0.05, 0), SOULFIRE, 3f, 50, 2);
                if (t == 30) {
                    Vfx.pillar(level, position(), SOULFIRE, 1.8f, 16, 30);
                    Vfx.spray(level, FxStyle.FLAME, position().add(0, 1, 0), SOULFIRE, 0x1A60FF, 0.5f, 50, 0.4f, 0.8f);
                    Vfx.screenFlash(level, position(), SOULFIRE, 0.25f, 50, 12);
                    Fx.sound(level, position(), SoundEvents.BLAZE_SHOOT, 2f, 0.3f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 6)) SiegeDamage.push(e, position(), 1.4, 0.5);
                }
                return t >= 50;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    /** Horizontal greatsword sweep. {@code side} 1 = right to left. */
    private void sweep(ServerLevel level, float mult, int side) {
        Vec3 c = forward(2.4).add(0, 1, 0);
        Vfx.slash(level, position().add(0, getBbHeight() * 0.5, 0), getYRot(), side * 8, side * 160, 4.2f, dangerColor(), 0xFFFFFF, 9, 0.35f);
        for (LivingEntity e : SiegeDamage.defendersAround(level, c, 3.1)) {
            SiegeDamage.deal(e, this, this, dmg(mult), Element.PHYSICAL, false);
            if (phase() >= 2) SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 80, 2);
            SiegeDamage.push(e, position(), 0.7, 0.2);
        }
        Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
    }

    /** The overhead lane at the end of the Ruin Combo. */
    private void overhead(ServerLevel level) {
        Vfx.slash(level, position().add(0, getBbHeight() * 0.6, 0), getYRot(), 90, 170, 4.4f, dangerColor(), 0xFFFFFF, 10, 0.35f);
        Vfx.wave(level, forward(1), forward(8), dangerColor(), 1.8f, 1.6f, 12);
        Vfx.shake(level, position(), 0.6f, 16, 8);
        for (LivingEntity e : SiegeDamage.defendersAround(level, forward(4), 4.5)) {
            Vec3 rel = e.position().subtract(position()).multiply(1, 0, 1);
            double along = rel.dot(dir);
            if (along < 0 || along > 8.5 || rel.subtract(dir.scale(along)).lengthSqr() > 1.8) continue;
            SiegeDamage.deal(e, this, this, dmg(1.8f), Element.PHYSICAL, false);
            SiegeDamage.push(e, position(), 1.4, 0.6);
        }
        if (phase() >= 2) {
            for (int i = 1; i <= 3; i++) HazardZone.spawn(level, HazardZone.Kind.SOUL_FIRE, position().add(dir.scale(i * 2.4)), 1.3f, 80, damageScale * 0.5f, this, true);
        }
        Fx.sound(level, forward(3), SoundEvents.ANVIL_LAND, 1.4f, 0.5f);
    }

    @Override
    protected void meleeImpact(ServerLevel level) {
        sweep(level, 1f, random.nextBoolean() ? 1 : -1);
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        pressure = Math.max(0, pressure * 0.985f - 0.02f);
        if (duelTicks > 0) {
            duelTicks--;
            if (foe == null || !foe.isAlive()) {
                if (foe != null && !foe.isAlive()) {
                    heal(getMaxHealth() * 0.05f);
                    Vfx.spray(level, FxStyle.SOUL, position().add(0, 1.5, 0), dangerColor(), 0xFFFFFF, 0.35f, 20, 0.2f, 0.6f);
                }
                duelTicks = 0;
                foe = null;
            } else if (tickCount % 40 == 0) {
                Vfx.tether(level, this, getBbHeight() * 0.7f, foe, SHADOW, 0.03f, 8);
            }
        }
        if (phase() >= 2) {
            if (tickCount % 5 == 0 && getDeltaMovement().horizontalDistanceSqr() > 0.002) {
                HazardZone.spawn(level, HazardZone.Kind.SOUL_FIRE, position(), 1.1f, 60, damageScale * 0.4f, this, true);
            }
            if (tickCount % 4 == 0) {
                Vec3 blade = forward(1.2).add(0, getBbHeight() * 0.55, 0);
                Vfx.spray(level, FxStyle.FLAME, blade, SOULFIRE, 0x1A60FF, 0.25f, 2, 0.04f, 1f);
            }
        } else if (tickCount % 8 == 0) {
            Vfx.spray(level, FxStyle.SOUL, position().add(0, 0.2, 0), SHADOW, 0x1A0A2A, 0.25f, 1, 0.03f, 0.5f);
        }
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        super.onPhase(level, phase);
        startSkill(ASCEND);
    }

    @Override
    protected String phaseLine(int phase) {
        return "kneels... and rises wreathed in SOULFIRE!";
    }
}
