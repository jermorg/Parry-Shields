package com.jermorg.parryshields.item.shield;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.UUID;

public class SpikeShield extends ShieldItem {

    private static final HashMap<UUID, Long> lastParryTime = new HashMap<>();

    public SpikeShield() {
        super(new Properties()
                .durability(216)
        );
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide) {
            lastParryTime.put(player.getUUID(), world.getGameTime());
        }
        return super.use(world, player, hand);
    }

    public static Long getLastParryTime(Player player) {
        return lastParryTime.get(player.getUUID());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }


    @Override
    public boolean isFoil(ItemStack stack) {
        if (Minecraft.getInstance().player != null) {
            Player player = Minecraft.getInstance().player;
            if (player.isUsingItem() && player.getUseItem() == stack) {
                Long parryTime = getLastParryTime(player);
                long gameTime = player.level().getGameTime();
                return parryTime != null && gameTime - parryTime < 10;
            }
        }
        return super.isFoil(stack);
    }

    @SubscribeEvent
    public void onPlayerAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.isBlocking()) return;
        if (player.level().isClientSide) return;
        ItemStack activeItem = player.getUseItem();
        if (!(activeItem.getItem() instanceof SpikeShield)) return;

        Entity directSource = event.getSource().getDirectEntity();

        if (directSource instanceof AbstractArrow) return;

        DamageSource source = event.getSource();
        String damageType = source.getMsgId();
        if ("explosion".equals(damageType) || "explosion.player".equals(damageType)) return;

        if (!(directSource instanceof LivingEntity attacker)) return;

        Long parryTime = lastParryTime.get(player.getUUID());
        float incomingDamage = event.getAmount();
        ServerLevel level = (ServerLevel) player.level();

        if (parryTime != null && level.getGameTime() - parryTime < 10) {

            if(incomingDamage * 0.5f < 6f){
                attacker.hurt(event.getSource(), 6f);
            } else if(incomingDamage * 0.5f > 10f){
                attacker.hurt(event.getSource(), 10f);
            } else {
                attacker.hurt(event.getSource(), incomingDamage * 0.5f);
            }

            Vec3 knockbackDir = attacker.position().subtract(player.position()).normalize();
            double knockbackStrength = 0.5D;
            attacker.setDeltaMovement(knockbackDir.scale(knockbackStrength).add(0, 0.2, 0));
            attacker.hurtMarked = true;

        } else {
            attacker.hurt(event.getSource(), 3f);
        }

        double yaw = Math.toRadians(player.getYRot());
        double x = player.getX() - Math.sin(yaw) * 0.5;
        double y = player.getY() + 1.2;
        double z = player.getZ() + Math.cos(yaw) * 0.5;

        level.sendParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                x, y + 1.0, z,
                10,
                0.3, 0.6, 0.3,
                0.1
        );
        level.playSound(null, player.blockPosition(), SoundEvents.HONEY_BLOCK_PLACE, player.getSoundSource(), 2.5f, 1.2f);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 0.2f, 1.2f);

        lastParryTime.remove(player.getUUID());
    }

}
