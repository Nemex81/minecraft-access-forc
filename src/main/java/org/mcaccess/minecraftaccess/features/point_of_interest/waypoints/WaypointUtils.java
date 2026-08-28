package org.mcaccess.minecraftaccess.features.point_of_interest.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class WaypointUtils {
    private WaypointUtils() {
    }

    public static boolean isOverworldNetherPair(Identifier dim1, Identifier dim2) {
        if (dim1 == null || dim2 == null) return false;
        boolean isOverworld1 = dim1.getPath().equals("overworld");
        boolean isNether1 = dim1.getPath().equals("the_nether");
        boolean isOverworld2 = dim2.getPath().equals("overworld");
        boolean isNether2 = dim2.getPath().equals("the_nether");
        return (isOverworld1 && isNether2) || (isNether1 && isOverworld2);
    }

    public static BlockPos convertCoordinates(BlockPos pos, Identifier fromDim, Identifier toDim) {
        if (pos == null || fromDim == null || toDim == null) return pos;
        if (fromDim.getPath().equals("overworld") && toDim.getPath().equals("the_nether")) {
            return new BlockPos(pos.getX() / 8, pos.getY(), pos.getZ() / 8);
        } else if (fromDim.getPath().equals("the_nether") && toDim.getPath().equals("overworld")) {
            return new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8);
        }
        return pos;
    }

    public static String getDimensionName(Identifier dimension) {
        if (dimension == null) return "";
        String key = dimension.toLanguageKey("dimension");
        String name = I18n.get(key);
        if (!name.equals(key)) {
            return name;
        }
        // Fallback to capitalizing path
        String path = dimension.getPath().replace('_', ' ');
        return path.substring(0, 1).toUpperCase() + path.substring(1);
    }

    public static Vec3 getTargetVectorForPlayer(Waypoint waypoint, LocalPlayer player, boolean allowCrossDimension) {
        if (waypoint == null || player == null) return null;
        Identifier currentDim = player.level().dimension().identifier();
        if (currentDim.equals(waypoint.dimension())) {
            return Vec3.atCenterOf(waypoint.pos());
        } else if (allowCrossDimension && isOverworldNetherPair(currentDim, waypoint.dimension())) {
            BlockPos converted = convertCoordinates(waypoint.pos(), waypoint.dimension(), currentDim);
            return Vec3.atCenterOf(converted);
        }
        return null;
    }

    public static Vec3 getAudioBeaconPosition(Vec3 targetPos, LocalPlayer player) {
        if (targetPos == null || player == null) return null;
        Vec3 eyePos = player.getEyePosition();
        Vec3 direction = targetPos.subtract(eyePos);
        double distance = direction.length();
        if (distance < 2.0) {
            return targetPos;
        }
        // Place sound 2 blocks away from player's head in the direction of the target
        return eyePos.add(direction.normalize().scale(2.0));
    }
}
