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
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Ossuary Colossus (wave 20). A 9-block titan of fused bone with a ribcage furnace of green
 * soul-light. Single-minded: it marches on the core, ignoring defenders unless they get close.
 * <ul>
 *   <li><b>Boulder Hurl</b> - tears up an ossified boulder and lobs it (turrets and the core are favourite targets).</li>
 *   <li><b>Quake Stomp</b> - ground-shaking stomp that launches nearby defenders.</li>
 *   <li><b>Bone Spikes</b> - spears of bone erupt beneath its target.</li>
 *   <li><b>Core Smash</b> - double-fisted hammer blow on the core for triple damage.</li>
 *   <li><b>Phase 2 (50%) Ribcage Breach</b> - its ribs crack open and disgorge skeletal runners.</li>
 * </ul>
 */
public class OssuaryBoss extends SiegeBoss {
    public static final int BOULDER = 3, STOMP = 4, SPIKES = 5, SMASH = 6, RIBCAGE = 7;
    private Vec3 throwAt = Vec3.ZERO;

    public OssuaryBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.OSSUARY);
        setMaxUpStep(2.5f);
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.EXPLOSIVE ? 1.3f : e == Element.PHYSICAL ? 0.8f : e == Element.POISON ? 0f : super.elementMultiplier(e);
    }

    @Override
    protected double engageRange() {
        return 6;
    }

    @Override
    protected double meleeReach() {
        return getBbWidth() * 0.5 + 3;
    }

    @Override
    protected int meleeWindup() {
        return 20;
    }

    @Override
    protected float coreDamage() {
        return dmg(2.5f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (dc < meleeReach() + 0.5 && ready(SMASH)) return SMASH;
        if (!SiegeDamage.defendersAround(level, position(), 7).isEmpty() && ready(STOMP)) return STOMP;
        if (ready(BOULDER)) {
            LivingEntity turret = null;
            for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 28)) {
                if (e instanceof TurretEntity && distanceTo(e) > 8) {
                    turret = e;
                    break;
                }
            }
            if (turret != null) {
                throwAt = turret.position();
                return BOULDER;
            }
            if (dc > 8 && dc < 28) {
                BlockPos c = core();
                if (c != null) {
                    throwAt = Vec3.atCenterOf(c);
                    return BOULDER;
                }
            }
        }
        if (t != null && dt > 5 && dt < 20 && ready(SPIKES)) return SPIKES;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case BOULDER -> {
                face(throwAt);
                if (t == 26) {
                    Vec3 hand = position().add(0, getBbHeight() * 0.95, 0);
                    SiegeProjectile b = SiegeProjectile.create(level, SiegeProjectile.ProjKind.BOULDER, this, hand);
                    b.payload(dmg(1.6f), Element.PHYSICAL).area(4f);
                    b.knockback = 1.2f;
                    final Vec3 at0 = throwAt;
                    BlockPos core = core();
                    b.onImpact = (p, at) -> {
                        if (core != null && at.distanceTo(Vec3.atCenterOf(core)) < 4) SiegeManager.damageCore(level, dmg(1.5f), this);
                    };
                    b.aim(at0, 1.1f).scale(3f).fire(level);
                    Fx.sound(level, hand, SoundEvents.IRON_GOLEM_ATTACK, 2f, 0.4f);
                }
                if (t >= 40) {
                    cooldown(BOULDER, 150);
                    return true;
                }
            }
            case STOMP -> {
                if (t == 22) {
                    Fx.shockwave(level, position(), 8, 0xE8E0D0);
                    Fx.sound(level, position(), SoundEvents.GENERIC_EXPLODE, 2.5f, 0.4f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 8)) {
                        SiegeDamage.deal(e, this, this, dmg(1.2f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 1.6, 0.9);
                        SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 80, 0);
                    }
                }
                if (t >= 40) {
                    cooldown(STOMP, 160);
                    return true;
                }
            }
            case SPIKES -> {
                if (t == 16 && target != null) {
                    Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    for (int i = 0; i < 5; i++) {
                        Vec3 at = target.position().add(dir.scale((i - 2) * 1.8));
                        HazardZone.spawn(level, HazardZone.Kind.BONE_SPIKES, at, 1.6f, 30 + i * 4, damageScale * 1.5f, this, true);
                    }
                    Fx.sound(level, target.position(), SoundEvents.BONE_BLOCK_BREAK, 2f, 0.5f);
                }
                if (t >= 30) {
                    cooldown(SPIKES, 120);
                    return true;
                }
            }
            case SMASH -> {
                BlockPos c = core();
                if (c != null) face(Vec3.atCenterOf(c));
                if (t == 30 && c != null) {
                    SiegeManager.damageCore(level, dmg(3f) * 1.5f, this);
                    Fx.shockwave(level, Vec3.atBottomCenterOf(c), 6, 0xE8E0D0);
                    Fx.sound(level, Vec3.atCenterOf(c), SoundEvents.ANVIL_LAND, 2f, 0.3f);
                }
                if (t >= 50) {
                    cooldown(SMASH, 140);
                    return true;
                }
            }
            case RIBCAGE -> {
                if (t == 20) {
                    for (int i = 0; i < 6; i++) {
                        Vec3 p = position().add(0, 3, 0);
                        SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.RUNNER, ZombieRank.ARMORED, 0, p, true, false);
                        if (z != null) {
                            z.setMinion(true);
                            z.setDeltaMovement(random.nextGaussian() * 0.5, 0.6, random.nextGaussian() * 0.5);
                        }
                    }
                    level.sendParticles(com.lastbastion.combat.Fx.dust(0x6BFF8A, 2f), getX(), getY() + 4, getZ(), 50, 1, 1, 1, 0);
                    Fx.sound(level, position(), SoundEvents.SKELETON_HORSE_DEATH, 2f, 0.4f);
                }
                return t >= 40;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        super.onPhase(level, phase);
        startSkill(RIBCAGE);
    }

    @Override
    protected String phaseLine(int phase) {
        return "tears open its ribcage!";
    }
}
