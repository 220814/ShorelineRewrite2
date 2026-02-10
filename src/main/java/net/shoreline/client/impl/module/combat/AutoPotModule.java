package net.shoreline.client.impl.module.combat;

import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.player.SprintResetEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoPotModule extends ToggleModule {

    public AutoPotModule() {
        // lol
        super("AutoPot", "Automatically throws health potions", ModuleCategory.COMBAT);
        
        // pls print
        System.out.println("hello example");
    }
}
