package com.lastbastion.registry;

import com.lastbastion.LastBastion;
import com.lastbastion.fx.FxParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LastBastion.MODID);

    public static final RegistryObject<FxParticleType> FX = PARTICLES.register("fx", FxParticleType::new);

    private ModParticles() {
    }
}
