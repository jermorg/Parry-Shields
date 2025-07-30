package com.jermorg.parryshields.item.shield;

import com.jermorg.parryshields.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;

public class AbsorbingShield extends ShieldItem {

    private static final HashMap<UUID, Long> lastParryTime = new HashMap<>();

    public AbsorbingShield() {
        super(new Properties()
                .durability(116));
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static Long getLastParryTime(Player player) {
        return lastParryTime.get(player.getUUID());
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (!world.isClientSide) {
            lastParryTime.put(player.getUUID(), world.getGameTime());

        }

        return InteractionResultHolder.consume(itemstack);
    }

    @SubscribeEvent
    public void onPlayerAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.isBlocking()) return;
        if (player.level().isClientSide) return;

        ItemStack activeItem = player.getUseItem();
        if (!(activeItem.getItem() instanceof AbsorbingShield)) return;

        Long parryTime = lastParryTime.get(player.getUUID());
        boolean isParrying = parryTime != null && player.level().getGameTime() - parryTime <= 10;

        Entity source = event.getSource().getDirectEntity();
        boolean isMelee = !(source instanceof Projectile);
        ServerLevel level = (ServerLevel) player.level();

        if (isMelee && source instanceof LivingEntity attacker) {
            InteractionHand attackHand = attacker.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack weapon = attacker.getItemInHand(attackHand);

            if (!weapon.isEmpty()) {
                if (isParrying) {
                    int count = weapon.getCount();
                    ItemStack dropped = weapon.copy();
                    dropped.setCount(count);
                    attacker.spawnAtLocation(dropped);
                    weapon.shrink(count);

                    attacker.addEffect(new MobEffectInstance(ModEffects.STUN.get(), 60));

                    Vec3 direction = attacker.position().subtract(player.position()).normalize();
                    Vec3 knockback = direction.scale(0.5).add(0, 0.2, 0);
                    attacker.setDeltaMovement(knockback);
                    attacker.hurtMarked = true;
                } else {
                    boolean isPowerfulWeapon = weapon.getItem() instanceof AxeItem ||
                            (weapon.getItem() instanceof TieredItem tieredItem &&
                                    (tieredItem.getTier() == Tiers.DIAMOND || tieredItem.getTier() == Tiers.NETHERITE));

                    if (isPowerfulWeapon) {
                        InteractionHand shieldHand = player.getUseItem() == player.getOffhandItem() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                        InteractionHand weaponHand = (shieldHand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

                        ItemStack weaponInHand = player.getItemInHand(weaponHand);
                        if (!weaponInHand.isEmpty()) {

                            ItemStack shieldStack = player.getUseItem();
                            if (shieldStack.getItem() instanceof ShieldItem) {
                                player.getCooldowns().addCooldown(shieldStack.getItem(), 140);
                            }

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.8f);
                        }
                    } else {
                        int count = weapon.getCount();
                        ItemStack dropped = weapon.copy();
                        dropped.setCount(count);
                        attacker.spawnAtLocation(dropped);
                        weapon.shrink(count);
                    }
                }
            }

            double yaw = Math.toRadians(player.getYRot());
            double x = player.getX() - Math.sin(yaw) * 0.5;
            double y = player.getY() + 1.2;
            double z = player.getZ() + Math.cos(yaw) * 0.5;

            level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, player.getSoundSource(), 1, 1.3f);
            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 0.5f, 1.2f);

            if (isParrying) {
                lastParryTime.remove(player.getUUID());
            }
        }
    }


    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        Projectile projectile = event.getProjectile();
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHitResult)) return;
        if (!(entityHitResult.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;

        ItemStack activeItem = player.getUseItem();
        if (!(activeItem.getItem() instanceof AbsorbingShield)) return;

        ServerLevel level = (ServerLevel) player.level();
        Long parryTime = lastParryTime.get(player.getUUID());
        boolean isParrying = parryTime != null && level.getGameTime() - parryTime <= 10;

        ItemStack itemCopy = ItemStack.EMPTY;

        if (projectile instanceof ThrownTrident trident) {
            try {
                Field field = ThrownTrident.class.getDeclaredField("tridentItem");
                field.setAccessible(true);
                ItemStack original = (ItemStack) field.get(trident);
                itemCopy = original.copy();

                if (trident.getOwner() instanceof Drowned drowned) {
                    ItemStack mainHand = drowned.getMainHandItem();
                    if (ItemStack.isSameItem(mainHand, original)) {
                        drowned.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    }
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                itemCopy = new ItemStack(Items.TRIDENT);
            }
        } else if (projectile.getType() == EntityType.ARROW) {
            itemCopy = new ItemStack(Items.ARROW);
        } else if (projectile instanceof ThrownEnderpearl) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            itemCopy = new ItemStack(Items.ENDER_PEARL);
        } else if (projectile instanceof ThrownPotion thrownPotion) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            itemCopy = thrownPotion.getItem().copy();
        } else {
            return;
        }

        if (!itemCopy.isEmpty()) {
            if (isParrying) {
                if (!player.getInventory().add(itemCopy)) {
                    player.spawnAtLocation(itemCopy);
                }
            } else {
                player.spawnAtLocation(itemCopy);
            }

            projectile.remove(Entity.RemovalReason.DISCARDED);
        }

        level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.1, 0.1, 0.1, 0.01);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, player.getSoundSource(), 1, 1.3f);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 0.5f, 1.2f);

        lastParryTime.remove(player.getUUID());
    }
}
