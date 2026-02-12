package net.shoreline.client.impl.module.combat;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.player.PotionLogicUtil;
import net.shoreline.client.util.world.PositionUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoPotModule extends ToggleModule {

    Config<Mode> modeConfig = register(new EnumConfig<>("Mode", "Potion type", Mode.BOTH, Mode.values()));
    Config<Boolean> totemForceConfig = register(new BooleanConfig("TotemForce", "Immediate re-pot on pop", true));
    Config<Boolean> liquidPotConfig = register(new BooleanConfig("LiquidPot", "Allow potting in water/lava", true));
    Config<Integer> delayConfig = register(new NumberConfig<>("Delay", "Ticks between pots", 1, 1, 10));
    Config<Integer> thresholdConfig = register(new NumberConfig<>("Threshold", "Duration ticks left to pot", 20, 0, 100));
    Config<Double> enemyCheckConfig = register(new NumberConfig<>("EnemyCheck", "Aborts if enemy in range", 0.11, 0.0, 1.0));

    private int potDelay;

    public AutoPotModule() {
        super("AutoPot", "Automatically throws potions", ModuleCategory.COMBAT);
    }

    private enum Mode { STRENGTH, TURTLE, BOTH }

    @EventListener
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (PositionUtil.isMoving() || PositionUtil.isOverAir()) return;

        boolean inLiquid = mc.player.isInLava() || mc.player.isTouchingWater();
        
        if (inLiquid && !liquidPotConfig.getValue()) return;
        if (!inLiquid && !mc.player.isOnGround()) return;

        if (PositionUtil.isEnemyInHole(enemyCheckConfig.getValue())) return;

        if (potDelay > 0) {
            potDelay--;
            return;
        }

        boolean force = totemForceConfig.getValue() && (System.currentTimeMillis() - Managers.TOTEM.getLastPopTime(mc.player) < 1000);

        if (modeConfig.getValue() == Mode.STRENGTH || modeConfig.getValue() == Mode.BOTH) {
            if (force || PotionLogicUtil.shouldPot(StatusEffects.STRENGTH, thresholdConfig.getValue())) {
                if (executePotting("Strength", inLiquid)) return;
            }
        }

        if (modeConfig.getValue() == Mode.TURTLE || modeConfig.getValue() == Mode.BOTH) {
            if (force || PotionLogicUtil.shouldPot(StatusEffects.RESISTANCE, thresholdConfig.getValue())) {
                executePotting("Turtle", inLiquid);
            }
        }
    }

    private boolean executePotting(String type, boolean inLiquid) {
        int slot = getPotSlot(type);
        if (slot == -1) return false;

        float pitch = inLiquid ? 85.0f : 90.0f;
        
        Managers.ROTATION.setRotationSilent(mc.player.getYaw(), pitch);

        int oldSlot = mc.player.getInventory().selectedSlot;
        Managers.INVENTORY.setSlot(slot);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        Managers.INVENTORY.setSlot(oldSlot);

        ChatUtil.clientSendMessage("Threw §s" + type + " §rpotion.");
        potDelay = delayConfig.getValue();
        return true;
    }

    private int getPotSlot(String type) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.SPLASH_POTION) {
                String name = stack.getName().getString().toLowerCase();
                if (type.equals("Turtle") && (name.contains("turtle") || name.contains("master"))) return i;
                if (type.equals("Strength") && name.contains("strength")) return i;
            }
        }
        return -1;
    }
}
            
