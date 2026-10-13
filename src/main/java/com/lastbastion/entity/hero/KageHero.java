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

/**
 * Kage the Ronin: a blur of steel. Iaido Flash dashes through a line of enemies; Shadow Clones
 * splits him into two phantom swordsmen; Thousand Cuts teleports between up to eight enemies.
 */
public class KageHero extends HeroEntity {
    public static final int IAIDO = 4, CLONES = 5, CUTS = 6;
    private Vec3 dashDir = Vec3.ZERO;
    private final List<LivingEntity> cutList = new ArrayList<>();

    public KageHero(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.KAGE);
    }

    protected KageHero(EntityType<? extends HeroEntity> type, Level level, HeroKind kind) {
        super(type, level, kind);
    }

    @Override
    protected int attackWindup() {
        return 4;
    }

    @Override
    protected int attackInterval() {
        return 10;
    }

    @Override
    protected int comboLength() {
        return 3;
    }

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        SiegeDamage.deal(t, this, this, dmg(1f), Element.PHYSICAL, false);
        if (combo == 2) SiegeDamage.applyStacking(t, ModEffects.BLEEDING.get(), 80, 3);
        Vec3 c = t.position().add(0, t.getBbHeight() * 0.6, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(Fx.dust(0xC0102A, 1f), c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (t == null) return 0;
        int near = SiegeDamage.enemiesAround(level, position(), 8).size();
        if (ready(CUTS) && (near >= 5 || (t instanceof SiegeEnemy se && se.isHighPriority() && dist < 8))) return CUTS;
        if (ready(CLONES) && near >= 4) return CLONES;
        if (ready(IAIDO) && dist > 4 && dist < 14) return IAIDO;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case IAIDO -> {
                if (t == 1 && target != null) {
                    dashDir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    faceTowards(target.position());
                }
                if (t == 12) {
                    Vec3 start = position();
                    Vec3 end = start.add(dashDir.scale(10));
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, start.add(dashDir.scale(5)), 6)) {
                        Vec3 rel = e.position().subtract(start);
                        double along = rel.dot(dashDir);
                        if (along < 0 || along > 10 || rel.subtract(dashDir.scale(along)).lengthSqr() > 2.5) continue;
                        SiegeDamage.deal(e, this, this, dmg(2.5f), Element.PHYSICAL, false);
                        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 120, 3);
                    }
                    Fx.line(level, start.add(0, 1, 0), end.add(0, 1, 0), Fx.dust(0xFF2040, 1.2f), 0.25);
                    Fx.line(level, start.add(0, 1, 0), end.add(0, 1, 0), ParticleTypes.SQUID_INK, 0.6);
                    randomTeleport(end.x, end.y, end.z, false);
                    playSound(SoundEvents.TRIDENT_THROW, 1.5f, 1.8f);
                }
                if (t >= 22) {
                    cooldown(IAIDO, 100);
                    return true;
                }
            }
            case CLONES -> {
                if (t == 10) {
                    for (int i = 0; i < 2; i++) {
                        KageCloneEntity c = com.lastbastion.registry.ModEntities.KAGE_CLONE.get().create(level);
                        if (c == null) continue;
                        Vec3 p = position().add(Vec3.directionFromRotation(0, getYRot() + (i == 0 ? 90 : -90)).scale(2));
                        c.moveTo(p.x, p.y, p.z, getYRot(), 0);
                        c.setOwnerId(getOwnerId());
                        c.setMode(HeroMode.HUNT);
                        c.setLevelAndXp(heroLevel(), 0);
                        level.addFreshEntity(c);
                        level.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 1, p.z, 30, 0.3, 0.7, 0.3, 0.05);
                    }
                    playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.5f, 1f);
                }
                if (t >= 18) {
                    cooldown(CLONES, 420);
                    return true;
                }
            }
            case CUTS -> {
                if (t == 1) {
                    cutList.clear();
                    cutList.addAll(SiegeDamage.enemiesAround(level, position(), 9));
                    cutList.sort(Comparator.comparingDouble(this::distanceToSqr));
                    while (cutList.size() > 8) cutList.remove(cutList.size() - 1);
                    setInvisible(true);
                    playSound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.5f, 1.5f);
                }
                int idx = (t - 4) / 5;
                if (t >= 4 && (t - 4) % 5 == 0 && idx < cutList.size()) {
                    LivingEntity e = cutList.get(idx);
                    if (e.isAlive()) {
                        Vec3 from = position().add(0, 1, 0);
                        randomTeleport(e.getX() + random.nextGaussian(), e.getY(), e.getZ() + random.nextGaussian(), false);
                        Fx.line(level, from, position().add(0, 1, 0), Fx.dust(0xFF2040, 1f), 0.3);
                        SiegeDamage.deal(e, this, this, dmg(2f), Element.PHYSICAL, false);
                        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 100, 3);
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 2, 0.3, 0.3, 0.3, 0);
                        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.7f);
                    }
                }
                if (t >= 4 + Math.max(1, cutList.size()) * 5 + 4) {
                    setInvisible(false);
                    level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 30, 0.4, 0.7, 0.4, 0.05);
                    cooldown(CUTS, 400);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }
}
