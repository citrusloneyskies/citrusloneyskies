package com.tokyoghoul.player;

import com.tokyoghoul.TokyoGhoul;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;

/** Attaches {@link TGPlayerData} to every player and carries it over death and dimension changes. */
public final class PlayerCaps {
    public static final Capability<TGPlayerData> DATA = CapabilityManager.get(new CapabilityToken<>() {
    });
    private static final ResourceLocation KEY = new ResourceLocation(TokyoGhoul.MODID, "player_data");
    /** Fallback so client code never sees null before the capability syncs. */
    private static final TGPlayerData EMPTY = new TGPlayerData();

    private PlayerCaps() {
    }

    public static TGPlayerData get(Player p) {
        return p.getCapability(DATA).orElse(EMPTY);
    }

    @Nullable
    public static TGPlayerData getOrNull(Entity e) {
        if (!(e instanceof Player p)) return null;
        return p.getCapability(DATA).resolve().orElse(null);
    }

    public static void register(RegisterCapabilitiesEvent e) {
        e.register(TGPlayerData.class);
    }

    public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof Player) e.addCapability(KEY, new Provider());
    }

    public static void clone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        TGPlayerData old = getOrNull(e.getOriginal());
        TGPlayerData now = getOrNull(e.getEntity());
        if (old != null && now != null) {
            now.copyFrom(old);
            if (e.isWasDeath()) now.rc = now.maxRc() * 0.5;
        }
        e.getOriginal().invalidateCaps();
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final TGPlayerData data = new TGPlayerData();
        private final LazyOptional<TGPlayerData> opt = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == DATA ? opt.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }
    }
}
