package com.jermorg.parryshields.effect;

import com.jermorg.parryshields.ParryShields;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ParryShields.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ParryShields.MODID);

    public static final RegistryObject<MobEffect> STUN =
            EFFECTS.register("stun", StunEffect::new);
}