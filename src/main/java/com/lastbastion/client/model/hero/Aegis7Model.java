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
import com.lastbastion.entity.hero.Aegis7Hero;
import com.lastbastion.entity.hero.HeroEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.hero.Gear.p;

/**
 * AEGIS-7 War Mech: a four-block bipedal walker on reverse-jointed legs, with a slab hull, a glowing
 * cockpit canopy, a sensor head with a red visor, twin six-barrel rotary cannons for forearms, a
 * shoulder missile pod and a cyan reactor on its back. Lv3 adds armour skirts, Lv4 a shield emitter,
 * Lv5 gold trim and a second pod.
 */
public final class Aegis7Model implements HeroModels.Def {
    @Override
    public float shadow() {
        return 1.6f;
    }

    @Override
    public int density() {
        return 3;
    }

    @Override
    public void build(ModelSpec s) {
        Paint hull = p(Mat.PLATE, 0x5B6E7A).gloss(0.45f).grime(0.25f);
        Paint hullLight = p(Mat.PLATE, 0x7A8E98).gloss(0.5f);
        Paint joint = p(Mat.DARK_METAL, 0x2E3238);
        Paint hazard = p(Mat.PLATE, 0x2A2A2A).pattern(Pattern.HAZARD, 0xE8B820, 1.2f);
        Paint glass = p(Mat.GLASS, 0x40D8FF).accent(0xE0FFFF).glow();
        Paint reactor = Paint.glowing(0x40D8FF).pattern(Pattern.HEX, 0xE0FFFF, 1.6f);
        Paint gold = p(Mat.GOLD, 0xD8B048).accent(0xFFF0B0);
        Paint gun = p(Mat.DARK_METAL, 0x3A3E46).gloss(0.5f);
        Paint redGlow = Paint.glowing(0xFF3020);

        Part pelvis = s.root.child("pelvis", 0, -7.3f, 0);
        pelvis.box(-7, -3, -5, 14, 6, 10, hull.copy().trim(0xE8B820, 0.4f));
        pelvis.octoX(-8.6f, 0.5f, 0, 4, 17.2f, joint);
        pelvis.sub(0, 0, 0).from(3).box(-7.6f, 2, -5.6f, 15.2f, 4, 1.2f, hazard).box(-7.6f, 2, 4.4f, 15.2f, 4, 1.2f, hull);
        for (int side = -1; side <= 1; side += 2) {
            String sd = side < 0 ? "r" : "l";
            Part thigh = pelvis.child(sd + "_thigh", side * 6, 1, 0).rot(-20, 0, 0);
            thigh.box(-3, -1, -3.5f, 6, 15, 7, hull);
            thigh.box(-3.3f, 3, -3.8f, 6.6f, 6, 1, hazard);
            thigh.octoX(-3.6f, 15, 0, 4.6f, 7.2f, joint);
            Part shin = thigh.child(sd + "_shin", 0, 15, 0).rot(40, 0, 0);
            shin.box(-2.6f, 0, -2.6f, 5.2f, 14, 5.2f, hullLight);
            shin.box(-1, 1, 2.6f, 2, 11, 1.6f, p(Mat.METAL, 0xC8CCD4).gloss(0.8f));
            shin.box(-2.9f, 9, -2.9f, 5.8f, 2, 5.8f, joint);
            Part foot = shin.child(sd + "_foot", 0, 14, 0).rot(-20, 0, 0);
            foot.box(-4, 0, -7, 8, 3, 11, joint.copy().trim(0x5B6E7A, 0.6f));
            foot.spike(-2.6f, 1.6f, -7, 1.6f, 2.4f, 90, 0, 0, gun).spike(0, 1.6f, -7.2f, 1.6f, 2.6f, 90, 0, 0, gun).spike(2.6f, 1.6f, -7, 1.6f, 2.4f, 90, 0, 0, gun);
            foot.spike(0, 1.6f, 4, 1.4f, 2.2f, -90, 0, 0, gun);
        }
        Part torso = pelvis.child("torso", 0, -3, 0);
        torso.box(-11, -19, -8, 22, 19, 16, hull.copy().trim(0x3A4A54, 0.6f).decal(Face.LEFT, DecalType.CHEVRON, 0xE8B820, false, 0.5f, 0.3f, 0.8f)
                .decal(Face.RIGHT, DecalType.CHEVRON, 0xE8B820, false, 0.5f, 0.3f, 0.8f));
        torso.box(-9, -10, -9.5f, 18, 9, 2, hullLight.copy().decal(Face.FRONT, DecalType.GRILLE, 0x101418, false, 0.5f, 0.6f, 0.8f));
        torso.box(-6, -17, -9, 12, 7, 1.5f, glass.copy().decal(Face.FRONT, DecalType.PANEL, 0x0A4A60, false));
        torso.box(-11.4f, -2, -8.4f, 22.8f, 2, 16.8f, hazard);
        Part head = torso.child("head", 0, -19, -2);
        head.box(-3.6f, -4.4f, -3.6f, 7.2f, 4.4f, 7.2f, hull.copy().decal(Face.FRONT, DecalType.VISOR, 0xFF3020, true, 0.5f, 0.45f, 1f));
        head.box(-0.3f, -12, 1.5f, 0.6f, 8, 0.6f, joint).box(-0.6f, -12.6f, 1.2f, 1.2f, 1.2f, 1.2f, redGlow);
        head.box(2.6f, -6.4f, -1, 2.4f, 2.4f, 0.4f, p(Mat.METAL, 0xC8CCD4));
        // reactor and exhausts
        torso.box(-4, -15, 8, 8, 10, 2.4f, reactor);
        torso.box(-5, -16, 7.8f, 10, 1, 3, joint).box(-5, -5, 7.8f, 10, 1, 3, joint);
        Part exL = torso.sub(-7, -19, 5);
        exL.octo(0, -5, 0, 2.6f, 5, 2.6f, joint).octo(0, -5.6f, 0, 2.2f, 0.6f, 2.2f, Paint.glowing(0xFF8030));
        Part exR = torso.sub(7, -19, 5);
        exR.octo(0, -5, 0, 2.6f, 5, 2.6f, joint).octo(0, -5.6f, 0, 2.2f, 0.6f, 2.2f, Paint.glowing(0xFF8030));
        torso.sub(0, 0, 0).from(5).box(-11.3f, -19.3f, -8.3f, 22.6f, 0.8f, 16.6f, gold).box(-6.4f, -17.4f, -9.3f, 12.8f, 0.6f, 0.6f, gold);
        // missile pod (left), second pod at Lv5 (right), shield emitter at Lv4
        Part pod = torso.child("pod", 8, -19, 0);
        pod.box(-4, -6, -5, 8, 6, 10, hull.copy().decal(Face.FRONT, DecalType.DOTS, 0xFF4020, true));
        pod.child("podlid", 0, -6, -5).box(-4.2f, -0.8f, 0, 8.4f, 0.8f, 10.4f, hazard);
        Part pod2 = torso.child("pod2", -8, -19, 0).from(5);
        pod2.box(-4, -6, -5, 8, 6, 10, hull.copy().decal(Face.FRONT, DecalType.DOTS, 0xFF4020, true));
        Part emitter = torso.sub(-11.5f, -13, 0).from(4);
        emitter.octoX(-1.4f, 0, 0, 6, 1.4f, p(Mat.METAL, 0xC8CCD4)).octoX(-1.8f, 0, 0, 3.6f, 0.6f, Paint.glowing(0x6FE3FF));
        for (int side = -1; side <= 1; side += 2) {
            String sd = side < 0 ? "r" : "l";
            Part arm = torso.child(sd + "_arm", side * 12.5f, -15, 0);
            arm.sphere(0, 0, 0, 3.6f, joint);
            arm.box(-4.5f, -4.4f, -5, 9, 6, 10, hull.copy().trim(0xE8B820, 0.4f));
            arm.box(-2.5f, 1, -2.5f, 5, 9, 5, hullLight);
            Part cannon = arm.child(sd + "_gun", 0, 10, 0);
            cannon.box(-3.6f, -2, -4.5f, 7.2f, 6.4f, 9, gun.copy().trim(0x5B6E7A, 0.5f));
            cannon.box(side * 3.4f - 1.6f, -1, -2, 3.2f, 4, 5, p(Mat.METAL, 0x6A5A3A).pattern(Pattern.BANDS, 0xC8A050, 1.5f));
            Part barrels = cannon.child(sd + "_barrels", 0, 1.2f, -4.5f);
            for (int i = 0; i < 6; i++) {
                float a = i * Mth.TWO_PI / 6;
                barrels.box(Mth.cos(a) * 1.5f - 0.45f, Mth.sin(a) * 1.5f - 0.45f, -12, 0.9f, 0.9f, 12, p(Mat.METAL, 0x9A9EA6).gloss(0.6f));
            }
            barrels.octoZ(0, 0, -12.6f, 4.6f, 1.2f, joint);
            barrels.octoZ(0, 0, -6, 4.2f, 0.8f, joint);
            barrels.box(-0.5f, -0.5f, -12.7f, 1, 1, 0.3f, redGlow);
        }
    }

    @Override
    public void animate(ProcModel<HeroEntity> m, HeroEntity h, HeroModels.Ctx c) {
        ModelPart pelvis = m.p("pelvis"), torso = m.p("torso"), head = m.p("head");
        ModelPart rT = m.p("r_thigh"), lT = m.p("l_thigh"), rS = m.p("r_shin"), lS = m.p("l_shin"), rF = m.p("r_foot"), lF = m.p("l_foot");
        ModelPart rA = m.p("r_arm"), lA = m.p("l_arm"), rB = m.p("r_barrels"), lB = m.p("l_barrels"), lid = m.p("podlid");
        float amt = Math.min(1, c.la);
        float ph = c.ls * 0.5f;
        float cs = Mth.cos(ph), sn = Mth.sin(ph);
        rT.xRot += cs * 0.5f * amt;
        lT.xRot -= cs * 0.5f * amt;
        rS.xRot += Math.max(0, sn) * 0.5f * amt;
        lS.xRot += Math.max(0, -sn) * 0.5f * amt;
        rF.xRot -= cs * 0.3f * amt;
        lF.xRot += cs * 0.3f * amt;
        pelvis.y -= Math.abs(sn) * 1.6f * amt;
        torso.zRot += cs * 0.05f * amt;
        torso.yRot += cs * 0.06f * amt;
        torso.y += Mth.sin(c.age * 0.06f) * 0.2f;
        head.yRot += Mth.clamp(c.headYaw, -60, 60) * Anim.D;
        head.xRot += Mth.clamp(c.headPitch, -30, 30) * Anim.D;
        boolean firing = c.combat && (c.id == 1 || c.id == 2) && c.t < 8;
        boolean od = h instanceof Aegis7Hero a && a.overdriven();
        float spinRate = c.combat ? (od ? 1.6f : 0.9f) : 0.05f;
        rB.zRot = c.age * spinRate;
        lB.zRot = -c.age * spinRate;
        float aim = (c.combat ? -70 : -10) + Mth.clamp(c.headPitch, -30, 30);
        rA.xRot += aim * Anim.D;
        lA.xRot += aim * Anim.D;
        if (firing) {
            float rec = Math.max(0, 1 - c.t / 3f);
            if (c.id == 1) rA.z += rec * 1.2f;
            else lA.z += rec * 1.2f;
        }
        if (od) {
            torso.x += Mth.sin(c.age * 3.1f) * 0.15f;
            torso.y += Mth.cos(c.age * 2.7f) * 0.1f;
        }
        if (c.hurt > 0) torso.xRot -= c.hurt * 0.12f;
        switch (c.id) {
            case Aegis7Hero.MISSILES -> {
                float k = Anim.hold(c.t, 6, 30, 4);
                lid.xRot -= 1.4f * k;
                torso.xRot -= 0.12f * k;
            }
            case Aegis7Hero.STOMP -> {
                float lift = Anim.hold(c.t, 2, 12, 3) * (c.t < 15 ? 1 : 0);
                float slam = Anim.pulse(c.t, 15, 8);
                rT.xRot -= 0.9f * lift;
                rS.xRot += 0.6f * lift;
                torso.xRot += 0.15f * slam;
                pelvis.y += slam * 2;
            }
            case Aegis7Hero.SHIELD -> {
                float k = Anim.hold(c.t, 2, 14, 3);
                rA.yRot -= 0.6f * k;
                lA.yRot += 0.6f * k;
                rA.xRot -= 0.4f * k;
                lA.xRot -= 0.4f * k;
            }
            case Aegis7Hero.OVERDRIVE -> {
                float k = Anim.pulse(c.t, 0, 16);
                torso.xRot -= 0.25f * k;
                head.xRot -= 0.5f * k;
            }
            case HeroEntity.ULT -> {
                float k = Anim.hold(c.t, 2, 38, 4);
                head.xRot += 0.2f * k;
                torso.xRot += 0.1f * k;
                rA.xRot -= 0.6f * k;
            }
            case HeroEntity.ANIM_ARRIVE -> {
                float k = 1 - Anim.easeOut(Anim.win(c.t, 0, 30));
                pelvis.y += k * 6;
                rS.xRot += k * 0.6f;
                lS.xRot += k * 0.6f;
                rT.xRot -= k * 0.6f;
                lT.xRot -= k * 0.6f;
                torso.xRot += k * 0.4f;
            }
            default -> {
            }
        }
    }
}
