package com.jermorg.parryshields.item.shield;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.UUID;

public class SlimeShield extends ShieldItem {

    private static final short ParryTime = 10;
    private static final HashMap<UUID, Long> lastParryTime = new HashMap<>();

    public SlimeShield() {
        super(new Item.Properties()
                .durability(336));
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
                return parryTime != null && gameTime - parryTime < ParryTime;
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
        boolean parried = parryTime != null && level.getGameTime() - parryTime < ParryTime;

        if (source instanceof LivingEntity attacker) {
            double strength = parried ? 1.5 : 0.5;
            Vec3 knockback = attacker.position().subtract(player.position()).normalize().scale(strength);
            attacker.setDeltaMovement(knockback.add(0, 0.2, 0));
            attacker.hurtMarked = true;

            level.playSound(null, player.blockPosition(), parried ? SoundEvents.SLIME_ATTACK : SoundEvents.SLIME_JUMP, player.getSoundSource(), 1, 1.5f);
            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 0.5f, 1.0f);
            level.sendParticles(ParticleTypes.ITEM_SLIME, player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.1, 0.1, 0.1, 0.01);
        }

        lastParryTime.remove(player.getUUID());
    }


    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        Projectile projectile = event.getProjectile();

        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHitResult)) return;
        if (!(entityHitResult.getEntity() instanceof LivingEntity blockingEntity)) return;

        if (!blockingEntity.isBlocking()) return;

        ItemStack activeItem = blockingEntity.getUseItem();
        if (!(activeItem.getItem() instanceof SlimeShield)) return;

        ServerLevel level = (ServerLevel) blockingEntity.level();
        Long parryTime = (blockingEntity instanceof Player player)
                ? getLastParryTime(player)
                : 0L;
        boolean parried = parryTime != null && level.getGameTime() - parryTime < ParryTime;

        Vec3 look = blockingEntity.getLookAngle().normalize();

        if (projectile instanceof LargeFireball fireball) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);

            Vec3 newDir = look.normalize().scale(parried ? 0.5 : 0.3);

            fireball.setOwner(blockingEntity);

            fireball.xPower = newDir.x;
            fireball.yPower = newDir.y;
            fireball.zPower = newDir.z;

            fireball.setDeltaMovement(newDir);
            fireball.hurtMarked = true;

        } else if (projectile instanceof AbstractArrow arrow) {
            double originalSpeed = projectile.getDeltaMovement().length();
            Vec3 returnVelocity = look.scale(parried ? originalSpeed * 1.5 : originalSpeed);

            Entity reflectedProjectile = projectile.getType().create(level);
            if (reflectedProjectile instanceof AbstractArrow newArrow) {
                newArrow.setPos(blockingEntity.getX(), blockingEntity.getEyeY(), blockingEntity.getZ());
                newArrow.setDeltaMovement(returnVelocity);
                level.addFreshEntity(newArrow);
                projectile.remove(Entity.RemovalReason.DISCARDED);
            }

        } else if (projectile instanceof ThrownEnderpearl) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            Vec3 reboundVelocity = look.scale(parried ? 2.0 : 1.0);
            projectile.setDeltaMovement(reboundVelocity);
            projectile.hurtMarked = true;

        } else if (projectile instanceof ThrownPotion potion) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            Vec3 reboundVelocity = look.scale(parried ? 2.0 : 1.0);
            potion.setDeltaMovement(reboundVelocity);
            potion.hurtMarked = true;
        }

        level.playSound(null, blockingEntity.blockPosition(), parried ? SoundEvents.SLIME_ATTACK : SoundEvents.SLIME_JUMP, blockingEntity.getSoundSource(), 1, 1.5f);
        level.playSound(null, blockingEntity.blockPosition(), SoundEvents.SHIELD_BLOCK, blockingEntity.getSoundSource(), 0.5f, 1.0f);
        level.sendParticles(ParticleTypes.ITEM_SLIME, blockingEntity.getX(), blockingEntity.getY() + 1.2, blockingEntity.getZ(), 5, 0.1, 0.1, 0.1, 0.01);

        lastParryTime.remove(blockingEntity.getUUID());
    }


}