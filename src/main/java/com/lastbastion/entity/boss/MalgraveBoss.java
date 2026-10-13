package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Malgrave the Dread Knight (wave 15). A fallen paladin in blackened, spiked plate wielding
 * a cursed greatsword and a tattered crimson cape.
 * <ul>
 *   <li><b>Ruin Combo</b> - sweep, reverse sweep, overhead cleave.</li>
 *   <li><b>Shadow Charge</b> - dissolves into shadow and lances through a line of defenders.</li>
 *   <li><b>Dark Wave</b> - drives his sword into the ground, sending an eruption of shadow along a line.</li>
 *   <li><b>Call the Fallen</b> - summons two Bulwark death-knights.</li>
 *   <li><b>Phase 2 (50%) Soulfire Ascension</b> - blade ignites with soulfire; gains <b>Soul Eruption</b> rings.</li>
 * </ul>
 */
public class MalgraveBoss extends SiegeBoss {
    public static final int COMBO = 3, CHARGE = 4, WAVE = 5, SUMMON = 6, ERUPTION = 7;
    private Vec3 dir = Vec3.ZERO;

    public MalgraveBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.MALGRAVE);
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.HOLY ? 1.5f : e == Element.PHYSICAL ? 0.85f : super.elementMultiplier(e);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (ready(SUMMON) && tickCount > 100) return SUMMON;
        if (phase() >= 2 && ready(ERUPTION) && !SiegeDamage.defendersAround(level, position(), 8).isEmpty()) return ERUPTION;
        if (t != null && dt < 4.5 && ready(COMBO)) return COMBO;
        if (t != null && dt > 6 && dt < 18 && ready(CHARGE)) return CHARGE;
        double d = t != null ? dt : dc;
        if (d > 4 && d < 16 && ready(WAVE)) return WAVE;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case COMBO -> {
                if (target != null && t < 36) face(target.position());
                if (t == 12) slash(level, 1.0f, false);
                if (t == 24) slash(level, 1.0f, false);
                if (t == 42) slash(level, 1.8f, true);
                if (t >= 56) {
                    cooldown(COMBO, 100);
                    return true;
                }
            }
            case CHARGE -> {
                if (t == 1) {
                    dir = aimPoint().subtract(position()).multiply(1, 0, 1).normalize();
                    face(position().add(dir));
                    Fx.sound(level, position(), SoundEvents.WITHER_SHOOT, 1.5f, 0.5f);
                }
                if (t > 14 && t < 30) {
                    setDeltaMovement(dir.x * 1.1, 0, dir.z * 1.1);
                    level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 6, 0.4, 0.6, 0.4, 0.02);
                    if (phase() >= 2) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 6, 0.4, 0.6, 0.4, 0.02);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 2.2)) {
                        if (e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.2f), Element.SHADOW, false);
                        SiegeDamage.push(e, position(), 1.0, 0.5);
                    }
                    if (t % 3 == 0) crushBlocks(level);
                }
                if (t >= 40) {
                    cooldown(CHARGE, 160);
                    return true;
                }
            }
            case WAVE -> {
                if (t == 1) dir = aimPoint().subtract(position()).multiply(1, 0, 1).normalize();
                face(position().add(dir));
                if (t >= 20 && t < 40) {
                    double dist = (t - 19) * 0.8;
                    Vec3 at = position().add(dir.scale(dist));
                    level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 0.3, at.z, 10, 0.4, 0.5, 0.4, 0.05);
                    level.sendParticles(phase() >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE, at.x, at.y + 0.3, at.z, 8, 0.4, 0.6, 0.4, 0.05);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, at, 1.8)) {
                        if (e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.1f), Element.SHADOW, false);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
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
                if (t == 24) {
                    Fx.sound(level, position(), SoundEvents.EVOKER_PREPARE_SUMMON, 2f, 0.5f);
                    for (int i = 0; i < 2; i++) {
                        Vec3 p = position().add(Vec3.directionFromRotation(0, getYRot() + (i == 0 ? 90 : -90)).scale(3));
                        level.sendParticles(ParticleTypes.SOUL, p.x, p.y + 1, p.z, 30, 0.4, 0.8, 0.4, 0.05);
                        SiegeZombie z = ZombieSpawner.spawn(level, ZombieKind.BULWARK, ZombieRank.byId(2 + phase()), 0, p, true, true);
                        if (z != null) z.setMinion(true);
                    }
                }
                if (t >= 40) {
                    cooldown(SUMMON, 480);
                    return true;
                }
            }
            case ERUPTION -> {
                if (t == 15 || t == 30 || t == 45) {
                    double r = 2 + t / 7.0;
                    Fx.ring(level, position().add(0, 0.2, 0), r, ParticleTypes.SOUL_FIRE_FLAME, (int) (r * 10));
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), r + 1)) {
                        double d = e.distanceTo(this);
                        if (Math.abs(d - r) < 1.6) {
                            SiegeDamage.deal(e, this, this, dmg(0.9f), Element.FIRE, false);
                            SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 80, 2);
                        }
                    }
                    Fx.sound(level, position(), SoundEvents.BLAZE_SHOOT, 1.5f, 0.4f);
                }
                if (t >= 55) {
                    cooldown(ERUPTION, 200);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    private void slash(ServerLevel level, float mult, boolean overhead) {
        Vec3 c = forward(2.4).add(0, 1, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 3, 1, 0.3, 1, 0);
        level.sendParticles(phase() >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SQUID_INK, c.x, c.y, c.z, 20, 1.2, 0.4, 1.2, 0.05);
        Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
        if (overhead) Fx.shockwave(level, forward(2.6), 3, 0x30103A);
        for (LivingEntity e : SiegeDamage.defendersAround(level, c, overhead ? 3.2 : 3.0)) {
            SiegeDamage.deal(e, this, this, dmg(mult), Element.PHYSICAL, false);
            if (phase() >= 2) SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 80, 2);
            SiegeDamage.push(e, position(), overhead ? 1.4 : 0.7, overhead ? 0.6 : 0.2);
        }
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (phase() >= 2 && tickCount % 5 == 0 && getDeltaMovement().horizontalDistanceSqr() > 0.002) {
            HazardZone.spawn(level, HazardZone.Kind.SOUL_FIRE, position(), 1.2f, 60, damageScale * 0.5f, this, true);
        }
    }

    @Override
    protected String phaseLine(int phase) {
        return "ascends in SOULFIRE!";
    }
}
