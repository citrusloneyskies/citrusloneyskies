package com.lastbastion.client.model.zombie;

import com.lastbastion.client.model.Anim;
import com.lastbastion.client.model.Body;
import com.lastbastion.client.model.ModelSpec;
import com.lastbastion.client.model.ModelSpec.Part;
import com.lastbastion.client.model.Paint;
import com.lastbastion.client.model.Paint.DecalType;
import com.lastbastion.client.model.Paint.Face;
import com.lastbastion.client.model.Paint.Mat;
import com.lastbastion.client.model.Paint.Pattern;
import com.lastbastion.client.model.Poser;
import com.lastbastion.client.model.ProcModel;
import com.lastbastion.client.model.ZombieModels;
import com.lastbastion.client.model.hero.Gear;
import com.lastbastion.entity.zombie.BloaterZombie;
import com.lastbastion.entity.zombie.SiegeZombie;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.zombie.ZGear.*;

/** The big ones: Brute, Bloater, Bulwark, Wrecker and the Abomination. */
public final class Hulks {
    private Hulks() {
    }

    // ================================================================== Brute

    /** Hunched mountain of mutated muscle: tiny head sunk between the shoulders, a spiked hump, shackled fists. */
    public static final class Brute implements ZombieModels.Def {
        static final float K = 1.5f;

        @Override
        public float shadow() {
            return 1.0f;
        }

        @Override
        public void build(ModelSpec s) {
            int sk = 0x7A8A62;
            Paint muscle = p(Mat.FLESH, sk).accent(0x4A5A3A).pattern(Pattern.MUSCLE, 0x5A6A42, 1.2f).grime(0.3f)
                    .decal(Face.FRONT, DecalType.STITCH, 0, false, 0.3f, 0.5f, 1f).decal(Face.BACK, DecalType.SCAR, 0x5A2020, false, 0.5f, 0.4f, 1.2f);
            Paint limb = p(Mat.FLESH, sk).accent(0x4A5A3A).pattern(Pattern.MUSCLE, 0x5A6A42, 1.4f).grime(0.3f);
            Paint head = face(sk, 0xFF5020);
            Paint pants = cloth(0x4A3B2A);
            Body.Spec sp = new Body.Spec(limb, muscle).scale(K);
            sp.legs(5, 5, 4.6f);
            sp.foot = 1.4f;
            sp.hipW = 9;
            sp.torso(13, 9, 8, 9, 4, 6);
            sp.neck = 0.4f;
            sp.head(5.4f, 5.6f, 5.6f);
            sp.arms(7, 7, 5.2f);
            sp.hand = 3.2f;
            sp.paints(muscle, pants, limb, limb, limb, pants, pants, p(Mat.FLESH, 0x5A6A42));
            sp.headPaint(head);
            Body b = Body.build(s.root, sp);
            b.neck.pz -= 2.6f * K;
            b.neck.py += 2.2f * K;
            Part hump = b.chest.child("hump", 0, -8 * K, 3 * K);
            hump.sphere(0, 0, 0, 4.2f * K, muscle);
            for (int i = 0; i < 4; i++) hump.spike((i - 1.5f) * 2.2f * K, -2.6f * K, 1.4f * K, 1.1f * K, (2.4f + (i % 2)) * K, -40, 0, (i - 1.5f) * 15, bone());
            Part jaw = b.head.child("jaw", 0, -0.6f * K, -0.6f * K);
            jaw.box(-3 * K, 0, -3.4f * K, 6 * K, 2 * K, 4 * K, p(Mat.FLESH, 0x6A7A52).decal(Face.FRONT, DecalType.FANGS, 0xE0D8C0, false, 0.5f, 0.15f, 1.1f));
            b.pelvis.box(-4.8f * K, -1.4f * K, -3.3f * K, 9.6f * K, 1.2f * K, 6.6f * K, p(Mat.CLOTH, 0x8A7A5A).pattern(Pattern.CLOTH, 0x6A5A3A, 2f));
            for (int side = -1; side <= 1; side += 2) {
                Part fore = side < 0 ? b.rFore : b.lFore;
                fore.box(-2.9f * K, 4.6f * K, -2.9f * K, 5.8f * K, 1.6f * K, 5.8f * K, rusty());
                Part ch = fore.child(side < 0 ? "chain_r" : "chain_l", 0, 6.2f * K, 1.6f * K);
                for (int i = 0; i < 4; i++) ch.box(-0.4f * K, i * 1.2f * K, -0.4f * K, 0.8f * K, 1 * K, 0.8f * K, p(Mat.METAL, 0x5A5A5E).pattern(Pattern.CHAINMAIL, 0x8A8A90, 2f));
                Part hand = side < 0 ? b.rHand : b.lHand;
                hand.box(-3 * K, -0.2f * K, -3 * K, 6 * K, 4.2f * K, 6 * K, limb.copy().pattern(Pattern.CRACKS, BLOOD, 1f));
                Part arm = side < 0 ? b.rArm : b.lArm;
                arm.spike(side * 2 * K, 1 * K, 1.4f * K, 0.9f * K, 2 * K, -30, 0, side * -50, bone());
            }
            rankGear(b, 5.4f * K, 5.6f * K, 5.6f * K, 13 * K, 9 * K, 8 * K);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            float move = Math.min(1, c.la);
            float ph = c.ls * 0.6662f;
            p.idle(c.age, 1.4f);
            p.walk(c.ls, c.la, 0.75f, 0, 1.8f);
            p.spine(22, 0, Mth.sin(ph) * 5 * move);
            p.pelvis.zRot += Mth.sin(ph) * 0.06f * move;
            p.armR(-20 + Mth.cos(ph) * 25 * move, 0, 12, 30);
            p.armL(-20 - Mth.cos(ph) * 25 * move, 0, -12, 30);
            p.look(c.headYaw, c.headPitch);
            p.head.xRot -= 0.35f;
            p.flinch(c.hurt * 0.6f);
            ModelPart jaw = m.p("jaw");
            jaw.xRot += 0.1f + Math.max(0, Mth.sin(c.age * 0.1f)) * 0.1f;
            m.p("chain_r").xRot -= p.rArm.xRot * 0.7f;
            m.p("chain_l").xRot -= p.lArm.xRot * 0.7f;
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float roar = Anim.hold(c.t, 0, 10, 3) * (c.t < 12 ? 1 : 0);
                float beat = Mth.sin(c.t * 1.6f) * roar;
                p.armR(-70 * roar + 20 * beat, 50 * roar, 0, 90 * roar);
                p.armL(-70 * roar - 20 * beat, -50 * roar, 0, 90 * roar);
                p.spine(-15 * roar, 0, 0);
                jaw.xRot += 0.8f * roar;
                float run = Anim.hold(c.t, 12, 40, 4);
                p.spine(25 * run, 0, 0);
                p.armR(40 * run, 0, 30 * run, 0);
                p.armL(40 * run, 0, -30 * run, 0);
                if (run > 0) p.walk(c.age * 1.5f, run, 1.2f, 0, 2);
            }
            ZGear.claw(p, c, true);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Bloater

    /** A walking plague-bomb: a vast, vein-lit belly stitched shut and studded with glowing pustules. */
    public static final class Bloater implements ZombieModels.Def {
        static final float K = 1.15f;

        @Override
        public float shadow() {
            return 1.0f;
        }

        @Override
        public void build(ModelSpec s) {
            int sk = 0x8FA860;
            Paint skin = skin(sk);
            Paint belly = p(Mat.FLESH, sk).accent(0x5A7A30).pattern(Pattern.VEINS, 0xB8FF60, 0.7f).accentGlow()
                    .decal(Face.FRONT, DecalType.STITCH, 0, false, 0.5f, 0.5f, 1.4f);
            Body.Spec sp = new Body.Spec(skin, cloth(0x5A5A4A)).scale(K);
            sp.legs(4.4f, 4.2f, 4.2f);
            sp.foot = 1.2f;
            sp.hipW = 9;
            sp.torso(9, 6.6f, 6, 9, 4.6f, 6);
            sp.neck = 0.4f;
            sp.head(6, 6, 6);
            sp.arms(4.6f, 4.6f, 3.4f);
            sp.paints(skin, cloth(0x5A5A4A), skin, skin, skin, cloth(0x4A3B2A), cloth(0x4A3B2A), skin);
            sp.headPaint(face(sk, 0xC8FF60));
            Body b = Body.build(s.root, sp);
            Part bl = b.pelvis.child("belly", 0, -4.4f * K, -2 * K);
            bl.sphere(0, 0, 0, 8.4f * K, belly);
            int[][] pus = {{-5, -3, -7}, {4, 2, -7}, {6, -4, -4}, {-6, 4, -5}, {0, -6, -6}, {2, 5, -7}};
            for (int[] q : pus) bl.sphere(q[0] * K, q[1] * K, q[2] * K, 1.3f * K, Paint.glowing(0xC8FF60).accent(0xFFFFA0));
            bl.box(-0.4f * K, -7 * K, -8.6f * K, 0.8f * K, 14 * K, 0.6f * K, p(Mat.LEATHER, 0x2A1A10));
            for (int i = 0; i < 6; i++) bl.box(-1.4f * K, -6.4f * K + i * 2.4f * K, -8.7f * K, 2.8f * K, 0.4f * K, 0.4f * K, p(Mat.LEATHER, 0x2A1A10));
            rankGear(b, 6 * K, 6 * K, 6 * K, 9 * K, 6.6f * K, 6 * K);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            float move = Math.min(1, c.la);
            float ph = c.ls * 0.6662f;
            p.idle(c.age, 1f);
            p.walk(c.ls, c.la, 0.5f, 0, 0.8f);
            p.pelvis.zRot += Mth.sin(ph) * 0.12f * move;
            p.spine(-10, 0, -Mth.sin(ph) * 5 * move);
            p.armR(-10, 0, 35, 20);
            p.armL(-10, 0, -35, 20);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            float sw = z instanceof BloaterZombie bz ? bz.swell(c.pt) : 0;
            ModelPart belly = m.p("belly");
            float pulse = 1f + Mth.sin(c.age * (0.1f + sw * 1.2f)) * (0.03f + sw * 0.08f) + sw * 0.22f;
            belly.xScale = belly.zScale = pulse;
            belly.yScale = pulse * (1 + Mth.sin(c.age * 0.2f + ph) * 0.02f);
            p.armR(0, 0, 30 * sw, 0);
            p.armL(0, 0, -30 * sw, 0);
            p.head.xRot -= 0.4f * sw;
            ZGear.claw(p, c, false);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Bulwark

    /** A dead riot-guard in scavenged plate behind a spiked tower shield, swinging a flanged mace. */
    public static final class Bulwark implements ZombieModels.Def {
        static final float K = 1.08f;

        @Override
        public float shadow() {
            return 0.6f;
        }

        @Override
        public void build(ModelSpec s) {
            int sk = 0x6E8B5E;
            Paint plate = p(Mat.PLATE, 0x5A5C64).accent(0x2A2C34).grime(0.45f).pattern(Pattern.SPOTS, RUST, 1.6f);
            Paint mail = p(Mat.METAL, 0x4A4A50).pattern(Pattern.CHAINMAIL, 0x7A7A80, 2f).grime(0.5f);
            Paint helm = p(Mat.PLATE, 0x4A4C54).accent(0x22242A).grime(0.4f).decal(Face.FRONT, DecalType.VISOR, 0xFFB040, true, 0.5f, 0.48f, 1f);
            Body.Spec sp = Walkers.standard(skin(sk), plate, mail, helm).scale(K);
            sp.paints(plate, mail, plate, plate, skin(sk), mail, plate, plate);
            Body b = Body.build(s.root, sp);
            Gear.pauldron(b.rArm, -1, 3.2f * K, plate, rusty());
            Gear.pauldron(b.lArm, 1, 3.2f * K, plate, rusty());
            b.head.box(-0.4f * K, -7.6f * K, -3.6f * K, 0.8f * K, 1.2f * K, 7.2f * K, p(Mat.CLOTH, 0x8A1010).pattern(Pattern.FUR, 0x5A0A0A, 1.2f));
            Part shield = b.lFore.child("shield", 1.4f * K, 3 * K, -2.6f * K).rot(0, 12, 0);
            Paint wood = p(Mat.WOOD, 0x5A3A20).accent(0x3A2410).pattern(Pattern.WOOD, 0x3A2410, 1.5f)
                    .decal(Face.FRONT, DecalType.SKULL, 0xD8D0B8, false, 0.5f, 0.42f, 1.4f);
            shield.box(-5.5f * K, -10 * K, -1.2f * K, 11 * K, 20 * K, 1.2f * K, wood);
            shield.box(-6 * K, -10.5f * K, -1.4f * K, 12 * K, 1.4f * K, 1.6f * K, rusty());
            shield.box(-6 * K, 9.1f * K, -1.4f * K, 12 * K, 1.4f * K, 1.6f * K, rusty());
            shield.box(-6 * K, -10.5f * K, -1.4f * K, 1.2f * K, 21 * K, 1.6f * K, rusty());
            shield.box(4.8f * K, -10.5f * K, -1.4f * K, 1.2f * K, 21 * K, 1.6f * K, rusty());
            for (int i = 0; i < 3; i++) shield.spike(0, (-6 + i * 6) * K, -1.2f * K, 1.0f * K, 2 * K, 90, 0, 0, p(Mat.METAL, 0x8A8A90));
            Part mace = b.rHand.child("mace", 0, 1.4f * K, 0);
            mace.box(-0.5f * K, -0.5f * K, -12 * K, 1 * K, 1 * K, 14 * K, p(Mat.WOOD, 0x4A3020));
            mace.octoZ(0, 0, -15 * K, 3.2f * K, 4 * K, p(Mat.METAL, 0x6A6A70).grime(0.4f));
            for (int i = 0; i < 4; i++) {
                float a = i * Mth.HALF_PI;
                mace.sub(0, 0, -13 * K).rot(0, 0, a * Mth.RAD_TO_DEG).box(-0.3f * K, -2.6f * K, -1.6f * K, 0.6f * K, 1.2f * K, 3.2f * K, p(Mat.METAL, 0x8A8A90));
            }
            rankGear(b, Walkers.HW * K, Walkers.HH * K, Walkers.HD * K, 8 * K, 6.6f * K, 4.2f * K);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            p.idle(c.age, 0.8f);
            p.walk(c.ls, c.la, 0.75f, 0, 1f);
            p.spine(6, 0, 0);
            // shield wall: shield forward, mace cocked
            p.armL(-55, -25, 0, 70);
            p.armR(-30, 10, 10, 50);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt * 0.5f);
            if (c.id == SiegeZombie.ANIM_BLOCK) {
                float k = Anim.pulse(c.t, 0, 10);
                p.armL(-20 * k, 0, 0, 10 * k);
                p.spine(10 * k, 0, 0);
                p.crouch(0.3f * k, 2);
            }
            if (c.id == SiegeZombie.ANIM_ATTACK || c.id == SiegeZombie.ANIM_CORE) {
                float sw = Anim.swing(c.t, c.windup, 3, 7);
                float up = Math.max(0, sw), hit = Math.max(0, -sw);
                p.armR(-110 * up - 40 * hit, 0, 10 * up, 30 * up);
                p.spine(-6 * up + 14 * hit, 0, 0);
            }
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Wrecker

    /** A dead demolition worker: dented hard hat, hi-vis vest, tool belt and a rebar-wrapped sledgehammer. */
    public static final class Wrecker implements ZombieModels.Def {
        static final float K = 1.18f;

        @Override
        public float shadow() {
            return 0.7f;
        }

        @Override
        public void build(ModelSpec s) {
            int sk = 0x6E8B5E;
            Paint vest = p(Mat.CLOTH, 0xE0A020).accent(0x8A6010).grime(0.5f).decal(Face.FRONT, DecalType.HSTRIPE, 0xE8E8E8, false, 0.5f, 0.55f, 1f)
                    .decal(Face.BACK, DecalType.HSTRIPE, 0xE8E8E8, false, 0.5f, 0.55f, 1f);
            Paint jeans = cloth(0x3A4A6A);
            Body.Spec sp = Walkers.standard(skin(sk), vest, jeans, face(sk, 0xFF9020)).scale(K);
            sp.torso(9, 6.8f, 4.8f, 7.6f, 4.6f, 4.2f);
            sp.arms(5.6f, 5.6f, 3.6f);
            sp.paints(vest, jeans, skin(sk), skin(sk), p(Mat.LEATHER, 0x6A4A2A), jeans, jeans, p(Mat.LEATHER, 0x4A3018).grime(0.6f));
            Body b = Body.build(s.root, sp);
            Part hat = b.head.child("hardhat", 0, -Walkers.HH * K + 1, 0);
            Paint yellow = p(Mat.PLATE, 0xD8A020).accent(0x8A6010).grime(0.5f).decal(Face.FRONT, DecalType.SCAR, 0x5A4010, false, 0.6f, 0.5f, 1f);
            hat.box(-4.2f * K, -3 * K, -4.2f * K, 8.4f * K, 3.2f * K, 8.4f * K, yellow);
            hat.box(-4.6f * K, -0.2f * K, -5.2f * K, 9.2f * K, 0.5f * K, 9.4f * K, yellow);
            hat.box(-0.5f * K, -3.4f * K, -4.3f * K, 1 * K, 0.6f * K, 8.6f * K, yellow);
            Gear.belt(b.pelvis, 7.6f * K, 4.2f * K, -1.4f * K, 1.4f * K, p(Mat.LEATHER, 0x5A3A1A), p(Mat.METAL, 0xA8A8A8));
            b.pelvis.box(3 * K, -1.2f * K, -1.6f * K, 1.6f * K, 2.6f * K, 3.2f * K, p(Mat.LEATHER, 0x6A4A2A));
            b.pelvis.box(-4.6f * K, -1.2f * K, -1 * K, 1.4f * K, 3 * K, 2 * K, p(Mat.METAL, 0x8A8A8A));
            Part hammer = b.rHand.child("hammer", 0, 1.4f * K, 0);
            hammer.box(-0.6f * K, -0.6f * K, -18 * K, 1.2f * K, 1.2f * K, 22 * K, p(Mat.WOOD, 0x5A3A20).pattern(Pattern.WOOD, 0x3A2410, 1.5f));
            hammer.box(-3.4f * K, -2.4f * K, -22 * K, 6.8f * K, 4.8f * K, 5 * K, p(Mat.METAL, 0x6A6A6A).grime(0.5f).pattern(Pattern.CRACKS, 0x3A3A3A, 1f));
            hammer.box(-3.6f * K, -2.6f * K, -20.4f * K, 7.2f * K, 5.2f * K, 1.2f * K, rusty());
            for (int i = 0; i < 3; i++) hammer.box(-0.8f * K, -0.8f * K, (-14 + i * 3) * K, 1.6f * K, 1.6f * K, 0.6f * K, rusty());
            rankGear(b, Walkers.HW * K, Walkers.HH * K, Walkers.HD * K, 9 * K, 6.8f * K, 4.8f * K);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            m.p("hardhat").visible = c.rank < 2;
            p.idle(c.age, 1f);
            p.walk(c.ls, c.la, 0.8f, 0, 1.3f);
            p.spine(10, 0, 0);
            // hammer slung over the right shoulder
            p.armR(-150, -10, 0, 120);
            p.armL(-10, 0, -8, 20);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt * 0.6f);
            if (c.id == SiegeZombie.ANIM_ATTACK || c.id == SiegeZombie.ANIM_CORE) {
                float sw = Anim.swing(c.t, c.windup, 3, 7);
                float up = Math.max(0, sw), hit = Math.max(0, -sw);
                p.armR(-20 * up + 100 * hit, 10 * up, 0, -80 * up - 120 * hit);
                p.armL(-150 * up - 40 * hit, 30 * up, 0, 20 * up);
                p.spine(-15 * up + 35 * hit, 0, 0);
                p.crouch(hit * 0.4f, 3);
            }
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Abomination

    /**
     * Many corpses stitched into one: a swollen torso, a second head on the shoulder, a third arm sprouting
     * from the back, crawlers clinging to its hide, bone spurs and staples everywhere.
     */
    public static final class Abomination implements ZombieModels.Def {
        static final float K = 1.6f;

        @Override
        public float shadow() {
            return 1.3f;
        }

        @Override
        public void build(ModelSpec s) {
            int sk = 0x8A9070;
            Paint hide = p(Mat.FLESH, sk).accent(0x5A6048).pattern(Pattern.VEINS, 0x7A3A4A, 1f).grime(0.4f)
                    .decal(Face.FRONT, DecalType.STITCH, 0, false, 0.3f, 0.4f, 1.3f).decal(Face.FRONT, DecalType.STITCH, 0, false, 0.7f, 0.7f, 1.1f)
                    .decal(Face.BACK, DecalType.STITCH, 0, false, 0.5f, 0.5f, 1.4f).decal(Face.LEFT, DecalType.STITCH, 0, false, 0.5f, 0.5f, 1f);
            Paint pale = p(Mat.FLESH, 0xA8A890).accent(0x6A6A58).pattern(Pattern.SPOTS, 0x7A7A60, 1.3f).decal(Face.RIGHT, DecalType.STITCH, 0, false, 0.5f, 0.5f, 1f);
            Paint dark = p(Mat.FLESH, 0x6A6A50).accent(0x3A3A28).pattern(Pattern.MUSCLE, 0x4A4A38, 1.2f);
            Body.Spec sp = new Body.Spec(hide, hide).scale(K);
            sp.legs(5.2f, 5, 5);
            sp.foot = 1.4f;
            sp.hipW = 10;
            sp.torso(14, 9, 9, 11, 5, 8);
            sp.neck = 0.6f;
            sp.head(6, 6.4f, 6.4f);
            sp.arms(7, 7.4f, 4.6f);
            sp.hand = 3;
            sp.paints(hide, hide, pale, dark, dark, dark, pale, p(Mat.FLESH, 0x5A5A44));
            sp.headPaint(face(sk, 0xC8FF60));
            Body b = Body.build(s.root, sp);
            b.neck.pz -= 2 * K;
            b.neck.py += 1.6f * K;
            Paint staple = p(Mat.METAL, 0x8A8A8A).grime(0.4f);
            for (int i = 0; i < 6; i++) b.chest.box(-5 * K + i * 2 * K, -6 * K + (i % 2) * 2 * K, -4.65f * K, 1.4f * K, 0.4f * K, 0.4f * K, staple);
            // belly of faces
            Part belly = b.pelvis.child("belly", 0, -3.4f * K, -3 * K);
            belly.sphere(0, 0, 0, 5 * K, hide);
            belly.box(-2.2f * K, -1.6f * K, -5.1f * K, 4.4f * K, 3.6f * K, 0.4f * K, face(0x9A9A80, 0xC8FF60));
            // second head on the left shoulder
            Part head2 = b.chest.child("head2", 4.6f * K, -9 * K, -1 * K).rot(0, 0, 25);
            head2.box(-2.4f * K, -4.8f * K, -2.4f * K, 4.8f * K, 4.8f * K, 4.8f * K, face(0xA8A890, 0xFFD040));
            Part jaw2 = head2.child("jaw2", 0, -0.6f * K, -0.4f * K);
            jaw2.box(-2 * K, 0, -2.2f * K, 4 * K, 1.4f * K, 2.6f * K, p(Mat.FLESH, 0x8A6A6A).decal(Face.FRONT, DecalType.FANGS, 0xE0D8C0, false, 0.5f, 0.2f, 1f));
            // third arm from the back
            Part arm3 = b.chest.child("arm3", -4 * K, -8 * K, 3.6f * K).rot(-30, 0, 40);
            arm3.box(-1.8f * K, -0.6f * K, -1.8f * K, 3.6f * K, 7 * K, 3.6f * K, pale);
            Part fore3 = arm3.child("fore3", 0, 7 * K, 0).rot(-50, 0, 0);
            fore3.box(-1.6f * K, 0, -1.6f * K, 3.2f * K, 6.6f * K, 3.2f * K, dark);
            for (int i = -1; i <= 1; i++) fore3.spike(i * 0.9f * K, 6.6f * K, 0, 0.6f * K, 2 * K, 180, 0, i * 10, bone());
            // a crawler clinging to its back, bone spurs, crude jaw
            Part rider = b.chest.child("rider", 3 * K, -4 * K, 4.8f * K).rot(-15, 160, 0);
            rider.box(-2 * K, -4.4f * K, -1.4f * K, 4 * K, 4.4f * K, 2.8f * K, cloth(0x5A4A3A));
            rider.box(-1.8f * K, -8 * K, -1.8f * K, 3.6f * K, 3.6f * K, 3.6f * K, face(0x6A7A58, 0xFFE066));
            rider.box(-3 * K, -4.2f * K, -1 * K, 1.2f * K, 5 * K, 1.2f * K, skin(0x6A7A58));
            rider.box(1.8f * K, -4.2f * K, -1 * K, 1.2f * K, 5 * K, 1.2f * K, skin(0x6A7A58));
            for (int i = 0; i < 3; i++) b.chest.spike((i - 1) * 3 * K, -9 * K, 2 * K, 1.1f * K, 2.4f * K, -30, 0, (i - 1) * 20, bone());
            Part jaw = b.head.child("jaw", 0, -0.6f * K, -0.6f * K);
            jaw.box(-2.6f * K, 0, -3.2f * K, 5.2f * K, 1.8f * K, 3.6f * K, p(Mat.FLESH, 0x7A6A5A).decal(Face.FRONT, DecalType.FANGS, 0xE0D8C0, false, 0.5f, 0.2f, 1f));
            for (int side = -1; side <= 1; side += 2) {
                Part hand = side < 0 ? b.rHand : b.lHand;
                hand.box(-2.6f * K, -0.2f * K, -2.6f * K, 5.2f * K, 3.8f * K, 5.2f * K, dark);
            }
            rankGear(b, 6 * K, 6.4f * K, 6.4f * K, 14 * K, 9 * K, 9 * K);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            ModelPart arm3 = m.p("arm3"), fore3 = m.p("fore3"), head2 = m.p("head2"), jaw = m.p("jaw"), jaw2 = m.p("jaw2"), belly = m.p("belly");
            float move = Math.min(1, c.la);
            float ph = c.ls * 0.6662f;
            p.idle(c.age, 1.6f);
            p.walk(c.ls, c.la, 0.6f, 0, 2.2f);
            p.pelvis.zRot += Mth.sin(ph) * 0.1f * move;
            p.spine(18, Mth.sin(ph) * 6 * move, Mth.sin(ph) * 6 * move);
            p.armR(-25 + Mth.cos(ph) * 25 * move, 0, 15, 30);
            p.armL(-35 - Mth.cos(ph) * 20 * move, 0, -15, 40);
            p.look(c.headYaw, c.headPitch);
            p.head.xRot -= 0.25f;
            p.flinch(c.hurt * 0.5f);
            head2.zRot += Mth.sin(c.age * 0.11f) * 0.2f;
            head2.xRot += Mth.sin(c.age * 0.07f + 1) * 0.15f;
            jaw.xRot += 0.15f + Mth.sin(c.age * 0.13f) * 0.1f;
            jaw2.xRot += 0.3f + Mth.sin(c.age * 0.31f) * 0.2f;
            arm3.xRot += Mth.sin(c.age * 0.09f) * 0.25f;
            arm3.zRot += Mth.sin(c.age * 0.13f) * 0.15f;
            fore3.xRot += Mth.sin(c.age * 0.15f + 1) * 0.3f;
            belly.yScale = belly.xScale = 1 + Mth.sin(c.age * 0.12f + ph) * 0.03f;
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float reach = Anim.hold(c.t, 0, 14, 3) * (c.t < 16 ? 1 : 0);
                float thr = Anim.pulse(c.t, 14, 10);
                arm3.xRot += -1.4f * reach + 2.2f * thr;
                fore3.xRot += 0.8f * reach - 0.6f * thr;
                p.armR(-60 * reach - 150 * thr, 0, 0, 40 * reach);
                p.spine(-15 * reach + 25 * thr, -30 * reach + 30 * thr, 0);
                jaw.xRot += 0.6f * thr;
            }
            if (c.id == SiegeZombie.ANIM_SPECIAL2) {
                float up = c.t < 18 ? Anim.smooth(Anim.win(c.t, 0, 14)) : 0;
                float slam = Anim.hold(c.t, 18, 26, 5);
                p.armR(-170 * up - 60 * slam, 0, 15 * up, 0);
                p.armL(-170 * up - 60 * slam, 0, -15 * up, 0);
                arm3.xRot -= 1.2f * up - 1.5f * slam;
                p.spine(-20 * up + 40 * slam, 0, 0);
                p.crouch(slam * 0.6f, 6);
                jaw.xRot += 0.6f * slam;
                jaw2.xRot += 0.8f * (up + slam);
            }
            ZGear.claw(p, c, true);
            ZGear.common(m, p, z, c);
        }
    }
}
