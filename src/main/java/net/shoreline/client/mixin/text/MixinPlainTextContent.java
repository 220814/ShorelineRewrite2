package net.shoreline.client.mixin.text;

import net.minecraft.text.PlainTextContent;
import net.shoreline.client.util.Globals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.text.PlainTextContent$Literal")
public abstract class MixinPlainTextContent implements Globals 
{
    @Inject(method = "string", at = @At("HEAD"), cancellable = true)
    private void onGetString(CallbackInfoReturnable<String> cir) 
    {
        if (mc == null || mc.player == null) 
        {
            cir.setReturnValue("");
        }
    }
}
