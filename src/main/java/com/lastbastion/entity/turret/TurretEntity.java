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
import com.lastbastion.fx.FxParticleOptions;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * tracks its target smoothly and sweeps the approaches when idle. Level 3 unlocks a perk; at level 5
 * the owner picks one of two specialisations ({@link #branch()}), each with its own model and kit.
 * Big abilities charge up visibly (animation 3) before they release (animation 4).
 */
public class TurretEntity extends Mob implements SiegeAlly {
    public enum TargetMode {
        FIRST("Closest to Core"), NEAREST("Nearest"), STRONGEST("Strongest"), WEAKEST("Weakest");
        public final String title;

        TargetMode(String t) {
            title = t;
        }
    }

    /** Animation ids. */
    public static final int A_FIRE = 1, A_FIRE_ALT = 2, A_CHARGE = 3, A_RELEASE = 4, A_DEPLOY = 8, A_UPGRADE = 9;

    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> KILLS = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BRANCH = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);

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
    private boolean deployed;
    /** Pending charged ability: ticks until release (0 = none). */
    private int charging;
    private int burstLeft, burstTimer;
    private float heat;
    private int beamRefresh;
    private int lastBeamTarget = -1;
    private float idlePhase = -1;

    // client-side animation state
    public float clientYaw, clientYawO, clientPitch, clientPitchO;
    public float spin, spinO;
    private float spinSpeed;
    public int clientShots;
    private int lastAnimSeq = -1;

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
        entityData.define(BRANCH, 0);
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

    /** 0 = none, 1 = first specialisation, 2 = second. */
    public int branch() {
        return entityData.get(BRANCH);
    }

    public boolean isA() {
        return turretLevel() >= 5 && branch() != 2;
    }

    public boolean isB() {
        return turretLevel() >= 5 && branch() == 2;
    }

    /** Level used by the model: 1-4, then 5 for specialisation A and 6 for B. */
    public int modelLevel() {
        int l = turretLevel();
        return l < 5 ? l : (branch() == 2 ? 6 : 5);
    }

    public String specialisation() {
        return turretLevel() < 5 ? "" : isB() ? kind.branchB : kind.branchA;
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

    public void setBranch(int b) {
        entityData.set(BRANCH, Mth.clamp(b, 0, 2));
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

    /** True while the turret has a target (drives spin-up and idle animation on the client). */
    public boolean isActive() {
        return entityData.get(TARGET_ID) >= 0;
    }

    /** Point the weapon (used by the showcase and when deploying facing the player). */
    public void setAim(float yaw, float pitch) {
        entityData.set(AIM_YAW, yaw);
        entityData.set(AIM_PITCH, pitch);
        clientYaw = clientYawO = yaw;
        clientPitch = clientPitchO = pitch;
    }

    public float aimYaw() {
        return entityData.get(AIM_YAW);
    }

    public float aimPitch() {
        return entityData.get(AIM_PITCH);
    }

    private float scaleNow() {
        return TurretKind.SCALE[turretLevel() - 1];
    }

    /** Height of the weapon above the turret's feet, in blocks (before level scale). */
    private float muzzleHeight() {
        return switch (kind) {
            case BALLISTA -> 0.78f;
            case FLAME -> 0.8f;
            case FROST -> isB() ? 1.3f : 1.35f;
            case TIDE -> 0.78f;
            case TESLA -> isA() ? 2.6f : 1.25f;
            case VENOM -> 1.15f;
            case MORTAR -> 1.1f;
            case PRISM -> 1.15f;
            case GALE -> 0.72f;
            case SANCTUM -> 1.6f;
        };
    }

    private float muzzleForward() {
        return switch (kind) {
            case BALLISTA -> 0.6f;
            case FLAME -> isA() ? 1.2f : 0.85f;
            case FROST -> isB() ? 1.1f : 0f;
            case TIDE -> isB() ? 0.8f : 0.45f;
            case MORTAR -> isB() ? 1.0f : 0.45f;
            case PRISM -> isA() ? 0.9f : 0f;
            case GALE -> 0.5f;
            default -> 0f;
        };
    }

    public Vec3 muzzle() {
        float s = scaleNow();
        Vec3 base = position().add(0, muzzleHeight() * s, 0);
        float fwd = muzzleForward() * s;
        if (fwd <= 0) return base;
        float yawR = aimYaw() * Mth.DEG_TO_RAD, pitchR = aimPitch() * Mth.DEG_TO_RAD;
        Vec3 dir = new Vec3(-Mth.sin(yawR) * Mth.cos(pitchR), -Mth.sin(pitchR), Mth.cos(yawR) * Mth.cos(pitchR));
        return base.add(dir.scale(fwd));
    }

    public float range() {
        float r = kind.rangeAt(turretLevel());
        if (kind == TurretKind.FLAME && isA()) r += 3;
        if (kind == TurretKind.FLAME && isB()) r += 6;
        if (kind == TurretKind.MORTAR && isB()) r += 10;
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
        if (kind == TurretKind.BALLISTA && isA()) cd = Math.round(cd * 1.5f);
        if (kind == TurretKind.BALLISTA && isB()) cd = Math.round(cd * 1.25f);
        if (kind == TurretKind.FLAME && isB()) cd = 45;
        if (kind == TurretKind.FROST && isB()) cd = Math.round(cd * 2.2f);
        if (kind == TurretKind.MORTAR && isB()) cd = Math.round(cd * 1.6f);
        if (kind == TurretKind.TESLA && isB()) cd = Math.max(4, Math.round(cd * (1f - 0.05f * heat)));
        if (level() instanceof ServerLevel sl) {
            int oc = SiegeManager.data(sl).level(CoreUpgrade.OVERCLOCK);
            cd = Math.max(1, Math.round(cd * (1f - 0.06f * oc)));
            if (SiegeManager.overdrive(sl)) cd = Math.max(1, Math.round(cd * 0.667f));
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

    /** Upgrade one level. Going from 4 to 5 requires {@code choice} "A" or "B". */
    public void upgrade(ServerPlayer by, String choice) {
        int lvl = turretLevel();
        if (lvl >= TurretKind.MAX_LEVEL) return;
        int pick = 0;
        if (lvl == 4) {
            pick = "B".equalsIgnoreCase(choice) ? 2 : "A".equalsIgnoreCase(choice) ? 1 : 0;
            if (pick == 0) {
                by.displayClientMessage(Component.literal("Choose a specialisation first.").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                return;
            }
        }
        ServerLevel sl = (ServerLevel) level();
        if (!SiegeManager.spend(sl, by, kind.upgradeCost(lvl))) {
            by.displayClientMessage(Component.literal("Not enough credits!").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        if (pick != 0) setBranch(pick);
        setTurretLevel(lvl + 1);
        setHealth(getMaxHealth());
        anim.play(A_UPGRADE);
        float s = scaleNow();
        Vfx.vortex(sl, position(), kind.color, 0xFFFFFF, 1.4f * s, 3f * s, 24, this);
        Vfx.pillar(sl, position(), kind.color, 0.7f, 8, 22);
        Vfx.ring(sl, position().add(0, 0.05, 0), kind.color, 3.5f, 16, 0.8f);
        Vfx.spray(sl, FxStyle.STAR, position().add(0, 1, 0), kind.color, 0xFFFFFF, 0.3f, 20, 0.35f, 0.6f);
        sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 0.6f + 0.15f * lvl);
        sl.playSound(null, blockPosition(), SoundEvents.SMITHING_TABLE_USE, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 0.8f);
        if (turretLevel() == 3) {
            by.displayClientMessage(Component.literal(kind.title + " unlocked: " + kind.perk3).withStyle(net.minecraft.ChatFormatting.GOLD), false);
        } else if (turretLevel() == 5) {
            com.lastbastion.siege.Milestones.award(by, com.lastbastion.siege.Milestones.SPECIALIST);
            by.displayClientMessage(Component.literal(kind.title + " specialised: " + specialisation() + ". "
                    + (isB() ? kind.branchBDesc : kind.branchADesc)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), false);
        }
    }

    public void upgrade(ServerPlayer by) {
        upgrade(by, "A");
    }

    public void repair(ServerPlayer by) {
        float missing = getMaxHealth() - getHealth();
        if (missing < 1) return;
        int cost = repairCost();
        if (!SiegeManager.spend((ServerLevel) level(), by, cost)) return;
        setHealth(getMaxHealth());
        ServerLevel sl = (ServerLevel) level();
        Vfx.spray(sl, FxStyle.SPARK, position().add(0, 0.8, 0), 0xFFE080, 0xFF9A30, 0.12f, 18, 0.4f, 0.6f);
        Vfx.ring(sl, position().add(0, 0.05, 0), 0x7CFF9B, 1.6f, 12, 0.6f);
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
        if (!isAlive() || isRemoved()) return; // a second click in the same tick must not pay twice
        ServerLevel sl = (ServerLevel) level();
        SiegeData d = SiegeManager.data(sl);
        d.addCredits(by.getUUID(), sellValue());
        Vfx.spray(sl, FxStyle.SMOKE, position().add(0, 0.6, 0), 0xC8C8C8, 0x8A8A8A, 0.9f, 14, 0.15f, 0.5f);
        Vfx.spray(sl, FxStyle.STAR, position().add(0, 0.8, 0), 0xFFD966, 0xFFFFFF, 0.25f, 10, 0.25f, 0.8f);
        sl.playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 0.6f);
        discard();
        SiegeManager.syncHud(sl, d);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        try {
        super.tick();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (level().isClientSide) {
            clientTick();
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (isNoAi()) return;
        yBodyRot = 0;
        setYRot(0);
        if (!deployed) {
            deployed = true;
            anim.play(A_DEPLOY);
            cooldown = 28;
            Vfx.ring(sl, position().add(0, 0.05, 0), kind.color, 2.2f, 14, 0.5f);
            Vfx.spray(sl, FxStyle.DEBRIS, position().add(0, 0.2, 0), 0x6A5A48, 0x3A3028, 0.18f, 12, 0.3f, 0.8f);
            Vfx.spray(sl, FxStyle.SMOKE, position().add(0, 0.2, 0), 0xB8B0A8, 0x8A8480, 0.7f, 8, 0.12f, -1);
            sound(sl, SoundEvents.PISTON_EXTEND, 1f, 0.7f);
        }
        if (cooldown > 0) cooldown--;
        specialTimer++;
        if (tickCount - lastHurt > 200 && tickCount % 40 == 0 && getHealth() < getMaxHealth()) heal(1f);
        if (hasEffect(ModEffects.JAMMED.get())) {
            if (tickCount % 5 == 0) {
                Vfx.spray(sl, FxStyle.SMOKE, muzzle(), 0x4A4A4A, 0x2A2A2A, 0.5f, 3, 0.05f, 1);
                if (random.nextInt(3) == 0) Vfx.spray(sl, FxStyle.SPARK, muzzle(), 0xFFE080, 0xFF8020, 0.08f, 4, 0.3f, 0.4f);
            }
            entityData.set(TARGET_ID, -1);
            charging = 0;
            return;
        }
        ambient(sl);
        if (kind == TurretKind.SANCTUM) sanctumAura(sl);
        if (kind == TurretKind.GALE && isA() && tickCount % 10 == 0) cycloneWall(sl);
        if (kind == TurretKind.FROST && isA() && tickCount % 20 == 0) permafrost(sl);
        if (kind == TurretKind.TESLA && isB() && target == null) heat = Math.max(0, heat - 0.2f);

        if (--scan <= 0 || target == null || !valid(target)) {
            scan = 5;
            LivingEntity old = target;
            target = acquire(sl);
            if (target != old) {
                rampTicks = 0;
                beamRefresh = 0;
            }
            entityData.set(TARGET_ID, target != null ? target.getId() : -1);
        }
        if (target == null) {
            idleScan(sl);
            return;
        }
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position().add(0, muzzleHeight() * scaleNow(), 0));
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) (-(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG));
        entityData.set(AIM_YAW, yaw);
        entityData.set(AIM_PITCH, pitch);
        if (charging > 0) {
            if (--charging == 0) release(sl, target);
            return;
        }
        if (burstLeft > 0) {
            if (--burstTimer <= 0) {
                burstTimer = 4;
                burstLeft--;
                anim.play(A_FIRE);
                repeaterBolt(sl, target);
            }
            return;
        }
        if (trySpecial(sl, target)) return;
        if (cooldown <= 0) {
            cooldown = fireInterval();
            fire(sl, target);
        }
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("TurretEntity.tick", lbCrash);
        }
    }

    private void clientTick() {
        clientYawO = clientYaw;
        clientPitchO = clientPitch;
        clientYaw = Mth.rotLerp(isActive() ? 0.35f : 0.12f, clientYaw, aimYaw());
        clientPitch = Mth.lerp(0.3f, clientPitch, aimPitch());
        spinO = spin;
        boolean active = isActive();
        float idle, hot;
        switch (kind) {
            case BALLISTA -> {
                idle = 0;
                hot = 0.05f;
            }
            case FROST -> {
                idle = 0.04f;
                hot = 0.12f;
            }
            case TIDE -> {
                idle = 0.05f;
                hot = 0.16f;
            }
            case TESLA -> {
                idle = 0.06f;
                hot = 0.32f;
            }
            case PRISM -> {
                idle = 0.04f;
                hot = 0.24f;
            }
            case GALE -> {
                idle = 0.12f;
                hot = 0.65f;
            }
            case SANCTUM -> {
                idle = 0.03f;
                hot = 0.07f;
            }
            default -> {
                idle = 0;
                hot = 0;
            }
        }
        float targetSpeed = active ? hot : idle;
        if (anim.id() == A_CHARGE) targetSpeed = hot * 2.2f;
        spinSpeed = Mth.lerp(0.06f, spinSpeed, targetSpeed);
        spin += spinSpeed;
        int seq = entityData.get(ANIM) >>> 8;
        if (seq != lastAnimSeq) {
            if (lastAnimSeq >= 0 && (anim.id() == A_FIRE || anim.id() == A_FIRE_ALT)) clientShots++;
            lastAnimSeq = seq;
        }
        clientAmbient();
    }

    /** Signature idle particles for higher levels (client only). */
    private void clientAmbient() {
        int ml = modelLevel();
        if (ml < 4 || random.nextInt(ml >= 5 ? 3 : 6) != 0) return;
        float s = scaleNow();
        double x = getX() + (random.nextDouble() - 0.5) * 0.9 * s, z = getZ() + (random.nextDouble() - 0.5) * 0.9 * s;
        double y = getY() + random.nextDouble() * 1.6 * s;
        FxParticleOptions o = switch (kind) {
            case FLAME -> FxParticleOptions.of(FxStyle.EMBER, isA() ? 0x60E0FF : 0xFF8A20, 0.12f);
            case FROST -> FxParticleOptions.of(FxStyle.FLAKE, 0xD8F8FF, 0.12f);
            case TIDE -> FxParticleOptions.of(FxStyle.BUBBLE, 0x8AC8FF, 0.12f);
            case TESLA -> FxParticleOptions.of(FxStyle.BOLT, 0xF8F49A, 0.18f);
            case VENOM -> FxParticleOptions.of(isB() ? FxStyle.GLOW : FxStyle.BUBBLE, 0xA8F050, 0.14f);
            case PRISM -> FxParticleOptions.of(FxStyle.STAR, 0xD890FF, 0.12f);
            case GALE -> FxParticleOptions.of(FxStyle.LEAF, 0xD8FFF0, 0.14f);
            case SANCTUM -> FxParticleOptions.of(FxStyle.STAR, 0xFFF0B0, 0.12f);
            case MORTAR -> FxParticleOptions.of(FxStyle.SMOKE, 0x6A6A6A, 0.3f);
            default -> FxParticleOptions.of(FxStyle.SPARK, 0xFFD080, 0.06f);
        };
        level().addParticle(o, x, y, z, 0, 0.01, 0);
    }

    /** Sweep the approaches (outward from the core) while there is nothing to shoot. */
    private void idleScan(ServerLevel sl) {
        charging = 0;
        burstLeft = 0;
        if (idlePhase < 0) {
            BlockPos core = SiegeManager.corePos(sl);
            Vec3 out = core != null ? position().subtract(Vec3.atCenterOf(core)) : new Vec3(0, 0, 1);
            idlePhase = (float) (Mth.atan2(out.z, out.x) * Mth.RAD_TO_DEG) - 90f;
            if (core == null || out.lengthSqr() < 1) idlePhase = random.nextFloat() * 360;
        }
        if (tickCount % 4 != 0) return;
        float sweep = Mth.sin((tickCount + getId() * 37) * 0.012f) * 55f;
        entityData.set(AIM_YAW, idlePhase + sweep);
        entityData.set(AIM_PITCH, 6f + Mth.sin(tickCount * 0.02f) * 4f);
    }

    private void ambient(ServerLevel sl) {
        if (tickCount % 80 != 0) return;
        if (kind == TurretKind.TIDE && isA() && isActive()) {
            Vfx.vortex(sl, position().add(0, 0.6 * scaleNow(), 0), 0x2F7BFF, 0x8AD0FF, 1.2f, 1.2f, 80, this);
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
        boolean lob = kind == TurretKind.MORTAR || (kind == TurretKind.FLAME && isB()) || kind == TurretKind.VENOM;
        return lob || hasLos(e);
    }

    private boolean hasLos(LivingEntity e) {
        Vec3 from = position().add(0, muzzleHeight() * scaleNow(), 0);
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

    private Vec3 aimAt(LivingEntity t) {
        return t.position().add(0, t.getBbHeight() * 0.5, 0);
    }

    // ------------------------------------------------------------------ charged specials

    /** Starts a charged special ability if one is ready. Returns true if the turret is now charging. */
    private boolean trySpecial(ServerLevel sl, LivingEntity t) {
        int lvl = turretLevel();
        int need;
        if (lvl >= 5) {
            need = switch (kind) {
                case FROST -> isA() ? 120 : -1;
                case TIDE -> 160;
                case TESLA -> isA() ? (SiegeManager.modifier(sl) == NightModifier.STORM ? 80 : 160) : -1;
                case MORTAR -> isA() ? 240 : -1;
                case GALE -> isB() ? 200 : -1;
                case SANCTUM -> isA() ? 240 : -1;
                default -> -1;
            };
        } else {
            need = -1;
        }
        if (need < 0 || specialTimer < need) return false;
        specialTimer = 0;
        charging = 20;
        anim.play(A_CHARGE);
        Vfx.aura(sl, this, kind.color, 1.6f * scaleNow(), 22);
        switch (kind) {
            case SANCTUM -> {
                LivingEntity strongest = strongestInRange(sl, t);
                Vfx.teleCircle(sl, strongest.position().add(0, 0.05, 0), 1.8f, 20, 0xFFE070, false);
                sound(sl, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.6f);
            }
            case TESLA -> sound(sl, SoundEvents.BEACON_POWER_SELECT, 1f, 1.8f);
            case MORTAR -> sound(sl, SoundEvents.IRON_DOOR_OPEN, 1f, 0.6f);
            case FROST -> sound(sl, SoundEvents.POWDER_SNOW_STEP, 1.4f, 0.6f);
            default -> sound(sl, SoundEvents.BEACON_AMBIENT, 1f, 1.4f);
        }
        return true;
    }

    private void release(ServerLevel sl, LivingEntity t) {
        anim.play(A_RELEASE);
        float dmg = damage();
        Vec3 m = muzzle();
        switch (kind) {
            case FROST -> frostNova(sl, clusterCentre(sl, t), dmg);
            case TIDE -> {
                if (isA()) {
                    Vec3 at = clusterCentre(sl, t);
                    HazardZone.spawn(sl, HazardZone.Kind.WHIRLPOOL, at, 3.5f, 80, dmg / 3f, this, false);
                    Vfx.vortex(sl, at, 0x2F7BFF, 0x8AD0FF, 3.5f, 1.4f, 80, null);
                    Vfx.ring(sl, at.add(0, 0.05, 0), 0x8AD0FF, 4f, 16, 0.8f);
                    sound(sl, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 0.8f);
                } else {
                    tidalWave(sl, t, dmg);
                }
            }
            case TESLA -> {
                int n = 0;
                for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), range() + 2)) {
                    if (!valid(e) || n >= 6) continue;
                    n++;
                    Vfx.lightning(sl, e.position(), 0xF4F06A, 1.3f);
                    SiegeDamage.deal(e, this, this, dmg * 4f, Element.LIGHTNING, false);
                    SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 30, 0);
                }
                Vfx.arc(sl, m, m.add(0, 14, 0), 0xF8F49A, 0.12f, 8, 1f);
                sound(sl, SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, 1.1f);
            }
            case MORTAR -> {
                List<LivingEntity> all = new ArrayList<>(others(sl, t, t.position(), 10, 5));
                all.add(0, t);
                for (int i = 0; i < 6; i++) {
                    LivingEntity e = all.get(i % all.size());
                    Vec3 at = e.position().add(random.nextGaussian() * 1.5, 0, random.nextGaussian() * 1.5);
                    Vfx.teleCircle(sl, at.add(0, 0.05, 0), 3.2f, 26 + i * 4, 0xFF9A30, false);
                    shell(sl, m.add(0, 0.2 * i, 0), at, dmg * 0.85f, 3, i * 4);
                }
                Vfx.flash(sl, m, 0xFFB040, 2f, 6);
                Vfx.shake(sl, position(), 0.6f, 14, 10);
                sound(sl, SoundEvents.GENERIC_EXPLODE, 1f, 1.5f);
            }
            case GALE -> {
                Vec3 at = clusterCentre(sl, t);
                Vec3 out = outward(sl, at);
                HazardZone z = HazardZone.spawn(sl, HazardZone.Kind.TORNADO, at, 3.5f, 120, dmg / 2f, this, false).heading(out.scale(0.09));
                Vfx.vortex(sl, at, 0xE8FFF8, 0x9FFFE8, 3.2f, 9f, 120, z);
                sound(sl, SoundEvents.ELYTRA_FLYING, 1.5f, 0.6f);
            }
            case SANCTUM -> {
                LivingEntity strongest = strongestInRange(sl, t);
                Vec3 at = strongest.position();
                Vfx.pillar(sl, at, 0xFFF2B0, 1.8f, 30, 26);
                Vfx.flash(sl, at.add(0, 1, 0), 0xFFF2B0, 3f, 8);
                Vfx.ring(sl, at.add(0, 0.05, 0), 0xFFE070, 5f, 16, 1.2f);
                Vfx.shake(sl, at, 0.8f, 20, 10);
                Vfx.screenFlash(sl, at, 0xFFF8D0, 0.18f, 18, 6);
                SiegeDamage.deal(strongest, this, this, dmg * 8, Element.HOLY, false);
                for (LivingEntity e : SiegeDamage.enemiesAround(sl, at, 2.5)) {
                    if (e != strongest) SiegeDamage.deal(e, this, this, dmg * 2, Element.HOLY, false);
                }
                sound(sl, SoundEvents.BEACON_POWER_SELECT, 2f, 1.5f);
            }
            default -> {
            }
        }
    }

    private LivingEntity strongestInRange(ServerLevel sl, LivingEntity fallback) {
        LivingEntity strongest = fallback;
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), range())) {
            if (valid(e) && e.getHealth() > strongest.getHealth()) strongest = e;
        }
        return strongest;
    }

    // ------------------------------------------------------------------ firing behaviours

    private void fire(ServerLevel sl, LivingEntity t) {
        int lvl = turretLevel();
        float dmg = damage();
        Vec3 m = muzzle();
        Vec3 aim = aimAt(t);
        shotCount++;
        boolean continuous = kind == TurretKind.PRISM || (kind == TurretKind.FLAME && !isB());
        if (!continuous) anim.play(A_FIRE);
        switch (kind) {
            case BALLISTA -> {
                if (isB()) {
                    burstLeft = 3;
                    burstTimer = 0;
                    return;
                }
                shootBolt(sl, m, t, dmg, lvl);
                if (lvl >= 3) {
                    List<LivingEntity> o = others(sl, t, position(), range(), 1);
                    if (!o.isEmpty()) shootBolt(sl, m.add(0, 0.2, 0), o.get(0), dmg * (isA() ? 0.6f : 1f), 3);
                }
                Vfx.spray(sl, FxStyle.SPARK, m, 0xFFE8C0, 0xC8A060, 0.07f, 4, 0.2f, 0.2f);
                sound(sl, isA() ? SoundEvents.CROSSBOW_QUICK_CHARGE_3 : SoundEvents.CROSSBOW_SHOOT, 1.2f, isA() ? 0.5f : 0.7f);
            }
            case FLAME -> flame(sl, m, t, dmg, lvl);
            case FROST -> {
                if (isB()) {
                    glacialLance(sl, m, t, dmg);
                    return;
                }
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.FROST_SHARD, this, m).payload(dmg, Element.ICE)
                        .status(ModEffects.CHILLED, 80, 4);
                if (lvl >= 3) {
                    p.onEntityHit = (pr, hit) -> {
                        for (LivingEntity o : others(sl, hit, hit.position(), 5, 2)) {
                            SiegeProjectile s = SiegeProjectile.create(sl, ProjKind.FROST_SHARD, this, hit.position().add(0, hit.getBbHeight() * 0.6, 0))
                                    .payload(dmg * 0.5f, Element.ICE).status(ModEffects.CHILLED, 80, 4);
                            s.aim(aimAt(o), 1.2f).scale(0.6f).fire(sl);
                        }
                    };
                }
                p.aim(aim, 1.6f).fire(sl);
                Vfx.spray(sl, FxStyle.FLAKE, m, 0xFFFFFF, 0x8FE3FF, 0.14f, 5, 0.12f, 0);
                sound(sl, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8f, 1.6f);
            }
            case TIDE -> {
                boolean wave = lvl >= 3 && shotCount % 4 == 0;
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.WATER_ORB, this, m).payload(dmg * (wave ? 1.5f : 1f), Element.WATER)
                        .status(ModEffects.SOAKED, 120, 0);
                p.knockback = wave ? 1.3f : 0.5f;
                if (wave) {
                    p.area(3.5f).scale(1.8f);
                    Vfx.wave(sl, position(), aim, 0x6AB0FF, 3.5f, 1.6f, 16);
                }
                p.aim(aim, 1.3f).fire(sl);
                Vfx.spray(sl, FxStyle.DROP, m, 0x8AC8FF, 0x2F6BFF, 0.12f, 6, 0.2f, 0.5f);
                if (isB()) anim.play(A_FIRE);
                sound(sl, SoundEvents.PLAYER_SPLASH, 0.6f, 1.4f);
            }
            case TESLA -> {
                if (isB()) heat = Math.min(10, heat + 1);
                chain(sl, t, dmg, isB() ? 8 : lvl >= 3 ? 5 : 3, !isB());
            }
            case VENOM -> {
                if (isB()) {
                    List<LivingEntity> ts = new ArrayList<>(others(sl, t, t.position(), 8, 2));
                    ts.add(0, t);
                    for (int i = 0; i < 3; i++) {
                        LivingEntity e = ts.get(i % ts.size());
                        SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.SPORE, this, m.add(0, 0.2 * i, 0)).payload(dmg * 0.7f, Element.POISON)
                                .area(1.8f).status(ModEffects.CORRODED, 100, 4);
                        p.homing(e, 0.18f);
                        p.onImpact = (pr, at) -> {
                            HazardZone.spawn(sl, HazardZone.Kind.POISON_CLOUD, at, 2.2f, 60, dmg / 3f, this, false);
                            for (LivingEntity v : SiegeDamage.enemiesAround(sl, at, 2.2)) {
                                v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                            }
                        };
                        p.setDeltaMovement(random.nextGaussian() * 0.15, 0.55, random.nextGaussian() * 0.15);
                        p.scale(0.9f).fire(sl);
                    }
                    Vfx.spray(sl, FxStyle.SMOKE, m, 0x9BD940, 0x4A6A20, 0.7f, 6, 0.1f, 0.8f);
                    sound(sl, SoundEvents.SLIME_ATTACK, 1f, 0.6f);
                    return;
                }
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.ACID_GLOB, this, m).payload(dmg, Element.POISON)
                        .area(2.5f).status(ModEffects.CORRODED, 120, 4);
                p.onImpact = (pr, at) -> {
                    HazardZone.spawn(sl, HazardZone.Kind.ACID, at, lvl >= 3 ? 3.5f : 2.2f, lvl >= 3 ? 140 : 80, dmg / 3f, this, false);
                    if (isA()) {
                        HazardZone.spawn(sl, HazardZone.Kind.POISON_CLOUD, at, 3f, 80, dmg / 2.5f, this, false);
                        for (LivingEntity v : SiegeDamage.enemiesAround(sl, at, 3)) v.getPersistentData().putInt("lb_plague", getId());
                    }
                };
                p.aim(aim, 0.9f).scale(1.2f).fire(sl);
                sound(sl, SoundEvents.SLIME_JUMP, 1f, 0.6f);
            }
            case MORTAR -> {
                if (isB()) {
                    grandShell(sl, m, t, dmg);
                } else {
                    shell(sl, m, t.position(), dmg, lvl, 0);
                    Vfx.flash(sl, m, 0xFFB040, 1.2f, 4);
                    Vfx.spray(sl, FxStyle.SMOKE, m, 0x8A8480, 0x4A4846, 0.9f, 8, 0.12f, 0.9f);
                    Vfx.shake(sl, position(), 0.25f, 8, 6);
                    sound(sl, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.8f);
                }
            }
            case PRISM -> beam(sl, m, t, dmg, lvl);
            case GALE -> {
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.WIND_BLADE, this, m).payload(dmg, Element.WIND)
                        .status(ModEffects.DISORIENTED, 50, 0);
                p.knockback = 1.3f;
                p.pierce = 2;
                p.aim(aim, 1.5f).scale(1.3f).fire(sl);
                Vfx.spray(sl, FxStyle.LEAF, m, 0xE8FFF8, 0x9FFFE8, 0.16f, 5, 0.25f, 0);
                if (lvl >= 3 && !isB() && specialTimer >= 100) {
                    specialTimer = 0;
                    Vec3 at = clusterCentre(sl, t);
                    HazardZone z = HazardZone.spawn(sl, HazardZone.Kind.TORNADO, at, 2.5f, 50, dmg / 3f, this, false)
                            .heading(t.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.05));
                    Vfx.vortex(sl, at, 0xE8FFF8, 0x9FFFE8, 2f, 5f, 50, z);
                }
                sound(sl, SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.8f);
            }
            case SANCTUM -> {
                SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.HOLY_BOLT, this, m).payload(dmg, Element.HOLY)
                        .status(ModEffects.PURIFIED, 80, 0);
                p.homing(t, 0.2f).aim(aim, 1.2f).fire(sl);
                Vfx.flash(sl, m, 0xFFF2B0, 0.6f, 4);
                sound(sl, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            }
        }
    }

    private void shootBolt(ServerLevel sl, Vec3 m, LivingEntity t, float dmg, int lvl) {
        if (isA()) {
            SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.HARPOON, this, m).payload(dmg * 1.4f, Element.PHYSICAL)
                    .status(ModEffects.BLEEDING, 80, 3);
            p.pierce = 6;
            p.onEntityHit = (pr, hit) -> {
                SiegeDamage.applyStacking(hit, ModEffects.CORRODED.get(), 120, 4);
                if (!(hit instanceof SiegeEnemy se && se.isHighPriority())) SiegeDamage.apply(hit, ModEffects.SHOCKED.get(), 18, 0);
                hit.setDeltaMovement(Vec3.ZERO);
                Vfx.spray(sl, FxStyle.DROP, hit.position().add(0, hit.getBbHeight() * 0.6, 0), 0x9A1010, 0x5A0808, 0.12f, 8, 0.25f, 0.3f);
            };
            p.aim(aimAt(t), 2.8f).fire(sl);
            Vfx.dash(sl, m, aimAt(t), 0xC8CCD4, 0.25f, 6);
            Vfx.shake(sl, position(), 0.2f, 6, 5);
            return;
        }
        SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.BOLT, this, m).payload(dmg, Element.PHYSICAL)
                .status(ModEffects.BLEEDING, 60, 2);
        p.aim(aimAt(t), 2.4f).scale(lvl >= 4 ? 1.4f : 1f).fire(sl);
    }

    /** Repeater scorpion: one bolt of a three-round burst, ricocheting to a neighbour. */
    private void repeaterBolt(ServerLevel sl, LivingEntity t) {
        float dmg = damage() * 0.5f;
        Vec3 m = muzzle();
        SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.BOLT, this, m).payload(dmg, Element.PHYSICAL)
                .status(ModEffects.BLEEDING, 60, 2);
        p.onEntityHit = (pr, hit) -> {
            List<LivingEntity> o = others(sl, hit, hit.position(), 5, 1);
            if (!o.isEmpty()) {
                SiegeProjectile r = SiegeProjectile.create(sl, ProjKind.BOLT, this, hit.position().add(0, hit.getBbHeight() * 0.6, 0))
                        .payload(dmg * 0.6f, Element.PHYSICAL);
                r.aim(aimAt(o.get(0)), 2f).scale(0.7f).fire(sl);
            }
        };
        p.aim(aimAt(t).add(random.nextGaussian() * 0.1, 0, random.nextGaussian() * 0.1), 2.6f).scale(0.8f).fire(sl);
        Vfx.spray(sl, FxStyle.SPARK, m, 0xFFE8C0, 0xC8A060, 0.06f, 3, 0.2f, 0.2f);
        sound(sl, SoundEvents.CROSSBOW_SHOOT, 0.9f, 1.3f);
    }

    private void flame(ServerLevel sl, Vec3 m, LivingEntity t, float dmg, int lvl) {
        if (isB()) {
            napalmVolley(sl, m, t, dmg);
            return;
        }
        Vec3 dir = t.position().add(0, t.getBbHeight() * 0.4, 0).subtract(m).normalize();
        boolean soul = isA();
        float r = range();
        float cone = soul ? 0.8f : 0.85f;
        Vec3 end = m.add(dir.scale(r));
        if (soul) {
            Vfx.jet(sl, FxStyle.FLAME, m, end, 0xC8F8FF, 0x2A8AFF, 0.5f, 22, 13);
            Vfx.jet(sl, FxStyle.GLOW, m, end, 0x60E0FF, 0x1A4AFF, 0.35f, 8, 9);
        } else {
            Vfx.jet(sl, FxStyle.FLAME, m, end, 0xFFE070, 0xFF3A08, 0.45f + lvl * 0.03f, 16 + lvl * 2, 12);
            if (shotCount % 2 == 0) Vfx.jet(sl, FxStyle.SMOKE, m.add(dir.scale(r * 0.4)), end.add(0, 1.2, 0), 0x5A5450, 0x2A2826, 0.6f, 4, 18);
        }
        if (shotCount % 4 == 0) sound(sl, SoundEvents.BLAZE_BURN, 0.6f, soul ? 1.3f : 0.8f);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, m, r)) {
            if (!valid(e)) continue;
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.4, 0).subtract(m).normalize();
            if (to.dot(dir) < cone) continue;
            float mult = soul ? 1.5f : 1f;
            Element el = Element.FIRE;
            if (soul && SiegeDamage.multiplier(e, Element.FIRE) <= 0) el = Element.ARCANE;
            SiegeDamage.deal(e, this, this, dmg * mult, el, false);
            e.setSecondsOnFire(3);
            if (shotCount % 4 == 0) SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 60, soul ? 3 : 1);
        }
        if (lvl >= 3 && specialTimer >= 40) {
            specialTimer = 0;
            HazardZone.spawn(sl, soul ? HazardZone.Kind.SOUL_FIRE : HazardZone.Kind.FIRE, t.position(), 2f, 80, dmg, this, false);
        }
    }

    private void napalmVolley(ServerLevel sl, Vec3 m, LivingEntity t, float dmg) {
        anim.play(A_FIRE);
        List<LivingEntity> ts = new ArrayList<>(others(sl, t, t.position(), 6, 2));
        ts.add(0, t);
        for (int i = 0; i < 3; i++) {
            LivingEntity e = ts.get(i % ts.size());
            Vec3 at = e.position().add(random.nextGaussian() * (i == 0 ? 0.3 : 1.2), 0, random.nextGaussian() * (i == 0 ? 0.3 : 1.2));
            SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.NAPALM, this, m.add(0, 0.15 * i, 0)).payload(dmg * 6f, Element.FIRE).area(2.5f);
            p.onImpact = (pr, hit) -> HazardZone.spawn(sl, HazardZone.Kind.FIRE, hit, 3f, 100, dmg * 0.6f, this, false);
            p.aim(at, 0.8f + 0.1f * i).fire(sl);
        }
        Vfx.flash(sl, m, 0xFF9A30, 1.4f, 5);
        Vfx.spray(sl, FxStyle.SMOKE, m, 0x6A6460, 0x3A3836, 0.8f, 8, 0.12f, 0.6f);
        sound(sl, SoundEvents.FIRECHARGE_USE, 1.2f, 0.6f);
    }

    private void glacialLance(ServerLevel sl, Vec3 m, LivingEntity t, float dmg) {
        Vec3 dir = aimAt(t).subtract(m).normalize();
        Vec3 end = m.add(dir.scale(range()));
        Vfx.beam(sl, m, end, 0x9FE8FF, 0.35f, 10);
        Vfx.stream(sl, FxStyle.FLAKE, 0xE8FFFF, 0.2f, m, end, 0.8);
        Vfx.flash(sl, m, 0xC8F4FF, 1.4f, 6);
        Vfx.shake(sl, position(), 0.2f, 8, 5);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, m.add(dir.scale(range() / 2)), range() / 2 + 1.5)) {
            Vec3 rel = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(m);
            double along = rel.dot(dir);
            if (along < 0 || rel.subtract(dir.scale(along)).lengthSqr() > 1.2) continue;
            boolean frozen = e.hasEffect(ModEffects.FROZEN.get());
            SiegeDamage.deal(e, this, this, dmg * (frozen ? 7.5f : 2.5f), Element.ICE, false);
            if (frozen) {
                e.removeEffect(ModEffects.FROZEN.get());
                Vfx.spray(sl, FxStyle.SHARD, e.position().add(0, e.getBbHeight() * 0.5, 0), 0xE8FFFF, 0x8FE3FF, 0.25f, 18, 0.4f, 0.4f);
                Fx.sound(sl, e.position(), SoundEvents.GLASS_BREAK, 1.2f, 0.7f);
            } else {
                SiegeDamage.applyStacking(e, ModEffects.CHILLED.get(), 100, 4);
                SiegeDamage.applyStacking(e, ModEffects.CHILLED.get(), 100, 4);
            }
        }
        sound(sl, SoundEvents.GLASS_BREAK, 1f, 1.8f);
        sound(sl, SoundEvents.TRIDENT_THROW, 1f, 0.6f);
    }

    private void frostNova(ServerLevel sl, Vec3 at, float dmg) {
        Vfx.sphere(sl, at.add(0, 0.6, 0), 0x9FE8FF, 4.5f, 12);
        Vfx.ring(sl, at.add(0, 0.05, 0), 0xD8F8FF, 5f, 16, 1.2f);
        Vfx.spray(sl, FxStyle.SHARD, at.add(0, 0.6, 0), 0xE8FFFF, 0x8FE3FF, 0.2f, 24, 0.45f, 0.4f);
        Vfx.spray(sl, FxStyle.FLAKE, at.add(0, 0.6, 0), 0xFFFFFF, 0xBFF4FF, 0.18f, 18, 0.2f, 0);
        Vfx.shake(sl, at, 0.4f, 14, 8);
        sound(sl, SoundEvents.GLASS_BREAK, 1.2f, 0.6f);
        sound(sl, SoundEvents.POWDER_SNOW_BREAK, 1.5f, 0.5f);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, at, 4.5)) {
            SiegeDamage.deal(e, this, this, dmg * 2, Element.ICE, false);
            SiegeDamage.apply(e, ModEffects.FROZEN.get(), 60, 0);
        }
    }

    private void permafrost(ServerLevel sl) {
        float r = 6 * scaleNow();
        Vfx.ring(sl, position().add(0, 0.05, 0), 0x9FE8FF, r, 18, 0.3f);
        Vfx.spray(sl, FxStyle.FLAKE, position().add(0, 0.3, 0), 0xFFFFFF, 0xBFF4FF, 0.14f, 8, 0.2f, -1);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), r)) SiegeDamage.applyStacking(e, ModEffects.CHILLED.get(), 40, 2);
    }

    private void tidalWave(ServerLevel sl, LivingEntity t, float dmg) {
        Vec3 dir = outward(sl, t.position());
        Vec3 start = t.position().subtract(dir.scale(3));
        Vec3 end = t.position().add(dir.scale(9));
        Vfx.wave(sl, start, end, 0x2F7BFF, 6f, 3.2f, 24);
        Vfx.spray(sl, FxStyle.DROP, t.position().add(0, 1, 0), 0xC8E8FF, 0x2F6BFF, 0.18f, 30, 0.4f, 0.8f);
        Vfx.shake(sl, t.position(), 0.5f, 16, 10);
        sound(sl, SoundEvents.GENERIC_SPLASH, 2f, 0.5f);
        sound(sl, SoundEvents.ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, t.position().add(dir.scale(3)), 8)) {
            Vec3 rel = e.position().subtract(start);
            double along = rel.dot(dir), across = Math.abs(rel.dot(side));
            if (along < -1 || along > 13 || across > 3.5) continue;
            SiegeDamage.deal(e, this, this, dmg * 3, Element.WATER, false);
            SiegeDamage.apply(e, ModEffects.SOAKED.get(), 160, 0);
            if (!(e instanceof SiegeEnemy se && se.isHighPriority())) {
                e.setDeltaMovement(dir.x * 1.6, 0.45, dir.z * 1.6);
                e.hurtMarked = true;
            }
        }
    }

    /** Horizontal unit vector pointing away from the core through {@code at}. */
    private Vec3 outward(ServerLevel sl, Vec3 at) {
        BlockPos core = SiegeManager.corePos(sl);
        Vec3 c = core != null ? Vec3.atCenterOf(core) : position();
        Vec3 d = new Vec3(at.x - c.x, 0, at.z - c.z);
        if (d.lengthSqr() < 1e-3) d = new Vec3(at.x - getX(), 0, at.z - getZ());
        if (d.lengthSqr() < 1e-3) d = new Vec3(1, 0, 0);
        return d.normalize();
    }

    private void chain(ServerLevel sl, LivingEntity first, float dmg, int hops, boolean falloff) {
        Set<Integer> hit = new HashSet<>();
        LivingEntity cur = first;
        Vec3 from = muzzle();
        float d = dmg;
        Vfx.flash(sl, from, 0xF8F49A, 0.8f, 3);
        for (int i = 0; i < hops && cur != null; i++) {
            hit.add(cur.getId());
            Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
            Vfx.arc(sl, from, to, 0xF4F06A, i == 0 ? 0.08f : 0.06f, 5, 0.7f);
            Vfx.spray(sl, FxStyle.SPARK, to, 0xFFFFFF, 0xF4F06A, 0.1f, 5, 0.35f, 0.3f);
            SiegeDamage.deal(cur, this, this, d, Element.LIGHTNING, false);
            SiegeDamage.apply(cur, ModEffects.SHOCKED.get(), 10, 0);
            boolean soaked = cur.hasEffect(ModEffects.SOAKED.get());
            if (soaked && i == hops - 1) hops++;
            from = to;
            if (falloff) d *= 0.75f;
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

    private void shell(ServerLevel sl, Vec3 m, Vec3 at, float dmg, int lvl, int delay) {
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
        // a slower, higher arc for later shells in a barrage so they land staggered
        p.aim(jitter, Math.max(0.55f, 1.0f - delay * 0.02f)).scale(1.2f).fire(sl);
    }

    private void grandShell(ServerLevel sl, Vec3 m, LivingEntity t, float dmg) {
        SiegeProjectile p = SiegeProjectile.create(sl, ProjKind.GRAND_SHELL, this, m).payload(dmg * 2.2f, Element.EXPLOSIVE)
                .area(6f).status(ModEffects.CONCUSSED, 120, 0);
        p.knockback = 1.2f;
        p.onEntityHit = (pr, hit) -> {
            if (!(hit instanceof SiegeEnemy se && se.isHighPriority())) {
                hit.setDeltaMovement(hit.getDeltaMovement().add(0, 0.7, 0));
                hit.hurtMarked = true;
            }
        };
        p.aim(t.position(), 1.25f).scale(1.6f).fire(sl);
        Vfx.flash(sl, m, 0xFFC060, 2.6f, 6);
        Vfx.ring(sl, m, 0xD8D0C8, 2.5f, 10, 0.4f);
        Vfx.spray(sl, FxStyle.SMOKE, m, 0x9A948E, 0x4A4846, 1.4f, 16, 0.25f, 0.3f);
        Vfx.spray(sl, FxStyle.SMOKE, position().add(0, 0.2, 0), 0xB8B0A8, 0x8A8480, 1f, 10, 0.25f, -1);
        Vfx.shake(sl, position(), 1.1f, 22, 12);
        sound(sl, SoundEvents.GENERIC_EXPLODE, 1.6f, 0.6f);
        sound(sl, SoundEvents.ANVIL_LAND, 0.8f, 0.5f);
    }

    private void beam(ServerLevel sl, Vec3 m, LivingEntity t, float dmg, int lvl) {
        rampTicks++;
        float rampRate = isA() ? 20f : 40f;
        float ramp = 1f + Math.min(1.5f, rampTicks / rampRate);
        float originH = muzzleHeight() * scaleNow();
        if (isA()) {
            Vec3 dir = aimAt(t).subtract(m).normalize();
            Vec3 end = m.add(dir.scale(range()));
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, m.add(dir.scale(range() / 2)), range() / 2 + 1)) {
                Vec3 rel = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(m);
                double along = rel.dot(dir);
                if (along < 0 || rel.subtract(dir.scale(along)).lengthSqr() > 1.0) continue;
                SiegeDamage.deal(e, this, this, dmg * ramp, Element.ARCANE, false);
                SiegeDamage.applyStacking(e, ModEffects.MARKED.get(), 60, 2);
            }
            if (rampTicks % 2 == 1) Vfx.beam(sl, m, end, 0xD070FF, 0.14f + ramp * 0.08f, 3, 0.04f);
        } else if (isB()) {
            List<LivingEntity> ts = new ArrayList<>(others(sl, t, position(), range(), 3));
            ts.add(0, t);
            for (LivingEntity e : ts) {
                SiegeDamage.deal(e, this, this, dmg * 0.7f * ramp, Element.ARCANE, false);
                if (rampTicks % 10 == 1) SiegeDamage.applyStacking(e, ModEffects.MARKED.get(), 60, 2);
            }
            if (--beamRefresh <= 0) {
                beamRefresh = 10;
                for (LivingEntity e : ts) Vfx.tether(sl, this, originH, e, 0xE090FF, 0.08f + ramp * 0.04f, 12);
            }
        } else {
            SiegeDamage.deal(t, this, this, dmg * ramp, Element.ARCANE, false);
            if (rampTicks % 10 == 1) SiegeDamage.applyStacking(t, ModEffects.MARKED.get(), 60, 2);
            if (--beamRefresh <= 0 || lastBeamTarget != t.getId()) {
                beamRefresh = 10;
                lastBeamTarget = t.getId();
                Vfx.tether(sl, this, originH, t, 0xC86BFF, 0.08f + ramp * 0.05f, 12);
            }
            if (lvl >= 3) {
                List<LivingEntity> o = others(sl, t, t.position(), 6, 1);
                if (!o.isEmpty()) {
                    LivingEntity s = o.get(0);
                    SiegeDamage.deal(s, this, this, dmg * 0.6f, Element.ARCANE, false);
                    if (beamRefresh == 10) Vfx.tether(sl, t, s, 0xE0A0FF, 0.07f, 12);
                }
            }
        }
        if (rampTicks % 6 == 0) Vfx.spray(sl, FxStyle.STAR, aimAt(t), 0xFFFFFF, 0xC86BFF, 0.12f, 3, 0.15f, 0);
        if (rampTicks % 20 == 1) sound(sl, SoundEvents.BEACON_AMBIENT, 1f, 1.5f + ramp * 0.2f);
    }

    /** Seraph's Sanctuary: ticks until it can raise another downed ally. */
    private int sanctuaryReviveCd;

    private void sanctumAura(ServerLevel sl) {
        if (tickCount % 40 != 0) return;
        int lvl = turretLevel();
        boolean sanctuary = isB();
        if (sanctuaryReviveCd > 0) sanctuaryReviveCd -= 40;
        double r = 8 + lvl + (sanctuary ? 3 : 0);
        float heal = (1f + lvl * 0.5f) * (sanctuary ? 2 : 1);
        Vfx.ring(sl, position().add(0, 0.05, 0), 0xFFE8A0, (float) r, 20, 0.3f);
        BlockPos core = SiegeManager.corePos(sl);
        if (core != null && core.closerToCenterThan(position(), r + 1)) {
            SiegeManager.healCore(sl, 2f * lvl * (sanctuary ? 2 : 1));
            if (sanctuary) {
                SiegeData d = SiegeManager.data(sl);
                float cap = Math.max(d.maxShield(), d.maxHp() * 0.2f);
                if (d.shield < cap) {
                    d.shield = Math.min(cap, d.shield + 6 + lvl * 2);
                    d.setDirty();
                }
                Vfx.dome(sl, Vec3.atBottomCenterOf(core), 0xFFE8A0, 3.2f, 44, null);
            }
        }
        for (LivingEntity e : SiegeDamage.defendersAround(sl, position(), r)) {
            if (sanctuary && sanctuaryReviveCd <= 0 && e instanceof net.minecraft.server.level.ServerPlayer sp
                    && com.lastbastion.siege.raid.Downed.isDowned(sp)) {
                com.lastbastion.siege.raid.Downed.revive(sp, null);
                sanctuaryReviveCd = 600;
                Vfx.beam(sl, position().add(0, getBbHeight() * 0.9, 0), sp.position().add(0, 1, 0), 0xFFE8A0, 0.25f, 16);
                Vfx.spray(sl, FxStyle.FEATHER, sp.position().add(0, 1.2, 0), 0xFFFFFF, 0xFFE8A0, 0.25f, 16, 0.2f, 0.6f);
                sl.playSound(null, sp.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.4f);
                continue;
            }
            if (e.getHealth() < e.getMaxHealth()) {
                e.heal(heal);
                Vfx.spray(sl, FxStyle.STAR, e.position().add(0, e.getBbHeight() * 0.6, 0), 0xFFFFFF, 0x9CFFB0, 0.15f, 4, 0.1f, 1);
            }
            if (lvl >= 3 && !(e instanceof TurretEntity && e == this)) SiegeDamage.apply(e, ModEffects.AEGIS.get(), 60, sanctuary ? 1 : 0);
        }
    }

    private void cycloneWall(ServerLevel sl) {
        float r = 4.5f * scaleNow();
        if (tickCount % 40 == 0) Vfx.vortex(sl, position(), 0xE8FFF8, 0x9FFFE8, r, 2.5f, 42, this);
        Vfx.spray(sl, FxStyle.LEAF, position().add(0, 0.5, 0), 0xE8FFF8, 0x9FFFE8, 0.15f, 4, 0.3f, -1);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, position(), r)) {
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

    /** Plague Cauldron contagion: called when an enemy marked by this turret dies. */
    public void spreadPlague(ServerLevel sl, LivingEntity dead) {
        Vec3 at = dead.position();
        HazardZone.spawn(sl, HazardZone.Kind.POISON_CLOUD, at, 2.5f, 60, damage() / 3f, this, false);
        Vfx.spray(sl, FxStyle.SMOKE, at.add(0, 0.6, 0), 0x9BD940, 0x4A6A20, 1f, 10, 0.15f, 0.4f);
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, at, 3.5)) {
            SiegeDamage.applyStacking(e, ModEffects.CORRODED.get(), 120, 4);
            e.getPersistentData().putInt("lb_plague", getId());
        }
    }

    // ------------------------------------------------------------------ damage / physics

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && !p.isCreative()) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) return false;
        boolean r = super.hurt(source, amount);
        if (r) {
            lastHurt = tickCount;
            if (level() instanceof ServerLevel sl && amount >= 2) {
                Vfx.spray(sl, FxStyle.SPARK, position().add(0, 0.7 * scaleNow(), 0), 0xFFE0A0, 0xFF8020, 0.08f, 5, 0.35f, 0.4f);
                if (getHealth() < getMaxHealth() * 0.35f) Vfx.spray(sl, FxStyle.SMOKE, position().add(0, 1.1 * scaleNow(), 0), 0x3A3A3A, 0x1A1A1A, 0.7f, 3, 0.05f, 1);
            }
        }
        return r;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            Fx.explosion(sl, position().add(0, 0.8, 0), 2);
            Vfx.spray(sl, FxStyle.DEBRIS, position().add(0, 0.8, 0), 0x6A6C72, 0x3A3C42, 0.25f, 18, 0.5f, 0.7f);
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
        tag.putInt("lbBranch", branch());
        UUID o = getOwnerId();
        if (o != null) tag.putUUID("lbOwner", o);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(LEVEL, Math.max(1, tag.getInt("lbLevel")));
        entityData.set(MODE, tag.getInt("lbMode"));
        entityData.set(KILLS, tag.getInt("lbKills"));
        entityData.set(BRANCH, tag.getInt("lbBranch"));
        if (tag.hasUUID("lbOwner")) setOwnerId(tag.getUUID("lbOwner"));
        sizeLevel = turretLevel();
        deployed = true;
        refreshDimensions();
    }
}
