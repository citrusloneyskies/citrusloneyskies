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
 * core upgrades) plus a Command tab: orders for your summons, rushing fallen ones back, wave intel, the siege
 * rules and the war horn.
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
    private final List<Button> rushButtons = new ArrayList<>();
    private final List<Integer> rushSlots = new ArrayList<>();

    /** Inventory slots holding sigils or pacts that are still recovering from a defeat. */
    private static List<Integer> recoveringSlots() {
        var p = Minecraft.getInstance().player;
        var lvl = Minecraft.getInstance().level;
        List<Integer> out = new ArrayList<>();
        if (p == null || lvl == null) return out;
        long now = lvl.getGameTime();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.getItem() instanceof com.lastbastion.item.HeroSigilItem && com.lastbastion.entity.hero.Companions.recoveryLeft(st, now) > 0) out.add(i);
        }
        return out;
    }

    private void updateRushButtons() {
        var p = Minecraft.getInstance().player;
        var lvl = Minecraft.getInstance().level;
        if (p == null || lvl == null) return;
        long now = lvl.getGameTime();
        for (int i = 0; i < rushButtons.size(); i++) {
            Button b = rushButtons.get(i);
            ItemStack st = p.getInventory().getItem(rushSlots.get(i));
            if (!(st.getItem() instanceof com.lastbastion.item.HeroSigilItem sig) || com.lastbastion.entity.hero.Companions.recoveryLeft(st, now) <= 0) {
                b.active = false;
                b.setMessage(Component.literal("Ready"));
                continue;
            }
            long secs = (com.lastbastion.entity.hero.Companions.recoveryLeft(st, now) + 19) / 20;
            int cost = ShopCatalog.rushCost(sig.kind, st, now);
            b.setMessage(Component.literal("Rush " + sig.kind.shortName() + " (" + secs / 60 + ":" + String.format("%02d", secs % 60) + " left) - " + String.format("%,d", cost)));
            b.active = ClientSiegeState.credits() >= cost;
        }
    }

    private void clearWidgetsExceptBuy() {
        for (Button b : dynamic) removeWidget(b);
        dynamic.clear();
    }

    private void buildCommandTab() {
        int x = left + 92, y = top + 34;
        Button horn = Button.builder(Component.literal("Sound the War Horn (start the wave now, +25% bonus)"), b -> {
            Net.toServer(new ActionPacket(ActionPacket.CALL_WAVE, 0, ""));
            onClose();
        }).bounds(x, y, w - 100, 20).build();
        horn.active = ClientSiegeState.state() == SiegeState.PREPARING;
        dynamic.add(addRenderableWidget(horn));
        y += 22;
        int half = (w - 100 - 4) / 2;
        dynamic.add(addRenderableWidget(Button.builder(Component.literal("Core Protocols (V)"),
                b -> Minecraft.getInstance().setScreen(new ProtocolScreen())).bounds(x, y, half, 20).build()));
        dynamic.add(addRenderableWidget(Button.builder(Component.literal("\u2699 Siege Rules"),
                b -> Net.toServer(new ActionPacket(ActionPacket.OPEN_RULES, 0, ""))).bounds(x + half + 4, y, half, 20).build()));
        y += 24;
        int modes = HeroMode.values().length;
        int bw = (w - 100 - 4 * (modes - 1)) / modes;
        for (int i = 0; i < modes; i++) {
            HeroMode m = HeroMode.values()[i];
            dynamic.add(addRenderableWidget(Button.builder(Component.literal("All: " + m.title),
                    b -> Net.toServer(new ActionPacket(ActionPacket.ALL_HEROES_MODE, 0, m.name()))).bounds(x + i * (bw + 4), y, bw, 20).build()));
        }
        rushSlots.clear();
        rushButtons.clear();
        List<Integer> rec = recoveringSlots();
        int nRush = Math.min(rec.size(), 3);
        y = top + h - 6 - nRush * 20;
        for (int i = 0; i < nRush; i++) {
            int slot = rec.get(i);
            Button b = Button.builder(Component.literal("Rush"), btn -> {
                Net.toServer(new ActionPacket(ActionPacket.BUY, slot, "revive"));
                refresh();
            }).bounds(x, y, w - 100, 18).build();
            rushSlots.add(slot);
            rushButtons.add(b);
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
        int lbCrashDepth = com.lastbastion.util.Safe.depth(g.pose());
        try {
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
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("CoreScreen.render", lbCrash);
            com.lastbastion.util.Safe.restore(g.pose(), lbCrashDepth);
        }
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
                int cost = e.cost(up, ClientSiegeState.wave(), held());
                String name = font.plainSubstrByWidth(e.name, lw - 26);
                g.drawString(font, name, lx + 22, y + 2, 0xFFFFFF, false);
                if (ShopCatalog.paidInTokens(e)) {
                    String c = cost == -2 ? "\u2014" : cost < 0 ? "MAX" : "1 Warforged Token";
                    g.drawString(font, c, lx + 22, y + 12, cost < 0 ? 0x7CFF9B : hasToken() ? 0xC070FF : 0xFF6060, false);
                } else {
                    String c = cost == -2 ? "\u2014" : cost < 0 ? "MAX" : String.format("%,d", cost);
                    g.drawString(font, c, lx + 22, y + 12, cost < 0 ? 0x7CFF9B : ClientSiegeState.credits() >= cost ? 0xFFD966 : 0xFF6060, false);
                }
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
        int cost = selected.cost(up, ClientSiegeState.wave(), held());
        if (ShopCatalog.paidInTokens(selected)) {
            buyButton.active = cost >= 0 && hasToken();
            buyButton.setMessage(Component.literal(cost == -2 ? "Hold a weapon" : cost < 0 ? "Maxed" : hasToken() ? "Spend 1 Token" : "No Token"));
        } else {
            buyButton.active = cost >= 0 && ClientSiegeState.credits() >= cost;
            buyButton.setMessage(Component.literal(cost == -2 ? "Hold a weapon" : cost < 0 ? "Maxed" : "Buy (" + String.format("%,d", cost) + ")"));
        }
    }

    private void renderCommand(GuiGraphics g) {
        int x = left + 92, y = top + 104;
        int colW = (w - 100 - 6) / 2;
        int ix = x + colW + 6;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        // left column: your heroes
        g.drawString(font, "Your summons nearby:", x, y, 0x7CFF9B, false);
        int hy = y + 12;
        List<HeroEntity> heroes = mc.level.getEntitiesOfClass(HeroEntity.class, new AABB(core).inflate(96),
                hh -> mc.player.getUUID().equals(hh.getOwnerId()) && !(hh instanceof com.lastbastion.entity.hero.KageCloneEntity));
        if (heroes.isEmpty()) g.drawString(font, font.plainSubstrByWidth("none yet (Heroes tab)", colW), x + 4, hy, 0x808080, false);
        int count = 0;
        int maxRows = Math.max(1, (top + h - 24 - rushButtons.size() * 20 - hy) / 10);
        for (HeroEntity he : heroes) {
            if (count++ >= maxRows) break;
            String st = he.kind.title.split(",")[0] + " Lv" + he.heroLevel() + " " + Math.round(100 * he.getHealth() / he.getMaxHealth()) + "%";
            g.drawString(font, font.plainSubstrByWidth(st, colW - 4), x + 4, hy, 0xD0D0D0, false);
            hy += 10;
        }
        // right column: next-wave intel
        g.fill(ix - 2, y - 2, ix + colW, y + 92, 0xFF12161B);
        var hud = ClientSiegeState.hud;
        int[] intel = hud == null ? new int[0] : hud.intel();
        if (ClientSiegeState.state() == SiegeState.WAVE) {
            g.drawString(font, "Wave " + ClientSiegeState.wave() + " in progress", ix + 2, y + 2, 0xFF7070, false);
            g.drawString(font, (hud == null ? 0 : hud.remaining()) + " undead remain", ix + 2, y + 14, 0xC0C0C0, false);
        } else if (intel.length >= 2) {
            g.drawString(font, "INTEL: wave " + ClientSiegeState.wave(), ix + 2, y + 2, 0x8FD8FF, false);
            int iy = y + 14;
            com.lastbastion.siege.NightModifier m = com.lastbastion.siege.NightModifier.byId(intel[0]);
            if (m != com.lastbastion.siege.NightModifier.NONE) {
                g.drawString(font, font.plainSubstrByWidth("\u263D " + m.title, colW - 4), ix + 2, iy, m.color, false);
                iy += 10;
            }
            g.drawString(font, intel[1] + " undead", ix + 2, iy, 0xC0C0C0, false);
            iy += 10;
            for (int i = 2; i + 1 < intel.length && iy < y + 84; i += 2) {
                int kind = intel[i];
                String name;
                int col;
                if (kind >= com.lastbastion.siege.WavePlanner.BOSS_OFFSET) {
                    int b = kind - com.lastbastion.siege.WavePlanner.BOSS_OFFSET;
                    name = "BOSS " + (b < com.lastbastion.entity.boss.BossKind.values().length ? com.lastbastion.entity.boss.BossKind.values()[b].title : "?");
                    col = 0xFF5050;
                } else {
                    var zk = com.lastbastion.entity.zombie.ZombieKind.values()[Math.max(0, Math.min(kind, com.lastbastion.entity.zombie.ZombieKind.values().length - 1))];
                    name = intel[i + 1] + "\u00D7 " + Component.translatable("entity.lastbastion." + zk.id).getString();
                    col = 0xA8B0B8;
                }
                g.drawString(font, font.plainSubstrByWidth(name, colW - 6), ix + 4, iy, col, false);
                iy += 10;
            }
        } else {
            g.drawString(font, "Intel arrives once the", ix + 2, y + 2, 0x808080, false);
            g.drawString(font, "next wave is planned.", ix + 2, y + 12, 0x808080, false);
        }
        updateRushButtons();
        int ry = top + h - 6 - rushButtons.size() * 20;
        if (!rushButtons.isEmpty()) g.drawString(font, "Recovering summons (rush them back):", x, ry - 11, 0xFF8080, false);
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

    private static boolean hasToken() {
        var p = Minecraft.getInstance().player;
        return p != null && (p.isCreative() || ShopCatalog.tokenSlot(p) >= 0);
    }

    private static ItemStack held() {
        var p = Minecraft.getInstance().player;
        return p == null ? ItemStack.EMPTY : p.getMainHandItem();
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
