package com.lastbastion.client.screen;

import com.lastbastion.network.Net;
import com.lastbastion.network.RulesApplyPacket;
import com.lastbastion.siege.SiegeRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Siege Rules: every knob of this world's siege, grouped in sections. Edits happen on a local copy and are sent
 * with Apply; players without permission see the rules read-only.
 */
public class RulesScreen extends Screen {
    private static int lastSection = 0;
    private SiegeRules rules;
    private SiegeRules original;
    private boolean canEdit;
    private int section;
    private int scroll;
    private int left, top, w, h;
    private Button apply, reset;
    private SiegeRules.Rule hovered;
    private static final int ROW = 20;

    public RulesScreen(CompoundTag tag, boolean canEdit) {
        super(Component.literal("Siege Rules"));
        this.rules = SiegeRules.fromTag(tag);
        this.original = rules.copy();
        this.canEdit = canEdit;
        this.section = lastSection;
    }

    /** The server confirmed (or refused) an apply: show the authoritative rules. */
    public void refresh(CompoundTag tag, boolean canEdit) {
        this.rules = SiegeRules.fromTag(tag);
        this.original = rules.copy();
        this.canEdit = canEdit;
    }

    @Override
    protected void init() {
        w = Math.min(420, width - 16);
        h = Math.min(260, height - 16);
        left = (width - w) / 2;
        top = (height - h) / 2;
        apply = addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            Net.toServer(new RulesApplyPacket(rules.save(), false));
            click();
        }).bounds(left + w - 222, top + h - 24, 70, 20).build());
        reset = addRenderableWidget(Button.builder(Component.literal("Defaults"), b -> {
            Net.toServer(new RulesApplyPacket(new CompoundTag(), true));
            click();
        }).bounds(left + w - 148, top + h - 24, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(left + w - 74, top + h - 24, 66, 20).build());
    }

    private List<SiegeRules.Rule> visible() {
        List<SiegeRules.Rule> out = new ArrayList<>();
        SiegeRules.Section s = SiegeRules.Section.values()[section];
        for (SiegeRules.Rule r : SiegeRules.ALL) if (r.section == s) out.add(r);
        return out;
    }

    private int listTop() {
        return top + 30;
    }

    private int listHeight() {
        return h - 30 - 58;
    }

    private int maxScroll() {
        return Math.max(0, visible().size() * ROW - listHeight());
    }

    private boolean dirty() {
        for (SiegeRules.Rule r : SiegeRules.ALL) if (Math.abs(rules.get(r) - original.get(r)) > 1e-9) return true;
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left, top, left + w, top + h, 0xE40E1116);
        g.fill(left, top, left + w, top + 1, 0xFFFFB000);
        g.fill(left, top + h - 1, left + w, top + h, 0xFFFFB000);
        g.drawString(font, "⚙ SIEGE RULES", left + 8, top + 7, 0xFFD080, true);
        String sub = canEdit ? (dirty() ? "Unsaved changes - press Apply" : "Changes apply to this world's siege") : "Read only: the core's owner or an operator can change these";
        g.drawString(font, sub, left + w - 8 - font.width(sub), top + 7, canEdit ? (dirty() ? 0xFFE070 : 0x8090A0) : 0xFF8080, false);
        // sections
        int sy = top + 30;
        for (int i = 0; i < SiegeRules.Section.values().length; i++) {
            SiegeRules.Section s = SiegeRules.Section.values()[i];
            boolean sel = i == section, hover = mx >= left + 6 && mx < left + 104 && my >= sy && my < sy + 20;
            g.fill(left + 6, sy, left + 104, sy + 20, sel ? 0xFF2A3440 : hover ? 0xFF20262E : 0xFF161A20);
            if (sel) g.fill(left + 6, sy, left + 8, sy + 20, 0xFFFFB000);
            String t = font.plainSubstrByWidth(s.title, 92);
            g.drawString(font, t, left + 12, sy + 6, sel ? 0xFFD080 : 0xC0C0C0, false);
            sy += 22;
        }
        // rows
        int lx = left + 110, ly = listTop(), lw = w - 116, lh = listHeight();
        g.fill(lx, ly, lx + lw, ly + lh, 0xFF12161B);
        g.enableScissor(lx, ly, lx + lw, ly + lh);
        hovered = null;
        int y = ly - scroll;
        for (SiegeRules.Rule r : visible()) {
            if (y + ROW > ly && y < ly + lh) {
                boolean hover = mx >= lx && mx < lx + lw && my >= Math.max(y, ly) && my < Math.min(y + ROW, ly + lh);
                if (hover) hovered = r;
                g.fill(lx, y, lx + lw, y + ROW - 1, hover ? 0xFF1C232B : 0xFF151A20);
                boolean changed = Math.abs(rules.get(r) - original.get(r)) > 1e-9;
                g.drawString(font, font.plainSubstrByWidth(r.label, lw - 120), lx + 6, y + 6, changed ? 0xFFE070 : 0xE0E0E0, false);
                String v = r.format(rules.get(r));
                int col = r.type == SiegeRules.Type.TOGGLE ? (rules.on(r) ? 0x7CFF9B : 0xFF7070) : 0x8FD8FF;
                g.drawCenteredString(font, v, lx + lw - 84, y + 6, col);
                if (canEdit) {
                    if (r.type == SiegeRules.Type.TOGGLE || r.type == SiegeRules.Type.CHOICE) {
                        drawBtn(g, lx + lw - 24, y + 3, 20, "↻", mx, my);
                    } else {
                        drawBtn(g, lx + lw - 46, y + 3, 20, "-", mx, my);
                        drawBtn(g, lx + lw - 24, y + 3, 20, "+", mx, my);
                    }
                }
            }
            y += ROW;
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            int bar = Math.max(16, lh * lh / (visible().size() * ROW));
            int by = ly + (int) ((lh - bar) * (scroll / (float) maxScroll()));
            g.fill(lx + lw - 2, by, lx + lw, by + bar, 0x80FFB000);
        }
        // description
        int dy = ly + lh + 4;
        SiegeRules.Rule info = hovered;
        if (info != null) {
            int yy = dy;
            for (FormattedCharSequence seq : font.split(Component.literal(info.desc), w - 236)) {
                if (yy > top + h - 12) break;
                g.drawString(font, seq, left + 8, yy, 0xA8B0B8, false);
                yy += 10;
            }
        } else {
            g.drawString(font, "Hover a rule to see what it does.", left + 8, dy, 0x6A7480, false);
        }
        apply.active = canEdit && dirty();
        reset.active = canEdit;
        super.render(g, mx, my, pt);
    }

    private void drawBtn(GuiGraphics g, int x, int y, int bw, String label, int mx, int my) {
        boolean hover = mx >= x && mx < x + bw && my >= y && my < y + 14;
        g.fill(x, y, x + bw, y + 14, hover ? 0xFF3A4654 : 0xFF262E38);
        g.renderOutline(x, y, bw, 14, 0xFF4A5868);
        g.drawCenteredString(font, label, x + bw / 2, y + 3, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int sy = top + 30;
        for (int i = 0; i < SiegeRules.Section.values().length; i++) {
            if (mx >= left + 6 && mx < left + 104 && my >= sy && my < sy + 20) {
                section = lastSection = i;
                scroll = 0;
                click();
                return true;
            }
            sy += 22;
        }
        if (canEdit) {
            int lx = left + 110, ly = listTop(), lw = w - 116, lh = listHeight();
            if (mx >= lx && mx < lx + lw && my >= ly && my < ly + lh) {
                int idx = (int) ((my - ly + scroll) / ROW);
                List<SiegeRules.Rule> vis = visible();
                if (idx >= 0 && idx < vis.size()) {
                    SiegeRules.Rule r = vis.get(idx);
                    int rowY = ly - scroll + idx * ROW;
                    boolean big = hasShiftDown();
                    if (r.type == SiegeRules.Type.TOGGLE || r.type == SiegeRules.Type.CHOICE) {
                        if (mx >= lx + lw - 24 && mx < lx + lw - 4 && my >= rowY + 3 && my < rowY + 17) {
                            double v = rules.get(r) + (button == 1 ? -1 : 1);
                            if (v > r.max) v = r.min;
                            if (v < r.min) v = r.max;
                            rules.set(r, v);
                            click();
                            return true;
                        }
                    } else {
                        double step = r.step * (big ? 5 : 1);
                        if (mx >= lx + lw - 46 && mx < lx + lw - 26 && my >= rowY + 3 && my < rowY + 17) {
                            rules.set(r, round(rules.get(r) - step, r));
                            click();
                            return true;
                        }
                        if (mx >= lx + lw - 24 && mx < lx + lw - 4 && my >= rowY + 3 && my < rowY + 17) {
                            rules.set(r, round(rules.get(r) + step, r));
                            click();
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** Snap to the rule's step grid so repeated clicks don't drift. */
    private static double round(double v, SiegeRules.Rule r) {
        return Math.round(v / r.step) * r.step;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - delta * ROW));
        return true;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
