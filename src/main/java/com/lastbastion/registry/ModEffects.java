package com.lastbastion.registry;

import com.lastbastion.LastBastion;
import com.lastbastion.effect.SiegeEffect;
import com.lastbastion.effect.SiegeEffect.Kind;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LastBastion.MODID);

    public static final RegistryObject<MobEffect> BLEEDING = reg("bleeding", Kind.BLEEDING, MobEffectCategory.HARMFUL, 0x9B0E0E);
    public static final RegistryObject<MobEffect> SCORCHED = reg("scorched", Kind.SCORCHED, MobEffectCategory.HARMFUL, 0xFF6A00);
    public static final RegistryObject<MobEffect> CHILLED = reg("chilled", Kind.CHILLED, MobEffectCategory.HARMFUL, 0x9FE6FF);
    public static final RegistryObject<MobEffect> FROZEN = reg("frozen", Kind.FROZEN, MobEffectCategory.HARMFUL, 0xD8F8FF);
    public static final RegistryObject<MobEffect> SOAKED = reg("soaked", Kind.SOAKED, MobEffectCategory.HARMFUL, 0x2F6BFF);
    public static final RegistryObject<MobEffect> SHOCKED = reg("shocked", Kind.SHOCKED, MobEffectCategory.HARMFUL, 0xF4F06A);
    public static final RegistryObject<MobEffect> CORRODED = reg("corroded", Kind.CORRODED, MobEffectCategory.HARMFUL, 0x7BD940);
    public static final RegistryObject<MobEffect> CONCUSSED = reg("concussed", Kind.CONCUSSED, MobEffectCategory.HARMFUL, 0xB08A5A);
    public static final RegistryObject<MobEffect> MARKED = reg("marked", Kind.MARKED, MobEffectCategory.HARMFUL, 0xE04AE0);
    public static final RegistryObject<MobEffect> DISORIENTED = reg("disoriented", Kind.DISORIENTED, MobEffectCategory.HARMFUL, 0xCFFFF0);
    public static final RegistryObject<MobEffect> PURIFIED = reg("purified", Kind.PURIFIED, MobEffectCategory.HARMFUL, 0xFFF2B0);
    public static final RegistryObject<MobEffect> JAMMED = reg("jammed", Kind.JAMMED, MobEffectCategory.HARMFUL, 0x5A5A5A);
    public static final RegistryObject<MobEffect> FEARED = reg("feared", Kind.FEARED, MobEffectCategory.HARMFUL, 0x3A1A4A);
    public static final RegistryObject<MobEffect> AEGIS = reg("aegis", Kind.AEGIS, MobEffectCategory.BENEFICIAL, 0xFFD966);
    public static final RegistryObject<MobEffect> RALLY = reg("rally", Kind.RALLY, MobEffectCategory.BENEFICIAL, 0xFF4040);

    private static RegistryObject<MobEffect> reg(String name, Kind kind, MobEffectCategory cat, int color) {
        return EFFECTS.register(name, () -> new SiegeEffect(kind, cat, color));
    }

    private ModEffects() {
    }
}
