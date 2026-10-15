package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeAlly;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.AnimTracker;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.fx.DangerZones;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.network.Net;
import com.lastbastion.network.OpenScreenPacket;
import com.lastbastion.network.SpeechPacket;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeData;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base hero AI. Every tick the brain runs, in order:
 * <ol>
 *   <li><b>Danger check</b> - if standing in a telegraphed boss/elite attack ({@link DangerZones}), dodge out
 *   (cancelling a skill that is still winding up).</li>
 *   <li><b>Skill in progress</b> - follow its timeline (wind-up, impact frames, recovery).</li>
 *   <li><b>Morale</b> - non-tanks below their retreat threshold fall back toward the core and healers until
 *   patched up; tanks never retreat.</li>
 *   <li><b>Target selection</b> (every 10 ticks), scored by mode and role: GUARD protects the core,
 *   HUNT roams for priority targets, FOLLOW protects the summoner. Every role values enemies that are
 *   hitting the core or an ally; tanks pull bosses and big groups, assassins dive the back line
 *   (spitters, necromancers, shriekers) and wounded prey, ranged heroes pick off whatever threatens allies.</li>
 *   <li><b>Ultimate</b> - charged by dealing and taking damage; fired when the situation is worth it.</li>
 *   <li><b>Skills</b> - each hero scores its kit against the situation (crowds, HP, distance, allies).</li>
 *   <li><b>Basic attack and footwork</b> - melee heroes close in and circle between swings, ranged heroes kite
 *   to their preferred range and strafe while shooting.</li>
 *   <li><b>Idle</b> - GUARD holds a post at the core, FOLLOW escorts the owner, HUNT patrols the ring.</li>
 * </ol>
 * Heroes regenerate out of combat, level up from kills (Lv 1-5, +12% HP/damage per level, extra gear on the
 * model) and speak a few lines in character. They don't need a Siege Core: away from one they guard the spot they
 * were ordered to, hunt around their summoner, or follow them (through portals too), joining whatever fight their
 * summoner picks and defending them when struck. When slain, their sigil returns to the summoner and recovers.
 */
public abstract class HeroEntity extends PathfinderMob implements SiegeAlly {
    /** Basic attack animation ids are 1..5 (combo step); hero skills use 6..19. */
    public static final int ANIM_NONE = 0, ANIM_ATTACK = 1, ANIM_ATTACK2 = 2, ANIM_ATTACK3 = 3;
    public static final int ANIM_ULT = 20, ANIM_DODGE = 21, ANIM_ARRIVE = 22, ANIM_LEVELUP = 23, ANIM_STAGGER = 24;
    public static final int ULT = 20;
    public static final int[] XP_LEVELS = {0, 60, 160, 360, 700};

    public enum Role {TANK, BRUISER, ASSASSIN, RANGED, SUPPORT}

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> XP = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLAG = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ULT_ENERGY = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> COMBAT = SynchedEntityData.defineId(HeroEntity.class, EntityDataSerializers.BOOLEAN);

    public final HeroKind kind;
    public final AnimTracker anim;
    @Nullable
    protected LivingEntity target;
    protected int skill;
    protected int skillTick;
    protected final int[] cooldowns = new int[24];
    private int retarget;
    private int attackCooldown;
    private int swing = -1;
    protected int combo;
    private int repath;
    private int lastDamaged = -1000;
    private int lastCombat = -1000;
    private int dodgeCooldown;
    private int lastSpoke = -1000;
    protected boolean retreating;
    private int strafeDir = 1;
    private int strafeTimer;
    private final float postAngle;
    /** Where a Guard order was given (used when there is no Siege Core around). */
    @Nullable
    private BlockPos guardPos;
    /** Something the owner is fighting (or that attacked us) - fought even if it isn't a monster. */
    @Nullable
    private LivingEntity commanded;
    private int commandedUntil;

    protected HeroEntity(EntityType<? extends HeroEntity> type, Level level, HeroKind kind) {
        super(type, level);
        this.kind = kind;
        this.anim = new AnimTracker(this, ANIM);
        this.postAngle = level.random.nextFloat() * Mth.TWO_PI;
        setPersistenceRequired();
        setMaxUpStep(kind.hp > 300 ? 1.6f : 1.2f);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes(HeroKind k) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, k.hp)
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.ARMOR_TOUGHNESS, k.armor / 3)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, k.hp > 300 ? 0.9 : 0.5)
                .add(Attributes.FOLLOW_RANGE, 64);
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
        entityData.define(ULT_ENERGY, 0f);
        entityData.define(COMBAT, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (anim != null) anim.onSync(key);
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
        retreating = false;
        commanded = null;
        if (m == HeroMode.GUARD) guardPos = blockPosition();
    }

    public void setGuardPos(@Nullable BlockPos p) {
        guardPos = p == null ? null : p.immutable();
    }

    /** The Siege Core this hero serves, if one is near enough to matter (heroes far from it act as companions). */
    @Nullable
    public BlockPos core(ServerLevel level) {
        BlockPos c = SiegeManager.corePos(level);
        return c != null && c.distSqr(blockPosition()) < 160 * 160 ? c : null;
    }

    /** The owner, if alive and close enough to escort. */
    @Nullable
    protected Player ownerHere() {
        Player o = owner();
        return o != null && o.isAlive() && !o.isSpectator() && o.level() == level() && distanceToSqr(o) < 96 * 96 ? o : null;
    }

    private Vec3 postCentre() {
        return guardPos != null ? Vec3.atBottomCenterOf(guardPos) : position();
    }

    /** Fight {@code e} for a while even if it isn't a monster (the owner's fight, or self-defence). */
    public void command(LivingEntity e, int ticks) {
        if (e == this || !Companions.fairGame(e, owner())) return;
        if (commanded != e) retarget = 0;
        commanded = e;
        commandedUntil = tickCount + ticks;
    }

    /** Join the owner's fights: whatever they hit, and whatever hits them. */
    private void listenToOwner(ServerLevel level) {
        if (mode() == HeroMode.GUARD && core(level) != null) return;
        Player o = ownerHere();
        if (o == null || distanceToSqr(o) > 40 * 40) return;
        LivingEntity by = Companions.struckBy(level, o, 100);
        if (by != null && by != this && by.distanceToSqr(this) < 32 * 32) command(by, 240);
        else {
            LivingEntity hit = Companions.struck(level, o, 60);
            if (hit != null && hit != this && hit.distanceToSqr(this) < 32 * 32) command(hit, 300);
        }
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

    /** True while the hero has a target (client poses: weapon raised, fighting stance). */
    public boolean inCombat() {
        return entityData.get(COMBAT);
    }

    /** Ultimate charge, 0..100. */
    public float ultEnergy() {
        return entityData.get(ULT_ENERGY);
    }

    public boolean ultReady() {
        return ultEnergy() >= 100f;
    }

    public void gainUlt(float amount) {
        if (amount <= 0 || skill == ULT) return;
        entityData.set(ULT_ENERGY, Math.min(100f, ultEnergy() + amount));
    }

    protected void spendUlt() {
        entityData.set(ULT_ENERGY, 0f);
    }

    public void setLevelAndXp(int level, int xp) {
        entityData.set(LEVEL, Mth.clamp(level, 1, 5));
        entityData.set(XP, xp);
        applyLevelStats();
    }

    public void addXp(int amount) {
        if (!isAlive()) return;
        int xp = xp() + amount;
        entityData.set(XP, xp);
        int lvl = heroLevel();
        while (lvl < 5 && xp >= XP_LEVELS[lvl]) lvl++;
        if (lvl > heroLevel()) {
            entityData.set(LEVEL, lvl);
            applyLevelStats();
            setHealth(getMaxHealth());
            if (level() instanceof ServerLevel sl) {
                Vfx.vortex(sl, position(), kind.color, 0xFFE38A, getBbWidth() + 0.6f, getBbHeight() + 1, 30, this);
                Vfx.pillar(sl, position(), 0xFFE38A, getBbWidth() * 0.7f, 12, 24);
                Vfx.spray(sl, FxStyle.STAR, position().add(0, getBbHeight() * 0.6, 0), 0xFFFFFF, kind.color, 0.3f, 24, 0.35f, 0.6f);
                sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1.2f, 0.8f);
                if (skill == 0) anim.play(ANIM_LEVELUP);
                speak(Lines.LEVEL_UP, true);
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
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * mult;
    }

    /** Deal hero damage (charges the ultimate). */
    protected boolean hit(LivingEntity e, float amount, Element element) {
        boolean ok = SiegeDamage.deal(e, this, this, amount, element, false);
        if (ok) gainUlt(amount * ultGainPerDamage());
        return ok;
    }

    /** Ultimate energy gained every half second while fighting. */
    protected float ultPassive() {
        return 0.35f;
    }

    /** Ultimate energy per point of damage dealt. Tuned (with {@link #ultPassive}) for roughly one ultimate per minute of fighting. */
    protected float ultGainPerDamage() {
        return 0.12f;
    }

    // ------------------------------------------------------------------ personality

    /** Speech categories. */
    public enum Lines {ARRIVE, ULT, BOSS, KILL, LOW_HP, LEVEL_UP, RETREAT, IDLE}

    /** Lines this hero says, by category (may be empty). */
    protected String[] lines(Lines which) {
        return new String[0];
    }

    /** Say a random line from a category; rate-limited unless {@code force}. */
    public void speak(Lines which, boolean force) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (!force && tickCount - lastSpoke < 220) return;
        String[] l = lines(which);
        if (l.length == 0) return;
        lastSpoke = tickCount;
        String text = l[random.nextInt(l.length)];
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new SpeechPacket(getId(), text, kind.color));
    }

    /** Called by the sigil once the hero is in the world. */
    public void onSummoned(ServerLevel level) {
        anim.play(ANIM_ARRIVE);
        speak(Lines.ARRIVE, true);
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
        LivingEntity lbPrev = com.lastbastion.combat.SiegeDamage.pushHostile(getTarget());
        try {
        super.aiStep();
        if (level() instanceof ServerLevel sl && isAlive()) brain(sl);
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("HeroEntity.aiStep", lbCrash);
        } finally {
            com.lastbastion.combat.SiegeDamage.popHostile(lbPrev);
        }
    }

    private void brain(ServerLevel level) {
        if (isNoAi()) return;
        // keep this summon's record for the per-player limits fresh; a summon whose pact lapsed while it sat unloaded
        // (and whose owner has since filled its place) dissolves
        if (tickCount % 100 == 7 && getOwnerId() != null && !Companions.touch(level, this)) {
            if (owner() instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal(kind.title + " was left untended too long and its bond has faded.")
                        .withStyle(ChatFormatting.GRAY), false);
            }
            Vfx.spray(level, FxStyle.SMOKE, position().add(0, getBbHeight() * 0.5, 0), 0x6A6A70, 0x2A2A30, 1f, 20, 0.2f, 0.5f);
            claimSigilReturn();
            discard();
            return;
        }
        int rate = cooldownRate();
        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i] = Math.max(0, cooldowns[i] - rate);
        if (attackCooldown > 0) attackCooldown = Math.max(0, attackCooldown - rate);
        if (dodgeCooldown > 0) dodgeCooldown--;
        boolean fighting = target != null || tickCount - lastCombat < 60;
        if (!fighting && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(getMaxHealth() * (retreating ? 0.05f : 0.025f));
        if (fighting && tickCount % 10 == 0) gainUlt(ultPassive());
        passiveTick(level);
        if (skill == 0 && isNoGravity()) setNoGravity(false);

        if (target != null && !validTarget(target)) target = null;
        entityData.set(COMBAT, target != null);
        if (target != null) lastCombat = tickCount;

        // 1. get out of telegraphed attacks
        if (dodgeCooldown <= 0 && (skill == 0 || (skillTick < 6 && skill != ULT)) && !hasEffect(ModEffects.FROZEN.get())) {
            DangerZones.Zone z = DangerZones.threatAt(level, position(), true, getBbWidth() * 0.5 + 0.4);
            if (z != null && canDodge()) {
                interruptSkill();
                dodge(level, z.escape(position()));
                return;
            }
        }
        // 2. skill in progress
        if (skill != 0) {
            skillTick++;
            if (tickSkill(level, skill, skillTick)) {
                skill = 0;
                skillTick = 0;
            }
            return;
        }
        // 3. morale
        if (role() != Role.TANK) {
            if (!retreating && getHealth() < getMaxHealth() * retreatThreshold() && target != null) {
                retreating = true;
                speak(Lines.RETREAT, false);
            } else if (retreating && getHealth() > getMaxHealth() * 0.6f) {
                retreating = false;
            }
        }
        if (tickCount % 5 == 0) listenToOwner(level);
        if (--retarget <= 0) {
            retarget = 10;
            LivingEntity t = pickTarget(level);
            if (t != target && t instanceof SiegeEnemy se && se.isHighPriority() && (target == null || !(target instanceof SiegeEnemy o && o.isHighPriority()))) {
                speak(Lines.BOSS, false);
            }
            target = t;
        }
        setTarget(target);
        if (hasEffect(ModEffects.FROZEN.get()) || hasEffect(ModEffects.SHOCKED.get())) return;

        double dist = target != null ? distanceTo(target) : 999;
        if (ultReady() && skill == 0) {
            if (wantsUltimate(level, target, dist)) {
                spendUlt();
                speak(Lines.ULT, true);
                startSkill(ULT);
                return;
            }
        }
        int s = pickSkill(level, target, dist);
        if (s != 0) {
            startSkill(s);
            return;
        }
        if (retreating) {
            fallBack(level);
            return;
        }
        if (target != null) engage(level, target, dist);
        else idle(level);
    }

    protected boolean canDodge() {
        return true;
    }

    /** Quick sidestep / roll out of danger. */
    protected void dodge(ServerLevel level, Vec3 dir) {
        dodgeCooldown = 26;
        getNavigation().stop();
        anim.play(ANIM_DODGE);
        double power = role() == Role.TANK ? 0.65 : 0.95;
        setDeltaMovement(dir.x * power, onGround() ? 0.28 : getDeltaMovement().y, dir.z * power);
        hasImpulse = true;
        faceTowards(position().add(dir.scale(-1)));
        Vfx.dash(level, position().add(0, 0.6, 0), position().add(dir.scale(2.2)).add(0, 0.6, 0), kind.color, 0.5f, 8);
        Vfx.spray(level, FxStyle.SMOKE, position().add(0, 0.2, 0), 0xB8B0A8, 0x8A8480, 0.6f, 5, 0.1f, -1);
    }

    protected void startSkill(int id) {
        skill = id;
        skillTick = 0;
        anim.play(id);
        getNavigation().stop();
        swing = -1;
    }

    /** Abort the running skill (a dodge, a recall, a test) and let the hero undo whatever state it had set up. */
    public void interruptSkill() {
        if (skill == 0) return;
        int s = skill;
        skill = 0;
        skillTick = 0;
        onSkillInterrupted(s);
    }

    /** Clean up after a skill that was cut short: flight, invisibility, invulnerability, held victims. */
    protected void onSkillInterrupted(int skill) {
        if (isNoGravity()) setNoGravity(false);
    }

    /** Test hook: start a skill (or {@link #ULT}) right now, cutting off whatever is running. */
    public void forceSkill(int id) {
        interruptSkill();
        if (id == ULT) speak(Lines.ULT, true);
        startSkill(id);
    }

    /** Test hook: every skill ready at once. */
    public void resetCooldowns() {
        java.util.Arrays.fill(cooldowns, 0);
    }

    /** The skill currently running (0 when none). */
    public int activeSkill() {
        return skill;
    }

    private boolean sigilReturned;

    /** True the first time only: a summon hands back its sigil at most once (death, dismissal or a defeat). */
    public boolean claimSigilReturn() {
        if (sigilReturned) return false;
        sigilReturned = true;
        return true;
    }

    protected boolean ready(int id) {
        return cooldowns[id] <= 0;
    }

    protected void cooldown(int id, int ticks) {
        cooldowns[id] = ticks;
    }

    protected boolean validTarget(LivingEntity e) {
        if (!e.isAlive() || e.isRemoved() || e.level() != level()) return false;
        if (e instanceof SiegeEnemy se && se.isUntargetable()) return false;
        if (distanceToSqr(e) >= 48 * 48) return false;
        if (e == commanded && tickCount < commandedUntil) return true;
        return SiegeDamage.isEnemy(e) && !(e instanceof Player);
    }

    /** Neutral mobs (endermen, zombified piglins...) are left alone unless they're already fighting our side. */
    private static boolean provoked(LivingEntity e) {
        if (!(e instanceof net.minecraft.world.entity.NeutralMob)) return true;
        return e instanceof Mob m && m.getTarget() != null && SiegeDamage.isDefender(m.getTarget());
    }

    @Nullable
    protected LivingEntity pickTarget(ServerLevel level) {
        BlockPos core = core(level);
        Vec3 corePos = core != null ? Vec3.atCenterOf(core) : postCentre();
        Player owner = ownerHere();
        HeroMode m = mode();
        if (m == HeroMode.FOLLOW && owner == null) m = core != null ? HeroMode.GUARD : HeroMode.HUNT;
        if (commanded != null && tickCount < commandedUntil && validTarget(commanded)) return commanded;
        commanded = null;
        Vec3 centre;
        double radius;
        switch (m) {
            case GUARD -> {
                centre = corePos;
                radius = core != null ? 18 : 14;
            }
            case FOLLOW -> {
                centre = owner.position();
                radius = 14;
            }
            default -> {
                if (core != null) {
                    centre = corePos;
                    radius = 48;
                } else {
                    centre = owner != null ? owner.position() : postCentre();
                    radius = 28;
                }
            }
        }
        Role role = role();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, centre, radius)) {
            if (!validTarget(e) || e instanceof Player || !provoked(e)) continue;
            double score;
            if (m == HeroMode.GUARD) {
                score = e.position().distanceTo(corePos) + distanceTo(e) * 0.35;
            } else if (m == HeroMode.FOLLOW) {
                score = e.distanceTo(owner) + (e instanceof Mob mob && mob.getTarget() == owner ? -8 : 0);
            } else {
                score = distanceTo(e) * 0.8;
            }
            boolean priority = e instanceof SiegeEnemy se && se.isHighPriority();
            boolean backline = e instanceof com.lastbastion.entity.zombie.NecromancerZombie || e instanceof com.lastbastion.entity.zombie.ShriekerZombie
                    || e instanceof com.lastbastion.entity.zombie.SpitterZombie;
            LivingEntity victim = e instanceof Mob mob ? mob.getTarget() : null;
            boolean hittingAlly = victim != null && victim != this && SiegeDamage.isDefender(victim) && victim.distanceToSqr(e) < 16;
            if (core != null && e.position().distanceToSqr(corePos) < 16) score -= 8;
            if (hittingAlly) score -= role == Role.TANK || role == Role.SUPPORT ? 10 : 6;
            if (victim == this) score -= role == Role.RANGED ? 7 : 3;
            switch (role) {
                case TANK -> {
                    if (priority) score -= 18;
                    score -= SiegeDamage.enemiesAround(level, e.position(), 4).size() * 1.5;
                }
                case ASSASSIN -> {
                    if (backline) score -= 14;
                    score -= (1 - e.getHealth() / e.getMaxHealth()) * 8;
                    if (priority && m == HeroMode.HUNT) score -= 10;
                }
                case RANGED -> {
                    if (backline) score -= 6;
                    if (priority) score -= 8;
                }
                case BRUISER -> {
                    if (priority) score -= 14;
                    score -= SiegeDamage.enemiesAround(level, e.position(), 3.5).size();
                }
                case SUPPORT -> {
                    if (priority) score -= 6;
                }
            }
            if (m == HeroMode.HUNT && priority) score -= 12;
            if (e == target) score -= 4;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** Approach and use the basic attack, with footwork between swings. */
    protected void engage(ServerLevel level, LivingEntity t, double dist) {
        double range = attackRange() + t.getBbWidth() * 0.5;
        getLookControl().setLookAt(t, 40, 40);
        if (swing >= 0) {
            swing++;
            if (swing == attackWindup()) {
                if (distanceTo(t) <= range + 1.5) basicAttack(level, t, combo);
                combo = (combo + 1) % comboLength();
            }
            if (swing >= attackWindup() + attackRecover()) swing = -1;
            return;
        }
        double want = preferredRange();
        if (dist <= range && attackCooldown <= 0) {
            swing = 0;
            attackCooldown = attackInterval();
            anim.play(ANIM_ATTACK + combo % 5);
            getNavigation().stop();
            faceTowards(t.position());
            return;
        }
        if (--strafeTimer <= 0) {
            strafeTimer = 30 + random.nextInt(40);
            strafeDir = random.nextBoolean() ? 1 : -1;
        }
        if (isRanged()) {
            if (dist < want * 0.55) {
                Vec3 away = position().subtract(t.position()).multiply(1, 0, 1).normalize().scale(5);
                if (--repath <= 0) {
                    repath = 8;
                    getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.25);
                }
            } else if (dist > range * 0.95) {
                if (--repath <= 0) {
                    repath = 8;
                    getNavigation().moveTo(t, 1.1);
                }
            } else {
                getNavigation().stop();
                getMoveControl().strafe(0, 0.35f * strafeDir);
                faceTowards(t.position());
            }
            return;
        }
        if (dist > range + 0.4) {
            if (--repath <= 0) {
                repath = 6;
                getNavigation().moveTo(t, dist > 8 ? 1.25 : 1.1);
            }
        } else {
            // in reach but waiting for the next swing: circle the target
            getNavigation().stop();
            getMoveControl().strafe(-0.1f, 0.3f * strafeDir);
            faceTowards(t.position());
        }
    }

    /** Fall back toward the core / a Sanctum, still facing the enemy. */
    protected void fallBack(ServerLevel level) {
        BlockPos core = core(level);
        Player owner = ownerHere();
        Vec3 safe = core != null ? Vec3.atBottomCenterOf(core) : owner != null ? owner.position() : postCentre();
        for (LivingEntity a : SiegeDamage.defendersAround(level, position(), 24)) {
            if (a instanceof TurretEntity t && t.kind == com.lastbastion.entity.turret.TurretKind.SANCTUM) {
                safe = t.position();
                break;
            }
        }
        if (target != null) getLookControl().setLookAt(target, 30, 30);
        if (position().distanceToSqr(safe) > 9) {
            if (--repath <= 0) {
                repath = 10;
                getNavigation().moveTo(safe.x, safe.y, safe.z, 1.3);
            }
        } else if (target != null && distanceTo(target) < attackRange() + 1) {
            engage(level, target, distanceTo(target));
        }
    }

    protected void idle(ServerLevel level) {
        BlockPos core = core(level);
        Player owner = ownerHere();
        Vec3 dest = null;
        double tolerance = 2.5;
        HeroMode m = mode();
        if (m == HeroMode.FOLLOW && owner == null) m = core != null ? HeroMode.GUARD : HeroMode.HUNT;
        switch (m) {
            case GUARD -> {
                if (core != null) {
                    double r = 4 + getBbWidth();
                    dest = Vec3.atBottomCenterOf(core).add(Math.cos(postAngle) * r, 0, Math.sin(postAngle) * r);
                } else if (guardPos != null) {
                    dest = Vec3.atBottomCenterOf(guardPos);
                    tolerance = 1.5 + getBbWidth();
                }
            }
            case FOLLOW -> {
                if (distanceToSqr(owner) > 28 * 28) {
                    Vfx.spray(level, FxStyle.SMOKE, position().add(0, 1, 0), 0xB8B0A8, 0x8A8480, 0.7f, 10, 0.12f, 0);
                    Vec3 to = Companions.safeSpotNear(level, this, owner.position());
                    teleportTo(to.x, to.y, to.z);
                    getNavigation().stop();
                    Vfx.spray(level, FxStyle.STAR, position().add(0, 1, 0), 0xFFFFFF, kind.color, 0.25f, 12, 0.2f, 0);
                }
                // walk at the owner's side rather than on top of them
                Vec3 side = Vec3.directionFromRotation(0, owner.getYRot() + (postAngle > Mth.PI ? 120 : -120)).scale(2 + getBbWidth());
                dest = owner.position().add(side);
                tolerance = 2 + getBbWidth();
            }
            case HUNT -> {
                if (core != null) {
                    double a = postAngle + tickCount * 0.004;
                    dest = Vec3.atBottomCenterOf(core).add(Math.cos(a) * 20, 0, Math.sin(a) * 20);
                    tolerance = 4;
                } else {
                    Vec3 c = owner != null ? owner.position() : postCentre();
                    double a = postAngle + tickCount * 0.006;
                    dest = c.add(Math.cos(a) * 9, 0, Math.sin(a) * 9);
                    tolerance = 3;
                }
            }
        }
        if (dest == null) return;
        if (position().distanceToSqr(dest) > tolerance * tolerance) {
            if (--repath <= 0) {
                repath = 20;
                double speed = m == HeroMode.FOLLOW && position().distanceToSqr(dest) > 100 ? 1.3 : 1.0;
                getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            }
        } else {
            getNavigation().stop();
            if (tickCount % 1200 == 600) speak(Lines.IDLE, false);
        }
    }

    protected void faceTowards(Vec3 at) {
        double dx = at.x - getX(), dz = at.z - getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        setYRot(Mth.rotLerp(0.5f, getYRot(), yaw));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    protected void faceNow(Vec3 at) {
        double dx = at.x - getX(), dz = at.z - getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    protected Vec3 forward(double d) {
        return position().add(Vec3.directionFromRotation(0, getYRot()).scale(d));
    }

    protected Vec3 look() {
        return Vec3.directionFromRotation(0, getYRot());
    }

    // ------------------------------------------------------------------ situational helpers

    protected List<LivingEntity> enemiesNear(ServerLevel level, double r) {
        return SiegeDamage.enemiesAround(level, position(), r);
    }

    /** Number of living enemies within {@code r} of {@code at}. */
    protected int crowd(ServerLevel level, Vec3 at, double r) {
        return SiegeDamage.enemiesAround(level, at, r).size();
    }

    /** The position (an enemy's) that has the most other enemies within {@code r}, searching {@code range} around us. */
    @Nullable
    protected Vec3 bestCluster(ServerLevel level, double range, double r, int min) {
        Vec3 best = null;
        int bestN = min - 1;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), range)) {
            int n = crowd(level, e.position(), r);
            if (n > bestN) {
                bestN = n;
                best = e.position();
            }
        }
        return best;
    }

    /** Number of enemies within 4 blocks that are attacking this hero. */
    protected int attackers(ServerLevel level) {
        int n = 0;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), 4)) {
            if (e instanceof Mob mob && mob.getTarget() == this) n++;
        }
        return n;
    }

    /** First ally (not a turret) within {@code r} below {@code frac} health, or null. */
    @Nullable
    protected LivingEntity hurtAlly(ServerLevel level, double r, float frac) {
        for (LivingEntity a : SiegeDamage.defendersAround(level, position(), r)) {
            if (a instanceof TurretEntity) continue;
            if (a.getHealth() < a.getMaxHealth() * frac) return a;
        }
        return null;
    }

    /** Melee helper: damage everything in a frontal arc of {@code arcDeg} within {@code reach}. */
    protected int arc(ServerLevel level, double reach, double arcDeg, float damage, Element element) {
        Vec3 look = look();
        double cos = Math.cos(Math.toRadians(arcDeg / 2));
        int n = 0;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, position(), reach + 1.5)) {
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
            double d = to.length();
            if (d > reach + e.getBbWidth() * 0.5) continue;
            if (d > 0.8 && to.normalize().dot(look) < cos) continue;
            if (hit(e, damage, element)) n++;
        }
        return n;
    }

    /** Melee helper: damage everything in a small sphere in front. */
    protected int cleave(ServerLevel level, double reach, double radius, float damage, Element element) {
        Vec3 c = forward(reach).add(0, getBbHeight() * 0.5, 0);
        int n = 0;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, c, radius)) {
            if (hit(e, damage, element)) n++;
        }
        return n;
    }

    /** Hits everything along a horizontal line from here, width {@code w}. */
    protected int line(ServerLevel level, Vec3 from, Vec3 dir, double length, double w, float damage, Element element) {
        Vec3 n = dir.multiply(1, 0, 1).normalize();
        int count = 0;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, from.add(n.scale(length / 2)), length / 2 + w)) {
            Vec3 rel = e.position().subtract(from).multiply(1, 0, 1);
            double along = rel.dot(n);
            if (along < -0.5 || along > length) continue;
            if (rel.subtract(n.scale(along)).lengthSqr() > (w + e.getBbWidth() / 2) * (w + e.getBbWidth() / 2)) continue;
            if (hit(e, damage, element)) count++;
        }
        return count;
    }

    // ------------------------------------------------------------------ hero-specific hooks

    public abstract Role role();

    protected abstract void basicAttack(ServerLevel level, LivingEntity t, int combo);

    protected abstract int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist);

    /** @return true when finished. Skill id {@link #ULT} is the ultimate. */
    protected abstract boolean tickSkill(ServerLevel level, int skill, int t);

    /** Is now a good moment for the ultimate? */
    protected boolean wantsUltimate(ServerLevel level, @Nullable LivingEntity t, double dist) {
        if (t == null) return false;
        return crowd(level, t.position(), 5) >= 4 || (t instanceof SiegeEnemy se && se.isHighPriority() && dist < 12);
    }

    protected void passiveTick(ServerLevel level) {
    }

    protected float retreatThreshold() {
        return role() == Role.BRUISER ? 0.18f : 0.28f;
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

    protected int attackRecover() {
        return 5;
    }

    protected int attackInterval() {
        return 20;
    }

    /** How many ticks of skill and attack cooldown recover per tick (devils fight at double tempo). */
    protected int cooldownRate() {
        return 1;
    }

    protected int comboLength() {
        return 1;
    }

    // ------------------------------------------------------------------ lifecycle

    public void onWaveCleared() {
        // never "heal" a hero in its death throes back to life: its sigil is already on the way home
        if (!isAlive()) return;
        setHealth(getMaxHealth());
        retreating = false;
        if (level() instanceof ServerLevel sl) Vfx.spray(sl, FxStyle.STAR, position().add(0, getBbHeight(), 0), 0xFFFFFF, 0x9CFFB0, 0.25f, 10, 0.15f, 1);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && !p.isCreative()) return false;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return false;
        boolean low = getHealth() > getMaxHealth() * 0.3f;
        boolean r = super.hurt(source, amount);
        if (r) {
            lastDamaged = tickCount;
            lastCombat = tickCount;
            if (source.getEntity() instanceof LivingEntity att && att != this && !SiegeDamage.isEnemy(att)) command(att, 200);
            gainUlt(Math.min(amount, 20) * 0.2f);
            if (low && getHealth() <= getMaxHealth() * 0.3f) speak(Lines.LOW_HP, false);
        }
        return r;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl && getOwnerId() != null) {
            Companions.returnSigil(sl, this, true);
            if (owner() instanceof ServerPlayer sp) {
                int secs = Companions.recoveryTicks(kind) / 20;
                sp.displayClientMessage(Component.literal(kind.title + " has fallen! Their " + (kind.devil ? "pact" : "sigil") + " returns to you and recovers in "
                        + secs / 60 + ":" + String.format("%02d", secs % 60) + " (rush it at the Siege Core).").withStyle(ChatFormatting.RED), false);
            }
            Vfx.pillar(sl, position(), kind.color, getBbWidth() * 0.6f, 15, 30);
            Vfx.spray(sl, FxStyle.SOUL, position().add(0, getBbHeight() * 0.5, 0), 0xFFFFFF, kind.color, 0.4f, 16, 0.15f, 1);
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
        tag.putFloat("lbUlt", ultEnergy());
        if (guardPos != null) tag.put("lbGuard", net.minecraft.nbt.NbtUtils.writeBlockPos(guardPos));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("lbOwner")) setOwnerId(tag.getUUID("lbOwner"));
        entityData.set(MODE, tag.getInt("lbMode"));
        setLevelAndXp(Math.max(1, tag.getInt("lbLevel")), tag.getInt("lbXp"));
        entityData.set(ULT_ENERGY, tag.getFloat("lbUlt"));
        if (tag.contains("lbGuard")) guardPos = net.minecraft.nbt.NbtUtils.readBlockPos(tag.getCompound("lbGuard"));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof Player || other instanceof SiegeAlly || super.isAlliedTo(other);
    }
}
