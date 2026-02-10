package net.shoreline.client.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.impl.event.entity.*;
import net.shoreline.client.impl.event.render.entity.ElytraTransformEvent;
import net.shoreline.client.impl.module.movement.ElytraFlyModule;
import net.shoreline.client.impl.event.network.SprintCancelEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity implements Globals 
{
    @Shadow
    protected ItemStack activeItemStack;

    @Shadow
    public abstract float getYaw(float tickDelta);

    @Shadow
    protected abstract float getJumpVelocity();

    @Shadow
    private int jumpingCooldown;

    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> par1);

    @Shadow 
    public abstract void setSprinting(boolean sprinting);

    @Unique
    private boolean prevFlying;

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void hookSetSprinting(boolean sprinting, CallbackInfo ci) 
    {
        if ((Object) this != mc.player) 
        {
            return;
        }
        final SprintCancelEvent sprintEvent = new SprintCancelEvent();
        EventBus.INSTANCE.dispatch(sprintEvent);
        if (sprintEvent.isCanceled()) 
        {
            this.setSprinting(true);
            ci.cancel();
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void hookGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) 
    {
        SwingSpeedEvent swingSpeedEvent = new SwingSpeedEvent();
        EventBus.INSTANCE.dispatch(swingSpeedEvent);
        if (swingSpeedEvent.isCanceled()) 
        {
            if (swingSpeedEvent.getSelfOnly() && ((Object) this != mc.player)) 
            {
                return;
            }
            cir.setReturnValue(swingSpeedEvent.getSwingSpeed());
        }
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float hookJump$getYaw(float original) 
    {
        if ((Object) this != mc.player) 
        {
            return original;
        }
        JumpRotationEvent jumpRotationEvent = new JumpRotationEvent(original);
        EventBus.INSTANCE.dispatch(jumpRotationEvent);
        return jumpRotationEvent.isCanceled() ? jumpRotationEvent.getYaw() : original;
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    private boolean hookHasStatusEffect(LivingEntity instance, RegistryEntry<StatusEffect> effect) 
    {
        if (instance.equals(mc.player) && effect == StatusEffects.LEVITATION) 
        {
            LevitationEvent levitationEvent = new LevitationEvent();
            EventBus.INSTANCE.dispatch(levitationEvent);
            return !levitationEvent.isCanceled() && hasStatusEffect(effect);
        }
        return hasStatusEffect(effect);
    }

    @Inject(method = "consumeItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;", shift = At.Shift.AFTER))
    private void hookConsumeItem(CallbackInfo ci) 
    {
        if ((Object) this == mc.player) 
        {
            EventBus.INSTANCE.dispatch(new ConsumeItemEvent(activeItemStack));
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "HEAD"))
    private void hookTickMovement(CallbackInfo ci) 
    {
        JumpDelayEvent jumpDelayEvent = new JumpDelayEvent();
        EventBus.INSTANCE.dispatch(jumpDelayEvent);
        if (jumpDelayEvent.isCanceled()) 
        {
            jumpingCooldown = 0;
        }
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At(value = "HEAD"))
    private void hookAddStatusEffect(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) 
    {
        if ((Object) this == mc.player) 
        {
            EventBus.INSTANCE.dispatch(new StatusEffectEvent.Add(effect));
        }
    }

    @Inject(method = "setStatusEffect", at = @At(value = "HEAD"))
    private void hookAddStatusEffect$1(StatusEffectInstance effect, Entity source, CallbackInfo ci) 
    {
        if ((Object) this == mc.player) 
        {
            EventBus.INSTANCE.dispatch(new StatusEffectEvent.Add(effect));
        }
    }

    @Inject(method = "removeStatusEffectInternal", at = @At(value = "HEAD"))
    private void hookRemoveStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<StatusEffectInstance> cir) 
    {
        if ((Object) this == mc.player) 
        {
            EventBus.INSTANCE.dispatch(new StatusEffectEvent.Remove(effect));
        }
    }

    @Inject(method = "isFallFlying", at = @At("TAIL"), cancellable = true)
    public void hookIsFallFlying(CallbackInfoReturnable<Boolean> cir) 
    {
        // Baritone logic removed for compilation
        boolean flying = cir.getReturnValue();
        if (ElytraFlyModule.getInstance().isEnabled() && ElytraFlyModule.getInstance().isBounce() && (prevFlying && !flying)) 
        {
            if (mc.player != null) {
                mc.player.startFallFlying();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                cir.setReturnValue(true);
            }
        }
        prevFlying = flying;
    }

    @Inject(method = "applyDamage", at = @At(value = "HEAD"))
    private void hookDamage(DamageSource source, float amount, CallbackInfo ci) 
    {
        if (mc.player != null && source.getAttacker() != null && source.getAttacker().getName().getString().equalsIgnoreCase(mc.player.getName().getString())) 
        {
            EventBus.INSTANCE.dispatch(new PlayerDamageEvent((LivingEntity) (Object) this));
        }
    }

    @Inject(method = "updateTrackedPositionAndAngles", at = @At(value = "HEAD"))
    private void hookUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, CallbackInfo ci) 
    {
        EventBus.INSTANCE.dispatch(new UpdateServerPositionEvent((LivingEntity) (Object) this, x, y, z, yaw, pitch));
    }

    @Inject(method = "travel", at = @At(value = "HEAD"), cancellable = true)
    private void hookTravelPre(Vec3d movementInput, CallbackInfo ci) 
    {
        EntityTravelEvent entityTravelEvent = new EntityTravelEvent((LivingEntity) (Object) this, true);
        EventBus.INSTANCE.dispatch(entityTravelEvent);
        if (entityTravelEvent.isCanceled()) ci.cancel();
    }

    @Inject(method = "travel", at = @At(value = "RETURN"))
    private void hookTravelPost(Vec3d movementInput, CallbackInfo ci) 
    {
        EventBus.INSTANCE.dispatch(new EntityTravelEvent((LivingEntity) (Object) this, false));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isFallFlying()Z"))
    private boolean hookTick(LivingEntity instance) 
    {
        ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
        EventBus.INSTANCE.dispatch(elytraTransformEvent);
        return !elytraTransformEvent.isCanceled() && instance.isFallFlying();
    }

    @Inject(method = "getStepHeight", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetStepHeight(CallbackInfoReturnable<Float> cir) 
    {
        StepEvent stepEvent = new StepEvent((LivingEntity) (Object) this, cir.getReturnValueF());
        EventBus.INSTANCE.dispatch(stepEvent);
        if (stepEvent.isCanceled()) 
        {
            cir.setReturnValue(stepEvent.getStepHeight());
        }
    }

    @Inject(method = "isClimbing", at = @At(value = "HEAD"), cancellable = true)
    private void hookIsClimbing(CallbackInfoReturnable<Boolean> cir) 
    {
        if ((Object) this != mc.player) return;
        PlayerClimbEvent playerClimbEvent = new PlayerClimbEvent();
        EventBus.INSTANCE.dispatch(playerClimbEvent);
        if (playerClimbEvent.isCanceled()) 
        {
            // climb check
            System.out.println("Baritone has been removed");
            cir.setReturnValue(false);
        }
    }
}
        
