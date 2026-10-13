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

/** The man-sized dead: Shambler, Runner, Spitter, Leaper, Shrieker and Crawler. */
public final class Walkers {
    private Walkers() {
    }

    /** Head size and chest dims of the standard zombie body. */
    static final float HW = 6.6f, HH = 6.8f, HD = 6.6f;

    static Body.Spec standard(Paint skin, Paint shirt, Paint pants, Paint head) {
        Body.Spec sp = new Body.Spec(skin, shirt);
        sp.legs(5.6f, 5.6f, 3.6f);
        sp.foot = 1.3f;
        sp.hipW = 7;
        sp.torso(8, 6.6f, 4.2f, 7, 4.6f, 3.8f);
        sp.neck = 0.8f;
        sp.head(HW, HH, HD);
        sp.arms(5.4f, 5.4f, 3.2f);
        sp.hand = 2.2f;
        sp.paints(shirt, pants, shirt, skin, skin, pants, pants, p(Mat.LEATHER, 0x3A2A1E).grime(0.6f));
        sp.headPaint(head);
        sp.neckP = skin;
        return sp;
    }

    // ================================================================== Shambler

    /** The rank and file: torn clothes in four outfits (office, farmhand, soldier, hoodie), exposed ribs, a hanging jaw. */
    public static final class Shambler implements ZombieModels.Def {
        @Override
        public void build(ModelSpec s) {
            int sk = 0x6E8B5E;
            Paint skin = skin(sk), shirt = cloth(0x7A8A9A), pants = cloth(0x4A3B2A);
            Body b = Body.build(s.root, standard(skin, shirt, pants, face(sk, 0xFFE066)));
            tatters(b.chest, 8, 4.2f, -0.2f, 6, 2.2f, shirt);
            ribs(b.chest, 1.6f, -5.6f, -2.1f, 4, 2.6f);
            b.lFore.spike(0, 0.6f, 1.4f, 0.7f, 1.6f, -110, 0, 0, bone());
            Part jaw = b.head.child("jaw", 0, -1.4f, -0.6f);
            jaw.box(-2.6f, 0, -3.2f, 5.2f, 1.6f, 3.4f, skin.copy().decal(Face.FRONT, DecalType.FANGS, 0xC8C0A0, false, 0.5f, 0.2f, 1f));
            Gear.hair(b.head, HW, HH, HD, 0.4f, 0.25f, p(Mat.HAIR, 0x2A2420)).levels(1, 2);
            // outfit variants (toggled per zombie)
            Part office = b.chest.child("v_office", 0, 0, 0);
            office.box(-0.5f, -6.4f, -2.35f, 1, 4.6f, 0.3f, p(Mat.CLOTH, 0x8A1A1A));
            office.box(-4.3f, -6.6f, -2.3f, 2.2f, 6.6f, 4.6f, cloth(0x2A2A34));
            office.box(2.1f, -6.6f, -2.3f, 2.2f, 6.6f, 4.6f, cloth(0x2A2A34));
            Part farm = b.chest.child("v_farm", 0, 0, 0);
            farm.box(-2.6f, -4.4f, -2.35f, 5.2f, 4.6f, 0.4f, cloth(0x3A5A8A));
            farm.box(-2.6f, -6.6f, -2.3f, 0.9f, 2.4f, 4.6f, cloth(0x3A5A8A));
            farm.box(1.7f, -6.6f, -2.3f, 0.9f, 2.4f, 4.6f, cloth(0x3A5A8A));
            Part hat = b.head.child("v_strawhat", 0, -HH + 0.6f, 0);
            hat.box(-5, -0.4f, -5, 10, 0.6f, 10, p(Mat.WOOD, 0xC8A85A).pattern(Pattern.CLOTH, 0x9A7A3A, 2f));
            hat.box(-3.6f, -2.6f, -3.6f, 7.2f, 2.4f, 7.2f, p(Mat.WOOD, 0xC8A85A).pattern(Pattern.CLOTH, 0x9A7A3A, 2f));
            Part army = b.chest.child("v_army", 0, 0, 0);
            army.box(-4.3f, -6.4f, -2.4f, 8.6f, 5.4f, 4.8f, p(Mat.CLOTH, 0x4A5A34).pattern(Pattern.SPOTS, 0x3A4428, 1.4f).grime(0.5f));
            army.box(-0.6f, -5.4f, -2.6f, 1.2f, 1.6f, 0.2f, p(Mat.METAL, 0xA8A8A8));
            Part helmet = b.head.child("v_helmet", 0, -HH + 1.6f, 0);
            helmet.box(-4, -2.4f, -4, 8, 2.6f, 8, p(Mat.METAL, 0x4A5A34).grime(0.5f));
            helmet.box(-4.4f, 0, -4.4f, 8.8f, 0.5f, 8.8f, p(Mat.METAL, 0x3A4428));
            Part hood = b.chest.child("v_hood", 0, -6.6f, 2.1f);
            hood.box(-3.2f, -3.4f, -0.6f, 6.4f, 3.6f, 1.8f, cloth(0x6A2A2A));
            b.chest.child("v_hoodie", 0, 0, 0).box(-4.2f, -6.7f, -2.25f, 8.4f, 6.8f, 4.5f, cloth(0x6A2A2A));
            rankGear(b, HW, HH, HD, 8, 6.6f, 4.2f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            int v = c.variant % 4;
            boolean hatsOk = c.rank < 2;
            m.p("v_office").visible = v == 0;
            m.p("v_farm").visible = v == 1;
            m.p("v_strawhat").visible = v == 1 && hatsOk;
            m.p("v_army").visible = v == 2;
            m.p("v_helmet").visible = v == 2 && hatsOk;
            m.p("v_hood").visible = v == 3;
            m.p("v_hoodie").visible = v == 3;
            ZGear.shamble(p, c, 80 + (c.variant % 3) * 8, c.variant % 2 == 0);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            m.p("jaw").xRot += 0.25f + Mth.sin(c.age * 0.15f + c.variant) * 0.12f;
            ZGear.claw(p, c, false);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Runner

    /** Gaunt sprinter in a torn tracksuit, bloody jaw hanging open, long clawed arms. */
    public static final class Runner implements ZombieModels.Def {
        @Override
        public void build(ModelSpec s) {
            int sk = 0x7E8F6A;
            Paint skin = wounded(sk), suit = cloth(0x2A3A6A).copy().decal(Face.FRONT, DecalType.STRIPE, 0xD8D8D8, false, 0.2f, 0.5f, 1f);
            Paint head = face(sk, 0xFF3020).decal(Face.FRONT, DecalType.BLOOD, BLOOD, false, 0.5f, 0.85f, 0.9f);
            Body.Spec sp = standard(skin, suit, cloth(0x2A3A6A), head);
            sp.legs(6, 6, 3.2f);
            sp.torso(7, 6.4f, 3.8f, 6, 4.4f, 3.4f);
            sp.head(6.2f, 6.6f, 6.2f);
            sp.arms(6.2f, 6.2f, 2.8f);
            sp.paints(suit, cloth(0x2A3A6A), suit, skin, skin, cloth(0x2A3A6A), skin, p(Mat.RUBBER, 0xD8D8D0).grime(0.5f));
            Body b = Body.build(s.root, sp);
            tatters(b.chest, 7, 3.8f, -0.2f, 5, 2.6f, suit);
            ribs(b.chest, -1.4f, -5.2f, -1.9f, 3, 2.2f);
            Part jaw = b.head.child("jaw", 0, -1.2f, -0.5f);
            jaw.box(-2.4f, 0, -3, 4.8f, 1.6f, 3.2f, p(Mat.FLESH, 0x8A3A3A).decal(Face.FRONT, DecalType.FANGS, 0xE0D8C0, false, 0.5f, 0.2f, 1f));
            Gear.hair(b.head, 6.2f, 6.6f, 6.2f, 0.4f, 0.4f, p(Mat.HAIR, 0x3A2A1A)).levels(1, 2);
            for (int side = -1; side <= 1; side += 2) {
                Part hand = side < 0 ? b.rHand : b.lHand;
                for (int i = -1; i <= 1; i++) hand.spike(i * 0.55f, 2, -0.3f, 0.4f, 1.6f, 170, 0, i * 6, bone());
            }
            rankGear(b, 6.2f, 6.6f, 6.2f, 7, 6.4f, 3.8f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            float move = Math.min(1, c.la * 1.3f);
            p.idle(c.age, 1.2f);
            p.walk(c.ls, c.la, 1.5f, 0, 1.8f);
            float ph = c.ls * 0.6662f;
            // sprinting hunched, arms swept back and flailing
            p.spine(18 + 22 * move, 0, Mth.sin(ph) * 6 * move);
            p.head.xRot -= 0.5f * move;
            p.armR(-30 + 45 * move + Mth.cos(ph) * 40 * move, 0, 18, 30);
            p.armL(-30 + 45 * move - Mth.cos(ph) * 40 * move, 0, -18, 30);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            m.p("jaw").xRot += 0.4f + Mth.sin(c.age * 0.4f) * 0.15f;
            p.head.zRot += Mth.sin(c.age * 0.9f + c.variant) * 0.06f;
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float k = Anim.pulse(c.t, 0, 16);
                p.armR(-150 * k, 0, 10 * k, -20 * k);
                p.armL(-150 * k, 0, -10 * k, -20 * k);
                p.legR(-40 * k, 0, 20 * k);
                p.legL(40 * k, 0, 60 * k);
                p.spine(-20 * k, 0, 0);
            }
            ZGear.claw(p, c, true);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Spitter

    /** Hunched, with a bulging, glowing bile sac in its throat, acid-burned skin and a distended jaw. */
    public static final class Spitter implements ZombieModels.Def {
        @Override
        public void build(ModelSpec s) {
            int sk = 0x7A8A5A;
            Paint skin = skin(sk).decal(Face.FRONT, DecalType.BLOOD, 0x6B9930, false, 0.5f, 0.3f, 1f);
            Paint head = face(sk, 0x9BFF50);
            Body b = Body.build(s.root, standard(skin, cloth(0x5E6B3A), cloth(0x4A3B2A), head));
            Paint sac = p(Mat.FLESH, 0x9BD940).accent(0x5A8A20).pattern(Pattern.VEINS, 0xD8FF70, 0.8f).accentGlow();
            Part sacP = b.neck.child("sac", 0, 0.4f, -2.2f);
            sacP.sphere(0, 0, -0.6f, 2.8f, sac);
            sacP.box(-1, -1.6f, -3.6f, 2, 2, 1, Paint.glowing(0xC8FF60));
            Part jaw = b.head.child("jaw", 0, -1.2f, -0.6f);
            jaw.box(-2.8f, 0, -3.4f, 5.6f, 2, 3.6f, p(Mat.FLESH, 0x6A7A4A).decal(Face.FRONT, DecalType.MOUTH, 0x9BFF50, true, 0.5f, 0.4f, 1f));
            b.chest.box(-1.5f, -6.4f, -2.25f, 3, 6.6f, 0.3f, p(Mat.FLESH, 0x9BD940).pattern(Pattern.STRIPES, 0x6B9930, 1f).glow());
            for (int i = 0; i < 3; i++) b.chest.sphere(-2.4f + i * 2.4f, -5 + (i % 2) * 2, 2.1f, 0.9f, Paint.glowing(0xC8FF60));
            rankGear(b, HW, HH, HD, 8, 6.6f, 4.2f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            ZGear.shamble(p, c, 30, false);
            p.spine(14, 0, 0);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            ModelPart sac = m.p("sac"), jaw = m.p("jaw");
            float pump = 1f + Mth.sin(c.age * 0.15f) * 0.06f;
            jaw.xRot += 0.15f;
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float charge = Anim.hold(c.t, 0, 8, 2) * (c.t < 9 ? 1 : 0);
                float spit = Anim.pulse(c.t, 8, 8);
                pump += 0.4f * charge - 0.25f * spit;
                p.spine(-22 * charge + 25 * spit, 0, 0);
                p.head.xRot += -0.4f * charge + 0.5f * spit;
                jaw.xRot += 1.0f * spit;
                p.armR(30 * charge, 0, 20 * charge, 0);
                p.armL(30 * charge, 0, -20 * charge, 0);
            }
            sac.xScale = sac.yScale = sac.zScale = pump;
            ZGear.claw(p, c, false);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Leaper

    /** Lanky, frog-crouched mutant on long digitigrade legs, with slit eyes and hooked claws. */
    public static final class Leaper implements ZombieModels.Def {
        @Override
        public void build(ModelSpec s) {
            int sk = 0x8A9A72;
            Paint skin = skin(sk).pattern(Pattern.MUSCLE, 0x6A7A52, 1.2f);
            Paint head = skin(sk).decal(Face.FRONT, DecalType.SLIT_EYES, 0xFFB020, true, 0.5f, 0.45f, 1.1f)
                    .decal(Face.FRONT, DecalType.GRIN, 0x2A0A0A, false, 0.5f, 0.8f, 1f);
            Body.Spec sp = standard(skin, skin, cloth(0x3A3A3A), head);
            sp.legs(6.4f, 7.2f, 3.2f);
            sp.torso(7, 6.2f, 3.8f, 5.6f, 4.6f, 3.2f);
            sp.head(6.4f, 6, 6.8f);
            sp.arms(6.6f, 6.6f, 2.8f);
            sp.paints(skin, cloth(0x3A3A3A), skin, skin, skin, cloth(0x3A3A3A), skin, skin);
            Body b = Body.build(s.root, sp);
            for (int side = -1; side <= 1; side += 2) {
                Part hand = side < 0 ? b.rHand : b.lHand;
                hand.box(-1.6f, 1.6f, -1.6f, 3.2f, 2.6f, 0.6f, bone());
                hand.spike(0, 4, -1.2f, 0.7f, 2, 160, 0, 0, bone());
                Part foot = side < 0 ? b.rFoot : b.lFoot;
                foot.spike(0, 0.6f, -2.4f, 0.6f, 1.6f, 100, 0, 0, bone());
            }
            b.chest.spike(0, -6, 1.9f, 0.8f, 2, -60, 0, 0, bone());
            b.chest.spike(0, -3.5f, 1.9f, 0.7f, 1.6f, -60, 0, 0, bone());
            rankGear(b, 6.4f, 6, 6.8f, 7, 6.2f, 3.8f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            float move = Math.min(1, c.la);
            p.idle(c.age, 1f);
            // permanent frog crouch on digitigrade legs
            p.crouch(0.75f, 5);
            p.legR(-10, 8, 0);
            p.legL(-10, -8, 0);
            p.spine(28, 0, 0);
            p.head.xRot -= 0.45f;
            float hop = Math.abs(Mth.sin(c.ls * 0.5f)) * move;
            p.lift(-hop * 2.5f);
            p.walk(c.ls, c.la, 0.8f, 0, 0.5f);
            p.armR(-40 + Mth.cos(c.ls * 0.6662f) * 25 * move, 0, 14, 40);
            p.armL(-40 - Mth.cos(c.ls * 0.6662f) * 25 * move, 0, -14, 40);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float coil = Anim.pulse(c.t, 0, 8);
                float fly = Anim.hold(c.t, 6, 18, 3);
                p.crouch(0.3f * coil - 0.7f * fly, 4);
                p.armR(-140 * fly, 0, 10, -30 * fly);
                p.armL(-140 * fly, 0, -10, -30 * fly);
                p.legR(40 * fly, 0, 20 * fly);
                p.legL(30 * fly, 0, 30 * fly);
                p.spine(-20 * fly, 0, 0);
            }
            ZGear.claw(p, c, true);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Shrieker

    /** Emaciated banshee: lank black hair, a split jaw that unhinges to her chest, tattered dress. */
    public static final class Shrieker implements ZombieModels.Def {
        @Override
        public void build(ModelSpec s) {
            int sk = 0xB8C0A8;
            Paint pale = p(Mat.ROT, sk).accent(0x7A8270).pattern(Pattern.VEINS, 0x7A5A7A, 0.9f).grime(0.2f);
            Paint head = p(Mat.ROT, sk).accent(0x7A8270).pattern(Pattern.VEINS, 0x7A5A7A, 0.9f)
                    .decal(Face.FRONT, DecalType.HOLLOW_EYES, 0xF0F8FF, true, 0.5f, 0.42f, 1.1f);
            Paint dress = cloth(0x3A3040);
            Body.Spec sp = standard(pale, dress, dress, head);
            sp.legs(6.2f, 6.2f, 2.4f);
            sp.torso(6.4f, 6.6f, 3.2f, 5.4f, 4.8f, 3);
            sp.head(6, 6.4f, 6);
            sp.arms(6.4f, 6.4f, 2.1f);
            sp.hand = 2.8f;
            sp.paints(dress, dress, pale, pale, pale, pale, pale, pale);
            Body b = Body.build(s.root, sp);
            Part skirt = b.pelvis.child("skirt", 0, 0, 0);
            skirt.taper(0, 9, 0, 9, 6, 6, 4, 9, 3, dress);
            Part jaw = b.head.child("jaw", 0, -1.4f, -0.4f);
            jaw.box(-2.6f, 0, -3.1f, 5.2f, 5, 4.4f, p(Mat.FLESH, 0x9AA090).accent(0x5A5A50).decal(Face.FRONT, DecalType.FANGS, 0xE8E0D0, false, 0.5f, 0.08f, 1f));
            jaw.box(-1.6f, 0.5f, -2.6f, 3.2f, 4, 0.4f, p(Mat.FLESH, 0x1A0A0A));
            jaw.box(-0.8f, 2, -2.9f, 1.6f, 3.6f, 1.2f, p(Mat.FLESH, BLOOD));
            Part hair = b.head.child("hair", 0, -6.6f, 0);
            hair.box(-3.4f, 0, -3.4f, 6.8f, 1, 6.8f, p(Mat.HAIR, 0x14141A));
            hair.box(-3.5f, 0.6f, 2.4f, 7, 11, 1.4f, p(Mat.HAIR, 0x14141A).pattern(Pattern.FUR, 0x2A2A30, 1.5f));
            hair.box(-3.6f, 0.6f, -3.4f, 1.4f, 9, 5.8f, p(Mat.HAIR, 0x14141A));
            hair.box(2.2f, 0.6f, -3.4f, 1.4f, 9, 5.8f, p(Mat.HAIR, 0x14141A));
            ribs(b.chest, 0, -6, -1.7f, 4, 3.4f);
            rankGear(b, 6, 6.4f, 6, 6.4f, 6.6f, 3.2f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            ModelPart jaw = m.p("jaw"), hair = m.p("hair");
            p.idle(c.age, 1f);
            p.walk(c.ls, c.la, 0.7f, 0.1f, 0.8f);
            // twitchy, jerking head and limp arms
            float twitch = Mth.sin(c.age * 2.3f + c.variant) * Mth.sin(c.age * 0.31f) > 0.85f ? 0.3f : 0;
            p.head.zRot += Mth.sin(c.age * 0.07f) * 0.15f + twitch;
            p.head.xRot += 0.25f;
            p.spine(12, 0, 0);
            p.armR(-10, 0, 6, 10);
            p.armL(-10, 0, -6, 10);
            p.look(c.headYaw, c.headPitch);
            p.flinch(c.hurt);
            jaw.xRot += 0.2f + Mth.sin(c.age * 0.2f) * 0.05f;
            hair.xRot += 0.15f + Mth.sin(c.age * 0.1f) * 0.05f;
            if (c.id == SiegeZombie.ANIM_SPECIAL) {
                float k = Anim.pulse(c.t, 0, 30);
                float shake = Mth.sin(c.age * 3) * 0.08f * k;
                jaw.xRot += 1.2f * k;
                p.head.xRot -= 0.7f * k;
                p.head.yRot += shake;
                p.armR(-40 * k, 0, 80 * k, 0);
                p.armL(-40 * k, 0, -80 * k, 0);
                p.spine(-20 * k, 0, 0);
                hair.xRot += 0.6f * k;
            }
            ZGear.claw(p, c, false);
            ZGear.common(m, p, z, c);
        }
    }

    // ================================================================== Crawler

    /** A torso with stumps for legs, dragging itself on its elbows, spine and gore trailing. */
    public static final class Crawler implements ZombieModels.Def {
        @Override
        public float shadow() {
            return 0.45f;
        }

        @Override
        public void build(ModelSpec s) {
            int sk = 0x6A7A58;
            Paint skin = wounded(sk);
            Paint shirt = cloth(0x5A4A3A);
            Body.Spec sp = standard(skin, shirt, cloth(0x3A3028), face(sk, 0xFFD040));
            sp.legs(2.4f, 0.6f, 3.4f);
            sp.foot = 0.4f;
            Body b = Body.build(s.root, sp);
            tatters(b.chest, 8, 4.2f, -0.2f, 6, 1.8f, shirt);
            Paint gore = p(Mat.FLESH, 0x7A1A1A).accent(0x3A0606).pattern(Pattern.VEINS, 0xA02020, 1f);
            for (int side = -1; side <= 1; side += 2) {
                Part thigh = side < 0 ? b.rThigh : b.lThigh;
                thigh.box(-1.9f, 2, -1.9f, 3.8f, 1, 3.8f, gore);
                thigh.spike(0, 2.6f, 0, 0.9f, 1.4f, 180, 0, 0, bone());
            }
            Part spine = b.pelvis.child("trail", 0, 0.6f, 0);
            for (int i = 0; i < 3; i++) spine.box(-0.6f, i * 1.3f, -0.6f, 1.2f, 1, 1.2f, bone());
            spine.box(-1.4f, 0.2f, -1.4f, 2.8f, 2.6f, 2.8f, gore);
            Part jaw = b.head.child("jaw", 0, -1.4f, -0.6f);
            jaw.box(-2.6f, 0, -3.2f, 5.2f, 1.6f, 3.4f, skin.copy().decal(Face.FRONT, DecalType.FANGS, 0xC8C0A0, false, 0.5f, 0.2f, 1f));
            rankGear(b, HW, HH, HD, 8, 6.6f, 4.2f);
        }

        @Override
        public void animate(ProcModel<SiegeZombie> m, SiegeZombie z, ZombieModels.Ctx c) {
            Poser p = Poser.of(m);
            float move = Math.min(1, c.la * 1.5f);
            float ph = c.ls * 0.9f;
            // prone: torso laid forward along the ground, head craned up
            p.pelvis.xRot += 1.42f;
            p.pelvis.y += 1.6f;
            p.neck.xRot -= 0.9f;
            p.head.xRot -= 0.35f;
            p.look(c.headYaw, c.headPitch * 0.5f);
            // elbow-dragging crawl
            float pullR = Mth.sin(ph), pullL = Mth.sin(ph + Mth.PI);
            p.armR(-150 + 30 * pullR * move, 10, 12, 50 + 30 * Math.max(0, pullR) * move);
            p.armL(-150 + 30 * pullL * move, -10, -12, 50 + 30 * Math.max(0, pullL) * move);
            p.chest.zRot += Mth.sin(ph) * 0.1f * move;
            p.chest.yRot += Mth.sin(ph) * 0.12f * move;
            p.rThigh.xRot += 0.3f + Mth.sin(ph) * 0.2f * move;
            p.lThigh.xRot += 0.3f - Mth.sin(ph) * 0.2f * move;
            m.p("jaw").xRot += 0.3f + Mth.sin(c.age * 0.25f) * 0.15f;
            p.flinch(c.hurt * 0.5f);
            if (c.id == SiegeZombie.ANIM_SPECIAL || c.id == SiegeZombie.ANIM_ATTACK || c.id == SiegeZombie.ANIM_CORE) {
                float k = Anim.pulse(c.t, 0, 10);
                p.armR(-30 * k, 0, 0, -40 * k);
                p.armL(-30 * k, 0, 0, -40 * k);
                p.neck.xRot -= 0.3f * k;
                m.p("jaw").xRot += 0.6f * k;
            }
            if (c.rise < 1) p.armR(-60 * (1 - c.rise), 0, 0, 0);
        }
    }
}
