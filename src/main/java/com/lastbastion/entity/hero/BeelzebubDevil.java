package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.zombie.CrawlerZombie;
import com.lastbastion.entity.zombie.RunnerZombie;
import com.lastbastion.entity.zombie.ShamblerZombie;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Beelzebub, Lord of the Flies: a bloated insect-king in a moth-eaten royal robe, red compound eyes burning
 * over a court of flies. He rots the horde from range and eats what falls.
 * <ul>
 *   <li><b>Swarm Bolts</b> - clots of flies that burst on impact and eat away armour.</li>
 *   <li><b>Plague Swarm</b> - a living swarm that hunts the thickest crowd for eight seconds.</li>
 *   <li><b>Gluttony</b> - lunges onto a broken or small enemy and devours it whole; each meal gorges him (more damage).</li>
 *   <li><b>Pestilent Rain</b> - a cloud of rot rains on a crowd.</li>
 *   <li><b>Brood of Flies</b> - giant flies orbit him, chewing on anything near.</li>
 *   <li><b>Lord of Decay</b> - everything nearby rots; his allies feast and heal.</li>
 *   <li><b>Bile Torrent</b> - a cone of corrosive bile.</li>
 *   <li><b>ULT Feast of a Thousand Flies</b> - he dissolves into a storm of flies that sweeps through the horde,
 *   untouchable, devouring and healing, then re-forms in a burst of maggots.</li>
 * </ul>
 */
public class BeelzebubDevil extends DevilEntity {
    public static final int SWARM = 6, GLUTTONY = 7, PESTILENCE = 8, BROOD = 9, DECAY = 10, BILE = 11;
    public static final int ROT = 0x8DBE3C, BILE_GREEN = 0xC8FF60, CHITIN = 0x1A2214, EYE = 0xFF2A1A, FLY = 0x0E0E0C;
    private static final EntityDataAccessor<Integer> GORGED = SynchedEntityData.defineId(BeelzebubDevil.class, EntityDataSerializers.INT);

    private static final class Swarm {
        Vec3 pos;
        int life;

        Swarm(Vec3 pos, int life) {
            this.pos = pos;
            this.life = life;
        }
    }

    private final List<Swarm> swarms = new ArrayList<>();
    private int brood, gorgeDecay;
    @Nullable
    private LivingEntity meal;
    private Vec3 storm = Vec3.ZERO;

    public BeelzebubDevil(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.BEELZEBUB);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(GORGED, 0);
    }

    /** Meals in his belly (0-5): more damage, a fatter model. */
    public int gorged() {
        return entityData.get(GORGED);
    }

    /** True while dissolved into the fly storm (ultimate). */
    public boolean dissolved() {
        return flag();
    }

    @Override
    protected int fireColor() {
        return ROT;
    }

    @Override
    protected int fireCore() {
        return BILE_GREEN;
    }

    @Override
    public Role role() {
        return Role.SUPPORT;
    }

    @Override
    protected boolean isRanged() {
        return true;
    }

    @Override
    protected double attackRange() {
        return 12;
    }

    @Override
    protected double preferredRange() {
        return 8;
    }

    @Override
    protected int attackWindup() {
        return 9;
    }

    @Override
    protected int attackInterval() {
        return 16;
    }

    @Override
    public float dmg(float mult) {
        return super.dmg(mult) * (1f + 0.1f * gorged());
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"Bzzzz... the feast is laid. Where are the guests?", "The Lord of the Flies answers. Hungry."};
            case ULT -> new String[]{"FEAST, MY CHILDREN! FEAST!", "A THOUSAND MOUTHS, ONE HUNGER!"};
            case BOSS -> new String[]{"Such a big meal...", "That one will feed the swarm for a century."};
            case KILL -> new String[]{"*wet chewing*", "Rot is a gift."};
            case LOW_HP -> new String[]{"The swarm... does not... die!", "You swat at a god of flies?"};
            case LEVEL_UP -> new String[]{"More mouths. More hunger."};
            case IDLE -> new String[]{"*a thousand wings buzz in chorus*", "Everything rots. I only hurry it along."};
            default -> new String[0];
        };
    }

    private void rot(LivingEntity e, float amount) {
        if (hitKills(e, amount, Element.INFERNAL)) feed(0.015f);
        SiegeDamage.applyStacking(e, ModEffects.CORRODED.get(), 100, 4);
    }

    private void flies(ServerLevel level, Vec3 at, float radius, int count) {
        Vfx.spray(level, FxStyle.DEBRIS, at, FLY, 0x2A2A20, 0.12f, count, 0.25f + radius * 0.05f, 0.1f);
        Vfx.particles(level, FxParticleOptions.of(FxStyle.SPARK, ROT, 0.08f, 10), at, count / 3 + 1, radius * 0.4, 0.4, radius * 0.4, 0.05);
    }

    // ------------------------------------------------------------------ basic attack

    @Override
    protected void basicAttack(ServerLevel level, LivingEntity t, int combo) {
        Vec3 hand = position().add(0, getBbHeight() * 0.75, 0).add(look().scale(0.8));
        SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.SPORE, this, hand).payload(dmg(1.0f), Element.INFERNAL)
                .status(ModEffects.CORRODED, 100, 4);
        p.area(2f);
        p.onEntityHit = (pr, e) -> {
            gainUlt(pr.damage * ultGainPerDamage());
            flies(level, e.position().add(0, e.getBbHeight() * 0.5, 0), 1.5f, 10);
        };
        p.aim(t.position().add(0, t.getBbHeight() * 0.5, 0), 1.3f).scale(1.3f).fire(level);
        flies(level, hand, 0.6f, 6);
        playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 1.2f, 0.5f);
    }

    // ------------------------------------------------------------------ skills

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (ready(DECAY) && (crowd(level, position(), 10) >= 6 || hurtAlly(level, 10, 0.5f) != null)) return DECAY;
        if (ready(GLUTTONY)) {
            for (LivingEntity e : enemiesNear(level, 6)) {
                boolean small = e instanceof ShamblerZombie || e instanceof RunnerZombie || e instanceof CrawlerZombie;
                if (!big(e) && (small || e.getHealth() < e.getMaxHealth() * 0.35f)) {
                    meal = e;
                    return GLUTTONY;
                }
            }
        }
        if (ready(BROOD) && crowd(level, position(), 6) >= 3) return BROOD;
        if (t == null) return 0;
        if (ready(BILE) && dist < 7 && crowd(level, t.position(), 4) >= 2) return BILE;
        if (ready(SWARM) && crowd(level, position(), 20) >= 4) return SWARM;
        if (ready(PESTILENCE) && bestCluster(level, 18, 5, 4) != null) return PESTILENCE;
        return 0;
    }

    @Override
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return t != null && (crowd(level, position(), 14) >= 8 || (big(t) && dist < 14) || getHealth() < getMaxHealth() * 0.3f);
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        switch (skill) {
            case SWARM -> {
                if (t == 10) {
                    swarms.add(new Swarm(position().add(0, getBbHeight() * 0.7, 0), 160));
                    flies(level, position().add(0, 2, 0), 2f, 30);
                    playSound(SoundEvents.BEEHIVE_EXIT, 2f, 0.4f);
                    playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 2f, 0.5f);
                }
                if (t >= 18) {
                    cooldown(SWARM, 200);
                    return true;
                }
            }
            case GLUTTONY -> {
                LivingEntity m = meal;
                if (m == null || !m.isAlive()) {
                    cooldown(GLUTTONY, 60);
                    return true;
                }
                faceNow(m.position());
                if (t <= 6 && distanceTo(m) > 2.2) {
                    Vec3 v = m.position().subtract(position()).normalize().scale(0.9);
                    setDeltaMovement(v.x, 0.15, v.z);
                    hasImpulse = true;
                }
                if (t == 10) {
                    Vec3 at = m.position().add(0, m.getBbHeight() * 0.5, 0);
                    Vfx.spray(level, FxStyle.DROP, at, 0x5A0A08, 0x2A0404, 0.2f, 20, 0.35f, 0.4f);
                    flies(level, at, 1.2f, 16);
                    SiegeDamage.deal(m, this, this, m.getMaxHealth() * 4f + 40f, Element.INFERNAL, false, true);
                    feed(0.08f);
                    entityData.set(GORGED, Math.min(5, gorged() + 1));
                    gorgeDecay = 600;
                    playSound(SoundEvents.GENERIC_EAT, 2f, 0.4f);
                    playSound(SoundEvents.PLAYER_BURP, 1.5f, 0.5f);
                }
                if (t >= 20) {
                    meal = null;
                    cooldown(GLUTTONY, 240);
                    return true;
                }
            }
            case PESTILENCE -> {
                if (t == 12) {
                    Vec3 c = bestCluster(level, 18, 5, 1);
                    if (c == null && target != null) c = target.position();
                    if (c != null) {
                        HazardZone.spawn(level, HazardZone.Kind.PLAGUE_RAIN, c, 7, 140, dmg(0.3f), this, false);
                        Vfx.darkSphere(level, c.add(0, 7, 0), 0x1A2210, ROT, 4f, 60);
                        Vfx.darkRing(level, c.add(0, 0.05, 0), 0x1A2210, 7, 30, 0.6f);
                    }
                    playSound(SoundEvents.WEATHER_RAIN_ABOVE, 2f, 0.5f);
                    playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 1.5f, 0.4f);
                }
                if (t >= 20) {
                    cooldown(PESTILENCE, 300);
                    return true;
                }
            }
            case BROOD -> {
                if (t == 8) {
                    brood = 240;
                    Vfx.darkVortex(level, position(), FLY, ROT, 3.2f, 3.4f, 240, this);
                    playSound(SoundEvents.BEEHIVE_EXIT, 2f, 0.3f);
                }
                if (t >= 14) {
                    cooldown(BROOD, 360);
                    return true;
                }
            }
            case DECAY -> {
                if (t == 12) {
                    Vfx.darkSphere(level, position().add(0, 1.2, 0), 0x1A2210, ROT, 10f, 16);
                    Vfx.ring(level, position().add(0, 0.1, 0), ROT, 10, 18, 0.8f);
                    flies(level, position().add(0, 1.5, 0), 4f, 40);
                    playSound(SoundEvents.WITHER_SHOOT, 1.5f, 0.4f);
                    for (LivingEntity e : enemiesNear(level, 10)) {
                        SiegeDamage.applyStacking(e, ModEffects.CORRODED.get(), 200, 4);
                        SiegeDamage.applyStacking(e, ModEffects.CORRODED.get(), 200, 4);
                        rot(e, dmg(1.2f));
                        SiegeDamage.dot(e, dmg(0.4f), Element.INFERNAL);
                    }
                    for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 10)) {
                        if (a instanceof com.lastbastion.entity.turret.TurretEntity) continue;
                        a.heal(a.getMaxHealth() * 0.12f);
                        Vfx.spray(level, FxStyle.DROP, a.position().add(0, 1, 0), BILE_GREEN, ROT, 0.15f, 8, 0.1f, 1f);
                    }
                }
                if (t >= 22) {
                    cooldown(DECAY, 420);
                    return true;
                }
            }
            case BILE -> {
                if (target != null) faceTowards(target.position());
                if (t >= 8 && t <= 22 && t % 2 == 0) {
                    Vec3 mouth = position().add(0, getBbHeight() * 0.82, 0).add(look().scale(1));
                    Vec3 dir = look();
                    Vfx.jet(level, FxStyle.DROP, mouth, mouth.add(dir.scale(8)).add(0, -1.5, 0), BILE_GREEN, ROT, 0.35f, 16, 30);
                    Vfx.jet(level, FxStyle.BUBBLE, mouth, mouth.add(dir.scale(7)), BILE_GREEN, 0x5A8A2A, 0.25f, 8, 25);
                    double cos = Math.cos(Math.toRadians(35));
                    for (LivingEntity e : enemiesNear(level, 8.5)) {
                        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                        if (to.lengthSqr() > 0.5 && to.normalize().dot(dir) < cos) continue;
                        rot(e, dmg(0.35f));
                        SiegeDamage.apply(e, ModEffects.SOAKED.get(), 80, 0);
                    }
                    if (t == 8) playSound(SoundEvents.SLIME_SQUISH, 2f, 0.4f);
                }
                if (t >= 26) {
                    cooldown(BILE, 160);
                    return true;
                }
            }
            case ULT -> {
                if (t == 1) {
                    setFlag(true);
                    setInvisible(true);
                    setInvulnerable(true);
                    storm = position();
                    Vfx.darkSphere(level, position().add(0, 1.5, 0), FLY, ROT, 4f, 14);
                    flies(level, position().add(0, 1.5, 0), 3f, 60);
                    playSound(SoundEvents.BEEHIVE_EXIT, 3f, 0.3f);
                    playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 3f, 0.3f);
                }
                if (t > 1 && t < 140) {
                    Vec3 goal = bestCluster(level, 20, 4.5, 1);
                    if (goal == null) goal = storm;
                    Vec3 step = goal.subtract(storm);
                    if (step.lengthSqr() > 0.16) storm = storm.add(step.normalize().scale(0.4));
                    teleportTo(storm.x, storm.y, storm.z);
                    if (t % 4 == 0) {
                        Vfx.darkVortex(level, storm, FLY, ROT, 3.6f, 3.2f, 8, null);
                        flies(level, storm.add(0, 1.4, 0), 4f, 24);
                    }
                    if (t % 8 == 0) {
                        int n = 0;
                        for (LivingEntity e : SiegeDamage.enemiesAround(level, storm, 4.5)) {
                            rot(e, dmg(1.1f));
                            n++;
                        }
                        heal(getMaxHealth() * 0.004f * n);
                    }
                    if (t % 20 == 0) playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 2.5f, 0.35f);
                }
                if (t >= 140) {
                    setFlag(false);
                    setInvisible(false);
                    setInvulnerable(false);
                    Vfx.darkSphere(level, position().add(0, 1.2, 0), 0x1A2210, ROT, 6f, 14);
                    Vfx.spray(level, FxStyle.DROP, position().add(0, 1.2, 0), 0xE8E0C8, 0xC8C0A0, 0.2f, 40, 0.6f, 0.5f);
                    for (LivingEntity e : enemiesNear(level, 6)) {
                        rot(e, dmg(3f));
                        SiegeDamage.push(e, position(), 1.0, 0.4);
                    }
                    playSound(SoundEvents.SLIME_BLOCK_BREAK, 2f, 0.5f);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ passive: court of flies, swarms, brood

    @Override
    protected void aura(ServerLevel level) {
        if (gorgeDecay > 0 && --gorgeDecay == 0 && gorged() > 0) {
            entityData.set(GORGED, gorged() - 1);
            if (gorged() > 0) gorgeDecay = 300;
        }
        if (tickCount % 20 == 0) {
            for (LivingEntity e : enemiesNear(level, 5)) rot(e, dmg(0.12f));
            if (!dissolved()) flies(level, position().add(0, getBbHeight() * 0.7, 0), 1.6f, 6);
        }
        for (Iterator<Swarm> it = swarms.iterator(); it.hasNext(); ) {
            Swarm s = it.next();
            if (--s.life <= 0) {
                it.remove();
                continue;
            }
            if (s.life % 5 == 0) {
                Vec3 goal = null;
                int bestN = 0;
                for (LivingEntity e : SiegeDamage.enemiesAround(level, s.pos, 20)) {
                    int n = crowd(level, e.position(), 3.5);
                    if (n > bestN) {
                        bestN = n;
                        goal = e.position().add(0, 1, 0);
                    }
                }
                if (goal != null) {
                    Vec3 step = goal.subtract(s.pos);
                    if (step.lengthSqr() > 0.25) s.pos = s.pos.add(step.normalize().scale(Math.min(2.2, step.length())));
                }
                flies(level, s.pos, 2.2f, 14);
                Vfx.darkVortex(level, s.pos.add(0, -1, 0), FLY, ROT, 2.4f, 2.4f, 6, null);
            }
            if (s.life % 10 == 0) {
                for (LivingEntity e : SiegeDamage.enemiesAround(level, s.pos, 3)) rot(e, dmg(0.6f));
            }
        }
        if (brood > 0) {
            brood--;
            if (brood % 10 == 0) {
                for (LivingEntity e : enemiesNear(level, 5)) rot(e, dmg(0.5f));
                double a = tickCount * 0.3;
                for (int i = 0; i < 4; i++) {
                    Vec3 f = position().add(Math.cos(a + i * Math.PI / 2) * 3, getBbHeight() * 0.6, Math.sin(a + i * Math.PI / 2) * 3);
                    flies(level, f, 0.6f, 5);
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && !dissolved() && random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2, r = 0.8 + random.nextDouble() * 1.4;
            level().addParticle(FxParticleOptions.of(FxStyle.DEBRIS, FLY, 0.06f, 12), getX() + Math.cos(a) * r,
                    getY() + 0.8 + random.nextDouble() * getBbHeight(), getZ() + Math.sin(a) * r,
                    (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.2);
        }
    }

    @Override
    protected void arrival(ServerLevel level) {
        flies(level, position().add(0, 1.5, 0), 4f, 80);
        Vfx.darkVortex(level, position(), FLY, ROT, 3f, 4f, 40, this);
        playSound(SoundEvents.BEEHIVE_EXIT, 3f, 0.3f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (dissolved()) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_LOOP;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("lbGorged", gorged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(GORGED, tag.getInt("lbGorged"));
        setInvisible(false);
        setInvulnerable(false);
    }
}
