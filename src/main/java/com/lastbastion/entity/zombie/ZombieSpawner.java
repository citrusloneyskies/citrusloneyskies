package com.lastbastion.entity.zombie;

import com.lastbastion.siege.SiegeManager;
import com.lastbastion.siege.WavePlanner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class ZombieSpawner {
    private ZombieSpawner() {
    }

    @Nullable
    public static SiegeZombie spawn(ServerLevel level, ZombieKind kind, ZombieRank rank, int affixes, Vec3 pos,
                                    boolean waveMob, boolean rise) {
        com.lastbastion.siege.SiegeData d = SiegeManager.data(level);
        return spawn(level, kind, rank, affixes, pos, waveMob, rise, WavePlanner.hpScale(d.rules, d.wave), WavePlanner.damageScale(d.rules, d.wave));
    }

    @Nullable
    public static SiegeZombie spawn(ServerLevel level, ZombieKind kind, ZombieRank rank, int affixes, Vec3 pos,
                                    boolean waveMob, boolean rise, float hpScale, float dmgScale) {
        // boss broods, warlord escorts and splitting children can't outgrow the wave: past the cap, nothing more rises
        if (waveMob && SiegeManager.atCapacity(level)) return null;
        SiegeZombie z = kind.type().get().create(level);
        if (z == null) return null;
        z.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360f, 0f);
        z.setup(rank, affixes, hpScale, dmgScale, waveMob);
        z.finalizeSpawn(level, level.getCurrentDifficultyAt(z.blockPosition()), MobSpawnType.EVENT, null, null);
        if (rise) z.beginRise();
        level.addFreshEntity(z);
        if (waveMob) SiegeManager.track(level, z);
        return z;
    }
}
