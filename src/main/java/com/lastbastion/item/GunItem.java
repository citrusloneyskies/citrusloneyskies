package com.lastbastion.item;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.fx.FxStyle;
import com.lastbastion.fx.Vfx;
import com.lastbastion.item.weapon.Arsenal;
import com.lastbastion.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Core-shop firearms. Each gun has a magazine (or a heat/charge system), reloads (automatically when
 * empty or with the Reload key), a sneak alt-fire, recoil, muzzle flash and tracers, and takes part in
 * the Mk upgrade track (damage; Mk III enlarges the magazine, Mk V speeds up reloads).
 */
public class GunItem extends Item implements Arsenal.Upgradeable {
    public enum GunKind {
        //           title                               auto  cd  dmg   cost mag reload color     alt
        RAVAGER("M60 'Ravager' Machine Gun", true, 2, 4.5f, 600, 0, 0, 0xFFD070,
                "Hold to fire 10 rounds a second. Builds heat - overheat and it locks up until it cools.",
                "Incendiary Belt", "Sneak + use toggles incendiary rounds: they set targets ablaze but heat the barrel 50% faster."),
        THUNDERBOOM("'Thunderboom' Scattergun", false, 16, 3.2f, 450, 6, 36, 0xFFB060,
                "Eight pellets per blast with heavy knockback. Pump-action, 6 shells.",
                "Slug Round", "Sneak + use fires a single slug (2 shells) that punches through 3 enemies for massive damage."),
        HELLFIRE("'Hellfire' Rocket Launcher", false, 20, 30f, 900, 1, 46, 0xFF8A20,
                "High-explosive rockets with a 4-block blast. One rocket per reload.",
                "Cluster Warhead", "Sneak + use fires a rocket that bursts into five bomblets on impact."),
        STARPIERCER("'Starpiercer' Railgun", false, 60, 90f, 1400, 0, 0, 0x7FE8FF,
                "Hold to charge (1s), release to fire a beam that pierces every enemy in a line.",
                "Overcharge", "Sneak while charging to keep charging for 2.5s: a beam three times as strong and twice as wide."),
        DRAGONBREATH("'Dragon's Breath' Flamethrower", true, 2, 2.2f, 700, 0, 0, 0xFF7A1A,
                "Hold to spray a 7-block cone of fire. Builds heat while firing.",
                "Firebomb", "Sneak + use lobs a napalm canister that floods the ground with fire for 6 seconds."),
        STORMFIST("'Stormfist' Tesla Gauntlet", false, 22, 12f, 800, 8, 30, 0xF4F06A,
                "Chain lightning that arcs through 5 enemies. 8 cell charges.",
                "Thunderclap", "Sneak + use (3 cells) releases a shockwave that hurls back and stuns everything in front of you."),
        GLACIER("'Glacier' Cryo Cannon", false, 45, 14f, 750, 4, 40, 0x9FEAFF,
                "Frost orb that freezes everything in a 3.5-block burst. 4 orbs.",
                "Flash Freeze", "Sneak + use (2 orbs) vents a freezing cone that instantly freezes enemies in front of you."),
        SUNLANCE("'Sunlance' Holy Repeater", false, 8, 7f, 650, 24, 30, 0xFFE070,
                "Rapid holy bolts. Devastating against the undead. 24-bolt magazine.",
                "Blessed Volley", "Sneak + use fires a fan of five bolts at once.");

        public final String title;
        public final boolean automatic;
        public final int cooldown;
        public final float damage;
        public final int cost;
        /** Magazine size; 0 = no magazine (heat or charge based). */
        public final int mag;
        public final int reloadTicks;
        public final int color;
        public final String desc, alt, altDesc;

        GunKind(String title, boolean automatic, int cooldown, float damage, int cost, int mag, int reloadTicks, int color, String desc,
                String alt, String altDesc) {
            this.title = title;
            this.automatic = automatic;
            this.cooldown = cooldown;
            this.damage = damage;
            this.cost = cost;
            this.mag = mag;
            this.reloadTicks = reloadTicks;
            this.color = color;
            this.desc = desc;
            this.alt = alt;
            this.altDesc = altDesc;
        }

        public boolean usesHeat() {
            return this == RAVAGER || this == DRAGONBREATH;
        }

        public static GunKind byId(int i) {
            GunKind[] v = values();
            return v[Math.floorMod(i, v.length)];
        }
    }

    public static final String TAG_AMMO = "lbAmmo", TAG_RELOAD = "lbReload", TAG_RELOAD_LEN = "lbReloadLen", TAG_HEAT = "heat", TAG_OVERHEAT = "overheated",
            TAG_INCENDIARY = "lbInc";

    public final GunKind kind;

    public GunItem(Properties p, GunKind kind) {
        super(p.stacksTo(1).rarity(kind.cost >= 1000 ? Rarity.EPIC : Rarity.RARE));
        this.kind = kind;
    }

    @Override
    public int baseCost() {
        return kind.cost;
    }

    @Override
    public String displayTitle() {
        return kind.title;
    }

    /** Damage per hit after the weapon's Mk upgrades. */
    float dmg(ItemStack stack) {
        return kind.damage * Arsenal.damageMult(stack);
    }

    // ------------------------------------------------------------------ magazine

    public int magSize(ItemStack stack) {
        if (kind.mag <= 1) return kind.mag;
        return Arsenal.awakened(stack) ? Math.round(kind.mag * 1.5f) : kind.mag;
    }

    public int reloadTicks(ItemStack stack) {
        return Math.round(kind.reloadTicks * (Arsenal.ascended(stack) ? 0.7f : 1f));
    }

    /** Length of the reload in progress (ticks), for progress displays. */
    public int reloadLength(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t != null && t.getInt(TAG_RELOAD_LEN) > 0 ? t.getInt(TAG_RELOAD_LEN) : reloadTicks(stack);
    }

    public int ammo(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null || !t.contains(TAG_AMMO) ? magSize(stack) : t.getInt(TAG_AMMO);
    }

    void setAmmo(ItemStack stack, int v) {
        stack.getOrCreateTag().putInt(TAG_AMMO, Math.max(0, v));
    }

    public static boolean reloading(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t != null && t.getLong(TAG_RELOAD) > 0;
    }

    /** Starts a reload (server). Returns false if the magazine is already full or it is reloading. */
    public boolean startReload(Level level, Player p, ItemStack stack) {
        if (kind.mag <= 0 || reloading(stack) || ammo(stack) >= magSize(stack)) return false;
        int len = Math.max(1, Math.round(reloadTicks(stack) * com.lastbastion.item.armor.ArmorSets.reloadMult(p)));
        stack.getOrCreateTag().putLong(TAG_RELOAD, level.getGameTime() + len);
        stack.getOrCreateTag().putInt(TAG_RELOAD_LEN, len);
        level.playSound(null, p.blockPosition(), reloadSound(), SoundSource.PLAYERS, 0.8f, 1.0f);
        return true;
    }

    SoundEvent reloadSound() {
        return switch (kind) {
            case THUNDERBOOM -> SoundEvents.CROSSBOW_LOADING_START;
            case HELLFIRE -> SoundEvents.IRON_DOOR_OPEN;
            case STORMFIST -> SoundEvents.BEACON_DEACTIVATE;
            case GLACIER -> SoundEvents.BREWING_STAND_BREW;
            default -> SoundEvents.CROSSBOW_LOADING_MIDDLE;
        };
    }

    /** Spend {@code n} rounds; starts a reload and fails when there aren't enough. */
    boolean consume(Level level, Player p, ItemStack stack, int n) {
        if (kind.mag <= 0 || p.getAbilities().instabuild) return true;
        int a = ammo(stack);
        if (a < n) {
            if (!startReload(level, p, stack)) level.playSound(null, p.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
            return false;
        }
        setAmmo(stack, a - n);
        if (a - n == 0) startReload(level, p, stack);
        return true;
    }

    // ------------------------------------------------------------------ use

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (reloading(stack)) return InteractionResultHolder.fail(stack);
        boolean alt = player.isShiftKeyDown();
        if (kind == GunKind.RAVAGER && alt) {
            if (level instanceof ServerLevel) {
                boolean on = !stack.getOrCreateTag().getBoolean(TAG_INCENDIARY);
                stack.getOrCreateTag().putBoolean(TAG_INCENDIARY, on);
                player.displayClientMessage(Component.literal(on ? "Incendiary belt loaded." : "Standard belt loaded.")
                        .withStyle(on ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
                level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 1f, 0.8f);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (kind.automatic || kind == GunKind.STARPIERCER) {
            if (kind.usesHeat() && stack.getOrCreateTag().getBoolean(TAG_OVERHEAT)) return InteractionResultHolder.fail(stack);
            if (kind == GunKind.DRAGONBREATH && alt) {
                if (level instanceof ServerLevel sl) firebomb(sl, player, stack);
                return InteractionResultHolder.consume(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level instanceof ServerLevel sl) {
            boolean fired = alt ? fireAlt(sl, player, stack) : fireSemi(sl, player, stack);
            if (!fired) return InteractionResultHolder.fail(stack);
            player.getCooldowns().addCooldown(this, alt ? Math.max(kind.cooldown, 20) : kind.cooldown);
        } else if (!reloading(stack) && (ammo(stack) > 0 || kind.mag <= 0 || player.getAbilities().instabuild)) {
            clientShot(alt ? 1.6f : 1f);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Charge needed for the railgun shot (ticks): normal, or the sneaking overcharge. */
    public static int chargeNeeded(boolean overcharge) {
        return overcharge ? 50 : 20;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int used = getUseDuration(stack) - remaining;
        if (kind == GunKind.STARPIERCER) {
            if (!(level instanceof ServerLevel sl)) return;
            boolean over = user.isShiftKeyDown();
            Vec3 m = muzzle(user);
            if (used == 20) sl.playSound(null, user.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 2f);
            if (over && used == 50) {
                sl.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.6f);
                Vfx.flash(sl, m, 0x7FE8FF, 1.2f, 6);
            }
            if (used % 3 == 0 && used < (over ? 50 : 20)) {
                Vfx.spray(sl, FxStyle.SPARK, m, 0xFFFFFF, 0x7FE8FF, 0.08f, 2, 0.08f, 0f);
            }
            return;
        }
        if (used % kind.cooldown != 0) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (level.isClientSide) {
            if (!tag.getBoolean(TAG_OVERHEAT)) clientShot(kind == GunKind.RAVAGER ? 0.35f : 0.1f);
            return;
        }
        ServerLevel sl = (ServerLevel) level;
        boolean incendiary = kind == GunKind.RAVAGER && tag.getBoolean(TAG_INCENDIARY);
        int heat = tag.getInt(TAG_HEAT) + (kind == GunKind.RAVAGER ? (incendiary ? 6 : 4) : 3);
        tag.putInt(TAG_HEAT, heat);
        if (heat >= 100) {
            tag.putBoolean(TAG_OVERHEAT, true);
            user.stopUsingItem();
            sl.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 0.8f);
            Vfx.spray(sl, FxStyle.SMOKE, muzzle(user), 0xC8C8C8, 0x808080, 0.6f, 10, 0.08f, 1f);
            return;
        }
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 m = muzzle(user);
        if (kind == GunKind.RAVAGER) {
            Vec3 dir = Hitscan.spreadDir(user, look, 0.025 + heat * 0.0002);
            LivingEntity hit = Hitscan.fireTracer(sl, user, eye.add(0, -0.15, 0), m, dir, 48, dmg(stack), Element.PHYSICAL, incendiary ? 0xFF8A30 : 0xFFE6A0);
            if (hit != null && incendiary) {
                hit.setSecondsOnFire(3);
                SiegeDamage.applyStacking(hit, ModEffects.SCORCHED.get(), 60, 1);
            }
            muzzleFlash(sl, m, look, incendiary ? 0xFF8A30 : 0xFFD070, 0.55f);
            sl.playSound(null, user.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.5f, 1.8f + sl.random.nextFloat() * 0.2f);
            if (sl.random.nextInt(3) == 0) Vfx.spray(sl, FxStyle.SHARD, m.add(new Vec3(-look.z, 0, look.x).scale(0.2)), 0xE8C060, 0xB08A30, 0.08f, 1, 0.15f, 0.6f);
        } else if (kind == GunKind.DRAGONBREATH) {
            Vfx.jet(sl, FxStyle.FLAME, m, m.add(look.scale(7.5)), 0xFFE070, 0xFF3A08, 0.55f, 6, 14);
            if (used % 4 == 0) Vfx.jet(sl, FxStyle.SMOKE, m.add(look.scale(2)), m.add(look.scale(8.5)), 0x6A6260, 0x2A2826, 0.8f, 2, 16);
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, m.add(look.scale(3.5)), 4.5)) {
                if (e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(m).normalize().dot(look) < 0.8 || !reach(sl, user, m, e)) continue;
                SiegeDamage.deal(e, user, user, dmg(stack), Element.FIRE, false);
                e.setSecondsOnFire(4);
                if (sl.random.nextInt(4) == 0) SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 60, 1);
            }
            if (used % 8 == 0) sl.playSound(null, user.blockPosition(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (kind != GunKind.STARPIERCER) return;
        int used = getUseDuration(stack) - remaining;
        boolean over = user.isShiftKeyDown() && used >= chargeNeeded(true);
        if (used < chargeNeeded(false)) return;
        if (level.isClientSide) {
            clientShot(over ? 3f : 1.8f);
            return;
        }
        ServerLevel sl = (ServerLevel) level;
        Vec3 from = user.getEyePosition().add(0, -0.1, 0);
        Vec3 dir = user.getLookAngle();
        double width2 = over ? 5.0 : 1.5;
        // the slug pierces every enemy in its path, but not the wall that stops it
        net.minecraft.world.phys.HitResult wall = sl.clip(new net.minecraft.world.level.ClipContext(from, from.add(dir.scale(64)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, user));
        double range = wall.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? 64 : wall.getLocation().distanceTo(from);
        Vec3 end = from.add(dir.scale(range));
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, from.add(dir.scale(range / 2)), range / 2 + 1)) {
            Vec3 rel = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            double along = rel.dot(dir);
            if (along < 0 || along > range + e.getBbWidth() * 0.5 || rel.subtract(dir.scale(along)).lengthSqr() > width2) continue;
            hit.add(e);
        }
        float d = dmg(stack) * (over ? 3f : 1f);
        for (LivingEntity e : hit) {
            SiegeDamage.deal(e, user, user, d, Element.ARCANE, true);
            Vfx.flash(sl, e.position().add(0, e.getBbHeight() * 0.5, 0), 0x7FE8FF, over ? 1.4f : 0.8f, 5);
        }
        Vec3 m = muzzle(user);
        Vfx.beam(sl, m, end, 0x7FE8FF, over ? 0.55f : 0.3f, over ? 16 : 10, 0.02f);
        Vfx.beam(sl, m, end, 0xFFFFFF, over ? 0.18f : 0.1f, over ? 10 : 6);
        muzzleFlash(sl, m, dir, 0x7FE8FF, over ? 1.6f : 1f);
        Vfx.ring(sl, m, 0x7FE8FF, over ? 1.6f : 0.9f, 8, 0.2f);
        if (over) Vfx.shake(sl, user.position(), 0.6f, 8, 8);
        sl.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1f, over ? 1.1f : 1.6f);
        if (user instanceof Player p) p.getCooldowns().addCooldown(this, over ? 160 : kind.cooldown);
        user.push(-dir.x * (over ? 0.8 : 0.4), 0.1, -dir.z * (over ? 0.8 : 0.4));
    }

    // ------------------------------------------------------------------ firing

    private boolean fireSemi(ServerLevel sl, Player p, ItemStack stack) {
        if (!consume(sl, p, stack, 1)) return false;
        Vec3 eye = p.getEyePosition().add(0, -0.1, 0);
        Vec3 look = p.getLookAngle();
        Vec3 m = muzzle(p);
        switch (kind) {
            case THUNDERBOOM -> {
                for (int i = 0; i < 8; i++) {
                    LivingEntity hit = Hitscan.fireTracer(sl, p, eye, m, Hitscan.spreadDir(p, look, 0.09), 20, dmg(stack), Element.PHYSICAL, 0xFFC890);
                    if (hit != null) SiegeDamage.push(hit, p.position(), 0.35, 0.1);
                }
                muzzleFlash(sl, m, look, 0xFFB060, 1.2f);
                Vfx.jet(sl, FxStyle.SMOKE, m, m.add(look.scale(2.5)), 0xB0A8A0, 0x605850, 0.6f, 5, 25);
                sound(sl, p, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.6f);
                p.push(-look.x * 0.3, 0.05, -look.z * 0.3);
            }
            case HELLFIRE -> {
                SiegeProjectile r = SiegeProjectile.create(sl, ProjKind.ROCKET, p, m).payload(dmg(stack), Element.EXPLOSIVE).area(4f);
                r.knockback = 0.8f;
                r.aim(eye.add(look.scale(50)), 1.6f).scale(1.3f).fire(sl);
                muzzleFlash(sl, m, look, 0xFF8A20, 1.4f);
                Vfx.jet(sl, FxStyle.SMOKE, m.add(look.scale(-1.2)), m.add(look.scale(-4.5)), 0xC8C0B8, 0x707070, 0.9f, 8, 30);
                sound(sl, p, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.6f);
            }
            case STORMFIST -> {
                LivingEntity first = Hitscan.fire(sl, p, eye, look, 24, dmg(stack), Element.LIGHTNING, com.lastbastion.combat.Fx.dust(0xF4F06A, 0.6f));
                Vec3 end = first != null ? first.position().add(0, first.getBbHeight() * 0.5, 0) : eye.add(look.scale(10));
                Vfx.arc(sl, m, end, 0xF4F06A, 0.08f, 6, 0.8f);
                if (first != null) {
                    Set<LivingEntity> hit = new HashSet<>();
                    hit.add(first);
                    LivingEntity cur = first;
                    for (int i = 0; i < 4; i++) {
                        LivingEntity c = cur;
                        Vec3 from = c.position().add(0, c.getBbHeight() * 0.5, 0);
                        LivingEntity next = SiegeDamage.enemiesAround(sl, cur.position(), 6).stream().filter(e -> !hit.contains(e) && reach(sl, p, from, e))
                                .min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
                        if (next == null) break;
                        com.lastbastion.combat.Fx.bolt(sl, cur.position().add(0, cur.getBbHeight() * 0.5, 0),
                                next.position().add(0, next.getBbHeight() * 0.5, 0), 0xF4F06A, 1f);
                        SiegeDamage.deal(next, p, p, dmg(stack) * 0.8f, Element.LIGHTNING, false);
                        SiegeDamage.apply(next, ModEffects.SHOCKED.get(), 15, 0);
                        hit.add(next);
                        cur = next;
                    }
                    SiegeDamage.apply(first, ModEffects.SHOCKED.get(), 15, 0);
                }
                Vfx.flash(sl, m, 0xF4F06A, 0.7f, 4);
                sound(sl, p, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 1.6f);
            }
            case GLACIER -> {
                SiegeProjectile f = SiegeProjectile.create(sl, ProjKind.FROST_SHARD, p, m).payload(dmg(stack), Element.ICE).area(3.5f);
                f.onEntityHit = (pr, e) -> SiegeDamage.apply(e, ModEffects.FROZEN.get(), 60, 0);
                f.aim(eye.add(look.scale(40)), 1.4f).scale(2.2f).fire(sl);
                muzzleFlash(sl, m, look, 0x9FEAFF, 0.9f);
                Vfx.jet(sl, FxStyle.FLAKE, m, m.add(look.scale(2)), 0xFFFFFF, 0x9FEAFF, 0.25f, 6, 30);
                sound(sl, p, SoundEvents.SNOW_GOLEM_SHOOT, 1.2f, 0.5f);
            }
            case SUNLANCE -> {
                bolt(sl, p, stack, m, eye.add(look.scale(40)));
                muzzleFlash(sl, m, look, 0xFFE070, 0.6f);
                sound(sl, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.6f);
            }
            default -> {
            }
        }
        return true;
    }

    private void bolt(ServerLevel sl, Player p, ItemStack stack, Vec3 m, Vec3 target) {
        SiegeProjectile h = SiegeProjectile.create(sl, ProjKind.HOLY_BOLT, p, m).payload(dmg(stack), Element.HOLY).status(ModEffects.PURIFIED, 60, 0);
        h.aim(target, 2.2f).fire(sl);
    }

    /** Sneak + use: each gun's alternate fire. */
    private boolean fireAlt(ServerLevel sl, Player p, ItemStack stack) {
        Vec3 eye = p.getEyePosition().add(0, -0.1, 0);
        Vec3 look = p.getLookAngle();
        Vec3 m = muzzle(p);
        switch (kind) {
            case THUNDERBOOM -> {
                if (!consume(sl, p, stack, 2)) return false;
                List<LivingEntity> line = lineTargets(sl, p, eye, look, 30, 1.0);
                Vec3 end = eye.add(look.scale(30));
                int n = 0;
                for (LivingEntity e : line) {
                    if (n++ >= 3) break;
                    SiegeDamage.deal(e, p, p, dmg(stack) * 8f, Element.PHYSICAL, false);
                    SiegeDamage.push(e, p.position(), 0.9, 0.2);
                    Vfx.spray(sl, FxStyle.SPARK, e.position().add(0, e.getBbHeight() * 0.6, 0), 0xFFFFFF, 0xFFB060, 0.15f, 10, 0.4f, 0.2f);
                    end = e.position().add(0, e.getBbHeight() * 0.5, 0);
                }
                Vfx.beam(sl, m, end, 0xFFC890, 0.08f, 4);
                muzzleFlash(sl, m, look, 0xFFB060, 1.6f);
                sound(sl, p, SoundEvents.GENERIC_EXPLODE, 1f, 1.2f);
                p.push(-look.x * 0.6, 0.1, -look.z * 0.6);
            }
            case HELLFIRE -> {
                if (!consume(sl, p, stack, 1)) return false;
                float d = dmg(stack);
                SiegeProjectile r = SiegeProjectile.create(sl, ProjKind.ROCKET, p, m).payload(d * 0.6f, Element.EXPLOSIVE).area(3f);
                r.onImpact = (pr, at) -> {
                    for (int i = 0; i < 5; i++) {
                        double a = i * Math.PI * 2 / 5;
                        SiegeProjectile b = SiegeProjectile.create(sl, ProjKind.GRENADE, p, at.add(0, 0.6, 0)).payload(d * 0.45f, Element.EXPLOSIVE).area(2.6f);
                        b.setDeltaMovement(Math.cos(a) * 0.35, 0.45, Math.sin(a) * 0.35);
                        b.life = 30;
                        b.fire(sl);
                    }
                };
                r.aim(eye.add(look.scale(50)), 1.5f).scale(1.4f).fire(sl);
                muzzleFlash(sl, m, look, 0xFF8A20, 1.4f);
                sound(sl, p, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.5f);
            }
            case STORMFIST -> {
                if (!consume(sl, p, stack, 3)) return false;
                Vfx.wave(sl, p.position(), p.position().add(new Vec3(look.x, 0, look.z).normalize().scale(6)), 0xF4F06A, 3.5f, 1.8f, 10);
                for (LivingEntity e : SiegeDamage.enemiesAround(sl, p.position().add(look.scale(3)), 4.5)) {
                    Vec3 rel = e.position().subtract(p.position());
                    if (rel.normalize().dot(look) < 0.5 || !reach(sl, p, m, e)) continue;
                    SiegeDamage.deal(e, p, p, dmg(stack) * 1.5f, Element.LIGHTNING, false);
                    SiegeDamage.push(e, p.position(), 1.4, 0.4);
                    SiegeDamage.apply(e, ModEffects.SHOCKED.get(), 40, 0);
                    Vfx.arc(sl, m, e.position().add(0, e.getBbHeight() * 0.5, 0), 0xF4F06A, 0.06f, 6, 0.6f);
                }
                Vfx.flash(sl, m, 0xFFFFFF, 1.6f, 6);
                Vfx.shake(sl, p.position(), 0.5f, 8, 6);
                sound(sl, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
            }
            case GLACIER -> {
                if (!consume(sl, p, stack, 2)) return false;
                Vfx.jet(sl, FxStyle.FLAKE, m, m.add(look.scale(6)), 0xFFFFFF, 0x9FEAFF, 0.4f, 30, 30);
                Vfx.jet(sl, FxStyle.SHARD, m, m.add(look.scale(5)), 0xE8FAFF, 0x9FEAFF, 0.2f, 14, 25);
                for (LivingEntity e : SiegeDamage.enemiesAround(sl, m.add(look.scale(3)), 4)) {
                    if (e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(m).normalize().dot(look) < 0.65 || !reach(sl, p, m, e)) continue;
                    SiegeDamage.deal(e, p, p, dmg(stack) * 0.5f, Element.ICE, false);
                    SiegeDamage.apply(e, ModEffects.FROZEN.get(), 50, 0);
                }
                sound(sl, p, SoundEvents.POWDER_SNOW_BREAK, 1.2f, 0.6f);
                sound(sl, p, SoundEvents.GLASS_BREAK, 0.8f, 0.7f);
            }
            case SUNLANCE -> {
                if (!consume(sl, p, stack, 5)) return false;
                Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
                Vec3 side = new Vec3(-flat.z, 0, flat.x);
                for (int i = -2; i <= 2; i++) bolt(sl, p, stack, m, eye.add(look.scale(30)).add(side.scale(i * 3.0)));
                muzzleFlash(sl, m, look, 0xFFE070, 1.2f);
                sound(sl, p, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 1.4f);
            }
            default -> {
                return fireSemi(sl, p, stack);
            }
        }
        return true;
    }

    private void firebomb(ServerLevel sl, Player p, ItemStack stack) {
        if (p.getCooldowns().isOnCooldown(this)) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_HEAT, Math.min(99, tag.getInt(TAG_HEAT) + 30));
        Vec3 m = muzzle(p);
        SiegeProjectile b = SiegeProjectile.create(sl, ProjKind.NAPALM, p, m).payload(dmg(stack) * 4f, Element.FIRE).area(3f);
        b.onImpact = (pr, at) -> HazardZone.spawn(sl, HazardZone.Kind.FIRE, at, 3.5f, 120, 2f * Arsenal.damageMult(stack), p, false);
        b.setDeltaMovement(p.getLookAngle().scale(1.0).add(0, 0.25, 0));
        b.life = 60;
        b.fire(sl);
        sound(sl, p, SoundEvents.FIRECHARGE_USE, 1f, 0.7f);
        p.getCooldowns().addCooldown(this, 160);
    }

    /** Can a cone, beam or chain from {@code from} reach {@code e} without passing through a block? */
    static boolean reach(ServerLevel sl, Entity shooter, Vec3 from, LivingEntity e) {
        return com.lastbastion.item.weapon.MeleeCombat.sight(sl, shooter, from, e.position().add(0, e.getBbHeight() * 0.5, 0));
    }

    /** Enemies along a ray within {@code width} blocks and in sight, nearest first. */
    static List<LivingEntity> lineTargets(ServerLevel sl, Entity shooter, Vec3 from, Vec3 dir, double range, double width) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, from.add(dir.scale(range / 2)), range / 2 + 2)) {
            Vec3 rel = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            double along = rel.dot(dir);
            if (along < 0 || along > range || rel.subtract(dir.scale(along)).length() > width + e.getBbWidth() * 0.5) continue;
            if (!reach(sl, shooter, from, e)) continue;
            out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(shooter)));
        return out;
    }

    static void muzzleFlash(ServerLevel sl, Vec3 m, Vec3 look, int color, float size) {
        Vfx.flash(sl, m.add(look.scale(0.15)), color, 0.45f * size, 3);
        Vfx.spray(sl, FxStyle.SPARK, m, 0xFFFFFF, color, 0.06f, Math.round(3 * size), 0.25f * size, 0f);
    }

    public static Vec3 muzzle(Entity e) {
        Vec3 look = e.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        return e.getEyePosition().add(look.scale(0.9)).add(right.scale(0.3)).add(0, -0.25, 0);
    }

    private static void sound(ServerLevel sl, Entity e, SoundEvent s, float v, float p) {
        sl.playSound(null, e.blockPosition(), s, SoundSource.PLAYERS, v, p);
    }

    /** Client-side recoil and first-person kick for the local player's shot. */
    private void clientShot(float strength) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lastbastion.client.render.weapon.GunAnimations.shot(kind, strength));
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        CompoundTag tag = stack.getTag();
        if (kind.usesHeat()) {
            if (tag == null) return;
            boolean firing = entity instanceof LivingEntity le && le.isUsingItem() && le.getUseItem() == stack;
            int heat = tag.getInt(TAG_HEAT);
            if (!firing && heat > 0) {
                heat = Math.max(0, heat - (tag.getBoolean(TAG_OVERHEAT) ? 2 : 3));
                tag.putInt(TAG_HEAT, heat);
                if (heat == 0) tag.putBoolean(TAG_OVERHEAT, false);
            }
            return;
        }
        if (tag != null && tag.getLong(TAG_RELOAD) > 0 && level.getGameTime() >= tag.getLong(TAG_RELOAD)) {
            tag.remove(TAG_RELOAD);
            setAmmo(stack, magSize(stack));
            level.playSound(null, entity.blockPosition(), kind == GunKind.THUNDERBOOM ? SoundEvents.CROSSBOW_LOADING_END : SoundEvents.ARMOR_EQUIP_IRON,
                    SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    // ------------------------------------------------------------------ bar: ammo / heat / reload

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (kind.usesHeat()) return stack.hasTag() && stack.getTag().getInt(TAG_HEAT) > 0;
        return kind.mag > 0 && (reloading(stack) || ammo(stack) < magSize(stack));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (kind.usesHeat()) return Math.round(13f * (stack.hasTag() ? stack.getTag().getInt(TAG_HEAT) : 0) / 100f);
        if (reloading(stack)) return 0;
        return Math.round(13f * ammo(stack) / Math.max(1, magSize(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (kind.usesHeat()) return stack.hasTag() && stack.getTag().getBoolean(TAG_OVERHEAT) ? 0xFF2020 : 0xFFA020;
        return ammo(stack) == 0 ? 0xFF4040 : kind.color;
    }

    // ------------------------------------------------------------------ tooltip

    @Override
    public Component getName(ItemStack stack) {
        int mk = Arsenal.mk(stack);
        Component base = Component.translatable(getDescriptionId(stack));
        if (mk <= 1) return base;
        return Component.empty().append(base).append(Component.literal(" Mk " + Arsenal.roman(mk)).withStyle(Arsenal.mkColor(mk)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(String.format("Damage %.1f   Mk %s", dmg(stack), Arsenal.roman(Arsenal.mk(stack)))).withStyle(ChatFormatting.DARK_AQUA));
        if (kind.mag > 0) {
            tip.add(Component.literal("Magazine " + ammo(stack) + "/" + magSize(stack) + "   Reload " + String.format("%.1fs", reloadTicks(stack) / 20f)
                    + (reloading(stack) ? "  (reloading)" : "")).withStyle(ChatFormatting.DARK_GRAY));
        }
        tip.add(Component.literal("◆ " + kind.alt).withStyle(s -> s.withColor(kind.color)));
        tip.add(Component.literal("  " + kind.altDesc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Mk III: larger magazine.  Mk V: faster reload.").withStyle(ChatFormatting.DARK_GRAY));
        if (kind.mag > 0) tip.add(Component.literal("Reload: R (rebindable)").withStyle(ChatFormatting.DARK_GRAY));
    }

    // ------------------------------------------------------------------ client

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.lastbastion.client.render.weapon.GunClientExt.INSTANCE);
    }
}
