package com.lastbastion.client.model.gear;

import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.item.weapon.WeaponKind;

/**
 * 3D models for the melee arsenal. Conventions (model pixels, Y down): the centre of the grip is the
 * origin, the blade or head extends toward -Y, flat faces look along +/-Z and cutting edges face +/-X.
 * Everything hangs off a single {@code weapon} part so renderers can pose the whole weapon at once.
 */
public final class WeaponModels {
    private WeaponModels() {
    }

    public static int density(WeaponKind k) {
        return 6;
    }

    public static void build(WeaponKind k, ModelSpec s) {
        Part w = s.root.child("weapon", 0, 0, 0);
        switch (k) {
            case MILITIA_LONGSWORD -> longsword(w);
            case FROSTFANG -> frostfang(w);
            case BONECLEAVER -> bonecleaver(w);
            case VOIDEDGE -> voidedge(w);
            case STORMPIERCER -> stormpiercer(w);
            case EARTHSHAKER -> earthshaker(w);
            case DAWNBREAKER -> dawnbreaker(w);
            case SOULREAPER -> soulreaper(w);
        }
    }

    // ------------------------------------------------------------------ materials

    static Paint p(Mat m, int c) {
        return Paint.of(m, c);
    }

    static Paint steel(int c) {
        return p(Mat.METAL, c).gloss(0.75f).noise(0.025f);
    }

    static Paint edge(int c) {
        return p(Mat.METAL, c).gloss(0.95f).noise(0.015f).flat();
    }

    static Paint gold() {
        return p(Mat.GOLD, 0xD6A83C).accent(0xFFF0B8);
    }

    static Paint leather(int c, int band) {
        return p(Mat.LEATHER, c).pattern(Pattern.BANDS, band, 1.6f).flat();
    }

    // ------------------------------------------------------------------ shared parts

    /**
     * A straight double-edged blade from {@code y0} upward: core plate, bright bevelled edges, a fuller
     * groove down both flats and a tapered point.
     */
    static void blade(Part p, float y0, float len, float w, float d, float tip, Paint core, Paint bevel, Paint fuller, float fullerLen) {
        p.box(-w / 2f, y0 - len, -d / 2f, w, len, d, core);
        float bw = Math.max(0.28f, w * 0.14f);
        p.box(-w / 2f - bw * 0.5f, y0 - len, -d * 0.32f, bw, len, d * 0.64f, bevel);
        p.box(w / 2f - bw * 0.5f, y0 - len, -d * 0.32f, bw, len, d * 0.64f, bevel);
        if (fuller != null && fullerLen > 0) p.box(-w * 0.13f, y0 - 1.2f - fullerLen, -d / 2f - 0.04f, w * 0.26f, fullerLen, d + 0.08f, fuller);
        p.taper(0, y0 - len, 0, w + bw * 0.6f, d, w * 0.1f, d * 0.5f, tip, 9, bevel);
        p.taper(0, y0 - len, 0, w * 0.7f, d + 0.04f, w * 0.05f, d * 0.4f, tip * 0.8f, 7, core);
    }

    /** Wrapped grip: an octagonal core plus raised wrap ridges. */
    static void grip(Part p, float yTop, float len, float dia, Paint wrap, Paint ridge, int ridges) {
        p.octo(0, yTop, 0, dia, len, dia, wrap);
        for (int i = 0; i < ridges; i++) {
            float y = yTop + (i + 0.5f) * len / ridges;
            p.octo(0, y - 0.2f, 0, dia + 0.24f, 0.4f, dia + 0.24f, ridge);
        }
    }

    // ------------------------------------------------------------------ weapons

    /** Militia Longsword: practical steel arming sword, leather grip, brass-capped pommel. */
    static void longsword(Part w) {
        Paint core = steel(0xBCC4CE);
        Paint bevel = edge(0xF2F6FA);
        Paint fuller = steel(0x7E8794).gloss(0.5f);
        Paint guardSteel = steel(0x8E96A2).trim(0x5A616C, 0.25f);
        Paint brass = p(Mat.GOLD, 0xB88A3A).accent(0xF0D890);
        Paint wrap = leather(0x5C3A22, 0x3A2414);
        // pommel
        w.sphere(0, 6.4f, 0, 1.15f, guardSteel);
        w.octo(0, 7.4f, 0, 1.0f, 0.5f, 1.0f, brass);
        w.octo(0, 4.9f, 0, 1.2f, 0.6f, 1.2f, brass);
        grip(w, -2.6f, 7.6f, 1.45f, wrap, p(Mat.LEATHER, 0x46301C), 5);
        // crossguard with drooping quillons
        w.box(-3.0f, -3.9f, -0.8f, 6.0f, 1.1f, 1.6f, guardSteel);
        Part qr = w.sub(-2.9f, -3.35f, 0).rot(0, 0, -18);
        qr.box(-2.4f, -0.5f, -0.65f, 2.4f, 1.0f, 1.3f, guardSteel).sphere(-2.6f, 0, 0, 0.62f, brass);
        Part ql = w.sub(2.9f, -3.35f, 0).rot(0, 0, 18);
        ql.box(0, -0.5f, -0.65f, 2.4f, 1.0f, 1.3f, guardSteel).sphere(2.6f, 0, 0, 0.62f, brass);
        w.center(0, -3.35f, 0, 1.9f, 1.7f, 2.0f, brass.copy().decal(Face.FRONT, DecalType.GEM, 0xB02020, false, 0.5f, 0.5f, 1.3f)
                .decal(Face.BACK, DecalType.GEM, 0xB02020, false, 0.5f, 0.5f, 1.3f));
        // ricasso + blade
        w.box(-1.25f, -5.0f, -0.32f, 2.5f, 1.2f, 0.64f, core);
        blade(w, -4.6f, 19.6f, 2.4f, 0.56f, 3.8f, core, bevel, fuller, 14f);
    }

    /** Frostfang: a glacier-crystal fang on a silver hilt, frost spikes on the guard. */
    static void frostfang(Part w) {
        Paint ice = p(Mat.ICE, 0x9FDCF6).accent(0xFFFFFF).accentGlow().gloss(0.6f);
        Paint iceDeep = p(Mat.CRYSTAL, 0x5AAAD8).accent(0xD8F8FF).accentGlow();
        Paint silver = steel(0xD8E2EC);
        Paint wrap = leather(0x1E3450, 0x0E1C30);
        Paint coreGlow = Paint.glowing(0xC8F4FF);
        // pommel crystal
        w.spike(0, 3.9f, 0, 1.2f, 2.4f, 180, 0, 0, iceDeep);
        w.octo(0, 3.3f, 0, 1.3f, 0.6f, 1.3f, silver);
        grip(w, -1.3f, 4.6f, 1.25f, wrap, silver, 3);
        // guard: silver bar with ice spikes
        w.box(-2.6f, -2.1f, -0.65f, 5.2f, 0.9f, 1.3f, silver.copy().trim(0x8AA0B8, 0.2f));
        w.spike(-2.4f, -1.7f, 0, 0.9f, 2.6f, 0, 0, -48, ice);
        w.spike(2.4f, -1.7f, 0, 0.9f, 2.6f, 0, 0, 48, ice);
        w.spike(-1.4f, -2.0f, 0, 0.6f, 1.6f, 0, 0, -20, iceDeep);
        w.spike(1.4f, -2.0f, 0, 0.6f, 1.6f, 0, 0, 20, iceDeep);
        // the fang: faceted crystal blade with a glowing heart
        w.taper(0, -2.2f, 0, 2.3f, 0.7f, 0.25f, 0.3f, 11.5f, 9, ice);
        w.taper(0, -2.2f, 0, 1.2f, 0.82f, 0.1f, 0.4f, 9.5f, 6, iceDeep);
        w.box(-0.18f, -9.8f, -0.45f, 0.36f, 7.4f, 0.9f, coreGlow);
        // frost shards orbiting the base of the blade
        w.spike(0.9f, -2.6f, 0.5f, 0.45f, 1.4f, 20, 0, 35, ice);
        w.spike(-0.9f, -2.6f, -0.5f, 0.45f, 1.2f, -20, 0, -35, ice);
    }

    /** Bonecleaver: a crescent of yellowed bone lashed to an iron-shod haft. */
    static void bonecleaver(Part w) {
        Paint bone = p(Mat.BONE, 0xE4D8BC).accent(0x9A8A6A);
        Paint boneEdge = p(Mat.BONE, 0xF4ECD8).accent(0xB8A880).flat();
        Paint bloody = bone.copy().decal(Face.FRONT, DecalType.BLOOD, 0x7A0E0E, false, 0.35f, 0.62f, 0.9f)
                .decal(Face.BACK, DecalType.BLOOD, 0x7A0E0E, false, 0.6f, 0.4f, 0.8f);
        Paint haft = p(Mat.WOOD, 0x4A3222).accent(0x2E1E14);
        Paint iron = p(Mat.DARK_METAL, 0x4C4E54).gloss(0.4f);
        Paint wrap = leather(0x6A3A1E, 0x40200E);
        Paint rope = p(Mat.CLOTH, 0xB8A27A).pattern(Pattern.BANDS, 0x8A7450, 2.5f);
        // haft
        w.octo(0, -14.5f, 0, 1.7f, 27f, 1.7f, haft);
        grip(w, 2.0f, 8.5f, 1.95f, wrap, p(Mat.LEATHER, 0x4A2610), 5);
        w.octo(0, -6.5f, 0, 2.0f, 1.0f, 2.0f, iron);
        // pommel: knuckle bone with iron spikes
        w.sphere(0, 13.4f, 0, 1.35f, bone);
        w.spike(0, 14.2f, 0, 0.8f, 1.8f, 180, 0, 0, iron);
        // head socket lashed with rope
        w.box(-1.5f, -16.5f, -1.5f, 3.0f, 5.6f, 3.0f, iron.copy().trim(0x2A2C30, 0.3f));
        w.box(-1.65f, -13.6f, -1.65f, 3.3f, 1.0f, 3.3f, rope);
        w.spike(0, -16.4f, 0, 1.4f, 3.2f, 0, 0, 0, bone);
        // crescent blade: vertical slices that grow taller and thinner away from the haft
        int n = 7;
        float x = -1.5f, sw = 1.25f;
        for (int i = 0; i < n; i++) {
            float t = (i + 1) / (float) n;
            float hh = 4.6f + 8.2f * t * t;
            float dd = 1.5f - 0.8f * t;
            float yc = -13.6f - 0.7f * t;
            x -= sw;
            w.box(x, yc - hh / 2f, -dd / 2f, sw + 0.06f, hh, dd, i == 3 ? bloody : bone);
        }
        // the cutting edge: a bright rim running the full height, with hooked horns
        float eh = 4.6f + 8.2f + 1.0f, ey = -13.6f - 0.7f;
        w.box(x - 0.85f, ey - eh / 2f, -0.32f, 0.95f, eh, 0.64f, boneEdge);
        w.spike(x - 0.4f, ey - eh / 2f + 0.3f, 0, 0.7f, 2.0f, 0, 0, 38, boneEdge);
        w.spike(x - 0.4f, ey + eh / 2f - 0.3f, 0, 0.7f, 2.0f, 180, 0, 38, boneEdge);
        // notches chipped into the edge
        w.box(x - 0.9f, ey - 2.4f, -0.4f, 0.5f, 0.6f, 0.8f, bone.copy().dark(0.5f));
        w.box(x - 0.9f, ey + 1.9f, -0.4f, 0.5f, 0.5f, 0.8f, bone.copy().dark(0.5f));
        // iron binding plates and rivets on the blade root
        w.box(-3.4f, -16.2f, -0.85f, 2.0f, 5.6f, 1.7f, iron);
        // back spike
        w.spike(1.4f, -13.6f, 0, 1.1f, 3.6f, 0, 0, 90, bone);
    }

    /** Voidedge: a black katana with a living violet edge, dark gold fittings. */
    static void voidedge(Part w) {
        Paint black = p(Mat.DARK_METAL, 0x1C1824).gloss(0.7f).noise(0.02f);
        Paint edgeGlow = Paint.glowing(0xC86BFF);
        Paint hamon = p(Mat.METAL, 0x6A5A80).gloss(0.9f).flat();
        Paint darkGold = p(Mat.GOLD, 0x9A7A3A).accent(0xE8D090);
        Paint wrap = p(Mat.CLOTH, 0x2A1838).pattern(Pattern.HEX, 0x8A5AC8, 2.2f).flat();
        Paint same = p(Mat.LEATHER, 0xE8E0D0).flat();
        // kashira + tsuka (grip) with diamond wrap over ray-skin
        w.box(-0.85f, 9.6f, -0.65f, 1.7f, 0.9f, 1.3f, darkGold);
        w.box(-0.72f, -1.8f, -0.55f, 1.44f, 11.4f, 1.1f, same);
        w.box(-0.78f, -1.6f, -0.6f, 1.56f, 11.0f, 1.2f, wrap);
        w.box(-0.2f, 3.0f, -0.75f, 0.9f, 1.6f, 0.3f, darkGold);
        // tsuba (octagonal guard) with a void ring
        Part tsuba = w.sub(0, -2.25f, 0);
        tsuba.octo(0, -0.4f, 0, 4.4f, 0.8f, 3.6f, darkGold);
        tsuba.octo(0, -0.46f, 0, 3.2f, 0.92f, 2.6f, black);
        tsuba.octo(0, -0.5f, 0, 1.8f, 1.0f, 1.4f, Paint.glowing(0xB04AFF));
        // habaki
        w.box(-0.85f, -3.9f, -0.42f, 1.7f, 1.3f, 0.84f, darkGold);
        // curved blade: segments rotating progressively toward the spine (+X)
        Part seg = w.sub(0, -3.6f, 0);
        int n = 6;
        float segLen = 4.3f;
        for (int i = 0; i < n; i++) {
            float wd = 1.5f - i * 0.06f;
            seg.box(-wd / 2f, -segLen, -0.24f, wd, segLen + 0.05f, 0.48f, black);
            seg.box(-wd / 2f - 0.16f, -segLen, -0.15f, 0.34f, segLen + 0.05f, 0.3f, edgeGlow);
            seg.box(-wd / 2f + 0.2f, -segLen, -0.26f, wd * 0.35f, segLen + 0.05f, 0.52f, hamon);
            Part next = seg.sub(0, -segLen, 0).rot(0, 0, 2.6f);
            seg = next;
        }
        // kissaki (point)
        seg.taper(0, 0, 0, 1.2f, 0.48f, 0.1f, 0.3f, 3.2f, 5, black);
        seg.sub(-0.55f, 0, 0).rot(0, 0, -14).box(-0.2f, -3.2f, -0.15f, 0.34f, 3.3f, 0.3f, edgeGlow);
    }

    /** Stormpiercer: ash-wood spear with a leaf blade of storm-blue steel and a lightning channel. */
    static void stormpiercer(Part w) {
        Paint ash = p(Mat.WOOD, 0xD4C4A0).accent(0xA89470);
        Paint blueSteel = steel(0xA8C4E0);
        Paint bevel = edge(0xE8F4FF);
        Paint bandP = p(Mat.METAL, 0x5A7AA8).gloss(0.6f);
        Paint goldP = gold();
        Paint wrap = leather(0x2A4A7A, 0x18305A);
        Paint bolt = Paint.glowing(0xF8F48A);
        Paint cloth = p(Mat.CLOTH, 0x2A5AB0).accent(0x183A80).pattern(Pattern.STRIPES, 0xF4E07A, 1.5f);
        // shaft
        w.octo(0, -19f, 0, 1.3f, 36.5f, 1.3f, ash);
        grip(w, -1.0f, 7f, 1.55f, wrap, bandP, 4);
        for (float y : new float[]{-14f, -8f, 10f}) w.octo(0, y, 0, 1.6f, 0.7f, 1.6f, bandP);
        // butt spike
        w.octo(0, 17.2f, 0, 1.6f, 1.2f, 1.6f, bandP);
        w.spike(0, 18.3f, 0, 1.2f, 3.0f, 180, 0, 0, blueSteel);
        // socket with gold ring and winged lugs
        w.octo(0, -21.5f, 0, 1.9f, 2.6f, 1.9f, blueSteel);
        w.octo(0, -20.2f, 0, 2.2f, 0.7f, 2.2f, goldP);
        w.spike(-0.9f, -20.6f, 0, 0.7f, 2.6f, 0, 0, -62, blueSteel);
        w.spike(0.9f, -20.6f, 0, 0.7f, 2.6f, 0, 0, 62, blueSteel);
        // leaf blade
        w.taper(0, -21.4f, 0, 1.4f, 0.7f, 3.6f, 0.62f, 3.4f, 4, blueSteel);
        w.taper(0, -24.8f, 0, 3.6f, 0.62f, 0.2f, 0.3f, 9.6f, 9, blueSteel);
        w.taper(0, -24.8f, 0, 4.0f, 0.4f, 0.2f, 0.2f, 9.2f, 9, bevel);
        // lightning channel
        w.box(-0.22f, -32.4f, -0.4f, 0.44f, 10.6f, 0.8f, bolt);
        w.sub(0, -27f, 0).rot(0, 0, 30).box(-0.15f, -1.6f, -0.38f, 0.3f, 1.6f, 0.76f, bolt);
        w.sub(0, -29.8f, 0).rot(0, 0, -30).box(-0.15f, -1.4f, -0.38f, 0.3f, 1.4f, 0.76f, bolt);
        // pennant ribbons under the head
        w.sub(0.9f, -19.6f, 0.3f).rot(0, 0, -12).box(0, 0, -0.1f, 0.7f, 5.2f, 0.2f, cloth);
        w.sub(-0.9f, -19.6f, -0.3f).rot(0, 0, 12).box(-0.7f, 0, -0.1f, 0.7f, 4.4f, 0.2f, cloth);
    }

    /** Earthshaker: dwarven maul with a molten core breaking through blackened stone. */
    static void earthshaker(Part w) {
        Paint rock = p(Mat.DARK_METAL, 0x3A3436).pattern(Pattern.VEINS, 0xFF7A1A, 1.4f).gloss(0.2f);
        Paint face = p(Mat.STONE, 0x55504E).pattern(Pattern.CRACKS, 0xFF9A30, 1.2f).trim(0xC08A3A, 0.6f);
        Paint goldP = gold();
        Paint iron = p(Mat.DARK_METAL, 0x45474E).gloss(0.45f);
        Paint wrap = leather(0x5A2E1A, 0x3A1A0C);
        Paint magma = Paint.glowing(0xFF7A20);
        // handle
        w.octo(0, -10.5f, 0, 2.0f, 25f, 2.0f, iron);
        grip(w, 3.0f, 9.5f, 2.3f, wrap, p(Mat.LEATHER, 0x3A1A0C), 6);
        for (float y : new float[]{-8f, 1.6f, 13f}) w.octo(0, y, 0, 2.5f, 0.9f, 2.5f, goldP);
        // pommel: spiked iron cap with a magma eye
        w.octo(0, 14.2f, 0, 2.7f, 2.2f, 2.7f, iron);
        w.spike(0, 16.4f, 0, 1.4f, 2.0f, 180, 0, 0, goldP);
        w.center(0, 15.3f, -1.25f, 1.0f, 1.0f, 0.3f, magma);
        // head: a block of black rock with magma veins, gold-banded, with stone striking faces
        w.box(-5.5f, -18.2f, -3.4f, 11f, 7.2f, 6.8f, rock);
        w.box(-2.4f, -18.6f, -3.75f, 1.3f, 8.0f, 7.5f, goldP);
        w.box(1.1f, -18.6f, -3.75f, 1.3f, 8.0f, 7.5f, goldP);
        Paint runeFace = face.copy().decal(Face.RIGHT, DecalType.RUNE, 0xFF8A20, true, 0.5f, 0.5f, 0.55f)
                .decal(Face.LEFT, DecalType.RUNE, 0xFF8A20, true, 0.5f, 0.5f, 0.55f);
        w.box(-7.1f, -18.9f, -4.0f, 1.6f, 8.6f, 8.0f, runeFace);
        w.box(5.5f, -18.9f, -4.0f, 1.6f, 8.6f, 8.0f, runeFace);
        // molten seams between the plates
        w.box(-1.1f, -18.3f, -3.5f, 2.2f, 7.4f, 7.0f, magma);
        w.box(-1.6f, -18.35f, -3.45f, 3.2f, 7.5f, 6.9f, rock);
        // crown spikes
        w.spike(-3.6f, -18.2f, 0, 1.5f, 2.6f, 0, 0, -12, iron);
        w.spike(3.6f, -18.2f, 0, 1.5f, 2.6f, 0, 0, 12, iron);
        w.spike(0, -18.6f, 0, 1.8f, 3.4f, 0, 0, 0, goldP);
        // collar where the head meets the handle
        w.octo(0, -11.2f, 0, 3.2f, 1.2f, 3.2f, goldP);
    }

    /** Dawnbreaker: a winged greatsword of polished silver with a sun-gold rune fuller. */
    static void dawnbreaker(Part w) {
        Paint silver = steel(0xE6EAF0);
        Paint bevel = edge(0xFFFFFF);
        Paint inlay = p(Mat.GOLD, 0xD8A838).accent(0xFFF0B0).pattern(Pattern.RUNES, 0xFFF6C8, 2.4f);
        Paint goldP = gold();
        Paint wrap = p(Mat.LEATHER, 0xF2EEE4).pattern(Pattern.BANDS, 0xC8A050, 1.4f).flat();
        Paint sunGem = Paint.glowing(0xFFE070);
        Paint feather = p(Mat.GOLD, 0xE8C060).accent(0xFFF4C8).pattern(Pattern.FEATHERS, 0xB8862A, 2.0f);
        // pommel: sun disc
        w.sphere(0, 10.2f, 0, 1.6f, goldP);
        w.center(0, 10.2f, 0, 1.2f, 1.2f, 3.5f, sunGem);
        for (int i = 0; i < 6; i++) {
            Part ray = w.sub(0, 10.2f, 0).rot(0, 0, i * 60f);
            ray.box(-0.25f, -2.6f, -0.3f, 0.5f, 1.0f, 0.6f, goldP);
        }
        grip(w, -3.0f, 11.6f, 1.7f, wrap, goldP, 6);
        // guard: sun-gem block with feathered wings
        w.center(0, -4.3f, 0, 3.8f, 2.6f, 2.0f, goldP);
        w.center(0, -4.3f, 0, 1.4f, 1.4f, 2.3f, sunGem);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                Part f = w.sub(side * (1.9f + i * 1.3f), -4.6f + i * 0.2f, 0).rot(0, 0, side * (18f + i * 14f));
                float len = 3.4f - i * 0.5f;
                f.box(side < 0 ? -len : 0, -0.55f, -0.7f + i * 0.1f, len, 1.1f, 1.4f - i * 0.2f, feather);
            }
        }
        // blade
        w.box(-1.6f, -7.0f, -0.4f, 3.2f, 1.6f, 0.8f, goldP);
        blade(w, -6.6f, 27.5f, 3.7f, 0.72f, 5.2f, silver, bevel, null, 0);
        w.box(-0.5f, -27.5f, -0.4f, 1.0f, 20.5f, 0.8f, inlay);
    }

    /** Soulreaper: a spectral scythe on a black-wood snath, a bone skull binding the blade. */
    static void soulreaper(Part w) {
        Paint snath = p(Mat.WOOD, 0x2E2228).accent(0x1A1218);
        Paint bone = p(Mat.BONE, 0xDCD4C0).accent(0x8A8070);
        Paint skull = p(Mat.BONE, 0xE4DCC8).accent(0x8A8070)
                .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x5AFFD8, true, 0.5f, 0.42f, 1.0f)
                .decal(Face.FRONT, DecalType.GRIN, 0x000000, false, 0.5f, 0.82f, 0.8f)
                .decal(Face.BACK, DecalType.HOLLOW_EYES, 0x5AFFD8, true, 0.5f, 0.42f, 1.0f);
        Paint blackSteel = p(Mat.DARK_METAL, 0x22262C).gloss(0.6f).pattern(Pattern.VEINS, 0x3AD8B0, 1.6f);
        Paint edgeGlow = Paint.glowing(0x6AFFE0);
        Paint chain = p(Mat.METAL, 0x6A6E78).pattern(Pattern.CHAINMAIL, 0x3A3E46, 2.5f);
        Paint wrap = leather(0x2A3A38, 0x18242A);
        // snath with bone rings
        w.octo(0, -19.5f, 0, 1.4f, 41f, 1.4f, snath);
        grip(w, -1.5f, 6.5f, 1.6f, wrap, bone, 4);
        for (float y : new float[]{-12f, 8f, 15f}) w.octo(0, y, 0, 1.75f, 0.6f, 1.75f, bone);
        // upper nib (second handle) sticking out of the snath
        w.sub(0, -8.5f, 0).rot(70, 0, 0).octo(0, -3.6f, 0, 0.9f, 3.8f, 0.9f, bone);
        // butt: bone spike
        w.spike(0, 21.4f, 0, 1.2f, 3.0f, 180, 0, 0, bone);
        // skull binding at the top
        w.box(-1.7f, -23.4f, -1.7f, 3.4f, 3.2f, 3.4f, skull);
        w.box(-1.2f, -20.4f, -1.4f, 2.4f, 1.0f, 2.2f, bone);
        w.spike(-1.3f, -23.2f, 0, 0.7f, 1.6f, 0, 0, -30, bone);
        w.spike(1.3f, -23.2f, 0, 0.7f, 1.6f, 0, 0, 30, bone);
        // chain hanging from the binding
        w.sub(1.4f, -20.6f, 0).rot(0, 0, -8).box(-0.25f, 0, -0.25f, 0.5f, 5.5f, 0.5f, chain);
        // the blade: sweeps out toward -X and curls downward, edge glowing on the inside
        Part seg = w.sub(-1.6f, -22.2f, 0).rot(0, 0, -6);
        float[] h = {3.2f, 3.0f, 2.7f, 2.3f, 1.9f, 1.4f, 0.9f};
        for (int i = 0; i < h.length; i++) {
            float len = 2.6f;
            seg.box(-len, -h[i] / 2f, -0.3f, len + 0.05f, h[i], 0.6f, blackSteel);
            seg.box(-len, h[i] / 2f - 0.15f, -0.2f, len + 0.05f, 0.5f, 0.4f, edgeGlow);
            seg = seg.sub(-len, 0.1f, 0).rot(0, 0, 9f + i * 3f);
        }
        seg.spike(0, 0, 0, 0.9f, 2.6f, 0, 0, 90, blackSteel);
    }
}
