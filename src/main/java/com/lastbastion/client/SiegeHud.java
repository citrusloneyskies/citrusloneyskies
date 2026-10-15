package com.lastbastion.client;

import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.misc.TitanExoEntity;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.siege.NightModifier;
import com.lastbastion.siege.SiegeRules;
import com.lastbastion.siege.SiegeState;
import com.lastbastion.siege.WavePlanner;
import com.lastbastion.siege.raid.RaidEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The siege HUD: the top-left war panel (wave, special night, core hull and barrier, enemies or the countdown to
 * the next wave, credits), next-wave intel while preparing, live war-event objectives with a compass arrow and
 * distance, the downed overlay, the weapon readout by the hotbar and the exosuit HUD.
 */
public final class SiegeHud implements IGuiOverlay {
    public static final SiegeHud INSTANCE = new SiegeHud();
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        int lbCrashDepth = com.lastbastion.util.Safe.depth(g.pose());
        try {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        ClientDowned.State down = ClientDowned.self();
        if (down != null) downed(g, mc.font, down, w, h, pt);
        if (mc.player.getVehicle() instanceof TitanExoEntity exo) exoHud(g, mc.font, exo, w, h);
        else weaponHud(g, mc, w, h);
        var hud = ClientSiegeState.hud;
        if (hud == null) return;
        Font f = mc.font;
        SiegeState state = ClientSiegeState.state();
        int x = 6, y = 6, pw = 176;
        if (state == SiegeState.INACTIVE) {
            if (hud.bestWave() > 0) g.drawString(f, "⛁ " + String.format("%,d", hud.credits()) + " credits", x, y, 0xFFD966, true);
            return;
        }
        boolean wave = state == SiegeState.WAVE;
        int ph = wave ? 56 : 48;
        panel(g, x - 4, y - 4, pw + 4, ph, wave ? 0xFFE04040 : 0xFF6FE3FF);
        NightModifier mod = ClientSiegeState.modifier();
        String title = "WAVE " + hud.wave() + (hud.victoryWave() > 0 ? " / " + hud.victoryWave() : "");
        g.drawString(f, title, x, y, wave ? 0xFF6060 : 0xFFFFFF, true);
        int tx = x + f.width(title) + 6;
        if (!wave) {
            g.drawString(f, "preparing", tx, y, 0x8FA8C0, false);
        } else if (mod != NightModifier.NONE) {
            pill(g, f, mod.title, tx, y - 1, mod.color);
        }
        if (hud.perfect()) g.drawString(f, "✦", x + pw - 22, y, 0x7FE8FF, true);
        if (hud.boss()) g.drawString(f, "☠", x + pw - 10, y, 0xFF3030, true);
        y += 12;
        // core hull + barrier
        float frac = Mth.clamp(ClientSiegeState.displayedCoreHp / Math.max(1, hud.coreMax()), 0, 1);
        int bw = pw - 4;
        g.fill(x, y, x + bw, y + 9, 0xFF240A0A);
        int col = frac > 0.5f ? 0xFF3FD050 : frac > 0.25f ? 0xFFE0C030 : 0xFFE03030;
        if (frac <= 0.25f && (mc.level.getGameTime() / 5) % 2 == 0) col = 0xFFFF6060;
        g.fill(x, y, x + (int) (bw * frac), y + 9, col);
        g.fill(x, y, x + (int) (bw * frac), y + 1, 0x50FFFFFF);
        float sf = ClientSiegeState.shieldFraction();
        if (sf > 0) g.fill(x, y + 6, x + (int) (bw * sf), y + 9, 0xFF6FE3FF);
        if (ClientSiegeState.aegis()) g.renderOutline(x - 1, y - 1, bw + 2, 11, 0xFF8FD8FF);
        String hp = "Core " + (int) hud.coreHp() + " / " + (int) hud.coreMax() + (hud.shield() > 0 ? "  +" + (int) hud.shield() : "");
        g.drawString(f, hp, x + bw / 2 - f.width(hp) / 2, y + 1, 0xFFFFFF, true);
        y += 13;
        if (wave) {
            int killed = Math.max(0, hud.total() - hud.remaining());
            float wf = hud.total() <= 0 ? 0 : killed / (float) hud.total();
            g.fill(x, y, x + bw, y + 5, 0xFF1A1A1A);
            g.fill(x, y, x + (int) (bw * wf), y + 5, 0xFFB050FF);
            g.drawString(f, "Undead remaining: " + hud.remaining(), x, y + 7, 0xD0D0D0, true);
            y += 19;
        } else {
            String s;
            if (hud.toWave() < 0) {
                s = "Sound the war horn to begin";
            } else {
                int secs = hud.toWave() / 20;
                String clock = secs / 60 + ":" + String.format("%02d", secs % 60);
                s = (hud.trigger() == SiegeRules.TRIGGER_TIMER ? "Next wave in " : "Nightfall in ") + clock;
            }
            g.drawString(f, s, x, y, 0xA0C8FF, true);
            y += 11;
        }
        int c = ClientSiegeState.creditFlash > 0 ? 0xFFFFA0 : 0xFFD966;
        g.drawString(f, "⛁ " + String.format("%,d", hud.credits()) + " credits", x, y, c, true);
        y += 16;
        if (!wave) y = intel(g, f, hud.intel(), x, y, pw);
        else objectives(g, mc, f, hud.objectives(), x, y, pw, pt);
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("SiegeHud.render", lbCrash);
            com.lastbastion.util.Safe.restore(g.pose(), lbCrashDepth);
        }
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h, int accent) {
        g.fill(x, y, x + w, y + h, 0xA0080A0E);
        g.fill(x, y, x + w, y + 1, accent);
        g.fill(x, y + h - 1, x + w, y + h, 0x40FFFFFF & accent);
    }

    private static void pill(GuiGraphics g, Font f, String text, int x, int y, int color) {
        int w = f.width(text) + 6;
        g.fill(x, y, x + w, y + 10, 0xFF000000 | (color & 0x3F3F3F));
        g.renderOutline(x, y, w, 10, 0xFF000000 | color);
        g.drawString(f, text, x + 3, y + 1, color, false);
    }

    /** Next-wave forecast: the special night, the head count and the roster, bosses first. */
    private static int intel(GuiGraphics g, Font f, int[] intel, int x, int y, int pw) {
        if (intel == null || intel.length < 2) return y;
        NightModifier m = NightModifier.byId(intel[0]);
        int rows = Math.min(6, (intel.length - 2) / 2);
        int ph = 14 + rows * 10 + (m != NightModifier.NONE ? 10 : 0);
        panel(g, x - 4, y - 4, pw + 4, ph, 0xFF8FA8C0);
        g.drawString(f, "INTEL  •  " + intel[1] + " undead", x, y, 0xC8D8E8, true);
        y += 11;
        if (m != NightModifier.NONE) {
            g.drawString(f, "☽ " + m.title, x, y, m.color, true);
            y += 10;
        }
        for (int i = 0; i < rows; i++) {
            int kind = intel[2 + i * 2], n = intel[3 + i * 2];
            String name;
            int color;
            if (kind >= WavePlanner.BOSS_OFFSET) {
                int b = kind - WavePlanner.BOSS_OFFSET;
                name = "BOSS: " + (b >= 0 && b < BossKind.values().length ? BossKind.values()[b].title : "?");
                color = 0xFF5050;
            } else {
                ZombieKind k = kind >= 0 && kind < ZombieKind.values().length ? ZombieKind.values()[kind] : ZombieKind.SHAMBLER;
                name = n + "× " + Component.translatable("entity.lastbastion." + k.id).getString();
                color = 0xB8C0C8;
            }
            g.drawString(f, name, x + 4, y, color, false);
            y += 10;
        }
        int more = (intel.length - 2) / 2 - rows;
        if (more > 0) g.drawString(f, "+" + more + " more types", x + pw - 4 - f.width("+" + more + " more types"), y - 10, 0x707880, false);
        return y + 6;
    }

    /** War events and downed teammates: what, how far, which way. */
    private static void objectives(GuiGraphics g, Minecraft mc, Font f, int[] obj, int x, int y, int pw, float pt) {
        if (obj == null || obj.length < RaidEvents.OBJ_STRIDE) return;
        java.util.List<int[]> rows = new java.util.ArrayList<>();
        for (int o = 0; o + RaidEvents.OBJ_STRIDE <= obj.length; o += RaidEvents.OBJ_STRIDE) {
            if (obj[o] == RaidEvents.OBJ_DOWNED && obj[o + 5] == mc.player.getId()) continue;
            rows.add(java.util.Arrays.copyOfRange(obj, o, o + RaidEvents.OBJ_STRIDE));
        }
        if (rows.isEmpty()) return;
        panel(g, x - 4, y - 4, pw + 4, 12 + rows.size() * 11, 0xFFFFB000);
        g.drawString(f, "OBJECTIVES", x, y, 0xFFD080, true);
        y += 11;
        double px = mc.player.getX(), pz = mc.player.getZ();
        for (int[] r : rows) {
            int type = r[0], ox = r[1], oz = r[3], value = r[4], id = r[5];
            Entity e = mc.level.getEntity(id);
            String label;
            int color;
            switch (type) {
                case RaidEvents.OBJ_CRATE -> {
                    label = "Supply drop (" + value + "s)";
                    color = 0x7CFF9B;
                }
                case RaidEvents.OBJ_RIFT -> {
                    label = "Close the rift (" + value + "%)";
                    color = 0xC070FF;
                }
                case RaidEvents.OBJ_WARLORD -> {
                    label = (e != null && e.hasCustomName() ? e.getCustomName().getString() : "Warlord") + " " + value + "%";
                    color = 0xFFB000;
                }
                default -> {
                    label = "Revive " + (e != null ? e.getName().getString() : "ally") + " (" + value + "s)";
                    color = (mc.level.getGameTime() / 8) % 2 == 0 ? 0xFF5050 : 0xFFA0A0;
                }
            }
            double dx = ox + 0.5 - px, dz = oz + 0.5 - pz;
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
            float rel = Mth.wrapDegrees(yaw - mc.player.getViewYRot(pt));
            String arrow = ARROWS[Math.floorMod(Math.round(rel / 45f), 8)];
            g.drawString(f, arrow, x, y, color, true);
            g.drawString(f, f.plainSubstrByWidth(label, pw - 40), x + 10, y, color, false);
            String dt = dist + "m";
            g.drawString(f, dt, x + pw - 4 - f.width(dt), y, 0xA0A0A0, false);
            y += 11;
        }
    }

    /** Red vignette, bleed-out bar and revive progress while downed. */
    private static void downed(GuiGraphics g, Font f, ClientDowned.State s, int w, int h, float pt) {
        Minecraft mc = Minecraft.getInstance();
        float pulse = 0.5f + 0.5f * Mth.sin((mc.level.getGameTime() + pt) * 0.25f);
        int edge = (int) (90 + 60 * pulse) << 24 | 0x500000;
        g.fillGradient(0, 0, w, h / 3, edge, 0x00500000);
        g.fillGradient(0, h - h / 3, w, h, 0x00500000, edge);
        g.fill(0, 0, w, h, 0x30200000);
        g.pose().pushPose();
        g.pose().translate(w / 2f, h / 2f - 46, 0);
        g.pose().scale(2.5f, 2.5f, 1);
        g.drawCenteredString(f, "DOWNED", 0, 0, 0xFF3030);
        g.pose().popPose();
        float bleed = Mth.clamp(s.bleed / (float) Math.max(1, s.maxBleed), 0, 1);
        int bw = 160, bx = w / 2 - bw / 2, by = h / 2 - 14;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xFF000000);
        g.fill(bx, by, bx + (int) (bw * bleed), by + 6, 0xFFC01818);
        g.drawCenteredString(f, "Bleeding out in " + (s.bleed + 19) / 20 + "s", w / 2, by + 10, 0xFFB0B0);
        if (s.revive > 0) {
            float r = Mth.clamp(s.revive / (float) com.lastbastion.siege.raid.Downed.REVIVE_TICKS, 0, 1);
            g.fill(bx - 1, by + 23, bx + bw + 1, by + 31, 0xFF000000);
            g.fill(bx, by + 24, bx + (int) (bw * r), by + 30, 0xFFFFD040);
            g.drawCenteredString(f, "Being revived...", w / 2, by + 34, 0xFFE080);
        } else {
            g.drawCenteredString(f, "A teammate can crouch beside you to revive you", w / 2, by + 24, 0xC0C0C0);
        }
    }

    /** Readout next to the hotbar for the held arsenal weapon: ammo/heat/reload for guns, Fury and the special for melee. */
    private void weaponHud(GuiGraphics g, Minecraft mc, int w, int h) {
        net.minecraft.world.item.ItemStack stack = mc.player.getMainHandItem();
        Font f = mc.font;
        int x = w / 2 + 98, y = h - 21;
        if (stack.getItem() instanceof com.lastbastion.item.GunItem gun) {
            var k = gun.kind;
            String line;
            int col;
            float frac;
            if (k.usesHeat()) {
                int heat = stack.hasTag() ? stack.getTag().getInt(com.lastbastion.item.GunItem.TAG_HEAT) : 0;
                boolean over = stack.hasTag() && stack.getTag().getBoolean(com.lastbastion.item.GunItem.TAG_OVERHEAT);
                line = over ? "OVERHEATED" : "HEAT " + heat + "%";
                col = over ? 0xFF4040 : heat > 70 ? 0xFFA040 : 0xE0E0E0;
                frac = heat / 100f;
            } else if (k.mag > 0) {
                if (com.lastbastion.item.GunItem.reloading(stack)) {
                    long end = stack.getTag().getLong(com.lastbastion.item.GunItem.TAG_RELOAD);
                    frac = 1f - Math.max(0, (end - mc.level.getGameTime()) / (float) Math.max(1, gun.reloadLength(stack)));
                    line = "RELOADING";
                    col = 0xFFD966;
                } else {
                    int ammo = gun.ammo(stack), mag = gun.magSize(stack);
                    line = ammo + " / " + mag;
                    col = ammo == 0 ? 0xFF4040 : ammo <= mag / 4 ? 0xFFA040 : 0xFFFFFF;
                    frac = ammo / (float) Math.max(1, mag);
                }
            } else {
                boolean charging = mc.player.isUsingItem() && mc.player.getUseItem() == stack;
                frac = charging ? Math.min(1f, mc.player.getTicksUsingItem() / (float) com.lastbastion.item.GunItem.chargeNeeded(mc.player.isShiftKeyDown())) : 0;
                line = charging ? (frac >= 1 ? "READY" : "CHARGING") : "RAILGUN";
                col = frac >= 1 ? 0x7FE8FF : 0xE0E0E0;
            }
            g.fill(x - 2, y - 2, x + 64, y + 18, 0x80000000);
            g.drawString(f, line, x, y, col, true);
            g.fill(x, y + 11, x + 60, y + 14, 0xFF202020);
            g.fill(x, y + 11, x + (int) (60 * Mth.clamp(frac, 0, 1)), y + 14, 0xFF000000 | k.color);
        } else if (stack.getItem() instanceof com.lastbastion.item.weapon.MeleeWeaponItem mw) {
            var k = mw.kind;
            boolean reaper = k == com.lastbastion.item.weapon.WeaponKind.SOULREAPER;
            int val = reaper ? com.lastbastion.item.weapon.MeleeWeaponItem.souls(stack) : com.lastbastion.item.weapon.MeleeWeaponItem.fury(stack);
            int max = reaper ? com.lastbastion.item.weapon.MeleeWeaponItem.MAX_SOULS : com.lastbastion.item.weapon.MeleeWeaponItem.MAX_FURY;
            boolean cd = mc.player.getCooldowns().isOnCooldown(mw);
            boolean ready = !cd && (reaper ? val >= 3 : val >= k.specialFury);
            g.fill(x - 2, y - 2, x + 84, y + 18, 0x80000000);
            g.drawString(f, k.special + (ready ? "" : cd ? " ..." : ""), x, y, ready ? k.color : 0x909090, true);
            g.fill(x, y + 11, x + 80, y + 14, 0xFF202020);
            g.fill(x, y + 11, x + (int) (80f * val / max), y + 14, reaper ? 0xFF5AFFD8 : ready ? 0xFFFF5030 : 0xFF8A3020);
            if (!reaper) g.fill(x + (int) (80f * k.specialFury / max), y + 10, x + (int) (80f * k.specialFury / max) + 1, y + 15, 0xFFFFFFFF);
        }
    }

    private void exoHud(GuiGraphics g, Font f, TitanExoEntity exo, int w, int h) {
        int cx = w / 2, by = h - 70;
        float time = exo.timeLeft() / (float) TitanExoEntity.DURATION;
        float fuel = exo.fuel() / (float) TitanExoEntity.MAX_FUEL;
        float hp = exo.getHealth() / exo.getMaxHealth();
        g.fill(cx - 91, by - 2, cx + 91, by + 30, 0x80000000);
        bar(g, f, cx - 88, by, 176, "POWER " + exo.timeLeft() / 20 + "s", time, 0xFFE8B020);
        bar(g, f, cx - 88, by + 10, 176, "HULL", hp, 0xFF50D060);
        bar(g, f, cx - 88, by + 20, 176, "JETS", fuel, 0xFF50C8FF);
        int ch = h / 2, cw = w / 2;
        g.fill(cw - 8, ch, cw - 3, ch + 1, 0xFFFF6040);
        g.fill(cw + 3, ch, cw + 8, ch + 1, 0xFFFF6040);
        g.fill(cw, ch - 8, cw + 1, ch - 3, 0xFFFF6040);
        g.fill(cw, ch + 3, cw + 1, ch + 8, 0xFFFF6040);
    }

    private void bar(GuiGraphics g, Font f, int x, int y, int w, String label, float frac, int col) {
        g.fill(x, y, x + w, y + 8, 0xFF202020);
        g.fill(x, y, x + (int) (w * Mth.clamp(frac, 0, 1)), y + 8, col);
        g.drawString(f, label, x + 2, y, 0xFFFFFF, true);
    }
}
