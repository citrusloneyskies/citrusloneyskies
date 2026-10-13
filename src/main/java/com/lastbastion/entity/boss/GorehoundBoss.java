package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
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
 * Gorehound the Butcher: a giggling mountain of stitched flesh. Every big move is telegraphed:
 * the Cleaver Combo ends in an overhead chop down a lane; the Meat Hook flies along a marked line and
 * drags its catch back for carving; Rampage charges through walls. At half health his belly splits:
 * Gut Spill floods a cone with blood, and Butcher's Frenzy whirls his cleaver in a ring.
 */
public class GorehoundBoss extends SiegeBoss {
    public static final int CLEAVE = 3, HOOK = 4, RAMPAGE = 5, GUT_SPILL = 6, FRENZY = 7, ROAR = 8;
    private Vec3 chargeDir = Vec3.ZERO;
    private Vec3 lane = Vec3.ZERO;

    public GorehoundBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.GOREHOUND);
    }

    @Override
    public int dangerColor() {
        return 0xD02020;
    }

    @Override
    protected String epithet() {
        return "the Butcher of the Breach";
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"FRESH MEAT! Hehehe... fresh meat!", "Who ordered the butcher?"};
            case TAUNT -> new String[]{"Stand still! I'm carving!", "More for the stew!", "Hold still, little chops!"};
            case PHASE -> new String[]{"You spilled my SUPPER! NOW I'M ANGRY!", "Look what you did to my belly!"};
            case STAGGER -> new String[]{"Urk-! My... belly...", "Oof! Not the gut!"};
            case DEATH -> new String[]{"...the kitchen... closes...", "Ugh... save me... a plate..."};
            case CORE -> new String[]{"Let's carve this shiny rock open!"};
            default -> new String[0];
        };
    }

    @Override
    protected void introTick(ServerLevel level, int t) {
        if (t == 10 || t == 22) {
            Fx.sound(level, position(), SoundEvents.GRINDSTONE_USE, 1.5f, 0.6f);
            Vfx.spray(level, FxStyle.SPARK, position().add(0, getBbHeight() * 0.5, 0).add(forward(1.4).subtract(position())), 0xFFE0A0, 0xFF8020, 0.1f, 12, 0.4f, 0.4f);
        }
        if (t == 34) Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 2.5f, 0.7f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        if (phase() >= 2) {
            if (ready(FRENZY) && SiegeDamage.defendersAround(level, position(), 4.5).size() >= 2) return FRENZY;
            if (t != null && dt < 7 && ready(GUT_SPILL)) return GUT_SPILL;
        }
        if (t != null && dt < 4.5 && ready(CLEAVE)) return CLEAVE;
        if (t != null && dt > 6 && dt < 18 && ready(HOOK) && getSensing().hasLineOfSight(t)) return HOOK;
        double d = t != null ? dt : dc;
        if (d > 8 && d < 22 && ready(RAMPAGE)) return RAMPAGE;
        if (t != null && random.nextInt(400) == 0) speak(Lines.TAUNT, false);
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case CLEAVE -> {
                if (target != null && t < 36) face(target.position());
                if (t == 2) teleCone(level, position(), forward(3), 3.6f, 70, 10);
                if (t == 16) teleCone(level, position(), forward(3), 3.6f, 70, 10);
                if (t == 30) {
                    lane = Vec3.directionFromRotation(0, getYRot());
                    teleLine(level, position(), position().add(lane.scale(6)), 2.2f, 14);
                }
                if (t == 12) chop(level, 1f, false, 1);
                if (t == 26) chop(level, 1f, false, -1);
                if (t == 44) chop(level, 1.8f, true, 0);
                if (t >= 58) {
                    cooldown(CLEAVE, 110);
                    return true;
                }
            }
            case HOOK -> {
                if (target != null && t < 18) face(target.position());
                if (t == 2) {
                    lane = Vec3.directionFromRotation(0, getYRot());
                    teleLine(level, position(), position().add(lane.scale(18)), 1.4f, 16);
                    Fx.sound(level, position(), SoundEvents.CHAIN_PLACE, 1.5f, 0.5f);
                }
                if (t == 18) {
                    Vec3 hand = position().add(0, getBbHeight() * 0.65, 0).add(Vec3.directionFromRotation(0, getYRot() + 40).scale(1.2));
                    SiegeProjectile hook = SiegeProjectile.create(level, SiegeProjectile.ProjKind.HOOK, this, hand);
                    hook.payload(dmg(0.5f), Element.PHYSICAL).status(ModEffects.BLEEDING, 100, 2);
                    hook.onEntityHit = (p, victim) -> {
                        Vec3 pull = position().subtract(victim.position());
                        victim.setDeltaMovement(pull.x * 0.28, 0.45 + pull.y * 0.05, pull.z * 0.28);
                        victim.hurtMarked = true;
                        Vfx.tether(level, this, getBbHeight() * 0.6f, victim, 0x8A8A8A, 0.08f, 12);
                        Fx.sound(level, victim.position(), SoundEvents.CHAIN_BREAK, 1.2f, 0.6f);
                    };
                    hook.aim(hand.add(lane.scale(18)), 1.8f).scale(1.6f).fire(level);
                    Fx.sound(level, position(), SoundEvents.CHAIN_PLACE, 1.5f, 0.5f);
                }
                if (t >= 34) {
                    cooldown(HOOK, 150);
                    return true;
                }
            }
            case RAMPAGE -> {
                if (t == 1) {
                    chargeDir = aimPoint().subtract(position()).multiply(1, 0, 1).normalize();
                    face(position().add(chargeDir));
                    teleLine(level, position(), position().add(chargeDir.scale(16)), 3.2f, 18);
                    Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 2f, 0.7f);
                    speak(Lines.TAUNT, false);
                }
                if (t > 18 && t < 56) {
                    setDeltaMovement(chargeDir.x * 0.62, getDeltaMovement().y, chargeDir.z * 0.62);
                    if (t % 2 == 0) Vfx.spray(level, FxStyle.SMOKE, position().add(0, 0.3, 0), 0xC8B8A0, 0x8A7A60, 0.9f, 3, 0.1f, -1);
                    if (t % 4 == 0) crushBlocks(level);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position().add(chargeDir.scale(1.5)), 2.4)) {
                        if (e.hurtTime > 0) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.3f), Element.PHYSICAL, false);
                        SiegeDamage.push(e, position(), 1.8, 0.7);
                    }
                    if (horizontalCollision && t > 24) skillTick = 56;
                }
                if (t == 57) {
                    Fx.shockwave(level, position(), 3.5, 0xD02020);
                    Vfx.shake(level, position(), 0.6f, 18, 8);
                }
                if (t >= 64) {
                    cooldown(RAMPAGE, 220);
                    return true;
                }
            }
            case GUT_SPILL -> {
                if (target != null && t < 14) face(target.position());
                if (t == 2) teleCone(level, position(), forward(6), 7.5f, 32, 16);
                if (t == 18) {
                    Vec3 dir = Vec3.directionFromRotation(0, getYRot());
                    Vfx.jet(level, FxStyle.DROP, position().add(0, getBbHeight() * 0.55, 0), position().add(dir.scale(7.5)), 0xB01020, 0x5A0008, 0.25f, 50, 26);
                    for (int i = 1; i <= 3; i++) {
                        HazardZone.spawn(level, HazardZone.Kind.BLOOD_POOL, position().add(dir.scale(i * 2.2)), 1.8f + i * 0.3f, 140, damageScale, this, true);
                    }
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 7.5)) {
                        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1).normalize();
                        if (to.dot(dir) < Math.cos(Math.toRadians(32))) continue;
                        SiegeDamage.deal(e, this, this, dmg(1.2f), Element.POISON, false);
                        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 120, 3);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    }
                    Fx.sound(level, position(), SoundEvents.RAVAGER_STUNNED, 2f, 0.6f);
                }
                if (t >= 34) {
                    cooldown(GUT_SPILL, 240);
                    return true;
                }
            }
            case FRENZY -> {
                if (t == 2) {
                    teleCircle(level, position(), 4.8f, 16);
                    Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 2f, 1f);
                }
                if (t >= 18 && t <= 54 && (t - 18) % 9 == 0) {
                    Vfx.slash(level, position().add(0, getBbHeight() * 0.45, 0), getYRot() + t * 30, 5, 360, 4.6f, 0xD02020, 0xFFE0D0, 8, 0.3f);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 4.8)) {
                        SiegeDamage.deal(e, this, this, dmg(0.9f), Element.PHYSICAL, false);
                        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 100, 4);
                        SiegeDamage.push(e, position(), 0.7, 0.2);
                    }
                    Fx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.5f);
                }
                if (t >= 62) {
                    cooldown(FRENZY, 200);
                    return true;
                }
            }
            case ROAR -> {
                if (t == 10) {
                    Fx.shockwave(level, position(), 8, 0x8B0000);
                    Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
                    Vfx.spray(level, FxStyle.DROP, position().add(0, getBbHeight() * 0.5, 0), 0xB01020, 0x5A0008, 0.3f, 40, 0.5f, 0.6f);
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

    /** One cleaver chop. {@code side}: 1 right-to-left, -1 left-to-right, 0 overhead. */
    private void chop(ServerLevel level, float mult, boolean overhead, int side) {
        Vec3 c = forward(2.2).add(0, 1, 0);
        if (overhead) {
            Vfx.slash(level, position().add(0, getBbHeight() * 0.6, 0), getYRot(), 90, 160, 4.2f, 0xD02020, 0xFFE0D0, 10, 0.3f);
            Fx.shockwave(level, forward(4).add(0, 0.1, 0), 2.2, 0x8B0000);
            for (LivingEntity e : SiegeDamage.defendersAround(level, forward(3), 3.2)) {
                Vec3 rel = e.position().subtract(position()).multiply(1, 0, 1);
                double along = rel.dot(lane);
                if (along < 0 || along > 6.5 || rel.subtract(lane.scale(along)).lengthSqr() > 1.6) continue;
                SiegeDamage.deal(e, this, this, dmg(mult), Element.PHYSICAL, false);
                SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 120, 3);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9, 0));
                e.hurtMarked = true;
            }
            Vfx.shake(level, position(), 0.7f, 18, 8);
        } else {
            Vfx.slash(level, position().add(0, getBbHeight() * 0.5, 0), getYRot(), side * 10, side * 150, 3.6f, 0xD02020, 0xFFE0D0, 8, 0.3f);
            for (LivingEntity e : SiegeDamage.defendersAround(level, c, 2.9)) {
                SiegeDamage.deal(e, this, this, dmg(mult), Element.PHYSICAL, false);
                SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 120, 3);
                SiegeDamage.push(e, position(), 0.5, 0.1);
            }
        }
        Vfx.spray(level, FxStyle.DROP, c, 0xB01020, 0x5A0008, 0.18f, 10, 0.35f, 0.4f);
        Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_STRONG, 1.5f, 0.5f);
    }

    @Override
    protected void onMeleeHit(ServerLevel level, LivingEntity e) {
        SiegeDamage.applyStacking(e, ModEffects.BLEEDING.get(), 80, 2);
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (phase() >= 2 && tickCount % 20 == 0) {
            Vfx.spray(level, FxStyle.DROP, position().add(0, getBbHeight() * 0.45, 0), 0xB01020, 0x5A0008, 0.15f, 6, 0.15f, 0.2f);
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
