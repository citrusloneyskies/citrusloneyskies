package com.lastbastion.client.render.weapon;

import com.lastbastion.LastBastion;
import com.lastbastion.entity.misc.TitanExoEntity;
import com.lastbastion.item.weapon.MeleeWeaponItem;
import com.lastbastion.item.weapon.SwingStyle;
import com.lastbastion.item.weapon.MeleeCombat;
import com.lastbastion.item.weapon.WeaponKind;
import com.lastbastion.network.Net;
import com.lastbastion.network.WeaponAnimPacket;
import com.lastbastion.network.WeaponSwingPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Client side of melee combat: turns left clicks (and a held attack button) into swing packets,
 * predicts the local combo so the animation starts instantly, keeps every entity's current swing, and
 * poses the arms (third person) and the view model (first person) through each {@link SwingStyle}.
 */
@Mod.EventBusSubscriber(modid = LastBastion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponAnimations {
    public record Anim(WeaponKind kind, SwingStyle style, int step, float start, float duration) {
        public float progress(float now) {
            return (now - start) / duration;
        }
    }

    private static final Map<Integer, Anim> ANIMS = new HashMap<>();
    private static int ticks;
    private static int lastSent = -100;
    private static int localStep = -1;
    @Nullable
    private static WeaponKind localKind;
    private static long localLast = -1000;
    private static net.minecraft.world.item.Item lastItem;
    private static int lastChange = -100;

    private WeaponAnimations() {
    }

    public static float now() {
        return ticks + Minecraft.getInstance().getFrameTime();
    }

    @Nullable
    public static Anim get(LivingEntity e) {
        Anim a = ANIMS.get(e.getId());
        if (a == null) return null;
        float t = a.progress(now());
        return t >= 0 && t <= 1 ? a : null;
    }

    static void play(int id, WeaponKind k, SwingStyle s, int step, int dur) {
        ANIMS.put(id, new Anim(k, s, step, ticks, Math.max(3, dur)));
    }

    public static void onPacket(WeaponAnimPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && p.entity() == mc.player.getId()) {
            // follow the server's combo, but only from real combo strokes (weak swings don't advance it)
            WeaponKind wk = WeaponKind.byId(p.weapon());
            if (wk == localKind && p.step() >= 0 && p.step() < wk.combo.length && wk.combo[p.step()].ordinal() == p.style()) localStep = p.step();
            Anim cur = ANIMS.get(p.entity());
            if (cur != null && ticks - cur.start() < 8 && cur.style().ordinal() == p.style()) return;
        }
        play(p.entity(), WeaponKind.byId(p.weapon()), SwingStyle.byId(p.style()), p.step(), p.duration());
    }

    // ------------------------------------------------------------------ input

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.getVehicle() instanceof TitanExoEntity) return;
        if (!(p.getMainHandItem().getItem() instanceof MeleeWeaponItem w)) return;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return;
        e.setCanceled(true);
        e.setSwingHand(false);
        trySwing(p, w.kind);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        ticks++;
        if (ticks % 40 == 0) ANIMS.values().removeIf(a -> ticks > a.start() + a.duration() + 20);
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || !mc.options.keyAttack.isDown() || p.isUsingItem()) return;
        if (!(p.getMainHandItem().getItem() instanceof MeleeWeaponItem w) || p.getVehicle() instanceof TitanExoEntity) return;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) return;
        if (p.getAttackStrengthScale(0f) >= 1f) trySwing(p, w.kind);
    }

    static void trySwing(LocalPlayer p, WeaponKind k) {
        if (p.isUsingItem() || com.lastbastion.client.ClientDowned.isDowned(p)) return;
        if (localKind != k) {
            // a different weapon starts its own combo (mirrors MeleeCombat.swing)
            localKind = k;
            localStep = -1;
        }
        // the server allows at most two weak swings per full swing; don't predict swings it would refuse
        if (ticks - lastSent < Math.max(3, Math.round(20f / k.attacksPerSecond) / 2)) return;
        lastSent = ticks;
        float strength = p.getAttackStrengthScale(0.5f);
        boolean strong = strength > 0.7f;
        long now = p.level().getGameTime();
        int window = Math.max(30, k.swingTicks() * 3);
        int step = MeleeCombat.comboStep(k, strong, localStep, now - localLast <= window);
        if (strong) localStep = step;
        localLast = now;
        play(p.getId(), k, strong ? k.combo[step] : MeleeCombat.weakStyle(k), step, Mth.clamp(Math.round(k.swingTicks() * 0.85f), 6, 16));
        p.resetAttackStrengthTicker();
        Net.toServer(new WeaponSwingPacket(0));
    }

    // ------------------------------------------------------------------ first person

    /**
     * Vanilla drops the held item after every right-click use (the re-equip dip); arsenal weapons only
     * play the equip animation after an actual switch of the main-hand item.
     */
    public static float equip(LocalPlayer player, ItemStack stack, float equip) {
        if (player.getMainHandItem() != stack) return equip;
        if (stack.getItem() != lastItem) {
            lastItem = stack.getItem();
            lastChange = ticks;
        }
        return ticks - lastChange < 12 ? equip : 0f;
    }

    public static void firstPerson(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, WeaponKind k, float equip) {
        boolean left = arm == HumanoidArm.LEFT;
        pose.translate((left ? -1 : 1) * 0.56F, -0.52F + equip(player, stack, equip) * -0.6F, -0.72F);
        PoseXf xf = new PoseXf(pose);
        if (player.getMainHandItem() == stack) {
            if (player.isUsingItem() && player.getUseItem() == stack) {
                WeaponPoses.firstPersonCharge(xf, Math.min(1f, (player.getTicksUsingItem() + Minecraft.getInstance().getFrameTime()) / 20f), left);
            } else {
                Anim a = get(player);
                if (a != null) WeaponPoses.firstPersonSwing(xf, a.style(), a.progress(now()), k, left);
            }
        }
        WeaponPoses.firstPersonRest(xf, k, left);
    }

    // ------------------------------------------------------------------ third person

    /** Arm pose for players holding a melee weapon; also sets the body twist during big swings. */
    public static void armPose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm, boolean twoHanded) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart main = right ? model.rightArm : model.leftArm;
        ModelPart off = right ? model.leftArm : model.rightArm;
        float side = right ? 1 : -1;
        if (twoHanded) {
            main.xRot = -0.75f;
            main.yRot = -0.25f * side;
            main.zRot = 0;
            off.xRot = -0.9f;
            off.yRot = 0.6f * side;
            off.zRot = 0;
        } else {
            main.xRot = main.xRot * 0.5f - (float) Math.PI / 10f;
            main.yRot = 0;
        }
        if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof MeleeWeaponItem) {
            float t = Math.min(1f, (entity.getTicksUsingItem() + Minecraft.getInstance().getFrameTime()) / 20f);
            main.xRot = Mth.lerp(t, main.xRot, -2.95f);
            main.yRot = Mth.lerp(t, main.yRot, -0.15f * side);
            off.xRot = Mth.lerp(t, off.xRot, -2.8f);
            off.yRot = Mth.lerp(t, off.yRot, 0.35f * side);
            return;
        }
        Anim a = get(entity);
        if (a == null) return;
        float t = a.progress(now());
        float heft = a.kind().weaponClass.heft;
        float wa = 0.22f * heft, wb = wa + 0.28f;
        float wind = t < wa ? smooth(t / wa) : t < wb ? 1f - easeOut((t - wa) / (wb - wa)) : 0f;
        float strike = t < wa ? 0f : t < wb ? easeOut((t - wa) / (wb - wa)) : 1f - smooth((t - wb) / (1f - wb));
        float xr, yr, zr = 0, twist = 0;
        switch (a.style()) {
            case SLASH_R -> {
                xr = -1.5f - 0.2f * wind;
                yr = 0.9f * wind - 1.0f * strike;
                twist = 0.35f * wind - 0.55f * strike;
            }
            case SLASH_L -> {
                xr = -1.45f;
                yr = -0.8f * wind + 0.9f * strike;
                twist = -0.4f * wind + 0.5f * strike;
            }
            case DIAG_DOWN -> {
                xr = -2.6f * wind - 0.5f * strike + (1 - wind) * -0.6f * (1 - strike);
                yr = 0.5f * wind - 0.6f * strike;
                zr = -0.3f * strike;
                twist = 0.25f * wind - 0.45f * strike;
            }
            case DIAG_UP -> {
                xr = -0.3f * wind - 2.3f * strike;
                yr = -0.6f * wind + 0.5f * strike;
                zr = 0.25f * strike;
                twist = -0.3f * wind + 0.35f * strike;
            }
            case OVERHEAD -> {
                xr = -3.0f * wind - 0.55f * strike + (1 - wind - strike) * -0.6f;
                yr = -0.1f;
                twist = -0.1f * strike;
            }
            case THRUST -> {
                xr = -1.2f * wind - 1.55f * strike;
                yr = 0.2f * wind;
                main.z -= 3f * strike;
                twist = 0.2f * wind - 0.25f * strike;
            }
            case SPIN -> {
                xr = -1.55f;
                yr = 0.4f;
                twist = -Mth.TWO_PI * Math.min(1f, t * 1.4f);
            }
            case UPPERCUT -> {
                xr = -0.2f * wind - 2.7f * strike;
                yr = 0.1f;
                twist = 0.15f * wind - 0.2f * strike;
            }
            default -> {
                xr = main.xRot;
                yr = main.yRot;
            }
        }
        main.xRot = xr;
        main.yRot = yr * side;
        main.zRot = zr * side;
        if (twoHanded) {
            off.xRot = xr - 0.1f;
            off.yRot = (yr + 0.55f) * side;
            off.zRot = zr * side;
        }
        if (twist != 0) {
            model.body.yRot = twist * side;
            model.rightArm.z = Mth.sin(model.body.yRot) * 5.0F;
            model.rightArm.x = -Mth.cos(model.body.yRot) * 5.0F;
            model.leftArm.z = -Mth.sin(model.body.yRot) * 5.0F;
            model.leftArm.x = Mth.cos(model.body.yRot) * 5.0F;
            model.rightArm.yRot += model.body.yRot;
            model.leftArm.yRot += model.body.yRot;
        }
    }

    static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    static float easeOut(float t) {
        t = Mth.clamp(t, 0, 1);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}
