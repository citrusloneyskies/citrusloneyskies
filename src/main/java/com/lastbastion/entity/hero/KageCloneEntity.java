package com.lastbastion.entity.hero;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.entity.zombie.RunnerZombie;
import com.lastbastion.entity.zombie.SiegeZombie;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Short-lived shadow duplicate of Kage. Fights like him (basic slashes only) for 10 seconds. */
public class KageCloneEntity extends KageHero {
    public KageCloneEntity(EntityType<? extends HeroEntity> type, Level level) {
        super(type, level, HeroKind.KAGE);
    }

    @Override
    protected int pickSkill(ServerLevel level, @Nullable LivingEntity t, double dist) {
        return 0;
    }

    @Override
    protected void passiveTick(ServerLevel level) {
        if (tickCount > 200) {
            level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.05);
            discard();
        }
    }

    @Override
    public float dmg(float mult) {
        return super.dmg(mult) * 0.5f;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
