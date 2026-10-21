package com.tokyoghoul.entity;

import com.tokyoghoul.combat.Fighter;
import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.data.Faction;
import com.tokyoghoul.data.KaguneType;
import com.tokyoghoul.entity.ai.CombatBrain;
import com.tokyoghoul.entity.ai.CombatMovementGoal;
import com.tokyoghoul.item.KakuhouItem;
import com.tokyoghoul.move.MoveRunner;
import com.tokyoghoul.registry.TGEntities;
import com.tokyoghoul.registry.TGItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Every Tokyo Ghoul NPC, from civilians to the One-Eyed Owl. The character definition (looked up from
 * the entity type) decides stats, model, faction and moveset; {@link CombatBrain} picks moves like
 * Jujutsu Craft's AI procedures and {@link MoveRunner} executes them exactly as for players.
 */
public class TGCharacterEntity extends Monster implements Fighter {
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(TGCharacterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(TGCharacterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(TGCharacterEntity.class, EntityDataSerializers.INT);
    public static final int FLAG_KAGUNE = 1, FLAG_FORM = 2, FLAG_GUARD = 4, FLAG_MASK = 8;

    private final CharacterDef def;
    private final FighterState state = new FighterState();
    public final CombatBrain brain;
    @Nullable
    private ServerBossEvent bossBar;
    /** Client: tick the last animation change arrived. */
    private int animStart = -1000;

    public TGCharacterEntity(EntityType<? extends TGCharacterEntity> type, Level level) {
        super(type, level);
        CharacterDef d = TGEntities.defOf(type);
        this.def = d == null ? Characters.CIVILIAN : d;
        this.brain = new CombatBrain(this);
        this.xpReward = 5 + def.npcLevel * 8;
        setMaxUpStep(1.0f);
        applyStats();
        if (!def.faction.ghoul && def.moves().isEmpty()) {
            // civilians run from ghouls that hunt them
            goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, TGCharacterEntity.class, 12f, 1.0, 1.35,
                    e -> e instanceof TGCharacterEntity t && t.fighterFaction().hunts(def.faction)));
            goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.PanicGoal(this, 1.4));
        }
    }

    private void applyStats() {
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(def.npcHealth);
        var sp = getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.setBaseValue(def.npcSpeed);
        var ar = getAttribute(Attributes.ARMOR);
        if (ar != null) ar.setBaseValue(def.npcArmor);
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FLAGS, 0);
        entityData.define(ANIM, 0);
        entityData.define(LEVEL, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CombatMovementGoal(this));
        goalSelector.addGoal(3, new OpenDoorGoal(this, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(TGCharacterEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::hunts));
    }

    /** JJK's LogicAttackTargetStart: does this NPC attack {@code e} on sight? */
    public boolean hunts(LivingEntity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        Faction ft = Fighters.faction(e);
        return ft != null && fighterFaction().hunts(ft);
    }

    // ------------------------------------------------------------------ Fighter

    @Override
    public CharacterDef fighterDef() {
        return def;
    }

    @Override
    public FighterState fighterState() {
        return state;
    }

    @Override
    public int fighterLevel() {
        return entityData.get(LEVEL);
    }

    @Override
    public Faction fighterFaction() {
        return def.faction;
    }

    public void setFighterLevel(int lv) {
        entityData.set(LEVEL, Math.max(1, lv));
    }

    // ------------------------------------------------------------------ visuals

    public int flags() {
        return entityData.get(FLAGS);
    }

    public void updateFlags() {
        int f = 0;
        if (state.kaguneTicks > 0 || state.kaguneToggled) f |= FLAG_KAGUNE;
        if (state.transformed()) f |= FLAG_FORM;
        if (state.guardingNow()) f |= FLAG_GUARD;
        if (def.look.alwaysMasked || getTarget() != null) f |= FLAG_MASK;
        if (f != entityData.get(FLAGS)) {
            boolean formChanged = ((f ^ entityData.get(FLAGS)) & FLAG_FORM) != 0;
            entityData.set(FLAGS, f);
            if (formChanged) refreshDimensions();
        }
    }

    /** Client-only dummy entities (select screen preview) set their own look. */
    public void setPreviewFlags(int f) {
        if (level().isClientSide && f != entityData.get(FLAGS)) entityData.set(FLAGS, f);
    }

    /** Server: play a one-shot animation (sequence-tagged so repeats restart). */
    public void playAnim(int id) {
        int seq = (entityData.get(ANIM) >>> 8) + 1;
        entityData.set(ANIM, ((seq & 0xFFFFFF) << 8) | (id & 0xFF));
    }

    public int animId() {
        return entityData.get(ANIM) & 0xFF;
    }

    public float animTime(float pt) {
        return tickCount - animStart + pt;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ANIM.equals(key) && level().isClientSide) animStart = tickCount;
        if (FLAGS.equals(key)) refreshDimensions();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            MoveRunner.tick(this);
            brain.tick();
            updateFlags();
            if (bossBar != null) {
                bossBar.setProgress(getHealth() / getMaxHealth());
                if (tickCount % 20 == 0) refreshBossBar();
            }
        }
    }

    private void refreshBossBar() {
        if (bossBar == null) return;
        LivingEntity t = getTarget();
        for (ServerPlayer p : new java.util.ArrayList<>(bossBar.getPlayers())) {
            if (p.distanceToSqr(this) > 48 * 48 || !p.isAlive()) bossBar.removePlayer(p);
        }
        if (t instanceof ServerPlayer sp && sp.distanceToSqr(this) < 48 * 48) bossBar.addPlayer(sp);
        for (Player p : level().players()) {
            if (p instanceof ServerPlayer sp && sp.distanceToSqr(this) < 24 * 24 && (getTarget() != null)) bossBar.addPlayer(sp);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        if (bossBar != null) bossBar.removePlayer(p);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData r = super.finalizeSpawn(level, diff, reason, data, tag);
        int lv = def.npcLevel;
        if (!def.boss) lv = Math.max(1, lv + random.nextInt(3) - 1);
        setFighterLevel(lv);
        equip();
        if (def.boss) setPersistenceRequired();
        return r;
    }

    private void equip() {
        if (!def.quinque.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, stack(def.quinque));
        if (!def.quinque2.isEmpty()) setItemSlot(EquipmentSlot.OFFHAND, stack(def.quinque2));
        setDropChance(EquipmentSlot.MAINHAND, def.boss ? 0.12f : 0.04f);
        setDropChance(EquipmentSlot.OFFHAND, 0.05f);
    }

    private static ItemStack stack(String id) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(com.tokyoghoul.TokyoGhoul.MODID, id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean r = super.hurt(src, amount);
        if (r && !level().isClientSide && def.boss && bossBar == null && def.npcLevel >= 5) {
            bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            refreshBossBar();
        }
        return r;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (bossBar != null) bossBar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(src, looting, recentlyHit);
        if (def.hasKagune()) {
            KaguneType t = def.kagune;
            if (random.nextFloat() < (def.boss ? 1f : 0.45f + looting * 0.1f)) {
                spawnAtLocation(KakuhouItem.of(t, def.boss ? def.name : ""));
            }
        }
        if (!def.race.ghoulBody && random.nextFloat() < 0.6f) spawnAtLocation(new ItemStack(TGItems.HUMAN_FLESH.get(), 1 + random.nextInt(2)));
        if (def.race.ghoulBody && random.nextFloat() < 0.3f) spawnAtLocation(new ItemStack(TGItems.RC_CELLS.get(), 1 + random.nextInt(3)));
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return !def.boss && super.removeWhenFarAway(dist);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPreventingPlayerRest(Player p) {
        return def.faction.hunts(Fighters.faction(p) == null ? Faction.HUMAN : Fighters.faction(p));
    }

    @Override
    protected Component getTypeName() {
        return def.title();
    }

    @Override
    public Component getDisplayName() {
        Component base = super.getDisplayName();
        return def.boss ? base : Component.literal(base.getString() + " Lv." + fighterLevel());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity e) {
        // all damage comes from moves; vanilla melee is replaced by the brain's basic attacks
        return false;
    }

    @Override
    public boolean canBeLeashed(Player p) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("TGLevel", fighterLevel());
        t.putBoolean("UltUsed", brain.ultimateUsed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("TGLevel")) setFighterLevel(t.getInt("TGLevel"));
        brain.ultimateUsed = t.getBoolean("UltUsed");
        if (hasCustomName() && bossBar != null) bossBar.setName(getDisplayName());
    }

    @Override
    public float getScale() {
        float s = def.look.height;
        if ((flags() & FLAG_FORM) != 0 && def.ultimate() != null && def.ultimate().kind == com.tokyoghoul.move.MoveDef.Kind.FORM) {
            s *= def.ultimate().formScale;
        }
        return s;
    }
}
