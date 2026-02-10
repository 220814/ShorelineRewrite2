package net.shoreline.client.mixin.text;

import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import net.shoreline.client.impl.event.ClientColorEvent;
import net.shoreline.client.impl.event.text.TextVisitEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mixin xử lý văn bản hệ thống.
 * Priority -1000 đảm bảo Shoreline giành quyền kiểm soát trước MioClient và các mod khác.
 */
@Mixin(value = TextVisitFactory.class, priority = -1000)
public abstract class MixinTextVisitFactory implements Globals 
{
    private static final Logger TRACER = LogManager.getLogger("Shoreline-Tracer");

    @Shadow
    private static boolean visitRegularCharacter(Style style, CharacterVisitor visitor, int index, char c) {
        return false;
    }

    /**
     * TRACER & SAFEGUARD: 
     * Ghi lại nhật ký class nào đang gọi hàm xử lý văn bản và ép buộc sử dụng logic gốc
     * nếu game chưa khởi tạo xong (tránh lỗi NPE từ nameprotect của MioClient).
     */
    @Inject(method = "visitFormatted(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", 
            at = @At("HEAD"), cancellable = true)
    private static void debugTracer(String text, Style style, CharacterVisitor visitor, CallbackInfoReturnable<Boolean> cir) {
        if (mc == null || mc.player == null) {
            // Truy vết class Java đang thực hiện cuộc gọi
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            if (stack.length > 3) {
                TRACER.info("[Trace] Text: '{}' | Caller: {}.{}:{}", 
                    text, stack[3].getClassName(), stack[3].getMethodName(), stack[3].getLineNumber());
            }

            // Ép trả về logic gốc của Minecraft để bỏ qua các Mixin bị lỗi phía sau
            try {
                cir.setReturnValue(TextVisitFactory.visitFormatted(text, 0, style, visitor));
            } catch (Throwable t) {
                TRACER.error("Bypass failed!");
            }
        }
    }

    /**
     * Xử lý các mã màu tùy chỉnh của Shoreline (§s, §g).
     */
    @Inject(method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", 
            at = @At(value = "HEAD"), cancellable = true)
    private static void hookShorelineCustom(String text, int startIndex, Style startingStyle, Style resetStyle, CharacterVisitor visitor, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (text == null || mc == null || mc.player == null) return;

            if (text.contains("§s") || text.contains("§g")) {
                cir.cancel();
                int len = text.length();
                Style style = startingStyle;

                for (int j = startIndex; j < len; ++j) {
                    char c = text.charAt(j);
                    if (c == '§') {
                        if (j + 1 >= len) break;
                        char code = text.charAt(j + 1);

                        if (code == 's') {
                            ClientColorEvent event = new ClientColorEvent();
                            EventBus.INSTANCE.dispatch(event);
                            style = style.withColor(event.getClientRgb());
                            j++;
                        } else if (code == 'g') {
                            ClientColorEvent.Friend event = new ClientColorEvent.Friend();
                            EventBus.INSTANCE.dispatch(event);
                            style = style.withColor(event.getClientRgb());
                            j++;
                        } else {
                            Formatting formatting = Formatting.byCode(code);
                            if (formatting != null) {
                                style = (formatting == Formatting.RESET) ? resetStyle : style.withExclusiveFormatting(formatting);
                            }
                            j++;
                        }
                    } else {
                        if (!visitRegularCharacter(style, visitor, j, c)) {
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            TRACER.error("[Internal Error] Source File: {}", 
                t.getStackTrace().length > 0 ? t.getStackTrace()[0].getFileName() : "Unknown");
        }
    }
}
                
