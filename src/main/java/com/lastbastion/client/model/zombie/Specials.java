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
import com.lastbastion.entity.zombie.SiegeZombie;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.lastbastion.client.model.zombie.ZGear.*;

/** The odd ones: Burrower, Wraith, Necromancer and Incinerator. */
public final class Specials {
    private Specials() {
    }

    // ================================================================== Burrower

    /** Mole-like mutant: armoured back plates, shovel claws, a blunt snout and dirt caked over everything. */
    public static final class Burrower implements ZombieModels.Def {
        @Override
        public void build(ModelSpec s) {
            int sk = 0x6B5A3A;
            Paint dirt = p(Mat.ROT, sk).accent(0x4A3A20).pattern(Pattern.SPOTS, 0x4A3A20, 1.2f).grime(0.7f);
            Paint head = p(Mat.ROT, 0x7A6A4A).accent(0x4A3A20).pattern(Pattern.SPOTS, 0x5A4A2A, 1.2f)
                    .decal(Face.FRONT, DecalType.SLIT_EYES, 0xFFD040, true, 0.5f, 0.38f, 0.9f);
            Body.Spec sp = Walkers.standard(dirt, dirt, cloth(0x4A3B2A), head);
            sp.legs(4.6f, 4.6f, 3.8f);
            sp.torso(8.6f, 6.6f, 5, 7.4f, 4.4f, 4.4f);
            sp.head(6.4f, 5.6f, 6.4f);
            sp.arms(4.8f, 4.8f, 3.8f);
            Body b = Body.build(s.root, sp);
            Paint plates = p(Mat.CHITIN, 0x4A3A28).accent(0x2A2014).pattern(Pattern.SCALES, 0x6A5A40, 1.2f).gloss(0.3f);
            for (int i = 0; i < 4; i++) b.chest.box(-4.6f + i * 0.2f, -6.6f + i * 1.7f, 2.2f, 9.2f - i * 0.4f, 1.9f, 1.2f, plates);
            b.pelvis.box(-3.8f, -3.6f, 2, 7.6f, 2.4f, 1.2f, plates);
            b.head.box(-2, -3, -5.4f, 4, 2.6f, 2.6f, p(Mat.FLESH, 0x8A6A5A).decal(Face.FRONT, DecalType.DOTS, 0x2A1A10, false, 0.5f, 0.5f, 1f));
            for (int side = -1; side <= 1; side += 2) {
                Part hand = side < 0 ? b.rHand : b.lHand;
                hand.box(-2.8f, 0.4f, -1.2f, 5.6f, 4, 1.4f, p(Mat.BONE, 0x9A9080).accent(0x5A5040).pattern(Pattern.STRIPES, 0x5A5040, 1f));
                for (int i = -1; i <= 1; i++) hand.spike(i * 1.6f, 4.2f, -0.6f, 0.9f, 2.2f, 180, 0, i * 6, bone());
            }
            rankGear(b, 6.4f, 5.6f, 6.4f, 8.6f, 6.6f, 5);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            float move = Math.min(1, c.la);
            p.idle(c.age, 1f);
            p.walk(c.ls, c.la, 0.8f, 0, 1.2f);
            p.spine(35, 0, 0);
            p.head.xRot -= 0.6f;
            float dig = Mth.sin(c.age * 0.5f + c.variant);
            p.armR(-60 + dig * 15 * (1 - move), 15, 10, 40);
            p.armL(-60 - dig * 15 * (1 - move), -15, -10, 40);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float k = Anim.hold(c.t, 0, 20, 3);
                float scoop = Mth.sin(c.t * 1.2f) * 40 * k;
                p.crouch(0.9f * k, 6);
                p.armR(-60 * k + scoop, 0, 0, 0);
                p.armL(-60 * k - scoop, 0, 0, 0);
                p.spine(30 * k, 0, 0);
            }
            if (c.id == SiegeZombie.ANIM_SPECIAL2) {
                float burst = 1 - Anim.win(c.t, 0, 15);
                p.armR(-170 * burst, 0, 20 * burst, 0);
                p.armL(-170 * burst, 0, -20 * burst, 0);
                p.spine(-30 * burst, 0, 0);
            }
            ZGear.claw(p, c, true);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Wraith

    /** A drifting hooded specter: a void face with two cold eyes, ghostly claws and a robe trailing into mist. */
    public static final class Wraith implements ZombieModels.Def {
        @Override
        public boolean translucent() {
            return true;
        }

        @Override
        public float shadow() {
            return 0.2f;
        }

        @Override
        public void build(ModelSpec s) {
            Paint robe = p(Mat.ROBE, 0x40506A).accent(0x101820).pattern(Pattern.GRADIENT, 0x101820, 1f).grime(0.3f);
            Paint voidP = p(Mat.PLAIN, 0x0A0E16).decal(Face.FRONT, DecalType.EYES, 0x7FFFF0, true, 0.5f, 0.5f, 1.1f);
            Paint claw = p(Mat.ENERGY, 0x9FE8E0).accent(0x40A0A0).glow();
            Body.Spec sp = Walkers.standard(robe, robe, robe, voidP);
            sp.paints(robe, robe, robe, robe, claw, robe, robe, robe);
            Body b = Body.build(s.root, sp);
            Part skirt = b.pelvis.child("robe", 0, 0, 0);
            skirt.taper(0, 9, 0, 9, 7, 7, 5, 9, 3, robe);
            Part tail = skirt.child("tail", 0, 9, 1);
            tail.box(-3.5f, 0, -2.4f, 7, 3, 4.8f, robe).box(-2.6f, 3, -1.8f, 5.2f, 3, 3.6f, robe).box(-1.4f, 6, -1, 2.8f, 3, 2, robe);
            Part hood = b.head.child("hood", 0, 0, 0);
            hood.box(-4, -7.6f, -3.6f, 8, 0.8f, 7.8f, robe);
            hood.box(-4, -7.6f, 3.4f, 8, 7.6f, 0.8f, robe);
            hood.box(-4, -7.6f, -3.8f, 0.8f, 7.6f, 7.6f, robe);
            hood.box(3.2f, -7.6f, -3.8f, 0.8f, 7.6f, 7.6f, robe);
            hood.spike(0, -7.6f, 2, 1.2f, 3, -60, 0, 0, robe);
            for (int side = -1; side <= 1; side += 2) {
                Part hand = side < 0 ? b.rHand : b.lHand;
                for (int i = -1; i <= 1; i++) hand.spike(i * 0.7f, 2, -0.2f, 0.5f, 3, 175, 0, i * 8, claw);
                Part fore = side < 0 ? b.rFore : b.lFore;
                fore.taper(0, 5.4f, 0, 4.8f, 4.8f, 3, 3, 5.4f, 2, robe);
            }
            b.chest.box(-1.4f, -4.6f, -2.3f, 2.8f, 2.8f, 0.3f, Paint.glowing(0x7FFFF0));
            Part chains = b.chest.child("chains", 0, -1, 0);
            for (int i = 0; i < 4; i++) chains.box(-4.4f + i * 0.4f, -5.4f + i * 1.4f, -2.4f, 8.8f - i * 0.8f, 0.5f, 0.5f, p(Mat.METAL, 0x5A6A7A).glow());
            rankGear(b, Walkers.HW, Walkers.HH, Walkers.HD, 8, 6.6f, 4.2f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            float move = Math.min(1, c.la);
            float bob = Mth.sin(c.age * 0.12f + c.variant) * 1.2f;
            p.idle(c.age, 1f);
            p.lift(bob - 3);
            p.spine(12 + 15 * move, 0, 0);
            p.rThigh.xRot -= 0.3f;
            p.lThigh.xRot -= 0.2f;
            p.rShin.xRot += 0.6f;
            p.lShin.xRot += 0.5f;
            p.armR(-50 + Mth.sin(c.age * 0.1f) * 10, 10, 15, 30);
            p.armL(-50 - Mth.sin(c.age * 0.1f) * 10, -10, -15, 30);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            ModelPart tail = m.p("tail");
            tail.xRot += 0.35f + move * 0.5f + Mth.sin(c.age * 0.2f) * 0.15f;
            tail.zRot += Mth.sin(c.age * 0.13f) * 0.12f;
            if (c.id == SiegeZombie.ANIM_CORE || c.id == SiegeZombie.ANIM_ATTACK) {
                float sw = Anim.swing(c.t, c.windup, 3, 7);
                float up = Math.max(0, sw), hit = Math.max(0, -sw);
                p.armR(-110 * up - 20 * hit, 30 * up - 40 * hit, 0, 0);
                p.armL(-110 * up - 20 * hit, -30 * up + 40 * hit, 0, 0);
                p.spine(-10 * up + 25 * hit, 0, 0);
            }
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Necromancer

    /** A robed cultist in a bone mask, with a soul-lantern staff and a grimoire floating at its side. */
    public static final class Necromancer implements ZombieModels.Def {
        @Override
        public void build(ModelSpec s) {
            Paint robe = p(Mat.ROBE, 0x2A1A3A).accent(0x1A1028).trim(0x6A5A3A, 0.6f).pattern(Pattern.RUNES, 0x3A6A4A, 1.4f);
            Paint robeR = robe.copy().decal(Face.FRONT, DecalType.RUNE, 0x6BFF8A, true, 0.5f, 0.45f, 1.2f);
            Paint mask = p(Mat.BONE, 0xC8C0B0).accent(0x6A6458).decal(Face.FRONT, DecalType.HOLLOW_EYES, 0x6BFF8A, true, 0.5f, 0.45f, 1.1f)
                    .decal(Face.FRONT, DecalType.FANGS, 0x2A2420, false, 0.5f, 0.85f, 0.8f);
            Paint hands = skin(0x8A9A7A);
            Body.Spec sp = Walkers.standard(hands, robeR, robe, mask);
            sp.paints(robeR, robe, robe, robe, hands, robe, robe, robe);
            Body b = Body.build(s.root, sp);
            Part skirt = b.pelvis.child("robe", 0, 0, 0);
            skirt.taper(0, 12.5f, 0, 10, 8, 7, 5, 12.5f, 4, robe);
            Part hood = b.head.child("hood", 0, 0, 0);
            hood.box(-4, -7.6f, -3.6f, 8, 1, 7.8f, robe);
            hood.box(-4, -7.6f, 3.4f, 8, 8, 0.8f, robe);
            hood.box(-4.1f, -7.6f, -3.8f, 0.8f, 8, 7.6f, robe);
            hood.box(3.3f, -7.6f, -3.8f, 0.8f, 8, 7.6f, robe);
            for (int i = 0; i < 4; i++) b.chest.sphere(-3 + i * 2, -1 + (i % 2) * 0.6f, -2.3f, 0.6f, bone());
            for (int side = -1; side <= 1; side += 2) {
                Part fore = side < 0 ? b.rFore : b.lFore;
                fore.taper(0, 5.4f, 0, 4.8f, 4.8f, 3.2f, 3.2f, 5.4f, 2, robe);
            }
            Part staff = b.rHand.child("staff", 0, 1.2f, 0);
            staff.box(-0.5f, -0.5f, -18, 1, 1, 26, p(Mat.WOOD, 0x3A2A1A).pattern(Pattern.WOOD, 0x1A1008, 1.5f));
            staff.box(-1.6f, -1.6f, -19, 3.2f, 0.6f, 3.2f, p(Mat.BONE, 0xC8C0B0));
            Part lantern = staff.child("lantern", 0, 0, -21.5f);
            lantern.box(-1.6f, -1.6f, -1.6f, 3.2f, 3.2f, 3.2f, p(Mat.METAL, 0x3A3A30).grime(0.5f));
            lantern.sphere(0, 0, 0, 1.4f, Paint.glowing(0x6BFF8A).pattern(Pattern.VEINS, 0xFFFFFF, 0.8f));
            Part book = b.lHand.child("book", 0, 2.4f, -3.4f).rot(-40, 0, 0);
            book.box(-2.6f, -0.6f, -1.8f, 5.2f, 1.2f, 3.6f, p(Mat.LEATHER, 0x4A1A1A).trim(0xA08040, 0.6f));
            book.box(-2.4f, -0.8f, -1.6f, 4.8f, 0.3f, 3.2f, p(Mat.CLOTH, 0xE0D8C0).decal(Face.TOP, DecalType.RUNE, 0x6BFF8A, true, 0.5f, 0.5f, 1f));
            rankGear(b, Walkers.HW, Walkers.HH, Walkers.HD, 8, 6.6f, 4.2f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            float hover = Mth.sin(c.age * 0.1f) * 0.6f;
            p.idle(c.age, 1f);
            p.lift(hover - 0.6f);
            p.walk(c.ls, c.la, 0.4f, 0, 0.3f);
            p.spine(8, 0, 0);
            p.armR(-25, 10, 6, 70);
            p.armL(-45, -20, -6, 60);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            m.p("lantern").yRot = c.age * 0.08f;
            m.p("book").y += Mth.sin(c.age * 0.15f) * 0.5f;
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float r = Anim.pulse(c.t, 0, 30);
                p.armR(-110 * r, 0, 10 * r, -50 * r);
                p.armL(-100 * r, -20 * r, -40 * r, -40 * r);
                p.head.xRot -= 0.5f * r;
                p.spine(-15 * r, 0, 0);
                p.lift(-2 * r);
            }
            ZGear.claw(p, c, false);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Incinerator

    /** A husk in a scorched hazmat suit and gas mask, twin fuel tanks on its back, a hose-fed flamethrower in hand. */
    public static final class Incinerator implements ZombieModels.Def {
        @Override
        public float shadow() {
            return 0.6f;
        }

        @Override
        public void build(ModelSpec s) {
            Paint suit = p(Mat.RUBBER, 0xC88A2A).accent(0x6A4A14).grime(0.65f).pattern(Pattern.SPOTS, 0x3A2A1A, 1.4f)
                    .decal(Face.FRONT, DecalType.HSTRIPE, 0xD8D8D0, false, 0.5f, 0.6f, 1f);
            Paint suitPlain = p(Mat.RUBBER, 0xC88A2A).accent(0x6A4A14).grime(0.65f).pattern(Pattern.SPOTS, 0x3A2A1A, 1.4f);
            Paint mask = p(Mat.RUBBER, 0x2A2A2A).accent(0x141414).decal(Face.FRONT, DecalType.GRILLE, 0x6A6A6A, false, 0.5f, 0.8f, 0.8f);
            Paint boots = p(Mat.RUBBER, 0x1E1E1E).grime(0.5f);
            Body.Spec sp = Walkers.standard(suitPlain, suit, suitPlain, mask);
            sp.torso(8.4f, 6.8f, 4.6f, 7.4f, 4.6f, 4.2f);
            sp.paints(suit, suitPlain, suitPlain, suitPlain, boots, suitPlain, suitPlain, boots);
            Body b = Body.build(s.root, sp);
            // gas mask: goggles and filter canister
            Paint lens = Paint.glowing(0xFF9020).accent(0xFFE0A0);
            b.head.box(-2.6f, -4.8f, -3.5f, 2, 2, 0.4f, lens);
            b.head.box(0.6f, -4.8f, -3.5f, 2, 2, 0.4f, lens);
            b.head.octoZ(0, -1.6f, -5.2f, 2.2f, 2, p(Mat.METAL, 0x5A5A5A).grime(0.5f));
            b.head.box(-3.6f, -7.2f, -3.6f, 7.2f, 0.6f, 7.2f, suitPlain);
            // twin tanks with valves and a warning stripe
            Paint tank = p(Mat.METAL, 0x8A2A1A).accent(0x4A140A).grime(0.5f).decal(Face.BACK, DecalType.HSTRIPE, 0xE0C020, false, 0.5f, 0.3f, 1f)
                    .decal(Face.BACK, DecalType.FLAME, 0xFFD060, false, 0.5f, 0.65f, 0.8f);
            Part tanks = b.chest.child("tanks", 0, -3.4f, 3.2f);
            for (int side = -1; side <= 1; side += 2) {
                tanks.box(side * 1.9f - 1.6f, -3.6f, -0.6f, 3.2f, 8, 3.2f, tank);
                tanks.box(side * 1.9f - 0.6f, -4.6f, 0.4f, 1.2f, 1, 1.2f, p(Mat.METAL, 0xA8A8A8));
            }
            tanks.box(-3.6f, -1, -0.8f, 7.2f, 0.8f, 3.6f, p(Mat.LEATHER, 0x2A1E14));
            // flamethrower held in both hands, hose to the tanks
            Part gun = b.rHand.child("flamer", 0, 1.4f, 0);
            Paint metal = p(Mat.DARK_METAL, 0x3A3A3E).accent(0x1E1E20).grime(0.4f);
            gun.box(-0.8f, -1.2f, -9, 1.6f, 2, 11, metal);
            gun.octoZ(0, -0.2f, -13, 1.6f, 4, p(Mat.METAL, 0x6A6A6A).grime(0.5f));
            gun.box(-0.6f, 0.6f, -2, 1.2f, 2.4f, 1.4f, metal);
            gun.box(-1.4f, -2.4f, -7, 2.8f, 1.2f, 3, p(Mat.METAL, 0x8A2A1A));
            Part pilot = gun.child("pilot", 0, -0.2f, -13.4f);
            pilot.spike(0, 0, 0, 0.8f, 1.8f, 90, 0, 0, Paint.glowing(0x60A0FF).accent(0xFFFFFF));
            Part hose = gun.child("hose", 0, 0.6f, 1.6f);
            for (int i = 0; i < 4; i++) hose.box(-0.5f, -0.5f + i * 0.4f, i * 1.4f, 1, 1, 1.5f, p(Mat.RUBBER, 0x2A2A2A));
            rankGear(b, Walkers.HW, Walkers.HH, Walkers.HD, 8.4f, 6.8f, 4.6f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            p.idle(c.age, 0.9f);
            p.walk(c.ls, c.la, 0.75f, 0, 1.1f);
            p.spine(8, 0, 0);
            // flamethrower carried at the hip, left hand on the barrel
            p.armR(-35, 15, 0, 55);
            p.armL(-55, -35, 0, 70);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            ModelPart pilot = m.p("pilot");
            pilot.xScale = pilot.zScale = 1 + Mth.sin(c.age * 1.7f) * 0.2f;
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float aim = Anim.hold(c.t, 2, 40, 4);
                float sweep = Mth.sin(c.t * 0.25f) * 12 * aim;
                p.armR(-45 * aim, sweep, 0, -25 * aim);
                p.armL(-30 * aim, sweep, 0, -20 * aim);
                p.stance(0.8f * aim);
                p.spine(0, sweep * 0.5f, 0);
                pilot.xScale = pilot.yScale = pilot.zScale = 1 + 0.8f * aim;
            }
            ZGear.claw(p, c, false);
            ZGear.common(m, p, z, c);
        }
    }
}
