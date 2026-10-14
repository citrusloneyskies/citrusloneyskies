package com.lastbastion.item.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Shared upgrade track for every arsenal weapon (melee and firearms): Mk I to Mk V, bought at the
 * Siege Core. Each Mk raises damage; Mk III awakens a weapon's second passive and Mk V ascends its
 * special. Stored on the stack so the weapon keeps its Mk when traded or dropped.
 */
public final class Arsenal {
    public static final int MAX_MK = 5;
    public static final String TAG_MK = "lbMk";
    private static final float[] DAMAGE = {1.0f, 1.25f, 1.55f, 1.9f, 2.4f};
    private static final float[] COST = {0.4f, 0.7f, 1.1f, 1.6f};
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

    private Arsenal() {
    }

    /** Implemented by items that take part in the Mk upgrade track. */
    public interface Upgradeable {
        /** Shop price of the Mk I weapon; upgrade costs derive from it. */
        int baseCost();

        String displayTitle();
    }

    public static int mk(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null ? 1 : Math.max(1, Math.min(MAX_MK, t.getInt(TAG_MK) == 0 ? 1 : t.getInt(TAG_MK)));
    }

    public static void setMk(ItemStack stack, int mk) {
        stack.getOrCreateTag().putInt(TAG_MK, Math.max(1, Math.min(MAX_MK, mk)));
    }

    public static float damageMult(int mk) {
        return DAMAGE[Math.max(1, Math.min(MAX_MK, mk)) - 1];
    }

    public static float damageMult(ItemStack stack) {
        return damageMult(mk(stack));
    }

    /** Credits to raise {@code stack} by one Mk, or -1 at Mk V / for non-arsenal items. */
    public static int upgradeCost(ItemStack stack) {
        if (!(stack.getItem() instanceof Upgradeable u)) return -1;
        int mk = mk(stack);
        if (mk >= MAX_MK) return -1;
        return Math.round(u.baseCost() * COST[mk - 1] / 10f) * 10;
    }

    public static boolean awakened(ItemStack stack) {
        return mk(stack) >= 3;
    }

    public static boolean ascended(ItemStack stack) {
        return mk(stack) >= MAX_MK;
    }

    public static String roman(int mk) {
        return ROMAN[Math.max(1, Math.min(MAX_MK, mk)) - 1];
    }

    public static ChatFormatting tierColor(int tier) {
        return switch (tier) {
            case 1 -> ChatFormatting.WHITE;
            case 2 -> ChatFormatting.GREEN;
            case 3 -> ChatFormatting.AQUA;
            case 4 -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GOLD;
        };
    }

    public static ChatFormatting mkColor(int mk) {
        return switch (mk) {
            case 1 -> ChatFormatting.GRAY;
            case 2 -> ChatFormatting.GREEN;
            case 3 -> ChatFormatting.AQUA;
            case 4 -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GOLD;
        };
    }
}
