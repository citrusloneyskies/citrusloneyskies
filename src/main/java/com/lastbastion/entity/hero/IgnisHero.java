package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.entity.zombie.RunnerZombie;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ignis the Phoenix: flying fire-bird. Dives into crowds, heals allies with embers and is reborn once per wave. */
public class IgnisHero extends HeroEntity {
    public static final int DIVE = 4, EMBER = 5, REBIRTH = 6;
    private boolean rebirthUsed;
    private Vec3 diveAt = Vec3.ZERO;

    public IgnisHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.IGNIS);
    }

    /** True while reborn as a burning egg. */
    public boolean reborn() {
        return flag();
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 18;
    }

    @Override
    protected double preferredRange() {
        return 9;
    }

    @Override
    protected int attackWindup() {
        return 6;
    }

    @Override
    protected int attackInterval() {
        return 16;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        SiegeProjectile p = SiegeProjectile.create(level, ProjKind.FIREBALL, this, position().add(0, getBbHeight() * 0.6, 0))
                .payload(dmg(1f), Element.FIRE).area(1.5f).status(ModEffects.SCORCHED, 60, 1);
        p.homing(t, 0.15f).aim(t.position().add(0, t.getBbHeight() * 0.5, 0), 1.3f).fire(level);
        playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 1.3f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(EMBER) && alliesHurt(level)) return EMBER;
        if (t != null && ready(DIVE) && SiegeDamage.enemiesAround(level, t.position(), 4).size() >= 3) return DIVE;
        return 0;
    }

    private boolean alliesHurt(ServerLevel level) {
        for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 12)) {
            if (a.getHealth() < a.getMaxHealth() * 0.7f) return true;
        }
        var d = SiegeManager.data(level);
        BlockPos core = SiegeManager.corePos(level);
        return core != null && d.coreHp < d.maxHp() * 0.7f && core.closerToCenterThan(position(), 16);
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case DIVE -> {
                if (t == 1 && target != null) diveAt = target.position();
                if (t < 15) hover(diveAt.add(0, 9, 0), 0.7);
                else {
                    Vec3 v = diveAt.subtract(position());
                    setDeltaMovement(v.normalize().scale(1.3));
                    faceTowards(diveAt);
                    level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 8, 0.4, 0.4, 0.4, 0.03);
                    if (v.length() < 1.8 || t > 50) {
                        Fx.explosion(level, position(), 4);
                        Fx.ring(level, position().add(0, 0.2, 0), 4, ParticleTypes.FLAME, 30);
                        for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 4)) {
                            SiegeDamage.deal(e, this, this, dmg(3f), Element.FIRE, false);
                            SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 100, 2);
                            e.setSecondsOnFire(5);
                        }
                        HazardZone.spawn(level, HazardZone.Kind.FIRE, position(), 3.5f, 100, dmg(0.2f), this, false);
                        setDeltaMovement(0, 0.8, 0);
                        cooldown(DIVE, 200);
                        return true;
                    }
                }
            }
            case EMBER -> {
                setDeltaMovement(getDeltaMovement().scale(0.5));
                if (t == 14) {
                    Fx.spiral(level, position(), 2.5, 3, ParticleTypes.FLAME, 4, 0);
                    Fx.ring(level, position(), 12, Fx.dust(0xFFB040, 1.5f), 50);
                    playSound(SoundEvents.FIRECHARGE_USE, 1.5f, 0.7f);
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 12)) a.heal(a.getMaxHealth() * 0.25f);
                    BlockPos core = SiegeManager.corePos(level);
                    if (core != null && core.closerToCenterThan(position(), 16)) SiegeManager.healCore(level, SiegeManager.data(level).maxHp() * 0.06f);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 8)) {
                        SiegeDamage.deal(e, this, this, dmg(1f), Element.FIRE, false);
                        e.setSecondsOnFire(3);
                    }
                }
                if (t >= 24) {
                    cooldown(EMBER, 300);
                    return true;
                }
            }
            case REBIRTH -> {
                setDeltaMovement(0, -0.05, 0);
                heal(getMaxHealth() / 100f);
                if (t % 5 == 0) level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 10, 0.4, 0.4, 0.4, 0.04);
                if (t >= 100) {
                    setFlag(false);
                    setHealth(getMaxHealth());
                    Fx.explosion(level, position(), 4);
                    Fx.skyBeam(level, position(), 0xFF7A1A, 25, 1.5);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 6)) {
                        SiegeDamage.deal(e, this, this, dmg(4f), Element.FIRE, false);
                        e.setSecondsOnFire(6);
                    }
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 1f, 2f);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (reborn()) return false;
        if (!level().isClientSide && !rebirthUsed && amount >= getHealth() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            rebirthUsed = true;
            setHealth(1f);
            setFlag(true);
            startSkill(REBIRTH);
            playSound(SoundEvents.TOTEM_USE, 1.5f, 0.8f);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void onWaveCleared() {
        super.onWaveCleared();
        rebirthUsed = false;
    }

    @Override
    protected void passiveTick(ServerLevel level) {
        if (tickCount % 2 == 0) level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.6, getZ(), 1, 0.6, 0.2, 0.6, 0.01);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }
}
