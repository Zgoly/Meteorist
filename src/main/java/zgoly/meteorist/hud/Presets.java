package zgoly.meteorist.hud;

import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.value.Value;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import zgoly.meteorist.Meteorist;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Presets {
    public static final HudElementInfo<TextHud> INFO = new HudElementInfo<>(Meteorist.HUD_GROUP, "Meteorist", "Displays arbitrary text with Starscript.", Presets::create);

    public static final HudElementInfo<TextHud>.Preset FALL_DISTANCE;
    public static final HudElementInfo<TextHud>.Preset FALL_DAMAGE;

    static {
        FALL_DISTANCE = addPreset("Fall Distance", "Fall distance: #1{fall_distance}");
        FALL_DAMAGE = addPreset("Fall Damage", "Fall damage: #1{ceil((((fall_distance - 3) * (1 - fall_damage_reduce_strength / 100) / 2 > 0) ? (ceil(fall_distance - 3) * (1 - fall_damage_reduce_strength / 100) / 2) : 0) * 2) / 2} ❤");
    }

    private static TextHud create() {
        return new TextHud(INFO);
    }

    private static HudElementInfo<TextHud>.Preset addPreset(String title, String text) {
        return INFO.addPreset(title, textHud -> {
            if (text != null) textHud.text.set(text);
        });
    }

    static int maxDistance = 1024;
    static double fallDistance = 0;
    static Block block = Blocks.AIR;

    public static void starscriptAdd() {
        MeteorStarscript.ss.set("fall_distance", () -> {
            if (mc.player == null || mc.world == null) return Value.number(fallDistance);
            if (!mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().subtract(0, 0.01, 0))).isReplaceable()) {
                fallDistance = 0;
            } else {
                Vec3d pos = mc.player.getPos();
                double step = 1;
                Vec3d start = pos.add(0, 0, 0);
                Vec3d end = pos.subtract(0, maxDistance, 0);
                BlockHitResult result = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.WATER, mc.player));
                if (result != null) {
                    block = mc.world.getBlockState(result.getBlockPos()).getBlock();
                    double distance = start.distanceTo(result.getPos()) - step;
                    fallDistance = Math.max(fallDistance, distance);
                    return Value.number(fallDistance);
                }
            }
            return Value.number(fallDistance);
        });

        MeteorStarscript.ss.set("fall_damage_reduce_strength", () -> {
            if (mc.player == null || mc.world == null) return Value.number(0);
            double reduce = 0;

            for (ItemStack item : mc.player.getArmorItems()) {
                if (EnchantmentHelper.getLevel(Enchantments.PROTECTION, item) > 0) {
                    reduce = reduce + 4 * EnchantmentHelper.getLevel(Enchantments.PROTECTION, item);
                }
                if (EnchantmentHelper.getLevel(Enchantments.FEATHER_FALLING, item) > 0) {
                    reduce = reduce + 12 * EnchantmentHelper.getLevel(Enchantments.FEATHER_FALLING, item);
                }
            }
            reduce = Math.min(reduce, 80);

            if (block.equals(Blocks.WATER) || (block.equals(Blocks.SLIME_BLOCK) && !mc.player.isSneaking())) reduce = 100;
            return Value.number(Math.min(reduce, 100));
        });
    }
}