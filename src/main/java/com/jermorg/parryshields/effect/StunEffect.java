package com.jermorg.parryshields.effect;

import com.jermorg.parryshields.ParryShields;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class StunEffect extends MobEffect {

    public StunEffect() {
        super(MobEffectCategory.HARMFUL, 0x888888);

        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                "eac1bc63-1b9a-4dbf-bc96-20727a9c9a3f",
                -1000.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

}
