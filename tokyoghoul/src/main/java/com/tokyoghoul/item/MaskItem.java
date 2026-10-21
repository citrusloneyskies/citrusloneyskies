package com.tokyoghoul.item;

import com.tokyoghoul.TokyoGhoul;
import com.tokyoghoul.data.Look;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ghoul masks (Uta's handiwork) and the CCG coat. Worn pieces with procedural 3D models; masks hide
 * the wearer's identity, so CCG investigators have a harder time recognising a masked ghoul.
 */
public class MaskItem extends ArmorItem {
    public final Look.Mask mask;
    public final int color, accent;

    static final ArmorMaterial CLOTH = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type t) {
            return 400;
        }

        @Override
        public int getDefenseForType(Type t) {
            return t == Type.CHESTPLATE ? 4 : 2;
        }

        @Override
        public int getEnchantmentValue() {
            return 12;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(net.minecraft.world.item.Items.LEATHER);
        }

        @Override
        public String getName() {
            return TokyoGhoul.MODID + ":mask";
        }

        @Override
        public float getToughness() {
            return 0;
        }

        @Override
        public float getKnockbackResistance() {
            return 0;
        }
    };

    public MaskItem(Look.Mask mask, int color, int accent) {
        this(mask, color, accent, Type.HELMET);
    }

    public MaskItem(Look.Mask mask, int color, int accent, Type type) {
        super(CLOTH, type, new Properties().stacksTo(1));
        this.mask = mask;
        this.color = color;
        this.accent = accent;
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return TokyoGhoul.MODID + ":dynamic/worn_" + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (getType() == Type.HELMET) tip.add(Component.translatable("tooltip.tokyoghoul.mask").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.tokyoghoul.client.render.WornClientExt.INSTANCE);
    }
}
