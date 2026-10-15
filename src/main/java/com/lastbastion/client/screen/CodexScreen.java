package com.lastbastion.client.screen;

import com.lastbastion.entity.boss.BossKind;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.HeroKind;
import com.lastbastion.entity.turret.TurretEntity;
import com.lastbastion.entity.turret.TurretKind;
import com.lastbastion.entity.zombie.ZombieKind;
import com.lastbastion.item.GunItem;
import com.lastbastion.item.armor.ArmorSet;
import com.lastbastion.item.weapon.WeaponKind;
import com.lastbastion.registry.ModItems;
import com.lastbastion.siege.NightModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The Siege Codex: an in-game encyclopedia of the siege. Every zombie, boss, turret, hero and devil is shown as a live,
 * animated 3D model you can turn with the mouse, next to its stats, moves and specialisations, all read from the game
 * data itself so the book never goes out of date. Weapons and armour show their item, the siege's rules get pages of
 * their own.
 */
public class CodexScreen extends Screen {
    private enum Tab {
        SIEGE("Siege", 0xFFB000), ZOMBIES("Zombies", 0x7BD940), BOSSES("Bosses", 0xE04040), TURRETS("Turrets", 0x8FD8FF),
        HEROES("Heroes", 0xFFD966), DEVILS("Devils", 0xFF3A2A), ARSENAL("Arsenal", 0xFF8A50), ARMOUR("Armour", 0x6FA8FF);

        final String title;
        final int color;

        Tab(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }

    /** One line of an entry's text: a heading (coloured) or body text. */
    private record Line(String text, int color, boolean heading) {
    }

    private record Entry(String title, int color, @Nullable Supplier<Entity> model, @Nullable Supplier<ItemStack> icon, List<Line> lines) {
    }

    private static final int BODY = 0xB8C0C8, DIM = 0x7A8490, LIST_ROW = 14;
    private static int lastTab, lastEntry;
    private final Map<Tab, List<Entry>> entries = new HashMap<>();
    private final Map<String, Entity> models = new HashMap<>();
    private Tab tab;
    private int selected;
    private int listScroll, textScroll;
    private float yaw = 25, pitch = -8;
    private boolean dragging;
    private int ticks;
    private int left, top, w, h;

    public CodexScreen() {
        super(Component.literal("Siege Codex"));
        this.tab = Tab.values()[Mth.clamp(lastTab, 0, Tab.values().length - 1)];
        this.selected = lastEntry;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        w = Math.min(500, width - 12);
        h = Math.min(300, height - 12);
        left = (width - w) / 2;
        top = (height - h) / 2;
        if (entries.isEmpty()) build();
        selected = Mth.clamp(selected, 0, Math.max(0, list().size() - 1));
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(left + w - 58, top + h - 22, 52, 18).build());
    }

    private List<Entry> list() {
        return entries.getOrDefault(tab, List.of());
    }

    @Nullable
    private Entry current() {
        List<Entry> l = list();
        return l.isEmpty() ? null : l.get(Mth.clamp(selected, 0, l.size() - 1));
    }

    private int tabsY() {
        return top + 22;
    }

    private int bodyTop() {
        return top + 42;
    }

    private int bodyBottom() {
        return top + h - 26;
    }

    private int listW() {
        return Math.min(132, w / 4 + 10);
    }

    private int modelW() {
        return Math.min(150, (w - listW()) * 2 / 5);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void tick() {
        ticks++;
        if (!dragging) yaw += 0.8f;
        for (Entity e : models.values()) e.tickCount++;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left, top, left + w, top + h, 0xEE0D1015);
        g.fill(left, top, left + w, top + 1, 0xFF000000 | tab.color);
        g.fill(left, top + h - 1, left + w, top + h, 0xFF000000 | tab.color);
        g.drawString(font, "✦ SIEGE CODEX", left + 8, top + 7, 0xFFD080, true);
        String hint = "Drag the model to turn it · scroll to read on";
        g.drawString(font, hint, left + w - 8 - font.width(hint), top + 7, DIM, false);
        // tabs
        Tab[] tabs = Tab.values();
        int tw = (w - 12) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int x = left + 6 + i * tw, y = tabsY();
            boolean sel = tabs[i] == tab, hover = mx >= x && mx < x + tw - 2 && my >= y && my < y + 16;
            g.fill(x, y, x + tw - 2, y + 16, sel ? 0xFF283240 : hover ? 0xFF1E242C : 0xFF151A20);
            if (sel) g.fill(x, y + 14, x + tw - 2, y + 16, 0xFF000000 | tabs[i].color);
            g.drawCenteredString(font, font.plainSubstrByWidth(tabs[i].title, tw - 6), x + (tw - 2) / 2, y + 4, sel ? tabs[i].color : 0xA0A8B0);
        }
        renderList(g, mx, my);
        Entry e = current();
        if (e != null) renderEntry(g, e, mx, my, pt);
        super.render(g, mx, my, pt);
    }

    private void renderList(GuiGraphics g, int mx, int my) {
        int x0 = left + 6, y0 = bodyTop(), x1 = x0 + listW(), y1 = bodyBottom();
        g.fill(x0, y0, x1, y1, 0xFF11151A);
        g.enableScissor(x0, y0, x1, y1);
        List<Entry> l = list();
        for (int i = 0; i < l.size(); i++) {
            int y = y0 + 2 + i * LIST_ROW - listScroll;
            if (y + LIST_ROW < y0 || y > y1) continue;
            boolean sel = i == selected, hover = mx >= x0 && mx < x1 && my >= y && my < y + LIST_ROW && my >= y0 && my < y1;
            if (sel || hover) g.fill(x0, y, x1, y + LIST_ROW, sel ? 0xFF243040 : 0xFF1A2028);
            if (sel) g.fill(x0, y, x0 + 2, y + LIST_ROW, 0xFF000000 | l.get(i).color());
            g.drawString(font, font.plainSubstrByWidth(l.get(i).title(), listW() - 10), x0 + 6, y + 3, sel ? l.get(i).color() : 0xD0D4D8, false);
        }
        g.disableScissor();
        int max = maxListScroll();
        if (max > 0) {
            int lh = y1 - y0, bar = Math.max(12, lh * lh / (l.size() * LIST_ROW + 4));
            int by = y0 + (int) ((lh - bar) * (listScroll / (float) max));
            g.fill(x1 - 2, by, x1, by + bar, 0x80FFFFFF);
        }
    }

    private void renderEntry(GuiGraphics g, Entry e, int mx, int my, float pt) {
        int x0 = left + 12 + listW(), y0 = bodyTop(), y1 = bodyBottom();
        boolean visual = e.model() != null || e.icon() != null;
        int textX = visual ? x0 + modelW() + 8 : x0;
        if (visual) {
            int bx1 = x0 + modelW();
            g.fill(x0, y0, bx1, y1, 0xFF0A0D11);
            g.renderOutline(x0, y0, modelW(), y1 - y0, 0xFF000000 | darken(e.color()));
            if (e.model() != null) renderModel(g, e, x0, y0, bx1, y1, pt);
            else renderIcon(g, e, x0, y0, bx1, y1);
        }
        // title and text
        int tw = left + w - 8 - textX;
        g.drawString(font, font.plainSubstrByWidth(e.title(), tw), textX, y0, e.color(), true);
        int ty0 = y0 + 14;
        g.enableScissor(textX, ty0, textX + tw, y1);
        int y = ty0 - textScroll;
        for (Line l : e.lines()) {
            if (l.heading()) y += 3;
            for (FormattedCharSequence seq : font.split(Component.literal(l.text()), tw - 4)) {
                if (y + 9 >= ty0 && y < y1) g.drawString(font, seq, textX, y, l.color(), false);
                y += 10;
            }
        }
        g.disableScissor();
        int total = y + textScroll - ty0;
        maxText = Math.max(0, total - (y1 - ty0));
        if (maxText > 0) {
            int lh = y1 - ty0, bar = Math.max(12, lh * lh / Math.max(lh, total));
            int by = ty0 + (int) ((lh - bar) * (textScroll / (float) maxText));
            g.fill(textX + tw - 2, by, textX + tw, by + bar, 0x80FFFFFF);
        }
    }

    private int maxText;

    private void renderModel(GuiGraphics g, Entry e, int x0, int y0, int x1, int y1, float pt) {
        Entity ent = model(e);
        if (!(ent instanceof LivingEntity le)) return;
        float bh = Math.max(0.5f, le.getBbHeight()), bw = Math.max(0.5f, le.getBbWidth());
        int boxW = x1 - x0, boxH = y1 - y0;
        int scale = (int) Math.max(6, Math.min(boxH * 0.72f / bh, boxW * 0.42f / bw));
        float yawNow = yaw + (dragging ? 0 : 0.8f * pt);
        le.yBodyRot = le.yBodyRotO = 180f + yawNow;
        le.setYRot(180f + yawNow);
        le.yRotO = le.getYRot();
        le.yHeadRot = le.yHeadRotO = le.getYRot();
        le.setXRot(0);
        le.xRotO = 0;
        Quaternionf tilt = new Quaternionf().rotateX(pitch * Mth.DEG_TO_RAD);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
        int baseY = y0 + (int) (boxH * 0.5f + bh * scale * 0.5f);
        g.enableScissor(x0 + 1, y0 + 1, x1 - 1, y1 - 1);
        try {
            InventoryScreen.renderEntityInInventory(g, (x0 + x1) / 2, baseY, scale, pose, tilt, le);
        } catch (RuntimeException ex) {
            // a model that can't draw outside a world must not take the book down with it
            models.put(e.title(), null);
        }
        g.disableScissor();
        if (e.title() != null && le instanceof TurretEntity t) {
            String lv = "Level " + t.turretLevel() + (t.turretLevel() == 5 ? (t.isA() ? " (A)" : " (B)") : "");
            g.drawCenteredString(font, lv, (x0 + x1) / 2, y1 - 12, DIM);
        }
    }

    private void renderIcon(GuiGraphics g, Entry e, int x0, int y0, int x1, int y1) {
        ItemStack s = e.icon() != null ? e.icon().get() : ItemStack.EMPTY;
        if (s.isEmpty()) return;
        float k = Math.min(x1 - x0, y1 - y0) / 16f * 0.55f;
        g.pose().pushPose();
        g.pose().translate((x0 + x1) / 2f - 8 * k, (y0 + y1) / 2f - 8 * k, 100);
        g.pose().scale(k, k, 1);
        g.renderItem(s, 0, 0);
        g.pose().popPose();
    }

    @Nullable
    private Entity model(Entry e) {
        if (models.containsKey(e.title())) {
            Entity m = models.get(e.title());
            if (m instanceof TurretEntity t) {
                // the model cycles through every level and both level-5 specialisations
                int phase = (ticks / 50) % 6;
                int lv = Math.min(5, phase + 1), br = phase == 5 ? 2 : phase == 4 ? 1 : 0;
                if (t.turretLevel() != lv) t.setTurretLevel(lv);
                if (t.branch() != br) t.setBranch(br);
            }
            return m;
        }
        Entity m = null;
        try {
            m = e.model() != null ? e.model().get() : null;
        } catch (RuntimeException ignored) {
        }
        models.put(e.title(), m);
        return m;
    }

    private static int darken(int c) {
        return ((c >> 16 & 0xFF) / 2 << 16) | ((c >> 8 & 0xFF) / 2 << 8) | (c & 0xFF) / 2;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Tab[] tabs = Tab.values();
        int tw = (w - 12) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int x = left + 6 + i * tw, y = tabsY();
            if (mx >= x && mx < x + tw - 2 && my >= y && my < y + 16) {
                if (tab != tabs[i]) {
                    tab = tabs[i];
                    lastTab = i;
                    selected = lastEntry = 0;
                    listScroll = textScroll = 0;
                    click();
                }
                return true;
            }
        }
        int x0 = left + 6, y0 = bodyTop(), x1 = x0 + listW(), y1 = bodyBottom();
        if (mx >= x0 && mx < x1 && my >= y0 && my < y1) {
            int i = (int) ((my - y0 - 2 + listScroll) / LIST_ROW);
            if (i >= 0 && i < list().size() && i != selected) {
                selected = lastEntry = i;
                textScroll = 0;
                click();
            }
            return true;
        }
        Entry e = current();
        int mx0 = left + 12 + listW();
        if (e != null && e.model() != null && mx >= mx0 && mx < mx0 + modelW() && my >= y0 && my < y1) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            yaw += (float) dx * 1.6f;
            pitch = Mth.clamp(pitch - (float) dy * 0.8f, -40, 30);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < left + 6 + listW()) {
            listScroll = (int) Mth.clamp(listScroll - delta * LIST_ROW * 2, 0, maxListScroll());
        } else {
            textScroll = (int) Mth.clamp(textScroll - delta * 20, 0, maxText);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // up / down arrows walk the list
        if (key == 265 || key == 264) {
            int n = list().size();
            if (n > 0) {
                selected = lastEntry = Mth.clamp(selected + (key == 264 ? 1 : -1), 0, n - 1);
                textScroll = 0;
                int y = selected * LIST_ROW, view = bodyBottom() - bodyTop() - 4;
                if (y < listScroll) listScroll = y;
                if (y + LIST_ROW > listScroll + view) listScroll = y + LIST_ROW - view;
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private int maxListScroll() {
        return Math.max(0, list().size() * LIST_ROW + 4 - (bodyBottom() - bodyTop()));
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        for (Entity m : models.values()) if (m != null) m.discard();
        models.clear();
    }

    // ------------------------------------------------------------------ content

    private static Line head(String s, int color) {
        return new Line(s, color, true);
    }

    private static Line body(String s) {
        return new Line(s, BODY, false);
    }

    private static Line dim(String s) {
        return new Line(s, DIM, false);
    }

    private static String name(String id) {
        String key = "entity.lastbastion." + id;
        String t = Component.translatable(key).getString();
        if (!t.equals(key)) return t;
        return Character.toUpperCase(id.charAt(0)) + id.substring(1).replace('_', ' ');
    }

    private static String num(float f) {
        return f == Math.round(f) ? String.valueOf(Math.round(f)) : String.format(Locale.ROOT, "%.1f", f);
    }

    @Nullable
    private static Entity spawn(Supplier<? extends net.minecraft.world.entity.EntityType<?>> type) {
        var level = Minecraft.getInstance().level;
        return level == null ? null : type.get().create(level);
    }

    private void build() {
        List<Entry> siege = new ArrayList<>();
        siege.add(new Entry("The Siege Core", 0xFFB000, null, () -> new ItemStack(ModItems.SIEGE_CORE.get()), List.of(
                body("Craft a Siege Core (iron blocks at the corners, gold, redstone and a diamond in the middle) and place it. "
                        + "Only one core can stand in a world. Right-click it to open the War Room: shop, rules, protocols and your summons."),
                head("Defeat", 0xFF7070),
                body("When the core's health runs out it explodes. You keep part of your credits (25% by default), your summons' "
                        + "sigils come back to you recovering, and the siege restarts from wave 1 with a fresh core."),
                head("Upgrades", 0xFFD080),
                body("Fortify the core's health, raise a regenerating Arcane Barrier, add a Retaliation Pulse, a War Treasury, "
                        + "Turret Overclock and a Rally Beacon. Emergency repairs are always on sale."))));
        siege.add(new Entry("Waves & Credits", 0xFFD080, null, null, List.of(
                body("A wave starts at nightfall, on a timer, or only when you sound the War Horn (choose in the rules). "
                        + "Sounding the horn early pays +25%."),
                body("Zombies rise in a ring around the core and march on it, breaching any block in their way. "
                        + "Each wave is bigger, tougher and comes from more sides."),
                head("Credits", 0xFFE070),
                body("Every kill pays whoever landed it (turrets, heroes and devils pay their owner). A cleared wave pays everyone; "
                        + "a wave where the core is never touched pays a perfect-defence bonus."),
                head("Warforged Tokens", 0xFFB000),
                body("Every 10th wave, every boss and every warlord pays a token: the currency of the best weapon upgrades."))));
        siege.add(new Entry("Siege Rules", 0x8FD8FF, null, null, List.of(
                body("Open Siege Rules from the War Room to tune this world's siege. Presets (Casual, Normal, Veteran, Nightmare) "
                        + "set the difficulty; change anything and it becomes Custom."),
                head("Difficulty", 0xFFD080), body("Enemy health and damage, wave size and growth, boss health, champions, core health."),
                head("Pacing", 0xFFD080), body("What starts a wave, preparation time, boss interval, a victory wave, enemy cap, spawn distance, wave time limit."),
                head("Twists", 0xFFD080), body("Special nights and each night type, war events and their rate, supply drops, rifts, warlords."),
                head("Roster", 0xFFD080), body("Every zombie and boss on or off; whether zombies break blocks."),
                head("Stakes", 0xFFD080), body("Credit multiplier, downed state and bleed-out time, harsh defeat, salvage, perfect-defence bonus."))));
        List<Line> nights = new ArrayList<>();
        nights.add(body("From wave 6 a night may turn special (chance and types are in the rules). Special nights pay bonus credits."));
        for (NightModifier m : NightModifier.values()) {
            if (m == NightModifier.NONE) continue;
            nights.add(head(m.title, m.color));
            nights.add(body(m.desc));
        }
        siege.add(new Entry("Special Nights", 0xD01818, null, null, nights));
        siege.add(new Entry("War Events", 0xB050FF, null, null, List.of(
                body("Later waves are interrupted by events, marked on your HUD with an arrow and a distance."),
                head("Supply Drop", 0x7CFF9B),
                body("A crate parachutes in. Reach it before it scuttles itself: credits, grenades, call-ins, consumables, sometimes tokens."),
                head("Warlord", 0xFFB000),
                body("A named, oversized champion with an escort and a boss bar that rallies the horde. Four times the credits and a token."),
                head("Corruption Rift", 0xB050FF),
                body("A tear that keeps spewing zombies at the core until it is destroyed, and blasts back anyone who loiters at it."))));
        siege.add(new Entry("Co-op: Downed", 0x7CFF9B, null, null, List.of(
                body("With other players defending, a lethal blow knocks you down instead of killing you. You crawl and cannot fight "
                        + "while you bleed out."),
                body("A teammate crouching beside you for three seconds revives you (and is paid for it). A Seraph's Sanctuary obelisk "
                        + "raises the fallen in its aura. Clearing the wave gets everyone back on their feet."))));
        siege.add(new Entry("Summons", 0xFFD966, null, null, List.of(
                body("Use a Sigil (hero) or an Infernal Pact (devil) anywhere. Near the core a summon guards it; elsewhere it follows you."),
                head("Orders", 0xFFD080),
                body("Guard holds the core or the spot you left it. Hunt roams after bosses, champions, necromancers and shriekers. "
                        + "Follow Me stays at your side, attacks what you hit and what hits you, and follows you through portals."),
                body("Sneak + right-click a summon to change its orders, right-click it for its screen, or press G to order them all."),
                head("Falling", 0xFF7070),
                body("A fallen summon's sigil returns to you recovering (a minute for Ashfang up to ten for a devil). "
                        + "Rush the recovery at the core. Sigils keep their summon's level."))));
        siege.add(new Entry("Elements", 0xC86BFF, null, null, List.of(
                body("Damage has an element; enemies resist or fear some of them."),
                head("Combos", 0xFFD080),
                body("Soaked enemies take +50% lightning damage and chain further. Frozen enemies shatter for +50% physical or explosive "
                        + "damage. Marked enemies take more from everything; Corroded ones lose armour."),
                head("Infernal", 0xE0301A),
                body("The devils' hellfire ignores armour and nothing is immune to it."))));
        siege.add(new Entry("Controls", 0xA0A8B0, null, null, List.of(
                body("G: orders for all your summons.   V: core protocols.   R: reload."),
                body("Melee weapons: attack to combo, right-click for the special."),
                body("Guns: use to fire, sneak + use for the alternate fire, R to reload."))));
        entries.put(Tab.SIEGE, siege);

        List<Entry> zombies = new ArrayList<>();
        for (ZombieKind k : ZombieKind.values()) {
            zombies.add(new Entry(name(k.id), 0x9BD970, () -> spawn(k.type()), null, List.of(
                    body(k.role),
                    head("Stats (rank I, wave 1)", 0xFFD080),
                    body("Health " + num(k.hp) + "   Damage " + num(k.damage) + "   Armour " + num(k.armor)),
                    body("Core damage " + k.coreDamage + "   Bounty " + k.credits + " credits"),
                    dim("First seen around wave " + k.unlockWave + ". Five ranks: Rotten, Feral, Armored, Corrupted, Abyssal - "
                            + "each tougher and more fearsome; champions add affixes."))));
        }
        entries.put(Tab.ZOMBIES, zombies);

        List<Entry> bosses = new ArrayList<>();
        for (BossKind k : BossKind.values()) {
            bosses.add(new Entry(k.title, 0xFF7070, () -> spawn(k.type()), null, List.of(
                    body(k.lore),
                    head("Stats (wave of arrival)", 0xFFD080),
                    body("Health " + num(k.hp) + "   Damage " + num(k.damage) + "   Armour " + num(k.armor)),
                    body("Bounty " + k.credits + " credits and a Warforged Token"),
                    dim("First arrives on wave " + (5 + k.ordinal() * 5) + " (with the default boss interval)."))));
        }
        entries.put(Tab.BOSSES, bosses);

        List<Entry> turrets = new ArrayList<>();
        for (TurretKind k : TurretKind.values()) {
            turrets.add(new Entry(k.title, k.color, () -> {
                Entity e = spawn(k.type());
                if (e instanceof TurretEntity t) t.setTurretLevel(1);
                return e;
            }, null, List.of(
                    body(k.desc),
                    head("Stats (level 1)", 0xFFD080),
                    body("Damage " + num(k.damage) + "   Range " + k.range + "   Element " + cap(k.element.name())),
                    body("Cost " + k.cost + " credits"),
                    head("Level 3", 0x8FD8FF), body(k.perk3),
                    head("Level 5: " + k.branchA, 0xFFB000), body(k.branchADesc),
                    head("Level 5: " + k.branchB, 0xFFB000), body(k.branchBDesc))));
        }
        entries.put(Tab.TURRETS, turrets);

        List<Entry> heroes = new ArrayList<>(), devils = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) {
            List<Line> lines = new ArrayList<>();
            lines.add(body(k.desc));
            lines.add(head("Stats (level 1)", 0xFFD080));
            lines.add(body("Health " + num(k.hp) + "   Damage " + num(k.damage) + "   Armour " + num(k.armor)));
            lines.add(body("Role " + cap(k.role.name()) + "   Cost " + k.cost + " credits"));
            lines.add(head(k.devil ? "Infernal moveset" : "Moveset", k.devil ? 0xFF5A3A : 0x8FD8FF));
            for (String mv : k.moves.split(" / ")) {
                boolean ult = mv.startsWith("ULT ");
                lines.add(ult ? new Line("★ " + mv.substring(4), 0xFFD080, false) : body("• " + mv));
            }
            if (k.devil) {
                lines.add(head("The pact", 0xFF3A2A));
                lines.add(body("One devil per player. Binding costs half your current health. A fallen devil needs ten minutes to return. "
                        + "Devils never retreat, ignore fire and harmful effects, and their hellfire pierces armour."));
            }
            Entry en = new Entry(k.title, k.color | 0x202020, () -> {
                Entity e = spawn(k.type());
                if (e instanceof HeroEntity he) he.setLevelAndXp(5, 0);
                return e;
            }, null, lines);
            (k.devil ? devils : heroes).add(en);
        }
        entries.put(Tab.HEROES, heroes);
        entries.put(Tab.DEVILS, devils);

        List<Entry> arsenal = new ArrayList<>();
        for (WeaponKind k : WeaponKind.values()) {
            arsenal.add(new Entry(k.title, k.color, null, () -> new ItemStack(ModItems.MELEE.get(k).get()), List.of(
                    dim(cap(k.weaponClass.name()) + (k.weaponClass.twoHanded ? " (two-handed)" : "") + "   Tier " + k.tier + "   " + cap(k.element.name())),
                    body("Damage " + num(k.damage) + "   Speed " + num(k.attacksPerSecond) + "/s   Reach " + num(k.reach) + "   Cost " + k.cost),
                    head("Special: " + k.special, 0xFFB000), body(k.specialDesc),
                    head("Passive: " + k.passive, 0x8FD8FF), body(k.passiveDesc),
                    head("Awakened: " + k.awakened, 0xC86BFF), body(k.awakenedDesc),
                    head("Ascended", 0xFFD080), body(k.ascendedDesc),
                    dim(k.lore))));
        }
        for (GunItem.GunKind k : GunItem.GunKind.values()) {
            arsenal.add(new Entry(k.title, k.color, null, () -> new ItemStack(ModItems.GUNS.get(k).get()), List.of(
                    body(k.desc),
                    body("Damage " + num(k.damage) + (k.mag > 0 ? "   Magazine " + k.mag : "") + "   Cost " + k.cost),
                    head("Alt-fire: " + k.alt, 0xFFB000), body(k.altDesc))));
        }
        entries.put(Tab.ARSENAL, arsenal);

        List<Entry> armour = new ArrayList<>();
        for (ArmorSet s : ArmorSet.values()) {
            armour.add(new Entry(s.title, s.color, null, () -> new ItemStack(ModItems.ARMOR.get(s).get(1).get()), List.of(
                    dim("Tier " + s.tier + "   Defence " + (s.defence[0] + s.defence[1] + s.defence[2] + s.defence[3])
                            + "   Toughness " + num(s.toughness) + "   Cost " + s.cost + " per set"),
                    head("Set bonus: " + s.bonus, 0xFFB000), body(s.bonusDesc),
                    dim(s.lore))));
        }
        entries.put(Tab.ARMOUR, armour);
    }

    private static String cap(String s) {
        String l = s.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(l.charAt(0)) + l.substring(1);
    }
}
