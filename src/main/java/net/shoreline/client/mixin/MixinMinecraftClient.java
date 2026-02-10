package net.shoreline.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.shoreline.client.impl.event.*;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.gui.screen.pack.RefreshPacksEvent;
import net.shoreline.client.impl.imixin.IMinecraftClient;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements IMinecraftClient
{
    @Shadow public ClientWorld world;
    @Shadow public ClientPlayerEntity player;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow protected int attackCooldown;
    @Unique private boolean leftClick;
    @Unique private boolean rightClick;
    @Unique private boolean doAttackCalled;
    @Unique private boolean doItemUseCalled;

    @Shadow protected abstract void doItemUse();
    @Shadow protected abstract boolean doAttack();
    @Shadow @Final private Window window;

    @Override
    public void leftClick() {
        leftClick = true;
    }

    @Override
    public void rightClick() {
        rightClick = true;
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;render(Z)V", shift = At.Shift.BEFORE))
    private void hookRun(CallbackInfo ci) {
        EventBus.INSTANCE.dispatch(new RunTickEvent());
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;overlay:Lnet/minecraft/client/gui/screen/Overlay;"))
    private void hookTick(CallbackInfo ci) {
        EventBus.INSTANCE.dispatch(new ClientTickEvent());
    }

    @Inject(method = "onInitFinished", at = @At(value = "RETURN"))
    private void hookOnInitFinished(MinecraftClient.LoadingContext loadingContext, CallbackInfoReturnable<Runnable> cir) {
        EventBus.INSTANCE.dispatch(new FinishLoadingEvent());
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void hookTickPre(CallbackInfo ci) {
        doAttackCalled = false;
        doItemUseCalled = false;
        if (player != null && world != null) {
            TickEvent tickPreEvent = new TickEvent();
            tickPreEvent.setStage(StageEvent.EventStage.PRE);
            EventBus.INSTANCE.dispatch(tickPreEvent);
        }
        if (interactionManager == null) return;
        if (leftClick && !doAttackCalled) doAttack();
        if (rightClick && !doItemUseCalled) doItemUse();
        leftClick = false;
        rightClick = false;
    }

    @Unique
    private final List<Integer> deadList = new ArrayList<>();

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void hookTickPost(CallbackInfo ci) {
        if (player != null && world != null) {
            TickEvent tickPostEvent = new TickEvent();
            tickPostEvent.setStage(StageEvent.EventStage.POST);
            EventBus.INSTANCE.dispatch(tickPostEvent);
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity e) {
                    if (e.isDead() && !deadList.contains(e.getId())) {
                        EventBus.INSTANCE.dispatch(new EntityDeathEvent(e));
                        deadList.add(e.getId());
                    } else if (!e.isDead()) {
                        deadList.remove((Integer) e.getId());
                    }
                }
            }
        }
    }

    @Inject(method = "setScreen", at = @At(value = "TAIL"))
    private void hookSetScreen(Screen screen, CallbackInfo ci) {
        EventBus.INSTANCE.dispatch(new ScreenOpenEvent(screen));
    }

    @Inject(method = "doItemUse", at = @At(value = "HEAD"), cancellable = true)
    private void hookDoItemUse(CallbackInfo ci) {
        doItemUseCalled = true;
        ItemUseEvent itemUseEvent = new ItemUseEvent();
        EventBus.INSTANCE.dispatch(itemUseEvent);
        if (itemUseEvent.isCanceled()) ci.cancel();
    }

    @Inject(method = "doAttack", at = @At(value = "HEAD"))
    private void hookDoAttack(CallbackInfoReturnable<Boolean> cir) {
        doAttackCalled = true;
        AttackCooldownEvent attackCooldownEvent = new AttackCooldownEvent();
        EventBus.INSTANCE.dispatch(attackCooldownEvent);
        if (attackCooldownEvent.isCanceled()) attackCooldown = 0;
    }

    /**
     * Sửa lỗi xung đột bằng cách dùng ModifyExpressionValue thay vì Redirect
     */
    @ModifyExpressionValue(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean hookIsUsingItem(boolean original) {
        ItemMultitaskEvent itemMultitaskEvent = new ItemMultitaskEvent();
        EventBus.INSTANCE.dispatch(itemMultitaskEvent);
        // Nếu event bị cancel (Multitask bật), trả về false để Minecraft nghĩ player KHÔNG đang dùng item
        return !itemMultitaskEvent.isCanceled() && original;
    }

    /**
     * Sửa lỗi xung đột hookIsBreakingBlock với MioClient
     */
    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    private boolean hookIsBreakingBlock(boolean original) {
        ItemMultitaskEvent itemMultitaskEvent = new ItemMultitaskEvent();
        EventBus.INSTANCE.dispatch(itemMultitaskEvent);
        // Nếu Multitask bật, trả về false để cho phép vừa đào block vừa dùng item
        return !itemMultitaskEvent.isCanceled() && original;
    }

    @Inject(method = "getFramerateLimit", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        FramerateLimitEvent framerateLimitEvent = new FramerateLimitEvent();
        EventBus.INSTANCE.dispatch(framerateLimitEvent);
        if (framerateLimitEvent.isCanceled()) {
            cir.setReturnValue(framerateLimitEvent.getFramerateLimit());
        }
    }

    @Inject(method = "hasOutline", at = @At(value = "HEAD"), cancellable = true)
    private void hookHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        EntityOutlineEvent entityOutlineEvent = new EntityOutlineEvent(entity);
        EventBus.INSTANCE.dispatch(entityOutlineEvent);
        if (entityOutlineEvent.isCanceled()) cir.setReturnValue(true);
    }

    @Inject(method = "reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "RETURN"))
    private void hookReloadResources(boolean force, MinecraftClient.LoadingContext loadingContext, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        EventBus.INSTANCE.dispatch(new RefreshPacksEvent());
    }

    @Inject(method = "onResolutionChanged", at = @At(value = "TAIL"))
    private void hookOnResolutionChanged(CallbackInfo ci) {
        EventBus.INSTANCE.dispatch(new ResolutionEvent(window));
    }
}
