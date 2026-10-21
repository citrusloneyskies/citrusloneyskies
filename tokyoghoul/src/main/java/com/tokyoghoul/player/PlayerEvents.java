package com.tokyoghoul.player;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.combat.Combat;
import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.command.TGCommand;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.item.GhoulFoodItem;
import com.tokyoghoul.item.QuinqueItem;
import com.tokyoghoul.move.MoveRunner;
import com.tokyoghoul.network.Sync;
import com.tokyoghoul.network.TGNet;
import com.tokyoghoul.registry.TGEffects;
import com.tokyoghoul.registry.TGItems;
import com.tokyoghoul.world.Encounters;
import com.tokyoghoul.world.TGGameRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/** Forge-bus events: player upkeep, starter kit, diet, ghoul toughness, kills and commands. */
public final class PlayerEvents {
    private static final UUID HEALTH_ID = UUID.fromString("6f0d3c1e-0b5a-4d4f-9c1e-7a1b2c3d4e5f");
    private static final String FOOD_KEY = TokyoGhoul.MODID + ":food_before";

    private PlayerEvents() {
    }

    /** Max health grows with rank, plus the character's bonus. */
    public static void applyAttributes(Player p, TGPlayerData d) {
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        hp.removeModifier(HEALTH_ID);
        CharacterDef def = d.def();
        double bonus = (def == null ? 0 : def.hpBonus) + 4.0 * (d.level - 1);
        if (bonus > 0) hp.addPermanentModifier(new AttributeModifier(HEALTH_ID, "Tokyo Ghoul rank", bonus, AttributeModifier.Operation.ADDITION));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide || !(e.player instanceof ServerPlayer p)) return;
        com.tokyoghoul.util.Safe.run("PlayerEvents.tick", () -> tick(p));
    }

    private static void tick(ServerPlayer p) {
        TGPlayerData d = PlayerCaps.get(p);
        FighterState s = d.state;
        int flagsBefore = Sync.playerFlags(p);
        MoveRunner.tick(p);
        CharacterDef def = d.def();
        double max = d.maxRc();
        boolean ghoul = def != null && def.race.ghoulBody;
        boolean suppressed = p.hasEffect(TGEffects.SUPPRESSED.get());
        // Rc regeneration (slower while starving)
        double regen = max * 0.0018;
        if (ghoul && p.getFoodData().getFoodLevel() < 6) regen *= 0.3;
        if (suppressed) regen = 0;
        if (s.kaguneToggled && !s.busy()) regen -= max * 0.0004;
        d.rc = Math.max(0, Math.min(max, d.rc + regen));
        if (s.kaguneToggled && d.rc <= 0) {
            s.kaguneToggled = false;
            Sync.visual(p);
        }
        // ghoul passive regeneration
        if (ghoul && !suppressed && p.tickCount % 20 == 0 && p.getHealth() < p.getMaxHealth() && d.rc > max * 0.1
                && p.getFoodData().getFoodLevel() >= 6) {
            p.heal(1f);
            d.rc -= 1.5;
        }
        // M: regeneration burst (JJK's reverse cursed technique)
        if (PlayerActions.isRegenerating(p) && !suppressed) {
            if (p.getHealth() < p.getMaxHealth() && d.rc >= 1.2 && !s.busy() && s.stun <= 0) {
                p.heal(0.35f);
                d.rc -= 1.2;
                if (p.tickCount % 4 == 0) Fx.particles(p.level(), Fx.dust(def != null && def.hasKagune() ? def.kaguneColor : 0x60FF90, 1.0f),
                        p.position().add(0, 1, 0), 3, 0.4, 0.02);
            }
        }
        // G: guard holds slow you and cost a trickle of Rc
        if (s.guarding) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 2, false, false));
            if (p.tickCount % 10 == 0 && !Fighters.spendRc(p, 0.5)) {
                s.guarding = false;
                Sync.anim(p, com.tokyoghoul.move.Anims.NONE);
            }
        }
        if (d.satiated) {
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(Math.max(p.getFoodData().getSaturationLevel(), 5f));
        }
        if (Sync.playerFlags(p) != flagsBefore) Sync.visual(p);
        if (p.tickCount % 10 == 0) Sync.playerData(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        TGPlayerData d = PlayerCaps.get(p);
        if (!d.starterGiven && p.level().getGameRules().getBoolean(TGGameRules.STARTER_KIT)) {
            d.starterGiven = true;
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(TGItems.GHOUL_REGISTRY.get()));
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(TGItems.THERMOS.get()));
            p.sendSystemMessage(Component.translatable("msg.tokyoghoul.welcome").withStyle(ChatFormatting.DARK_RED));
        }
        if (d.rc <= 0) d.rc = d.maxRc();
        applyAttributes(p, d);
        Sync.playerData(p);
        Sync.visual(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        TGPlayerData d = PlayerCaps.get(p);
        d.state.clearMove();
        d.state.form = null;
        d.state.formTicks = 0;
        d.state.kaguneToggled = false;
        applyAttributes(p, d);
        p.setHealth(p.getMaxHealth());
        Sync.playerData(p);
        Sync.visual(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Sync.playerData(p);
            Sync.visual(p);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        PlayerCaps.clone(e);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof Player other && e.getEntity() instanceof ServerPlayer viewer) {
            Sync.send(PacketDistributor.PLAYER.with(() -> viewer), Sync.visualPacket(other));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        com.tokyoghoul.util.Safe.run("PlayerEvents.death", () -> {
            if (e.getSource().getEntity() instanceof LivingEntity killer) Progression.onKill(victim, killer);
            FighterState s = Fighters.state(victim);
            if (s != null && s.grabbed != null) s.grabbed.setNoGravity(false);
        });
    }

    /** Ghoul skin shrugs off ordinary weapons; quinques bite harder (canon: only Rc cells hurt a ghoul). */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide) return;
        var src = e.getSource();
        if (src.is(Combat.KAGUNE) || src.is(Combat.QUINQUE) || src.is(Combat.STRIKE)) return;
        if (!(src.getEntity() instanceof LivingEntity attacker)) return;
        boolean quinque = attacker.getMainHandItem().getItem() instanceof QuinqueItem && src.getDirectEntity() == attacker;
        if (Fighters.ghoulBody(target)) {
            if (quinque) e.setAmount(e.getAmount() * 1.25f);
            else if (!src.is(DamageTypes.ON_FIRE) && !src.is(DamageTypes.IN_FIRE) && !src.is(DamageTypes.MAGIC)) e.setAmount(e.getAmount() * 0.5f);
        }
        // fighters' own guard also blocks plain melee
        FighterState ts = Fighters.state(target);
        if (ts != null && ts.guardingNow()) {
            e.setAmount(e.getAmount() * 0.3f);
            Fx.particles(target.level(), ParticleTypes.ENCHANTED_HIT, target.getBoundingBox().getCenter(), 6, 0.3, 0.1);
        }
        if (ts != null && ts.dodgeTicks > 0) e.setCanceled(true);
        e.setAmount(e.getAmount() * Fighters.armorMult(target));
    }

    /** Ghoul diet: ordinary food tastes like clay and gives nothing back. */
    @SubscribeEvent
    public static void onEatStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof Player p && !p.level().isClientSide && e.getItem().isEdible()) {
            p.getPersistentData().putInt(FOOD_KEY, p.getFoodData().getFoodLevel());
            p.getPersistentData().putFloat(FOOD_KEY + "_sat", p.getFoodData().getSaturationLevel());
        }
    }

    @SubscribeEvent
    public static void onEatFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.getItem().isEdible()) return;
        if (!p.level().getGameRules().getBoolean(TGGameRules.GHOUL_DIET) || !Fighters.ghoulBody(p) || GhoulFoodItem.ghoulSafe(e.getItem())) return;
        if (!p.getPersistentData().contains(FOOD_KEY)) return;
        p.getFoodData().setFoodLevel(p.getPersistentData().getInt(FOOD_KEY));
        p.getFoodData().setSaturation(p.getPersistentData().getFloat(FOOD_KEY + "_sat"));
        p.removeEffect(MobEffects.REGENERATION);
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        p.displayClientMessage(Component.translatable("msg.tokyoghoul.disgusting").withStyle(ChatFormatting.DARK_GREEN), true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) com.tokyoghoul.util.Safe.run("Encounters", () -> Encounters.tick(e.getServer()));
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        TGCommand.register(e.getDispatcher());
    }
}
