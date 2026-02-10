package net.shoreline.client.util.world;

import net.minecraft.util.math.BlockPos;
import net.shoreline.client.util.Globals;

public class PositionUtil implements Globals 
{
    public static boolean isOverAir() 
    {
        if (mc.world == null || mc.player == null) return false;
        BlockPos pos = mc.player.getBlockPos();
        return mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos.down()).isAir();
    }

    public static boolean isOverLiquid() 
    {
        if (mc.world == null || mc.player == null) return false;
        BlockPos pos = mc.player.getBlockPos();
        return !mc.world.getFluidState(pos).isEmpty() || !mc.world.getFluidState(pos.down()).isEmpty();
    }

    public static boolean isFlying() 
    {
        return mc.player != null && (mc.player.getAbilities().flying || mc.player.isFallFlying());
    }
    
    public static boolean isMoving()
    {
        return mc.player != null && (mc.player.getVelocity().x != 0 || mc.player.getVelocity().z != 0);
    }

    public static boolean canThrowPot() 
    {
        return mc.player != null && mc.player.isOnGround() 
                && !isFlying() && !isOverAir() && !isOverLiquid();
    }
    
    public static boolean isEnemyInHole(double range)
    {
        if (mc.world == null || mc.player == null) return false;
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && !p.isDead())
                .anyMatch(p -> mc.player.squaredDistanceTo(p.getX(), mc.player.getY(), p.getZ()) <= range * range);
    }
}
