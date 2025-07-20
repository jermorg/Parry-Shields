package com.jermorg.parryshields.item.shield;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.UUID;

public class SlimeShield extends ShieldItem {

    private static final HashMap<UUID, Long> lastParryTime = new HashMap<>();

    public SlimeShield() {
        super(new Item.Properties()
                .durability(336)
                .stacksTo(1));
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
        return lastParryTime.getOrDefault(player.getUUID(), 0L);
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
        if (!(activeItem.getItem() instanceof SlimeShield)) return;

        ServerLevel level = (ServerLevel) player.level();
        Entity source = event.getSource().getDirectEntity();
        if (source == null) return;

        Long parryTime = getLastParryTime(player);
        boolean parried = parryTime != null && level.getGameTime() - parryTime < 10;

//        if (source instanceof Projectile projectile) {
//
//            double originalSpeed = projectile.getDeltaMovement().length();
//            Vec3 returnVelocity;
//            if (projectile instanceof LargeFireball || projectile instanceof SmallFireball || projectile instanceof WitherSkull || projectile instanceof LlamaSpit || projectile instanceof Arrow || projectile instanceof ThrownTrident) {
//                returnVelocity = projectile.getDeltaMovement().normalize().scale(-originalSpeed * (parried ? 2.0 : 0.5));
//                projectile.setDeltaMovement(returnVelocity);
//
//                if (projectile instanceof AbstractArrow arrow) {
//                    arrow.setNoPhysics(false);
//                    arrow.setDeltaMovement(returnVelocity);
//                }
//            }
//
//            level.playSound(null, player.blockPosition(), parried ? SoundEvents.SLIME_JUMP : SoundEvents.SLIME_JUMP_SMALL, player.getSoundSource(), 1, 1.5f);
//            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 0.5f, 1.5f);
//            level.sendParticles(ParticleTypes.ITEM_SLIME, player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.1, 0.1, 0.1, 0.01);
//            lastParryTime.remove(player.getUUID());
//            return;
//        }

        if (source instanceof LivingEntity attacker) {

            double strength = parried ? 1.5 : 0.5;
            Vec3 knockback = attacker.position().subtract(player.position()).normalize().scale(strength);
            attacker.setDeltaMovement(knockback.add(0, 0.2, 0));

            level.playSound(null, player.blockPosition(), parried ? SoundEvents.SLIME_ATTACK : SoundEvents.SLIME_JUMP, player.getSoundSource(), 1, 1.5f);
            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 0.5f, 1.0f);
            level.sendParticles(ParticleTypes.ITEM_SLIME, player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.1, 0.1, 0.1, 0.01);
        }

        lastParryTime.remove(player.getUUID());
    }
}