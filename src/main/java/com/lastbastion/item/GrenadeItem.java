package com.lastbastion.item;

import com.lastbastion.combat.Element;
import com.lastbastion.entity.projectile.HazardZone;
import com.lastbastion.entity.projectile.SiegeProjectile;
import com.lastbastion.entity.projectile.SiegeProjectile.ProjKind;
import com.lastbastion.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class GrenadeItem extends Item {
    public enum Kind {
        FRAG("Frag Grenade", "25 explosive damage in a 4-block radius."),
        INCENDIARY("Incendiary Grenade", "Blankets an area in fire for 6 seconds."),
        CRYO("Cryo Grenade", "Freezes everything in a 4-block radius."),
        HOLY("Holy Hand Grenade", "Holy blast plus consecrated ground. Lob it at the undead."),
        SINGULARITY("Singularity Grenade", "A gravity well that drags enemies together for 4 seconds.");

        public final String title, desc;

        Kind(String t, String d) {
            title = t;
            desc = d;
        }
    }

    public final Kind kind;

    public GrenadeItem(Properties p, Kind kind) {
        super(p.stacksTo(16));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            Vec3 from = player.getEyePosition();
            SiegeProjectile g = SiegeProjectile.create(sl, ProjKind.GRENADE, player, from);
            g.setDeltaMovement(player.getLookAngle().scale(1.1).add(0, 0.15, 0));
            g.life = 60;
            switch (kind) {
                case FRAG -> g.payload(25f, Element.EXPLOSIVE).area(4f).knockback = 0.8f;
                case INCENDIARY -> {
                    g.payload(6f, Element.FIRE).area(3f);
                    g.onImpact = (p, at) -> HazardZone.spawn(sl, HazardZone.Kind.FIRE, at, 4f, 120, 2f, player, false);
                }
                case CRYO -> {
                    g.payload(8f, Element.ICE).area(4f);
                    g.onEntityHit = (p, e) -> com.lastbastion.combat.SiegeDamage.apply(e, ModEffects.FROZEN.get(), 80, 0);
                }
                case HOLY -> {
                    g.payload(30f, Element.HOLY).area(4.5f).status(ModEffects.PURIFIED, 100, 0);
                    g.onImpact = (p, at) -> HazardZone.spawn(sl, HazardZone.Kind.HOLY, at, 4f, 120, 2f, player, false);
                }
                case SINGULARITY -> {
                    g.payload(4f, Element.ARCANE).area(2f);
                    g.onImpact = (p, at) -> HazardZone.spawn(sl, HazardZone.Kind.WHIRLPOOL, at, 5f, 80, 2f, player, false);
                }
            }
            g.fire(sl);
            sl.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1f, 0.6f);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
    }
}
