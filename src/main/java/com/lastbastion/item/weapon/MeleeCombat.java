package com.lastbastion.item.weapon;

import com.lastbastion.LastBastion;
import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.network.Net;
import com.lastbastion.network.WeaponAnimPacket;
import com.lastbastion.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Server-side melee combat for the arsenal: combo tracking, cone/line hit detection with line of sight,
 * crits and finishers, every weapon's passive, and all eight specials (with their lingering effects:
 * whirlwinds, leaps, void rifts, harvest vortices).
 */
@Mod.EventBusSubscriber(modid = LastBastion.MODID)
public final class MeleeCombat {
    private MeleeCombat() {
    }

    /** Transient per-player combat state (not saved; combos and specials are short-lived). */
    static final class State {
        long lastSwing = -1000;
        int step = -1;
        long nextSwing;
        long parryUntil;
        long edgeUntil;
        long whirlUntil;
        boolean whirlAscended;
        boolean leaping;
        long leapStart;
        long noFallUntil;
        long vortexUntil;
        float vortexPower;
        long phaseWindow;
        boolean phaseEcho;
        int hits;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** Delayed effects (void rifts, staggered pillars). */
    private record Pending(ServerLevel level, long at, Runnable action) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    static State state(Player p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new State());
    }

    static void later(ServerLevel level, int ticks, Runnable r) {
        PENDING.add(new Pending(level, level.getGameTime() + ticks, r));
    }

    // ------------------------------------------------------------------ swings

    /** Ticks between full-strength swings, after Bloodlust. */
    static int interval(ServerPlayer p, WeaponKind k, ItemStack stack) {
        float aps = k.attacksPerSecond;
        if (k == WeaponKind.BONECLEAVER && Arsenal.awakened(stack)) aps *= 1f + bloodlust(p);
        return Math.max(4, Math.round(20f / aps));
    }

    static float bloodlust(ServerPlayer p) {
        int n = 0;
        for (LivingEntity e : SiegeDamage.enemiesAround(p.serverLevel(), p.position(), 6)) {
            if (e.hasEffect(ModEffects.BLEEDING.get())) n++;
        }
        return Math.min(0.3f, n * 0.06f);
    }

    /**
     * A left-click swing with the main-hand weapon. {@code force} ignores the attack cooldown (GameTests).
     * Returns true if a swing happened.
     */
    public static boolean swing(ServerLevel level, ServerPlayer p, boolean force) {
        ItemStack stack = p.getMainHandItem();
        if (!(stack.getItem() instanceof MeleeWeaponItem w) || p.isSpectator()) return false;
        WeaponKind k = w.kind;
        State st = state(p);
        long now = level.getGameTime();
        if (!force && (now < st.nextSwing || now < st.whirlUntil || st.leaping)) return false;
        float strength = force ? 1f : Mth.clamp(p.getAttackStrengthScale(0.5f) * (k == WeaponKind.BONECLEAVER && Arsenal.awakened(stack) ? 1f + bloodlust(p) : 1f), 0f, 1f);
        boolean strong = strength > 0.7f;
        int window = Math.max(30, k.swingTicks() * 3);
        int step = strong ? (now - st.lastSwing <= window ? (st.step + 1) % k.combo.length : 0) : Math.max(0, st.step);
        st.step = step;
        st.lastSwing = now;
        st.nextSwing = now + 3;
        p.resetAttackStrengthTicker();
        SwingStyle style = k.combo[step];
        boolean finisher = strong && step == k.combo.length - 1;
        int dur = animTicks(k, stack);
        broadcastAnim(p, k, style, step, dur);
        swingFx(level, p, k, style, finisher, strong);
        if (style.lunge > 0 && strong && p.onGround()) {
            Vec3 f = flatLook(p);
            p.setDeltaMovement(p.getDeltaMovement().add(f.x * style.lunge, 0, f.z * style.lunge));
            p.hurtMarked = true;
        }

        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.2f + strength * strength * 0.8f);
        boolean crit = strength > 0.9f && p.fallDistance > 0 && !p.onGround() && !p.onClimbable() && !p.isInWater() && !p.isPassenger()
                && !p.hasEffect(MobEffects.BLINDNESS);
        if (k == WeaponKind.VOIDEDGE && strong && level.random.nextFloat() < 0.15f) crit = true;
        if (crit) dmg *= 1.5f;
        if (finisher) dmg *= 1.3f;
        if (now < st.edgeUntil) dmg *= 1.25f;

        List<LivingEntity> targets = targets(level, p, k, style, finisher);
        int landed = 0;
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity t = targets.get(i);
            float d = dmg * (i == 0 ? 1f : 0.8f);
            if (hit(level, p, stack, k, t, d, style, finisher, crit)) landed++;
        }
        if (landed > 0) {
            st.hits += landed;
            if (strong) gainFury(stack, k, landed, now);
            hitSound(level, p, k, crit, finisher);
            if (crit) level.sendParticles(ParticleTypes.CRIT, targets.get(0).getX(), targets.get(0).getY() + targets.get(0).getBbHeight() * 0.6,
                    targets.get(0).getZ(), 10, 0.3, 0.3, 0.3, 0.2);
        }
        if (finisher) finisherEffect(level, p, stack, k, style, dmg, targets);
        return true;
    }

    static int animTicks(WeaponKind k, ItemStack stack) {
        return Mth.clamp(Math.round(k.swingTicks() * 0.85f), 6, 16);
    }

    static Vec3 flatLook(Entity e) {
        Vec3 l = e.getLookAngle();
        Vec3 f = new Vec3(l.x, 0, l.z);
        return f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
    }

    /** Enemies inside the swing's arc or line, nearest first, the crosshair target always included. */
    static List<LivingEntity> targets(ServerLevel level, ServerPlayer p, WeaponKind k, SwingStyle style, boolean finisher) {
        double reach = k.reach * (finisher && style != SwingStyle.THRUST ? 1.1 : 1.0);
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 flat = flatLook(p);
        AABB box = p.getBoundingBox().inflate(reach + 1.5);
        List<LivingEntity> out = new ArrayList<>();
        LivingEntity aimed = null;
        EntityHitResult ehr = ProjectileUtil.getEntityHitResult(level, p, eye, eye.add(look.scale(reach)), box, e -> valid(p, e));
        if (ehr != null && ehr.getEntity() instanceof LivingEntity le && sight(level, p, eye, ehr.getLocation())) aimed = le;
        double cosHalf = Math.cos(Math.toRadians(style.arcDeg * 0.5));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> valid(p, e))) {
            if (e == aimed) continue;
            Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
            Vec3 d = c.subtract(eye);
            double horiz = Math.sqrt(d.x * d.x + d.z * d.z) - e.getBbWidth() * 0.5;
            if (horiz > reach) continue;
            if (e.getY() + e.getBbHeight() < eye.y - 2.8 || e.getY() > eye.y + (style == SwingStyle.UPPERCUT ? 2.4 : 1.4)) continue;
            if (style == SwingStyle.THRUST) {
                double along = d.dot(look);
                if (along < -0.3) continue;
                Vec3 perp = d.subtract(look.scale(along));
                if (perp.length() > 0.85 + e.getBbWidth() * 0.5) continue;
            } else if (style != SwingStyle.SPIN) {
                Vec3 dh = new Vec3(d.x, 0, d.z);
                if (dh.lengthSqr() > 1e-4 && horiz > 0.9 && dh.normalize().dot(flat) < cosHalf) continue;
            }
            if (!sight(level, p, eye, c)) continue;
            out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        if (aimed != null) out.add(0, aimed);
        int max;
        if (style == SwingStyle.SPIN) max = 64;
        else if (style == SwingStyle.THRUST && k == WeaponKind.STORMPIERCER) max = 8;
        else max = k.cleave + (finisher ? 2 : 0);
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    static boolean valid(Player p, Entity e) {
        return e instanceof LivingEntity le && e != p && le.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && e.isPickable()
                && SiegeDamage.canHit(p, e) && !(e instanceof SiegeEnemy se && se.isUntargetable());
    }

    static boolean sight(ServerLevel level, Entity p, Vec3 from, Vec3 to) {
        HitResult r = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS || r.getLocation().distanceToSqr(from) >= from.distanceToSqr(to) - 0.6;
    }

    static boolean undead(LivingEntity e) {
        return e instanceof SiegeEnemy || e.getMobType() == MobType.UNDEAD;
    }

    /** One weapon hit on one target, with every on-hit passive. */
    static boolean hit(ServerLevel level, ServerPlayer p, ItemStack stack, WeaponKind k, LivingEntity t, float dmg, SwingStyle style,
                       boolean finisher, boolean crit) {
        boolean pierce = false;
        switch (k) {
            case MILITIA_LONGSWORD -> pierce = finisher;
            case FROSTFANG -> {
                if (t.hasEffect(ModEffects.FROZEN.get())) {
                    dmg *= 2f;
                    t.removeEffect(ModEffects.FROZEN.get());
                    shatterFx(level, t);
                }
            }
            case DAWNBREAKER -> {
                if (undead(t)) dmg *= 1.5f;
            }
            default -> {
            }
        }
        float before = t.getHealth();
        boolean ok = SiegeDamage.deal(t, p, p, dmg, k.element, false, pierce);
        if (!ok) return false;
        float dealt = Math.max(0, before - t.getHealth());
        double kb = switch (k.weaponClass) {
            case DAGGER -> 0.12;
            case KATANA -> 0.22;
            case LONGSWORD -> 0.3;
            case SPEAR -> 0.32;
            case AXE -> 0.42;
            case SCYTHE -> 0.25;
            case GREATSWORD -> 0.5;
            case HAMMER -> 0.75;
        };
        if (finisher) kb *= 1.6;
        if (!(k == WeaponKind.SOULREAPER && finisher)) SiegeDamage.push(t, p.position(), kb, style == SwingStyle.UPPERCUT ? 0.45 : 0.08);
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.6, 0);
        Vfx.spray(level, FxStyle.SPARK, at, k.color2, k.color, 0.12f, crit ? 10 : 5, 0.35f, 0.2f);
        switch (k) {
            case FROSTFANG -> {
                MobEffectInstance chill = t.getEffect(ModEffects.CHILLED.get());
                if (chill != null && chill.getAmplifier() >= 3) {
                    t.removeEffect(ModEffects.CHILLED.get());
                    SiegeDamage.apply(t, ModEffects.FROZEN.get(), 60, 0);
                    Vfx.spray(level, FxStyle.FLAKE, at, 0xFFFFFF, 0x9FEAFF, 0.2f, 12, 0.25f, 0.4f);
                } else {
                    SiegeDamage.applyStacking(t, ModEffects.CHILLED.get(), 80, 4);
                }
            }
            case BONECLEAVER -> SiegeDamage.applyStacking(t, ModEffects.BLEEDING.get(), 100, 3);
            case VOIDEDGE -> {
                if (crit) {
                    SiegeDamage.applyStacking(t, ModEffects.MARKED.get(), 120, 2);
                    Vfx.flash(level, at, 0xB04AFF, 0.8f, 5);
                }
            }
            case STORMPIERCER -> {
                if (style == SwingStyle.THRUST) SiegeDamage.apply(t, ModEffects.SHOCKED.get(), 20, 0);
                if (Arsenal.awakened(stack) && (state(p).hits + 1) % 4 == 0) {
                    Vfx.lightning(level, t.position(), 0xF4F06A, 0.8f);
                    SiegeDamage.areaDamage(level, t.position(), 1.6, dmg * 0.8f, Element.LIGHTNING, p, p, true);
                    level.playSound(null, t.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.6f);
                }
            }
            case SOULREAPER -> {
                if (dealt > 0) p.heal(dealt * 0.08f);
            }
            default -> {
            }
        }
        return true;
    }

    static void gainFury(ItemStack stack, WeaponKind k, int landed, long now) {
        if (k == WeaponKind.SOULREAPER) return;
        int per = Mth.clamp(Math.round(8f / k.attacksPerSecond), 3, 12);
        int gain = per + (landed - 1) * per / 2;
        MeleeWeaponItem.setFury(stack, MeleeWeaponItem.fury(stack) + gain, now);
    }

    // ------------------------------------------------------------------ swing & hit presentation

    static void swingFx(ServerLevel level, ServerPlayer p, WeaponKind k, SwingStyle style, boolean finisher, boolean strong) {
        Vec3 flat = flatLook(p);
        float heft = k.weaponClass.heft;
        int life = Math.round(6 + 3 * heft);
        if (style == SwingStyle.THRUST) {
            Vec3 from = p.getEyePosition().add(0, -0.35, 0).add(flat.scale(0.6));
            Vec3 to = p.getEyePosition().add(0, -0.25, 0).add(p.getLookAngle().scale(k.reach + (finisher ? 1.2 : 0.4)));
            Vfx.dash(level, from, to, k.color, finisher ? 0.32f : 0.2f, life);
        } else {
            Vec3 c = p.position().add(0, p.getBbHeight() * 0.62, 0).add(flat.scale(0.2));
            float radius = (float) (k.reach * (style == SwingStyle.SPIN ? 0.85 : 0.72));
            float thick = Mth.clamp(0.22f + 0.12f * heft + (finisher ? 0.1f : 0f), 0.2f, 0.6f);
            Vfx.slash(level, c, p.getYRot(), style.tilt, style.sweep, radius, k.color, k.color2, life, thick);
            if (style == SwingStyle.SPIN && finisher) Vfx.ring(level, p.position().add(0, 0.1, 0), k.color, radius * 1.1f, 10, 0.5f);
        }
        float pitch = (1.25f - 0.25f * heft) + level.random.nextFloat() * 0.15f;
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, strong ? 0.8f : 0.4f, pitch);
        if (finisher && heft >= 1.2f) Vfx.shake(level, p.position(), 0.2f, 5, 4);
    }

    static void hitSound(ServerLevel level, ServerPlayer p, WeaponKind k, boolean crit, boolean finisher) {
        SoundEvent s = crit ? SoundEvents.PLAYER_ATTACK_CRIT : switch (k.weaponClass) {
            case HAMMER -> SoundEvents.ZOMBIE_ATTACK_IRON_DOOR;
            case AXE -> SoundEvents.PLAYER_ATTACK_KNOCKBACK;
            case DAGGER -> SoundEvents.PLAYER_ATTACK_WEAK;
            default -> SoundEvents.PLAYER_ATTACK_STRONG;
        };
        level.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, k.weaponClass == WeaponKind.WeaponClass.HAMMER ? 0.6f : 1f,
                0.9f + level.random.nextFloat() * 0.2f);
        if (k == WeaponKind.FROSTFANG) level.playSound(null, p.blockPosition(), SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 0.5f, 1.6f);
        if (k == WeaponKind.VOIDEDGE) level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.6f, 0.7f);
        if (k == WeaponKind.DAWNBREAKER && finisher) level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    static void shatterFx(ServerLevel level, LivingEntity t) {
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0);
        Vfx.spray(level, FxStyle.SHARD, at, 0xE8FAFF, 0x9FEAFF, 0.22f, 16, 0.4f, 0.3f);
        Vfx.flash(level, at, 0xBFF4FF, 1.0f, 5);
        level.playSound(null, t.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9f, 1.3f);
    }

    /** What the last hit of each combo does on top of its damage. */
    static void finisherEffect(ServerLevel level, ServerPlayer p, ItemStack stack, WeaponKind k, SwingStyle style, float dmg, List<LivingEntity> hitList) {
        Vec3 flat = flatLook(p);
        switch (k) {
            case EARTHSHAKER -> {
                Vec3 impact = p.position().add(flat.scale(2.2));
                Fx.shockwave(level, impact, 2.8, k.color);
                for (LivingEntity e : SiegeDamage.enemiesAround(level, impact, 2.8)) {
                    if (hitList.contains(e)) continue;
                    if (SiegeDamage.deal(e, p, p, dmg * 0.6f, Element.PHYSICAL, false)) SiegeDamage.push(e, impact, 0.3, 0.35);
                }
                level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.4f);
                if (Arsenal.awakened(stack)) HazardZone.spawn(level, HazardZone.Kind.FIRE, impact, 2.4f, 80, 1.5f + Arsenal.mk(stack) * 0.4f, p, false);
            }
            case DAWNBREAKER -> {
                Vec3 from = p.position().add(flat.scale(1.0)), to = p.position().add(flat.scale(7.0));
                Vfx.wave(level, from, to, k.color, 2.6f, 1.8f, 12);
                for (LivingEntity e : SiegeDamage.enemiesAround(level, p.position().add(flat.scale(4)), 4.5)) {
                    Vec3 rel = e.position().subtract(p.position());
                    double along = rel.dot(flat);
                    if (along < 0.5 || along > 7.5 || rel.subtract(flat.scale(along)).horizontalDistance() > 1.6 + e.getBbWidth() * 0.5) continue;
                    SiegeDamage.deal(e, p, p, dmg * 0.8f * (undead(e) ? 1.5f : 1f), Element.HOLY, false);
                    SiegeDamage.apply(e, ModEffects.PURIFIED.get(), 80, 0);
                }
            }
            case VOIDEDGE -> {
                if (Arsenal.awakened(stack)) {
                    SiegeProjectile c = SiegeProjectile.create(level, SiegeProjectile.ProjKind.ARCANE_BOLT, p, p.getEyePosition().add(0, -0.3, 0))
                            .payload(dmg * 0.8f, Element.ARCANE);
                    c.pierce = 4;
                    c.life = 12;
                    c.aim(p.getEyePosition().add(p.getLookAngle().scale(10)), 1.4f).scale(1.6f).fire(level);
                }
            }
            case SOULREAPER -> {
                if (Arsenal.awakened(stack)) {
                    for (LivingEntity e : SiegeDamage.enemiesAround(level, p.position(), k.reach + 0.5)) SiegeDamage.push(e, p.position(), -0.5, 0.05);
                    Vfx.vortex(level, p.position(), 0x5AFFD8, 0xC8FFF0, (float) k.reach, 1.2f, 10, p);
                }
            }
            case BONECLEAVER -> Vfx.spray(level, FxStyle.DROP, p.position().add(0, 1, 0), 0x9A0E0E, 0x5A0606, 0.15f, 14, 0.4f, 0.2f);
            default -> {
            }
        }
    }

    static void broadcastAnim(ServerPlayer p, WeaponKind k, SwingStyle style, int step, int dur) {
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new WeaponAnimPacket(p.getId(), k.ordinal(), style.ordinal(), step, dur));
    }

    // ------------------------------------------------------------------ specials

    public static boolean canAffordSpecial(Player p, ItemStack stack, WeaponKind k) {
        if (p.getAbilities().instabuild) return true;
        if (k == WeaponKind.SOULREAPER) return MeleeWeaponItem.souls(stack) >= 3;
        return MeleeWeaponItem.fury(stack) >= k.specialFury;
    }

    /** Client-side feedback when a special can't be afforded. */
    public static void notEnough(Player p, WeaponKind k) {
        p.displayClientMessage(Component.literal(k == WeaponKind.SOULREAPER ? "Harvest needs at least 3 souls." :
                k.special + " needs " + k.specialFury + " Fury. Land hits to build it.").withStyle(ChatFormatting.RED), true);
    }

    static void pay(ServerPlayer p, ItemStack stack, WeaponKind k, long now) {
        if (!p.getAbilities().instabuild) {
            if (k == WeaponKind.SOULREAPER) MeleeWeaponItem.setSouls(stack, 0);
            else MeleeWeaponItem.setFury(stack, MeleeWeaponItem.fury(stack) - k.specialFury, now);
        }
    }

    static void cooldown(ServerPlayer p, ItemStack stack, WeaponKind k, int ticks) {
        p.getCooldowns().addCooldown(stack.getItem(), ticks);
    }

    /** Right-click special for instant abilities. Returns false if it could not be used. */
    public static boolean special(ServerLevel level, ServerPlayer p, ItemStack stack, WeaponKind k) {
        State st = state(p);
        long now = level.getGameTime();
        // the Voidedge's echo dash is free
        boolean echo = k == WeaponKind.VOIDEDGE && st.phaseEcho && now <= st.phaseWindow;
        if (!echo && !canAffordSpecial(p, stack, k)) {
            notEnough(p, k);
            return false;
        }
        boolean ok = switch (k) {
            case MILITIA_LONGSWORD -> riposte(level, p, stack, st, now);
            case FROSTFANG -> shadowStep(level, p, stack);
            case BONECLEAVER -> whirlwind(level, p, stack, st, now);
            case VOIDEDGE -> phaseRend(level, p, stack);
            case STORMPIERCER -> javelin(level, p, stack);
            case EARTHSHAKER -> leap(level, p, stack, st, now);
            case SOULREAPER -> harvest(level, p, stack, st, now);
            case DAWNBREAKER -> false;
        };
        if (!ok) return false;
        if (k == WeaponKind.VOIDEDGE && Arsenal.ascended(stack)) {
            if (echo) {
                st.phaseEcho = false;
                cooldown(p, stack, k, k.specialCooldown * 20);
            } else {
                pay(p, stack, k, now);
                st.phaseEcho = true;
                st.phaseWindow = now + 60;
                cooldown(p, stack, k, 8);
            }
            return true;
        }
        pay(p, stack, k, now);
        cooldown(p, stack, k, k.specialCooldown * 20);
        return true;
    }

    static boolean riposte(ServerLevel level, ServerPlayer p, ItemStack stack, State st, long now) {
        st.parryUntil = now + 14;
        broadcastAnim(p, WeaponKind.MILITIA_LONGSWORD, SwingStyle.DIAG_UP, 0, 8);
        Vfx.flash(level, p.getEyePosition().add(flatLook(p).scale(0.8)), 0xFFFFFF, 0.8f, 6);
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1f, 1.4f);
        return true;
    }

    static boolean shadowStep(ServerLevel level, ServerPlayer p, ItemStack stack) {
        LivingEntity target = lookTarget(level, p, 14, 18);
        if (target == null) {
            p.displayClientMessage(Component.literal("No enemy in sight.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        Vec3 origin = p.position();
        Vec3 dir = target.position().subtract(origin).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? flatLook(p) : dir.normalize();
        Vec3 dest = null;
        double back = target.getBbWidth() * 0.5 + 0.8;
        for (Vec3 off : new Vec3[]{dir, new Vec3(-dir.z, 0, dir.x), new Vec3(dir.z, 0, -dir.x)}) {
            Vec3 c = target.position().add(off.scale(back));
            if (level.noCollision(p, p.getDimensions(p.getPose()).makeBoundingBox(c))) {
                dest = c;
                break;
            }
        }
        if (dest == null) return false;
        Vec3 face = target.position().subtract(dest);
        float yaw = (float) (Mth.atan2(face.z, face.x) * Mth.RAD_TO_DEG) - 90f;
        Vfx.dash(level, origin.add(0, 1, 0), dest.add(0, 1, 0), 0x9FEAFF, 0.35f, 10);
        Vfx.spray(level, FxStyle.FLAKE, origin.add(0, 1, 0), 0xFFFFFF, 0x9FEAFF, 0.2f, 14, 0.25f, 0.2f);
        p.connection.teleport(dest.x, dest.y, dest.z, yaw, 10f);
        p.fallDistance = 0;
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 4f;
        if (target.hasEffect(ModEffects.FROZEN.get())) {
            dmg *= 2f;
            shatterFx(level, target);
        }
        SiegeDamage.deal(target, p, p, dmg, Element.ICE, false);
        SiegeDamage.apply(target, ModEffects.FROZEN.get(), 60, 0);
        broadcastAnim(p, WeaponKind.FROSTFANG, SwingStyle.THRUST, 2, 8);
        Vfx.spray(level, FxStyle.SHARD, target.position().add(0, target.getBbHeight() * 0.6, 0), 0xE8FAFF, 0x9FEAFF, 0.2f, 12, 0.3f, 0.3f);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.6f);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.8f);
        if (Arsenal.ascended(stack)) {
            Vfx.ring(level, origin.add(0, 0.1, 0), 0x9FEAFF, 3.5f, 12, 0.8f);
            Vfx.spray(level, FxStyle.FLAKE, origin.add(0, 0.6, 0), 0xFFFFFF, 0x9FEAFF, 0.25f, 24, 0.4f, -1f);
            for (LivingEntity e : SiegeDamage.enemiesAround(level, origin, 3.5)) {
                SiegeDamage.deal(e, p, p, (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE), Element.ICE, false);
                SiegeDamage.applyStacking(e, ModEffects.CHILLED.get(), 100, 4);
            }
        }
        return true;
    }

    static boolean whirlwind(ServerLevel level, ServerPlayer p, ItemStack stack, State st, long now) {
        boolean asc = Arsenal.ascended(stack);
        st.whirlUntil = now + (asc ? 100 : 50);
        st.whirlAscended = asc;
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6f, 1.4f);
        return true;
    }

    static boolean phaseRend(ServerLevel level, ServerPlayer p, ItemStack stack) {
        Vec3 dir = flatLook(p);
        Vec3 start = p.position();
        Vec3 end = start;
        for (double d = 0.5; d <= 9.0; d += 0.5) {
            Vec3 c = start.add(dir.scale(d));
            if (!level.noCollision(p, p.getDimensions(p.getPose()).makeBoundingBox(c))) break;
            end = c;
        }
        if (end.distanceToSqr(start) < 1.0) return false;
        Vec3 e2 = end;
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.2f;
        List<LivingEntity> victims = new ArrayList<>();
        Vec3 seg = e2.subtract(start);
        double len = seg.length();
        for (LivingEntity e : SiegeDamage.enemiesAround(level, start.add(seg.scale(0.5)), len * 0.5 + 2)) {
            Vec3 rel = e.position().subtract(start);
            double along = Mth.clamp(rel.dot(seg) / len, 0, len);
            if (rel.subtract(seg.normalize().scale(along)).horizontalDistance() < 1.6 + e.getBbWidth() * 0.5) victims.add(e);
        }
        Vfx.dash(level, start.add(0, 1, 0), e2.add(0, 1, 0), 0xB04AFF, 0.45f, 14);
        Vfx.spray(level, FxStyle.GLOW, start.add(0, 1, 0), 0xD8A0FF, 0x6A20C0, 0.3f, 12, 0.2f, 0f);
        p.connection.teleport(e2.x, e2.y, e2.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        level.playSound(null, start.x, start.y, start.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9f, 0.7f);
        broadcastAnim(p, WeaponKind.VOIDEDGE, SwingStyle.SLASH_L, 0, 8);
        for (LivingEntity v : victims) {
            Vec3 at = v.position().add(0, v.getBbHeight() * 0.5, 0);
            Vfx.sigil(level, v.position().add(0, 0.05, 0), 0xB04AFF, 1.2f, 20, 2f);
            later(level, 18, () -> {
                if (!v.isAlive()) return;
                Vec3 now = v.position().add(0, v.getBbHeight() * 0.5, 0);
                Vfx.slash(level, now, p.getYRot() + 45, 45, 160, 1.4f, 0xB04AFF, 0xF0D8FF, 8, 0.35f);
                Vfx.slash(level, now, p.getYRot() - 45, -45, 160, 1.4f, 0xB04AFF, 0xF0D8FF, 8, 0.35f);
                Vfx.flash(level, now, 0xC86BFF, 1.4f, 6);
                SiegeDamage.deal(v, p, p, dmg, Element.ARCANE, false);
                SiegeDamage.applyStacking(v, ModEffects.MARKED.get(), 120, 2);
                level.playSound(null, v.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
            });
            Vfx.flash(level, at, 0x8A30E0, 0.6f, 6);
        }
        return true;
    }

    static boolean javelin(ServerLevel level, ServerPlayer p, ItemStack stack) {
        boolean asc = Arsenal.ascended(stack);
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 eye = p.getEyePosition();
        SiegeProjectile j = SiegeProjectile.create(level, SiegeProjectile.ProjKind.JAVELIN, p, eye.add(0, -0.1, 0)).payload(dmg * 2f, Element.LIGHTNING).area(2.5f);
        j.life = 50;
        j.onImpact = (pr, at) -> {
            int strikes = asc ? 3 : 1;
            for (int i = 0; i < strikes; i++) {
                Vec3 s = i == 0 ? at : at.add(Math.cos(i * 2.1) * 2.2, 0, Math.sin(i * 2.1) * 2.2);
                Vfx.lightning(level, s, 0xF4F06A, 1.3f);
                SiegeDamage.areaDamage(level, s, 2.5, dmg * 1.5f, Element.LIGHTNING, p, p, true);
            }
            Set<LivingEntity> hit = new HashSet<>();
            Vec3 from = at.add(0, 0.8, 0);
            List<LivingEntity> near = SiegeDamage.enemiesAround(level, at, 9);
            near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
            for (LivingEntity e : near) {
                if (hit.size() >= (asc ? 10 : 6)) break;
                Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0);
                Fx.bolt(level, from, to, 0xF4F06A, 1f);
                SiegeDamage.deal(e, p, p, dmg * 1.2f, Element.LIGHTNING, false);
                SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 30, 0);
                hit.add(e);
                from = to;
            }
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.2f, 1.1f);
        };
        j.aim(eye.add(p.getLookAngle().scale(48)), 2.4f).scale(1.6f).fire(level);
        broadcastAnim(p, WeaponKind.STORMPIERCER, SwingStyle.OVERHEAD, 0, 10);
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1f, 0.8f);
        return true;
    }

    static boolean leap(ServerLevel level, ServerPlayer p, ItemStack stack, State st, long now) {
        if (!p.onGround() && !p.isInWater()) return false;
        Vec3 f = flatLook(p);
        p.setDeltaMovement(f.x * 1.15, 0.85, f.z * 1.15);
        p.hurtMarked = true;
        st.leaping = true;
        st.leapStart = now;
        st.noFallUntil = now + 80;
        Vfx.spray(level, FxStyle.DEBRIS, p.position(), 0x6A5A48, 0x3A3028, 0.2f, 12, 0.3f, 0.6f);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.5f);
        broadcastAnim(p, WeaponKind.EARTHSHAKER, SwingStyle.UPPERCUT, 0, 14);
        return true;
    }

    static void slam(ServerLevel level, ServerPlayer p, ItemStack stack) {
        Vec3 at = p.position();
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.5f;
        broadcastAnim(p, WeaponKind.EARTHSHAKER, SwingStyle.OVERHEAD, 2, 10);
        Fx.shockwave(level, at, 4.5, 0xFF8A30);
        Vfx.flash(level, at.add(0, 0.5, 0), 0xFFB060, 2.5f, 6);
        Vfx.shake(level, at, 1.0f, 16, 10);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1f, 0.7f);
        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.5f);
        for (LivingEntity e : SiegeDamage.enemiesAround(level, at, 4.5)) {
            if (SiegeDamage.deal(e, p, p, dmg, Element.PHYSICAL, false)) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                e.hurtMarked = true;
                SiegeDamage.apply(e, ModEffects.CONCUSSED.get(), 40, 0);
                SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 25, 0);
            }
        }
        if (Arsenal.awakened(stack)) HazardZone.spawn(level, HazardZone.Kind.FIRE, at, 3f, 80, 1.5f + Arsenal.mk(stack) * 0.4f, p, false);
        if (Arsenal.ascended(stack)) {
            Vec3 f = flatLook(p);
            for (int i = -1; i <= 1; i++) {
                double a = Math.atan2(f.z, f.x) + i * 0.7;
                Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a));
                for (int s = 1; s <= 4; s++) {
                    Vec3 pt = at.add(d.scale(1.6 * s));
                    int delay = s * 3;
                    later(level, delay, () -> {
                        HazardZone.spawn(level, HazardZone.Kind.FIRE, pt, 1.2f, 80, 2f, p, false);
                        Vfx.spray(level, FxStyle.FLAME, pt.add(0, 0.3, 0), 0xFFD060, 0xFF4010, 0.4f, 8, 0.25f, 1f);
                        SiegeDamage.areaDamage(level, pt, 1.4, dmg * 0.4f, Element.FIRE, p, p, true);
                    });
                }
            }
        }
    }

    static boolean harvest(ServerLevel level, ServerPlayer p, ItemStack stack, State st, long now) {
        int souls = p.getAbilities().instabuild ? Math.max(10, MeleeWeaponItem.souls(stack)) : MeleeWeaponItem.souls(stack);
        p.heal(souls * 1.0f);
        st.vortexUntil = now + 30 + souls * 2L;
        st.vortexPower = souls;
        float r = 3f + souls * 0.08f;
        Vfx.vortex(level, p.position(), 0x5AFFD8, 0xC8FFF0, r, 3f, (int) (st.vortexUntil - now), p);
        Vfx.spray(level, FxStyle.SOUL, p.position().add(0, 1, 0), 0xC8FFF0, 0x5AFFD8, 0.3f, 10 + souls, 0.2f, 1f);
        level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5f, 1.4f);
        return true;
    }

    /** Charging the Dawnbreaker: light gathers, then a chime when it's ready. */
    public static void chargeTick(ServerLevel level, ServerPlayer p, ItemStack stack, WeaponKind k, int used) {
        if (k != WeaponKind.DAWNBREAKER) return;
        if (used % 4 == 0) {
            Vec3 at = p.position().add(0, 1.2, 0);
            Vfx.spray(level, FxStyle.STAR, at, 0xFFFFFF, 0xFFE070, 0.2f, 3, 0.15f, 0.8f);
        }
        if (used == 20) {
            Vfx.aura(level, p, 0xFFE070, 1.4f, 40);
            level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
    }

    public static void releaseCharge(ServerLevel level, ServerPlayer p, ItemStack stack, WeaponKind k, int used) {
        if (k != WeaponKind.DAWNBREAKER || used < 20) return;
        if (p.getCooldowns().isOnCooldown(stack.getItem()) || !canAffordSpecial(p, stack, k)) return;
        long now = level.getGameTime();
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3f;
        Vec3 f = flatLook(p);
        boolean asc = Arsenal.ascended(stack);
        int pillars = asc ? 3 : 1;
        broadcastAnim(p, k, SwingStyle.OVERHEAD, 2, 12);
        for (int i = 0; i < pillars; i++) {
            Vec3 ground = groundAt(level, p, p.position().add(f.scale(asc ? 4 + i * 4 : 6)));
            later(level, i * 6, () -> judgement(level, p, ground, dmg, asc));
        }
        pay(p, stack, k, now);
        cooldown(p, stack, k, k.specialCooldown * 20);
    }

    static void judgement(ServerLevel level, ServerPlayer p, Vec3 ground, float dmg, boolean consecrate) {
        Vfx.pillar(level, ground, 0xFFE070, 1.8f, 14, 22);
        Vfx.ring(level, ground.add(0, 0.1, 0), 0xFFF2B0, 4f, 14, 0.8f);
        Vfx.flash(level, ground.add(0, 1.5, 0), 0xFFFFFF, 3f, 8);
        Vfx.spray(level, FxStyle.STAR, ground.add(0, 1, 0), 0xFFFFFF, 0xFFE070, 0.3f, 20, 0.35f, 0.8f);
        Vfx.shake(level, ground, 0.5f, 14, 8);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.6f);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.4f);
        for (LivingEntity e : SiegeDamage.enemiesAround(level, ground.add(0, 1, 0), 3.5)) {
            SiegeDamage.deal(e, p, p, dmg * (undead(e) ? 1.5f : 1f), Element.HOLY, false);
            SiegeDamage.apply(e, ModEffects.PURIFIED.get(), 120, 0);
        }
        for (LivingEntity a : SiegeDamage.defendersAround(level, ground.add(0, 1, 0), 5)) {
            a.heal(6f);
            Vfx.spray(level, FxStyle.STAR, a.position().add(0, 1, 0), 0xFFFFFF, 0x9CFF9C, 0.2f, 5, 0.1f, 1f);
        }
        if (consecrate) HazardZone.spawn(level, HazardZone.Kind.CONSECRATION, ground, 3f, 100, 2f, p, false);
    }

    static Vec3 groundAt(ServerLevel level, Entity viewer, Vec3 at) {
        Vec3 top = at.add(0, 3, 0);
        HitResult r = level.clip(new ClipContext(top, at.add(0, -6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        return r.getType() == HitResult.Type.BLOCK ? r.getLocation() : at;
    }

    @Nullable
    static LivingEntity lookTarget(ServerLevel level, ServerPlayer p, double range, double coneDeg) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double cos = Math.cos(Math.toRadians(coneDeg));
        LivingEntity best = null;
        double bestScore = -1;
        for (LivingEntity e : SiegeDamage.enemiesAround(level, eye.add(look.scale(range * 0.5)), range * 0.5 + 2)) {
            if (!valid(p, e)) continue;
            Vec3 d = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double dist = d.length();
            if (dist > range) continue;
            double c = d.normalize().dot(look);
            if (c < cos || !sight(level, p, eye, e.position().add(0, e.getBbHeight() * 0.5, 0))) continue;
            double score = c * 2 - dist / range;
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ continuous effects

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        State st = STATES.get(p.getUUID());
        if (st == null) return;
        long now = level.getGameTime();
        ItemStack stack = p.getMainHandItem();
        float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (now < st.whirlUntil) {
            if (!(stack.getItem() instanceof MeleeWeaponItem w) || w.kind != WeaponKind.BONECLEAVER) {
                st.whirlUntil = 0;
            } else if ((st.whirlUntil - now) % 5 == 0) {
                broadcastAnim(p, WeaponKind.BONECLEAVER, SwingStyle.SPIN, 2, 6);
                Vfx.slash(level, p.position().add(0, 1.0, 0), p.getYRot() + (now % 20) * 18, 4, 360, 3.2f, 0xE8DCC0, 0xFF5040, 6, 0.4f);
                level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.7f + level.random.nextFloat() * 0.2f);
                for (LivingEntity t : SiegeDamage.enemiesAround(level, p.position().add(0, 1, 0), 3.4)) {
                    if (!valid(p, t)) continue;
                    if (SiegeDamage.deal(t, p, p, base * 0.55f, Element.PHYSICAL, false)) {
                        SiegeDamage.applyStacking(t, ModEffects.BLEEDING.get(), 100, 3);
                        if (st.whirlAscended) SiegeDamage.push(t, p.position(), -0.18, 0);
                        else SiegeDamage.push(t, p.position(), 0.15, 0.05);
                    }
                }
            }
        }
        if (st.leaping) {
            if (now - st.leapStart > 3 && (p.onGround() || p.isInWater())) {
                st.leaping = false;
                if (stack.getItem() instanceof MeleeWeaponItem w && w.kind == WeaponKind.EARTHSHAKER) slam(level, p, stack);
            } else if (now - st.leapStart > 80) {
                st.leaping = false;
            }
        }
        if (now < st.vortexUntil && (st.vortexUntil - now) % 5 == 0) {
            float r = 3f + st.vortexPower * 0.08f;
            boolean asc = stack.getItem() instanceof MeleeWeaponItem w && w.kind == WeaponKind.SOULREAPER && Arsenal.ascended(stack);
            for (LivingEntity t : SiegeDamage.enemiesAround(level, p.position().add(0, 1, 0), r)) {
                if (!valid(p, t)) continue;
                if (asc && !(t instanceof com.lastbastion.entity.boss.SiegeBoss) && t.getHealth() < t.getMaxHealth() * 0.15f) {
                    SiegeDamage.deal(t, p, p, t.getHealth() + 10, Element.SHADOW, false);
                    Vfx.spray(level, FxStyle.SKULL, t.position().add(0, 1, 0), 0xC8FFF0, 0x5AFFD8, 0.35f, 4, 0.1f, 1f);
                    continue;
                }
                SiegeDamage.deal(t, p, p, base * 0.45f * (1f + st.vortexPower / 20f), Element.SHADOW, false);
                if (asc) SiegeDamage.push(t, p.position(), -0.22, 0);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
        if (st.phaseEcho && now > st.phaseWindow) {
            st.phaseEcho = false;
            if (stack.getItem() instanceof MeleeWeaponItem w && w.kind == WeaponKind.VOIDEDGE) cooldown(p, stack, w.kind, w.kind.specialCooldown * 20);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel level) || PENDING.isEmpty()) return;
        long now = level.getGameTime();
        List<Pending> due = new ArrayList<>();
        PENDING.removeIf(pd -> {
            if (pd.level() != level || pd.at() > now) return false;
            due.add(pd);
            return true;
        });
        for (Pending pd : due) {
            try {
                pd.action().run();
            } catch (Exception ex) {
                LastBastion.LOGGER.warn("Delayed weapon effect failed", ex);
            }
        }
    }

    /** Riposte: blows that land inside the parry window are deflected and answered. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        State st = STATES.get(p.getUUID());
        if (st == null || level.getGameTime() > st.parryUntil) return;
        ItemStack stack = p.getMainHandItem();
        if (!(stack.getItem() instanceof MeleeWeaponItem w) || w.kind != WeaponKind.MILITIA_LONGSWORD) return;
        Entity src = e.getSource().getEntity();
        Entity direct = e.getSource().getDirectEntity();
        Vec3 from = direct != null ? direct.position() : src != null ? src.position() : null;
        if (from == null || from.subtract(p.position()).multiply(1, 0, 1).normalize().dot(flatLook(p)) < 0.1) return;
        e.setCanceled(true);
        st.parryUntil = 0;
        Vec3 spark = p.getEyePosition().add(flatLook(p).scale(0.7)).add(0, -0.3, 0);
        Vfx.flash(level, spark, 0xFFFFFF, 1.4f, 6);
        Vfx.spray(level, FxStyle.SPARK, spark, 0xFFFFFF, 0xFFD070, 0.15f, 14, 0.45f, 0.3f);
        level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.3f);
        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 1.8f);
        broadcastAnim(p, w.kind, SwingStyle.SLASH_R, 0, 8);
        float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3f;
        if (src instanceof LivingEntity att && att != p && att.distanceToSqr(p) < 36) {
            SiegeDamage.deal(att, p, p, dmg, Element.PHYSICAL, false, true);
            SiegeDamage.push(att, p.position(), 0.6, 0.2);
            Vfx.slash(level, att.position().add(0, att.getBbHeight() * 0.5, 0), p.getYRot(), 20, 160, 1.6f, 0xFFFFFF, 0xD8E0EA, 8, 0.4f);
        }
        if (direct instanceof Projectile pr) pr.discard();
        if (Arsenal.awakened(stack)) st.edgeUntil = level.getGameTime() + 80;
        if (Arsenal.ascended(stack)) {
            for (LivingEntity t : SiegeDamage.enemiesAround(level, p.position().add(flatLook(p).scale(2)), 3.5)) {
                SiegeDamage.push(t, p.position(), 0.8, 0.25);
                SiegeDamage.apply(t, ModEffects.CONCUSSED.get(), 60, 0);
            }
            Vfx.wave(level, p.position(), p.position().add(flatLook(p).scale(4)), 0xFFFFFF, 3f, 1.6f, 10);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        State st = STATES.get(p.getUUID());
        if (st != null && p.level().getGameTime() < st.noFallUntil) {
            e.setDistance(0);
            e.setCanceled(true);
        }
    }

    /** Kill passives: souls, Cold Blood, Beacon of Hope. */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        ItemStack stack = p.getMainHandItem();
        if (!(stack.getItem() instanceof MeleeWeaponItem w)) return;
        LivingEntity dead = e.getEntity();
        switch (w.kind) {
            case SOULREAPER -> {
                int gain = dead instanceof SiegeEnemy se && se.isHighPriority() ? 3 : 1;
                MeleeWeaponItem.setSouls(stack, MeleeWeaponItem.souls(stack) + gain);
                Vfx.stream(level, FxStyle.SOUL, 0x5AFFD8, 0.3f, dead.position().add(0, 1, 0), p.position().add(0, 1, 0), 0.6);
            }
            case FROSTFANG -> {
                if (Arsenal.awakened(stack) && p.getCooldowns().isOnCooldown(stack.getItem())) {
                    float left = p.getCooldowns().getCooldownPercent(stack.getItem(), 0f) * w.kind.specialCooldown * 20;
                    p.getCooldowns().addCooldown(stack.getItem(), Math.max(1, Math.round(left * 0.6f)));
                }
            }
            case DAWNBREAKER -> {
                if (Arsenal.awakened(stack)) {
                    for (LivingEntity a : SiegeDamage.defendersAround(level, p.position(), 6)) {
                        a.heal(2f);
                        a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false));
                    }
                    Vfx.ring(level, p.position().add(0, 0.1, 0), 0xFFE070, 6f, 10, 0.4f);
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        STATES.remove(e.getEntity().getUUID());
    }
}
