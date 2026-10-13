package com.lastbastion.client.model.hero;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.HeroModels;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.entity.hero.AshfangHero;
import com.lastbastion.entity.hero.HeroEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * Ashfang the Dire Wolf: smoke-grey fur darkening along the spine, a pale ruff, ember eyes, a scarred
 * muzzle and a spiked collar. Higher levels add iron shoulder plates, ember-cracked fur and a burning mane.
 */
public final class AshfangModel implements HeroModels.Def {
    @Override
    public float shadow() {
        return 0.7f;
    }

    @Override
    public void build(ModelSpec s) {
        Paint fur = p(Mat.FUR, 0x8C9098).accent(0x5A5E66).pattern(Pattern.GRADIENT, 0xB8BCC2, 1f);
        Paint back = p(Mat.FUR, 0x5A5E66).accent(0x3A3C42);
        Paint ruff = p(Mat.FUR, 0xC8CCD2).accent(0x8C9098);
        Paint dark = p(Mat.FUR, 0x46484E).accent(0x2A2C30);
        Paint nose = p(Mat.RUBBER, 0x1A1A1C).gloss(0.6f);
        Paint fang = p(Mat.BONE, 0xF0ECE0);
        Paint embers = p(Mat.FUR, 0x5A5E66).accent(0x3A3C42).pattern(Pattern.VEINS, 0xFF7A20, 1.2f);
        Paint skullP = fur.copy().decal(Face.FRONT, DecalType.SLIT_EYES, 0xFF9A20, true, 0.5f, 0.4f, 1.15f);
        Paint muzzle = p(Mat.FUR, 0x9CA0A8).accent(0x6A6E76).decal(Face.TOP, DecalType.SCAR, 0xD0B0A8, false, 0.4f, 0.4f, 1f);
        Paint collar = p(Mat.LEATHER, 0x4A2A18).trim(0x9A9CA2, 0.4f);
        Paint iron = p(Mat.METAL, 0x8A8C94);

        Part body = s.root.child("body", 0, 11.5f, 0);
        body.box(-4.5f, -4, -7, 9, 8, 15, fur);
        body.box(-3.6f, -4.6f, -6, 7.2f, 1.6f, 13, back);
        body.box(-5, -5, -9.5f, 10, 9.5f, 5, ruff);
        body.box(-3.8f, 3.4f, -6, 7.6f, 1.4f, 10, ruff);
        for (int i = 0; i < 5; i++) body.spike(0, -4.4f, -7 + i * 2.6f, 1.6f, 2.2f - i * 0.25f, -60, 0, 0, dark);
        body.sub(0, 0, 0).from(4).box(-4.6f, -4.1f, -6.9f, 9.2f, 8.2f, 15.2f, 0f, embers);
        Part mane = body.sub(0, -4.6f, -6).from(5);
        Paint fire = Paint.glowing(0xFF7A20).accent(0xFFE070);
        for (int i = 0; i < 6; i++) mane.spike((i % 2 == 0 ? -1 : 1) * 1.2f, 0, i * 1.6f, 1.6f, 3.2f - i * 0.3f, -70, 0, (i % 2 == 0 ? -1 : 1) * 15, fire);
        // shoulder armour
        Part plates = body.sub(0, 0, 0).from(3);
        plates.box(-5.6f, -4.6f, -8.6f, 2.4f, 5, 5, iron.copy().trim(0x5A5C62, 0.4f)).box(3.2f, -4.6f, -8.6f, 2.4f, 5, 5, iron.copy().trim(0x5A5C62, 0.4f));
        plates.spike(-5.4f, -4.4f, -6, 0.9f, 1.8f, 0, 0, -30, iron).spike(5.4f, -4.4f, -6, 0.9f, 1.8f, 0, 0, 30, iron);

        Part neck = body.child("neck", 0, -2.5f, -8.5f).rot(-20, 0, 0);
        neck.box(-3, -3, -4.5f, 6, 6.5f, 5, ruff);
        neck.box(-3.4f, -1.2f, -3.6f, 6.8f, 1.6f, 3.6f, collar);
        for (int i = 0; i < 4; i++) neck.spike(-3.4f + i * 2.26f, -1.2f, -3.6f, 0.6f, 1.2f, 70, 0, 0, iron);
        Part head = neck.child("head", 0, -0.5f, -4.3f).rot(20, 0, 0);
        head.box(-3.6f, -3.8f, -5.4f, 7.2f, 6.4f, 6, skullP);
        head.box(-3.4f, -4.4f, -3.4f, 6.8f, 1.2f, 4, back);
        Part snout = head.child("snout", 0, -0.8f, -5.2f);
        snout.box(-2.1f, -1.4f, -4.8f, 4.2f, 3, 5, muzzle);
        snout.box(-1, -1.6f, -5.1f, 2, 1.2f, 1, nose);
        snout.spike(-1.3f, 1.6f, -4.3f, 0.5f, 1.1f, 180, 0, 0, fang).spike(1.3f, 1.6f, -4.3f, 0.5f, 1.1f, 180, 0, 0, fang);
        Part jaw = head.child("jaw", 0, 1.6f, -4.8f);
        jaw.box(-1.8f, 0, -4.6f, 3.6f, 1.4f, 4.8f, muzzle);
        jaw.spike(-1.2f, 0, -4.1f, 0.45f, 0.9f, 0, 0, 0, fang).spike(1.2f, 0, -4.1f, 0.45f, 0.9f, 0, 0, 0, fang);
        jaw.box(-1.3f, -0.1f, -4.3f, 2.6f, 0.2f, 3.6f, p(Mat.FLESH, 0x8A2A30));
        head.spike(-2.4f, -3.8f, -2.4f, 1.8f, 3.2f, -10, 0, -18, dark).spike(2.4f, -3.8f, -2.4f, 1.8f, 3.2f, -10, 0, 18, dark);
        head.box(-3.8f, -1.6f, -2, 1, 3, 3, ruff).box(2.8f, -1.6f, -2, 1, 3, 3, ruff);

        leg(body, "fl_r", -3, 2.5f, -6.2f, 4.4f, 4.6f, false, fur, dark);
        leg(body, "fl_l", 3, 2.5f, -6.2f, 4.4f, 4.6f, false, fur, dark);
        leg(body, "hl_r", -3, 2, 5.6f, 5.2f, 4.8f, true, fur, dark);
        leg(body, "hl_l", 3, 2, 5.6f, 5.2f, 4.8f, true, fur, dark);

        Part tail = body.child("tail", 0, -2.8f, 7.6f).rot(55, 0, 0);
        tail.box(-1.5f, 0, -1.5f, 3, 6, 3, fur);
        Part tail2 = tail.child("tail2", 0, 6, 0).rot(-15, 0, 0);
        tail2.box(-1.3f, 0, -1.3f, 2.6f, 5, 2.6f, fur).box(-1.1f, 4.6f, -1.1f, 2.2f, 1.6f, 2.2f, ruff);
    }

    /** Leg with upper and lower segment and a paw; hind legs have a backward-bent hock. */
    private static void leg(Part body, String name, float x, float y, float z, float upper, float lower, boolean hind, Paint fur, Paint dark) {
        Part u = body.child(name, x, y, z).rot(hind ? 18 : 0, 0, 0);
        u.box(-1.6f, -1, -1.9f, 3.2f, upper + 1, 3.8f, fur);
        Part l = u.child(name + "2", 0, upper, hind ? 0.4f : 0).rot(hind ? -36 : 0, 0, 0);
        l.box(-1.15f, 0, -1.15f, 2.3f, lower, 2.3f, fur);
        Part paw = l.child(name + "p", 0, lower, 0).rot(hind ? 18 : 0, 0, 0);
        paw.box(-1.45f, -0.4f, -2.1f, 2.9f, 1.4f, 3.2f, dark);
        paw.spike(-0.8f, 0.4f, -2.2f, 0.4f, 0.7f, 90, 0, 0, p(Mat.BONE, 0x3A3632)).spike(0.8f, 0.4f, -2.2f, 0.4f, 0.7f, 90, 0, 0, p(Mat.BONE, 0x3A3632));
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        ModelPart body = m.p("body"), neck = m.p("neck"), head = m.p("head"), jaw = m.p("jaw"), tail = m.p("tail"), tail2 = m.p("tail2");
        ModelPart flr = m.p("fl_r"), fll = m.p("fl_l"), hlr = m.p("hl_r"), hll = m.p("hl_l");
        ModelPart flr2 = m.p("fl_r2"), fll2 = m.p("fl_l2"), hlr2 = m.p("hl_r2"), hll2 = m.p("hl_l2");
        float amt = Math.min(1, c.la);
        boolean run = h.getDeltaMovement().horizontalDistanceSqr() > 0.03;
        float ph = c.ls * (run ? 0.75f : 0.6662f);
        float sw = (run ? 1.0f : 0.7f) * amt;
        // diagonal gait (trot); bounding gallop when running
        float off = run ? 0.4f : Mth.PI;
        flr.xRot += Mth.cos(ph) * sw;
        hll.xRot += Mth.cos(ph + (run ? 0.6f : 0)) * sw;
        fll.xRot += Mth.cos(ph + off) * sw;
        hlr.xRot += Mth.cos(ph + off + (run ? 0.6f : 0)) * sw;
        flr2.xRot += Math.max(0, Mth.sin(ph)) * -0.7f * amt;
        fll2.xRot += Math.max(0, Mth.sin(ph + off)) * -0.7f * amt;
        hlr2.xRot += Math.max(0, Mth.sin(ph + off)) * 0.6f * amt;
        hll2.xRot += Math.max(0, Mth.sin(ph)) * 0.6f * amt;
        body.y += -Math.abs(Mth.sin(ph)) * (run ? 1.2f : 0.5f) * amt;
        body.xRot += (run ? Mth.sin(ph * 2) * 0.08f : 0) * amt;
        // breathing, look, tail
        float breathe = Mth.sin(c.age * (c.combat ? 0.25f : 0.08f));
        body.yScale += breathe * 0.012f;
        neck.yRot += c.headYaw * Anim.D * 0.4f;
        head.yRot += c.headYaw * Anim.D * 0.5f;
        head.xRot += c.headPitch * Anim.D * 0.6f;
        jaw.xRot += c.combat ? 0.12f + Math.max(0, breathe) * 0.1f : (h.getDeltaMovement().horizontalDistanceSqr() > 0.01 ? 0.25f + breathe * 0.08f : 0.03f);
        tail.yRot += Mth.sin(c.age * (c.combat ? 0.15f : 0.3f)) * (c.combat ? 0.15f : 0.4f);
        tail.xRot += c.combat ? -0.35f : amt * 0.4f;
        tail2.xRot += Mth.sin(c.age * 0.2f) * 0.1f;
        if (c.combat && amt < 0.2f) {
            // stalking crouch with lowered head
            body.y += 1.2f;
            neck.xRot += 0.25f;
            head.xRot -= 0.1f;
            flr.xRot -= 0.2f;
            fll.xRot -= 0.2f;
            hlr.xRot += 0.2f;
            hll.xRot += 0.2f;
        }
        if (c.hurt > 0) {
            head.xRot -= c.hurt * 0.4f;
            body.zRot += c.hurt * 0.08f;
        }
        switch (c.id) {
            case 1, 2, 3 -> {
                // lunge bite; the third is a savage head-shake
                float k = Anim.swing(c.t, 3, 2, 6);
                float lunge = Math.max(0, -k);
                body.z -= lunge * 2.5f;
                neck.xRot += 0.5f * k;
                jaw.xRot += 0.8f * Math.max(0, k) + 0.1f;
                if (c.id == 3) neck.yRot += Mth.sin(c.t * 1.8f) * 0.5f * Anim.hold(c.t, 5, 12, 2);
            }
            case AshfangHero.POUNCE -> {
                float crouch = Anim.hold(c.t, 0, 6, 2) * (c.t < 6 ? 1 : 0);
                float air = Anim.hold(c.t, 7, 18, 3);
                body.y += crouch * 3;
                body.xRot += -0.3f * air + 0.15f * crouch;
                flr.xRot -= 1.2f * air;
                fll.xRot -= 1.2f * air;
                hlr.xRot += 1.1f * air;
                hll.xRot += 1.1f * air;
                jaw.xRot += 0.9f * air;
            }
            case AshfangHero.HAMSTRING -> {
                float k = Anim.swing(c.t, 2, 2, 8);
                neck.xRot += 0.9f * Math.max(0, -k);
                neck.yRot += 0.6f * k;
                jaw.xRot += 0.7f * Math.max(0, k);
            }
            case AshfangHero.FRENZY -> {
                float k = Anim.hold(c.t, 0, 22, 2);
                body.z -= Mth.sin(c.t * 1.6f) * 1.5f * k;
                neck.yRot += Mth.sin(c.t * 1.2f) * 0.5f * k;
                jaw.xRot += (0.4f + 0.4f * Mth.sin(c.t * 1.6f)) * k;
            }
            case AshfangHero.HOWL -> {
                float k = Anim.hold(c.t, 4, 26, 4);
                neck.xRot -= 1.2f * k;
                head.xRot -= 0.3f * k;
                jaw.xRot += 0.7f * k;
                body.xRot -= 0.25f * k;
                hlr.xRot += 0.25f * k;
                hll.xRot += 0.25f * k;
                tail.xRot -= 0.4f * k;
            }
            case HeroEntity.ULT -> {
                float k = c.t < 10 ? Anim.smooth(c.t / 10f) : 1;
                if (c.t < 10) {
                    neck.xRot -= 1.1f * k;
                    jaw.xRot += 0.7f * k;
                }
                float lunge = Anim.pulse(c.t % 12, 2, 8);
                body.z -= lunge * 2;
                jaw.xRot += lunge * 0.8f;
                flr.xRot -= lunge * 0.9f;
                fll.xRot -= lunge * 0.9f;
            }
            case HeroEntity.ANIM_DODGE -> {
                float k = Anim.pulse(c.t, 0, 12);
                body.zRot += 0.4f * k;
                body.y += k * 1.5f;
            }
            case HeroEntity.ANIM_ARRIVE, HeroEntity.ANIM_LEVELUP -> {
                float k = Anim.pulse(c.t, 0, 30);
                neck.xRot -= 1.1f * k;
                jaw.xRot += 0.6f * k;
            }
            default -> {
            }
        }
    }
}
