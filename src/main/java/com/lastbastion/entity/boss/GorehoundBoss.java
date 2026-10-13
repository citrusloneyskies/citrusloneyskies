package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Gorehound the Butcher (wave 5).
 * <ul>
 *   <li><b>Cleaver Combo</b> - three heavy chops, the last one a launcher. Inflicts Bleeding.</li>
 *   <li><b>Meat Hook</b> - flings a chain hook; a hooked defender is dragged to the Butcher's feet.</li>
 *   <li><b>Rampage</b> - a bull charge that tramples defenders and smashes through walls.</li>
 *   <li><b>Phase 2 (50%)</b> - Blood Frenzy: faster, cooldowns shortened, aura of bleeding.</li>
 * </ul>
 */
public class GorehoundBoss extends SiegeBoss {
    public static final int CLEAVE = 3, HOOK = 4, RAMPAGE = 5, ROAR = 6;
    private Vec3 chargeDir = Vec3.ZERO;

    public GorehoundBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.GOREHOUND);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (t != null && dt < 4.5 && ready(CLEAVE)) return CLEAVE;
        if (t != null && dt > 6 && dt < 18 && ready(HOOK) && getSensing().hasLineOfSight(t)) return HOOK;
        double d = t != null ? dt : dc;
        if (d > 8 && d < 22 && ready(RAMPAGE)) return RAMPAGE;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case CLEAVE -> {
                if (target != null) face(target.position());
                if (t == 12 || t == 26 || t == 44) chop(level, t == 44 ? 1.7f : 1.0f, t == 44);
                if (t >= 56) {
                    cooldown(CLEAVE, 110);
                    return true;
                }
            }
            case HOOK -> {
                if (target != null) face(target.position());
                if (t == 14 && target != null) {
                    Vec3 hand = position().add(0, getBbHeight() * 0.7, 0).add(Vec3.directionFromRotation(0, getYRot() + 40).scale(1.2));
                    SiegeProjectile hook = SiegeProjectile.create(level, SiegeProjectile.ProjKind.HOOK, this, hand);
                    hook.payload(dmg(0.5f), Element.PHYSICAL).status(ModEffects.BLEEDING, 100, 2);
                    hook.onEntityHit = (p, victim) -> {
                        Vec3 pull = position().subtract(victim.position());
                        victim.setDeltaMovement(pull.x * 0.28, 0.45 + pull.y * 0.05, pull.z * 0.28);
                        victim.hurtMarked = true;
                        Fx.line(level, position().add(0, 2, 0), victim.position().add(0, 1, 0), Fx.dust(0x555555, 0.7f), 0.3);
                        Fx.sound(level, victim.position(), SoundEvents.CHAIN_BREAK, 1.2f, 0.6f);
                    };
                    hook.aim(target.position().add(0, target.getBbHeight() * 0.5, 0), 1.6f).scale(1.5f).fire(level);
                    Fx.sound(level, position(), SoundEvents.CHAIN_PLACE, 1.5f, 0.5f);
                }
                if (t >= 30) {
                    cooldown(HOOK, 150);
                    return true;
                }
            }
            case RAMPAGE -> {
                if (t == 1) {
                    chargeDir = aimPoint().subtract(position()).multiply(1, 0, 1).normalize();
                    face(position().add(chargeDir));
                    Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 2f, 0.7f);
                }
                if (t > 16 && t < 56) {
                    setDeltaMovement(chargeDir.x * 0.6, getDeltaMovement().y, chargeDir.z * 0.6);
                    level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 3, 0.6, 0.1, 0.6, 0.02);
                    if (t % 4 == 0) crushBlocks(level);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position().add(chargeDir.scale(1.5)), 2.2)) {
                        if (e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.3f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 1.8, 0.7);
                    }
                }
                if (t >= 62) {
                    cooldown(RAMPAGE, 220);
                    return true;
                }
            }
            case ROAR -> {
                if (t == 10) {
                    Fx.shockwave(level, position(), 8, 0x8B0000);
                    Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 8)) {
                        SiegeDamage.push(e, position(), 1.2, 0.4);
                        SiegeDamage.apply(e, ModEffects.BLEEDING.get(), 100, 1);
                    }
                }
                return t >= 40;
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    private void chop(ServerLevel level, float mult, boolean launch) {
        Vec3 c = forward(2.2).add(0, 1, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 2, 0.6, 0.2, 0.6, 0);
        level.sendParticles(Fx.dust(0x8B0000, 1.5f), c.x, c.y, c.z, 15, 1, 0.5, 1, 0);
        Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_STRONG, 1.5f, 0.5f);
        for (LivingEntity e : SiegeDamage.defendersAround(level, c, 2.8)) {
            SiegeDamage.deal(e, this, this, dmg(mult), Element.PHYSICAL, false);
            SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 120, 3);
            if (launch) e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9, 0));
            SiegeDamage.push(e, position(), launch ? 1.2 : 0.5, launch ? 0.6 : 0.1);
        }
    }

    @Override
    protected void onMeleeHit(ServerLevel level, LivingEntity e) {
        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 80, 2);
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (phase() >= 2 && tickCount % 20 == 0) {
            level.sendParticles(Fx.dust(0x8B0000, 1.8f), getX(), getY() + 1.5, getZ(), 12, 1.5, 1, 1.5, 0);
            for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 4)) SiegeDamage.apply(e, ModEffects.BLEEDING.get(), 40, 0);
        }
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        super.onPhase(level, phase);
        startSkill(ROAR);
    }

    @Override
    protected String phaseLine(int phase) {
        return "enters a BLOOD FRENZY!";
    }
}
