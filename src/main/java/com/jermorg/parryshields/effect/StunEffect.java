package com.jermorg.parryshields.effect;

import com.jermorg.parryshields.ParryShields;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StunEffect extends MobEffect {

    public StunEffect() {
        super(MobEffectCategory.HARMFUL, 0x888888);

        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(ParryShields.MODID, "stun"),
                -1000.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

}
