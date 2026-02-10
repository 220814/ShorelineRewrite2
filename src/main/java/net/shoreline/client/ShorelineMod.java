package net.shoreline.client;

/**
 * @author linus
 * @since 1.0
 */
public class ShorelineMod
{
    public static final String MOD_NAME = "Shoreline";
    public static final String MOD_VER = BuildConfig.VERSION;
    public static final String MOD_MC_VER = "1.21.1";

    public ShorelineMod()
    {
        // Print message when the mod class is instantiated
        System.out.println("Baritone has been removed");
    }

    /**
     * This code runs as soon as Minecraft is in a mod-load-ready state.
     * However, some things (like resources) may still be uninitialized.
     * Proceed with mild caution.
     */
    public void onInitializeClient()
    {
        Shoreline.init();
    }

    /**
     * Baritone check has been hardcoded to return false 
     * to prevent any remaining logic from executing.
     */
    public static boolean isBaritonePresent()
    {
        return false;
    }
}
