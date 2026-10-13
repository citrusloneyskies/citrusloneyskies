package com.lastbastion.client.model.hero;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.Body;
import com.lastbastion.client.model.HeroModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.Poser;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.hero.HeroEntity;
import com.lastbastion.entity.hero.KageHero;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Kage the Ronin: an indigo kimono over wide charcoal hakama, a crimson sash and trailing scarf, a red
 * lacquered menpo mask under a broad straw kasa, lacquered sode shoulder plates, and a long katana with
 * a crimson edge (its black saya worn at the left hip). Lv4 adds the wakizashi; Lv5 an oni mask.
 */
public final class KageModel implements HeroModels.Def {
    @Override
    public void build(ModelSpec s) {
        Paint skin = p(Mat.SKIN, 0xE0B898);
        Paint kimono = p(Mat.CLOTH, 0x2A2A44).accent(0x1A1A2E).pattern(Pattern.CLOTH, 0x22223A, 1f);
        Paint hakama = p(Mat.CLOTH, 0x3A3A40).accent(0x24242A).pattern(Pattern.STRIPES, 0x2E2E34, 0.7f);
        Paint sash = p(Mat.CLOTH, 0xA01A22).accent(0x6A0E14);
        Paint wraps = p(Mat.CLOTH, 0xD8D0C0).pattern(Pattern.BANDS, 0xA8A090, 2f);
        Paint straw = p(Mat.WOOD, 0xC8A860).accent(0x9A8040).pattern(Pattern.CLOTH, 0xA88A48, 2f);
        Paint lacquer = p(Mat.PLATE, 0x8A1418).gloss(0.7f).trim(0x1A1A1A, 0.35f);
        Paint black = p(Mat.PLATE, 0x18181C).gloss(0.7f);
        Paint steel = p(Mat.METAL, 0xD8DCE4).gloss(0.85f);
        Paint head = Gear.face(0xE0B898, 0x2A1A14, false, 0x1A1410, 0x7A4A3A)
                .decal(Face.FRONT, DecalType.MASK, 0x8A1418, false, 0.5f, 0.5f, 1f)
                .decal(Face.FRONT, DecalType.FANGS, 0xE8E0D0, false, 0.5f, 0.8f, 0.9f);

        Body.Spec sp = new Body.Spec(skin, kimono);
        sp.legs(5.6f, 5.4f, 3.4f);
        sp.foot = 1.1f;
        sp.hipW = 6.8f;
        sp.torso(7.4f, 6.6f, 4, 6.2f, 4, 3.8f);
        sp.neck = 0.8f;
        sp.head(6.2f, 6.4f, 6.4f);
        sp.arms(5.2f, 5, 3);
        sp.hand = 1.9f;
        sp.paints(kimono, kimono, kimono, wraps, skin, hakama, hakama, p(Mat.LEATHER, 0x2A2016));
        sp.headPaint(head);
        Body b = Body.build(s.root, sp);

        // topknot hair, kasa hat
        Gear.hair(b.head, 6.2f, 6.4f, 6.4f, 0.4f, 0.55f, p(Mat.HAIR, 0x14141A));
        b.head.box(-0.8f, -7.6f, 1.6f, 1.6f, 1.8f, 1.6f, p(Mat.HAIR, 0x14141A));
        Part kasa = b.head.child("kasa", 0, -6.2f, 0);
        kasa.octo(0, -0.6f, 0, 14, 0.6f, 14, straw);
        kasa.taper(0, -0.6f, 0, 11, 11, 1.4f, 1.4f, 3.4f, 4, straw);
        kasa.box(-3.4f, 0, -0.3f, 0.3f, 4.6f, 0.3f, p(Mat.CLOTH, 0x6A1418)).box(3.1f, 0, -0.3f, 0.3f, 4.6f, 0.3f, p(Mat.CLOTH, 0x6A1418));
        Part oni = b.head.sub(0, 0, 0).from(5);
        oni.box(-3.3f, -5.2f, -3.5f, 6.6f, 4.4f, 0.5f, p(Mat.PLATE, 0xA01418).gloss(0.6f)
                .decal(Face.FRONT, DecalType.SLIT_EYES, 0xFF3020, true, 0.5f, 0.45f, 1.1f).decal(Face.FRONT, DecalType.FANGS, 0xF0E8D8, false, 0.5f, 0.85f, 1f));
        oni.spike(-2.4f, -5, -3.2f, 1, 2.6f, 10, 0, -20, p(Mat.BONE, 0xF0E8D8)).spike(2.4f, -5, -3.2f, 1, 2.6f, 10, 0, 20, p(Mat.BONE, 0xF0E8D8));
        // kimono lapels, sash with bow, trailing scarf
        b.chest.box(-3.8f, -6.6f, -2.15f, 3.6f, 6.6f, 0.3f, kimono.copy().trim(0xC8C0B0, 0.5f));
        b.chest.box(0.2f, -6.6f, -2.15f, 3.6f, 6.6f, 0.3f, kimono.copy().trim(0xC8C0B0, 0.5f));
        b.pelvis.box(-3.4f, -2, -2.2f, 6.8f, 2.4f, 4.4f, sash);
        b.pelvis.box(-1.2f, -2.3f, 2.1f, 2.4f, 1.6f, 1, sash);
        Part scarf = b.neck.child("scarf", 0, 0, 2.4f).rot(30, 0, 0);
        scarf.box(-1.4f, 0, -0.3f, 2.8f, 5, 0.6f, sash);
        scarf.child("scarf2", 0, 5, 0).box(-1.2f, 0, -0.3f, 2.4f, 5, 0.6f, sash);
        b.neck.box(-2.4f, -0.4f, -2.4f, 4.8f, 1.4f, 4.8f, sash);
        // wide hakama legs
        b.rThigh.box(-2.3f, -0.5f, -2.3f, 4.6f, 5, 4.6f, hakama);
        b.lThigh.box(-2.3f, -0.5f, -2.3f, 4.6f, 5, 4.6f, hakama);
        b.rShin.box(-1.9f, 0, -1.9f, 3.8f, 3.4f, 3.8f, hakama);
        b.lShin.box(-1.9f, 0, -1.9f, 3.8f, 3.4f, 3.8f, hakama);
        b.rShin.box(-1.6f, 3.4f, -1.6f, 3.2f, 2, 3.2f, wraps);
        b.lShin.box(-1.6f, 3.4f, -1.6f, 3.2f, 2, 3.2f, wraps);
        // sode shoulder plates (Lv3)
        b.rArm.sub(-0.6f, -1.4f, 0).from(3).rot(0, 0, 12).box(-2.6f, 0, -2.2f, 3.4f, 3.6f, 4.4f, lacquer.copy().pattern(Pattern.BANDS, 0x5A0E10, 1.4f));
        b.lArm.sub(0.6f, -1.4f, 0).from(3).rot(0, 0, -12).box(-0.8f, 0, -2.2f, 3.4f, 3.6f, 4.4f, lacquer.copy().pattern(Pattern.BANDS, 0x5A0E10, 1.4f));
        // saya at the left hip, wakizashi (Lv4)
        Part saya = b.pelvis.child("saya", 3.4f, -1, -1).rot(-15, -20, 0);
        saya.box(-0.5f, -0.5f, -2, 1, 1, 12, black.copy().trim(0xC8A040, 0.4f));
        b.pelvis.sub(3.6f, -1.6f, 0).from(4).rot(-10, -30, 0).box(-0.45f, -0.45f, -3, 0.9f, 0.9f, 8, black)
                .box(-0.4f, -0.4f, -5, 0.8f, 0.8f, 2, wraps);
        // katana: blade along the hand's -Z, crimson edge
        Part kat = b.rHand.child("katana", 0, 1.3f, 0);
        kat.box(-0.4f, -0.4f, -1.6f, 0.8f, 0.8f, 4.4f, p(Mat.CLOTH, 0x1A1A1E).pattern(Pattern.CHECKER, 0xE8E0D0, 2.5f));
        kat.octoZ(0, 0, -2.1f, 2.2f, 0.5f, p(Mat.GOLD, 0xC8A040));
        Part blade = kat.child("blade", 0, 0, -2.2f).rot(-3, 0, 0);
        blade.box(-0.18f, -0.5f, -14, 0.36f, 1, 14, steel.copy().decal(Face.TOP, DecalType.STRIPE, 0xD02030, true, 0.5f, 0.5f, 0.6f));
        blade.box(-0.12f, 0.35f, -13.6f, 0.24f, 0.25f, 13.4f, Paint.glowing(0xE02030));
        blade.spike(0, 0, -14, 0.9f, 1.2f, 90, 0, 0, steel);
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        Poser p = Poser.of(m);
        HeroModels.humanoid(p, h, c, 1f, c.combat ? 0.3f : 0.75f);
        float move = Math.min(1, c.la);
        ModelPart scarf = m.p("scarf"), scarf2 = m.p("scarf2"), kat = m.p("katana");
        scarf.xRot += move * 0.9f + Mth.sin(c.age * 0.15f) * 0.08f;
        scarf2.xRot += move * 0.4f + Mth.sin(c.age * 0.19f + 1) * 0.1f;
        scarf.yRot += Mth.sin(c.age * 0.07f) * 0.2f;
        if (c.combat || c.id != 0) {
            // two-handed guard: blade angled up and forward
            p.armR(-45, -25, 0, 55);
            p.armL(-55, 30, 0, 60);
            p.crouch(0.2f, 1);
        } else {
            p.armR(-5, 0, 5, 15);
        }
        switch (c.id) {
            case 1 -> slash(p, c.t, 60, -40, 150);
            case 2 -> slash(p, c.t, -150, 40, -150);
            case 3 -> {
                float k = Anim.win(c.t, 2, 6);
                p.pelvis.yRot += Anim.smooth(k) * Mth.TWO_PI;
                p.armR(-90 * Anim.pulse(c.t, 1, 8), 70, 0, 0);
                p.crouch(Anim.pulse(c.t, 1, 8) * 0.6f, 2);
            }
            case 4 -> {
                float wind = Anim.hold(c.t, 0, 3, 1) * (c.t < 4 ? 1 : 0);
                float thrust = Anim.hold(c.t, 4, 8, 2);
                p.armR(-60 * wind - 85 * thrust, 25 * wind, 0, 100 * wind);
                p.spine(12 * thrust, 20 * wind - 15 * thrust, 0);
                p.legL(-35 * thrust, 0, 30 * thrust);
            }
            case KageHero.IAIDO -> {
                float draw = Anim.hold(c.t, 0, 9, 2) * (c.t < 10 ? 1 : 0);
                float cut = Anim.hold(c.t, 10, 16, 2);
                // crouched quick-draw stance, hand on the hilt
                p.crouch(draw, 4);
                p.spine(20 * draw + 10 * cut, -30 * draw + 35 * cut, 0);
                p.armR(30 * draw - 100 * cut, 40 * draw - 80 * cut, 0, 90 * draw);
                p.armL(-20 * draw, 40 * draw, 0, 60 * draw);
                kat.visible = c.t >= 9;
            }
            case KageHero.PARRY -> {
                float k = Anim.hold(c.t, 0, 36, 3);
                p.armR(-100 * k, -20 * k, 0, 70 * k);
                p.armL(-100 * k, 40 * k, 0, 80 * k);
                kat.zRot += 1.5f * k;
                p.crouch(0.3f * k, 2);
            }
            case KageHero.SMOKE -> slash(p, c.t - 4, 70, -50, 170);
            case KageHero.CLONES -> {
                float k = Anim.hold(c.t, 0, 14, 3);
                p.armL(-100 * k, 0, 0, 120 * k);
                p.armR(-10 * k, 0, 20 * k, 10);
                p.head.xRot += 0.3f * k;
            }
            case HeroEntity.ULT -> {
                float sheathe = Anim.hold(c.t, 30, 60, 4);
                p.armR(-20 * sheathe, 60 * sheathe, 0, 100 * sheathe);
                p.armL(-30 * sheathe, 40 * sheathe, 0, 70 * sheathe);
                p.crouch(0.4f * sheathe, 3);
                p.head.xRot += 0.35f * sheathe;
            }
            default -> {
            }
        }
    }

    /** Diagonal katana cut from (startPitch) to (startPitch + sweep), yawing across the body. */
    private static void slash(Poser p, float t, float startPitch, float yaw, float sweep) {
        if (t < 0) return;
        float wind = t < 3 ? Anim.smooth(t / 3f) : 0;
        float cut = Anim.smooth(Anim.win(t, 3, 3));
        float rec = Anim.win(t, 7, 6);
        float pitch = (startPitch * wind) * (1 - cut) + (startPitch - sweep) * 0.8f * cut * (1 - rec);
        p.armR(pitch, (yaw * (1 - cut) - yaw * cut) * (1 - rec), 0, 20);
        p.armL(pitch * 0.7f, (yaw * (1 - cut) - yaw * cut) * 0.6f * (1 - rec), 0, 40);
        p.spine(6 * cut * (1 - rec), (yaw * 0.6f) * (1 - 2 * cut) * (1 - rec), 0);
    }
}
