package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.fx.FxParticleOptions;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Moloch, the Furnace King: a bull-headed idol of blackened bronze with a living furnace for a belly, chimneys on
 * his shoulders and burning palms. The more he burns, the hotter he gets - and when the furnace runs white-hot he
 * turns molten.
 * <ul>
 *   <li><b>Backhand / Hammer Fist / Gore</b> - burning blows, the last a horn toss with a belch of fire.</li>
 *   <li><b>Brazen Charge</b> - a bull rush that gores and tosses everything in its path and leaves a trail of fire.</li>
 *   <li><b>Furnace Maw</b> - the furnace door in his belly swings open: a roaring cone of hellfire.</li>
 *   <li><b>The Offering</b> - he seizes an enemy and feeds it to the furnace (a heavy branding slam on bosses).</li>
 *   <li><b>Molten Rain</b> - his chimneys erupt, lobbing molten bronze that leaves pools of fire.</li>
 *   <li><b>Pillars of Tophet</b> - hellfire pillars erupt beneath his enemies.</li>
 *   <li><b>Brazen Brand</b> - a slam that brands everything near; branded enemies explode when they die.</li>
 *   <li><b>ULT The Valley of Tophet</b> - the ground becomes a lake of fire; pillars take offerings one by one, his
 *   allies burn with strength, and it ends in an eruption from the furnace that leaves him molten.</li>
 * </ul>
 * Passive, The Idol Hungers: enemies that die burning near him stoke the furnace (more damage, a wider fire aura).
 * At ten stokes he turns <b>Molten</b> for ten seconds: 30% less damage taken, every blow leaves fire.
 */
public class MolochDevil extends DevilEntity {
    public static final int CHARGE = 6, MAW = 7, OFFERING = 8, MOLTEN_RAIN = 9, PILLARS = 10, BRAND = 11;
    public static final int FIRE = 0xFF7A1A, HOT = 0xFFD27A, BRONZE = 0x8A5A2A, SOOT = 0x1A1210, WHITE_HOT = 0xFFF4D8;
    public static final int MAX_STOKE = 10;
    private static final EntityDataAccessor<Integer> STOKED = SynchedEntityData.defineId(MolochDevil.class, EntityDataSerializers.INT);

    private record Brand(LivingEntity e, int until) {
    }

    private final List<Brand> brands = new ArrayList<>();
    private final List<LivingEntity> fuel = new ArrayList<>();
    private final Set<Integer> gored = new HashSet<>();
    private final List<Vec3> pillars = new ArrayList<>();
    @Nullable
    private LivingEntity offering;
    private Vec3 chargeDir = Vec3.ZERO;
    private int moltenTicks, coolTicks;

    public MolochDevil(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.MOLOCH);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(STOKED, 0);
    }

    /** Furnace heat, 0-10. */
    public int stoked() {
        return entityData.get(STOKED);
    }

    /** True while white-hot (after the furnace reaches ten stokes, or after his ultimate). */
    public boolean molten() {
        return flag();
    }

    private void stoke(ServerLevel level, int n) {
        if (molten()) return;
        int s = Math.min(MAX_STOKE, stoked() + n);
        entityData.set(STOKED, s);
        coolTicks = 0;
        if (s >= MAX_STOKE) meltDown(level, 200);
    }

    private void meltDown(ServerLevel level, int ticks) {
        setFlag(true);
        moltenTicks = ticks;
        entityData.set(STOKED, MAX_STOKE);
        Vfx.pillar(level, position(), WHITE_HOT, 1.4f, 8, 20);
        Vfx.spray(level, FxStyle.FLAME, position().add(0, getBbHeight() * 0.5, 0), WHITE_HOT, FIRE, 0.5f, 40, 0.4f, 0.8f);
        Vfx.screenFlash(level, position(), FIRE, 0.25f, 24, 10);
        playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.4f);
        playSound(SoundEvents.LAVA_EXTINGUISH, 2f, 0.5f);
        speakMolten();
    }

    private void speakMolten() {
        if (owner() instanceof net.minecraft.server.level.ServerPlayer p && p.distanceToSqr(this) < 48 * 48) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal(kind.title + ": THE FURNACE RUNS WHITE!")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }
    }

    @Override
    protected int fireColor() {
        return FIRE;
    }

    @Override
    protected int fireCore() {
        return HOT;
    }

    @Override
    public Role role() {
        return Role.TANK;
    }

    @Override
    public float dmg(float mult) {
        return super.dmg(mult) * (1f + 0.05f * stoked() + (molten() ? 0.15f : 0f));
    }

    @Override
    protected double attackRange() {
        return getBbWidth() * 0.5 + 2.6;
    }

    @Override
    protected int attackWindup() {
        return 11;
    }

    @Override
    protected int attackRecover() {
        return 7;
    }

    @Override
    protected int attackInterval() {
        return 18;
    }

    @Override
    protected int comboLength() {
        return 3;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"WHO LIT MY FURNACE?", "An offering? No... a whole HORDE of offerings."};
            case ULT -> new String[]{"BEHOLD THE VALLEY OF TOPHET!", "ALL OF YOU... INTO THE FIRE!"};
            case BOSS -> new String[]{"Ahh. A worthy offering.", "That one will burn for days."};
            case KILL -> new String[]{"Fuel.", "The fire accepts.", "*the furnace roars*"};
            case LOW_HP -> new String[]{"Bronze does not break. It MELTS.", "You only stoke the flame!"};
            case LEVEL_UP -> new String[]{"The furnace grows. So does its hunger."};
            case IDLE -> new String[]{"*a low, iron groan from the furnace*", "Cold. Everything here is so cold."};
            default -> new String[0];
        };
    }

    private void burn(LivingEntity e, float amount, int stacks) {
        hit(e, amount, Element.INFERNAL);
        SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 120, stacks);
        e.setSecondsOnFire(4);
    }

    private Vec3 belly() {
        return position().add(0, getBbHeight() * 0.45, 0).add(look().scale(getBbWidth() * 0.55));
    }

    private Vec3 chimney(int side) {
        Vec3 l = look();
        Vec3 right = new Vec3(-l.z, 0, l.x);
        return position().add(0, getBbHeight() * 0.95, 0).add(right.scale(side * getBbWidth() * 0.45)).add(l.scale(-0.3));
    }

    private void moltenPool(ServerLevel level, Vec3 at, float r) {
        if (molten()) HazardZone.spawn(level, HazardZone.Kind.FIRE, at, r, 60, dmg(0.15f), this, false);
    }

    // ------------------------------------------------------------------ basic attack

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 look = look();
        switch (combo) {
            case 0 -> {
                Vfx.slash(level, position().add(0, getBbHeight() * 0.55, 0), getYRot(), -15, 160, 3.8f, FIRE, HOT, 8, 0.5f);
                double cos = Math.cos(Math.toRadians(80));
                for (LivingEntity e : enemiesNear(level, 4.4)) {
                    Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                    if (to.lengthSqr() > 0.6 && to.normalize().dot(look) < cos) continue;
                    burn(e, dmg(1.3f), 2);
                    if (!big(e)) SiegeDamage.push(e, position(), 0.9, 0.3);
                }
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.5f);
                playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 0.6f);
            }
            case 1 -> {
                Vec3 at = position().add(look.scale(2.4));
                Vfx.ring(level, at.add(0, 0.1, 0), FIRE, 2.8f, 10, 0.5f);
                Vfx.spray(level, FxStyle.DEBRIS, at.add(0, 0.2, 0), 0x3A2A1A, SOOT, 0.3f, 14, 0.4f, 1f);
                Vfx.spray(level, FxStyle.EMBER, at.add(0, 0.4, 0), HOT, FIRE, 0.18f, 14, 0.3f, 1f);
                for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 2.8)) {
                    burn(e, dmg(1.5f), 2);
                    SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 30, 0);
                }
                moltenPool(level, at, 2f);
                Vfx.shake(level, at, 0.3f, 12, 6);
                playSound(SoundEvents.ANVIL_LAND, 1f, 0.5f);
            }
            default -> {
                Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0);
                Vfx.flash(level, at, HOT, 1.4f, 6);
                burn(t, dmg(1.9f), 3);
                if (!big(t)) {
                    t.setDeltaMovement(look.x * 0.6, 0.85, look.z * 0.6);
                    t.hurtMarked = true;
                }
                Vec3 from = belly();
                Vfx.jet(level, FxStyle.FLAME, from, from.add(look.scale(5)), HOT, FIRE, 0.45f, 22, 22);
                double cos = Math.cos(Math.toRadians(24));
                for (LivingEntity e : enemiesNear(level, 5.5)) {
                    if (e == t) continue;
                    Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                    if (to.lengthSqr() > 0.5 && to.normalize().dot(look) < cos) continue;
                    burn(e, dmg(0.8f), 2);
                }
                moltenPool(level, t.position(), 2f);
                playSound(SoundEvents.RAVAGER_ATTACK, 1.6f, 0.6f);
                playSound(SoundEvents.FIRECHARGE_USE, 1.2f, 0.6f);
            }
        }
    }

    // ------------------------------------------------------------------ skills

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(BRAND) && crowd(level, position(), 7) >= 5) return BRAND;
        if (ready(OFFERING)) {
            LivingEntity pick = null;
            for (LivingEntity e : enemiesNear(level, 5)) {
                if (big(e)) {
                    if (pick == null || !big(pick)) pick = e;
                } else if (pick == null || (!big(pick) && e.getMaxHealth() > pick.getMaxHealth())) {
                    pick = e;
                }
            }
            if (pick != null && (big(pick) || pick.getMaxHealth() >= 30)) {
                offering = pick;
                return OFFERING;
            }
        }
        if (t == null) return 0;
        if (ready(MAW) && dist < 8 && crowd(level, t.position(), 4) >= 3) return MAW;
        if (ready(CHARGE) && dist > 5 && dist < 18) {
            chargeDir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
            return CHARGE;
        }
        if (ready(PILLARS) && crowd(level, position(), 14) >= 4) return PILLARS;
        if (ready(MOLTEN_RAIN) && bestCluster(level, 22, 4.5f, 4) != null) return MOLTEN_RAIN;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (crowd(level, position(), 14) >= 8 || (big(t) && dist < 10) || getHealth() < getMaxHealth() * 0.35f);
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case CHARGE -> {
                if (t < 10) {
                    faceNow(position().add(chargeDir));
                    if (t % 3 == 0) Vfx.spray(level, FxStyle.DEBRIS, position().add(0, 0.2, 0), 0x3A2A1A, SOOT, 0.3f, 8, 0.25f, 1f);
                    if (t == 2) {
                        Vfx.teleLine(level, position(), position().add(chargeDir.scale(14)), 1.6f, 10, FIRE, false);
                        playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
                    }
                    setDeltaMovement(0, getDeltaMovement().y, 0);
                } else if (t < 28) {
                    if (t == 10) gored.clear();
                    faceNow(position().add(chargeDir));
                    setDeltaMovement(chargeDir.x * 0.95, getDeltaMovement().y, chargeDir.z * 0.95);
                    hasImpulse = true;
                    Vec3 front = position().add(chargeDir.scale(getBbWidth() * 0.6)).add(0, 1, 0);
                    Vfx.spray(level, FxStyle.FLAME, position().add(0, 0.4, 0), HOT, FIRE, 0.4f, 6, 0.1f, 0.6f);
                    if (t % 3 == 0) HazardZone.spawn(level, HazardZone.Kind.FIRE, position(), 1.7f, 80, dmg(0.12f), this, false);
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, front, 2.4)) {
                        if (!gored.add(e.getId())) continue;
                        burn(e, dmg(2.4f), 3);
                        if (big(e)) {
                            SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 40, 0);
                        } else {
                            Vec3 side = new Vec3(-chargeDir.z, 0, chargeDir.x).scale(random.nextBoolean() ? 1 : -1);
                            e.setDeltaMovement(side.x * 0.9 + chargeDir.x * 0.6, 0.9, side.z * 0.9 + chargeDir.z * 0.6);
                            e.hurtMarked = true;
                        }
                        Vfx.flash(level, e.position().add(0, e.getBbHeight() * 0.5, 0), HOT, 1.2f, 5);
                        playSound(SoundEvents.RAVAGER_ATTACK, 1.5f, 0.7f);
                    }
                    if (t % 5 == 0) playSound(SoundEvents.RAVAGER_STEP, 1.8f, 0.6f);
                    if (horizontalCollision && t > 12) return endCharge(level);
                } else {
                    return endCharge(level);
                }
            }
            case MAW -> {
                if (target != null) faceTowards(target.position());
                if (t == 8) playSound(SoundEvents.IRON_DOOR_OPEN, 2f, 0.4f);
                if (t >= 10 && t <= 40 && t % 2 == 0) {
                    Vec3 from = belly();
                    Vec3 dir = look();
                    Vfx.jet(level, FxStyle.FLAME, from, from.add(dir.scale(9)).add(0, -0.8, 0), HOT, FIRE, 0.7f, 24, 26);
                    Vfx.jet(level, FxStyle.SMOKE, from.add(0, 0.6, 0), from.add(dir.scale(7)).add(0, 1.6, 0), 0x3A3030, SOOT, 0.9f, 4, 20);
                    double cos = Math.cos(Math.toRadians(26));
                    for (LivingEntity e : enemiesNear(level, 9.5)) {
                        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                        if (to.lengthSqr() > 0.5 && to.normalize().dot(dir) < cos) continue;
                        burn(e, dmg(0.42f), 5);
                        if (!big(e)) {
                            e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(0.12)));
                            e.hurtMarked = true;
                        }
                    }
                    if (t % 6 == 0) playSound(SoundEvents.BLAZE_SHOOT, 1.8f, 0.4f);
                }
                if (t == 42) playSound(SoundEvents.IRON_DOOR_CLOSE, 2f, 0.4f);
                if (t >= 46) {
                    cooldown(MAW, 220);
                    return true;
                }
            }
            case OFFERING -> {
                LivingEntity e = offering;
                if (e == null || !e.isAlive() || e.distanceToSqr(this) > 10 * 10) {
                    offering = null;
                    cooldown(OFFERING, 80);
                    return true;
                }
                faceNow(e.position());
                if (big(e)) {
                    if (t == 14) {
                        Vec3 at = e.position();
                        Vfx.darkSigil(level, at.add(0, 0.05, 0), SOOT, FIRE, 3.4f, 40, 3f);
                        Vfx.pillar(level, at, FIRE, 1.6f, 7, 20);
                        Vfx.shake(level, at, 0.6f, 20, 12);
                        burn(e, dmg(6f), 5);
                        SiegeDamage.apply(e, ModEffects.MARKED.get(), 200, 2);
                        brands.add(new Brand(e, tickCount + 200));
                        playSound(SoundEvents.ANVIL_LAND, 2f, 0.4f);
                        playSound(SoundEvents.GENERIC_EXPLODE, 1.4f, 0.6f);
                    }
                    if (t >= 24) {
                        offering = null;
                        cooldown(OFFERING, 260);
                        return true;
                    }
                    return false;
                }
                if (t >= 6 && t < 24) {
                    Vec3 hand = position().add(0, getBbHeight() * 0.62, 0).add(look().scale(getBbWidth() * 0.5 + 1.4));
                    Vec3 hold = t < 14 ? hand : hand.lerp(belly(), (t - 14) / 10.0);
                    e.teleportTo(hold.x, hold.y - e.getBbHeight() * 0.5, hold.z);
                    e.setDeltaMovement(Vec3.ZERO);
                    e.hurtMarked = true;
                    if (t % 3 == 0) Vfx.spray(level, FxStyle.EMBER, hold, HOT, FIRE, 0.15f, 6, 0.1f, 0.5f);
                }
                if (t == 6) playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.8f, 0.5f);
                if (t == 24) {
                    Vec3 at = belly();
                    Vfx.flash(level, at, WHITE_HOT, 2.4f, 8);
                    Vfx.spray(level, FxStyle.FLAME, at, WHITE_HOT, FIRE, 0.5f, 30, 0.3f, 0.4f);
                    for (int side = -1; side <= 1; side += 2) {
                        Vec3 ch = chimney(side);
                        Vfx.jet(level, FxStyle.FLAME, ch, ch.add(0, 5, 0), WHITE_HOT, FIRE, 0.6f, 30, 12);
                        Vfx.jet(level, FxStyle.SMOKE, ch.add(0, 1, 0), ch.add(0, 7, 0), 0x3A3030, SOOT, 1.2f, 8, 16);
                    }
                    SiegeDamage.deal(e, this, this, e.getMaxHealth() * 4f + 60f, Element.INFERNAL, false, true);
                    feed(0.08f);
                    stoke(level, 3);
                    playSound(SoundEvents.BLAZE_SHOOT, 2.2f, 0.3f);
                    playSound(SoundEvents.GENERIC_BURN, 2f, 0.5f);
                }
                if (t >= 34) {
                    offering = null;
                    cooldown(OFFERING, 260);
                    return true;
                }
            }
            case MOLTEN_RAIN -> {
                if (t == 6) playSound(SoundEvents.RAVAGER_ROAR, 1.8f, 0.5f);
                if (t >= 10 && t <= 34 && t % 3 == 1) {
                    Vec3 c = bestCluster(level, 22, 4.5f, 1);
                    if (c == null && target != null) c = target.position();
                    if (c != null) {
                        int side = (t / 3) % 2 == 0 ? 1 : -1;
                        Vec3 from = chimney(side);
                        Vec3 aim = c.add(random.nextGaussian() * 2.2, 0, random.nextGaussian() * 2.2);
                        SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.METEOR, this, from)
                                .payload(dmg(1.2f), Element.INFERNAL).status(ModEffects.SCORCHED, 120, 4);
                        p.area(2.6f);
                        p.onImpact = (pr, at) -> HazardZone.spawn(level, HazardZone.Kind.FIRE, at, 2.4f, 90, dmg(0.15f), this, false);
                        p.aim(aim, 0.9f).scale(1.3f).fire(level);
                        Vfx.jet(level, FxStyle.FLAME, from, from.add(0, 3, 0), WHITE_HOT, FIRE, 0.5f, 12, 18);
                        Vfx.spray(level, FxStyle.SMOKE, from.add(0, 1, 0), 0x3A3030, SOOT, 1f, 3, 0.1f, 1f);
                        playSound(SoundEvents.BLAZE_SHOOT, 1.6f, 0.5f);
                    }
                }
                if (t >= 40) {
                    cooldown(MOLTEN_RAIN, 300);
                    return true;
                }
            }
            case PILLARS -> {
                if (t == 4) {
                    pillars.clear();
                    for (LivingEntity e : enemiesNear(level, 14)) {
                        if (pillars.size() >= 7) break;
                        boolean near = false;
                        for (Vec3 p : pillars) near |= p.distanceToSqr(e.position()) < 6;
                        if (!near) pillars.add(e.position());
                    }
                    for (Vec3 p : pillars) Vfx.teleCircle(level, p, 2.2f, 16, FIRE, false);
                    playSound(SoundEvents.BLAZE_AMBIENT, 2f, 0.4f);
                }
                if (t == 20) {
                    for (Vec3 p : pillars) {
                        Vfx.darkPillar(level, p, SOOT, FIRE, 1.6f, 9, 24);
                        Vfx.pillar(level, p, FIRE, 1.1f, 10, 20);
                        Vfx.jet(level, FxStyle.FLAME, p, p.add(0, 6, 0), WHITE_HOT, FIRE, 0.7f, 26, 14);
                        for (LivingEntity e : SiegeDamage.enemiesAround(level, p, 2.4)) {
                            burn(e, dmg(2.6f), 3);
                            if (!big(e)) {
                                e.setDeltaMovement(e.getDeltaMovement().add(0, 1.0, 0));
                                e.hurtMarked = true;
                            }
                        }
                    }
                    Vfx.shake(level, position(), 0.6f, 24, 12);
                    playSound(SoundEvents.GENERIC_EXPLODE, 2f, 0.6f);
                    playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.4f);
                }
                if (t >= 28) {
                    pillars.clear();
                    cooldown(PILLARS, 280);
                    return true;
                }
            }
            case BRAND -> {
                if (t == 12) {
                    Vfx.darkSigil(level, position().add(0, 0.05, 0), SOOT, FIRE, 7, 30, 2f);
                    Vfx.ring(level, position().add(0, 0.1, 0), FIRE, 7, 16, 0.8f);
                    Vfx.groundRing(level, FxStyle.FLAME, HOT, 0.5f, position(), 2, 24, 0.5);
                    Vfx.shake(level, position(), 0.7f, 20, 12);
                    playSound(SoundEvents.ANVIL_LAND, 2f, 0.3f);
                    playSound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.7f);
                    for (LivingEntity e : enemiesNear(level, 7)) {
                        burn(e, dmg(1.6f), 2);
                        SiegeDamage.apply(e, ModEffects.MARKED.get(), 160, 1);
                        brands.add(new Brand(e, tickCount + 160));
                        Vfx.sigil(level, e.position().add(0, e.getBbHeight() + 0.4, 0), FIRE, 0.6f, 40, 4f);
                    }
                }
                if (t >= 22) {
                    cooldown(BRAND, 300);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    Vfx.screenFlash(level, position(), FIRE, 0.45f, 48, 30);
                    playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.4f);
                    playSound(SoundEvents.WITHER_SPAWN, 1.5f, 0.6f);
                }
                if (t == 14) {
                    HazardZone.spawn(level, HazardZone.Kind.FIRE, position(), 12, 150, dmg(0.25f), this, false);
                    Vfx.darkSigil(level, position().add(0, 0.05, 0), SOOT, FIRE, 13, 150, 1.5f);
                    Vfx.sigil(level, position().add(0, 0.07, 0), HOT, 9, 150, -2.5f);
                    Vfx.shake(level, position(), 1.2f, 40, 20);
                    playSound(SoundEvents.GENERIC_EXPLODE, 2.5f, 0.5f);
                }
                if (t > 14 && t < 150) {
                    if (t % 10 == 0) {
                        List<LivingEntity> in = enemiesNear(level, 14);
                        if (!in.isEmpty()) {
                            LivingEntity e = in.get(random.nextInt(in.size()));
                            Vec3 p = e.position();
                            Vfx.darkPillar(level, p, SOOT, FIRE, 1.4f, 10, 20);
                            Vfx.jet(level, FxStyle.FLAME, p, p.add(0, 7, 0), WHITE_HOT, FIRE, 0.7f, 24, 10);
                            for (LivingEntity o : SiegeDamage.enemiesAround(level, p, 2.2)) burn(o, dmg(2.2f) * (big(o) ? 1.5f : 1f), 3);
                            playSound(SoundEvents.BLAZE_SHOOT, 1.8f, 0.5f);
                        }
                    }
                    if (t % 20 == 0) {
                        for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 14)) {
                            if (a instanceof TurretEntity) continue;
                            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, true));
                            a.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, true));
                        }
                    }
                    if (t % 6 == 0) {
                        Vec3 at = position().add(random.nextGaussian() * 6, 0.2, random.nextGaussian() * 6);
                        Vfx.spray(level, FxStyle.FLAME, at, HOT, FIRE, 0.5f, 6, 0.1f, 1f);
                    }
                }
                if (t == 150) {
                    Vec3 c = belly();
                    Vfx.darkSphere(level, c, SOOT, FIRE, 10, 20);
                    Vfx.sphere(level, c, WHITE_HOT, 7, 14);
                    Vfx.ring(level, position().add(0, 0.1, 0), FIRE, 11, 22, 1.2f);
                    Vfx.shake(level, position(), 1.6f, 48, 30);
                    playSound(SoundEvents.GENERIC_EXPLODE, 3f, 0.4f);
                    for (LivingEntity e : enemiesNear(level, 10)) {
                        if (!big(e) && e.getHealth() < e.getMaxHealth() * 0.25f) {
                            SiegeDamage.deal(e, this, this, e.getHealth() * 3f + 10f, Element.INFERNAL, false, true);
                        }
                        burn(e, dmg(5f), 5);
                        if (!big(e)) SiegeDamage.push(e, position(), 1.4, 0.6);
                    }
                    meltDown(level, 200);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    private boolean endCharge(ServerLevel level) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        Vfx.ring(level, position().add(0, 0.1, 0), FIRE, 3.6f, 12, 0.6f);
        Vfx.spray(level, FxStyle.DEBRIS, position().add(0, 0.3, 0), 0x3A2A1A, SOOT, 0.35f, 18, 0.5f, 1f);
        Vfx.shake(level, position(), 0.6f, 20, 10);
        for (LivingEntity e : enemiesNear(level, 3.6)) {
            burn(e, dmg(1.4f), 2);
            if (!big(e)) SiegeDamage.push(e, position(), 1.2, 0.5);
        }
        playSound(SoundEvents.ANVIL_LAND, 1.4f, 0.5f);
        gored.clear();
        cooldown(CHARGE, 200);
        return true;
    }

    // ------------------------------------------------------------------ passive: furnace aura, hunger, brands

    @Override
    protected void aura(ServerLevel level) {
        float r = (3f + 0.25f * stoked()) * (molten() ? 1.5f : 1f);
        if (tickCount % 20 == 0 && inCombat()) {
            for (LivingEntity e : enemiesNear(level, r)) {
                hit(e, dmg(0.15f), Element.INFERNAL);
                SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 60, 1);
            }
            Vfx.groundRing(level, FxStyle.EMBER, molten() ? WHITE_HOT : FIRE, 0.15f, position(), r, 12, 0.02);
        }
        // the idol hungers: enemies that die burning nearby stoke the furnace
        if (tickCount % 10 == 0) {
            for (Iterator<LivingEntity> it = fuel.iterator(); it.hasNext(); ) {
                LivingEntity e = it.next();
                if (e.isDeadOrDying()) {
                    it.remove();
                    stoke(level, 1);
                    heal(getMaxHealth() * 0.01f);
                    Vfx.stream(level, FxStyle.EMBER, HOT, 0.15f, e.position().add(0, 0.6, 0), belly(), 0.6);
                } else if (e.isRemoved() || e.distanceToSqr(this) > 16 * 16 || !e.hasEffect(ModEffects.SCORCHED.get())) {
                    it.remove();
                }
            }
            if (fuel.size() < 48) {
                for (LivingEntity e : enemiesNear(level, 12)) {
                    if (e.hasEffect(ModEffects.SCORCHED.get()) && !fuel.contains(e)) fuel.add(e);
                }
            }
        }
        // brands detonate when their bearer dies
        for (Iterator<Brand> it = brands.iterator(); it.hasNext(); ) {
            Brand b = it.next();
            if (b.e().isDeadOrDying()) {
                it.remove();
                Vec3 at = b.e().position().add(0, 0.5, 0);
                Vfx.flash(level, at, HOT, 2f, 6);
                Vfx.spray(level, FxStyle.FLAME, at, WHITE_HOT, FIRE, 0.4f, 16, 0.35f, 0.5f);
                for (LivingEntity o : SiegeDamage.enemiesAround(level, at, 3.5)) burn(o, dmg(1.4f), 2);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 0.8f, 1.2f);
            } else if (b.e().isRemoved() || tickCount > b.until()) {
                it.remove();
            }
        }
        // molten state, and cooling off out of combat
        if (molten()) {
            if (tickCount % 4 == 0) {
                Vfx.particles(level, FxParticleOptions.of(FxStyle.FLAME, WHITE_HOT, 0.25f, 16), position().add(0, getBbHeight() * 0.5, 0), 2,
                        getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.02);
            }
            if (--moltenTicks <= 0) {
                setFlag(false);
                entityData.set(STOKED, 0);
                Vfx.spray(level, FxStyle.SMOKE, position().add(0, getBbHeight() * 0.6, 0), 0x6A6060, 0x2A2626, 1f, 16, 0.15f, 1f);
                playSound(SoundEvents.LAVA_EXTINGUISH, 1.5f, 0.6f);
            }
        } else if (!inCombat() && stoked() > 0 && ++coolTicks >= 120) {
            coolTicks = 0;
            entityData.set(STOKED, stoked() - 1);
        }
        if (tickCount % 8 == 0 && stoked() > 0) {
            for (int side = -1; side <= 1; side += 2) {
                Vfx.particles(level, FxParticleOptions.of(FxStyle.SMOKE, 0x2A2222, 0.5f + stoked() * 0.05f, 30), chimney(side), 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (molten()) amount *= 0.7f;
        return super.hurt(source, amount);
    }

    @Override
    protected void arrival(ServerLevel level) {
        Vfx.pillar(level, position(), FIRE, 2.2f, 10, 30);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 ch = chimney(side);
            Vfx.jet(level, FxStyle.FLAME, ch, ch.add(0, 6, 0), WHITE_HOT, FIRE, 0.6f, 30, 10);
        }
        playSound(SoundEvents.RAVAGER_ROAR, 2.5f, 0.4f);
        playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.3f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_BURN;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("lbStoked", stoked());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(STOKED, Math.min(MAX_STOKE - 1, tag.getInt("lbStoked")));
        setFlag(false);
    }

    @Override
    protected void onSkillInterrupted(int skill) {
        super.onSkillInterrupted(skill);
        offering = null;
        gored.clear();
        pillars.clear();
        if (skill == CHARGE) setDeltaMovement(0, getDeltaMovement().y, 0);
    }
}
