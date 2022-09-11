package zgoly.meteorist.hud;

import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.value.Value;
import net.minecraft.util.math.BlockPos;
import zgoly.meteorist.Meteorist;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Presets {
    public static final HudElementInfo<TextHud> INFO = new HudElementInfo<>(Meteorist.HUD_GROUP, "Meteorist", "Displays arbitrary text with Starscript.", Presets::create);

    public static final HudElementInfo<TextHud>.Preset FALL_DISTANCE;
    public static final HudElementInfo<TextHud>.Preset FALL_DAMAGE;



    static {
        FALL_DISTANCE = addPreset("Fall Distance", "Fall distance: #1{fall_distance}");
        FALL_DAMAGE = addPreset("Fall Damage", "Fall damage: #1{ceil((((fall_distance - 3) / 2) - (((fall_distance - 3) / 2) * (fall_damage_reduce_strength / 100))) * 2) / 2} ❤");
    }

    private static TextHud create() {
        return new TextHud(INFO);
    }

    private static HudElementInfo<TextHud>.Preset addPreset(String title, String text) {
        return INFO.addPreset(title, textHud -> {
            if (text != null) textHud.text.set(text);
        });
    }
    static int Y = -255;
    public static void starscriptAdd() {
        MeteorStarscript.ss.set("fall_distance", () -> {
            if (mc.player == null || mc.world == null) return Value.number(0);

            BlockPos pos = mc.player.getBlockPos();
            if (Y < pos.getY()) Y = pos.getY();
            else if (!mc.world.isAir(pos.down(1))) {
                Y = -255;
                return Value.number(0);
            }

            pos = new BlockPos(pos.getX(), Y, pos.getZ());
            int distance = 0;

            while (true) {
                assert mc.world != null;
                if (!mc.world.isAir(pos.down(1))) break;
                distance += 1;
                pos = pos.add(0, -1, 0);
            }

            return Value.number(distance);
        });

        MeteorStarscript.ss.set("fall_damage_reduce_strength", () -> {
            if (mc.player == null || mc.world == null) return Value.number(0);
            AtomicReference<Double> reduce = new AtomicReference<>((double) 0);

            mc.player.getArmorItems().forEach(item -> item.getEnchantments().forEach(enchantment -> {
                Pattern id = Pattern.compile("id:\"(.*)\"");
                Matcher matcherId = id.matcher(enchantment.toString());

                Pattern lvl = Pattern.compile("lvl:(.*)s");
                Matcher matcherLvl = lvl.matcher(enchantment.toString());

                if (matcherId.find()) {
                    if (Objects.equals(matcherId.group(1), "minecraft:protection")) {
                        if (matcherLvl.find()) {
                            reduce.updateAndGet(v -> v + 4 * Integer.parseInt(matcherLvl.group(1)));
                        }
                    }
                    if (Objects.equals(matcherId.group(1), "minecraft:feather_falling")) {
                        if (matcherLvl.find()) {
                            reduce.updateAndGet(v -> v + 12 * Integer.parseInt(matcherLvl.group(1)));
                        }
                    }
                }
            }));

            if (reduce.get() <= 80) {
                return Value.number(reduce.get());
            } else {
                return Value.number(80);
            }
        });
    }
}