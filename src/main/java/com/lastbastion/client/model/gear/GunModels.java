package com.lastbastion.client.model.gear;

import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.item.GunItem.GunKind;

/**
 * 3D models for the firearms. Conventions (model pixels, Y down): the top of the pistol grip, where the
 * trigger hand closes, is the origin; the muzzle points toward -Z, the top of the gun toward -Y and the
 * stock toward +Z. Parts whose glow follows heat or charge are named so the renderer can find them.
 */
public final class GunModels {
    private GunModels() {
    }

    public static void build(GunKind k, ModelSpec s) {
        build(k.name(), s);
    }

    /** Builds by enum constant name (lets offline tools build models without loading the item class). */
    public static void build(String kind, ModelSpec s) {
        Part g = s.root.child("gun", 0, 0, 0);
        switch (kind) {
            case "RAVAGER" -> ravager(g);
            case "THUNDERBOOM" -> thunderboom(g);
            case "HELLFIRE" -> hellfire(g);
            case "STARPIERCER" -> starpiercer(g);
            case "DRAGONBREATH" -> dragonbreath(g);
            case "STORMFIST" -> stormfist(g);
            case "GLACIER" -> glacier(g);
            case "SUNLANCE" -> sunlance(g);
            default -> throw new IllegalArgumentException("Unknown gun " + kind);
        }
    }

    // ------------------------------------------------------------------ materials

    static Paint p(Mat m, int c) {
        return Paint.of(m, c);
    }

    static Paint gunmetal() {
        return p(Mat.DARK_METAL, 0x34373E).gloss(0.45f).noise(0.03f);
    }

    static Paint steel() {
        return p(Mat.METAL, 0x9EA4AE).gloss(0.7f);
    }

    static Paint polymer(int c) {
        return p(Mat.RUBBER, c).noise(0.04f);
    }

    static Paint brass() {
        return p(Mat.GOLD, 0xC09040).accent(0xF4DC98);
    }

    static Paint wood(int c) {
        return p(Mat.WOOD, c).accent(0x3A2414);
    }

    /** Barrel along -Z: octagonal tube from z0 forward by len. */
    static void barrel(Part g, float y, float z0, float len, float dia, Paint paint) {
        g.octoZ(0, y, z0 - len, dia, len, paint);
    }

    /** Standard pistol grip below the origin, raked back. */
    static void grip(Part g, Paint paint) {
        Part gr = g.sub(0, -0.4f, 0.4f).rot(-16, 0, 0);
        gr.box(-0.9f, 0, -1.1f, 1.8f, 4.6f, 2.2f, paint);
        gr.box(-0.95f, 3.9f, -1.2f, 1.9f, 0.7f, 2.4f, gunmetal());
    }

    /** Trigger and guard just ahead of the grip. */
    static void trigger(Part g, float y) {
        g.box(-0.3f, y, -2.4f, 0.6f, 1.4f, 0.4f, steel());
        g.box(-0.45f, y + 1.3f, -3.2f, 0.9f, 0.4f, 3.0f, gunmetal());
    }

    // ------------------------------------------------------------------ guns

    /** M60 'Ravager': belt-fed machine gun with a vented heat shroud that glows as it heats. */
    static void ravager(Part g) {
        Paint body = gunmetal().trim(0x22242A, 0.25f);
        Paint olive = p(Mat.PLATE, 0x4A5236).noise(0.05f);
        // receiver and feed cover
        g.box(-1.4f, -3.6f, -4.0f, 2.8f, 3.4f, 10.5f, body);
        g.box(-1.5f, -4.4f, -3.4f, 3.0f, 0.9f, 6.2f, olive.copy().decal(Face.LEFT, DecalType.STRIPE, 0xE8C040, false, 0.2f, 0.5f, 0.6f));
        g.box(-0.5f, -5.4f, -1.6f, 1.0f, 1.0f, 3.6f, steel());
        // carry handle
        g.box(-0.3f, -6.3f, -2.6f, 0.6f, 0.6f, 4.4f, steel());
        // stock
        Part st = g.sub(0, -2.3f, 6.4f);
        st.box(-1.2f, -1.4f, 0, 2.4f, 3.0f, 3.0f, olive);
        st.box(-1.1f, -1.2f, 3.0f, 2.2f, 3.4f, 4.6f, olive.copy().decal(Face.LEFT, DecalType.CHEVRON, 0xC8C090, false, 0.5f, 0.5f, 0.7f));
        st.box(-1.2f, -1.4f, 7.5f, 2.4f, 4.0f, 0.9f, polymer(0x1C1C1E));
        grip(g, polymer(0x2A2A2C));
        trigger(g, -0.2f);
        // barrel with a vented shroud; the heat core shows through the vents
        barrel(g, -2.0f, -4.0f, 17.5f, 1.15f, steel());
        // vented shroud: the vent lattice glows as the barrel heats (the renderer scales the glow by heat)
        g.box(-1.15f, -3.25f, -15.0f, 2.3f, 2.5f, 11f, gunmetal().pattern(Pattern.HEX, 0xFF6A18, 1.6f).accentGlow());
        g.octoZ(0, -2.0f, -23.0f, 1.8f, 2.0f, body);
        // front sight and folded bipod
        g.box(-0.2f, -3.7f, -20.5f, 0.4f, 1.6f, 0.6f, steel());
        g.box(-0.7f, -0.6f, -18.5f, 0.5f, 0.5f, 8.0f, steel());
        g.box(0.2f, -0.6f, -18.5f, 0.5f, 0.5f, 8.0f, steel());
        // ammo box and belt on the left
        g.box(1.4f, -2.8f, -2.2f, 2.6f, 4.0f, 3.8f, olive.copy().trim(0x2E3220, 0.3f).decal(Face.LEFT, DecalType.STRIPE, 0xE8C040, false, 0.5f, 0.3f, 0.5f));
        Paint round = brass();
        for (int i = 0; i < 4; i++) g.box(1.2f - i * 0.15f, -3.5f + i * 0.2f, -1.3f + i * 0.05f, 0.5f, 0.5f, 1.6f, round);
    }

    /** 'Thunderboom': pump-action scattergun with a flared muzzle and an engraved brass receiver. */
    static void thunderboom(Part g) {
        Paint walnut = wood(0x6A3E22);
        Paint brassP = brass().trim(0x8A6A2A, 0.35f).decal(Face.LEFT, DecalType.BOLT, 0x3A2A10, false, 0.5f, 0.5f, 0.9f)
                .decal(Face.RIGHT, DecalType.BOLT, 0x3A2A10, false, 0.5f, 0.5f, 0.9f);
        Paint blued = p(Mat.DARK_METAL, 0x2C3038).gloss(0.6f);
        // receiver
        g.box(-1.1f, -3.4f, -3.0f, 2.2f, 3.2f, 6.2f, brassP);
        // twin tubes: barrel on top, magazine below
        barrel(g, -2.6f, -3.0f, 13f, 1.35f, blued);
        barrel(g, -1.15f, -3.0f, 11f, 1.1f, blued);
        // flared muzzle (bell)
        Part bell = g.sub(0, -2.6f, -16.0f).rot(90, 0, 0);
        bell.taper(0, 0, 0, 1.4f, 1.4f, 2.4f, 2.4f, 2.2f, 4, blued);
        g.octoZ(0, -2.6f, -18.6f, 2.5f, 0.5f, brassP);
        // pump fore-end with grooves
        g.box(-1.0f, -2.2f, -10.4f, 2.0f, 2.2f, 5.0f, walnut.copy().pattern(Pattern.BANDS, 0x4A2A14, 2.2f));
        // stock and grip in one sweeping piece
        Part st = g.sub(0, -2.0f, 3.2f).rot(-8, 0, 0);
        st.box(-1.0f, -1.2f, 0, 2.0f, 2.4f, 3.5f, walnut);
        st.box(-1.05f, -1.3f, 3.4f, 2.1f, 3.6f, 5.0f, walnut.copy().decal(Face.LEFT, DecalType.BOLT, 0xF4F06A, false, 0.5f, 0.5f, 0.8f));
        st.box(-1.1f, -1.4f, 8.3f, 2.2f, 3.9f, 0.7f, blued);
        grip(g, walnut);
        trigger(g, -0.2f);
        g.box(-0.2f, -3.6f, -14.5f, 0.4f, 0.5f, 0.5f, brass());
    }

    /** 'Hellfire': shoulder-fired launcher, armoured tube with optic and a visible warhead. */
    static void hellfire(Part g) {
        Paint tube = p(Mat.PLATE, 0x3E4A32).noise(0.05f);
        Paint hazard = p(Mat.PLATE, 0x262626).pattern(Pattern.HAZARD, 0xE8B820, 1.4f);
        Paint warhead = p(Mat.PLATE, 0xB82A1A).gloss(0.5f);
        Paint lens = Paint.glowing(0xFF4030);
        // main tube
        g.octoZ(0, -4.4f, -12.0f, 4.2f, 22.0f, tube);
        g.octoZ(0, -4.4f, -13.4f, 4.8f, 1.6f, hazard);
        g.octoZ(0, -4.4f, 9.6f, 4.8f, 1.4f, hazard);
        // exhaust flare at the back
        Part flare = g.sub(0, -4.4f, 11.0f).rot(-90, 0, 0);
        flare.taper(0, 0, 0, 4.2f, 4.2f, 5.4f, 5.4f, 1.8f, 3, p(Mat.DARK_METAL, 0x2A2A2A));
        // warhead peeking out of the muzzle
        Part wh = g.sub(0, -4.4f, -13.2f).rot(90, 0, 0);
        wh.taper(0, 0, 0, 2.6f, 2.6f, 0.4f, 0.4f, 2.6f, 5, warhead);
        // optic on the left with a glowing lens
        g.box(2.2f, -6.6f, -4.0f, 1.6f, 1.8f, 4.5f, gunmetal());
        g.box(2.4f, -6.4f, -4.3f, 1.2f, 1.4f, 0.4f, lens);
        // grips and shoulder rest
        grip(g, polymer(0x2A2A2C));
        trigger(g, -0.4f);
        Part fore = g.sub(0, -2.2f, -6.5f).rot(-8, 0, 0);
        fore.box(-0.8f, 0, -0.9f, 1.6f, 3.6f, 1.8f, polymer(0x2A2A2C));
        g.box(-1.6f, -2.6f, 4.0f, 3.2f, 2.0f, 4.0f, polymer(0x1E1E20));
        g.box(-2.4f, -6.9f, -2.0f, 4.8f, 0.6f, 1.2f, tube.copy().decal(Face.FRONT, DecalType.SKULL, 0xE8E0D0, false, 0.5f, 0.5f, 0.8f));
    }

    /** 'Starpiercer': twin magnetic rails with glowing coils and a capacitor bank. */
    static void starpiercer(Part g) {
        Paint shell = p(Mat.PLATE, 0xD8DEE6).gloss(0.5f);
        Paint dark = p(Mat.DARK_METAL, 0x2A2E36).gloss(0.6f);
        Paint coil = p(Mat.METAL, 0xB87A3A).pattern(Pattern.BANDS, 0x7A4A1E, 3.0f);
        Paint charge = Paint.glowing(0x7FE8FF);
        // body
        g.box(-1.6f, -4.0f, -4.0f, 3.2f, 3.8f, 9.0f, shell.copy().decal(Face.LEFT, DecalType.STRIPE, 0x3A8AD8, false, 0.3f, 0.5f, 0.6f)
                .decal(Face.RIGHT, DecalType.STRIPE, 0x3A8AD8, false, 0.3f, 0.5f, 0.6f));
        g.box(-1.0f, -4.6f, -2.0f, 2.0f, 0.7f, 5.0f, dark);
        // capacitor bank: three glowing cells
        for (int i = 0; i < 3; i++) g.box(1.6f, -3.6f, -3.0f + i * 2.4f, 0.8f, 2.2f, 1.8f, charge);
        // rails: two plates with coils between
        g.box(-1.3f, -4.2f, -22f, 0.6f, 1.2f, 18.2f, dark);
        g.box(0.7f, -4.2f, -22f, 0.6f, 1.2f, 18.2f, dark);
        g.box(-1.3f, -1.6f, -22f, 0.6f, 1.2f, 18.2f, dark);
        g.box(0.7f, -1.6f, -22f, 0.6f, 1.2f, 18.2f, dark);
        g.box(-0.4f, -3.4f, -21.5f, 0.8f, 2.2f, 17.4f, charge);
        for (int i = 0; i < 5; i++) g.box(-1.5f, -4.4f, -20f + i * 3.4f, 3.0f, 4.2f, 0.9f, coil);
        // muzzle crown
        g.box(-1.7f, -4.6f, -23.0f, 3.4f, 4.6f, 1.0f, shell);
        // scope
        g.octoZ(0, -6.0f, -3.5f, 1.4f, 5.0f, dark);
        g.box(-0.5f, -6.5f, -3.7f, 1.0f, 1.0f, 0.3f, charge);
        // stock
        g.box(-1.2f, -3.4f, 5.0f, 2.4f, 2.6f, 5.0f, shell);
        g.box(-1.3f, -3.6f, 9.6f, 2.6f, 4.2f, 1.0f, dark);
        grip(g, dark);
        trigger(g, -0.2f);
    }

    /** "Dragon's Breath": twin fuel tanks, a heat-shielded nozzle and a blue pilot flame. */
    static void dragonbreath(Part g) {
        Paint tank = p(Mat.PLATE, 0xB02A1A).gloss(0.4f).trim(0x4A1A10, 0.3f);
        Paint hazard = p(Mat.PLATE, 0x262626).pattern(Pattern.HAZARD, 0xE8B820, 1.2f);
        Paint copper = p(Mat.METAL, 0xB86E38).gloss(0.6f);
        Paint pilot = Paint.glowing(0x5AB0FF);
        // body
        g.box(-1.3f, -3.4f, -5.0f, 2.6f, 3.2f, 9.0f, gunmetal());
        // tanks under and behind
        g.octoZ(-1.0f, 1.6f, -2.0f, 2.4f, 9.0f, tank);
        g.octoZ(1.0f, 1.6f, -2.0f, 2.4f, 9.0f, tank);
        g.octoZ(0, 1.6f, 1.0f, 3.0f, 1.0f, hazard);
        // hose
        g.box(1.4f, -1.2f, -5.0f, 0.7f, 0.7f, 6.0f, p(Mat.RUBBER, 0x1A1A1A));
        // nozzle: copper pipe inside a perforated heat shield, glowing tip
        barrel(g, -2.2f, -5.0f, 11f, 1.2f, copper);
        g.box(-1.2f, -3.4f, -14.5f, 2.4f, 2.4f, 8.0f, gunmetal().pattern(Pattern.HEX, 0xFF6A18, 1.8f).accentGlow());
        g.octoZ(0, -2.2f, -17.6f, 1.8f, 1.6f, copper);
        g.box(-0.3f, -0.9f, -17.2f, 0.6f, 0.6f, 0.6f, pilot);
        // stock
        g.box(-1.0f, -3.0f, 4.0f, 2.0f, 2.2f, 5.5f, gunmetal());
        grip(g, polymer(0x2A2A2C));
        trigger(g, -0.2f);
        Part fore = g.sub(0, -1.8f, -7.5f).rot(-6, 0, 0);
        fore.box(-0.7f, 0, -0.8f, 1.4f, 3.0f, 1.6f, polymer(0x2A2A2C));
    }

    /** 'Stormfist': a copper-coil tesla gauntlet with a glass capacitor and knuckle prongs. */
    static void stormfist(Part g) {
        Paint copper = p(Mat.METAL, 0xB8763A).gloss(0.65f);
        Paint plate = p(Mat.PLATE, 0x3A3E48).gloss(0.4f);
        Paint coil = p(Mat.METAL, 0xC88A48).pattern(Pattern.BANDS, 0x7A4A1E, 3.2f);
        Paint arc = Paint.glowing(0xF4F06A);
        Paint glass = p(Mat.GLASS, 0x9FD8FF).accent(0xFFFFFF).accentGlow();
        // forearm bracer (centred on the grip, sleeve toward +Z)
        g.box(-2.0f, -2.4f, -1.0f, 4.0f, 4.0f, 8.0f, plate.copy().trim(0xB8763A, 0.4f));
        for (int i = 0; i < 3; i++) g.octoZ(0, -0.4f, 0.4f + i * 2.4f, 4.6f, 0.8f, coil);
        // fist plate and knuckle prongs
        g.box(-2.1f, -2.6f, -4.0f, 4.2f, 4.2f, 3.0f, plate);
        for (int i = 0; i < 3; i++) {
            float x = -1.4f + i * 1.4f;
            g.box(x - 0.3f, -2.2f, -6.0f, 0.6f, 0.6f, 2.2f, copper);
            g.box(x - 0.2f, -2.1f, -6.4f, 0.4f, 0.4f, 0.5f, arc);
        }
        // glass capacitor tube on top with lightning inside
        g.octoZ(0, -3.8f, -2.5f, 1.8f, 8.0f, glass);
        g.box(-0.2f, -4.0f, -2.0f, 0.4f, 0.4f, 7.0f, arc);
        g.octoZ(0, -3.8f, -3.0f, 2.2f, 0.6f, copper);
        g.octoZ(0, -3.8f, 5.4f, 2.2f, 0.6f, copper);
        // side coils
        g.octoZ(-2.3f, -0.4f, -3.5f, 1.0f, 6.0f, coil);
        g.octoZ(2.3f, -0.4f, -3.5f, 1.0f, 6.0f, coil);
    }

    /** 'Glacier': a frost cannon with coolant canisters and ice growing on the barrel. */
    static void glacier(Part g) {
        Paint frost = p(Mat.PLATE, 0xC8DCE8).gloss(0.5f);
        Paint dark = p(Mat.DARK_METAL, 0x2E3A48).gloss(0.5f);
        Paint coolant = Paint.glowing(0x6FE3FF);
        Paint ice = p(Mat.ICE, 0xA8E4FA).accent(0xFFFFFF).accentGlow();
        g.box(-1.6f, -3.8f, -5.0f, 3.2f, 3.6f, 9.5f, frost.copy().decal(Face.LEFT, DecalType.STAR, 0x3A8AD8, false, 0.5f, 0.5f, 0.6f));
        // wide barrel with frost fins
        barrel(g, -2.2f, -5.0f, 10.5f, 2.6f, dark);
        for (int i = 0; i < 4; i++) g.octoZ(0, -2.2f, -7.0f - i * 2.2f, 3.4f, 0.6f, frost);
        g.octoZ(0, -2.2f, -16.6f, 3.2f, 1.2f, frost);
        g.octoZ(0, -2.2f, -16.8f, 1.6f, 0.4f, coolant);
        // coolant canisters
        g.octoZ(1.9f, -1.2f, -3.0f, 1.4f, 4.0f, coolant);
        g.octoZ(-1.9f, -1.2f, -3.0f, 1.4f, 4.0f, coolant);
        // ice crystals grown over the barrel
        g.spike(0.6f, -3.6f, -11f, 0.8f, 2.2f, -30, 0, 20, ice);
        g.spike(-0.8f, -3.4f, -14f, 0.6f, 1.8f, -20, 0, -25, ice);
        g.spike(1.2f, -1.0f, -9f, 0.6f, 1.6f, 160, 0, 30, ice);
        // stock
        g.box(-1.2f, -3.2f, 4.5f, 2.4f, 2.6f, 5.0f, frost);
        g.box(-1.3f, -3.4f, 9.2f, 2.6f, 3.8f, 0.9f, dark);
        grip(g, dark);
        trigger(g, -0.2f);
    }

    /** 'Sunlance': a gilded repeating crossbow with a bolt magazine and a sun emblem. */
    static void sunlance(Part g) {
        Paint ivory = p(Mat.PLATE, 0xEDE6D6).gloss(0.4f);
        Paint goldP = p(Mat.GOLD, 0xD6A83C).accent(0xFFF0B8);
        Paint string = p(Mat.CLOTH, 0xF8F0D8);
        Paint holy = Paint.glowing(0xFFE070);
        // stock / tiller
        g.box(-1.0f, -3.2f, -10.0f, 2.0f, 2.4f, 18.0f, ivory.copy().decal(Face.LEFT, DecalType.SUN, 0xD6A83C, false, 0.3f, 0.5f, 0.5f)
                .decal(Face.RIGHT, DecalType.SUN, 0xD6A83C, false, 0.3f, 0.5f, 0.5f));
        g.box(-1.1f, -3.3f, 7.6f, 2.2f, 3.6f, 1.0f, goldP);
        // bolt magazine on top with a glowing bolt
        g.box(-1.1f, -6.2f, -6.0f, 2.2f, 3.0f, 6.0f, goldP.copy().trim(0x9A7A2A, 0.3f));
        g.box(-0.25f, -3.9f, -14.0f, 0.5f, 0.5f, 9.0f, holy);
        // prod (limbs) sweeping back from the front
        for (int side = -1; side <= 1; side += 2) {
            Part limb = g.sub(side * 1.0f, -2.2f, -9.4f).rot(0, side * -16, 0);
            limb.box(side < 0 ? -7.5f : 0, -0.5f, -0.6f, 7.5f, 1.0f, 1.2f, goldP);
            limb.box(side < 0 ? -8.2f : 7.2f, -0.8f, -0.8f, 1.0f, 1.6f, 1.6f, holy);
            Part str = g.sub(side * 8.0f, -2.2f, -7.2f).rot(0, side * 34, 0);
            str.box(side < 0 ? 0 : -8.6f, -0.12f, -0.12f, 8.6f, 0.24f, 0.24f, string);
        }
        g.box(-1.3f, -3.4f, -11.0f, 2.6f, 2.8f, 1.2f, goldP.copy().decal(Face.FRONT, DecalType.SUN, 0xFFF0B0, true, 0.5f, 0.5f, 1.0f));
        grip(g, goldP);
        trigger(g, -0.4f);
    }
}
