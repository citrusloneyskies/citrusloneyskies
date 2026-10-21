package com.tokyoghoul.registry;

import com.tokyoghoul.TokyoGhoul;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TGEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TokyoGhoul.MODID);

    /** RC suppressant: no kagune moves, no ghoul regeneration. */
    public static final RegistryObject<MobEffect> SUPPRESSED = EFFECTS.register("rc_suppressed",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x4A6A8A) {
            });

    private TGEffects() {
    }
}
