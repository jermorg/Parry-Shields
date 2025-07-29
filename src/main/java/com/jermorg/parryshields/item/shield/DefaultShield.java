package com.jermorg.parryshields.item.shield;

import com.jermorg.parryshields.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DefaultShield extends ShieldItem {

    private static final HashMap<UUID, Long> lastParryTime = new HashMap<>();
    private final Map<UUID, UUID> pendingCrits = new HashMap<>();
    private static Holder<MobEffect> STUN_EFFECT;

    public DefaultShield() {
        super(new Properties()
                .durability(316)
                .stacksTo(1));
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

            if (STUN_EFFECT == null) {
                STUN_EFFECT = ModEffects.STUN.getHolder().get();
            }
        }

        return InteractionResultHolder.consume(itemstack);
    }

    @SubscribeEvent
    public void onPlayerAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.isBlocking()) return;
        if (player.level().isClientSide) return;
        ItemStack activeItem = player.getUseItem();
        if (!(activeItem.getItem() instanceof DefaultShield)) return;

        Long parryTime = lastParryTime.get(player.getUUID());
        if (parryTime == null || player.level().getGameTime() - parryTime > 10) return;

        Entity source = event.getSource().getDirectEntity();
        boolean isMelee = !(source instanceof Projectile);

        if (isMelee && source instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(STUN_EFFECT, 30, 0));
            pendingCrits.put(attacker.getUUID(), player.getUUID());
        }

        ServerLevel level = (ServerLevel) player.level();
        double yaw = Math.toRadians(player.getYRot());
        double x = player.getX() - Math.sin(yaw) * 0.5;
        double y = player.getY() + 1.2;
        double z = player.getZ() + Math.cos(yaw) * 0.5;

        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, player.getSoundSource(), 1, 1.2f);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 1, 1.2f);

        lastParryTime.remove(player.getUUID());
    }

    @SubscribeEvent
    public void onCritAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        UUID targetId = target.getUUID();
        UUID playerId = player.getUUID();

        if (!pendingCrits.containsKey(targetId)) return;
        if (!pendingCrits.get(targetId).equals(playerId)) return;

        ((ServerLevel) target.level()).sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY(0.5), target.getZ(),
                10, 0.2, 0.5, 0.2, 0.1
        );

        event.setAmount(event.getAmount() * 1.5f); // 50% more
        target.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 1.0f, 1.0f
        );

        pendingCrits.remove(targetId);
    }
}
