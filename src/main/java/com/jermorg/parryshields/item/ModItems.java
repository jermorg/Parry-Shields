package com.jermorg.parryshields.item;

import com.jermorg.parryshields.ParryShields;
import com.jermorg.parryshields.item.shield.DefaultShield;
import com.jermorg.parryshields.item.shield.SlimeShield;
import com.jermorg.parryshields.item.shield.SpikeShield;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ParryShields.MODID);

    public static final RegistryObject<Item> SHIELD_PLUS = ITEMS.register("shield_plus", DefaultShield::new);
    public static final RegistryObject<Item> SPIKE_SHIELD = ITEMS.register("spike_shield", SpikeShield::new);
    public static final RegistryObject<Item> SLIME_SHIELD = ITEMS.register("slime_shield", SlimeShield::new);

    public static void register(IEventBus bus){
        ITEMS.register(bus);
    }
}
