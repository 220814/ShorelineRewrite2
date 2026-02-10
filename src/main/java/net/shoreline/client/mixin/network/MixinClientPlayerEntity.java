package net.shoreline.client.mixin.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.impl.event.entity.SwingEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.impl.event.network.*;
import net.shoreline.client.impl.imixin.IClientPlayerEntity;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientPlayerEntity.class, priority = 1500)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity implements Globals, IClientPlayerEntity {
    @Shadow @Final public ClientPlayNetworkHandler networkHandler;
    @Shadow public double lastX;
    @Shadow public double lastBaseY;
    @Shadow public double lastZ;
    @Shadow public Input input;
    @Shadow @Final protected MinecraftClient client;
    @Shadow private boolean lastSneaking;
    @Shadow private float lastYaw;
    @Shadow private float lastPitch;
    @Shadow private boolean lastOnGround;
    @Shadow private int ticksSinceLastPositionPacketSent;
    @Shadow private boolean autoJumpEnabled;
    @Unique private boolean ticking;

    public MixinClientPlayerEntity() {
        super(MinecraftClient.getInstance().world, MinecraftClient.getInstance().player.getGameProfile());
    }

    @Shadow protected abstract void sendSprintingPacket();
    @Shadow public abstract boolean isSneaking();
    @Shadow protected abstract boolean isCamera();
    @Shadow protected abstract void autoJump(float dx, float dz);
    @Shadow public abstract void tick();
    @Shadow protected abstract void sendMovementPackets();

    @Inject(method = "sendMovementPackets", at = @At(value = "HEAD"), cancellable = true)
    private void hookSendMovementPackets(CallbackInfo ci) {
        PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent();
        playerUpdateEvent.setStage(StageEvent.EventStage.PRE);
        EventBus.INSTANCE.dispatch(playerUpdateEvent);
        MovementPacketsEvent movementPacketsEvent = new MovementPacketsEvent(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
        EventBus.INSTANCE.dispatch(movementPacketsEvent);
        double x = movementPacketsEvent.getX();
        double y = movementPacketsEvent.getY();
        double z = movementPacketsEvent.getZ();
        float yaw = movementPacketsEvent.getYaw();
        float pitch = movementPacketsEvent.getPitch();
        boolean ground = movementPacketsEvent.getOnGround();
        EncodeYawEvent encodeYawEvent = new EncodeYawEvent();
        EventBus.INSTANCE.dispatch(encodeYawEvent);
        if (encodeYawEvent.isCanceled()) yaw += 36000000.0f;
        if (movementPacketsEvent.isCanceled() || encodeYawEvent.isCanceled()) {
            ci.cancel();
            sendSprintingPacket();
            boolean bl = isSneaking();
            if (bl != lastSneaking) {
                networkHandler.sendPacket(new ClientCommandC2SPacket(this, bl ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                lastSneaking = bl;
            }
            if (isCamera()) {
                double d = x - lastX;
                double e = y - lastBaseY;
                double f = z - lastZ;
                double g = yaw - lastYaw;
                double h = pitch - lastPitch;
                ++ticksSinceLastPositionPacketSent;
                boolean bl2 = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || ticksSinceLastPositionPacketSent >= 20;
                boolean bl3 = g != 0.0 || h != 0.0;
                if (hasVehicle()) {
                    Vec3d vec3d = getVelocity();
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(vec3d.x, -999.0, vec3d.z, getYaw(), getPitch(), ground));
                    bl2 = false;
                } else if (bl2 && bl3) networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, ground));
                else if (bl2) networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, ground));
                else if (bl3) networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, ground));
                else if (lastOnGround != isOnGround()) networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(ground));
                if (bl2) {
                    lastX = x;
                    lastBaseY = y;
                    lastZ = z;
                    ticksSinceLastPositionPacketSent = 0;
                }
                if (bl3) {
                    lastYaw = yaw;
                    lastPitch = pitch;
                }
                lastOnGround = ground;
                autoJumpEnabled = client.options.getAutoJump().getValue();
            }
        }
        playerUpdateEvent.setStage(StageEvent.EventStage.POST);
        EventBus.INSTANCE.dispatch(playerUpdateEvent);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookTickPre(CallbackInfo ci) {
        EventBus.INSTANCE.dispatch(new PlayerTickEvent());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V", ordinal = 0, shift = At.Shift.AFTER))
    private void hookTick(CallbackInfo ci) {
        if (ticking) return;
        TickMovementEvent tickMovementEvent = new TickMovementEvent();
        EventBus.INSTANCE.dispatch(tickMovementEvent);
        if (tickMovementEvent.isCanceled()) {
            for (int i = 0; i < tickMovementEvent.getIterations(); i++) {
                ticking = true;
                tick();
                ticking = false;
                sendMovementPackets();
            }
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void hookTickMovementPost(CallbackInfo ci) {
        EventBus.INSTANCE.dispatch(new MovementSlowdownEvent(input));
    }

    @Inject(method = "move", at = @At(value = "HEAD"), cancellable = true)
    private void hookMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        final PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(movementType, movement);
        EventBus.INSTANCE.dispatch(playerMoveEvent);
        if (playerMoveEvent.isCanceled()) {
            ci.cancel();
            double d = getX();
            double e = getZ();
            super.move(movementType, playerMoveEvent.getMovement());
            autoJump((float) (getX() - d), (float) (getZ() - e));
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At(value = "HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        PushOutOfBlocksEvent pushOutOfBlocksEvent = new PushOutOfBlocksEvent();
        EventBus.INSTANCE.dispatch(pushOutOfBlocksEvent);
        if (pushOutOfBlocksEvent.isCanceled()) ci.cancel();
    }

    @Inject(method = "setCurrentHand", at = @At(value = "HEAD"))
    private void hookSetCurrentHand(Hand hand, CallbackInfo ci) {
        EventBus.INSTANCE.dispatch(new SetCurrentHandEvent(hand));
    }

    @Inject(method = "getMountJumpStrength", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetMountJumpStrength(CallbackInfoReturnable<Float> cir) {
        MountJumpStrengthEvent mountJumpStrengthEvent = new MountJumpStrengthEvent();
        EventBus.INSTANCE.dispatch(mountJumpStrengthEvent);
        if (mountJumpStrengthEvent.isCanceled()) cir.setReturnValue(mountJumpStrengthEvent.getJumpStrength());
    }

    @Inject(method = "swingHand", at = @At(value = "RETURN"))
    private void hookSwingHand(Hand hand, CallbackInfo ci) {
        EventBus.INSTANCE.dispatch(new SwingEvent(hand));
    }

    @Inject(method = "dismountVehicle", at = @At(value = "HEAD"), cancellable = true)
    private void hookDismountVehicle(CallbackInfo ci) {
        DismountVehicleEvent dismountVehicleEvent = new DismountVehicleEvent();
        EventBus.INSTANCE.dispatch(dismountVehicleEvent);
        if (dismountVehicleEvent.isCanceled()) ci.cancel();
    }

    @Override
    public float getLastSpoofedYaw() { return lastYaw; }
    @Override
    public float getLastSpoofedPitch() { return lastPitch; }
}
