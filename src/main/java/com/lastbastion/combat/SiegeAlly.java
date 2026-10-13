package com.lastbastion.combat;

import javax.annotation.Nullable;
import java.util.UUID;

/** Turrets, heroes, drones and exosuits that defend the core. */
public interface SiegeAlly {
    /** The player who bought/summoned this ally; credits for its kills go to them. */
    @Nullable
    UUID getOwnerId();
}
