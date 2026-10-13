package com.lastbastion.item;

import com.lastbastion.combat.Element;
import com.lastbastion.combat.Fx;
import com.lastbastion.combat.SiegeDamage;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Core-shop firearms. Automatic weapons fire while right-click is held (with heat management);
 * semi-automatic weapons fire per click with a cooldown; the railgun charges and fires on release.
 */
public class GunItem extends Item {
    public enum GunKind {
        RAVAGER("M60 'Ravager' Machine Gun", true, 2, 4.5f, "Hold to fire. 30 rounds/sec of pain. Overheats - let it cool."),
        THUNDERBOOM("'Thunderboom' Scattergun", false, 16, 3.2f, "8 pellets per blast with heavy knockback."),
        HELLFIRE("'Hellfire' Rocket Launcher", false, 40, 30f, "High-explosive rockets with a 4-block blast."),
        STARPIERCER("'Starpiercer' Railgun", false, 60, 90f, "Hold to charge (1s), release to fire a beam that pierces every enemy in a line."),
        DRAGONBREATH("'Dragon's Breath' Flamethrower", true, 2, 2.2f, "Hold to spray a 7-block cone of fire."),
        STORMFIST("'Stormfist' Tesla Gauntlet", false, 22, 12f, "Chain lightning that arcs through 5 enemies."),
        GLACIER("'Glacier' Cryo Cannon", false, 45, 14f, "Frost orb that freezes everything in a 3.5-block burst."),
        SUNLANCE("'Sunlance' Holy Repeater", false, 8, 7f, "Rapid holy bolts. Devastating against the undead.");

        public final String title;
        public final boolean automatic;
        public final int cooldown;
        public final float damage;
        public final String desc;

        GunKind(String title, boolean automatic, int cooldown, float damage, String desc) {
            this.title = title;
            this.automatic = automatic;
            this.cooldown = cooldown;
            this.damage = damage;
            this.desc = desc;
        }
    }

    public final GunKind kind;

    public GunItem(Properties p, GunKind kind) {
        super(p.stacksTo(1).rarity(Rarity.RARE));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind.automatic || kind == GunKind.STARPIERCER) {
            if (kind == GunKind.RAVAGER && stack.getOrCreateTag().getBoolean("overheated")) return InteractionResultHolder.fail(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level instanceof ServerLevel sl) {
            fireSemi(sl, player, stack);
            player.getCooldowns().addCooldown(this, kind.cooldown);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == GunKind.STARPIERCER ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl)) return;
        int used = getUseDuration(stack) - remaining;
        if (kind == GunKind.STARPIERCER) {
            if (used == 20) sl.playSound(null, user.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 2f);
            if (used < 20 && used % 4 == 0) {
                Vec3 m = muzzle(user);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.x, m.y, m.z, 3, 0.1, 0.1, 0.1, 0.05);
            }
            return;
        }
        if (used % kind.cooldown != 0) return;
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        if (kind == GunKind.RAVAGER) {
            var tag = stack.getOrCreateTag();
            int heat = tag.getInt("heat") + 4;
            tag.putInt("heat", heat);
            if (heat >= 100) {
                tag.putBoolean("overheated", true);
                user.stopUsingItem();
                sl.playSound(null, user.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 0.8f);
                return;
            }
            Hitscan.fire(sl, user, eye.add(0, -0.15, 0), Hitscan.spreadDir(user, look, 0.03), 48, kind.damage, Element.PHYSICAL, Fx.dust(0xFFD070, 0.5f));
            sl.playSound(null, user.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.5f, 1.8f + sl.random.nextFloat() * 0.2f);
            Vec3 m = muzzle(user);
            sl.sendParticles(ParticleTypes.SMOKE, m.x, m.y, m.z, 1, 0.02, 0.02, 0.02, 0.01);
        } else if (kind == GunKind.DRAGONBREATH) {
            Vec3 m = muzzle(user);
            Fx.cone(sl, m, look, 7, 14, ParticleTypes.FLAME, 14);
            for (LivingEntity e : SiegeDamage.enemiesAround(sl, m.add(look.scale(3.5)), 4.5)) {
                if (e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(m).normalize().dot(look) < 0.8) continue;
                SiegeDamage.deal(e, user, user, kind.damage, Element.FIRE, false);
                e.setSecondsOnFire(4);
                if (sl.random.nextInt(4) == 0) SiegeDamage.applyStacking(e, ModEffects.SCORCHED.get(), 60, 1);
            }
            if (used % 8 == 0) sl.playSound(null, user.blockPosition(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (kind != GunKind.STARPIERCER || !(level instanceof ServerLevel sl)) return;
        int used = getUseDuration(stack) - remaining;
        if (used < 20) return;
        Vec3 from = user.getEyePosition().add(0, -0.1, 0);
        Vec3 dir = user.getLookAngle();
        Vec3 end = from.add(dir.scale(64));
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity e : SiegeDamage.enemiesAround(sl, from.add(dir.scale(32)), 33)) {
            Vec3 rel = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            double along = rel.dot(dir);
            if (along < 0 || rel.subtract(dir.scale(along)).lengthSqr() > 1.5) continue;
            hit.add(e);
        }
        for (LivingEntity e : hit) SiegeDamage.deal(e, user, user, kind.damage, Element.ARCANE, true);
        Fx.line(sl, from.add(dir), end, Fx.dust(0x7FE8FF, 1.6f), 0.25);
        Fx.line(sl, from.add(dir), end, ParticleTypes.END_ROD, 1.2);
        sl.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1f, 1.6f);
        if (user instanceof Player p) p.getCooldowns().addCooldown(this, kind.cooldown);
        user.push(-dir.x * 0.4, 0.1, -dir.z * 0.4);
    }

    private void fireSemi(ServerLevel sl, Player p, ItemStack stack) {
        Vec3 eye = p.getEyePosition().add(0, -0.1, 0);
        Vec3 look = p.getLookAngle();
        Vec3 m = muzzle(p);
        switch (kind) {
            case THUNDERBOOM -> {
                for (int i = 0; i < 8; i++) {
                    LivingEntity hit = Hitscan.fire(sl, p, eye, Hitscan.spreadDir(p, look, 0.09), 20, kind.damage, Element.PHYSICAL, Fx.dust(0xFFB060, 0.4f));
                    if (hit != null) SiegeDamage.push(hit, p.position(), 0.35, 0.1);
                }
                sound(sl, p, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.6f);
                sl.sendParticles(ParticleTypes.POOF, m.x, m.y, m.z, 6, 0.1, 0.1, 0.1, 0.05);
                p.push(-look.x * 0.3, 0.05, -look.z * 0.3);
            }
            case HELLFIRE -> {
                SiegeProjectile r = SiegeProjectile.create(sl, ProjKind.ROCKET, p, m).payload(kind.damage, Element.EXPLOSIVE).area(4f);
                r.knockback = 0.8f;
                r.aim(eye.add(look.scale(50)), 1.6f).scale(1.3f).fire(sl);
                sound(sl, p, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.6f);
            }
            case STORMFIST -> {
                LivingEntity first = Hitscan.fire(sl, p, eye, look, 24, kind.damage, Element.LIGHTNING, Fx.dust(0xF4F06A, 0.6f));
                if (first != null) {
                    Set<LivingEntity> hit = new HashSet<>();
                    hit.add(first);
                    LivingEntity cur = first;
                    Fx.bolt(sl, m, first.position().add(0, first.getBbHeight() * 0.5, 0), 0xF4F06A, 1f);
                    for (int i = 0; i < 4; i++) {
                        LivingEntity c = cur;
                        LivingEntity next = SiegeDamage.enemiesAround(sl, cur.position(), 6).stream().filter(e -> !hit.contains(e))
                                .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
                        if (next == null) break;
                        Fx.bolt(sl, cur.position().add(0, cur.getBbHeight() * 0.5, 0), next.position().add(0, next.getBbHeight() * 0.5, 0), 0xF4F06A, 1f);
                        SiegeDamage.deal(next, p, p, kind.damage * 0.8f, Element.LIGHTNING, false);
                        SiegeDamage.apply(next, ModEffects.SHOCKED.get(), 15, 0);
                        hit.add(next);
                        cur = next;
                    }
                    SiegeDamage.apply(first, ModEffects.SHOCKED.get(), 15, 0);
                }
                sound(sl, p, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 1.6f);
            }
            case GLACIER -> {
                SiegeProjectile f = SiegeProjectile.create(sl, ProjKind.FROST_SHARD, p, m).payload(kind.damage, Element.ICE).area(3.5f);
                f.onEntityHit = (pr, e) -> SiegeDamage.apply(e, ModEffects.FROZEN.get(), 60, 0);
                f.aim(eye.add(look.scale(40)), 1.4f).scale(2.2f).fire(sl);
                sound(sl, p, SoundEvents.SNOW_GOLEM_SHOOT, 1.2f, 0.5f);
            }
            case SUNLANCE -> {
                SiegeProjectile h = SiegeProjectile.create(sl, ProjKind.HOLY_BOLT, p, m).payload(kind.damage, Element.HOLY)
                        .status(ModEffects.PURIFIED, 60, 0);
                h.aim(eye.add(look.scale(40)), 2.2f).fire(sl);
                sound(sl, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.6f);
            }
            default -> {
            }
        }
    }

    private static Vec3 muzzle(Entity e) {
        Vec3 look = e.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        return e.getEyePosition().add(look.scale(0.8)).add(right.scale(0.3)).add(0, -0.25, 0);
    }

    private static void sound(ServerLevel sl, Entity e, SoundEvent s, float v, float p) {
        sl.playSound(null, e.blockPosition(), s, SoundSource.PLAYERS, v, p);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (kind != GunKind.RAVAGER || level.isClientSide) return;
        boolean firing = entity instanceof LivingEntity le && le.isUsingItem() && le.getUseItem() == stack;
        var tag = stack.getOrCreateTag();
        int heat = tag.getInt("heat");
        if (!firing && heat > 0) {
            heat = Math.max(0, heat - (tag.getBoolean("overheated") ? 2 : 3));
            tag.putInt("heat", heat);
            if (heat == 0) tag.putBoolean("overheated", false);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return kind == GunKind.RAVAGER && stack.hasTag() && stack.getTag().getInt("heat") > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * (stack.hasTag() ? stack.getTag().getInt("heat") : 0) / 100f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("overheated") ? 0xFF2020 : 0xFFA020;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Damage: " + kind.damage).withStyle(ChatFormatting.DARK_AQUA));
    }
}
