package com.lastbastion.client;

import com.lastbastion.network.HudSyncPacket;
import com.lastbastion.siege.NightModifier;
import com.lastbastion.siege.SiegeState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Client-side mirror of the siege state for the HUD, screens, fog and the core renderer. */
public final class ClientSiegeState {
    public static HudSyncPacket hud;
    public static final List<Popup> POPUPS = new ArrayList<>();
    private static int lastCredits = -1;
    public static int creditFlash;
    public static float displayedCoreHp = -1;

    public static final class Popup {
        public final Vec3 pos;
        public final int amount;
        public int age;

        Popup(Vec3 pos, int amount) {
            this.pos = pos;
            this.amount = amount;
        }
    }

    private ClientSiegeState() {
    }

    public static void update(HudSyncPacket p) {
        if (lastCredits >= 0 && p.credits() > lastCredits) creditFlash = 20;
        lastCredits = p.credits();
        hud = p;
        if (displayedCoreHp < 0) displayedCoreHp = p.coreHp();
    }

    public static final class Speech {
        public final int entity;
        public final String text;
        public final int color;
        public int age;

        Speech(int entity, String text, int color) {
            this.entity = entity;
            this.text = text;
            this.color = color;
        }
    }

    public static final List<Speech> SPEECH = new ArrayList<>();

    public static void speech(int entity, String text, int color) {
        SPEECH.removeIf(s -> s.entity == entity);
        if (SPEECH.size() > 12) SPEECH.remove(0);
        SPEECH.add(new Speech(entity, text, color));
    }

    public static void popup(Vec3 pos, int amount) {
        if (POPUPS.size() > 60) POPUPS.remove(0);
        POPUPS.add(new Popup(pos, amount));
    }

    public static void tick() {
        Iterator<Popup> it = POPUPS.iterator();
        while (it.hasNext()) {
            Popup p = it.next();
            if (++p.age > 30) it.remove();
        }
        SPEECH.removeIf(sp -> ++sp.age > 70);
        if (creditFlash > 0) creditFlash--;
        if (hud != null) displayedCoreHp += (hud.coreHp() - displayedCoreHp) * 0.15f;
    }

    public static SiegeState state() {
        return hud == null ? SiegeState.INACTIVE : SiegeState.byId(hud.state());
    }

    public static boolean active() {
        return state() != SiegeState.INACTIVE;
    }

    public static NightModifier modifier() {
        return hud == null ? NightModifier.NONE : NightModifier.byId(hud.modifier());
    }

    public static int credits() {
        return hud == null ? 0 : hud.credits();
    }

    public static float coreFraction() {
        return hud == null || hud.coreMax() <= 0 ? 1f : Math.max(0, Math.min(1, hud.coreHp() / hud.coreMax()));
    }

    public static float shieldFraction() {
        return hud == null || hud.shieldMax() <= 0 ? 0f : Math.max(0, Math.min(1, hud.shield() / hud.shieldMax()));
    }

    public static BlockPos corePos() {
        return hud == null ? BlockPos.ZERO : hud.corePos();
    }

    public static int[] upgrades() {
        return hud == null ? new int[16] : hud.upgrades();
    }

    /** Seconds until each Core Protocol is ready (0 = ready). */
    public static int abilityCooldown(int i) {
        return hud == null || hud.abilityCd() == null || i >= hud.abilityCd().length ? 0 : hud.abilityCd()[i];
    }

    public static boolean aegis() {
        return hud != null && hud.aegis() > 0;
    }

    public static int wave() {
        return hud == null ? 1 : hud.wave();
    }
}
