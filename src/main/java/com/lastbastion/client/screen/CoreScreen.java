package com.lastbastion.client.screen;

import com.lastbastion.client.ClientSiegeState;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.HeroKind;
import com.lastbastion.entity.hero.HeroMode;
import com.lastbastion.network.ActionPacket;
import com.lastbastion.network.Net;
import com.lastbastion.registry.ModItems;
import com.lastbastion.siege.CoreUpgrade;
import com.lastbastion.siege.ShopCatalog;
import com.lastbastion.siege.SiegeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The Siege Core "war room": a tabbed shop (turrets, heroes, arsenal, support, supplies, fortifications,
 * core upgrades) plus a Command tab to manage heroes, revive the fallen and sound the war horn early.
 */
public class CoreScreen extends Screen {
    private static int lastTab = 0;
    private final BlockPos core;
    private final int[] fallen;
    private int tab;
    private int scroll;
    private ShopCatalog.Entry selected;
    private int left, top, w, h;
    private Button buyButton;
    private final List<ShopCatalog.Entry> visible = new ArrayList<>();
    private static final int ROW = 22;
    private static final int COMMAND_TAB = ShopCatalog.Category.values().length;

    public CoreScreen(BlockPos core, int[] fallen) {
        super(Component.literal("Siege Core"));
        this.core = core;
        this.fallen = fallen;
        this.tab = lastTab;
    }

    @Override
    protected void init() {
        w = Math.min(400, width - 20);
        h = Math.min(250, height - 20);
        left = (width - w) / 2;
        top = (height - h) / 2;
        buyButton = addRenderableWidget(Button.builder(Component.literal("Purchase"), b -> buy())
                .bounds(left + w - 112, top + h - 26, 104, 20).build());
        refresh();
    }

    private void refresh() {
        visible.clear();
        clearWidgetsExceptBuy();
        if (tab < COMMAND_TAB) {
            ShopCatalog.Category cat = ShopCatalog.Category.values()[tab];
            for (ShopCatalog.Entry e : ShopCatalog.all()) if (e.category == cat) visible.add(e);
            if (selected == null || selected.category != cat) selected = visible.isEmpty() ? null : visible.get(0);
            buyButton.visible = true;
        } else {
            buyButton.visible = false;
            buildCommandTab();
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private final List<Button> dynamic = new ArrayList<>();

    private void clearWidgetsExceptBuy() {
        for (Button b : dynamic) removeWidget(b);
        dynamic.clear();
    }

    private void buildCommandTab() {
        int x = left + 92, y = top + 34;
        Button horn = Button.builder(Component.literal("Sound the War Horn (start next wave, +25% bonus)"), b -> {
            Net.toServer(new ActionPacket(ActionPacket.CALL_WAVE, 0, ""));
            onClose();
        }).bounds(x, y, w - 100, 20).build();
        horn.active = ClientSiegeState.state() == SiegeState.PREPARING;
        dynamic.add(addRenderableWidget(horn));
        y += 22;
        dynamic.add(addRenderableWidget(Button.builder(Component.literal("Core Protocols (abilities) - also on key V"),
                b -> Minecraft.getInstance().setScreen(new ProtocolScreen())).bounds(x, y, w - 100, 20).build()));
        y += 26;
        int bw = (w - 100 - 8) / 3;
        for (int i = 0; i < HeroMode.values().length; i++) {
            HeroMode m = HeroMode.values()[i];
            dynamic.add(addRenderableWidget(Button.builder(Component.literal("All: " + m.title),
                    b -> Net.toServer(new ActionPacket(ActionPacket.ALL_HEROES_MODE, 0, m.name()))).bounds(x + i * (bw + 4), y, bw, 20).build()));
        }
        int nFallen = Math.min(fallen.length / 2, 3);
        y = top + h - 6 - nFallen * 20;
        for (int i = 0; i + 1 < fallen.length && i / 2 < nFallen; i += 2) {
            HeroKind k = HeroKind.values()[fallen[i]];
            int idx = i / 2;
            int cost = k.cost / 2;
            Button b = Button.builder(Component.literal("Revive " + k.title + " (Lv" + fallen[i + 1] + ") - " + cost),
                    btn -> Net.toServer(new ActionPacket(ActionPacket.BUY, idx, "revive"))).bounds(x, y, w - 100, 18).build();
            b.active = ClientSiegeState.credits() >= cost;
            dynamic.add(addRenderableWidget(b));
            y += 20;
        }
    }

    private int maxScroll() {
        int listH = h - 40;
        return Math.max(0, visible.size() * ROW - listH);
    }

    private void buy() {
        if (selected == null) return;
        Net.toServer(new ActionPacket(ActionPacket.BUY, 0, selected.id));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left, top, left + w, top + h, 0xE0101418);
        g.fill(left, top, left + w, top + 1, 0xFF6FE3FF);
        g.fill(left, top + h - 1, left + w, top + h, 0xFF6FE3FF);
        g.drawString(font, "✦ SIEGE CORE - WAR ROOM", left + 8, top + 6, 0x6FE3FF, true);
        String credits = "⛁ " + String.format("%,d", ClientSiegeState.credits()) + " credits";
        g.drawString(font, credits, left + w - 8 - font.width(credits), top + 6, 0xFFD966, true);
        String wave = "Wave " + ClientSiegeState.wave() + "  |  Core " + Math.round(ClientSiegeState.coreFraction() * 100) + "%";
        g.drawString(font, wave, left + 8, top + 18, 0xA0A0A0, false);

        // tabs
        int ty = top + 32;
        for (int i = 0; i <= COMMAND_TAB; i++) {
            String name = i < COMMAND_TAB ? ShopCatalog.Category.values()[i].title : "Command";
            int col = i < COMMAND_TAB ? ShopCatalog.Category.values()[i].color : 0x7CFF9B;
            boolean sel = i == tab;
            boolean hover = mx >= left + 6 && mx < left + 84 && my >= ty && my < ty + 18;
            g.fill(left + 6, ty, left + 84, ty + 18, sel ? 0xFF2A3440 : hover ? 0xFF20262E : 0xFF181C22);
            if (sel) g.fill(left + 6, ty, left + 8, ty + 18, 0xFF000000 | col);
            g.drawString(font, name, left + 12, ty + 5, sel ? col : 0xC0C0C0, false);
            ty += 20;
        }

        if (tab < COMMAND_TAB) renderShop(g, mx, my);
        else renderCommand(g);
        super.render(g, mx, my, pt);
    }

    private void renderShop(GuiGraphics g, int mx, int my) {
        int lx = left + 90, ly = top + 32, lw = 150, lh = h - 40;
        g.fill(lx, ly, lx + lw, ly + lh, 0xFF14181C);
        g.enableScissor(lx, ly, lx + lw, ly + lh);
        int[] up = ClientSiegeState.upgrades();
        int y = ly - scroll;
        for (ShopCatalog.Entry e : visible) {
            if (y + ROW > ly && y < ly + lh) {
                boolean sel = e == selected;
                boolean hover = mx >= lx && mx < lx + lw && my >= y && my < y + ROW && my >= ly && my < ly + lh;
                g.fill(lx, y, lx + lw, y + ROW - 1, sel ? 0xFF283848 : hover ? 0xFF1E262E : 0xFF14181C);
                g.renderItem(e.icon.get(), lx + 3, y + 3);
                int cost = e.cost(up, ClientSiegeState.wave());
                String name = font.plainSubstrByWidth(e.name, lw - 26);
                g.drawString(font, name, lx + 22, y + 2, 0xFFFFFF, false);
                String c = cost < 0 ? "MAX" : String.format("%,d", cost);
                g.drawString(font, c, lx + 22, y + 12, cost < 0 ? 0x7CFF9B : ClientSiegeState.credits() >= cost ? 0xFFD966 : 0xFF6060, false);
                if (e.upgrade != null) {
                    String lvl = "Lv " + up[e.upgrade.ordinal()] + "/" + e.upgrade.maxLevel;
                    g.drawString(font, lvl, lx + lw - 4 - font.width(lvl), y + 12, 0xC86BFF, false);
                }
            }
            y += ROW;
        }
        g.disableScissor();
        // details
        int dx = lx + lw + 8, dy = ly, dw = left + w - 8 - dx;
        g.fill(dx, dy, dx + dw, top + h - 32, 0xFF14181C);
        if (selected == null) return;
        g.renderItem(selected.icon.get(), dx + 4, dy + 4);
        g.drawString(font, font.plainSubstrByWidth(selected.name, dw - 26), dx + 24, dy + 8, 0xFFFFFF, true);
        int cy = dy + 26;
        for (String line : selected.lines) {
            for (FormattedCharSequence seq : font.split(Component.literal(line), dw - 8)) {
                if (cy > top + h - 44) break;
                g.drawString(font, seq, dx + 4, cy, 0xC8C8C8, false);
                cy += 10;
            }
            cy += 3;
        }
        int cost = selected.cost(up, ClientSiegeState.wave());
        buyButton.active = cost >= 0 && ClientSiegeState.credits() >= cost;
        buyButton.setMessage(Component.literal(cost < 0 ? "Maxed" : "Buy (" + String.format("%,d", cost) + ")"));
    }

    private void renderCommand(GuiGraphics g) {
        int x = left + 92, y = top + 104;
        g.drawString(font, "Your heroes on the field:", x, y, 0x7CFF9B, false);
        y += 12;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        List<HeroEntity> heroes = mc.level.getEntitiesOfClass(HeroEntity.class, new AABB(core).inflate(96),
                hh -> mc.player.getUUID().equals(hh.getOwnerId()));
        if (heroes.isEmpty()) g.drawString(font, "  none - buy hero sigils in the Heroes tab", x, y, 0x808080, false);
        int count = 0;
        int maxRows = Math.max(1, (top + h - 24 - Math.min(fallen.length / 2, 3) * 20 - y) / 10);
        for (HeroEntity he : heroes) {
            if (count++ >= maxRows) break;
            String s = "  " + he.kind.title + "  Lv" + he.heroLevel() + "  " + (int) he.getHealth() + "/" + (int) he.getMaxHealth() + "  [" + he.mode().title + "]";
            g.drawString(font, font.plainSubstrByWidth(s, w - 100), x, y, 0xD0D0D0, false);
            y += 10;
        }
        int ry = top + h - 6 - Math.min(fallen.length / 2, 3) * 20;
        if (fallen.length > 0) g.drawString(font, "Fallen heroes (revive for half price):", x, ry - 11, 0xFF8080, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int ty = top + 32;
        for (int i = 0; i <= COMMAND_TAB; i++) {
            if (mx >= left + 6 && mx < left + 84 && my >= ty && my < ty + 18) {
                tab = lastTab = i;
                scroll = 0;
                refresh();
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                return true;
            }
            ty += 20;
        }
        if (tab < COMMAND_TAB) {
            int lx = left + 90, ly = top + 32, lw = 150, lh = h - 40;
            if (mx >= lx && mx < lx + lw && my >= ly && my < ly + lh) {
                int idx = (int) ((my - ly + scroll) / ROW);
                if (idx >= 0 && idx < visible.size()) {
                    selected = visible.get(idx);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - delta * ROW));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Keeps icons valid even before items finish registering in odd load orders. */
    static ItemStack safe(java.util.function.Supplier<ItemStack> s) {
        try {
            return s.get();
        } catch (Exception e) {
            return new ItemStack(ModItems.SIEGE_CORE.get());
        }
    }

    static String upgradeLine(CoreUpgrade u, int[] up) {
        return u.title + " " + up[u.ordinal()] + "/" + u.maxLevel;
    }
}
