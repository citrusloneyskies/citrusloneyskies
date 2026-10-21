package com.tokyoghoul.client.model;

import com.tokyoghoul.client.ClientState;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.move.Anims;
import com.tokyoghoul.network.Sync;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.tokyoghoul.client.model.Anim.easeOut;
import static com.tokyoghoul.client.model.Anim.hold;
import static com.tokyoghoul.client.model.Anim.pulse;
import static com.tokyoghoul.client.model.Anim.swing;
import static com.tokyoghoul.client.model.Anim.win;

/**
 * Poses a character body every frame: idle breathing, gait, fighting stance, and a keyframed pose for
 * every move animation (jabs, barrages, kicks, slashes, kagune thrusts, guards, roars...). Also shows
 * the mask, kakugan and transformation armour from the synced visual flags.
 */
public final class CharacterAnimator {
    private static final Map<ModelSpec.Baked, Groups> GROUPS = new IdentityHashMap<>();

    private CharacterAnimator() {
    }

    /** Toggleable part groups of one baked model. */
    static final class Groups {
        final List<ModelPart> mask = new ArrayList<>(), kak = new ArrayList<>(), eye = new ArrayList<>(), form = new ArrayList<>(), face = new ArrayList<>();
        ModelPart cape;
    }

    static Groups groups(ModelSpec.Baked b) {
        return GROUPS.computeIfAbsent(b, k -> {
            Groups g = new Groups();
            for (Map.Entry<String, ModelPart> e : k.parts.entrySet()) {
                String n = e.getKey();
                if (n.equals("mask")) g.mask.add(e.getValue());
                else if (n.startsWith("kak_")) g.kak.add(e.getValue());
                else if (n.startsWith("eye_")) g.eye.add(e.getValue());
                else if (n.startsWith("form_")) g.form.add(e.getValue());
                else if (n.startsWith("face_")) g.face.add(e.getValue());
                else if (n.equals("cape")) g.cape = e.getValue();
            }
            return g;
        });
    }

    public static <T extends LivingEntity> void animate(ProcModel<T> m, T e, float ls, float la, float age, float headYaw, float headPitch, float pt) {
        Poser p = Poser.of(m);
        CharacterDef def = ClientState.character(e);
        int flags = ClientState.flags(e);
        int id = ClientState.anim(e);
        float t = ClientState.animTime(e, pt);
        if (id != Anims.NONE && !Anims.held(id) && t > Anims.length(id)) id = Anims.NONE;
        boolean kagune = (flags & Sync.FLAG_KAGUNE) != 0;
        boolean form = (flags & Sync.FLAG_FORM) != 0;
        boolean masked = (flags & Sync.FLAG_MASK) != 0 || (def != null && def.look.alwaysMasked);

        Groups g = groups(m.baked);
        for (ModelPart part : g.mask) part.visible = masked && !form;
        for (ModelPart part : g.face) part.visible = !masked && !form;
        boolean kakOn = (kagune || form) && def != null && def.race.hasKagune;
        for (ModelPart part : g.kak) {
            part.visible = kakOn;
            part.z = masked || form ? -0.75f : 0f;
        }
        for (ModelPart part : g.eye) part.visible = !kakOn;
        for (ModelPart part : g.form) part.visible = form;

        // shared motion
        boolean combat = masked || kagune || form;
        float move = Math.min(1, la);
        boolean run = e.isSprinting() || e.getDeltaMovement().horizontalDistanceSqr() > 0.04;
        p.idle(age, combat ? 0.6f : 1f);
        p.walk(ls, la, run ? 1.25f : 1f, id == Anims.NONE ? 1f : 0.2f, run ? 1.3f : 1f);
        if (run && id == Anims.NONE) {
            p.spine(18 * move, 0, 0);
            if (combat) {
                // anime run: arms trailing behind
                p.armR(55 * move, 0, 10 * move, 20 * move);
                p.armL(55 * move, 0, -10 * move, 20 * move);
            }
        }
        p.look(headYaw, headPitch);
        float hurt = e.hurtTime > 0 ? (e.hurtTime - pt) / 10f : 0;
        p.flinch(hurt);
        if (combat && move < 0.3f && id == Anims.NONE) {
            p.stance(0.7f * (1 - move));
            p.armR(-35, 0, 10, 70);
            p.armL(-45, 0, -10, 80);
        }
        if (!e.getMainHandItem().isEmpty() && id == Anims.NONE) p.armR(-20, 0, 0, 15);
        if (g.cape != null) g.cape.xRot = 0.08f + move * 0.5f + Mth.sin(age * 0.15f) * 0.04f;
        if (e.isShiftKeyDown() && id == Anims.NONE) p.crouch(0.5f, 3);

        switch (id) {
            case Anims.JAB -> {
                float k = pulse(t, 0, 7);
                p.armR(-85 * k, 10 * k, 0, 40 * (1 - k));
                p.spine(5 * k, -25 * k, 0);
                p.armL(-40, 0, -10, 80);
            }
            case Anims.HEAVY -> {
                float s = swing(t, 6, 3, 6);
                p.spine(-8 * s, 35 * s, 0);
                p.armR(s > 0 ? 30 * s : -95 * -s, s > 0 ? -20 * s : 0, 0, s > 0 ? 90 * s : 0);
                p.legR(s < 0 ? -25 * -s : 0, 0, 0);
                p.armL(-50, 0, -10, 80);
            }
            case Anims.BARRAGE -> {
                float a = Mth.sin(t * 2.4f), b = Mth.sin(t * 2.4f + Mth.PI);
                p.stance(1);
                p.armR(-90 + a * 12, 10, 0, 30 + Math.max(0, a) * 50);
                p.armL(-90 + b * 12, -10, 0, 30 + Math.max(0, b) * 50);
                p.spine(6, a * 12, 0);
            }
            case Anims.KICK -> {
                float k = pulse(t, 0, 13);
                p.pelvis.yRot += Anim.easeOut(win(t, 0, 10)) * Mth.TWO_PI * 0.75f;
                p.legR(-110 * k, -20 * k, 10 * k);
                p.spine(-20 * k, 0, 15 * k);
                p.armR(30 * k, 0, 50 * k, 20);
                p.armL(30 * k, 0, -50 * k, 20);
            }
            case Anims.UPPERCUT -> {
                float s = swing(t, 5, 3, 6);
                p.crouch(Math.max(0, s), 3);
                p.armR(s > 0 ? 20 : -160 * -s, 0, 0, s > 0 ? 100 : 20);
            }
            case Anims.SLASH -> {
                float s = swing(t, 5, 3, 6);
                p.armR(-80, s > 0 ? 70 * s : -80 * -s, 0, 10);
                p.spine(0, s > 0 ? 30 * s : -40 * -s, 0);
                p.armL(-30, 0, -20, 40);
            }
            case Anims.OVERHEAD -> {
                float s = swing(t, 6, 3, 6);
                p.armR(s > 0 ? -170 * s : -40 * -s - 30, 0, 0, 10);
                p.armL(s > 0 ? -170 * s : -40 * -s - 30, 0, 0, 10);
                p.spine(s > 0 ? -15 * s : 25 * -s, 0, 0);
                p.crouch(s < 0 ? -s * 0.5f : 0, 3);
            }
            case Anims.THRUST -> {
                float s = swing(t, 5, 3, 6);
                p.armR(-90, 0, 0, s > 0 ? 100 * s : 0);
                p.armL(-80, 20, 0, s > 0 ? 80 * s : 20);
                p.spine(s < 0 ? 20 * -s : -5 * s, s > 0 ? 20 * s : -10 * -s, 0);
                p.legR(s < 0 ? -30 * -s : 0, 0, 0);
            }
            case Anims.CAST, Anims.THROW -> {
                float s = swing(t, 5, 3, 6);
                boolean over = id == Anims.THROW;
                p.armR(s > 0 ? (over ? -160 : -40) * s : -90 * -s, 0, 0, s > 0 ? 40 * s : 0);
                p.armL(-40, 0, -20, 30);
                p.spine(s < 0 ? 12 * -s : -10 * s, s > 0 ? 25 * s : -15 * -s, 0);
            }
            case Anims.K_PIERCE, Anims.K_SPIKES -> {
                float k = hold(t, 2, 16, 3);
                p.spine(15 * k, 0, 0);
                p.armR(30 * k, 0, 25 * k, 20);
                p.armL(30 * k, 0, -25 * k, 20);
                p.crouch(0.4f * k, 2);
                if (id == Anims.K_SPIKES) p.armR(-60 * k, 0, 0, 0);
            }
            case Anims.K_SWEEP, Anims.SPIN -> {
                float k = win(t, 3, 14);
                p.pelvis.yRot += easeOut(k) * Mth.TWO_PI;
                p.armR(-80 * pulse(t, 0, 20), 0, 60, 0);
                p.armL(-30, 0, -60, 0);
                p.crouch(0.4f, 2);
            }
            case Anims.K_BARRAGE -> {
                p.stance(1);
                p.spine(10, Mth.sin(t * 1.3f) * 8, 0);
                p.armR(20, 0, 30, 40);
                p.armL(20, 0, -30, 40);
            }
            case Anims.K_GRAB -> {
                float reach = hold(t, 4, 30, 4);
                p.armL(-95 * reach, 0, 0, 0);
                p.armR(-95 * hold(t, 26, 36, 3), 0, 0, 0);
                p.spine(-10 * win(t, 10, 20), 0, 0);
            }
            case Anims.K_VOLLEY -> {
                float k = hold(t, 2, 20, 3);
                p.spine(-10 * k, 0, 0);
                p.armR(-90 * k, 0, 0, 0);
                p.armL(-20 * k, 0, -40 * k, 30);
            }
            case Anims.DASH -> {
                p.spine(35, 0, 0);
                p.armR(70, 0, 15, 10);
                p.armL(70, 0, -15, 10);
                p.legR(-40, 0, 40);
                p.legL(30, 0, 50);
            }
            case Anims.LEAP -> {
                float crouch = 1 - win(t, 0, 5);
                p.crouch(crouch, 5);
                float tuck = hold(t, 5, 26, 3);
                p.legR(-60 * tuck, 0, 90 * tuck);
                p.legL(-40 * tuck, 0, 80 * tuck);
                p.armR(-150 * tuck, 0, 20, 30);
                p.armL(-150 * tuck, 0, -20, 30);
            }
            case Anims.SLAM -> {
                float k = 1 - easeOut(win(t, 0, 16));
                p.crouch(k, 6);
                p.armR(-60 * k, 0, 0, 0);
                p.armL(-60 * k, 0, 0, 0);
                p.spine(30 * k, 0, 0);
            }
            case Anims.GUARD -> {
                p.stance(1);
                p.crouch(0.3f, 2);
                p.armR(-80, 35, 0, 100);
                p.armL(-80, -35, 0, 100);
            }
            case Anims.K_SHIELD -> {
                p.stance(1);
                p.crouch(0.35f, 2);
                p.armR(-60, 20, 0, 60);
                p.armL(-40, -20, 0, 60);
            }
            case Anims.TRANSFORM -> {
                float k = hold(t, 2, 30, 6);
                p.spine(-25 * k, 0, 0);
                p.head.xRot -= 0.6f * k;
                p.armR(-30 * k, 0, 70 * k, 30);
                p.armL(-30 * k, 0, -70 * k, 30);
                p.crouch(0.3f * k, 2);
            }
            case Anims.BITE -> {
                float k = pulse(t, 0, 18);
                p.spine(30 * k, 0, 0);
                p.head.xRot += 0.4f * k;
                p.armR(-80 * k, 0, 0, 40);
                p.armL(-80 * k, 0, 0, 40);
            }
            case Anims.BREATH -> {
                p.spine(-8, 0, 0);
                p.head.xRot -= 0.15f;
                p.armR(20, 0, 30, 20);
                p.armL(20, 0, -30, 20);
                p.stance(1);
            }
            case Anims.DODGE -> {
                float k = pulse(t, 0, 12);
                p.crouch(k, 4);
                p.spine(-15 * k, 0, 20 * k);
                p.armR(-30 * k, 0, 40 * k, 30);
                p.armL(-30 * k, 0, -40 * k, 30);
            }
            case Anims.STAGGER, Anims.HURT -> {
                float k = pulse(t, 0, id == Anims.HURT ? 8 : 10);
                p.spine(-20 * k, 0, 0);
                p.head.xRot -= 0.3f * k;
                p.armR(-20 * k, 0, 30 * k, 20);
                p.armL(-20 * k, 0, -30 * k, 20);
            }
            case Anims.REGEN -> {
                p.crouch(0.4f, 2);
                p.armR(-50, -30, 0, 90);
                p.armL(-50, 30, 0, 90);
                p.head.xRot += 0.3f;
            }
            case Anims.CHARGE -> {
                float k = hold(t, 0, 18, 4);
                p.crouch(0.5f * k, 3);
                p.armR(10 * k, 0, 20 * k, 60 * k);
                p.armL(10 * k, 0, -20 * k, 60 * k);
                p.head.xRot -= 0.25f * k;
            }
            default -> {
            }
        }
    }
}
