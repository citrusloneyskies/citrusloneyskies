package com.lastbastion.item.weapon;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A melee weapon from the arsenal. Left-click swings run through {@link MeleeCombat} (combos, cleave,
 * crits, finishers); right-click spends Fury on the weapon's special. Rendered in 3D by the client
 * weapon renderer.
 */
public class MeleeWeaponItem extends Item implements Arsenal.Upgradeable {
    public static final String TAG_FURY = "lbFury";
    public static final String TAG_FURY_TIME = "lbFuryT";
    public static final String TAG_SOULS = "lbSouls";
    public static final int MAX_FURY = 100;
    public static final int MAX_SOULS = 20;

    public final WeaponKind kind;

    public MeleeWeaponItem(Properties p, WeaponKind kind) {
        super(p.stacksTo(1).rarity(kind.tier >= 4 ? Rarity.EPIC : kind.tier >= 3 ? Rarity.RARE : kind.tier >= 2 ? Rarity.UNCOMMON : Rarity.COMMON));
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

    // ------------------------------------------------------------------ stats

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        float dmg = kind.damage * Arsenal.damageMult(stack);
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", dmg - 1.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", kind.attacksPerSecond - 4.0,
                AttributeModifier.Operation.ADDITION));
        return b.build();
    }

    public static int fury(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null ? 0 : t.getInt(TAG_FURY);
    }

    public static void setFury(ItemStack stack, int v, long gameTime) {
        CompoundTag t = stack.getOrCreateTag();
        t.putInt(TAG_FURY, Math.max(0, Math.min(MAX_FURY, v)));
        t.putLong(TAG_FURY_TIME, gameTime);
    }

    public static int souls(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null ? 0 : t.getInt(TAG_SOULS);
    }

    public static void setSouls(ItemStack stack, int v) {
        stack.getOrCreateTag().putInt(TAG_SOULS, Math.max(0, Math.min(MAX_SOULS, v)));
    }

    /** True for specials that charge while right-click is held and fire on release. */
    public boolean charges() {
        return kind == WeaponKind.DAWNBREAKER;
    }

    // ------------------------------------------------------------------ actions

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (charges()) {
            if (!MeleeCombat.canAffordSpecial(player, stack, kind)) {
                if (level.isClientSide) MeleeCombat.notEnough(player, kind);
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            if (!MeleeCombat.special(sl, sp, stack, kind)) return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return charges() ? 72000 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level instanceof ServerLevel sl && user instanceof ServerPlayer sp) {
            MeleeCombat.chargeTick(sl, sp, stack, kind, getUseDuration(stack) - remaining);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (level instanceof ServerLevel sl && user instanceof ServerPlayer sp) {
            MeleeCombat.releaseCharge(sl, sp, stack, kind, getUseDuration(stack) - remaining);
        }
    }

    /** Vanilla attacks never reach the target directly; the swing goes through the combo system instead. */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (player instanceof ServerPlayer sp && player.level() instanceof ServerLevel sl) MeleeCombat.swing(sl, sp, false);
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;
        CompoundTag t = stack.getTag();
        if (t == null) return;
        int fury = t.getInt(TAG_FURY);
        if (fury > 0 && level.getGameTime() - t.getLong(TAG_FURY_TIME) > 160) {
            t.putInt(TAG_FURY, Math.max(0, fury - 2));
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    // ------------------------------------------------------------------ bar: Fury (souls for the Soulreaper)

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return kind == WeaponKind.SOULREAPER ? souls(stack) > 0 : fury(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (kind == WeaponKind.SOULREAPER) return Math.round(13f * souls(stack) / MAX_SOULS);
        return Math.round(13f * fury(stack) / MAX_FURY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (kind == WeaponKind.SOULREAPER) return 0x5AFFD8;
        return fury(stack) >= kind.specialFury && kind.specialFury > 0 ? kind.color : 0xB0B0B0;
    }

    // ------------------------------------------------------------------ tooltip

    @Override
    public Component getName(ItemStack stack) {
        int mk = Arsenal.mk(stack);
        Component base = Component.translatable(getDescriptionId(stack)).withStyle(Arsenal.tierColor(kind.tier));
        if (mk <= 1) return base;
        return Component.empty().append(base).append(Component.literal(" Mk " + Arsenal.roman(mk)).withStyle(Arsenal.mkColor(mk)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        int mk = Arsenal.mk(stack);
        tip.add(Component.literal(className(kind.weaponClass) + "  •  " + elementName(kind) + "  •  Mk " + Arsenal.roman(mk))
                .withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal(String.format("Reach %.1f   Cleave %d   Combo %d hits", kind.reach, kind.cleave, kind.combo.length))
                .withStyle(ChatFormatting.GRAY));
        tip.add(Component.empty());
        String cost = kind.specialFury > 0 ? kind.specialFury + " Fury, " : "";
        tip.add(Component.literal("◆ " + kind.special + " (Right-click, " + cost + kind.specialCooldown + "s)").withStyle(s -> s.withColor(kind.color)));
        tip.add(Component.literal("  " + kind.specialDesc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("◇ " + kind.passive).withStyle(ChatFormatting.YELLOW));
        tip.add(Component.literal("  " + kind.passiveDesc).withStyle(ChatFormatting.GRAY));
        boolean awake = mk >= 3;
        tip.add(Component.literal("◇ " + kind.awakened + (awake ? "" : " (Mk III)")).withStyle(awake ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("  " + kind.awakenedDesc).withStyle(awake ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
        boolean asc = mk >= Arsenal.MAX_MK;
        tip.add(Component.literal("✦ Ascended" + (asc ? "" : " (Mk V)")).withStyle(asc ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("  " + kind.ascendedDesc).withStyle(asc ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
        if (kind == WeaponKind.SOULREAPER) {
            tip.add(Component.literal("Souls: " + souls(stack) + "/" + MAX_SOULS).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tip.add(Component.literal("Fury: " + fury(stack) + "/" + MAX_FURY).withStyle(ChatFormatting.DARK_RED));
        }
        tip.add(Component.literal(kind.lore).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    static String className(WeaponKind.WeaponClass c) {
        return switch (c) {
            case LONGSWORD -> "Longsword";
            case DAGGER -> "Dagger";
            case AXE -> "Two-handed Axe";
            case KATANA -> "Katana";
            case SPEAR -> "Spear";
            case HAMMER -> "Warhammer";
            case GREATSWORD -> "Greatsword";
            case SCYTHE -> "Scythe";
        };
    }

    static String elementName(WeaponKind k) {
        String n = k.element.name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }

    // ------------------------------------------------------------------ client

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.lastbastion.client.render.weapon.WeaponClientExt.INSTANCE);
    }
}
