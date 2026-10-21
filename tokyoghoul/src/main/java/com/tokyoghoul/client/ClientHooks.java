package com.tokyoghoul.client;

import com.tokyoghoul.client.screen.SelectScreen;
import net.minecraft.client.Minecraft;

/** Entry points called from common code through DistExecutor. */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openSelectScreen() {
        Minecraft.getInstance().setScreen(new SelectScreen());
    }
}
