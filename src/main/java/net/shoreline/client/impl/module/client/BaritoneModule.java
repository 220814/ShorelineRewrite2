package net.shoreline.client.impl.module.client;

import net.minecraft.text.Text;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ConcurrentModule;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.gui.hud.ChatMessageEvent;
import net.shoreline.client.impl.event.world.LoadWorldEvent;
import net.shoreline.client.util.FormattingUtil;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.eventbus.annotation.EventListener;

import java.awt.*;

/**
 * @author Shoreline
 * @since 1.0
 */
public class BaritoneModule extends ConcurrentModule
{
    Config<Float> rangeConfig = register(new NumberConfig<>("Range", "Baritone block reach distance", 0.0f, 4.0f, 5.0f));
    Config<Boolean> placeConfig = register(new BooleanConfig("Place", "Allow baritone to place blocks", true));
    Config<Boolean> breakConfig = register(new BooleanConfig("Break", "Allow baritone to break blocks", true));
    Config<Boolean> sprintConfig = register(new BooleanConfig("Sprint", "Allow baritone to sprint", true));
    Config<Boolean> inventoryConfig = register(new BooleanConfig("UseInventory", "Allow baritone to use player inventory", false));
    Config<Boolean> vinesConfig = register(new BooleanConfig("Vines", "Allow baritone to climb vines", true));
    Config<Boolean> jump256Config = register(new BooleanConfig("JumpAt256", "Allow baritone to jump at 256 blocks", false));
    Config<Boolean> waterBucketFallConfig = register(new BooleanConfig("WaterBucketFall", "Allow baritone to use waterbuckets when falling", false));
    Config<Boolean> parkourConfig = register(new BooleanConfig("Parkour", "Allow baritone to jump between blocks", true));
    Config<Boolean> parkourPlaceConfig = register(new BooleanConfig("ParkourPlace", "Allow baritone to jump and place blocks", false));
    Config<Boolean> parkourAscendConfig = register(new BooleanConfig("ParkourAscend", "Allow baritone to jump up blocks", true));
    Config<Boolean> diagonalAscendConfig = register(new BooleanConfig("DiagonalAscend", "Allow baritone to jump up blocks diagonally", false));
    Config<Boolean> diagonalDescendConfig = register(new BooleanConfig("DiagonalDescend", "Allow baritone to move down blocks diagonally", false));
    Config<Boolean> mineDownConfig = register(new BooleanConfig("MineDownward", "Allow baritone to mine down", true));
    Config<Boolean> legitMineConfig = register(new BooleanConfig("LegitMine", "Uses baritone legit mine", false));
    Config<Boolean> logOnArrivalConfig = register(new BooleanConfig("LogOnArrival", "Logout when you arrive at destination", false));
    Config<Boolean> freeLookConfig = register(new BooleanConfig("FreeLook", "Allows you to look around freely while using baritone", true));
    Config<Boolean> antiCheatConfig = register(new BooleanConfig("AntiCheat", "Uses NCP placements and breaks", false));
    Config<Boolean> strictLiquidConfig = register(new BooleanConfig("Strict-Liquid", "Uses strick liquid checks", false));
    Config<Boolean> censorCoordsConfig = register(new BooleanConfig("CensorCoords", "Censors goal coordinates in chat", false));
    Config<Boolean> censorCommandsConfig = register(new BooleanConfig("CensorCommands", "Censors baritone commands in chat", false));
    Config<Boolean> chatControlConfig = register(new BooleanConfig("ChatControl", "Allows you to type baritone commands in chat without prefix", true));
    Config<Boolean> debugConfig = register(new BooleanConfig("Debug", "Debugs in the chat", false));
    Config<Color> goalColor = register(new ColorConfig("GoalColor", "The color of the goal box", Color.GREEN, false, false));
    Config<Color> pathColor = register(new ColorConfig("CurrentPathColor", "The color of the path", Color.RED, false, false));
    Config<Color> nextPathColor = register(new ColorConfig("NextPathColor", "The color of the path", Color.MAGENTA, false, false));

    public BaritoneModule()
    {
        super("Baritone", "Configure baritone", ModuleCategory.CLIENT);
        // Print 
        System.out.println("Baritone has been removed");
    }

    public void onEnable()
    {
        // remove @override
        System.out.println("Baritone has been removed.");
    }

    @EventListener
    public void onConfigUpdate(ConfigUpdateEvent event)
    {
        // Baritone logic removed
    }
    
    @EventListener
    public void onChat(ChatMessageEvent event)
    {
        // syncBaritoneSettings removed
    }

    @EventListener
    public void onLoadWorld(LoadWorldEvent event)
    {
        // syncBaritoneSettings removed
    }

    @EventListener
    public void onChatText(ChatMessageEvent event)
    {
        String text = event.getText().getString();
        if (text.startsWith("[Baritone]"))
        {
            event.cancel();
            event.setText(Text.of(ChatUtil.PREFIX + FormattingUtil.toString(event.getText())));
        }
    }
}
    
