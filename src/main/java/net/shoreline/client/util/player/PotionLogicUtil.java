package net.shoreline.client.util.player;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.shoreline.client.util.Globals;

public class PotionLogicUtil implements Globals 
{
    public static int getDuration(RegistryEntry<StatusEffect> effect) 
    {
        if (mc.player == null) return 0;
        StatusEffectInstance instance = mc.player.getStatusEffect(effect);
        return instance != null ? instance.getDuration() : 0;
    }

    public static boolean hasEffect(RegistryEntry<StatusEffect> effect) 
    {
        return mc.player != null && mc.player.hasStatusEffect(effect);
    }

    public static boolean isExpiry(RegistryEntry<StatusEffect> effect, int thresholdTicks) 
    {
        return getDuration(effect) <= thresholdTicks;
    }

    public static boolean shouldPot(RegistryEntry<StatusEffect> effect, int thresholdTicks) 
    {
        if (mc.player == null) return false;
        return !hasEffect(effect) || isExpiry(effect, thresholdTicks);
    }

    public static boolean needsDoublePot(RegistryEntry<StatusEffect> effect1, RegistryEntry<StatusEffect> effect2, int threshold) 
    {
        return shouldPot(effect1, threshold) && shouldPot(effect2, threshold);
    }
}

