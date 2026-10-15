package com.lastbastion.item.armor;

import com.lastbastion.LastBastion;
import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.combat.SiegeEnemy;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.registry.ModEffects;
import com.lastbastion.siege.SiegeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/** Set-bonus rules for the siege armour (see {@link ArmorSet} for what each bonus does). */
@Mod.EventBusSubscriber(modid = LastBastion.MODID)
public final class ArmorSets {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    static final String TAG_BULWARK = "lb_bulwark_cd", TAG_NOVA = "lb_nova_t", TAG_JETS = "lb_jets";
    public static final int MAX_JETS = 2;

    private ArmorSets() {
    }

    public static int piecesWorn(LivingEntity e, ArmorSet set) {
        int n = 0;
        for (EquipmentSlot s : SLOTS) {
            if (e.getItemBySlot(s).getItem() instanceof SiegeArmorItem a && a.set == set) n++;
        }
        return n;
    }

    /** The set whose bonus is active (all four pieces), or null. */
    @Nullable
    public static ArmorSet fullSet(LivingEntity e) {
        ItemStack head = e.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof SiegeArmorItem a)) return null;
        return piecesWorn(e, a.set) == 4 ? a.set : null;
    }

    public static boolean wearing(LivingEntity e, ArmorSet set) {
        return fullSet(e) == set;
    }

    // ------------------------------------------------------------------ hooks used by other systems

    /** Credit multiplier for kills (Scavenger). */
    public static float creditMult(@Nullable Player p) {
        return p != null && wearing(p, ArmorSet.SCAVENGER) ? 1.2f : 1f;
    }

    /** Critical-hit damage multiplier for melee (Shadowstalker). */
    public static float critMult(Player p) {
        return wearing(p, ArmorSet.SHADOWSTALKER) ? 1.95f : 1.5f;
    }

    /** Special-ability cooldown and Fury gain (Arcanist). */
    public static float cooldownMult(Player p) {
        return wearing(p, ArmorSet.ARCANIST) ? 0.7f : 1f;
    }

    public static float furyMult(Player p) {
        return wearing(p, ArmorSet.ARCANIST) ? 1.5f : 1f;
    }

    public static float reloadMult(@Nullable LivingEntity p) {
        return p != null && wearing(p, ArmorSet.ARCANIST) ? 0.7f : 1f;
    }

    // ------------------------------------------------------------------ events

    /** Shadowstalker: slip out of attacks. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent e) {
        try {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        if (!wearing(p, ArmorSet.SHADOWSTALKER) || e.getSource().getEntity() == null || level.random.nextFloat() >= 0.2f) return;
        e.setCanceled(true);
        Vec3 at = p.position().add(0, 1, 0);
        Vfx.spray(level, FxStyle.SMOKE, at, 0x3A2A50, 0x120A1A, 0.7f, 8, 0.12f, 0.3f);
        Vfx.flash(level, at, 0x8A6AD0, 0.6f, 4);
        level.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6f, 1.6f);
        p.invulnerableTime = 10;
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("ArmorSets.onAttacked", lbCrash);
        }
    }

    /** Vanguard: damage reduction and the emergency barrier. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        try {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        if (!wearing(p, ArmorSet.VANGUARD)) return;
        e.setAmount(e.getAmount() * 0.85f);
        float after = p.getHealth() - e.getAmount();
        long now = level.getGameTime();
        if (after < p.getMaxHealth() * 0.35f && after > 0 && now >= p.getPersistentData().getLong(TAG_BULWARK)) {
            p.getPersistentData().putLong(TAG_BULWARK, now + 800);
            p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), 20f));
            Vfx.dome(level, p.position(), 0x6FA8FF, 1.6f, 30, p);
            Vfx.ring(level, p.position().add(0, 0.1, 0), 0x6FA8FF, 3f, 12, 0.6f);
            level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 1.3f);
            level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.5f);
        }
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("ArmorSets.onHurt", lbCrash);
        }
    }

    /** Titan: no fall damage; long falls end in a shockwave slam. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        try {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level) || !wearing(p, ArmorSet.TITAN)) return;
        float dist = e.getDistance();
        e.setDistance(0);
        e.setDamageMultiplier(0);
        p.getPersistentData().putInt(TAG_JETS, 0);
        if (dist >= 4) {
            float dmg = Math.min(40f, dist * 2.5f);
            Fx.shockwave(level, p.position(), Math.min(6, 2 + dist * 0.3), 0x50C8FF);
            for (LivingEntity t : SiegeDamage.enemiesAround(level, p.position(), Math.min(6, 2 + dist * 0.3))) {
                SiegeDamage.deal(t, p, p, dmg, Element.PHYSICAL, false);
                SiegeDamage.push(t, p.position(), 0.8, 0.4);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("ArmorSets.onFall", lbCrash);
        }
    }

    /** Sunforged kill heals. */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        try {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        if (!(e.getEntity() instanceof SiegeEnemy) || !wearing(p, ArmorSet.SUNFORGED)) return;
        for (LivingEntity a : SiegeDamage.defendersAround(level, p.position(), 6)) {
            a.heal(2f);
            Vfx.spray(level, FxStyle.STAR, a.position().add(0, 1.2, 0), 0xFFFFFF, 0xFFE070, 0.15f, 3, 0.08f, 1f);
        }
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("ArmorSets.onKill", lbCrash);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        try {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        ArmorSet set = fullSet(p);
        if (set == null) return;
        long now = level.getGameTime();
        switch (set) {
            case SUNFORGED -> {
                p.removeEffect(ModEffects.BLEEDING.get());
                p.removeEffect(ModEffects.CORRODED.get());
                if (now % 20 == 0) {
                    int wave = SiegeManager.wave(level);
                    float dmg = 3f + wave * 0.15f;
                    boolean any = false;
                    for (LivingEntity t : SiegeDamage.enemiesAround(level, p.position().add(0, 1, 0), 5)) {
                        if (!(t instanceof SiegeEnemy) && t.getMobType() != net.minecraft.world.entity.MobType.UNDEAD) continue;
                        SiegeDamage.deal(t, p, p, dmg, Element.HOLY, false);
                        any = true;
                    }
                    if (any) Vfx.ring(level, p.position().add(0, 0.1, 0), 0xFFE070, 5f, 14, 0.3f);
                }
            }
            case ARCANIST -> {
                if (now % 20 == 0 && now - p.getPersistentData().getLong(TAG_NOVA) >= 160) {
                    var foes = SiegeDamage.enemiesAround(level, p.position().add(0, 1, 0), 4);
                    if (!foes.isEmpty()) {
                        p.getPersistentData().putLong(TAG_NOVA, now);
                        float dmg = 10f + SiegeManager.wave(level) * 0.4f;
                        Vfx.sphere(level, p.position().add(0, 1, 0), 0x8A5AFF, 4.2f, 10);
                        Vfx.ring(level, p.position().add(0, 0.1, 0), 0xC8A0FF, 4.2f, 12, 0.6f);
                        level.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.4f);
                        for (LivingEntity t : foes) {
                            SiegeDamage.deal(t, p, p, dmg, Element.ARCANE, false);
                            SiegeDamage.push(t, p.position(), 0.5, 0.2);
                        }
                    }
                }
            }
            case TITAN -> {
                if (p.onGround()) p.getPersistentData().putInt(TAG_JETS, 0);
            }
            default -> {
            }
        }
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("ArmorSets.onPlayerTick", lbCrash);
        }
    }

    /** Titan jump jets, requested by the client when jump is pressed in mid-air. */
    public static void jumpJet(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level) || !wearing(p, ArmorSet.TITAN) || p.onGround() || p.isInWater() || p.getAbilities().flying) return;
        int used = p.getPersistentData().getInt(TAG_JETS);
        if (used >= MAX_JETS) return;
        p.getPersistentData().putInt(TAG_JETS, used + 1);
        Vec3 v = p.getDeltaMovement();
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(v.x + look.x * 0.25, 0.72, v.z + look.z * 0.25);
        p.hurtMarked = true;
        p.fallDistance = 0;
        Vec3 at = p.position();
        Vfx.jet(level, FxStyle.FLAME, at.add(0, 0.2, 0), at.add(0, -2.2, 0), 0x9FE8FF, 0x2F7BFF, 0.35f, 10, 18);
        Vfx.spray(level, FxStyle.SMOKE, at, 0xB0B8C0, 0x60686C, 0.5f, 6, 0.1f, -1f);
        level.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8f, 1.4f);
    }
}
