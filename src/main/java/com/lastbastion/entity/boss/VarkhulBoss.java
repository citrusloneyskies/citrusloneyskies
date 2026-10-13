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
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.registry.ModEntities;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Varkhul the Lich King: a theatrical, contemptuous sovereign who floats above the battlefield and
 * never lets anyone close. He kites at range with Soul Volleys, answers anyone who rushes him with a
 * Frost Nova and a Blink (leaving a frost mine behind), and seals a single defender in a Glacial Tomb.
 * Blizzard calls down ice spears on marked circles around every defender. At 75% and 40% health he
 * raises three phylactery crystals and is untouchable until they shatter. At half health the Army of
 * the Dead rises around the core and Soul Harvest drains everyone inside a marked circle to heal him;
 * staggering him or leaving the circle breaks it.
 */
public class VarkhulBoss extends SiegeBoss {
    public static final int VOLLEY = 3, NOVA = 4, COIL = 5, WARD = 6, BLINK = 7, ARMY = 8, BLIZZARD = 9, TOMB = 10, HARVEST = 11;
    public static final int FROST = 0x5AD8FF, SOULC = 0x9AF0FF;
    private final List<UUID> crystals = new ArrayList<>();
    private boolean ward75, ward40;
    private final List<Vec3> marks = new ArrayList<>();
    @Nullable
    private LivingEntity tombed;
    private Vec3 tombAt = Vec3.ZERO;

    public VarkhulBoss(EntityType<? extends SiegeBoss> type, Level level) {
        super(type, level, BossKind.VARKHUL);
        setNoGravity(true);
    }

    @Override
    public float elementMultiplier(Element e) {
        return e == Element.ICE ? 0f : e == Element.FIRE ? 1.3f : e == Element.HOLY ? 1.6f : super.elementMultiplier(e);
    }

    @Override
    public int dangerColor() {
        return FROST;
    }

    @Override
    protected String epithet() {
        return "Sovereign of the Frozen Dead";
    }

    @Override
    protected int introLength() {
        return 56;
    }

    @Override
    protected String[] lines(Lines which) {
        return switch (which) {
            case ARRIVE -> new String[]{"Bow before your king. Or don't - the dead are not particular.",
                    "Such warmth in these walls. How... offensive."};
            case TAUNT -> new String[]{"You flail like the living always do.", "Is this a siege, or a eulogy?", "Come closer. I insist."};
            case PHASE -> new String[]{"ENOUGH! My subjects - rise and feast!", "You mistake my patience for weakness. RISE, my legions!"};
            case STAGGER -> new String[]{"Impudent... worm...!", "You dare lay hands on a KING?"};
            case KILL -> new String[]{"Another subject for my court.", "Kneel forever.", "How fragile you are."};
            case DEATH -> new String[]{"I... was... eternal...", "My phylactery... no... NO..."};
            case CORE -> new String[]{"Let this little sun go cold."};
        };
    }

    @Override
    protected void introTick(ServerLevel level, int t) {
        if (t == 2) {
            Vfx.pillar(level, ground(), FROST, 1.4f, 6, 20);
            Vfx.spray(level, FxStyle.FLAKE, position().add(0, 1.5, 0), 0xFFFFFF, FROST, 0.3f, 30, 0.2f, 0);
        }
        if (t == 20) {
            Vfx.spray(level, FxStyle.SHARD, position().add(0, 1.5, 0), 0xBFF4FF, 0xFFFFFF, 0.4f, 30, 0.45f, 0.3f);
            Fx.sound(level, position(), SoundEvents.GLASS_BREAK, 2f, 0.5f);
        }
        if (t == 38) {
            Vfx.sigil(level, ground().add(0, 0.05, 0), FROST, 4f, 40, 1.5f);
            Fx.sound(level, position(), SoundEvents.EVOKER_PREPARE_WOLOLO, 2f, 0.5f);
        }
        if (t > 30) setDeltaMovement(0, 0.08, 0);
    }

    @Override
    protected double engageRange() {
        return 20;
    }

    public boolean warded() {
        return flag();
    }

    private Vec3 ground() {
        Level l = level();
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(getX()), (int) Math.floor(getZ()));
        return new Vec3(getX(), Math.min(y, getY()), getZ());
    }

    private Vec3 groundAt(Vec3 p) {
        int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
        return new Vec3(p.x, y, p.z);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (warded() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel sl && tickCount % 4 == 0) {
                Vfx.flash(sl, position().add(0, 1.5, 0), FROST, 1.2f, 4);
                Fx.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_HIT, 1f, 0.6f);
            }
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
        int g = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(dest.x), (int) Math.floor(dest.z));
        dest = new Vec3(dest.x, Math.max(g, anchor.y) + 3, dest.z);
        Vec3 v = dest.subtract(position());
        setDeltaMovement(getDeltaMovement().scale(0.7).add(v.normalize().scale(Math.min(0.12, v.length() * 0.05))));
        face(anchor);
        if (tickCount % 45 == 0) fireBolt(level, target, target == null && core != null ? Vec3.atCenterOf(core) : null);
    }

    private Vec3 staff() {
        return position().add(0, getBbHeight() * 1.05, 0).add(Vec3.directionFromRotation(0, getYRot() - 60).scale(0.9));
    }

    private void fireBolt(ServerLevel level, @Nullable LivingEntity t, @Nullable Vec3 corePos) {
        Vec3 from = staff();
        SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.SOUL_BOLT, this, from);
        p.payload(dmg(0.6f), Element.ICE).status(ModEffects.CHILLED, 60, 3);
        if (t != null) p.homing(t, 0.12f).aim(t.position().add(0, 1, 0), 0.7f);
        else if (corePos != null) {
            p.onImpact = (pr, at) -> {
                if (at.distanceToSqr(corePos) < 6) SiegeManager.damageCore(level, dmg(0.8f), this);
            };
            p.aim(corePos, 0.7f);
        } else return;
        p.scale(1.3f).fire(level);
        Vfx.flash(level, from, FROST, 0.8f, 4);
        Fx.sound(level, from, SoundEvents.EVOKER_CAST_SPELL, 1f, 0.7f);
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
        var near = SiegeDamage.defendersAround(level, position(), 7);
        if (!SiegeDamage.defendersAround(level, position(), 4).isEmpty() && ready(BLINK)) return BLINK;
        if (!near.isEmpty() && ready(NOVA)) return NOVA;
        if (phase() >= 2 && ready(ARMY)) return ARMY;
        if (phase() >= 2 && ready(HARVEST) && SiegeDamage.defendersAround(level, position(), 13).size() >= 2) return HARVEST;
        if (ready(BLIZZARD) && SiegeDamage.defendersAround(level, position(), 20).size() >= 1 && tickCount > 140) return BLIZZARD;
        if (ready(TOMB) && t != null && dt < 16 && !(t instanceof TurretEntity)) {
            tombed = t;
            return TOMB;
        }
        if (t != null && frac < 0.7f && ready(COIL) && dt < 16) return COIL;
        if (t != null && ready(VOLLEY)) return VOLLEY;
        if (t != null && random.nextInt(500) == 0) speak(Lines.TAUNT, false);
        return 0;
    }

    @Override
    protected boolean tickSkill(ServerLevel level, int skill, int t) {
        setDeltaMovement(getDeltaMovement().scale(0.6));
        switch (skill) {
            case VOLLEY -> {
                if (target != null) face(target.position());
                int shots = phase() >= 2 ? 5 : 3;
                if (t == 4) Vfx.spray(level, FxStyle.SOUL, staff(), SOULC, FROST, 0.35f, 10, 0.1f, 0);
                if (t >= 10 && t < 10 + shots * 4 && (t - 10) % 4 == 0) fireBolt(level, target, null);
                if (t >= 14 + shots * 4) {
                    cooldown(VOLLEY, 90);
                    return true;
                }
            }
            case NOVA -> {
                if (t == 1) teleCircle(level, ground(), 7.5f, 18);
                if (t == 18) {
                    Vec3 g = ground();
                    Vfx.ring(level, g.add(0, 0.1, 0), FROST, 7.5f, 14, 1.2f);
                    Vfx.sphere(level, position().add(0, 1.5, 0), 0xBFF4FF, 5f, 10);
                    Vfx.spray(level, FxStyle.SHARD, g.add(0, 0.3, 0), 0xBFF4FF, 0xFFFFFF, 0.4f, 40, 0.6f, -0.4f);
                    Fx.sound(level, position(), SoundEvents.GLASS_BREAK, 2f, 0.5f);
                    HazardZone.spawn(level, HazardZone.Kind.FROST, g, 6f, 100, damageScale, this, true);
                    for (LivingEntity e : SiegeDamage.defendersAround(level, g, 7.5)) {
                        SiegeDamage.deal(e, this, this, dmg(0.8f), Element.ICE, false);
                        SiegeDamage.apply(e, ModEffects.FROZEN.get(), 45, 0);
                    }
                }
                if (t >= 30) {
                    cooldown(NOVA, 170);
                    return true;
                }
            }
            case COIL -> {
                if (target != null && target.isAlive() && distanceTo(target) < 18) {
                    face(target.position());
                    if (t == 8) Vfx.tether(level, this, getBbHeight() * 0.7f, target, 0x40FF90, 0.1f, 36);
                    if (t >= 10 && t <= 40 && t % 5 == 0) {
                        if (SiegeDamage.deal(target, this, this, dmg(0.3f), Element.SHADOW, false)) heal(dmg(0.3f) * 3);
                        Vfx.spray(level, FxStyle.SOUL, target.position().add(0, 1, 0), 0x40FF90, 0xFFFFFF, 0.3f, 4, 0.1f, 0.5f);
                    }
                } else if (t > 8) {
                    cooldown(COIL, 120);
                    return true;
                }
                if (t >= 44) {
                    cooldown(COIL, 220);
                    return true;
                }
            }
            case WARD -> {
                if (t == 2) say(random.nextBoolean() ? "You cannot kill what has already died." : "My heart is not in this body, fools.");
                if (t == 20) summonCrystals(level);
                return t >= 40;
            }
            case BLINK -> {
                if (t == 1) {
                    teleCircle(level, ground(), 3f, 30);
                    Vfx.spray(level, FxStyle.SOUL, position().add(0, 1.2, 0), FROST, 0x1A3A6A, 0.4f, 20, 0.3f, 0);
                }
                if (t == 6) {
                    Vec3 from = position();
                    HazardZone.spawn(level, HazardZone.Kind.FROST, ground(), 3f, 120, damageScale * 1.3f, this, true);
                    Vec3 away = position().add(new Vec3(random.nextGaussian(), 0, random.nextGaussian()).normalize().scale(10));
                    Vec3 g = groundAt(away);
                    teleportTo(away.x, g.y + 3, away.z);
                    Vfx.dash(level, from.add(0, 1.5, 0), position().add(0, 1.5, 0), FROST, 1f, 10);
                    Vfx.spray(level, FxStyle.FLAKE, position().add(0, 1.5, 0), 0xFFFFFF, FROST, 0.3f, 24, 0.25f, 0);
                    Fx.sound(level, position(), SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
                }
                if (t >= 12) {
                    cooldown(BLINK, 200);
                    return true;
                }
            }
            case ARMY -> {
                BlockPos c = core();
                Vec3 centre = c != null ? Vec3.atCenterOf(c) : position();
                if (t == 1) {
                    marks.clear();
                    for (int i = 0; i < 8; i++) {
                        double a = random.nextDouble() * Math.PI * 2;
                        Vec3 p = groundAt(centre.add(Math.cos(a) * 14, 0, Math.sin(a) * 14));
                        marks.add(p);
                        Vfx.sigil(level, p.add(0, 0.05, 0), 0x40FF90, 1.4f, 34, 2);
                    }
                    say("Rise, my subjects. Your king commands it!");
                    Fx.sound(level, position(), SoundEvents.EVOKER_PREPARE_SUMMON, 2f, 0.5f);
                }
                if (t == 30) {
                    for (int i = 0; i < marks.size(); i++) {
                        Vec3 p = marks.get(i);
                        ZombieKind k = i % 4 == 0 ? ZombieKind.WRAITH : ZombieKind.SHAMBLER;
                        SiegeZombie z = ZombieSpawner.spawn(level, k, ZombieRank.CORRUPTED, 0, p, true, true);
                        if (z != null) z.setMinion(true);
                        Vfx.spray(level, FxStyle.SOUL, p.add(0, 0.5, 0), 0x40FF90, 0xFFFFFF, 0.3f, 10, 0.15f, 1f);
                    }
                    Fx.sound(level, position(), SoundEvents.WITHER_AMBIENT, 2f, 0.5f);
                }
                if (t >= 45) {
                    cooldown(ARMY, 600);
                    return true;
                }
            }
            case BLIZZARD -> {
                if (t == 1) {
                    marks.clear();
                    for (LivingEntity e : SiegeDamage.defendersAround(level, position(), 20)) {
                        if (marks.size() >= 8) break;
                        marks.add(groundAt(e.position()));
                        if (marks.size() < 8) {
                            Vec3 off = e.position().add(random.nextGaussian() * 3, 0, random.nextGaussian() * 3);
                            marks.add(groundAt(off));
                        }
                    }
                    while (marks.size() < 6) {
                        double a = random.nextDouble() * Math.PI * 2;
                        marks.add(groundAt(position().add(Math.cos(a) * (4 + random.nextDouble() * 8), 0, Math.sin(a) * (4 + random.nextDouble() * 8))));
                    }
                    say(random.nextBoolean() ? "Winter takes all." : "Let the heavens freeze!");
                    Fx.sound(level, position(), SoundEvents.EVOKER_PREPARE_ATTACK, 2f, 0.5f);
                }
                for (int i = 0; i < marks.size(); i++) {
                    int t0 = 4 + i * 3;
                    Vec3 at = marks.get(i);
                    if (t == t0) teleCircle(level, at, 2.4f, 24);
                    if (t == t0 + 14) {
                        SiegeProjectile p = SiegeProjectile.create(level, SiegeProjectile.ProjKind.ICE_SPEAR, this, at.add(random.nextGaussian(), 20, random.nextGaussian()));
                        p.payload(dmg(1.1f), Element.ICE).area(2.4f).status(ModEffects.CHILLED, 80, 3);
                        p.gravity = 0;
                        p.onImpact = (pr, hit) -> {
                            Vfx.spray(level, FxStyle.SHARD, hit, 0xBFF4FF, 0xFFFFFF, 0.35f, 14, 0.45f, 0.8f);
                            Vfx.ring(level, hit.add(0, 0.1, 0), FROST, 2.4f, 8, 0.6f);
                        };
                        p.aim(at, 2f).scale(2.2f).fire(level);
                    }
                }
                if (t >= 4 + marks.size() * 3 + 30) {
                    cooldown(BLIZZARD, 280);
                    return true;
                }
            }
            case TOMB -> {
                LivingEntity v = tombed;
                if (t == 1 && v != null) {
                    tombAt = v.position();
                    face(tombAt);
                    teleCircle(level, tombAt, 1.8f, 22);
                    Vfx.spray(level, FxStyle.FLAKE, tombAt.add(0, 0.2, 0), 0xFFFFFF, FROST, 0.3f, 14, 0.15f, 1f);
                }
                if (t == 22) {
                    if (v != null && v.isAlive() && v.position().distanceToSqr(tombAt) < 2.2 * 2.2) {
                        SiegeDamage.deal(v, this, this, dmg(1.2f), Element.ICE, false);
                        SiegeDamage.apply(v, ModEffects.FROZEN.get(), 60, 0);
                        Vfx.pillar(level, v.position(), 0xBFF4FF, 1f, 2.6f, 60);
                        Vfx.spray(level, FxStyle.SHARD, v.position().add(0, 1, 0), 0xBFF4FF, 0xFFFFFF, 0.4f, 20, 0.3f, 0.5f);
                        Fx.sound(level, v.position(), SoundEvents.GLASS_PLACE, 2f, 0.5f);
                        say(v instanceof Player ? "Stay. Forever." : "Frozen in my court.");
                        cooldowns[VOLLEY] = 0;
                    } else {
                        Vfx.spray(level, FxStyle.SHARD, tombAt.add(0, 0.5, 0), 0xBFF4FF, 0xFFFFFF, 0.35f, 12, 0.3f, 0.6f);
                    }
                }
                if (t >= 30) {
                    tombed = null;
                    cooldown(TOMB, 300);
                    return true;
                }
            }
            case HARVEST -> {
                Vec3 g = ground();
                if (t == 1) {
                    teleCircle(level, g, 13f, 16);
                    say(random.nextBoolean() ? "Your souls are TRIBUTE!" : "Give me your warmth. All of it.");
                    Fx.sound(level, position(), SoundEvents.WITHER_AMBIENT, 2f, 0.7f);
                }
                if (t >= 16 && t <= 66) {
                    setDeltaMovement(0, 0.02, 0);
                    if (t % 10 == 6) {
                        Vfx.sigil(level, g.add(0, 0.05, 0), 0x40FF90, 13f, 12, 1);
                        for (LivingEntity e : SiegeDamage.defendersAround(level, g, 13)) {
                            if (e instanceof TurretEntity) continue;
                            Vfx.tether(level, this, getBbHeight() * 0.7f, e, 0x40FF90, 0.06f, 10);
                            if (SiegeDamage.deal(e, this, this, dmg(0.25f), Element.SHADOW, false)) heal(dmg(0.25f) * 2.5f);
                            Vfx.spray(level, FxStyle.SOUL, e.position().add(0, 1, 0), 0x40FF90, 0xFFFFFF, 0.3f, 3, 0.1f, 0.6f);
                        }
                        Fx.sound(level, position(), SoundEvents.SOUL_ESCAPE, 2f, 0.6f);
                    }
                }
                if (t >= 72) {
                    cooldown(HARVEST, 420);
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
        Vfx.dome(level, position(), FROST, 1.8f, 60, this);
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3 + random.nextDouble();
            Vec3 p = groundAt(position().add(Math.cos(a) * 7, 0, Math.sin(a) * 7));
            PhylacteryEntity c = ModEntities.PHYLACTERY.get().create(level);
            if (c == null) continue;
            c.moveTo(p.x, p.y + 1.5, p.z);
            c.bind(this, 80 * damageScale * (1 + phase() * 0.5f));
            level.addFreshEntity(c);
            crystals.add(c.getUUID());
            Vfx.pillar(level, p, FROST, 0.7f, 10, 24);
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
                Vfx.flash(level, position().add(0, 1.5, 0), FROST, 3f, 10);
                Vfx.spray(level, FxStyle.SHARD, position().add(0, 1.5, 0), 0xBFF4FF, 0xFFFFFF, 0.4f, 30, 0.5f, 0);
                say("My... my phylacteries!");
            } else {
                if (tickCount % 60 == 0) Vfx.dome(level, position(), FROST, 1.8f, 62, this);
                for (UUID id : crystals) {
                    Entity e = level.getEntity(id);
                    if (e != null) Vfx.beam(level, e.position().add(0, 0.6, 0), position().add(0, 1.5, 0), FROST, 0.12f, 11, 0.1f);
                }
            }
        }
        if (tickCount % 4 == 0) Vfx.spray(level, FxStyle.FLAKE, position().add(0, 0.2, 0), 0xFFFFFF, FROST, 0.18f, 2, 0.05f, -0.5f);
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
