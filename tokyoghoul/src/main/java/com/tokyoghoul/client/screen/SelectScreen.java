package com.tokyoghoul.client.screen;

import com.tokyoghoul.client.ClientState;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.CharacterDef.Group;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.data.KaguneType;
import com.tokyoghoul.data.Rank;
import com.tokyoghoul.entity.TGCharacterEntity;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.network.Packets;
import com.tokyoghoul.network.TGNet;
import com.tokyoghoul.registry.TGEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ghoul Registry book: every playable character, grouped like Jujutsu Craft's technique selector
 * (Sorcerers / Cursed Spirits / Curse Users), with a live 3D preview, the canon blurb and the moveset
 * with the rank each move unlocks at.
 */
public class SelectScreen extends Screen {
    private Group group = Group.ANTEIKU;
    private CharacterDef selected;
    private boolean showKagune = true;
    private final Map<String, TGCharacterEntity> previews = new HashMap<>();
    private final List<Button> characterButtons = new ArrayList<>();

    public SelectScreen() {
        super(Component.translatable("screen.tokyoghoul.registry"));
        Packets.PlayerData d = ClientState.self;
        selected = d == null ? null : Characters.get(d.character());
        if (selected != null) group = selected.group;
    }

    @Override
    protected void init() {
        clearWidgets();
        characterButtons.clear();
        int left = 12;
        int x = left;
        for (Group g : Group.values()) {
            int w = font.width(g.title) + 12;
            Button b = Button.builder(Component.literal(g == group ? "§c" + g.title : g.title), btn -> {
                group = g;
                init();
            }).bounds(x, 26, w, 18).build();
            addRenderableWidget(b);
            x += w + 3;
        }
        List<CharacterDef> list = new ArrayList<>();
        for (CharacterDef d : Characters.selectable()) if (d.group == group) list.add(d);
        int colW = 104, rowH = 21;
        for (int i = 0; i < list.size(); i++) {
            CharacterDef d = list.get(i);
            int cx = left + (i % 2) * (colW + 4), cy = 52 + (i / 2) * rowH;
            Button b = Button.builder(Component.literal((d == selected ? "§c" : "") + d.name), btn -> {
                selected = d;
                init();
            }).bounds(cx, cy, colW, 19).build();
            addRenderableWidget(b);
            characterButtons.add(b);
        }
        int panelX = left + 2 * (colW + 4) + 10;
        addRenderableWidget(Button.builder(Component.translatable(showKagune ? "screen.tokyoghoul.hide_kagune" : "screen.tokyoghoul.show_kagune"), btn -> {
            showKagune = !showKagune;
            init();
        }).bounds(panelX, height - 30, 90, 20).build());
        Button become = Button.builder(Component.translatable("screen.tokyoghoul.become", selected == null ? "..." : selected.name), btn -> {
            if (selected != null) {
                TGNet.CHANNEL.sendToServer(new Packets.Select(selected.id));
                onClose();
            }
        }).bounds(width - 170, height - 30, 158, 20).build();
        become.active = selected != null;
        addRenderableWidget(become);
    }

    private TGCharacterEntity preview(CharacterDef d) {
        Minecraft mc = Minecraft.getInstance();
        return previews.computeIfAbsent(d.id, id -> {
            var type = TGEntities.typeOf(id);
            if (type == null || mc.level == null) return null;
            TGCharacterEntity e = type.create(mc.level);
            if (e != null) e.setFighterLevel(d.npcLevel);
            return e;
        });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g);
        g.fill(6, 6, width - 6, height - 6, 0xC0100608);
        g.fill(6, 6, width - 6, 8, 0xFFA01020);
        g.drawString(font, title, 12, 12, 0xFFE0E0, true);
        Packets.PlayerData d = ClientState.self;
        if (d != null && Characters.get(d.character()) != null) {
            CharacterDef cur = Characters.get(d.character());
            String now = "Current: " + cur.name + " (" + Rank.name(cur.race, d.level()) + ")";
            g.drawString(font, now, width - 12 - font.width(now), 12, 0xA0A0A0, false);
        }
        super.render(g, mouseX, mouseY, pt);
        if (selected == null) {
            g.drawString(font, Component.translatable("screen.tokyoghoul.pick"), 240, 60, 0xC0C0C0, false);
            return;
        }
        int panelX = 12 + 2 * 108 + 10;
        int modelX = panelX + 50, modelY = height - 48;
        TGCharacterEntity e = preview(selected);
        if (e != null) {
            int flags = TGCharacterEntity.FLAG_MASK;
            if (showKagune) flags |= TGCharacterEntity.FLAG_KAGUNE;
            e.setPreviewFlags(showKagune ? flags : 0);
            e.tickCount = (int) (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime());
            int scale = (int) (52 / Math.max(0.8f, selected.look.height));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, modelX, modelY - 20, scale, modelX - mouseX, modelY - 120 - mouseY, e);
        }
        int tx = panelX + 112, ty = 52, tw = width - tx - 14;
        g.drawString(font, selected.title(), tx, ty, 0xFFFFFF, true);
        ty += 12;
        String kind = Component.translatable(selected.faction.key()).getString();
        if (selected.kagune != KaguneType.NONE) {
            kind += "  •  " + Component.translatable(selected.kagune.key()).getString();
            if (selected.kagune2 != KaguneType.NONE) kind += " + " + Component.translatable(selected.kagune2.key()).getString();
        } else if (!selected.quinque.isEmpty()) {
            kind += "  •  Quinque";
        }
        g.drawString(font, kind, tx, ty, selected.hasKagune() ? selected.kaguneColor | 0xFF000000 : 0xFFB0C8E8, false);
        ty += 14;
        for (FormattedCharSequence line : font.split(Component.literal(selected.blurb), tw)) {
            g.drawString(font, line, tx, ty, 0xC8C8C8, false);
            ty += 10;
        }
        ty += 6;
        g.drawString(font, Component.translatable("screen.tokyoghoul.moves"), tx, ty, 0xE0B060, false);
        ty += 12;
        int level = d == null ? 1 : d.level();
        boolean mine = d != null && selected.id.equals(d.character());
        for (MoveDef m : selected.rotation()) {
            if (ty > height - 40) break;
            boolean unlocked = mine && m.minRank <= level;
            String name = m.title().getString();
            String req = m.minRank > 1 ? " - " + Rank.name(selected.race, m.minRank) : "";
            if (m.ultimate) req += m.needsKakuja ? " + kakuja" : "";
            int col = m.ultimate ? 0xFF5050 : unlocked || m.minRank <= 1 ? 0xFFFFFF : 0x909090;
            g.drawString(font, (m.ultimate ? "★ " : "• ") + name, tx, ty, col, false);
            g.drawString(font, req, tx + font.width((m.ultimate ? "★ " : "• ") + name), ty, 0x707070, false);
            ty += 10;
        }
        g.drawString(font, Component.translatable("screen.tokyoghoul.keys"), 12, height - 22, 0x808080, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
