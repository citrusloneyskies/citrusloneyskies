package com.lastbastion.entity.hero;

import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.fx.FxParticleOptions;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Base for the five Devils: hero AI with a devil's constitution. Devils never retreat, shrug off fire and every
 * harmful effect, rise through a summoning circle of hellfire, and on death sink back into the abyss (their pact
 * returns to the summoner and needs ten minutes to recover). Each devil adds a passive aura and a full moveset.
 */
public abstract class DevilEntity extends HeroEntity {
    protected DevilEntity(EntityType<? extends HeroEntity> type, Level level, HeroKind kind) {
        super(type, level, kind);
        setMaxUpStep(1.8f);
    }

    public static AttributeSupplier.Builder createDevilAttributes(HeroKind k) {
        return HeroEntity.createAttributes(k).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR_TOUGHNESS, k.armor / 2);
    }

    /** True for bosses, warlords, champions and rifts: they resist displacement and crowd control. */
    protected static boolean big(LivingEntity e) {
        return e instanceof SiegeEnemy se && se.isHighPriority();
    }

    // ------------------------------------------------------------------ constitution

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(effect);
    }

    @Override
    protected float retreatThreshold() {
        return 0f;
    }

    @Override
    protected boolean canDodge() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    @Override
    protected float ultPassive() {
        return 0.3f;
    }

    @Override
    protected float ultGainPerDamage() {
        return 0.03f;
    }

    // ------------------------------------------------------------------ arrival / departure

    /** Colour of this devil's hellfire. */
    protected abstract int fireColor();

    /** Second colour (core of flames, runes). */
    protected abstract int fireCore();

    @Override
    public void onSummoned(ServerLevel level) {
        super.onSummoned(level);
        Vec3 p = position();
        Vfx.sigil(level, p.add(0, 0.04, 0), fireColor(), 3.6f + getBbWidth(), 60, 2.5f);
        Vfx.sigil(level, p.add(0, 0.06, 0), fireCore(), 2.2f + getBbWidth() * 0.5f, 60, -4f);
        Vfx.pillar(level, p, fireColor(), 1.2f + getBbWidth() * 0.4f, 24, 40);
        Vfx.jet(level, FxStyle.FLAME, p.add(0, 0.1, 0), p.add(0, 5, 0), fireCore(), fireColor(), 0.5f, 40, 25);
        Vfx.spray(level, FxStyle.EMBER, p.add(0, 1, 0), fireCore(), fireColor(), 0.2f, 40, 0.4f, 0.9f);
        Vfx.screenFlash(level, p, fireColor(), 0.4f, 48, 14);
        Vfx.shake(level, p, 0.9f, 40, 24);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.6f, 0.6f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2f, 0.5f);
        arrival(level);
    }

    /** The devil's own entrance on top of the shared summoning circle. */
    protected void arrival(ServerLevel level) {
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            Vec3 p = position();
            Vfx.sigil(sl, p.add(0, 0.04, 0), fireColor(), 3f + getBbWidth(), 40, -3f);
            Vfx.vortex(sl, p, fireColor(), fireCore(), getBbWidth() + 0.8f, getBbHeight() + 1, 40, null);
            Vfx.spray(sl, FxStyle.SMOKE, p.add(0, 1, 0), 0x2A2020, 0x0A0606, 1.2f, 30, 0.3f, 0.3f);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1f, 0.7f);
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ ambience

    @Override
    protected void passiveTick(ServerLevel level) {
        if (tickCount % 6 == 0) {
            Vfx.particles(level, FxParticleOptions.of(FxStyle.EMBER, fireColor(), 0.12f, 20), position().add(0, 0.2, 0), 2,
                    getBbWidth() * 0.5, 0.1, getBbWidth() * 0.5, 0.02);
        }
        if (tickCount % 40 == 0 && inCombat()) {
            Vfx.particles(level, FxParticleOptions.of(FxStyle.SMOKE, 0x2A2222, 0.6f, 30), position().add(0, 0.1, 0), 3,
                    getBbWidth() * 0.6, 0.05, getBbWidth() * 0.6, 0.01);
        }
        aura(level);
    }

    /** Per-devil passive aura, called every tick. */
    protected void aura(ServerLevel level) {
    }

    /** Heal on kills: devils feed on the dying. */
    protected void feed(float fraction) {
        heal(getMaxHealth() * fraction);
    }

    /** Hit and report whether the target died (for kill-triggered passives). */
    protected boolean hitKills(LivingEntity e, float amount, com.lastbastion.combat.Element element) {
        boolean ok = hit(e, amount, element);
        return ok && !e.isAlive();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.6f, 0.7f);
    }

    /** All enemies within {@code r} of a point, nearest first. */
    protected java.util.List<LivingEntity> enemiesAt(ServerLevel level, Vec3 at, double r) {
        java.util.List<LivingEntity> l = SiegeDamage.enemiesAround(level, at, r);
        l.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        return l;
    }
}
