package com.lastbastion.entity.boss;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.config.SiegeConfig;
import com.lastbastion.entity.AnimTracker;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.network.Net;
import com.lastbastion.network.SpeechPacket;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import net.minecraftforge.network.PacketDistributor;

/**
 * Boss framework: boss bar, HP phases, a skill state machine with wind-ups and timelines, target
 * selection (core vs defenders), wall crushing and crowd-control resistance. Each boss implements
 * {@link #pickSkill} and {@link #tickSkill} to define its moveset.
 */
public abstract class SiegeBoss extends Monster implements SiegeEnemy {
    public static final int ANIM_NONE = 0, ANIM_MELEE = 1, ANIM_CORE = 2;
    /** Shared animation ids (boss skills use 3..27). */
    public static final int ANIM_STAGGER = 29, ANIM_INTRO = 30, ANIM_PHASE = 28, ANIM_TAUNT = 27;

    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(SiegeBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SiegeBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLAG = SynchedEntityData.defineId(SiegeBoss.class, EntityDataSerializers.BOOLEAN);

    public final BossKind kind;
    public final AnimTracker anim;
    protected final ServerBossEvent bossBar;
    protected float damageScale = 1f;
    protected int skill;
    protected int skillTick;
    protected final int[] cooldowns = new int[16];
    @Nullable
    protected LivingEntity target;
    protected int retarget;
    private int meleeSwing = -1;
    private int meleeCooldown;
    private int repath;
    private boolean coreSwing;
    private float tempoAcc;
    private int pounceCd = 60;
    private int pounceTicks = -1;

    /** Grounded bosses leap or charge at foes who keep their distance, and slam down on landing. */
    protected boolean canPounce() {
        return !isNoGravity();
    }
    /** Stagger meter 0..100: heavy hits and shrugged-off crowd control fill it; full = stunned and vulnerable. */
    protected float stagger;
    private int staggerTicks;
    private int staggerImmune;
    private int lastStaggerHit = -1000;
    private int introTicks;
    private int lastSpoke = -1000;
    protected final ServerBossEvent staggerBar = new ServerBossEvent(Component.literal("Stagger"), BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS);

    protected SiegeBoss(EntityType<? extends SiegeBoss> type, Level level, BossKind kind) {
        super(type, level);
        this.kind = kind;
        this.anim = new AnimTracker(this, ANIM);
        this.bossBar = new ServerBossEvent(Component.literal(kind.title).withStyle(ChatFormatting.BOLD), kind.bar, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossBar.setDarkenScreen(true);
        this.xpReward = 50;
        setPersistenceRequired();
        setMaxUpStep(1.5f);
    }

    public static AttributeSupplier.Builder createAttributes(BossKind k) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, k.hp)
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.ARMOR_TOUGHNESS, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 96);
    }

    @Nullable
    public static SiegeBoss spawn(ServerLevel level, BossKind kind, Vec3 pos, int wave, int players) {
        SiegeBoss b = kind.type().get().create(level);
        if (b == null) return null;
        b.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360, 0);
        com.lastbastion.siege.SiegeRules rules = com.lastbastion.siege.SiegeManager.data(level).rules;
        float scale = (float) ((1 + 0.08 * Math.max(0, wave - 5)) * (1 + 0.6 * (players - 1)) * rules.get(com.lastbastion.siege.SiegeRules.BOSS_HEALTH));
        b.getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.hp * scale);
        b.damageScale = (float) ((1 + 0.03 * Math.max(0, wave - 5)) * rules.get(com.lastbastion.siege.SiegeRules.ENEMY_DAMAGE));
        b.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(kind.damage * b.damageScale);
        b.setHealth(b.getMaxHealth());
        b.finalizeSpawn(level, level.getCurrentDifficultyAt(b.blockPosition()), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(b);
        b.onArrive(level);
        return b;
    }

    /** Dramatic entrance: intro animation, signature effects, a title card and the boss's first words. */
    protected void onArrive(ServerLevel level) {
        introTicks = introLength();
        anim.play(ANIM_INTRO);
        Fx.shockwave(level, position(), 6, dangerColor());
        Vfx.spray(level, FxStyle.SOUL, position().add(0, 1, 0), 0xC8F0FF, dangerColor(), 0.4f, 30, 0.2f, 1);
        Vfx.screenFlash(level, position(), dangerColor(), 0.25f, 80, 14);
        Vfx.shake(level, position(), 0.9f, 60, 20);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 128 * 128) {
                SiegeManager.title(p, Component.literal(kind.title).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                        Component.literal(epithet()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
        speak(Lines.ARRIVE, true);
    }

    /** Subtitle shown under the boss name when it arrives. */
    protected String epithet() {
        return "approaches the walls";
    }

    protected int introLength() {
        return 40;
    }

    /** Colour of this boss's telegraphs and signature effects. */
    public int dangerColor() {
        return Vfx.DANGER;
    }

    // ------------------------------------------------------------------ personality

    public enum Lines {ARRIVE, PHASE, TAUNT, KILL, STAGGER, DEATH, CORE}

    protected String[] lines(Lines which) {
        return new String[0];
    }

    public void speak(Lines which, boolean force) {
        if (!(level() instanceof ServerLevel)) return;
        if (!force && tickCount - lastSpoke < 260) return;
        String[] l = lines(which);
        if (l.length == 0) return;
        lastSpoke = tickCount;
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new SpeechPacket(getId(), l[random.nextInt(l.length)], dangerColor()));
    }

    /** A one-off line for a specific moment (summons, challenges, signature moves). */
    protected void say(String text) {
        if (!(level() instanceof ServerLevel)) return;
        lastSpoke = tickCount;
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new SpeechPacket(getId(), text, dangerColor()));
    }

    // ------------------------------------------------------------------ telegraphs (hostile: heroes dodge them)

    protected void teleCircle(ServerLevel level, Vec3 c, float r, int ticks) {
        Vfx.teleCircle(level, c.add(0, 0.05, 0), r, ticks, dangerColor(), true);
    }

    protected void teleLine(ServerLevel level, Vec3 from, Vec3 to, float w, int ticks) {
        Vfx.teleLine(level, from.add(0, 0.05, 0), to.add(0, 0.05, 0), w, ticks, dangerColor(), true);
    }

    protected void teleCone(ServerLevel level, Vec3 from, Vec3 toward, float len, float half, int ticks) {
        Vfx.teleCone(level, from.add(0, 0.05, 0), toward, len, half, ticks, dangerColor(), true);
    }

    protected void teleDonut(ServerLevel level, Vec3 c, float inner, float outer, int ticks) {
        Vfx.teleDonut(level, c.add(0, 0.05, 0), inner, outer, ticks, dangerColor(), true);
    }

    // ------------------------------------------------------------------ stagger

    public boolean staggered() {
        return staggerTicks > 0;
    }

    public float staggerFraction() {
        return stagger / 100f;
    }

    protected void addStagger(float amount) {
        if (staggerTicks > 0 || amount <= 0) return;
        stagger = Math.min(100, stagger + amount * (staggerImmune > 0 ? 0.4f : 1f));
        lastStaggerHit = tickCount;
        if (stagger >= 100 && level() instanceof ServerLevel sl) {
            staggerTicks = 70;
            staggerImmune = 400;
            stagger = 0;
            skill = 0;
            skillTick = 0;
            getNavigation().stop();
            anim.play(ANIM_STAGGER);
            Vfx.flash(sl, position().add(0, getBbHeight() * 0.6, 0), 0xFFE060, 2.5f, 8);
            Vfx.spray(sl, FxStyle.STAR, position().add(0, getBbHeight() + 0.3, 0), 0xFFE060, 0xFFFFFF, 0.3f, 16, 0.15f, 0);
            Vfx.ring(sl, position().add(0, 0.05, 0), 0xFFE060, getBbWidth() + 3, 14, 1);
            Fx.sound(sl, position(), SoundEvents.ANVIL_LAND, 2f, 0.5f);
            speak(Lines.STAGGER, true);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 64 * 64) {
                    p.displayClientMessage(Component.literal(kind.title + " is STAGGERED! (+30% damage taken)").withStyle(ChatFormatting.YELLOW), true);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIM, 0);
        entityData.define(PHASE, 1);
        entityData.define(FLAG, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (anim != null) anim.onSync(key);
    }

    @Override
    protected void registerGoals() {
        // Bosses are fully driven by the skill state machine in serverTick.
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    public boolean flag() {
        return entityData.get(FLAG);
    }

    protected void setFlag(boolean b) {
        entityData.set(FLAG, b);
    }

    public float dmg(float mult) {
        return kind.damage * damageScale * mult;
    }

    // ------------------------------------------------------------------ SiegeEnemy

    @Override
    public float elementMultiplier(Element e) {
        return switch (e) {
            case HOLY -> 1.3f;
            case SHADOW -> 0f;
            case POISON -> 0.6f;
            case INFERNAL -> 1.15f;
            default -> 1f;
        };
    }

    @Override
    public int creditValue() {
        return kind.credits;
    }

    @Override
    public int heroXp() {
        return 150;
    }

    @Override
    public boolean isWaveMob() {
        return true;
    }

    @Override
    public boolean isHighPriority() {
        return true;
    }

    // ------------------------------------------------------------------ CC resistance

    @Override
    public boolean canBeAffected(MobEffectInstance e) {
        var eff = e.getEffect();
        if (eff == ModEffects.FROZEN.get() || eff == ModEffects.SHOCKED.get() || eff == ModEffects.FEARED.get()
                || eff == ModEffects.DISORIENTED.get()) {
            if (!level().isClientSide) addStagger(eff == ModEffects.FROZEN.get() ? 10 : 5);
            return false;
        }
        if (eff == ModEffects.CONCUSSED.get() && !level().isClientSide) addStagger(3);
        if (eff == ModEffects.CHILLED.get() && e.getAmplifier() > 1) return false;
        return super.canBeAffected(e);
    }

    @Override
    public boolean addEffect(MobEffectInstance e, @Nullable Entity source) {
        if (e.getEffect() == ModEffects.CHILLED.get() && e.getAmplifier() > 1) {
            e = new MobEffectInstance(e.getEffect(), e.getDuration(), 1);
        }
        return super.addEffect(e, source);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void aiStep() {
        try {
        super.aiStep();
        if (level() instanceof ServerLevel sl && isAlive() && !isNoAi()) serverTick(sl);
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("SiegeBoss.aiStep", lbCrash);
        }
    }

    protected void serverTick(ServerLevel level) {
        bossBar.setProgress(getHealth() / getMaxHealth());
        staggerBar.setProgress(staggered() ? staggerTicks / 70f : stagger / 100f);
        staggerBar.setName(Component.literal(staggered() ? "STAGGERED" : "Stagger"));
        if (isNoAi()) return;
        if (tickCount % 20 == 0) updateBossBarPlayers(level);
        if (staggerImmune > 0) staggerImmune--;
        if (staggerTicks > 0) {
            staggerTicks--;
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.2, 1, 0.2));
            if (tickCount % 10 == 0) Vfx.spray(level, FxStyle.STAR, position().add(0, getBbHeight() + 0.3, 0), 0xFFE060, 0xFFFFFF, 0.25f, 3, 0.1f, 0);
            return;
        }
        if (tickCount - lastStaggerHit > 80 && stagger > 0) stagger = Math.max(0, stagger - 0.15f);
        if (introTicks > 0) {
            introTicks--;
            getNavigation().stop();
            introTick(level, introLength() - introTicks);
            return;
        }
        // enraged bosses act faster: x1.5 below half health, x2 below a quarter
        float hpFrac = getHealth() / getMaxHealth();
        tempoAcc += 1f + (hpFrac < 0.5f ? 0.5f : 0f) + (hpFrac < 0.25f ? 0.5f : 0f);
        int rate = (int) tempoAcc;
        tempoAcc -= rate;
        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i] = Math.max(0, cooldowns[i] - rate);
        if (meleeCooldown > 0) meleeCooldown = Math.max(0, meleeCooldown - rate);
        if (pounceCd > 0) pounceCd = Math.max(0, pounceCd - rate);
        checkPhase(level);
        if (--retarget <= 0) {
            retarget = 10;
            target = chooseTarget(level);
            // whoever is hurting it most gets its attention
            LivingEntity agg = getLastHurtByMob();
            if (agg != null && agg != target && agg.isAlive() && tickCount - getLastHurtByMobTimestamp() < 60 && SiegeDamage.isDefender(agg)
                    && !(agg instanceof Player p && (p.isCreative() || p.isSpectator())) && distanceTo(agg) < engageRange() * 1.5
                    && (target == null || random.nextFloat() < 0.6f)) {
                target = agg;
            }
            setTarget(target);
        }
        if (target != null && (!target.isAlive() || (target instanceof Player p && (p.isCreative() || p.isSpectator())))) target = null;

        if (skill != 0) {
            skillTick++;
            if (tickSkill(level, skill, skillTick)) {
                skill = 0;
                skillTick = 0;
            }
            return;
        }
        BlockPos core = SiegeManager.corePos(level);
        double distTarget = target != null ? distanceTo(target) : 999;
        double distCore = core != null ? Math.sqrt(distanceToSqr(Vec3.atBottomCenterOf(core))) : 999;
        int s = pickSkill(level, target, distTarget, distCore);
        if (s != 0) {
            startSkill(s);
            return;
        }
        tickMovementAndMelee(level, core, distTarget, distCore);
        if (horizontalCollision && tickCount % 10 == 0) crushBlocks(level);
    }

    /** Called every tick of the entrance (t = 1..introLength). */
    protected void introTick(ServerLevel level, int t) {
    }

    protected void startSkill(int id) {
        skill = id;
        skillTick = 0;
        anim.play(id);
        getNavigation().stop();
        meleeSwing = -1;
    }

    /** Default target choice: nearest defender within engage range, else none (core). */
    @Nullable
    protected LivingEntity chooseTarget(ServerLevel level) {
        LivingEntity best = null;
        double bestD = engageRange() * engageRange();
        for (LivingEntity e : SiegeDamage.defendersAround(level, position(), engageRange())) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            double d = distanceToSqr(e) * (e instanceof TurretEntity ? 1.3 : 1.0);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    protected double engageRange() {
        return 14;
    }

    protected double meleeReach() {
        return getBbWidth() * 0.5 + 2.2;
    }

    protected void tickMovementAndMelee(ServerLevel level, @Nullable BlockPos core, double distTarget, double distCore) {
        Vec3 goal;
        double reach = meleeReach();
        boolean vsCore = target == null;
        if (vsCore) {
            if (core == null) return;
            goal = Vec3.atBottomCenterOf(core);
        } else {
            goal = target.position();
            reach += target.getBbWidth() * 0.5;
        }
        double dist = vsCore ? distCore : distTarget;
        if (pounceTicks >= 0) {
            pounceTicks++;
            if (goal != null) getLookControl().setLookAt(goal.x, goal.y + 1, goal.z);
            if ((onGround() && pounceTicks > 4) || pounceTicks > 40) {
                pounceTicks = -1;
                Vfx.ring(level, position().add(0, 0.1, 0), dangerColor(), (float) meleeReach() + 1.5f, 14, 0.5f);
                Vfx.shake(level, position(), 0.4f, 10, 16);
                meleeImpact(level);
                meleeCooldown = Math.min(meleeCooldown, 10);
            }
            return;
        }
        if (!vsCore && canPounce() && pounceCd <= 0 && onGround() && meleeSwing < 0 && dist > reach + 3 && dist < 15 && hasLineOfSight(target)) {
            pounceCd = 110 + random.nextInt(60);
            pounceTicks = 0;
            Vec3 to = goal.subtract(position());
            double d = Math.max(0.1, to.horizontalDistance());
            double power = net.minecraft.util.Mth.clamp((d - reach) / 7.5, 0.5, 1.8);
            setDeltaMovement(to.x / d * power, 0.45, to.z / d * power);
            hasImpulse = true;
            getNavigation().stop();
            anim.play(ANIM_MELEE);
            Vfx.spray(level, FxStyle.SMOKE, position().add(0, 0.2, 0), 0x8A7A6A, 0x4A3A2A, 1f, 14, 0.25f, -1);
            Fx.sound(level, position(), SoundEvents.RAVAGER_ROAR, 1.2f, 1.1f);
            return;
        }
        if (meleeSwing >= 0) {
            meleeSwing++;
            getLookControl().setLookAt(goal.x, goal.y + 1, goal.z);
            if (meleeSwing == meleeWindup()) {
                if (coreSwing && core != null && distCore <= reach + 1) {
                    SiegeManager.damageCore(level, coreDamage(), this);
                    Fx.shockwave(level, Vec3.atBottomCenterOf(core), 2.5, 0x550000);
                } else if (!coreSwing) {
                    meleeImpact(level);
                }
            }
            if (meleeSwing >= meleeWindup() + 8) meleeSwing = -1;
            return;
        }
        if (dist <= reach) {
            getNavigation().stop();
            getLookControl().setLookAt(goal.x, goal.y + 1, goal.z);
            if (meleeCooldown <= 0) {
                meleeSwing = 0;
                coreSwing = vsCore;
                meleeCooldown = meleeInterval();
                anim.play(vsCore ? ANIM_CORE : ANIM_MELEE);
            }
        } else if (--repath <= 0 || getNavigation().isDone()) {
            repath = 8;
            moveToward(goal, dist);
        }
    }

    protected void moveToward(Vec3 goal, double dist) {
        if (dist > 20) {
            Vec3 dir = goal.subtract(position()).multiply(1, 0, 1).normalize().scale(14);
            Vec3 p = position().add(dir);
            getNavigation().moveTo(p.x, goal.y, p.z, 1.25);
        } else {
            getNavigation().moveTo(goal.x, goal.y, goal.z, dist > 8 ? 1.35 : 1.2);
        }
    }

    protected int meleeWindup() {
        return 14;
    }

    protected int meleeInterval() {
        return 40;
    }

    protected float coreDamage() {
        return dmg(2.0f);
    }

    /** Default melee: cleave everything in a frontal arc. */
    protected void meleeImpact(ServerLevel level) {
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot()).scale(meleeReach() * 0.6);
        Vec3 c = position().add(fwd).add(0, 1, 0);
        for (LivingEntity e : SiegeDamage.defendersAround(level, c, meleeReach() * 0.8)) {
            SiegeDamage.deal(e, this, this, dmg(1f), Element.PHYSICAL, false);
            SiegeDamage.push(e, position(), 0.9, 0.35);
            onMeleeHit(level, e);
        }
        Vfx.slash(level, position().add(0, getBbHeight() * 0.5, 0), getYRot(), 15, 150, (float) meleeReach(), dangerColor(), 0xFFFFFF, 9, 0.3f);
        Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
    }

    protected void onMeleeHit(ServerLevel level, LivingEntity e) {
    }

    @Override
    public void awardKillScore(Entity killed, int score, DamageSource source) {
        super.awardKillScore(killed, score, source);
        if (killed instanceof Player || killed instanceof com.lastbastion.entity.hero.HeroEntity) speak(Lines.KILL, false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (staggered()) amount *= 1.3f;
        boolean r = super.hurt(source, amount);
        if (r && !level().isClientSide) {
            float frac = amount / Math.max(1, getMaxHealth());
            float add = frac * 220f;
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) add *= 1.5f;
            if (amount > getMaxHealth() * 0.02f) add += 3;
            addStagger(add);
        }
        return r;
    }

    /** Crush walls the boss walks into. */
    protected void crushBlocks(ServerLevel level) {
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot()).scale(0.8);
        AABB box = getBoundingBox().move(fwd).deflate(0.05, 0, 0.05).move(0, 0.4, 0);
        boolean any = false;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.getBlock() instanceof com.lastbastion.block.SiegeCoreBlock) continue;
            float h = s.getDestroySpeed(level, p);
            if (h < 0 || h > 50) continue;
            level.destroyBlock(p, false, this);
            any = true;
        }
        if (any) Fx.sound(level, position(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f);
    }

    /** @return the skill id to start (0 = none). */
    protected abstract int pickSkill(ServerLevel level, @Nullable LivingEntity target, double distTarget, double distCore);

    /** @return true when the skill has finished. */
    protected abstract boolean tickSkill(ServerLevel level, int skill, int t);

    protected boolean ready(int id) {
        return cooldowns[id] <= 0;
    }

    protected void cooldown(int id, int ticks) {
        cooldowns[id] = phase() >= 2 ? (int) (ticks * 0.65f) : ticks;
    }

    // ------------------------------------------------------------------ phases

    private void checkPhase(ServerLevel level) {
        int p = phase();
        float frac = getHealth() / getMaxHealth();
        int target = frac <= phaseThreshold(3) ? 3 : frac <= phaseThreshold(2) ? 2 : 1;
        if (target > p) {
            entityData.set(PHASE, target);
            onPhase(level, target);
        }
    }

    /** HP fraction at which the given phase begins (phase 3 disabled by default). */
    protected float phaseThreshold(int phase) {
        return phase == 2 ? 0.5f : -1f;
    }

    protected void onPhase(ServerLevel level, int phase) {
        Fx.shockwave(level, position(), 7, dangerColor());
        Vfx.screenFlash(level, position(), dangerColor(), 0.2f, 60, 10);
        Fx.sound(level, position(), SoundEvents.ENDER_DRAGON_GROWL, 2f, 0.7f);
        speak(Lines.PHASE, true);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 80 * 80) {
                p.sendSystemMessage(Component.literal(kind.title + " " + phaseLine(phase)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
        }
    }

    protected String phaseLine(int phase) {
        return "becomes enraged!";
    }

    // ------------------------------------------------------------------ helpers

    protected Vec3 forward(double dist) {
        return position().add(Vec3.directionFromRotation(0, getYRot()).scale(dist));
    }

    protected void face(Vec3 at) {
        double dx = at.x - getX(), dz = at.z - getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    protected Vec3 aimPoint() {
        if (target != null) return target.position().add(0, target.getBbHeight() * 0.5, 0);
        BlockPos core = level() instanceof ServerLevel sl ? SiegeManager.corePos(sl) : null;
        return core != null ? Vec3.atCenterOf(core) : forward(5);
    }

    @Nullable
    protected BlockPos core() {
        return level() instanceof ServerLevel sl ? SiegeManager.corePos(sl) : null;
    }

    private void updateBossBarPlayers(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            boolean near = p.distanceToSqr(this) < 96 * 96;
            if (near && !bossBar.getPlayers().contains(p)) {
                bossBar.addPlayer(p);
                staggerBar.addPlayer(p);
            } else if (!near && bossBar.getPlayers().contains(p)) {
                bossBar.removePlayer(p);
                staggerBar.removePlayer(p);
            }
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
        staggerBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        bossBar.removeAllPlayers();
        staggerBar.removeAllPlayers();
        super.remove(reason);
    }

    /** Bosses take two and a half seconds to die, wracked by bursts of their own energy. */
    @Override
    protected void tickDeath() {
        ++deathTime;
        if (level() instanceof ServerLevel sl) {
            if (deathTime % 8 == 1) {
                Vec3 at = position().add((random.nextDouble() - 0.5) * getBbWidth(), random.nextDouble() * getBbHeight(), (random.nextDouble() - 0.5) * getBbWidth());
                Vfx.flash(sl, at, dangerColor(), 1.4f, 6);
                Vfx.spray(sl, FxStyle.SPARK, at, 0xFFFFFF, dangerColor(), 0.15f, 10, 0.4f, 0.3f);
                Fx.sound(sl, at, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f + random.nextFloat() * 0.3f);
            }
            if (deathTime >= 50 && !isRemoved()) {
                Fx.explosion(sl, position().add(0, 1, 0), 5);
                Vfx.pillar(sl, position(), 0xFFD966, getBbWidth(), 40, 40);
                Vfx.screenFlash(sl, position(), 0xFFF4D0, 0.35f, 80, 12);
                Vfx.spray(sl, FxStyle.SOUL, position().add(0, 1, 0), 0xFFFFFF, dangerColor(), 0.5f, 40, 0.3f, 1);
                level().broadcastEntityEvent(this, (byte) 60);
                com.lastbastion.siege.raid.RaidEvents.dropToken(sl, position().add(0, 1, 0), 1);
                com.lastbastion.siege.Milestones.awardSiege(sl, com.lastbastion.siege.Milestones.BOSS);
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            speak(Lines.DEATH, true);
            skill = 0;
            anim.play(ANIM_INTRO + 1);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 128 * 128) {
                    SiegeManager.title(p, Component.literal(kind.title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.literal("has been vanquished!").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        bossBar.removeAllPlayers();
        staggerBar.removeAllPlayers();
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 2f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("lbDmgScale", damageScale);
        tag.putInt("lbPhase", phase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damageScale = tag.contains("lbDmgScale") ? tag.getFloat("lbDmgScale") : 1f;
        entityData.set(PHASE, Math.max(1, tag.getInt("lbPhase")));
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }
}
