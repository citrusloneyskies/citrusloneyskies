package com.lastbastion.entity.zombie;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.AnimTracker;
import com.lastbastion.entity.ai.AssaultCoreGoal;
import com.lastbastion.entity.ai.FearGoal;
import com.lastbastion.entity.ai.IncapacitatedGoal;
import com.lastbastion.entity.ai.SiegeMeleeGoal;
import com.lastbastion.entity.ai.SiegeTargetGoal;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.NightModifier;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Base class for all wave zombies. Handles ranks, champion affixes, spawning-from-the-ground,
 * elemental resistances, core assault and shared AI. Subclasses add their signature behaviour.
 */
public class SiegeZombie extends Monster implements SiegeEnemy {
    public static final int ANIM_NONE = 0, ANIM_ATTACK = 1, ANIM_CORE = 2, ANIM_SPECIAL = 3, ANIM_SPECIAL2 = 4, ANIM_BLOCK = 5;

    private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(SiegeZombie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AFFIXES = SynchedEntityData.defineId(SiegeZombie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(SiegeZombie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RISE = SynchedEntityData.defineId(SiegeZombie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SHIELD = SynchedEntityData.defineId(SiegeZombie.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> STATE = SynchedEntityData.defineId(SiegeZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WARLORD = SynchedEntityData.defineId(SiegeZombie.class, EntityDataSerializers.BOOLEAN);

    public static final int RISE_TICKS = 30;
    private static final UUID HASTE_ID = UUID.fromString("0b6f5a52-91f3-4f43-8c0e-1fa3b1d0a201");
    private static final UUID JUGG_ID = UUID.fromString("0b6f5a52-91f3-4f43-8c0e-1fa3b1d0a202");

    public final ZombieKind kind;
    public final AnimTracker anim;
    private boolean waveMob;
    private float damageScale = 1f;
    private int clientRiseStart = -1;
    private int riseLeft;
    private int lastDamagedTick;
    private boolean revived;
    private boolean minion;
    private float sizeScale = 1f;
    @Nullable
    public UUID tauntedBy;
    public int tauntTicks;
    @Nullable
    public Vec3 fearFrom;

    public SiegeZombie(EntityType<? extends SiegeZombie> type, Level level, ZombieKind kind) {
        super(type, level);
        this.kind = kind;
        this.anim = new AnimTracker(this, ANIM);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.setMaxUpStep(1.0f);
    }

    public static AttributeSupplier.Builder createAttributes(ZombieKind k) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, k.hp)
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, k.kbResist)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RANK, 0);
        entityData.define(AFFIXES, 0);
        entityData.define(ANIM, 0);
        entityData.define(RISE, 0);
        entityData.define(SHIELD, 0f);
        entityData.define(STATE, false);
        entityData.define(WARLORD, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (anim != null) anim.onSync(key);
        if (RANK.equals(key) || AFFIXES.equals(key) || WARLORD.equals(key)) updateSize();
        if (RISE.equals(key) && level().isClientSide && entityData.get(RISE) > 0) clientRiseStart = tickCount;
    }

    // ------------------------------------------------------------------ goals

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new IncapacitatedGoal(this));
        goalSelector.addGoal(2, new FearGoal(this));
        addSpecialGoals();
        goalSelector.addGoal(5, new SiegeMeleeGoal(this));
        goalSelector.addGoal(6, new AssaultCoreGoal(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new SiegeTargetGoal(this));
    }

    /** Subclass hook for signature abilities (priority 3-4). */
    protected void addSpecialGoals() {
    }

    // ------------------------------------------------------------------ setup

    /** Configure stats for the wave this zombie belongs to. */
    public void setup(ZombieRank rank, int affixes, float hpScale, float dmgScale, boolean waveMob) {
        entityData.set(RANK, rank.ordinal());
        entityData.set(AFFIXES, affixes);
        this.waveMob = waveMob;
        this.damageScale = dmgScale * rank.dmgMult;
        float hp = kind.hp * rank.hpMult * hpScale;
        if (Affix.JUGGERNAUT.in(affixes)) {
            hp *= 2f;
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        }
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(kind.damage * damageScale);
        getAttribute(Attributes.ARMOR).setBaseValue(kind.armor + rank.bonusArmor);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(kind.speed * rank.speedMult);
        if (Affix.HASTED.in(affixes)) {
            getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                    new AttributeModifier(HASTE_ID, "Hasted", 0.4, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        setHealth(getMaxHealth());
        if (Affix.SHIELDED.in(affixes)) entityData.set(SHIELD, getMaxHealth() * 0.35f);
        updateSize();
        if (affixes != 0) {
            MutableComponent name = Component.empty();
            for (Affix a : Affix.values()) {
                if (a.in(affixes)) name.append(Component.literal(a.title + " ").withStyle(s -> s.withColor(a.color)));
            }
            name.append(Component.translatable("entity.lastbastion." + kind.id).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            setCustomName(name);
            setCustomNameVisible(true);
        }
    }

    // ------------------------------------------------------------------ warlords

    @Nullable
    private net.minecraft.server.level.ServerBossEvent warlordBar;

    public boolean isWarlord() {
        return entityData.get(WARLORD);
    }

    /**
     * Promote this zombie to a Warlord: a named, bigger, much tougher elite with its own boss bar that
     * rallies nearby zombies and drops a Warforged Token.
     */
    public void makeWarlord(String title) {
        if (isWarlord()) return;
        entityData.set(WARLORD, true);
        damageScale *= 1.4f;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(getMaxHealth() * 2.5);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(kind.damage * damageScale);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.9);
        setHealth(getMaxHealth());
        Component name = Component.literal("Warlord " + title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        setCustomName(name);
        setCustomNameVisible(true);
        setGlowingTag(true);
        updateSize();
    }

    private net.minecraft.server.level.ServerBossEvent warlordBar() {
        if (warlordBar == null) {
            warlordBar = new net.minecraft.server.level.ServerBossEvent(getDisplayName(), net.minecraft.world.BossEvent.BossBarColor.YELLOW,
                    net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_6);
        }
        return warlordBar;
    }

    private void warlordTick(ServerLevel level) {
        var bar = warlordBar();
        bar.setProgress(Math.max(0, getHealth() / getMaxHealth()));
        if (tickCount % 10 == 0) {
            for (net.minecraft.server.level.ServerPlayer p : level.players()) {
                boolean near = p.distanceToSqr(this) < 64 * 64;
                if (near && !bar.getPlayers().contains(p)) bar.addPlayer(p);
                else if (!near && bar.getPlayers().contains(p)) bar.removePlayer(p);
            }
        }
        if (tickCount % 40 == 0) {
            boolean any = false;
            for (SiegeZombie z : level.getEntitiesOfClass(SiegeZombie.class, getBoundingBox().inflate(8), z -> z != this && z.isAlive())) {
                z.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 60, 0, true, false));
                z.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 60, 0, true, false));
                any = true;
            }
            if (any) com.lastbastion.fx.Vfx.ring(level, position().add(0, 0.1, 0), 0xFFB000, 8f, 14, 0.5f);
        }
    }

    @Override
    public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (warlordBar != null) warlordBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (warlordBar != null) warlordBar.removeAllPlayers();
        super.remove(reason);
    }

    /** Force this zombie to fight {@code by} for {@code ticks} (hero taunts). */
    public void taunt(LivingEntity by, int ticks) {
        tauntedBy = by.getUUID();
        tauntTicks = Math.max(tauntTicks, ticks);
        setTarget(by);
    }

    public void beginRise() {
        riseLeft = RISE_TICKS;
        entityData.set(RISE, RISE_TICKS);
    }

    // ------------------------------------------------------------------ accessors

    public ZombieRank rank() {
        return ZombieRank.byId(entityData.get(RANK));
    }

    public int affixes() {
        return entityData.get(AFFIXES);
    }

    public boolean hasAffix(Affix a) {
        return a.in(affixes());
    }

    public boolean isChampion() {
        return affixes() != 0;
    }

    public float shield() {
        return entityData.get(SHIELD);
    }

    public boolean state() {
        return entityData.get(STATE);
    }

    public void setState(boolean b) {
        entityData.set(STATE, b);
    }

    public boolean isRising() {
        return level().isClientSide ? riseProgress(0) < 1f : riseLeft > 0;
    }

    /** 0 = fully underground, 1 = emerged. */
    public float riseProgress(float pt) {
        if (clientRiseStart < 0) return 1f;
        return Math.min(1f, (tickCount - clientRiseStart + pt) / RISE_TICKS);
    }

    public float damageScale() {
        return damageScale;
    }

    public boolean isIncapacitated() {
        return isRising() || hasEffect(ModEffects.FROZEN.get()) || hasEffect(ModEffects.SHOCKED.get());
    }

    public boolean isWaveMob() {
        return waveMob;
    }

    public void setWaveMob(boolean b) {
        waveMob = b;
    }

    /** Max distance at which this zombie diverts from the core to fight a defender. */
    public double aggroRange() {
        return 7.0;
    }

    /** Whether proximity targeting should consider turrets (true for bashers/wreckers). */
    public boolean targetsTurrets() {
        return false;
    }

    public int attackWindup() {
        return 8;
    }

    public int attackCooldown() {
        return hasAffix(Affix.HASTED) ? 13 : 22;
    }

    public double attackReach() {
        return getBbWidth() * 0.5 + 1.4;
    }

    public float coreDamage() {
        return kind.coreDamage * damageScale;
    }

    /** Multiplier on block-breaking speed (Brutes x3). */
    public float breachPower() {
        return 1f + rank().ordinal() * 0.25f;
    }

    public boolean canBreachHard() {
        return false;
    }

    @Override
    public int creditValue() {
        float v = kind.credits * rank().creditMult;
        if (isChampion()) v *= 3f;
        if (isWarlord()) v *= 4f;
        return Math.max(1, Math.round(v));
    }

    @Override
    public boolean isHighPriority() {
        return isChampion() || isWarlord();
    }

    @Override
    public boolean immuneToStatus() {
        return hasAffix(Affix.WARDED);
    }

    @Override
    public float elementMultiplier(Element e) {
        if (e == Element.INFERNAL) return 1.25f;
        float m = switch (e) {
            case HOLY -> 1.5f;
            case POISON -> 0.75f;
            case SHADOW -> 0.5f;
            default -> 1f;
        };
        if (rank().ordinal() >= ZombieRank.CORRUPTED.ordinal()) {
            if (e == Element.SHADOW) m = 0f;
            if (e == Element.ARCANE) m *= 0.8f;
        }
        if (hasAffix(Affix.MOLTEN) && e == Element.FIRE) m = 0f;
        if (hasAffix(Affix.FROSTBOUND) && e == Element.ICE) m = 0f;
        return m * kindElementMultiplier(e);
    }

    protected float kindElementMultiplier(Element e) {
        return 1f;
    }

    // ------------------------------------------------------------------ combat

    @Override
    public boolean doHurtTarget(Entity target) {
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (target instanceof SiegeAlly && !(target instanceof Player)) dmg *= turretDamageMultiplier();
        boolean hit;
        if (target instanceof LivingEntity le) {
            hit = SiegeDamage.deal(le, this, this, dmg, Element.PHYSICAL, true);
        } else {
            hit = target.hurt(damageSources().mobAttack(this), dmg);
        }
        if (hit && target instanceof LivingEntity le) {
            onMeleeHit(le, dmg);
            if (hasAffix(Affix.VAMPIRIC)) heal(dmg * 0.4f);
            if (hasAffix(Affix.MOLTEN)) le.setSecondsOnFire(4);
            if (hasAffix(Affix.FROSTBOUND)) SiegeDamage.applyStacking(le, ModEffects.CHILLED.get(), 60, 2);
        }
        return hit;
    }

    protected float turretDamageMultiplier() {
        return 1f;
    }

    protected void onMeleeHit(LivingEntity target, float dmg) {
    }

    /** Called by the assault goal when the swing connects with the core. */
    public void onCoreStrike(ServerLevel level, BlockPos core) {
        float dmg = coreDamage();
        SiegeManager.damageCore(level, dmg, this);
        if (hasAffix(Affix.VAMPIRIC)) heal(dmg * 0.4f);
        Vec3 c = Vec3.atCenterOf(core);
        level.sendParticles(ParticleTypes.CRIT, c.x, c.y + 0.5, c.z, 8, 0.4, 0.4, 0.4, 0.2);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isRising() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) amount *= 0.5f;
        float shield = shield();
        if (shield > 0 && amount > 0 && !level().isClientSide) {
            float absorbed = Math.min(shield, amount);
            entityData.set(SHIELD, shield - absorbed);
            amount -= absorbed;
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(Fx.dust(0xFFE38A, 1.2f), getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 0.5, 0.6, 0.5, 0);
            }
            if (amount <= 0) {
                lastDamagedTick = tickCount;
                return false;
            }
        }
        boolean r = super.hurt(source, amount);
        if (r) {
            lastDamagedTick = tickCount;
            if (hasAffix(Affix.REFLECTIVE) && source.getEntity() instanceof LivingEntity att && att instanceof SiegeAlly
                    && !(att instanceof Player) && att != this) {
                att.hurt(damageSources().thorns(this), amount * 0.2f);
            }
        }
        return r;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && level() instanceof ServerLevel sl && isWarlord()) {
            if (warlordBar != null) warlordBar.removeAllPlayers();
            com.lastbastion.siege.raid.RaidEvents.onWarlordSlain(sl, this, source);
        }
        if (!level().isClientSide && level() instanceof ServerLevel sl) {
            if (hasAffix(Affix.SPLITTING)) {
                for (int i = 0; i < 2; i++) {
                    SiegeZombie z = ZombieSpawner.spawn(sl, ZombieKind.RUNNER, rank(), 0, position().add(random.nextGaussian() * 0.5, 0, random.nextGaussian() * 0.5), waveMob, false);
                    if (z != null) z.setDeltaMovement((random.nextDouble() - 0.5) * 0.6, 0.4, (random.nextDouble() - 0.5) * 0.6);
                }
                sl.sendParticles(Fx.dust(0x7BD940, 1.8f), getX(), getY() + 1, getZ(), 30, 0.5, 0.5, 0.5, 0.1);
            }
            if (!revived && waveMob && SiegeManager.modifier(sl) == NightModifier.NECROTIC && random.nextFloat() < 0.3f && !minion) {
                SiegeZombie z = ZombieSpawner.spawn(sl, kind, rank(), 0, position(), true, true);
                if (z != null) {
                    z.revived = true;
                    sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.5, getZ(), 15, 0.4, 0.3, 0.4, 0.02);
                }
            }
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel sl) {
            if (isWarlord() && isAlive()) warlordTick(sl);
            if (!isNoAi()) serverTick(sl);
        } else if (isRising()) {
            BlockState below = level().getBlockState(blockPosition().below());
            if (!below.isAir()) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below), getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                        getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * getBbWidth(), 0, 0.1, 0);
            }
        }
    }

    protected void serverTick(ServerLevel level) {
        if (isNoAi()) return;
        if (riseLeft > 0) {
            riseLeft--;
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            if (riseLeft == RISE_TICKS - 1) Fx.sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.6f);
        }
        if (tauntTicks > 0 && --tauntTicks == 0) tauntedBy = null;
        int aff = affixes();
        if (aff != 0) {
            if (tickCount % 4 == 0) {
                level.sendParticles(Fx.dust(0xFFC83D, 0.9f), getX(), getY() + getBbHeight() * 0.6, getZ(), 1, getBbWidth() * 0.6, getBbHeight() * 0.4, getBbWidth() * 0.6, 0);
            }
            if (Affix.MOLTEN.in(aff) && tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 1, 0.2, 0, 0.2, 0);
                for (LivingEntity d : SiegeDamage.defendersAround(level, position(), 1.8)) {
                    SiegeDamage.deal(d, this, 1.5f * damageScale, Element.FIRE);
                }
            }
            if (Affix.FROSTBOUND.in(aff) && tickCount % 20 == 0) {
                Fx.ring(level, position().add(0, 0.2, 0), 3.5, ParticleTypes.SNOWFLAKE, 18);
                for (LivingEntity d : SiegeDamage.defendersAround(level, position(), 4)) {
                    SiegeDamage.applyStacking(d, ModEffects.CHILLED.get(), 40, 1);
                }
            }
            if (Affix.SHIELDED.in(aff) && tickCount - lastDamagedTick > 80 && tickCount % 20 == 0) {
                float max = getMaxHealth() * 0.35f;
                entityData.set(SHIELD, Math.min(max, shield() + max * 0.08f));
            }
            if (Affix.WARDED.in(aff) && tickCount % 10 == 0) {
                getActiveEffects().stream().filter(e -> !e.getEffect().isBeneficial()).map(MobEffectInstance::getEffect)
                        .toList().forEach(this::removeEffect);
            }
            if (Affix.BLINKING.in(aff) && tickCount % 160 == 80 && !isIncapacitated()) {
                blinkTowardCore(level, 7);
            }
        }
    }

    public void blinkTowardCore(ServerLevel level, double dist) {
        BlockPos core = SiegeManager.corePos(level);
        if (core == null) return;
        Vec3 to = Vec3.atBottomCenterOf(core).subtract(position());
        if (to.horizontalDistance() < 4) return;
        Vec3 dest = position().add(to.multiply(1, 0, 1).normalize().scale(Math.min(dist, to.horizontalDistance() - 3)));
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(dest.x), (int) Math.floor(dest.z));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 25, 0.3, 0.6, 0.3, 0.1);
        teleportTo(dest.x, y, dest.z);
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 25, 0.3, 0.6, 0.3, 0.3);
        Fx.sound(level, position(), SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.3f);
    }

    // ------------------------------------------------------------------ misc vanilla overrides

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
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
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (1.1f - rank().ordinal() * 0.08f);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return super.getDimensions(pose).scale(sizeScale == 0f ? 1f : sizeScale);
    }

    private void updateSize() {
        sizeScale = rank().scale * (Affix.JUGGERNAUT.in(affixes()) ? 1.25f : 1f) * (entityData.get(WARLORD) ? 1.3f : 1f) * baseScale();
        refreshDimensions();
    }

    /** Extra model/hitbox scale for a subclass (multiplied with rank scale). */
    protected float baseScale() {
        return 1f;
    }

    public float renderScale() {
        return sizeScale == 0f ? 1f : sizeScale;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("lbRank", entityData.get(RANK));
        tag.putInt("lbAffixes", affixes());
        tag.putBoolean("lbWave", waveMob);
        tag.putFloat("lbDmgScale", damageScale);
        tag.putFloat("lbShield", shield());
        tag.putBoolean("lbRevived", revived);
        tag.putBoolean("lbMinion", minion);
        tag.putBoolean("lbWarlord", isWarlord());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(RANK, tag.getInt("lbRank"));
        entityData.set(AFFIXES, tag.getInt("lbAffixes"));
        waveMob = tag.getBoolean("lbWave");
        damageScale = tag.contains("lbDmgScale") ? tag.getFloat("lbDmgScale") : 1f;
        entityData.set(SHIELD, tag.getFloat("lbShield"));
        revived = tag.getBoolean("lbRevived");
        minion = tag.getBoolean("lbMinion");
        entityData.set(WARLORD, tag.getBoolean("lbWarlord"));
        updateSize();
    }

    /** Necromancer/boss summons don't revive and give a third of the credits. */
    public boolean isMinion() {
        return minion;
    }

    public void setMinion(boolean b) {
        minion = b;
    }

    /** True while a subclass is steering itself (burrowing, phasing, charging). Blocks shared AI goals. */
    public boolean overrideMovement() {
        return false;
    }

    public static boolean isFriendlyEnemy(Mob a, Entity b) {
        return b instanceof SiegeEnemy && a != b;
    }
}
