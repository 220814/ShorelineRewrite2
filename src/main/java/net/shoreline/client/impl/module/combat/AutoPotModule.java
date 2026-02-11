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
import net.shoreline.client.impl.manager.player.rotation.Rotation;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.PotionLogicUtil;
import net.shoreline.client.util.world.PositionUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoPotModule extends ToggleModule {

    private final Config<Mode> modeConfig = new EnumConfig<>("Mode", "Potion type", Mode.BOTH, Mode.values());
    private final Config<Boolean> totemForceConfig = new BooleanConfig("TotemForce", "Immediate re-pot on pop", true);
    private final Config<Integer> delayConfig = new NumberConfig<>("Delay", "Ticks between pots", 1, 1, 10);
    private final Config<Integer> thresholdConfig = new NumberConfig<>("Threshold", "Duration ticks left to pot", 20, 0, 100);
    private final Config<Double> enemyCheckConfig = new NumberConfig<>("EnemyCheck", "Aborts if enemy in range", 0.11, 0.0, 1.0);
    private final Config<Boolean> silentRotationConfig = new BooleanConfig("SilentRotation", "Don't move your screen while potting", true);

    private int potDelay;

    public AutoPotModule() {
        super("AutoPot", "Automatic potion thrower", ModuleCategory.COMBAT);
    }

    private enum Mode { STRENGTH, SLOWNESS, BOTH }

    @EventListener
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (PositionUtil.isMoving() || !PositionUtil.canThrowPot() || PositionUtil.isEnemyInHole(enemyCheckConfig.getValue())) {
            return;
        }

        if (potDelay > 0) {
            potDelay--;
            return;
        }

        boolean force = totemForceConfig.getValue() && (System.currentTimeMillis() - Managers.TOTEM.getLastPopTime(mc.player) < 1000);

        if (modeConfig.getValue() == Mode.STRENGTH || modeConfig.getValue() == Mode.BOTH) {
            if (force || PotionLogicUtil.shouldPot(StatusEffects.STRENGTH, thresholdConfig.getValue())) {
                executePotting("Strength");
                return;
            }
        }

        if (modeConfig.getValue() == Mode.SLOWNESS || modeConfig.getValue() == Mode.BOTH) {
            if (force || PotionLogicUtil.shouldPot(StatusEffects.SLOWNESS, thresholdConfig.getValue())) {
                executePotting("Slowness");
            }
        }
    }

    private void executePotting(String type) {
        int slot = getPotSlot(type);
        if (slot == -1) return;

        Rotation potRotation = new Rotation(15, mc.player.getYaw(), 90.0f, !silentRotationConfig.getValue());
        Managers.ROTATION.setRotation(potRotation);

        int oldSlot = mc.player.getInventory().selectedSlot;
        
        Managers.INVENTORY.setSlot(slot);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        Managers.INVENTORY.setSlot(oldSlot);

        sendMessage("Potting " + type + "!");
        potDelay = delayConfig.getValue();
    }

    private int getPotSlot(String type) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.SPLASH_POTION) {
                String name = stack.getName().getString().toLowerCase();
                if (name.contains(type.toLowerCase())) return i;
            }
        }
        return -1;
    }
}

            
