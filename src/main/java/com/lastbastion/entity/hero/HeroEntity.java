package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.AnimTracker;
import com.lastbastion.network.Net;
import com.lastbastion.network.OpenScreenPacket;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Base hero AI. Each hero runs a brain every tick:
 * <ol>
 *   <li><b>Skill in progress</b> - follow its timeline (wind-up, impact frames, recovery). Nothing interrupts it.</li>
 *   <li><b>Target selection</b> (every 10 ticks) by mode:
 *     <ul><li>GUARD - enemies within 18 blocks of the core, scored by distance-to-core (closest = most dangerous) and core damage.</li>
 *     <li>HUNT - anything within 48 blocks of the core; bosses/champions/specials weighted heavily, then distance to self.</li>
 *     <li>FOLLOW - enemies within 12 blocks of the summoner, preferring those attacking them.</li></ul></li>
 *   <li><b>Skill choice</b> - each hero evaluates its abilities (cooldown + situational conditions such as enemy count, HP, distance).</li>
 *   <li><b>Basic attack</b> - melee or ranged with wind-up animation; ranged heroes kite to their preferred range.</li>
 *   <li><b>Idle</b> - GUARD returns to its post around the core, FOLLOW trails the owner (teleporting if left far behind), HUNT patrols.</li>
 * </ol>
 * Heroes regenerate out of combat, level up from kills (Lv 1-5: +12% HP/damage per level and a brighter aura),
 * and when slain can be revived at the core for half price.
 */
public abstract class HeroEntity extends PathfinderMob implements SiegeAlly {
    public static final int ANIM_NONE = 0, ANIM_ATTACK = 1, ANIM_ATTACK2 = 2, ANIM_ATTACK3 = 3;
    public static final int[] XP_LEVELS = {0, 60, 160, 360, 700};

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> XP = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLAG = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.BOOLEAN);

    public final HeroKind kind;
    public final AnimTracker anim;
    @Nullable
    protected LivingEntity target;
    protected int skill;
    protected int skillTick;
    protected final int[] cooldowns = new int[16];
    private int retarget;
    private int attackCooldown;
    private int swing = -1;
    protected int combo;
    private int repath;
    private int lastDamaged = -1000;
    private final float postAngle;

    protected HeroEntity(EntityType<? extends HeroEntity> type, Level level, HeroKind kind) {
        super(type, level);
        this.kind = kind;
        this.anim = new AnimTracker(this, ANIM);
        this.postAngle = level.random.nextFloat() * Mth.TWO_PI;
        setPersistenceRequired();
        setMaxUpStep(1.2f);
        xpReward = 0;
        if (kind.flying) setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes(HeroKind k) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, k.hp)
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.ARMOR_TOUGHNESS, k.armor / 3)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, k.hp > 300 ? 0.9 : 0.5)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER, Optional.empty());
        entityData.define(MODE, 0);
        entityData.define(ANIM, 0);
        entityData.define(LEVEL, 1);
        entityData.define(XP, 0);
        entityData.define(FLAG, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        anim.onSync(key);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    // ------------------------------------------------------------------ properties

    @Override
    @Nullable
    public UUID getOwnerId() {
        return entityData.get(OWNER).orElse(null);
    }

    public void setOwnerId(@Nullable UUID id) {
        entityData.set(OWNER, Optional.ofNullable(id));
    }

    @Nullable
    public Player owner() {
        UUID id = getOwnerId();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    public HeroMode mode() {
        return HeroMode.byId(entityData.get(MODE));
    }

    public void setMode(HeroMode m) {
        entityData.set(MODE, m.ordinal());
        target = null;
        retarget = 0;
    }

    public int heroLevel() {
        return entityData.get(LEVEL);
    }

    public int xp() {
        return entityData.get(XP);
    }

    public boolean flag() {
        return entityData.get(FLAG);
    }

    protected void setFlag(boolean b) {
        entityData.set(FLAG, b);
    }

    public void setLevelAndXp(int level, int xp) {
        entityData.set(LEVEL, Mth.clamp(level, 1, 5));
        entityData.set(XP, xp);
        applyLevelStats();
    }

    public void addXp(int amount) {
        int xp = xp() + amount;
        entityData.set(XP, xp);
        int lvl = heroLevel();
        while (lvl < 5 && xp >= XP_LEVELS[lvl]) lvl++;
        if (lvl > heroLevel()) {
            entityData.set(LEVEL, lvl);
            applyLevelStats();
            setHealth(getMaxHealth());
            if (level() instanceof ServerLevel sl) {
                Fx.spiral(sl, position(), getBbWidth() + 0.5, getBbHeight() + 1, Fx.dust(kind.color, 1.5f), 3, 0);
                Fx.skyBeam(sl, position(), 0xFFE38A, 12, 1);
                sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1.2f, 0.8f);
                if (owner() instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.literal(kind.title + " reached Level " + lvl + "!").withStyle(ChatFormatting.GOLD), false);
                }
            }
        }
    }

    protected void applyLevelStats() {
        float m = 1f + 0.12f * (heroLevel() - 1);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.hp * m);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(kind.damage * m);
    }

    /** Damage for a given multiplier of the hero's base damage, including level and buffs. */
    public float dmg(float mult) {
        float d = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * mult;
        return d;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!player.getUUID().equals(getOwnerId()) && !player.hasPermissions(2)) {
            if (!level().isClientSide) player.displayClientMessage(Component.literal("This hero answers to another commander."), true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player instanceof ServerPlayer sp) {
            if (player.isShiftKeyDown()) {
                setMode(HeroMode.values()[(mode().ordinal() + 1) % HeroMode.values().length]);
                sp.displayClientMessage(Component.literal(kind.title + ": " + mode().title).withStyle(ChatFormatting.AQUA), true);
                playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.2f);
            } else {
                Net.sendTo(sp, OpenScreenPacket.hero(getId()));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // ------------------------------------------------------------------ brain

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel sl && isAlive()) brain(sl);
    }

    private void brain(ServerLevel level) {
        if (isNoAi()) return;
        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
        if (attackCooldown > 0) attackCooldown--;
        if (tickCount - lastDamaged > 100 && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(getMaxHealth() * 0.02f);
        if (heroLevel() >= 4 && tickCount % 6 == 0) {
            level.sendParticles(Fx.dust(kind.color, 0.9f), getX(), getY() + getBbHeight() * 0.5, getZ(), 1,
                    getBbWidth() * 0.6, getBbHeight() * 0.4, getBbWidth() * 0.6, 0);
        }
        passiveTick(level);

        if (target != null && !validTarget(target)) target = null;
        if (skill != 0) {
            skillTick++;
            if (tickSkill(level, skill, skillTick)) {
                skill = 0;
                skillTick = 0;
            }
            return;
        }
        if (--retarget <= 0) {
            retarget = 10;
            LivingEntity t = pickTarget(level);
            if (t != null) target = t;
        }
        setTarget(target);
        if (hasEffect(ModEffects.FROZEN.get()) || hasEffect(ModEffects.SHOCKED.get())) return;

        double dist = target != null ? distanceTo(target) : 999;
        int s = pickSkill(level, target, dist);
        if (s != 0) {
            startSkill(s);
            return;
        }
        if (target != null) engage(level, target, dist);
        else idle(level);
    }

    protected void startSkill(int id) {
        skill = id;
        skillTick = 0;
        anim.play(id);
        getNavigation().stop();
        swing = -1;
    }

    protected boolean ready(int id) {
        return cooldowns[id] <= 0;
    }

    protected void cooldown(int id, int ticks) {
        cooldowns[id] = ticks;
    }

    protected boolean validTarget(LivingEntity e) {
        if (!e.isAlive() || e.isRemoved() || !SiegeDamage.isEnemy(e)) return false;
        if (e instanceof SiegeEnemy se && se.isUntargetable()) return false;
        return distanceToSqr(e) < 48 * 48;
    }

    @Nullable
    protected LivingEntity pickTarget(ServerLevel level) {
        BlockPos core = SiegeManager.corePos(level);
        Vec3 corePos = core != null ? Vec3.atCenterOf(core) : position();
        Player owner = owner();
        HeroMode m = mode();
        if (m == HeroMode.FOLLOW && owner == null) m = HeroMode.GUARD;
        Vec3 centre;
        double radius;
        switch (m) {
            case GUARD -> {
                centre = corePos;
                radius = 18;
            }
            case FOLLOW -> {
                centre = owner.position();
                radius = 12;
            }
            default -> {
                centre = corePos;
                radius = 48;
            }
        }
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, centre, radius)) {
            if (!validTarget(e)) continue;
            double score;
            if (m == HeroMode.GUARD) {
                score = e.position().distanceTo(corePos) + distanceTo(e) * 0.3;
            } else if (m == HeroMode.FOLLOW) {
                score = e.distanceTo(owner) + (e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == owner ? -6 : 0);
            } else {
                score = distanceTo(e);
                if (e instanceof SiegeEnemy se && se.isHighPriority()) score -= 25;
                if (e instanceof com.lastbastion.entity.zombie.NecromancerZombie || e instanceof com.lastbastion.entity.zombie.ShriekerZombie) score -= 12;
            }
            if (e == target) score -= 3; // stickiness
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** Approach and use the basic attack. */
    protected void engage(ServerLevel level, LivingEntity t, double dist) {
        double range = attackRange() + t.getBbWidth() * 0.5;
        getLookControl().setLookAt(t, 30, 30);
        if (swing >= 0) {
            swing++;
            if (swing == attackWindup()) {
                if (distanceTo(t) <= range + 1.5) basicAttack(level, t, combo);
                combo = (combo + 1) % comboLength();
            }
            if (swing >= attackWindup() + 4) swing = -1;
            if (kind.flying) hover(t.position().add(0, preferredAltitude(), 0), 0.2);
            return;
        }
        double want = preferredRange();
        if (dist <= range && attackCooldown <= 0 && (isRanged() || dist <= range)) {
            swing = 0;
            attackCooldown = attackInterval();
            anim.play(ANIM_ATTACK + combo % 3);
            if (!kind.flying) getNavigation().stop();
            return;
        }
        if (kind.flying) {
            Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
            Vec3 dest = t.position().add(away.normalize().scale(Math.max(2, want))).add(0, preferredAltitude(), 0);
            hover(dest, flySpeed());
            return;
        }
        if (--repath <= 0) {
            repath = 8;
            if (isRanged() && dist < want * 0.6) {
                Vec3 away = position().subtract(t.position()).multiply(1, 0, 1).normalize().scale(4);
                getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.2);
            } else if (dist > want) {
                getNavigation().moveTo(t, 1.15);
            } else if (isRanged()) {
                getNavigation().stop();
            }
        }
    }

    protected void idle(ServerLevel level) {
        BlockPos core = SiegeManager.corePos(level);
        Player owner = owner();
        Vec3 dest = null;
        double tolerance = 2.5;
        switch (mode()) {
            case GUARD -> {
                if (core != null) {
                    double r = 4 + getBbWidth();
                    dest = Vec3.atBottomCenterOf(core).add(Math.cos(postAngle) * r, 0, Math.sin(postAngle) * r);
                }
            }
            case FOLLOW -> {
                if (owner != null) {
                    if (distanceToSqr(owner) > 28 * 28 && owner.level() == level()) {
                        teleportTo(owner.getX() + random.nextGaussian(), owner.getY(), owner.getZ() + random.nextGaussian());
                        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 20, 0.4, 0.6, 0.4, 0.1);
                    }
                    dest = owner.position();
                    tolerance = 3 + getBbWidth();
                }
            }
            case HUNT -> {
                if (core != null) {
                    double a = postAngle + tickCount * 0.004;
                    dest = Vec3.atBottomCenterOf(core).add(Math.cos(a) * 20, 0, Math.sin(a) * 20);
                    tolerance = 4;
                }
            }
        }
        if (dest == null) return;
        if (kind.flying) {
            hover(dest.add(0, preferredAltitude(), 0), flySpeed() * 0.7);
            return;
        }
        if (position().distanceToSqr(dest) > tolerance * tolerance) {
            if (--repath <= 0) {
                repath = 20;
                getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0);
            }
        } else {
            getNavigation().stop();
        }
    }

    /** Flight steering for airborne heroes. */
    protected void hover(Vec3 dest, double speed) {
        int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(dest.x), Mth.floor(dest.z));
        if (dest.y < ground + 1.5) dest = new Vec3(dest.x, ground + 1.5, dest.z);
        Vec3 v = dest.subtract(position());
        Vec3 want = v.lengthSqr() < 0.04 ? Vec3.ZERO : v.normalize().scale(Math.min(speed, v.length() * 0.2));
        setDeltaMovement(getDeltaMovement().scale(0.8).add(want.scale(0.25)));
        Vec3 m = getDeltaMovement();
        if (target != null) faceTowards(target.position());
        else if (m.horizontalDistanceSqr() > 1e-3) faceTowards(position().add(m));
    }

    protected void faceTowards(Vec3 at) {
        double dx = at.x - getX(), dz = at.z - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        setYRot(Mth.rotLerp(0.3f, getYRot(), yaw));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    protected Vec3 forward(double d) {
        return position().add(Vec3.directionFromRotation(0, getYRot()).scale(d));
    }

    // ------------------------------------------------------------------ hero-specific hooks

    protected abstract void basicAttack(ServerLevel level, LivingEntity t, int combo);

    protected abstract int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist);

    /** @return true when finished. */
    protected abstract boolean tickSkill(ServerLevel level, int skill, int t);

    protected void passiveTick(ServerLevel level) {
    }

    protected double attackRange() {
        return getBbWidth() * 0.5 + 1.8;
    }

    protected double preferredRange() {
        return attackRange() * 0.8;
    }

    protected boolean isRanged() {
        return false;
    }

    protected int attackWindup() {
        return 7;
    }

    protected int attackInterval() {
        return 20;
    }

    protected int comboLength() {
        return 1;
    }

    protected double preferredAltitude() {
        return 4;
    }

    protected double flySpeed() {
        return 0.55;
    }

    /** Melee helper: damage everything in a small frontal arc. */
    protected int cleave(ServerLevel level, double reach, double radius, float damage, Element element) {
        Vec3 c = forward(reach).add(0, getBbHeight() * 0.5, 0);
        int n = 0;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, c, radius)) {
            if (SiegeDamage.deal(e, this, this, damage, element, false)) n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ lifecycle

    public void onWaveCleared() {
        setHealth(getMaxHealth());
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 5, 0.4, 0.2, 0.4, 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && !p.isCreative()) return false;
        if (kind.flying && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return false;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return false;
        boolean r = super.hurt(source, amount);
        if (r) lastDamaged = tickCount;
        return r;
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return !kind.flying && super.causeFallDamage(d, m, s);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl && getOwnerId() != null) {
            SiegeData d = SiegeManager.data(sl);
            d.fallenHeroes.add(new SiegeData.FallenHero(getOwnerId(), kind.ordinal(), heroLevel(), xp()));
            d.setDirty();
            if (owner() instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal(kind.title + " has fallen! Revive them at the Siege Core.")
                        .withStyle(ChatFormatting.RED), false);
            }
            Fx.skyBeam(sl, position(), kind.color, 15, 0.8);
        }
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
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID o = getOwnerId();
        if (o != null) tag.putUUID("lbOwner", o);
        tag.putInt("lbMode", entityData.get(MODE));
        tag.putInt("lbLevel", heroLevel());
        tag.putInt("lbXp", xp());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("lbOwner")) setOwnerId(tag.getUUID("lbOwner"));
        entityData.set(MODE, tag.getInt("lbMode"));
        setLevelAndXp(Math.max(1, tag.getInt("lbLevel")), tag.getInt("lbXp"));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof Player || other instanceof SiegeAlly || super.isAlliedTo(other);
    }
}
