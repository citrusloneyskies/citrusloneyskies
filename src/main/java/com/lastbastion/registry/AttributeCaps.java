package com.lastbastion.registry;

import com.lastbastion.LastBastion;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Vanilla clamps max health at 1024 and armour at 30, which silently capped every boss, every devil and the
 * strongest heroes. This raises those caps once at startup (on both sides, so client health bars agree). If
 * another mod already raised them further, nothing changes; if reflection is refused, the game runs on with the
 * vanilla caps and a warning in the log.
 */
public final class AttributeCaps {
    private AttributeCaps() {
    }

    public static void raise() {
        raise(Attributes.MAX_HEALTH, 1_000_000);
        raise(Attributes.ARMOR, 1024);
        raise(Attributes.ARMOR_TOUGHNESS, 1024);
    }

    private static void raise(Attribute a, double max) {
        if (!(a instanceof RangedAttribute r) || r.getMaxValue() >= max) return;
        double old = r.getMaxValue();
        try {
            // RangedAttribute holds two double fields (min and max); the max is the one equal to getMaxValue()
            for (Field f : RangedAttribute.class.getDeclaredFields()) {
                if (f.getType() != double.class || Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                if (f.getDouble(r) == old) {
                    f.setDouble(r, max);
                    if (r.getMaxValue() == max) {
                        LastBastion.LOGGER.info("Raised the cap of {} from {} to {}", a.getDescriptionId(), old, max);
                        return;
                    }
                    f.setDouble(r, old);
                }
            }
            LastBastion.LOGGER.warn("Could not raise the cap of {}: field not found", a.getDescriptionId());
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LastBastion.LOGGER.warn("Could not raise the cap of {}: {}", a.getDescriptionId(), ex.toString());
        }
    }
}
