package com.tokyoghoul.player;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.combat.FighterState;
import com.tokyoghoul.combat.Fighters;
import com.tokyoghoul.combat.Fx;
import com.tokyoghoul.data.CharacterDef;
import com.tokyoghoul.data.Characters;
import com.tokyoghoul.move.Anims;
import com.tokyoghoul.move.MoveDef;
import com.tokyoghoul.move.MoveRunner;
import com.tokyoghoul.network.Packets.Action;
import com.tokyoghoul.network.Sync;
import com.tokyoghoul.registry.TGItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * What each key does on the server, mirroring Jujutsu Craft's key procedures:
 * R = KeyChangeTechnique (cycle, skipping locked moves; shift reverses), Z = KeyStartTechnique (press
 * starts, holding keeps barrages going), X = reset selection, C = kagune out/in, G = guard,
 * B = backstep/dodge, M = Rc regeneration (JJK's reverse cursed technique).
 */
public final class PlayerActions {
    private PlayerActions() {
    }

    public static void key(ServerPlayer p, Action a) {
        TGPlayerData d = PlayerCaps.get(p);
        FighterState s = d.state;
        CharacterDef def = d.def();
        switch (a) {
            case CYCLE_NEXT, CYCLE_PREV -> {
                if (def == null) {
                    p.displayClientMessage(Component.translatable("msg.tokyoghoul.no_character"), true);
                    return;
                }
                cycle(p, d, def, a == Action.CYCLE_NEXT ? 1 : -1);
            }
            case RESET -> {
                d.selected = 0;
                announce(p, d, def);
            }
            case USE_PRESS -> {
                if (def == null) {
                    p.displayClientMessage(Component.translatable("msg.tokyoghoul.no_character"), true);
                    return;
                }
                List<MoveDef> rot = def.rotation();
                MoveDef m = rot.get(Math.max(0, Math.min(rot.size() - 1, d.selected)));
                MoveRunner.Fail f = MoveRunner.start(p, m, null);
                if (f != MoveRunner.Fail.NONE && f != MoveRunner.Fail.BUSY) p.displayClientMessage(MoveRunner.failMessage(f, m), true);
            }
            case USE_RELEASE -> s.holding = false;
            case TOGGLE_KAGUNE -> {
                if (def == null || !def.hasKagune()) return;
                s.kaguneToggled = !s.kaguneToggled;
                if (s.kaguneToggled) {
                    Fx.kaguneBurst(p.level(), p.position().add(0, 1.1, 0), def.kaguneColor);
                    Fx.sound(p.level(), p.position(), SoundEvents.SLIME_SQUISH, 1f, 0.6f);
                } else {
                    s.kaguneTicks = 0;
                }
                Sync.visual(p);
            }
            case GUARD_PRESS -> {
                if (s.busy() || s.stun > 0) return;
                s.guarding = true;
                Sync.anim(p, def != null && def.kagune == com.tokyoghoul.data.KaguneType.KOUKAKU ? Anims.K_SHIELD : Anims.GUARD);
                Sync.visual(p);
            }
            case GUARD_RELEASE -> {
                if (s.guarding) {
                    s.guarding = false;
                    Sync.anim(p, Anims.NONE);
                    Sync.visual(p);
                }
            }
            case DODGE -> dodge(p, d, s);
            case REGEN_PRESS -> {
                if (s.busy() || s.stun > 0) return;
                regenerating(p, true);
                Sync.anim(p, Anims.REGEN);
            }
            case REGEN_RELEASE -> {
                regenerating(p, false);
                Sync.anim(p, Anims.NONE);
            }
        }
    }

    private static void regenerating(ServerPlayer p, boolean on) {
        p.getPersistentData().putBoolean(TokyoGhoul.MODID + ":regen", on);
    }

    public static boolean isRegenerating(ServerPlayer p) {
        return p.getPersistentData().getBoolean(TokyoGhoul.MODID + ":regen");
    }

    /** JJK's backstep: a quick hop with a few invulnerable frames, costs a little Rc. */
    private static void dodge(ServerPlayer p, TGPlayerData d, FighterState s) {
        long now = p.level().getGameTime();
        if (s.busy() || s.stun > 0 || p.getPersistentData().getLong(TokyoGhoul.MODID + ":dodge") > now) return;
        if (!Fighters.spendRc(p, 8)) {
            p.displayClientMessage(Component.translatable("msg.tokyoghoul.no_rc"), true);
            return;
        }
        p.getPersistentData().putLong(TokyoGhoul.MODID + ":dodge", now + 16);
        Vec3 move = new Vec3(p.xxa, 0, p.zza);
        Vec3 dir;
        if (move.lengthSqr() < 0.01) {
            dir = p.getLookAngle().multiply(-1, 0, -1);
        } else {
            float yaw = p.getYRot() * ((float) Math.PI / 180f);
            dir = new Vec3(move.x * Math.cos(yaw) - move.z * Math.sin(yaw), 0, move.z * Math.cos(yaw) + move.x * Math.sin(yaw));
        }
        dir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, -1) : dir.normalize();
        p.setDeltaMovement(dir.x * 1.1, 0.3, dir.z * 1.1);
        p.hurtMarked = true;
        s.dodgeTicks = 8;
        Sync.anim(p, Anims.DODGE);
        Fx.particles(p.level(), ParticleTypes.CLOUD, p.position(), 6, 0.3, 0.02);
        Sync.playerData(p);
    }

    /** R: next/previous unlocked move (locked ones are skipped, like JJK's "-----" entries). */
    public static void cycle(ServerPlayer p, TGPlayerData d, CharacterDef def, int dir) {
        List<MoveDef> rot = def.rotation();
        int n = rot.size();
        int i = Math.floorMod(d.selected, n);
        boolean creative = p.getAbilities().instabuild;
        for (int k = 0; k < n; k++) {
            i = Math.floorMod(i + dir, n);
            MoveDef m = rot.get(i);
            if (creative || (m.minRank <= d.level && (!m.needsKakuja || d.kakuja >= 100))) break;
        }
        d.selected = i;
        announce(p, d, def);
    }

    private static void announce(ServerPlayer p, TGPlayerData d, CharacterDef def) {
        if (def != null) {
            MoveDef m = def.rotation().get(Math.max(0, Math.min(def.rotation().size() - 1, d.selected)));
            Component c = m.title().withStyle(m.ultimate ? ChatFormatting.DARK_RED : ChatFormatting.WHITE);
            if (m.cost > 0) c = c.copy().append(Component.literal("  [" + m.cost + " Rc]").withStyle(ChatFormatting.GRAY));
            p.displayClientMessage(c, true);
        }
        Sync.playerData(p);
    }

    /** The Ghoul Registry book: become a character (gear, mask and full Rc, like JJK's Select*Procedure). */
    public static void select(ServerPlayer p, String id) {
        CharacterDef def = Characters.get(id);
        if (def == null || !def.selectable) return;
        if (!p.getInventory().contains(new ItemStack(TGItems.GHOUL_REGISTRY.get())) && !p.getAbilities().instabuild) return;
        TGPlayerData d = PlayerCaps.get(p);
        boolean changed = !def.id.equals(d.character);
        d.character = def.id;
        d.selected = 0;
        d.state.clearMove();
        d.state.form = null;
        d.state.formTicks = 0;
        d.state.kaguneToggled = false;
        d.state.kaguneTicks = 0;
        d.rc = d.maxRc();
        if (changed) {
            give(p, def.quinque);
            give(p, def.quinque2);
            if (!def.mask.isEmpty()) {
                Item mask = item(def.mask);
                if (mask != null) {
                    if (p.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(mask));
                    else give(p, def.mask);
                }
            }
            if (def.faction == com.tokyoghoul.data.Faction.CCG && p.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TGItems.CCG_COAT.get()));
            }
        }
        PlayerEvents.applyAttributes(p, d);
        p.sendSystemMessage(Component.translatable("msg.tokyoghoul.selected", def.title().withStyle(ChatFormatting.RED),
                Component.translatable(def.faction.key())));
        Fx.sound(p.level(), p.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 0.8f);
        Fx.particles(p.level(), Fx.dust(def.hasKagune() ? def.kaguneColor : 0xE8E8E8, 1.6f), p.position().add(0, 1, 0), 30, 0.5, 0.05);
        Sync.playerData(p);
        Sync.visual(p);
    }

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(TokyoGhoul.MODID, id));
    }

    private static void give(ServerPlayer p, String id) {
        if (id == null || id.isEmpty()) return;
        Item it = item(id);
        if (it == null || p.getInventory().contains(new ItemStack(it))) return;
        ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(it));
    }
}
