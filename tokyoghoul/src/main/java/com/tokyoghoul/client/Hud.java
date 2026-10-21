package com.tokyoghoul.client;

import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.data.Rank;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * The fighter HUD (Jujutsu Craft's OVERLAY1/OVERLAY2 text): character and rank, the Rc bar, the selected
 * technique with cost and cooldown, its neighbours in the R rotation, kakuja progress and fame.
 */
public final class Hud implements IGuiOverlay {
    public static final Hud INSTANCE = new Hud();

    private Hud() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Packets.PlayerData d = ClientState.self;
        if (mc.player == null || mc.options.hideGui || d == null) return;
        Font font = mc.font;
        CharacterDef def = Characters.get(d.character());
        int x = 6, y = 24;
        if (def == null) {
            g.drawString(font, Component.translatable("hud.tokyoghoul.pick"), x, 6, 0xC8C8C8, true);
            return;
        }
        int color = def.hasKagune() ? def.kaguneColor : 0x8AB8E8;
        String rank = Rank.name(def.race, d.level());
        // panel
        g.fill(x - 3, y - 3, x + 150, y + 64, 0x70000000);
        g.fill(x - 3, y - 3, x - 1, y + 64, 0xFF000000 | color);
        g.drawString(font, def.title(), x + 2, y, 0xFFFFFF, true);
        g.drawString(font, rank, x + 2, y + 10, d.level() >= 5 ? 0xFF5050 : 0xE0C070, true);
        // Rc bar
        int bw = 140, by = y + 21;
        float frac = d.maxRc() <= 0 ? 0 : Math.min(1, d.rc() / d.maxRc());
        g.fill(x + 2, by, x + 2 + bw, by + 6, 0xFF1A1A1A);
        g.fill(x + 2, by, x + 2 + (int) (bw * frac), by + 6, 0xFF000000 | color);
        g.fill(x + 2, by, x + 2 + (int) (bw * frac), by + 2, 0x50FFFFFF);
        String rc = "Rc " + (int) d.rc() + "/" + (int) d.maxRc();
        g.drawString(font, rc, x + 4, by + 8, 0xC0C0C0, true);
        // selected technique
        List<MoveDef> rot = def.rotation();
        int sel = Math.max(0, Math.min(rot.size() - 1, d.selected()));
        MoveDef m = rot.get(sel);
        int cd = sel < d.cooldowns().length ? d.cooldowns()[sel] : 0;
        boolean locked = m.minRank > d.level() || (m.needsKakuja && d.kakuja() < 100);
        String name = m.title().getString();
        String extra = locked ? "  [locked]" : cd > 0 ? String.format("  %.1fs", cd / 20f) : m.cost > 0 ? "  " + m.cost + " Rc" : "";
        int mc2 = locked ? 0x806060 : cd > 0 ? 0x909090 : m.ultimate ? 0xFF4040 : 0xFFFFFF;
        g.drawString(font, "▶ " + name + extra, x + 2, y + 39, mc2, true);
        String prev = rot.get(Math.floorMod(sel - 1, rot.size())).title().getString();
        String next = rot.get(Math.floorMod(sel + 1, rot.size())).title().getString();
        g.drawString(font, "R: " + next + "   ⇧R: " + prev, x + 2, y + 50, 0x707070, false);
        // kakuja / fame line above the panel
        int ly = y + 68;
        if (def.race.hasKagune && def.ultimate() != null && def.ultimate().needsKakuja) {
            String k = d.kakuja() >= 100 ? "Kakuja: ready" : "Kakuja " + d.kakuja() + "%";
            g.drawString(font, k, x, ly, d.kakuja() >= 100 ? 0xFF3030 : 0xB08080, true);
            ly += 10;
        }
        int nextLv = Math.min(Rank.COUNT, d.level() + 1);
        String fame = d.level() >= Rank.COUNT ? "Fame " + (int) d.fame() : "Fame " + (int) d.fame() + " / " + Rank.fameFor(nextLv);
        g.drawString(font, fame, x, ly, 0xA0A0A0, true);
        if (d.satiated()) g.drawString(font, "☕", x + 140, ly, 0xC08040, true);
    }
}
