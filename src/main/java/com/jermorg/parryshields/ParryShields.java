package com.jermorg.parryshields;

import com.jermorg.parryshields.effect.ModEffects;
import com.jermorg.parryshields.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(ParryShields.MODID)
public class ParryShields {
    public static final String MODID = "parryshields";

    public ParryShields(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModEffects.EFFECTS.register(modEventBus);

        ModItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }


    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.SHIELD_PLUS);
            event.accept(ModItems.SPIKE_SHIELD);
            event.accept(ModItems.SLIME_SHIELD);
            event.accept(ModItems.ABSORBING_SHIELD);
        }
    }

    @Mod.EventBusSubscriber(modid = ParryShields.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            Minecraft.getInstance().execute(() -> {
                registerShieldProperty(ModItems.SHIELD_PLUS.get());
                registerShieldProperty(ModItems.SPIKE_SHIELD.get());
                registerShieldProperty(ModItems.SLIME_SHIELD.get());
                registerShieldProperty(ModItems.ABSORBING_SHIELD.get());
            });
        }

        private static void registerShieldProperty(Item item) {
            ItemProperties.register(item,
                    ResourceLocation.fromNamespaceAndPath(MODID, "blocking"),
                    (stack, level, entity, seed) -> {
                        if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
                            return 1.0F;
                        }
                        return 0.0F;
                    });
        }
    }

}
