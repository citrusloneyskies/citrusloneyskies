package com.lastbastion.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lastbastion.LastBastion;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** One piece of a siege armour set: unbreakable, rendered with its own 3D worn model. */
public class SiegeArmorItem extends ArmorItem {
    private static final UUID[] SPEED_UUID = {UUID.fromString("6b1f3a52-3c5e-4d3a-9a6e-1a7b2c3d4e01"), UUID.fromString("6b1f3a52-3c5e-4d3a-9a6e-1a7b2c3d4e02"),
            UUID.fromString("6b1f3a52-3c5e-4d3a-9a6e-1a7b2c3d4e03"), UUID.fromString("6b1f3a52-3c5e-4d3a-9a6e-1a7b2c3d4e04")};

    public final ArmorSet set;

    public SiegeArmorItem(ArmorSet set, ArmorItem.Type type, Properties p) {
        super(new Material(set), type, p.stacksTo(1).rarity(set.tier >= 4 ? Rarity.EPIC : set.tier >= 3 ? Rarity.RARE : Rarity.UNCOMMON));
        this.set = set;
    }

    /** 0 = helmet ... 3 = boots. */
    public int slotIndex() {
        return switch (getType()) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
        };
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (slot != getEquipmentSlot() || set != ArmorSet.SCAVENGER) return base;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.putAll(base);
        b.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPEED_UUID[slotIndex()], "Scavenger speed", 0.025, AttributeModifier.Operation.MULTIPLY_BASE));
        return b.build();
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return LastBastion.MODID + ":dynamic/armor_" + set.id + "_" + ArmorSet.SLOT_IDS[slotIndex()];
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        try {
        int worn = 0;
        Player viewer = level != null && level.isClientSide ? clientPlayer() : null;
        if (viewer != null) worn = ArmorSets.piecesWorn(viewer, set);
        tip.add(Component.literal(set.title + " (" + worn + "/4)").withStyle(s -> s.withColor(set.color)));
        boolean active = worn == 4;
        tip.add(Component.literal("◆ Set bonus - " + set.bonus).withStyle(active ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tip.add(Component.literal("  " + set.bonusDesc).withStyle(active ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
        tip.add(Component.literal(set.lore).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } catch (Exception | LinkageError | StackOverflowError | AssertionError lbCrash) {
            com.lastbastion.util.Safe.report("SiegeArmorItem.appendHoverText", lbCrash);
        }
    }

    @Nullable
    private static Player clientPlayer() {
        return net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> net.minecraft.client.Minecraft.getInstance().player);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.lastbastion.client.render.armor.ArmorClientExt.INSTANCE);
    }

    /** Armour stats for one set; pieces are unbreakable (durability 0). */
    record Material(ArmorSet set) implements ArmorMaterial {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET -> set.defence[0];
                case CHESTPLATE -> set.defence[1];
                case LEGGINGS -> set.defence[2];
                case BOOTS -> set.defence[3];
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public SoundEvent getEquipSound() {
            return switch (set) {
                case SCAVENGER, SHADOWSTALKER -> SoundEvents.ARMOR_EQUIP_LEATHER;
                case ARCANIST -> SoundEvents.ARMOR_EQUIP_ELYTRA;
                case VANGUARD -> SoundEvents.ARMOR_EQUIP_IRON;
                case SUNFORGED -> SoundEvents.ARMOR_EQUIP_GOLD;
                case TITAN -> SoundEvents.ARMOR_EQUIP_NETHERITE;
            };
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return LastBastion.MODID + ":" + set.id;
        }

        @Override
        public float getToughness() {
            return set.toughness;
        }

        @Override
        public float getKnockbackResistance() {
            return set.knockbackResistance;
        }
    }
}
