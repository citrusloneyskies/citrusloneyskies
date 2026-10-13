package com.lastbastion.entity.turret;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.AnimTracker;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.network.Net;
import com.lastbastion.network.OpenScreenPacket;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.CoreUpgrade;
import com.lastbastion.siege.NightModifier;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A deployed defensive turret. Stationary, levelled 1-5, with per-family firing behaviour. The head
 * tracks its target smoothly (yaw/pitch synced for the renderer); special abilities unlock at levels 3 and 5.
 */
public class TurretEntity extends Mob implements SiegeAlly {
    public enum TargetMode {
        FIRST("Closest to Core"), NEAREST("Nearest"), STRONGEST("Strongest"), WEAKEST("Weakest");
        public final String title;

        TargetMode(String t) {
            title = t;
        }
    }

    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> KILLS = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);

    public final TurretKind kind;
    public final AnimTracker anim;
    @Nullable
    private LivingEntity target;
    private int cooldown;
    private int scan;
    private int shotCount;
    private int specialTimer;
    private int rampTicks;
    private int lastHurt = -1000;
    private int sizeLevel = 1;
    public float clientYaw, clientYawO, clientPitch, clientPitchO;

    public TurretEntity(EntityType<? extends TurretEntity> type, Level level, TurretKind kind) {
        super(type, level);
        this.kind = kind;
        this.anim = new AnimTracker(this, ANIM);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes(TurretKind k) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, k.hp)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LEVEL, 1);
        entityData.define(ANIM, 0);
        entityData.define(AIM_YAW, 0f);
        entityData.define(AIM_PITCH, 0f);
        entityData.define(TARGET_ID, -1);
        entityData.define(MODE, 0);
        entityData.define(OWNER, Optional.empty());
        entityData.define(KILLS, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (anim != null) anim.onSync(key);
        if (LEVEL.equals(key)) {
            sizeLevel = turretLevel();
            refreshDimensions();
        }
    }

    @Override
    protected void registerGoals() {
    }

    // ------------------------------------------------------------------ properties

    public int turretLevel() {
        return entityData.get(LEVEL);
    }

    public void setTurretLevel(int l) {
        entityData.set(LEVEL, Mth.clamp(l, 1, TurretKind.MAX_LEVEL));
        float frac = getHealth() / getMaxHealth();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.hpAt(turretLevel()));
        setHealth(Math.max(1, getMaxHealth() * frac));
        getAttribute(Attributes.ARMOR).setBaseValue(6 + 2 * (turretLevel() - 1));
        sizeLevel = turretLevel();
        refreshDimensions();
    }

    public TargetMode mode() {
        return TargetMode.values()[Mth.clamp(entityData.get(MODE), 0, TargetMode.values().length - 1)];
    }

    public void cycleMode() {
        entityData.set(MODE, (entityData.get(MODE) + 1) % TargetMode.values().length);
    }

    @Override
    @Nullable
    public UUID getOwnerId() {
        return entityData.get(OWNER).orElse(null);
    }

    public void setOwnerId(@Nullable UUID id) {
        entityData.set(OWNER, Optional.ofNullable(id));
    }

    public int kills() {
        return entityData.get(KILLS);
    }

    public void addKill() {
        entityData.set(KILLS, kills() + 1);
    }

    @Nullable
    public Entity beamTarget() {
        int id = entityData.get(TARGET_ID);
        return id < 0 ? null : level().getEntity(id);
    }

    public float aimYaw() {
        return entityData.get(AIM_YAW);
    }

    public float aimPitch() {
        return entityData.get(AIM_PITCH);
    }

    public Vec3 muzzle() {
        return position().add(0, 1.15 * TurretKind.SCALE[turretLevel() - 1], 0);
    }

    public float range() {
        float r = kind.rangeAt(turretLevel());
        if (level() instanceof ServerLevel sl && SiegeManager.modifier(sl) == NightModifier.FOG) r *= 0.75f;
        return r;
    }

    public float damage() {
        float d = kind.damageAt(turretLevel());
        if (level() instanceof ServerLevel sl) {
            if (kind == TurretKind.TESLA && SiegeManager.modifier(sl) == NightModifier.STORM) d *= 1.3f;
            if (hasEffect(ModEffects.RALLY.get())) d *= 1.1f;
        }
        return d;
    }

    public int fireInterval() {
        int cd = kind.cooldownAt(turretLevel());
        if (level() instanceof ServerLevel sl) {
            int oc = SiegeManager.data(sl).level(CoreUpgrade.OVERCLOCK);
            cd = Math.max(1, Math.round(cd * (1f - 0.06f * oc)));
        }
        return cd;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            Net.sendTo(sp, OpenScreenPacket.turret(getId()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public boolean canManage(Player p) {
        UUID o = getOwnerId();
        return o == null || o.equals(p.getUUID()) || p.hasPermissions(2) || com.lastbastion.config.SiegeConfig.SHARED_CREDITS.get();
    }

    public void upgrade(ServerPlayer by) {
        int lvl = turretLevel();
        if (lvl >= TurretKind.MAX_LEVEL) return;
        ServerLevel sl = (ServerLevel) level();
        if (!SiegeManager.spend(sl, by, kind.upgradeCost(lvl))) {
            by.displayClientMessage(Component.literal("Not enough credits!").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        setTurretLevel(lvl + 1);
        setHealth(getMaxHealth());
        anim.play(9);
        Fx.spiral(sl, position(), 1.2, 2.5, Fx.dust(kind.color, 1.4f), 3, 0);
        Fx.skyBeam(sl, position(), kind.color, 6, 0.6);
        sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 0.6f + 0.15f * lvl);
        if (turretLevel() == 3 || turretLevel() == 5) {
            by.displayClientMessage(Component.literal(kind.title + " unlocked: " + (turretLevel() == 3 ? kind.perk3 : kind.perk5))
                    .withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
    }

    public void repair(ServerPlayer by) {
        float missing = getMaxHealth() - getHealth();
        if (missing < 1) return;
        int cost = repairCost();
        if (!SiegeManager.spend((ServerLevel) level(), by, cost)) return;
        setHealth(getMaxHealth());
        ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 15, 0.4, 0.6, 0.4, 0);
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.2f);
    }

    public int repairCost() {
        float missing = getMaxHealth() - getHealth();
        return Math.max(0, (int) Math.ceil(missing * kind.cost / (float) kind.hp * 0.35f));
    }

    public int sellValue() {
        return (int) (kind.totalInvested(turretLevel()) * 0.6f * (getHealth() / getMaxHealth()));
    }

    public void sell(ServerPlayer by) {
        ServerLevel sl = (ServerLevel) level();
        SiegeData d = SiegeManager.data(sl);
        d.addCredits(by.getUUID(), sellValue());
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 20, 0.4, 0.5, 0.4, 0.02);
        sl.playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 0.6f);
        discard();
        SiegeManager.syncHud(sl, d);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (level().isClientSide) {
            clientYawO = clientYaw;
            clientPitchO = clientPitch;
            clientYaw = Mth.rotLerp(0.35f, clientYaw, aimYaw());
            clientPitch = Mth.lerp(0.35f, clientPitch, aimPitch());
            if (turretLevel() >= 5 && random.nextInt(4) == 0) {
                level().addParticle(Fx.dust(kind.color, 0.8f), getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5), 0, 0.02, 0);
            }
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (isNoAi()) return;
        yBodyRot = 0;
        setYRot(0);
        if (cooldown > 0) cooldown--;
        specialTimer++;
        if (tickCount - lastHurt > 200 && tickCount % 40 == 0 && getHealth() < getMaxHealth()) heal(1f);
        if (hasEffect(ModEffects.JAMMED.get())) {
            if (tickCount % 5 == 0) sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.4, getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            entityData.set(TARGET_ID, -1);
            return;
        }
        if (kind == TurretKind.SANCTUM) sanctumAura(sl);
        if (kind == TurretKind.GALE && turretLevel() >= 5 && tickCount % 10 == 0) cycloneWall(sl);

        if (--scan <= 0 || target == null || !valid(target)) {
            scan = 5;
            LivingEntity old = target;
            target = acquire(sl);
            if (target != old) rampTicks = 0;
            entityData.set(TARGET_ID, target != null && kind == TurretKind.PRISM ? target.getId() : -1);
        }
        if (target == null) return;
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(muzzle());
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) (-(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG));
        entityData.set(AIM_YAW, yaw);
        entityData.set(AIM_PITCH, pitch);
        if (cooldown <= 0) {
            cooldown = fireInterval();
            fire(sl, target);
        }
    }

    private boolean valid(LivingEntity e) {
        if (!e.isAlive() || e.isRemoved()) return false;
        if (e instanceof SiegeEnemy se && se.isUntargetable()) return false;
        if (!SiegeDamage.isEnemy(e)) return false;
        double r = range() + e.getBbWidth() * 0.5;
        double d2 = e.position().distanceToSqr(position());
        if (d2 > r * r) return false;
        if (kind == TurretKind.MORTAR && d2 < 36) return false;
        return kind == TurretKind.MORTAR || hasLos(e);
    }

    private boolean hasLos(LivingEntity e) {
        Vec3 from = muzzle();
        Vec3 to = e.position().add(0, e.getBbHeight() * 0.6, 0);
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    @Nullable
    private LivingEntity acquire(ServerLevel sl) {
        BlockPos core = SiegeManager.corePos(sl);
        Vec3 corePos = core != null ? Vec3.atCenterOf(core) : position();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), range() + 2)) {
            if (!valid(e)) continue;
            double score = switch (mode()) {
                case FIRST -> e.position().distanceToSqr(corePos);
                case NEAREST -> e.position().distanceToSqr(position());
                case STRONGEST -> -e.getHealth();
                case WEAKEST -> e.getHealth();
            };
            if (e instanceof SiegeEnemy se && se.isHighPriority() && mode() == TargetMode.STRONGEST) score -= 10000;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private List<LivingEntity> others(ServerLevel sl, LivingEntity primary, Vec3 around, double radius, int max) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, around, radius)) {
            if (e != primary && valid(e)) list.add(e);
        }
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(primary)));
        return list.subList(0, Math.min(max, list.size()));
    }

    // ------------------------------------------------------------------ firing behaviours

    private void fire(ServerLevel sl, LivingEntity t) {
        int lvl = turretLevel();
        float dmg = damage();
        Vec3 m = muzzle();
        Vec3 aim = t.position().add(0, t.getBbHeight() * 0.5, 0);
        shotCount++;
        if (kind != TurretKind.PRISM && kind != TurretKind.FLAME) anim.play(1);
        switch (kind) {
            case BALLISTA -> {
                shootBolt(sl, m, t, dmg, lvl);
                if (lvl >= 3) {
                    List<LivingEntity> o = others(sl, t, position(), range(), 1);
                    if (!o.isEmpty()) shootBolt(sl, m, o.get(0), dmg, lvl);
                }
                sound(sl, SoundEvents.CROSSBOW_SHOOT, 1f, 0.7f);
            }
            case FLAME -> flame(sl, m, t, dmg, lvl);
            case FROST -> {
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.FROST_SHARD, this, m).payload(dmg, Element.ICE)
                        .status(ModEffects.CHILLED, 80, 4);
                if (lvl >= 3) {
                    p.onEntityHit = (pr, hit) -> {
                        for (LivingEntity o : others(sl, hit, hit.position(), 5, 2)) {
                            SiegeProjectile s = SiegeProjectile.create(sl, ProjKind.FROST_SHARD, this, hit.position().add(0, hit.getBbHeight() * 0.6, 0))
                                    .payload(dmg * 0.5f, Element.ICE).status(ModEffects.CHILLED, 80, 4);
                            s.aim(o.position().add(0, o.getBbHeight() * 0.5, 0), 1.2f).scale(0.6f).fire(sl);
                        }
                    };
                }
                p.aim(aim, 1.6f).fire(sl);
                if (lvl >= 5 && specialTimer >= 120) {
                    specialTimer = 0;
                    frostNova(sl, t.position(), dmg);
                }
                sound(sl, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8f, 1.6f);
            }
            case TIDE -> {
                boolean wave = lvl >= 3 && shotCount % 4 == 0;
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.WATER_ORB, this, m).payload(dmg * (wave ? 1.5f : 1f), Element.WATER)
                        .status(ModEffects.SOAKED, 120, 0);
                p.knockback = wave ? 1.3f : 0.5f;
                if (wave) p.area(3.5f).scale(1.8f);
                p.aim(aim, 1.3f).fire(sl);
                if (lvl >= 5 && specialTimer >= 160) {
                    specialTimer = 0;
                    HazardZone.spawn(sl, HazardZone.Kind.WHIRLPOOL, clusterCentre(sl, t), 3.5f, 70, dmg / 3f, this, false);
                    sound(sl, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 0.8f);
                }
                sound(sl, SoundEvents.PLAYER_SPLASH, 0.6f, 1.4f);
            }
            case TESLA -> {
                chain(sl, t, dmg, lvl >= 3 ? 5 : 3);
                if (lvl >= 5 && specialTimer >= 200) {
                    specialTimer = 0;
                    for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), range())) {
                        if (!valid(e)) continue;
                        Fx.bolt(sl, m, e.position().add(0, e.getBbHeight() * 0.5, 0), 0xF4F06A, 1f);
                        SiegeDamage.deal(e, this, this, dmg * 1.5f, Element.LIGHTNING, false);
                        SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 20, 0);
                    }
                    sound(sl, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 1.4f);
                }
            }
            case VENOM -> {
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.ACID_GLOB, this, m).payload(dmg, Element.POISON)
                        .area(2.5f).status(ModEffects.CORRODED, 120, 4);
                p.onImpact = (pr, at) -> {
                    HazardZone.spawn(sl, HazardZone.Kind.ACID, at, lvl >= 3 ? 3.5f : 2.2f, lvl >= 3 ? 140 : 80, dmg / 3f, this, false);
                    if (lvl >= 5) HazardZone.spawn(sl, HazardZone.Kind.POISON_CLOUD, at, 3f, 80, dmg / 2.5f, this, false);
                };
                p.aim(aim, 0.9f).scale(1.2f).fire(sl);
                sound(sl, SoundEvents.SLIME_JUMP, 1f, 0.6f);
            }
            case MORTAR -> {
                shell(sl, m, t.position(), dmg, lvl);
                if (lvl >= 5 && specialTimer >= 240) {
                    specialTimer = 0;
                    List<LivingEntity> all = others(sl, t, position(), range(), 5);
                    for (int i = 0; i < all.size(); i++) shell(sl, m.add(0, 0.2 * i, 0), all.get(i).position(), dmg * 0.8f, 3);
                }
                sound(sl, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.8f);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, m.x, m.y + 0.4, m.z, 6, 0.1, 0.2, 0.1, 0.03);
            }
            case PRISM -> beam(sl, m, t, dmg, lvl);
            case GALE -> {
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.WIND_BLADE, this, m).payload(dmg, Element.WIND)
                        .status(ModEffects.DISORIENTED, 50, 0);
                p.knockback = 1.3f;
                p.pierce = 2;
                p.aim(aim, 1.5f).scale(1.3f).fire(sl);
                if (lvl >= 3 && specialTimer >= 100) {
                    specialTimer = 0;
                    HazardZone.spawn(sl, HazardZone.Kind.TORNADO, clusterCentre(sl, t), 2.5f, 50, dmg / 3f, this, false)
                            .heading(t.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.05));
                }
                sound(sl, SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.8f);
            }
            case SANCTUM -> {
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.HOLY_BOLT, this, m).payload(dmg, Element.HOLY)
                        .status(ModEffects.PURIFIED, 80, 0);
                p.homing(t, 0.2f).aim(aim, 1.2f).fire(sl);
                if (lvl >= 5 && specialTimer >= 300) {
                    specialTimer = 0;
                    LivingEntity strongest = t;
                    for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), range())) {
                        if (valid(e) && e.getHealth() > strongest.getHealth()) strongest = e;
                    }
                    Fx.skyBeam(sl, strongest.position(), 0xFFF2B0, 30, 1.2);
                    SiegeDamage.deal(strongest, this, this, dmg * 8, Element.HOLY, false);
                    sound(sl, SoundEvents.BEACON_POWER_SELECT, 2f, 1.5f);
                }
                sound(sl, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            }
        }
    }

    private void shootBolt(ServerLevel sl, Vec3 m, LivingEntity t, float dmg, int lvl) {
        SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.BOLT, this, m).payload(dmg, Element.PHYSICAL)
                .status(ModEffects.BLEEDING, 60, 2);
        if (lvl >= 5) p.pierce = 4;
        p.aim(t.position().add(0, t.getBbHeight() * 0.5, 0), 2.4f).scale(lvl >= 4 ? 1.4f : 1f).fire(sl);
    }

    private void flame(ServerLevel sl, Vec3 m, LivingEntity t, float dmg, int lvl) {
        Vec3 dir = t.position().add(0, t.getBbHeight() * 0.4, 0).subtract(m).normalize();
        boolean soul = lvl >= 5;
        Fx.cone(sl, m, dir, range(), 15, soul ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, 12 + lvl * 2);
        if (shotCount % 4 == 0) sound(sl, SoundEvents.BLAZE_BURN, 0.6f, 0.8f);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, m, range())) {
            if (!valid(e)) continue;
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.4, 0).subtract(m).normalize();
            if (to.dot(dir) < 0.85) continue;
            float mult = soul ? 1.5f : 1f;
            Element el = Element.FIRE;
            if (soul && SiegeDamage.multiplier(e, Element.FIRE) <= 0) el = Element.ARCANE;
            SiegeDamage.deal(e, this, this, dmg * mult, el, false);
            e.setSecondsOnFire(3);
            if (shotCount % 4 == 0) SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 60, soul ? 2 : 1);
        }
        if (lvl >= 3 && specialTimer >= 40) {
            specialTimer = 0;
            HazardZone.spawn(sl, soul ? HazardZone.Kind.SOUL_FIRE : HazardZone.Kind.FIRE, t.position(), 2f, 80, dmg, this, false);
        }
    }

    private void frostNova(ServerLevel sl, Vec3 at, float dmg) {
        for (double r = 1; r <= 4; r += 1) Fx.ring(sl, at.add(0, 0.3, 0), r, ParticleTypes.SNOWFLAKE, (int) (r * 10));
        sound(sl, SoundEvents.GLASS_BREAK, 1.2f, 0.6f);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, at, 4)) {
            SiegeDamage.deal(e, this, this, dmg * 2, Element.ICE, false);
            SiegeDamage.apply(e, ModEffects.FROZEN.get(), 60, 0);
        }
    }

    private void chain(ServerLevel sl, LivingEntity first, float dmg, int hops) {
        Set<Integer> hit = new HashSet<>();
        LivingEntity cur = first;
        Vec3 from = muzzle();
        float d = dmg;
        for (int i = 0; i < hops && cur != null; i++) {
            hit.add(cur.getId());
            Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
            Fx.bolt(sl, from, to, 0xF4F06A, 0.9f);
            SiegeDamage.deal(cur, this, this, d, Element.LIGHTNING, false);
            SiegeDamage.apply(cur, ModEffects.SHOCKED.get(), 10, 0);
            boolean soaked = cur.hasEffect(ModEffects.SOAKED.get());
            if (soaked && i == hops - 1) hops++;
            from = to;
            d *= 0.75f;
            LivingEntity next = null;
            double best = soaked ? 7 * 7 : 5 * 5;
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, to, soaked ? 7 : 5)) {
                if (hit.contains(e.getId()) || (e instanceof SiegeEnemy se && se.isUntargetable())) continue;
                double dd = e.position().distanceToSqr(cur.position());
                if (dd < best) {
                    best = dd;
                    next = e;
                }
            }
            cur = next;
        }
        sound(sl, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.8f);
    }

    private void shell(ServerLevel sl, Vec3 m, Vec3 at, float dmg, int lvl) {
        Vec3 jitter = at.add(random.nextGaussian() * 0.6, 0, random.nextGaussian() * 0.6);
        SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.MORTAR_SHELL, this, m).payload(dmg, Element.EXPLOSIVE)
                .area(3.5f).status(ModEffects.CONCUSSED, 80, 0);
        if (lvl >= 3) {
            p.onImpact = (pr, hit) -> {
                for (int i = 0; i < 3; i++) {
                    SiegeProjectile b = SiegeProjectile.create(sl, ProjKind.GRENADE, this, hit.add(0, 0.5, 0)).payload(dmg * 0.4f, Element.EXPLOSIVE).area(2f);
                    b.setDeltaMovement(random.nextGaussian() * 0.25, 0.45, random.nextGaussian() * 0.25);
                    b.life = 30;
                    b.scale(0.6f).fire(sl);
                }
            };
        }
        p.aim(jitter, 1.0f).scale(1.2f).fire(sl);
    }

    private void beam(ServerLevel sl, Vec3 m, LivingEntity t, float dmg, int lvl) {
        rampTicks++;
        float ramp = 1f + Math.min(1.5f, rampTicks / 40f);
        Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0);
        if (lvl >= 5) {
            Vec3 dir = to.subtract(m).normalize();
            Vec3 end = m.add(dir.scale(range()));
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, m.add(dir.scale(range() / 2)), range() / 2 + 1)) {
                Vec3 rel = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(m);
                double along = rel.dot(dir);
                if (along < 0 || rel.subtract(dir.scale(along)).lengthSqr() > 1.0) continue;
                SiegeDamage.deal(e, this, this, dmg * (e == t ? ramp : 1f), Element.ARCANE, false);
                SiegeDamage.applyStacking(e, ModEffects.MARKED.get(), 60, 2);
            }
            if (rampTicks % 2 == 0) Fx.line(sl, m, end, Fx.dust(0xC86BFF, 0.9f), 0.5);
        } else {
            SiegeDamage.deal(t, this, this, dmg * ramp, Element.ARCANE, false);
            if (rampTicks % 10 == 1) SiegeDamage.applyStacking(t, ModEffects.MARKED.get(), 60, 2);
            if (rampTicks % 2 == 0) Fx.line(sl, m, to, Fx.dust(0xC86BFF, 0.6f + ramp * 0.3f), 0.45);
        }
        if (lvl >= 3) {
            List<LivingEntity> o = others(sl, t, t.position(), 6, 1);
            if (!o.isEmpty()) {
                LivingEntity s = o.get(0);
                SiegeDamage.deal(s, this, this, dmg * 0.6f, Element.ARCANE, false);
                if (rampTicks % 2 == 0) Fx.line(sl, to, s.position().add(0, s.getBbHeight() * 0.5, 0), Fx.dust(0xE0A0FF, 0.5f), 0.5);
            }
        }
        if (rampTicks % 20 == 1) sound(sl, SoundEvents.BEACON_AMBIENT, 1f, 1.5f + ramp * 0.2f);
    }

    private void sanctumAura(ServerLevel sl) {
        if (tickCount % 40 != 0) return;
        int lvl = turretLevel();
        double r = 8 + lvl;
        Fx.ring(sl, position().add(0, 0.2, 0), r, ParticleTypes.END_ROD, 20 + lvl * 4);
        BlockPos core = SiegeManager.corePos(sl);
        if (core != null && core.closerToCenterThan(position(), r + 1)) SiegeManager.healCore(sl, 2f * lvl);
        for (LivingEntity e : SiegeDamage.defendersAround(sl, position(), r)) {
            e.heal(1f + lvl * 0.5f);
            if (lvl >= 3 && !(e instanceof TurretEntity && e == this)) SiegeDamage.apply(e, ModEffects.AEGIS.get(), 60, 0);
        }
    }

    private void cycloneWall(ServerLevel sl) {
        Fx.ring(sl, position().add(0, 0.6, 0), 4, ParticleTypes.CLOUD, 24);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), 4.5)) {
            if (e instanceof SiegeEnemy se && se.isHighPriority()) continue;
            SiegeDamage.push(e, position(), 0.8, 0.15);
        }
    }

    private Vec3 clusterCentre(ServerLevel sl, LivingEntity t) {
        Vec3 sum = t.position();
        int n = 1;
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, t.position(), 4)) {
            if (e == t) continue;
            sum = sum.add(e.position());
            n++;
        }
        return sum.scale(1.0 / n);
    }

    private void sound(ServerLevel sl, SoundEvent s, float v, float p) {
        sl.playSound(null, getX(), getY(), getZ(), s, net.minecraft.sounds.SoundSource.NEUTRAL, v, p);
    }

    // ------------------------------------------------------------------ damage / physics

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && !p.isCreative()) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) return false;
        boolean r = super.hurt(source, amount);
        if (r) lastHurt = tickCount;
        return r;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            Fx.explosion(sl, position().add(0, 0.8, 0), 2);
            UUID o = getOwnerId();
            if (o != null && sl.getPlayerByUUID(o) instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("Your " + kind.title + " (Lv " + turretLevel() + ") was destroyed!")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }
    }

    @Override
    protected void tickDeath() {
        if (++deathTime >= 2 && !level().isClientSide) remove(RemovalReason.KILLED);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity e) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player p) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float s = TurretKind.SCALE[Mth.clamp(sizeLevel == 0 ? 1 : sizeLevel, 1, 5) - 1];
        return super.getDimensions(pose).scale(s);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("lbLevel", turretLevel());
        tag.putInt("lbMode", entityData.get(MODE));
        tag.putInt("lbKills", kills());
        UUID o = getOwnerId();
        if (o != null) tag.putUUID("lbOwner", o);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(LEVEL, Math.max(1, tag.getInt("lbLevel")));
        entityData.set(MODE, tag.getInt("lbMode"));
        entityData.set(KILLS, tag.getInt("lbKills"));
        if (tag.hasUUID("lbOwner")) setOwnerId(tag.getUUID("lbOwner"));
        sizeLevel = turretLevel();
        refreshDimensions();
    }
}
