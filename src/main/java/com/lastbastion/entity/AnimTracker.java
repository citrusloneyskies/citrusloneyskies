package com.lastbastion.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;

/**
 * Synchronises one-shot animations (attacks, skills) from server to client. The server bumps a
 * sequence counter packed with the animation id; clients note the local tick the change arrived and
 * models read {@link #time(float)} to drive keyframe curves.
 */
public final class AnimTracker {
    private final Entity entity;
    private final EntityDataAccessor<Integer> key;
    private int start = -1000;

    public AnimTracker(Entity entity, EntityDataAccessor<Integer> key) {
        this.entity = entity;
        this.key = key;
    }

    public void play(int id) {
        int seq = (entity.getEntityData().get(key) >>> 8) + 1;
        entity.getEntityData().set(key, ((seq & 0xFFFFFF) << 8) | (id & 0xFF));
        start = entity.tickCount;
    }

    public void clear() {
        play(0);
    }

    public int id() {
        return entity.getEntityData().get(key) & 0xFF;
    }

    /** Ticks since the current animation started. */
    public float time(float partialTick) {
        return entity.tickCount - start + partialTick;
    }

    public int ticks() {
        return entity.tickCount - start;
    }

    public boolean is(int id) {
        return id() == id;
    }

    /** Call from {@code onSyncedDataUpdated}. */
    public void onSync(EntityDataAccessor<?> changed) {
        if (key.equals(changed) && entity.level().isClientSide) start = entity.tickCount;
    }
}
