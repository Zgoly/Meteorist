package zgoly.meteorist.utils;

import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteoristUtils {
    /// Determines if the given box collides with any entities.
    ///
    /// @param box the box to check for collisions
    /// @return true if the box collides with any entities, false otherwise
    public static boolean isCollidesEntity(AABB box) {
        if (mc.level == null) return false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity)) return false;
            if (box.intersects(entity.getBoundingBox())) return true;
        }
        return false;
    }

    /// Check if the given block position collides with any entities.
    ///
    /// @param blockPos the block position to check for collisions
    /// @return true if the block position collides with an entity, false otherwise
    public static boolean isCollidesEntity(BlockPos blockPos) {
        return isCollidesEntity(new AABB(blockPos));
    }

    /// Retrieves the target that the crosshair is currently pointing at within a given range.
    ///
    /// @param entity       the entity from which the crosshair originates
    /// @param range        the maximum range to the target
    /// @param ignoreBlocks determines whether blocks should be ignored when checking for targets
    /// @param filter       a predicate used to filter potential targets
    /// @return the hit result representing the target that the crosshair is pointing at, or null if no target is found
    public static HitResult getCrosshairTarget(Entity entity, double range, boolean ignoreBlocks, Predicate<Entity> filter) {
        if (entity == null || mc.level == null) return null;

        Vec3 vec3d = entity.getEyePosition(1);
        Vec3 vec3d2 = entity.getViewVector(1);
        Vec3 vec3d3 = vec3d.add(vec3d2.scale(range));
        AABB box = entity.getBoundingBox().expandTowards(vec3d2.scale(range)).inflate(1);

        ClipContext raycastContext = new ClipContext(vec3d, vec3d3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        HitResult hitResult = mc.level.clip(raycastContext);

        double e = range * range;
        if (!ignoreBlocks) e = hitResult.getLocation().distanceToSqr(vec3d);

        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(entity, vec3d, vec3d3, box, filter.and(targetEntity -> !targetEntity.isSpectator()), e);
        if (entityHitResult != null) {
            return entityHitResult;
        } else if (!ignoreBlocks) {
            return hitResult;
        }

        return null;
    }

    /// Removes invalid characters from the given string. Not fast, but cross-platform.
    ///
    /// @param text the string to remove invalid characters from
    /// @return the string with invalid characters removed
    public static String removeInvalidChars(final String text) {
        try {
            Paths.get(text);
            return text;
        } catch (final InvalidPathException e) {
            if (e.getInput() != null && !e.getInput().isEmpty() && e.getIndex() >= 0) {
                final StringBuilder stringBuilder = new StringBuilder(e.getInput());
                stringBuilder.deleteCharAt(e.getIndex());
                return removeInvalidChars(stringBuilder.toString());
            }
            throw e;
        }
    }

    /// Converts a given number of ticks into a human-readable time format.
    ///
    /// This method takes the number of ticks and converts it into hours, minutes, and seconds.
    /// It supports both short (e.g., `3h, 25m, 45s`) and long (e.g., `3 hours, 25 minutes, 45 seconds`)
    /// formats, and can optionally include formatting markers `(highlight)` and `(default)`.
    ///
    /// @param ticks       the number of ticks to convert (20 ticks = 1 second)
    /// @param shortFormat `true` to use short format like "3h, 25m, 45s"; `false` for long format like "3 hours, 25 minutes, 45 seconds"
    /// @param formatMsg   `true` to include "(highlight)" and "(default)" formatting markers; `false` for plain text
    /// @return a formatted string representing the time in hours, minutes, and seconds
    public static String ticksToTime(int ticks, boolean shortFormat, boolean formatMsg) {
        int ticksPerSecond = 20;
        int ticksPerMinute = ticksPerSecond * 60;
        int ticksPerHour = ticksPerMinute * 60;

        int hours = ticks / ticksPerHour;
        int minutes = (ticks % ticksPerHour) / ticksPerMinute;
        int seconds = (ticks % ticksPerMinute) / ticksPerSecond;

        if (hours == 0 && minutes == 0 && seconds == 0) {
            String highlight = formatMsg ? "(highlight)" : "";
            String def = formatMsg ? "(default)" : "";
            if (shortFormat) {
                return highlight + "0" + def + "s";
            } else {
                return highlight + "0" + def + " seconds";
            }
        }

        StringBuilder result = new StringBuilder();
        String highlight = formatMsg ? "(highlight)" : "";
        String def = formatMsg ? "(default)" : "";

        if (shortFormat) {
            if (hours > 0) {
                result.append(highlight).append(hours).append(def).append("h");
                if (minutes > 0 || seconds > 0) result.append(", ");
            }
            if (minutes > 0) {
                result.append(highlight).append(minutes).append(def).append("m");
                if (seconds > 0) result.append(", ");
            }
            if (seconds > 0) {
                result.append(highlight).append(seconds).append(def).append("s");
            }
        } else {
            if (hours > 0) {
                result.append(highlight).append(hours).append(def).append(" hour").append(hours > 1 ? "s" : "");
                if (minutes > 0 || seconds > 0) result.append(", ");
            }
            if (minutes > 0) {
                result.append(highlight).append(minutes).append(def).append(" minute").append(minutes > 1 ? "s" : "");
                if (seconds > 0) result.append(", ");
            }
            if (seconds > 0) {
                result.append(highlight).append(seconds).append(def).append(" second").append(seconds > 1 ? "s" : "");
            }
        }

        return result.toString();
    }

    /// Calculates the FOV angle in degrees between the player's look direction and the target.
    ///
    /// @param player the player entity
    /// @param target the target entity
    /// @return the FOV angle in degrees
    public static float calculateFov(LivingEntity player, Entity target) {
        Vec3 lookDirection = player.getViewVector(1.0F);
        Vec3 targetDirection = target.position().subtract(player.position()).normalize();

        return (float) Math.toDegrees(Math.acos(lookDirection.dot(targetDirection)));
    }

    /// Checks if the recipe can be displayed based on the crafting grid size.
    ///
    /// @param screenHandler The crafting screen handler providing the grid dimensions
    /// @param display       The recipe to check
    /// @return True if the recipe can be displayed in the current grid
    /// @see CraftingRecipeBookComponent
    public static boolean canDisplayRecipe(AbstractCraftingMenu screenHandler, RecipeDisplay display) {
        int width = screenHandler.getGridWidth();
        int height = screenHandler.getGridHeight();

        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return width >= shaped.width() && height >= shaped.height();
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            return width * height >= shapeless.ingredients().size();
        }

        return false;
    }
}
