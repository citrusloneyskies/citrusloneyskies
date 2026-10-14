package com.lastbastion.client.model.gear;

import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.item.armor.ArmorSet;

/**
 * Worn 3D models for the armour sets. Each (set, slot) gets its own spec with the seven vanilla
 * humanoid parts at their vanilla pivots (head, hat, body, right_arm, left_arm, right_leg, left_leg) so
 * the armour layer can pose it like the player; only the parts for that slot get geometry.
 *
 * <p>Vanilla reference boxes: head (-4,-8,-4) 8x8x8, body (-4,0,-2) 8x12x4, right arm pivot (-5,2,0)
 * box (-3,-2,-2) 4x12x4, left arm pivot (5,2,0) box (-1,-2,-2), legs pivot (+-1.9,12,0) box (-2,0,-2) 4x12x4.
 */
public final class ArmorModels {
    public static final int HELMET = 0, CHEST = 1, LEGS = 2, BOOTS = 3;

    private ArmorModels() {
    }

    /** The seven humanoid parts of one armour model. */
    public static final class Rig {
        public final Part head, hat, body, rArm, lArm, rLeg, lLeg;

        Rig(ModelSpec s) {
            head = s.root.child("head", 0, 0, 0);
            hat = s.root.child("hat", 0, 0, 0);
            body = s.root.child("body", 0, 0, 0);
            rArm = s.root.child("right_arm", -5, 2, 0);
            lArm = s.root.child("left_arm", 5, 2, 0);
            rLeg = s.root.child("right_leg", -1.9f, 12, 0);
            lLeg = s.root.child("left_leg", 1.9f, 12, 0);
        }

        Part arm(int side) {
            return side < 0 ? rArm : lArm;
        }

        Part leg(int side) {
            return side < 0 ? rLeg : lLeg;
        }
    }

    public static Rig build(ArmorSet set, int slot, ModelSpec s) {
        Rig r = new Rig(s);
        switch (set) {
            case SCAVENGER -> scavenger(r, slot);
            case SHADOWSTALKER -> shadowstalker(r, slot);
            case ARCANIST -> arcanist(r, slot);
            case VANGUARD -> vanguard(r, slot);
            case SUNFORGED -> sunforged(r, slot);
            case TITAN -> titan(r, slot);
        }
        return r;
    }

    // ------------------------------------------------------------------ helpers

    static Paint p(Mat m, int c) {
        return Paint.of(m, c);
    }

    /** Arm centre X in the arm's own frame: the right arm's box sits at x -3..1, the left at -1..3. */
    static float ax(int side) {
        return side < 0 ? -1f : 1f;
    }

    /** A plate shell around the head (open-faced if {@code open}), {@code g} px out from the skin. */
    static void helmShell(Part h, float g, Paint plate, boolean open) {
        float x0 = -4 - g, y0 = -8 - g, z0 = -4 - g, w = 8 + 2 * g;
        h.box(x0, y0, z0, w, 2.2f + g, w, plate);
        h.box(x0, y0 + 2.2f + g, -4 + g * 0.2f, g + 0.01f + 0.2f, 6.2f, 8 + g * 0.8f, plate);
        h.box(4 - 0.2f, y0 + 2.2f + g, -4 + g * 0.2f, g + 0.2f, 6.2f, 8 + g * 0.8f, plate);
        h.box(x0, y0 + 2.2f + g, 4 - 0.2f, w, 6.2f, g + 0.2f, plate);
        if (!open) h.box(x0, y0 + 2.2f + g, z0, w, 6.2f, g + 0.2f, plate);
    }

    /** Torso shell: front/back/side plates around the vanilla body box. */
    static void torso(Part b, float g, float y0, float h, Paint front, Paint back, Paint side) {
        b.box(-4 - g, y0, -2 - g, 8 + 2 * g, h, g + 0.3f, front);
        b.box(-4 - g, y0, 2 - 0.3f, 8 + 2 * g, h, g + 0.3f, back);
        b.box(-4 - g, y0, -2 - g + 0.3f, g + 0.3f, h, 4 + 2 * g - 0.6f, side);
        b.box(4 - 0.3f, y0, -2 - g + 0.3f, g + 0.3f, h, 4 + 2 * g - 0.6f, side);
    }

    /** Sleeve around an arm from {@code y0} down by {@code len}. */
    static void sleeve(Part a, int side, float g, float y0, float len, Paint p) {
        a.box(ax(side) - 2 - g, y0, -2 - g, 4 + 2 * g, len, 4 + 2 * g, p);
    }

    /** Leg wrap from {@code y0} down by {@code len}. */
    static void legWrap(Part l, float g, float y0, float len, Paint p) {
        l.box(-2 - g, y0, -2 - g, 4 + 2 * g, len, 4 + 2 * g, p);
    }

    /** Layered shoulder plates. */
    static void pauldron(Part a, int side, int layers, float size, Paint p, Paint trim) {
        float cx = ax(side);
        for (int i = 0; i < layers; i++) {
            Part pl = a.sub(cx + side * (0.6f + i * 0.4f), -2.6f + i * 1.6f, 0).rot(0, 0, side * (18 + i * 8));
            float w = size - i * 0.6f;
            pl.box(-w / 2f, -0.8f, -size * 0.36f - 0.3f, w, 1.6f, size * 0.72f + 0.6f, i == 0 ? p : p.copy().trim(trim.base, 0.3f));
        }
    }

    /** Boot with a toe cap reaching forward. */
    static void boot(Part l, float g, float h, Paint p, Paint toe) {
        l.box(-2 - g, 12 - h, -2 - g, 4 + 2 * g, h + 0.01f, 4 + 2 * g, p);
        l.box(-2 - g * 0.8f, 12 - 1.6f, -3.0f - g, 4 + 1.6f * g, 1.6f, 1.6f, toe);
    }

    // ------------------------------------------------------------------ Scavenger

    static void scavenger(Rig r, int slot) {
        Paint leather = p(Mat.LEATHER, 0x7A5634).accent(0x4A3018).grime(0.35f);
        Paint darkLeather = p(Mat.LEATHER, 0x4A3220).accent(0x2A1A0C);
        Paint scrap = p(Mat.METAL, 0x8A7A62).pattern(Pattern.SPOTS, 0x7A4A22, 1.4f).grime(0.5f).gloss(0.25f);
        Paint cloth = p(Mat.CLOTH, 0x6A7048).accent(0x4A5030).pattern(Pattern.CLOTH, 0x50563A, 1.2f);
        Paint lens = p(Mat.GLASS, 0xE0A040).accent(0xFFF0C0).accentGlow();
        Paint brass = p(Mat.GOLD, 0xA88A40).accent(0xE8D090);
        switch (slot) {
            case HELMET -> {
                Part h = r.head;
                h.box(-4.5f, -8.6f, -4.5f, 9, 2.8f, 9, leather.copy().decal(Face.TOP, DecalType.STITCH, 0, false, 0.5f, 0.5f, 1f));
                h.box(-4.6f, -6.0f, 1.0f, 9.2f, 3.4f, 3.6f, leather);
                h.box(-4.6f, -6.2f, -2.4f, 1.0f, 4.8f, 3.6f, leather);
                h.box(3.6f, -6.2f, -2.4f, 1.0f, 4.8f, 3.6f, leather);
                // goggles pushed up on the brow
                h.box(-4.7f, -7.6f, -4.9f, 9.4f, 1.0f, 0.6f, darkLeather);
                for (int sd = -1; sd <= 1; sd += 2) {
                    h.octoZ(sd * 1.8f, -7.4f, -5.6f, 2.4f, 0.9f, brass);
                    h.octoZ(sd * 1.8f, -7.4f, -5.75f, 1.8f, 0.3f, lens);
                }
            }
            case CHEST -> {
                torso(r.body, 0.6f, -0.4f, 12.6f, leather.copy().decal(Face.FRONT, DecalType.STITCH, 0, false, 0.3f, 0.6f, 0.8f),
                        leather.copy().decal(Face.BACK, DecalType.PANEL, 0, false, 0.5f, 0.5f, 1f), leather);
                // scrap breastplate, bandolier and a mismatched pauldron
                r.body.sub(1.2f, 3.4f, -2.7f).rot(0, 0, 8).box(-2.0f, -2.2f, -0.5f, 4.0f, 4.4f, 0.8f, scrap);
                Part band = r.body.sub(0, 6, -2.8f).rot(0, 0, 38);
                band.box(-0.7f, -7.5f, -0.2f, 1.4f, 15f, 0.6f, darkLeather);
                for (int i = 0; i < 4; i++) band.box(-0.5f, -5.5f + i * 2.6f, -0.6f, 1.0f, 1.4f, 0.5f, brass);
                pauldron(r.rArm, -1, 2, 5.4f, scrap, brass);
                sleeve(r.rArm, -1, 0.55f, -2.4f, 6.5f, leather);
                sleeve(r.lArm, 1, 0.55f, -2.4f, 6.5f, leather);
                sleeve(r.rArm, -1, 0.35f, 6.5f, 3.6f, cloth);
                sleeve(r.lArm, 1, 0.35f, 6.5f, 3.6f, cloth);
                r.lArm.box(ax(1) + 2.1f, 0, -1.2f, 0.6f, 3.0f, 2.4f, scrap);
            }
            case LEGS -> {
                r.body.box(-4.6f, 9.6f, -2.6f, 9.2f, 1.8f, 5.2f, darkLeather.copy().decal(Face.FRONT, DecalType.GEM, 0xA88A40, false, 0.5f, 0.5f, 1.4f));
                r.body.box(-4.9f, 10.2f, -1.6f, 1.2f, 2.6f, 2.8f, cloth);
                r.body.box(3.7f, 10.2f, -1.6f, 1.2f, 2.6f, 2.8f, cloth);
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part l = r.leg(sd);
                    legWrap(l, 0.45f, -0.2f, 9.2f, cloth);
                    l.box(-1.8f, 4.6f, -2.8f, 3.6f, 2.6f, 0.8f, scrap);
                    l.box(sd * 2.1f - 0.4f, 1.0f, -1.4f, 0.8f, 2.6f, 2.8f, darkLeather);
                }
            }
            default -> {
                for (int sd = -1; sd <= 1; sd += 2) {
                    boot(r.leg(sd), 0.55f, 3.8f, darkLeather, scrap);
                    r.leg(sd).box(-2.6f, 8.1f, -2.6f, 5.2f, 0.6f, 5.2f, leather);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Shadowstalker

    static void shadowstalker(Rig r, int slot) {
        Paint hood = p(Mat.CLOTH, 0x2A2236).accent(0x16121E).pattern(Pattern.CLOTH, 0x221C2C, 1.3f);
        Paint leather = p(Mat.LEATHER, 0x2E2630).accent(0x1A141C);
        Paint strap = p(Mat.LEATHER, 0x4A3A2A).accent(0x2A1E14);
        Paint mask = p(Mat.CLOTH, 0x1A1622).decal(Face.FRONT, DecalType.STITCH, 0, false, 0.5f, 0.5f, 0.6f);
        Paint steel = p(Mat.METAL, 0x8A8E9A).gloss(0.6f);
        Paint violet = Paint.glowing(0x9A6AE0);
        switch (slot) {
            case HELMET -> {
                Part h = r.head;
                helmShell(h, 0.7f, hood, true);
                // the hood's peak and drape
                h.sub(0, -8.6f, -4.4f).rot(-28, 0, 0).box(-2.4f, -0.6f, -2.6f, 4.8f, 1.2f, 3.2f, hood);
                h.box(-4.4f, -2.4f, 3.4f, 8.8f, 3.6f, 1.6f, hood);
                // mask over nose and mouth, with violet thread
                h.box(-4.3f, -4.2f, -4.9f, 8.6f, 3.6f, 0.7f, mask);
                h.box(-4.3f, -4.3f, -4.95f, 8.6f, 0.3f, 0.2f, violet);
            }
            case CHEST -> {
                torso(r.body, 0.5f, -0.3f, 12.4f, leather, leather, leather);
                // crossed straps with throwing knives
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part st = r.body.sub(0, 6, -2.7f).rot(0, 0, sd * 32);
                    st.box(-0.6f, -7f, -0.2f, 1.2f, 14f, 0.5f, strap);
                }
                for (int i = 0; i < 3; i++) r.body.sub(-1.8f + i * 1.8f, 2.4f, -2.9f).rot(0, 0, 20).box(-0.25f, -1.6f, -0.2f, 0.5f, 3.2f, 0.4f, steel);
                // short cloak over the shoulders, hanging down the back
                r.body.box(-4.8f, -0.6f, 2.0f, 9.6f, 1.6f, 1.0f, hood);
                r.body.sub(0, 0.6f, 2.6f).rot(8, 0, 0).box(-4.6f, 0, 0, 9.2f, 13.5f, 0.6f, hood.copy().trim(0x5A3A8A, 0.4f));
                for (int sd = -1; sd <= 1; sd += 2) {
                    sleeve(r.arm(sd), sd, 0.45f, -2.3f, 7.0f, leather);
                    sleeve(r.arm(sd), sd, 0.55f, 5.6f, 4.2f, strap.copy().pattern(Pattern.BANDS, 0x2A1E14, 1.8f));
                    r.arm(sd).box(ax(sd) - 2.5f, -2.6f, -2.5f, 5.0f, 1.4f, 5.0f, hood);
                }
            }
            case LEGS -> {
                r.body.box(-4.5f, 9.8f, -2.5f, 9f, 1.4f, 5f, strap);
                r.body.box(1.4f, 10.4f, -2.9f, 2.0f, 2.6f, 0.8f, leather);
                for (int sd = -1; sd <= 1; sd += 2) {
                    legWrap(r.leg(sd), 0.4f, -0.2f, 10.4f, leather);
                    r.leg(sd).box(-1.6f, 4.2f, -2.7f, 3.2f, 2.4f, 0.6f, steel);
                }
                r.rLeg.box(-2.6f, 1.0f, -1.0f, 0.5f, 4.0f, 1.6f, steel);
            }
            default -> {
                for (int sd = -1; sd <= 1; sd += 2) {
                    boot(r.leg(sd), 0.5f, 4.6f, leather, leather);
                    for (int i = 0; i < 3; i++) r.leg(sd).box(-2.55f, 7.8f + i * 1.3f, -2.55f, 5.1f, 0.4f, 5.1f, strap);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Arcanist

    static void arcanist(Rig r, int slot) {
        Paint robe = p(Mat.ROBE, 0x3A2A6A).accent(0x24183E).pattern(Pattern.RUNES, 0x8A5AE0, 0.9f).accentGlow();
        Paint robePlain = p(Mat.ROBE, 0x3A2A6A).accent(0x24183E);
        Paint gold = p(Mat.GOLD, 0xD6A83C).accent(0xFFF0B8);
        Paint trim = p(Mat.CLOTH, 0xD8C8F0).accent(0xA898C8);
        Paint crystal = p(Mat.CRYSTAL, 0x8A5AFF).accent(0xE8D8FF).accentGlow();
        Paint gem = Paint.glowing(0xC8A0FF);
        switch (slot) {
            case HELMET -> {
                Part h = r.head;
                // circlet with a glowing gem, and a hood thrown back
                h.box(-4.4f, -6.8f, -4.5f, 8.8f, 0.9f, 0.5f, gold);
                h.box(-4.5f, -6.8f, -4.4f, 0.5f, 0.9f, 8.8f, gold);
                h.box(4.0f, -6.8f, -4.4f, 0.5f, 0.9f, 8.8f, gold);
                h.box(-4.4f, -6.8f, 4.0f, 8.8f, 0.9f, 0.5f, gold);
                h.sub(0, -6.9f, -4.7f).rot(0, 0, 45).box(-0.8f, -0.8f, -0.4f, 1.6f, 1.6f, 0.6f, gem);
                h.spike(0, -7.4f, -4.6f, 0.7f, 1.6f, 0, 0, 0, gold);
                h.sub(0, -1.0f, 4.2f).rot(20, 0, 0).box(-4.4f, -6.5f, 0, 8.8f, 7.0f, 1.6f, robePlain.copy().trim(0xD8C8F0, 0.4f));
            }
            case CHEST -> {
                torso(r.body, 0.55f, -0.4f, 12.6f, robe, robe, robePlain);
                // high collar, shoulder mantle, a crystal orbiting the right shoulder
                r.body.box(-4.8f, -1.6f, -2.8f, 9.6f, 1.6f, 5.6f, trim);
                r.body.box(-3.0f, -2.6f, 1.6f, 6f, 2.4f, 1.4f, gold);
                r.body.box(-1.2f, 0.4f, -2.75f, 2.4f, 11f, 0.3f, trim.copy().decal(Face.FRONT, DecalType.EYE_SIGIL, 0xB89AFF, true, 0.5f, 0.25f, 1.2f));
                Part orb = r.body.sub(-6.6f, -3.6f, 2.4f).rot(20, 30, 15);
                orb.taper(0, 0, 0, 1.6f, 1.6f, 0.2f, 0.2f, 2.6f, 4, crystal);
                orb.sub(0, 0, 0).rot(180, 0, 0).taper(0, 0, 0, 1.6f, 1.6f, 0.2f, 0.2f, 1.8f, 3, crystal);
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part a = r.arm(sd);
                    a.box(ax(sd) - 2.9f, -2.8f, -2.9f, 5.8f, 2.2f, 5.8f, robePlain.copy().trim(0xD6A83C, 0.4f));
                    sleeve(a, sd, 0.45f, -0.6f, 6.0f, robePlain);
                    // bell sleeves flare at the wrist
                    a.box(ax(sd) - 2.9f, 5.2f, -2.9f, 5.8f, 4.6f, 5.8f, robePlain.copy().trim(0xD8C8F0, 0.5f));
                }
            }
            case LEGS -> {
                // sash with hanging tabards: the robe falls past the knees
                r.body.box(-4.6f, 9.6f, -2.6f, 9.2f, 1.6f, 5.2f, gold);
                r.body.box(-2.4f, 11.0f, -2.9f, 4.8f, 8.5f, 0.5f, robe.copy().trim(0xD8C8F0, 0.4f));
                r.body.box(-2.4f, 11.0f, 2.4f, 4.8f, 8.5f, 0.5f, robe.copy().trim(0xD8C8F0, 0.4f));
                for (int sd = -1; sd <= 1; sd += 2) legWrap(r.leg(sd), 0.5f, -0.4f, 9.5f, robePlain);
            }
            default -> {
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part l = r.leg(sd);
                    l.box(-2.4f, 9.6f, -2.4f, 4.8f, 2.6f, 4.8f, robePlain);
                    l.sub(0, 11.2f, -2.6f).rot(-30, 0, 0).box(-1.3f, -0.4f, -2.0f, 2.6f, 0.8f, 2.4f, gold);
                    l.box(-2.5f, 9.3f, -2.5f, 5.0f, 0.5f, 5.0f, gold);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Vanguard

    static void vanguard(Rig r, int slot) {
        Paint plate = p(Mat.PLATE, 0xB4BAC4).gloss(0.6f);
        Paint plateDark = p(Mat.METAL, 0x7E8490).gloss(0.5f);
        Paint tabard = p(Mat.CLOTH, 0x2A4A9A).accent(0x1A3070).trim(0xD6A83C, 0.4f);
        Paint gold = p(Mat.GOLD, 0xC89A3A).accent(0xF0D890);
        Paint plume = p(Mat.FEATHER, 0xB02020).accent(0x701010);
        Paint mail = p(Mat.METAL, 0x6A6E78).pattern(Pattern.CHAINMAIL, 0x3A3E46, 2.5f);
        switch (slot) {
            case HELMET -> {
                Part h = r.head;
                Paint helm = plate.copy().decal(Face.FRONT, DecalType.VISOR, 0x0A0A0E, false, 0.5f, 0.42f, 1f)
                        .decal(Face.FRONT, DecalType.GRILLE, 0x14141A, false, 0.3f, 0.74f, 0.45f)
                        .decal(Face.FRONT, DecalType.GRILLE, 0x14141A, false, 0.7f, 0.74f, 0.45f);
                h.box(-4.8f, -8.9f, -4.8f, 9.6f, 9.4f, 9.6f, helm);
                h.box(-0.35f, -8.6f, -5.1f, 0.7f, 8.6f, 0.4f, gold);
                h.box(-4.9f, -6.0f, -5.0f, 9.8f, 0.6f, 0.4f, gold);
                // crest and plume
                h.box(-0.5f, -10.0f, -4.0f, 1.0f, 1.2f, 8.0f, gold);
                for (int i = 0; i < 6; i++) h.sub(0, -10.2f, -3.4f + i * 1.4f).rot(-30 + i * 14, 0, 0).box(-0.6f, -2.6f, -0.5f, 1.2f, 2.8f, 1.0f, plume);
            }
            case CHEST -> {
                torso(r.body, 0.7f, -0.5f, 10.6f, plate.copy().decal(Face.FRONT, DecalType.EMBLEM, 0xD6A83C, false, 0.5f, 0.35f, 0.9f), plate, plateDark);
                r.body.box(-4.4f, -1.2f, -2.4f, 8.8f, 1.6f, 4.8f, gold);
                // tabard over the lower torso, back and front
                r.body.box(-3.2f, 6.0f, -2.95f, 6.4f, 7.4f, 0.4f, tabard.copy().decal(Face.FRONT, DecalType.CROSS, 0xF0D890, false, 0.5f, 0.45f, 1.0f));
                r.body.box(-3.2f, 6.0f, 2.55f, 6.4f, 7.4f, 0.4f, tabard);
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part a = r.arm(sd);
                    pauldron(a, sd, 3, 6.4f, plate, gold);
                    sleeve(a, sd, 0.35f, 0.4f, 4.0f, mail);
                    sleeve(a, sd, 0.6f, 4.4f, 5.4f, plate.copy().trim(0xC89A3A, 0.3f));
                    a.box(ax(sd) - 2.2f, 9.6f, -2.3f, 4.4f, 1.0f, 4.6f, plateDark);
                }
            }
            case LEGS -> {
                r.body.box(-4.7f, 9.4f, -2.7f, 9.4f, 1.6f, 5.4f, plateDark);
                // tassets (hip plates) over the thighs
                for (int sd = -1; sd <= 1; sd += 2) {
                    r.body.sub(sd * 2.2f, 10.8f, -2.8f).rot(-8, 0, sd * -6).box(-2.2f, 0, -0.4f, 4.4f, 3.6f, 0.8f, plate.copy().trim(0xC89A3A, 0.3f));
                    Part l = r.leg(sd);
                    legWrap(l, 0.45f, 0, 5.6f, plate);
                    legWrap(l, 0.35f, 5.6f, 3.4f, mail);
                    // poleyn (knee cop) with a side fan
                    l.box(-1.8f, 4.6f, -2.9f, 3.6f, 2.6f, 1.2f, plate.copy().trim(0xC89A3A, 0.25f));
                    l.box(sd * 2.2f - 0.3f, 4.8f, -2.0f, 0.6f, 2.2f, 2.6f, plateDark);
                }
            }
            default -> {
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part l = r.leg(sd);
                    boot(l, 0.65f, 4.2f, plate, plateDark);
                    for (int i = 0; i < 3; i++) l.box(-2.4f + i * 0.2f, 10.6f, -3.2f - i * 0.6f, 4.8f - i * 0.4f, 1.4f, 0.6f, plate);
                    l.box(-2.7f, 7.6f, -2.7f, 5.4f, 0.6f, 5.4f, gold);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Sunforged

    static void sunforged(Rig r, int slot) {
        Paint white = p(Mat.PLATE, 0xEDEAE2).gloss(0.6f);
        Paint gold = p(Mat.GOLD, 0xE0B040).accent(0xFFF4C8);
        Paint feather = p(Mat.FEATHER, 0xF4F0E4).accent(0xC8B888);
        Paint sun = Paint.glowing(0xFFE070);
        Paint cape = p(Mat.CLOTH, 0xE8DCC0).accent(0xB8A880).trim(0xE0B040, 0.5f);
        switch (slot) {
            case HELMET -> {
                Part h = r.head;
                Paint helm = white.copy().decal(Face.FRONT, DecalType.CROSS, 0x2A2620, false, 0.5f, 0.55f, 1.3f).trim(0xE0B040, 0.35f);
                h.box(-4.7f, -8.8f, -4.7f, 9.4f, 9.2f, 9.4f, helm);
                // wings on the sides
                for (int sd = -1; sd <= 1; sd += 2) {
                    for (int i = 0; i < 4; i++) {
                        Part f = h.sub(sd * 4.8f, -6.0f + i * 0.4f, 1.0f + i * 0.6f).rot(-20 - i * 12, sd * 20, sd * (-25 - i * 6));
                        f.box(sd < 0 ? -0.5f : -0.5f, -4.6f + i * 0.6f, -0.6f, 1.0f, 4.6f - i * 0.6f, 1.2f, feather);
                    }
                }
                // floating halo
                Part halo = h.sub(0, -11.2f, 0);
                halo.octoRing(0, 7.4f, 0.7f, 0.6f, sun);
                h.box(-0.6f, -9.4f, -5.0f, 1.2f, 1.6f, 0.5f, sun);
            }
            case CHEST -> {
                torso(r.body, 0.7f, -0.5f, 11f, white.copy().decal(Face.FRONT, DecalType.SUN, 0xFFE070, true, 0.5f, 0.35f, 1.2f).trim(0xE0B040, 0.35f),
                        white, white);
                r.body.box(-4.5f, -1.3f, -2.5f, 9f, 1.6f, 5f, gold);
                // cape
                r.body.sub(0, -0.2f, 2.9f).rot(6, 0, 0).box(-4.6f, 0, 0, 9.2f, 15f, 0.6f, cape.copy().decal(Face.BACK, DecalType.SUN, 0xE0B040, false, 0.5f, 0.3f, 1f));
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part a = r.arm(sd);
                    pauldron(a, sd, 2, 6.2f, white, gold);
                    sleeve(a, sd, 0.35f, 0.2f, 4.2f, p(Mat.METAL, 0xC8C0A8).pattern(Pattern.CHAINMAIL, 0x8A806A, 2.5f));
                    // sun-ray spikes rising from the pauldron
                    for (int i = 0; i < 3; i++) a.spike(ax(sd) + sd * (1.0f + i * 0.6f), -3.4f, -1.2f + i * 1.2f, 0.8f, 2.4f - i * 0.3f, 0, 0, sd * (20 + i * 12), gold);
                    sleeve(a, sd, 0.6f, 4.2f, 5.8f, white.copy().trim(0xE0B040, 0.3f));
                    a.box(ax(sd) - 2.3f, 6.2f, -2.35f, 4.6f, 0.8f, 4.7f, gold);
                }
            }
            case LEGS -> {
                r.body.box(-4.7f, 9.4f, -2.7f, 9.4f, 1.6f, 5.4f, gold);
                r.body.box(-1.6f, 10.6f, -2.95f, 3.2f, 5.6f, 0.4f, cape.copy().decal(Face.FRONT, DecalType.SUN, 0xE0B040, false, 0.5f, 0.3f, 0.9f));
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part l = r.leg(sd);
                    legWrap(l, 0.5f, 0, 9.0f, white.copy().trim(0xE0B040, 0.3f));
                    l.box(-1.8f, 4.4f, -3.0f, 3.6f, 2.8f, 1.2f, gold);
                }
            }
            default -> {
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part l = r.leg(sd);
                    boot(l, 0.65f, 4.2f, white, gold);
                    l.box(-2.7f, 7.6f, -2.7f, 5.4f, 0.6f, 5.4f, gold);
                    l.spike(sd * 2.6f, 9.0f, 1.0f, 0.6f, 1.8f, 0, 0, sd * 70, gold);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Titan

    static void titan(Rig r, int slot) {
        Paint armor = p(Mat.PLATE, 0x4A5260).gloss(0.5f);
        Paint panel = p(Mat.PLATE, 0x6A7484).gloss(0.5f).pattern(Pattern.PLATES, 0x3A424E, 1.2f);
        Paint hazard = p(Mat.PLATE, 0x262626).pattern(Pattern.HAZARD, 0xE8B820, 1.6f);
        Paint dark = p(Mat.DARK_METAL, 0x24282E).gloss(0.4f);
        Paint energy = Paint.glowing(0x50C8FF);
        Paint piston = p(Mat.METAL, 0xC8CCD4).gloss(0.8f);
        switch (slot) {
            case HELMET -> {
                Part h = r.head;
                h.box(-4.8f, -8.9f, -4.8f, 9.6f, 9.4f, 9.6f, armor.copy().decal(Face.FRONT, DecalType.VISOR, 0x50C8FF, true, 0.5f, 0.4f, 1f)
                        .decal(Face.FRONT, DecalType.GRILLE, 0x14141A, false, 0.5f, 0.78f, 0.8f));
                h.box(-5.2f, -6.0f, -2.5f, 0.6f, 4.0f, 5.0f, panel);
                h.box(4.6f, -6.0f, -2.5f, 0.6f, 4.0f, 5.0f, panel);
                h.box(-3.0f, -9.6f, -3.0f, 6.0f, 0.8f, 6.0f, panel);
                h.box(4.8f, -12.0f, 1.0f, 0.4f, 6.0f, 0.4f, piston);
                h.box(4.7f, -12.4f, 0.9f, 0.6f, 0.6f, 0.6f, Paint.glowing(0xFF4030));
            }
            case CHEST -> {
                torso(r.body, 0.9f, -0.8f, 11.4f, armor.copy().decal(Face.FRONT, DecalType.CORE, 0x50C8FF, true, 0.5f, 0.35f, 0.9f), armor, panel);
                r.body.box(-3.6f, 8.0f, -3.05f, 7.2f, 2.6f, 0.4f, hazard);
                // thruster pack
                for (int sd = -1; sd <= 1; sd += 2) {
                    r.body.octo(sd * 2.4f, 0.2f, 4.4f, 2.6f, 8.0f, 2.6f, dark);
                    r.body.octo(sd * 2.4f, 8.2f, 4.4f, 2.0f, 1.0f, 2.0f, energy);
                }
                r.body.box(-3.6f, 0.6f, 2.8f, 7.2f, 6.0f, 1.0f, panel);
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part a = r.arm(sd);
                    a.box(ax(sd) - 3.2f, -3.4f, -3.2f, 6.4f, 4.4f, 6.4f, armor.copy().trim(0x2A2E36, 0.3f));
                    a.box(ax(sd) + sd * 2.9f - 0.5f, -3.0f, -1.6f, 1.0f, 3.2f, 3.2f, hazard);
                    sleeve(a, sd, 0.4f, 1.0f, 3.4f, dark);
                    a.box(ax(sd) - 2.8f, 4.4f, -2.8f, 5.6f, 5.6f, 5.6f, panel);
                    a.box(ax(sd) - 0.3f, 1.2f, -2.9f, 0.6f, 3.4f, 0.6f, piston);
                    a.box(ax(sd) - 2.0f, 5.8f, -2.95f, 4.0f, 0.6f, 0.3f, energy);
                }
            }
            case LEGS -> {
                r.body.box(-4.9f, 9.2f, -2.9f, 9.8f, 2.0f, 5.8f, dark);
                r.body.box(-1.4f, 9.4f, -3.1f, 2.8f, 1.6f, 0.4f, energy);
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part l = r.leg(sd);
                    legWrap(l, 0.7f, -0.4f, 5.4f, panel);
                    l.box(-2.1f, 4.6f, -3.0f, 4.2f, 2.8f, 1.4f, armor);
                    l.box(sd * 2.4f - 0.4f, 3.8f, -0.6f, 0.8f, 2.6f, 1.2f, piston);
                    legWrap(l, 0.4f, 5.0f, 4.0f, dark);
                }
            }
            default -> {
                for (int sd = -1; sd <= 1; sd += 2) {
                    Part l = r.leg(sd);
                    boot(l, 0.85f, 4.8f, armor, panel);
                    l.box(-2.9f, 11.6f, -3.6f, 5.8f, 0.8f, 6.6f, dark);
                    l.box(-1.2f, 12.15f, -1.2f, 2.4f, 0.3f, 2.4f, energy);
                    l.box(sd * 2.8f - 0.3f, 8.0f, -1.6f, 0.6f, 3.0f, 3.2f, hazard);
                }
            }
        }
    }
}
