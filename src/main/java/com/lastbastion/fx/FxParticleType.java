package com.lastbastion.fx;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public class FxParticleType extends ParticleType<FxParticleOptions> {
    @SuppressWarnings("deprecation")
    public FxParticleType() {
        super(true, FxParticleOptions.DESERIALIZER);
    }

    @Override
    public Codec<FxParticleOptions> codec() {
        return FxParticleOptions.CODEC;
    }
}
