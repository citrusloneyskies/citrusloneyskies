package com.lastbastion.block;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Siege barricades. Zombies must breach them; each tier takes much longer to break, and when a
 * reinforced tier is breached it degrades into the tier below instead of vanishing. Runic wards
 * also discharge into anything trying to break them.
 */
public class BarricadeBlock extends Block {
    public enum Tier {
        WOOD(3f), STONE(8f), IRON(18f), RUNIC(35f);
        public final float breach;

        Tier(float breach) {
            this.breach = breach;
        }
    }

    public final Tier tier;
    @Nullable
    private final Supplier<Block> degradesTo;

    public BarricadeBlock(Properties p, Tier tier, @Nullable Supplier<Block> degradesTo) {
        super(p);
        this.tier = tier;
        this.degradesTo = degradesTo;
    }

    public float breachHardness() {
        return tier.breach;
    }

    public void onBreached(ServerLevel level, BlockPos pos, BlockState state, LivingEntity breaker) {
        level.levelEvent(2001, pos, Block.getId(state));
        if (degradesTo != null) {
            level.setBlock(pos, degradesTo.get().defaultBlockState(), 3);
        } else {
            level.removeBlock(pos, false);
        }
    }

    /** Called periodically while a zombie hacks at this block. */
    public void onBreachTick(ServerLevel level, BlockPos pos, LivingEntity breaker) {
        if (tier == Tier.RUNIC) {
            Vec3 c = Vec3.atCenterOf(pos);
            Fx.bolt(level, c, breaker.position().add(0, breaker.getBbHeight() * 0.5, 0), 0x9F7BFF, 0.8f);
            SiegeDamage.deal(breaker, null, null, 6f, Element.ARCANE, false);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 0.6f);
        } else if (tier == Tier.IRON) {
            level.sendParticles(ParticleTypes.CRIT, breaker.getX(), breaker.getY() + 1, breaker.getZ(), 3, 0.2, 0.2, 0.2, 0.1);
            SiegeDamage.deal(breaker, null, null, 1.5f, Element.PHYSICAL, false);
        }
    }
}
