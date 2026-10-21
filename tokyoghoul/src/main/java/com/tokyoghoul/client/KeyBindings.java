package com.tokyoghoul.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tokyoghoul.network.Packets;
import com.tokyoghoul.network.Packets.Action;
import com.tokyoghoul.network.TGNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Jujutsu Craft's key layout, kept so JJK players feel at home: R cycles techniques (shift for reverse),
 * Z uses the selected one (hold for barrages and breath), X resets the selection. Tokyo Ghoul adds
 * C (kagune out/in), G (guard), B (dodge) and M (Rc regeneration, JJK's reverse cursed technique).
 */
public final class KeyBindings {
    private static final String CAT = "key.categories.tokyoghoul";
    public static final KeyMapping CYCLE = new KeyMapping("key.tokyoghoul.cycle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CAT);
    public static final KeyMapping USE = new KeyMapping("key.tokyoghoul.use", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CAT);
    public static final KeyMapping RESET = new KeyMapping("key.tokyoghoul.reset", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CAT);
    public static final KeyMapping KAGUNE = new KeyMapping("key.tokyoghoul.kagune", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CAT);
    public static final KeyMapping GUARD = new KeyMapping("key.tokyoghoul.guard", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CAT);
    public static final KeyMapping DODGE = new KeyMapping("key.tokyoghoul.dodge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CAT);
    public static final KeyMapping REGEN = new KeyMapping("key.tokyoghoul.regen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CAT);
    private static boolean useDown, guardDown, regenDown;

    private KeyBindings() {
    }

    public static void register(RegisterKeyMappingsEvent e) {
        e.register(CYCLE);
        e.register(USE);
        e.register(RESET);
        e.register(KAGUNE);
        e.register(GUARD);
        e.register(DODGE);
        e.register(REGEN);
    }

    private static void send(Action a) {
        TGNet.CHANNEL.sendToServer(new Packets.Key(a));
    }

    /** Polled every client tick. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) {
            release();
            return;
        }
        while (CYCLE.consumeClick()) send(mc.player.isShiftKeyDown() ? Action.CYCLE_PREV : Action.CYCLE_NEXT);
        while (RESET.consumeClick()) send(Action.RESET);
        while (KAGUNE.consumeClick()) send(Action.TOGGLE_KAGUNE);
        while (DODGE.consumeClick()) send(Action.DODGE);
        boolean use = USE.isDown();
        if (use != useDown) {
            useDown = use;
            send(use ? Action.USE_PRESS : Action.USE_RELEASE);
        }
        while (USE.consumeClick()) {
            // already handled through isDown edges
        }
        boolean guard = GUARD.isDown();
        if (guard != guardDown) {
            guardDown = guard;
            send(guard ? Action.GUARD_PRESS : Action.GUARD_RELEASE);
        }
        boolean regen = REGEN.isDown();
        if (regen != regenDown) {
            regenDown = regen;
            send(regen ? Action.REGEN_PRESS : Action.REGEN_RELEASE);
        }
    }

    private static void release() {
        if (useDown) send(Action.USE_RELEASE);
        if (guardDown) send(Action.GUARD_RELEASE);
        if (regenDown) send(Action.REGEN_RELEASE);
        useDown = guardDown = regenDown = false;
    }
}
