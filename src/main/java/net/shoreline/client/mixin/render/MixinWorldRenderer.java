package net.shoreline.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import net.shoreline.client.impl.event.PerspectiveEvent;
import net.shoreline.client.impl.event.render.RenderEntityInWorldEvent;
import net.shoreline.client.impl.event.render.RenderShaderEvent;
import net.shoreline.client.impl.event.render.RenderWorldBorderEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author TaoNeMay
 * @since 2.0
 */
@Mixin(WorldRenderer.class)
public class MixinWorldRenderer implements Globals
{
    @Inject(method = "render", at = @At(value = "RETURN"))
    private void hookRender(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci)
    {
        MatrixStack matrixStack = new MatrixStack();
        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        RenderSystem.applyModelViewMatrix();
        final RenderWorldEvent renderWorldEvent =
                new RenderWorldEvent(matrixStack, tickCounter.getTickDelta(true));
        EventBus.INSTANCE.dispatch(renderWorldEvent);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void hookRender$1(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci)
    {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        RenderShaderEvent renderOutlineShaderEvent = new RenderShaderEvent(matrixStack, tickCounter.getTickDelta(true));
        EventBus.INSTANCE.dispatch(renderOutlineShaderEvent);
    }

    @Inject(method = "renderWorldBorder", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderWorldBorder(Camera camera, CallbackInfo ci)
    {
        RenderWorldBorderEvent renderWorldBorderEvent = new RenderWorldBorderEvent();
        EventBus.INSTANCE.dispatch(renderWorldBorderEvent);
        if (renderWorldBorderEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * THAY THẾ @Redirect bằng @ModifyExpressionValue để tránh xung đột với MioClient
     * Target: Lnet/minecraft/client/render/Camera;isThirdPerson()Z
     */
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;isThirdPerson()Z"))
    private boolean hookIsThirdPerson(boolean original, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera)
    {
        PerspectiveEvent perspectiveEvent = new PerspectiveEvent(camera);
        EventBus.INSTANCE.dispatch(perspectiveEvent);

        // Nếu event bị cancel (ví dụ tính năng Freecam của Shoreline bật), ta ép trả về true
        if (perspectiveEvent.isCanceled())
        {
            return true;
        }
        return original; // Nếu không thì giữ nguyên giá trị (có thể đã được MioClient sửa đổi trước đó)
    }

    @Inject(method = "renderEntity", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderEntity(Entity entity, double cameraX, double cameraY, double cameraZ,
                                  float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci)
    {
        RenderEntityInWorldEvent renderEntityInWorldEvent = new RenderEntityInWorldEvent(entity);
        EventBus.INSTANCE.dispatch(renderEntityInWorldEvent);
        if (renderEntityInWorldEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
            
