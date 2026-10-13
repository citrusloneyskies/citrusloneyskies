package com.lastbastion.effect;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.registry.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.joml.Vector3f;

/**
 * All of Last Bastion's status effects. Ticking behaviour lives here; damage amplification
 * (Soaked, Marked, Corroded, Purified, Frozen shatter) is resolved in {@link SiegeDamage}.
 */
public class SiegeEffect extends MobEffect {
    public enum Kind {
        BLEEDING, SCORCHED, CHILLED, FROZEN, SOAKED, SHOCKED, CORRODED, CONCUSSED,
        MARKED, DISORIENTED, PURIFIED, JAMMED, FEARED, AEGIS, RALLY
    }

    public final Kind kind;

    public SiegeEffect(Kind kind, MobEffectCategory category, int color) {
        super(category, color);
        this.kind = kind;
        switch (kind) {
            case CHILLED -> addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    "6a1b6f1c-3a52-4a8e-8e61-6f2c1f0a1001", -0.12, AttributeModifier.Operation.MULTIPLY_TOTAL);
            case FROZEN -> addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    "6a1b6f1c-3a52-4a8e-8e61-6f2c1f0a1002", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
            case SHOCKED -> addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    "6a1b6f1c-3a52-4a8e-8e61-6f2c1f0a1003", -0.85, AttributeModifier.Operation.MULTIPLY_TOTAL);
            case CORRODED -> addAttributeModifier(Attributes.ARMOR,
                    "6a1b6f1c-3a52-4a8e-8e61-6f2c1f0a1004", -2.0, AttributeModifier.Operation.ADDITION);
            case CONCUSSED -> addAttributeModifier(Attributes.ATTACK_DAMAGE,
                    "6a1b6f1c-3a52-4a8e-8e61-6f2c1f0a1005", -0.25, AttributeModifier.Operation.MULTIPLY_TOTAL);
            case RALLY -> {
                addAttributeModifier(Attributes.MOVEMENT_SPEED,
                        "6a1b6f1c-3a52-4a8e-8e61-6f2c1f0a1006", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
                addAttributeModifier(Attributes.ATTACK_DAMAGE,
                        "6a1b6f1c-3a52-4a8e-8e61-6f2c1f0a1007", 0.2, AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            default -> {
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return switch (kind) {
            case BLEEDING, SCORCHED -> duration % 20 == 0;
            case CHILLED, FROZEN, SOAKED, SHOCKED, CORRODED, MARKED, PURIFIED, DISORIENTED, FEARED, AEGIS -> duration % 5 == 0;
            default -> false;
        };
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        double x = entity.getX(), y = entity.getY() + entity.getBbHeight() * 0.5, z = entity.getZ();
        double w = entity.getBbWidth() * 0.5;
        switch (kind) {
            case BLEEDING -> {
                SiegeDamage.dot(entity, 0.6f * (amplifier + 1), Element.PHYSICAL);
                level.sendParticles(dust(0x9B0E0E, 1.0f), x, y, z, 4 + amplifier * 2, w, 0.3, w, 0.0);
            }
            case SCORCHED -> {
                SiegeDamage.dot(entity, 1.0f * (amplifier + 1), Element.FIRE);
                level.sendParticles(amplifier >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                        x, y, z, 5, w, 0.4, w, 0.01);
            }
            case CHILLED -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1 + amplifier, w, 0.4, w, 0.0);
                if (amplifier >= 4) {
                    entity.removeEffect(this);
                    entity.addEffect(new MobEffectInstance(ModEffects.FROZEN.get(), 50, 0));
                }
            }
            case FROZEN -> {
                level.sendParticles(dust(0xBFF4FF, 1.6f), x, y, z, 4, w, 0.5, w, 0.0);
                entity.setDeltaMovement(0, Math.min(0, entity.getDeltaMovement().y), 0);
                entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), 140));
            }
            case SOAKED -> {
                if (entity.isOnFire()) entity.clearFire();
                level.sendParticles(ParticleTypes.FALLING_WATER, x, y + 0.4, z, 2, w, 0.3, w, 0.0);
            }
            case SHOCKED -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 3, w, 0.5, w, 0.05);
            case CORRODED -> level.sendParticles(dust(0x8BE04A, 0.9f), x, y, z, 2, w, 0.5, w, 0.0);
            case MARKED -> level.sendParticles(dust(0xE04AE0, 1.2f), x, entity.getY() + entity.getBbHeight() + 0.4, z, 1, 0.1, 0.05, 0.1, 0.0);
            case PURIFIED -> {
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, w, 0.5, w, 0.01);
                entity.getActiveEffects().stream()
                        .filter(e -> e.getEffect().isBeneficial())
                        .map(MobEffectInstance::getEffect).toList()
                        .forEach(entity::removeEffect);
            }
            case DISORIENTED -> {
                level.sendParticles(ParticleTypes.CRIT, x, entity.getY() + entity.getBbHeight() + 0.2, z, 2, 0.2, 0.05, 0.2, 0.0);
                if (entity.getRandom().nextInt(3) == 0) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(
                            (entity.getRandom().nextDouble() - 0.5) * 0.25, 0, (entity.getRandom().nextDouble() - 0.5) * 0.25));
                }
            }
            case FEARED -> level.sendParticles(ParticleTypes.SMOKE, x, entity.getY() + entity.getBbHeight(), z, 1, 0.2, 0.1, 0.2, 0.0);
            case AEGIS -> level.sendParticles(dust(0xFFE38A, 0.8f), x, y, z, 1, w + 0.3, 0.6, w + 0.3, 0.0);
            default -> {
            }
        }
    }

    public static ParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), scale);
    }
}
