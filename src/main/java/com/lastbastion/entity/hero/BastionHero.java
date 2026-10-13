package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.entity.zombie.NecromancerZombie;
import com.lastbastion.entity.zombie.ShriekerZombie;
import com.lastbastion.entity.zombie.SpitterZombie;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Bastion, the Stone Colossus: the wall. Haymakers and a double-fist slam; Taunt drags every nearby
 * zombie onto him; Earthshatter drives a lane of stone spikes; Boulder Toss picks off back-liners;
 * Stoneskin hardens him under pressure. Ultimate, Mountain's Wrath: a leap into the thick of it and
 * three waves of erupting stone.
 */
public class BastionHero extends HeroEntity {
    public static final int TAUNT = 6, SHATTER = 7, BOULDER = 8, STONESKIN = 9, SLAM = 10;
    private Vec3 leapTo = Vec3.ZERO;
    private Vec3 lane = Vec3.ZERO;
    @Nullable
    private LivingEntity throwAt;
    private boolean landed;

    public BastionHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.BASTION);
    }

    @Override
    public Role role() {
        return Role.TANK;
    }

    @Override
    protected int attackWindup() {
        return 10;
    }

    @Override
    protected int attackRecover() {
        return 8;
    }

    @Override
    protected int attackInterval() {
        return 22;
    }

    @Override
    protected int comboLength() {
        return 3;
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 2.2;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.12f;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"I. Am. The. Wall.", "Stone wakes. Stone stands."};
            case ULT -> new String[]{"THE MOUNTAIN... FALLS!", "FEEL THE EARTH!"};
            case BOSS -> new String[]{"Big one. Good. Hit me.", "Come. Break yourself on stone."};
            case LOW_HP -> new String[]{"Cracks... only cracks.", "Stone... does not... fall."};
            case LEVEL_UP -> new String[]{"Harder. Heavier. Better."};
            case IDLE -> new String[]{"*moss rustles as he settles*", "...waiting is easy for stone."};
            default -> new String[0];
        };
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        if (combo == 2) {
            int n = arc(level, 3.2, 120, dmg(1.5f), Element.PHYSICAL);
            Vec3 at = forward(2.2);
            Fx.shockwave(level, at, 2.6, 0x8C8577);
            for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 2.6)) {
                if (!(e instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(e, at, 0.5, 0.45);
            }
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.4f, 0.5f);
        } else {
            arc(level, 3, 90, dmg(1f), Element.PHYSICAL);
            if (!(t instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.push(t, position(), 0.9, 0.25);
            Vfx.spray(level, FxStyle.DEBRIS, t.position().add(0, t.getBbHeight() * 0.6, 0), 0x8C8577, 0x5A564C, 0.18f, 6, 0.3f, 0.4f);
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.7f);
        }
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(STONESKIN) && getHealth() < getMaxHealth() * 0.6f && attackers(level) >= 2) return STONESKIN;
        if (ready(TAUNT)) {
            int onAllies = 0;
            for (LivingEntity e : enemiesNear(level, 11)) {
                if (e instanceof Mob m && m.getTarget() != null && m.getTarget() != this && SiegeDamage.isDefender(m.getTarget())) onAllies++;
            }
            if (onAllies >= 2) return TAUNT;
        }
        if (t == null) return 0;
        if (ready(SHATTER) && dist < 9) {
            Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1);
            if (dir.lengthSqr() > 0.01) {
                int inLane = 0;
                Vec3 n = dir.normalize();
                for (LivingEntity e : enemiesNear(level, 10)) {
                    Vec3 rel = e.position().subtract(position()).multiply(1, 0, 1);
                    double along = rel.dot(n);
                    if (along > 0 && along < 10 && rel.subtract(n.scale(along)).lengthSqr() < 2.5) inLane++;
                }
                if (inLane >= 3) {
                    lane = n;
                    return SHATTER;
                }
            }
        }
        if (ready(BOULDER)) {
            for (LivingEntity e : enemiesNear(level, 20)) {
                double d = distanceTo(e);
                if (d > 6 && (e instanceof SpitterZombie || e instanceof NecromancerZombie || e instanceof ShriekerZombie)) {
                    throwAt = e;
                    return BOULDER;
                }
            }
        }
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case TAUNT -> {
                if (t == 8) {
                    playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.5f);
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xFFB040, 11, 18, 1.2f);
                    Vfx.shake(level, position(), 0.5f, 16, 10);
                    for (LivingEntity e : enemiesNear(level, 11)) {
                        if (e instanceof Mob m && !(e instanceof SiegeEnemy se && se.isHighPriority() && random.nextBoolean())) {
                            m.setTarget(this);
                            e.getPersistentData().putInt("lb_taunt", tickCount + 100);
                        }
                    }
                    addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 100, 0));
                }
                if (t >= 24) {
                    cooldown(TAUNT, 260);
                    return true;
                }
            }
            case SHATTER -> {
                faceTowards(position().add(lane));
                if (t == 12) {
                    Vfx.teleLine(level, position(), position().add(lane.scale(10)), 2.5f, 1, 0xFFB040, false);
                    playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f);
                    Vfx.shake(level, position(), 0.7f, 14, 8);
                }
                if (t >= 12 && t < 32 && t % 2 == 0) {
                    double d = (t - 12) / 2.0 + 1.5;
                    Vec3 at = position().add(lane.scale(d));
                    Vfx.spray(level, FxStyle.DEBRIS, at.add(0, 0.3, 0), 0x8C8577, 0x5A564C, 0.3f, 8, 0.35f, 0.9f);
                    Vfx.ring(level, at.add(0, 0.05, 0), 0xB0A890, 1.4f, 8, 0.8f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 1.6)) {
                        if (e.hurtTime > 0) continue;
                        hit(e, dmg(2.5f), Element.PHYSICAL);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) e.setDeltaMovement(e.getDeltaMovement().add(0, 0.65, 0));
                        e.hurtMarked = true;
                    }
                }
                if (t >= 36) {
                    cooldown(SHATTER, 200);
                    return true;
                }
            }
            case BOULDER -> {
                if (throwAt != null) faceTowards(throwAt.position());
                if (t == 4) {
                    Vfx.spray(level, FxStyle.DEBRIS, position().add(look().scale(1.2)), 0x8C8577, 0x5A564C, 0.25f, 12, 0.25f, 0.9f);
                    playSound(SoundEvents.STONE_BREAK, 1.5f, 0.5f);
                }
                if (t == 18 && throwAt != null && throwAt.isAlive()) {
                    Vec3 from = position().add(0, getBbHeight() * 0.95, 0);
                    SiegeProjectile p = SiegeProjectile.create(level, ProjKind.BOULDER, this, from).payload(dmg(3f), Element.PHYSICAL).area(2.6f);
                    p.knockback = 0.8f;
                    p.status(ModEffects.CONCUSSED, 80, 0);
                    p.aim(throwAt.position(), 1.2f).scale(1.4f).fire(level);
                    playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.4f, 0.4f);
                }
                if (t >= 26) {
                    cooldown(BOULDER, 160);
                    return true;
                }
            }
            case STONESKIN -> {
                if (t == 10) {
                    addEffect(new MobEffectInstance(ModEffects.AEGIS.get(), 140, 2));
                    setFlag(true);
                    Vfx.spray(level, FxStyle.SHARD, position().add(0, 1.6, 0), 0xFFC060, 0xB08040, 0.25f, 20, 0.3f, 0.3f);
                    playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
                }
                if (t >= 20) {
                    cooldown(STONESKIN, 480);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    landed = false;
                    Vec3 c = bestCluster(level, 14, 4, 1);
                    leapTo = c != null ? c : target != null ? target.position() : position();
                    faceNow(leapTo);
                    playSound(SoundEvents.RAVAGER_ROAR, 2.5f, 0.4f);
                }
                if (t == 12) {
                    Vec3 d = leapTo.subtract(position());
                    setDeltaMovement(d.x * 0.1, 1.15, d.z * 0.1);
                    hasImpulse = true;
                    Vfx.teleCircle(level, leapTo.add(0, 0.05, 0), 6f, 20, 0xFFB040, false);
                }
                if (t > 16 && t < 60 && (onGround() || t > 50) && !landed) {
                    landed = true;
                    skillTick = 59;
                    Fx.shockwave(level, position(), 6, 0x8C8577);
                    Vfx.screenFlash(level, position(), 0xFFE0B0, 0.15f, 20, 6);
                    for (LivingEntity e : enemiesNear(level, 6)) {
                        hit(e, dmg(4f), Element.PHYSICAL);
                        SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 30, 0);
                    }
                    playSound(SoundEvents.GENERIC_EXPLODE, 2f, 0.5f);
                }
                if (t >= 60 && t <= 84 && (t - 60) % 8 == 0) {
                    float r = 3 + (t - 60) / 8f * 3;
                    Vfx.ring(level, position().add(0, 0.05, 0), 0xB0A890, r + 1, 12, 1.6f);
                    for (int i = 0; i < (int) (r * 2.5f); i++) {
                        double a = Math.PI * 2 * i / (int) (r * 2.5f);
                        Vec3 at = position().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                        Vfx.spray(level, FxStyle.DEBRIS, at, 0x8C8577, 0x5A564C, 0.3f, 3, 0.35f, 1);
                    }
                    for (LivingEntity e : enemiesNear(level, r + 1)) {
                        if (distanceTo(e) < r - 1.5) continue;
                        hit(e, dmg(2.5f), Element.PHYSICAL);
                        if (!(e instanceof SiegeEnemy se && se.isHighPriority())) e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
                        e.hurtMarked = true;
                    }
                    Vfx.shake(level, position(), 0.5f, 20, 6);
                    playSound(SoundEvents.STONE_BREAK, 2f, 0.4f);
                }
                if (t >= 92) return true;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void passiveTick(ServerLevel level) {
        if (flag() && skill != ULT && !hasEffect(ModEffects.AEGIS.get())) setFlag(false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) amount *= 0.75f;
        return super.hurt(source, amount);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1f, 0.6f);
    }
}
