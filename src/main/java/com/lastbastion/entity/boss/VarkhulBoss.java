package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.entity.zombie.ZombieRank;
import com.lastbastion.entity.zombie.ZombieSpawner;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.registry.ModEntities;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Varkhul the Lich King (wave 25). A levitating skeletal sovereign in frost-rimed robes, a crown of
 * ice and a staff topped with a trapped soul.
 * <ul>
 *   <li><b>Soul Volley</b> - three homing soul bolts (also bombards the core from range).</li>
 *   <li><b>Frost Nova</b> - ring of ice that freezes nearby defenders.</li>
 *   <li><b>Death Coil</b> - drains life from a defender to heal himself.</li>
 *   <li><b>Phylactery Ward</b> (at 75% and 40%) - three soul crystals rise; Varkhul is invulnerable until they shatter.</li>
 *   <li><b>Blink</b> - teleports away when cornered.</li>
 *   <li><b>Phase 2 (50%) Army of the Dead</b> - raises shamblers and wraiths around the battlefield.</li>
 * </ul>
 */
public class VarkhulBoss extends SiegeBoss {
    public static final int VOLLEY = 3, NOVA = 4, COIL = 5, WARD = 6, BLINK = 7, ARMY = 8;
    private final List<UUID> crystals = new ArrayList<>();
    private boolean ward75, ward40;

    public VarkhulBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.VARKHUL);
        setNoGravity(true);
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.ICE ? 0f : e == Element.FIRE ? 1.3f : e == Element.HOLY ? 1.6f : super.elementMultiplier(e);
    }

    @Override
    protected double engageRange() {
        return 20;
    }

    public boolean warded() {
        return flag();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (warded() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.5, getZ(), 5, 0.6, 0.8, 0.6, 0.02);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void tickMovementAndMelee(ServerLevel level, @Nullable BlockPos core, double distTarget, double distCore) {
        Vec3 anchor = target != null ? target.position() : core != null ? Vec3.atCenterOf(core) : position();
        Vec3 flat = position().subtract(anchor).multiply(1, 0, 1);
        if (flat.lengthSqr() < 0.01) flat = new Vec3(1, 0, 0);
        double want = target != null ? 9 : 11;
        Vec3 dest = anchor.add(flat.normalize().scale(want));
        dest = dest.add(new Vec3(-flat.z, 0, flat.x).normalize().scale(Math.sin(tickCount * 0.02) * 3));
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(dest.x), (int) Math.floor(dest.z));
        dest = new Vec3(dest.x, Math.max(ground, anchor.y) + 3, dest.z);
        Vec3 v = dest.subtract(position());
        setDeltaMovement(getDeltaMovement().scale(0.7).add(v.normalize().scale(Math.min(0.12, v.length() * 0.05))));
        face(anchor);
        if (tickCount % 45 == 0) {
            fireBolt(level, target != null ? target : null, target == null && core != null ? Vec3.atCenterOf(core) : null);
        }
    }

    private void fireBolt(ServerLevel level, @Nullable LivingEntity t, @Nullable Vec3 corePos) {
        Vec3 staff = position().add(0, getBbHeight() * 0.9, 0).add(Vec3.directionFromRotation(0, getYRot() + 50).scale(0.8));
        SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.SOUL_BOLT, this, staff);
        p.payload(dmg(0.6f), Element.ICE).status(ModEffects.CHILLED, 60, 3);
        if (t != null) p.homing(t, 0.12f).aim(t.position().add(0, 1, 0), 0.7f);
        else if (corePos != null) {
            p.onImpact = (pr, at) -> {
                if (at.distanceToSqr(corePos) < 6) SiegeManager.damageCore(level, dmg(0.8f), this);
            };
            p.aim(corePos, 0.7f);
        } else return;
        p.scale(1.3f).fire(level);
        Fx.sound(level, staff, SoundEvents.EVOKER_CAST_SPELL, 1f, 0.7f);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dt, double dc) {
        float frac = getHealth() / getMaxHealth();
        if (!ward75 && frac < 0.75f) {
            ward75 = true;
            return WARD;
        }
        if (!ward40 && frac < 0.4f) {
            ward40 = true;
            return WARD;
        }
        if (!SiegeDamage.defendersAround(level, position(), 4).isEmpty() && ready(BLINK)) return BLINK;
        if (!SiegeDamage.defendersAround(level, position(), 7).isEmpty() && ready(NOVA)) return NOVA;
        if (phase() >= 2 && ready(ARMY)) return ARMY;
        if (t != null && frac < 0.7f && ready(COIL) && dt < 16) return COIL;
        if (t != null && ready(VOLLEY)) return VOLLEY;
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        setDeltaMovement(getDeltaMovement().scale(0.6));
        switch (skill) {
            case VOLLEY -> {
                if (target != null) face(target.position());
                if (t == 10 || t == 16 || t == 22) fireBolt(level, target, null);
                if (t >= 30) {
                    cooldown(VOLLEY, 90);
                    return true;
                }
            }
            case NOVA -> {
                if (t == 18) {
                    for (double r = 1; r <= 7; r += 1) Fx.ring(level, position(), r, ParticleTypes.SNOWFLAKE, (int) (r * 8));
                    Fx.sound(level, position(), SoundEvents.GLASS_BREAK, 2f, 0.5f);
                    HazardZone.spawn(level, HazardZone.Kind.FROST, position().subtract(0, 2.5, 0), 6f, 100, damageScale, this, true);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 7.5)) {
                        SiegeDamage.deal(e, this, this, dmg(0.8f), Element.ICE, false);
                        SiegeDamage.apply(e, ModEffects.FROZEN.get(), 50, 0);
                    }
                }
                if (t >= 30) {
                    cooldown(NOVA, 170);
                    return true;
                }
            }
            case COIL -> {
                if (target != null) {
                    face(target.position());
                    if (t >= 10 && t <= 40 && t % 5 == 0) {
                        Fx.line(level, target.position().add(0, 1, 0), position().add(0, 1.6, 0), Fx.dust(0x40FF90, 1f), 0.35);
                        if (SiegeDamage.deal(target, this, this, dmg(0.3f), Element.SHADOW, false)) heal(dmg(0.3f) * 3);
                        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.4, getZ(), 1, 0.3, 0.2, 0.3, 0);
                    }
                }
                if (t >= 44) {
                    cooldown(COIL, 220);
                    return true;
                }
            }
            case WARD -> {
                if (t == 20) summonCrystals(level);
                return t >= 40;
            }
            case BLINK -> {
                if (t == 6) {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.2);
                    Vec3 away = position().add(new Vec3(random.nextGaussian(), 0, random.nextGaussian()).normalize().scale(9));
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(away.x), (int) Math.floor(away.z));
                    teleportTo(away.x, y + 3, away.z);
                    level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
                    Fx.sound(level, position(), SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
                }
                if (t >= 12) {
                    cooldown(BLINK, 200);
                    return true;
                }
            }
            case ARMY -> {
                if (t == 30) {
                    BlockPos c = core();
                    Vec3 centre = c != null ? Vec3.atCenterOf(c) : position();
                    for (int i = 0; i < 8; i++) {
                        double a = random.nextDouble() * Math.PI * 2;
                        Vec3 p = centre.add(Math.cos(a) * 14, 0, Math.sin(a) * 14);
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
                        ZombieKind k = i % 4 == 0 ? ZombieKind.WRAITH : ZombieKind.SHAMBLER;
                        SiegeZombie z = ZombieSpawner.spawn(level, k, ZombieRank.CORRUPTED, 0, new Vec3(p.x, y, p.z), true, true);
                        if (z != null) z.setMinion(true);
                    }
                    Fx.sound(level, position(), SoundEvents.WITHER_AMBIENT, 2f, 0.5f);
                }
                if (t >= 45) {
                    cooldown(ARMY, 600);
                    return true;
                }
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    private void summonCrystals(ServerLevel level) {
        crystals.clear();
        setFlag(true);
        Fx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3 + random.nextDouble();
            Vec3 p = position().add(Math.cos(a) * 7, 0, Math.sin(a) * 7);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
            PhylacteryEntity c = ModEntities.PHYLACTERY.get().create(level);
            if (c == null) continue;
            c.moveTo(p.x, y + 1.5, p.z);
            c.bind(this, 80 * damageScale * (1 + phase() * 0.5f));
            level.addFreshEntity(c);
            crystals.add(c.getUUID());
            Fx.skyBeam(level, new Vec3(p.x, y, p.z), 0x5AD8FF, 10, 0.6);
        }
    }

    @Override
    protected void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (warded() && tickCount % 10 == 0) {
            crystals.removeIf(id -> {
                Entity e = level.getEntity(id);
                return e == null || !e.isAlive();
            });
            if (crystals.isEmpty()) {
                setFlag(false);
                Fx.sound(level, position(), SoundEvents.GLASS_BREAK, 2f, 0.4f);
                level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            } else {
                for (UUID id : crystals) {
                    Entity e = level.getEntity(id);
                    if (e != null) Fx.line(level, e.position().add(0, 0.5, 0), position().add(0, 1.5, 0), Fx.dust(0x5AD8FF, 0.8f), 0.6);
                }
            }
        }
        if (tickCount % 4 == 0) level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(), 2, 0.5, 0.1, 0.5, 0.01);
    }

    @Override
    protected String phaseLine(int phase) {
        return "calls forth the ARMY OF THE DEAD!";
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag l = new ListTag();
        for (UUID u : crystals) l.add(NbtUtils.createUUID(u));
        tag.put("lbCrystals", l);
        tag.putBoolean("lbW75", ward75);
        tag.putBoolean("lbW40", ward40);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        crystals.clear();
        for (Tag t : tag.getList("lbCrystals", Tag.TAG_INT_ARRAY)) crystals.add(NbtUtils.loadUUID(t));
        ward75 = tag.getBoolean("lbW75");
        ward40 = tag.getBoolean("lbW40");
    }
}
