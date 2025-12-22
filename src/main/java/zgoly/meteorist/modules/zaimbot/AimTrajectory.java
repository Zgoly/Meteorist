package zgoly.meteorist.modules.zaimbot;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import zgoly.meteorist.settings.ItemsSetting;

import java.util.List;

public class AimTrajectory implements ISerializable<AimTrajectory> {
    public final Settings settings = new Settings();

    SettingGroup sgAimTrajectory = settings.createGroup("Aim Trajectory");

    public final Setting<List<Item>> items = sgAimTrajectory.add(new ItemsSetting.Builder()
            .name("items")
            .description("The projectile items this trajectory applies to.")
            .defaultValue(List.of(Items.SNOWBALL, Items.EGG, Items.BLUE_EGG, Items.BROWN_EGG))
            .filter(item -> true)
            .build()
    );

    public final Setting<Double> gravity = sgAimTrajectory.add(new DoubleSetting.Builder()
            .name("gravity")
            .description("Gravity multiplier for this item's trajectory.")
            .defaultValue(1.0)
            .min(0.0)
            .sliderMax(5.0)
            .build()
    );

    public final Setting<Double> velocityScale = sgAimTrajectory.add(new DoubleSetting.Builder()
            .name("velocity-scale")
            .description("Base velocity scale for this item.")
            .defaultValue(0.6)
            .min(0.1)
            .sliderMax(10.0)
            .build()
    );

    public AimTrajectory() {}

    public AimTrajectory(List<Item> items, double gravity, double velocityScale) {
        this.items.set(items);
        this.gravity.set(gravity);
        this.velocityScale.set(velocityScale);
    }

    @Override
    public CompoundTag toTag() {
        return settings.toTag();
    }

    @Override
    public AimTrajectory fromTag(CompoundTag tag) {
        settings.fromTag(tag);
        return this;
    }

    public AimTrajectory copy() {
        return new AimTrajectory().fromTag(toTag());
    }
}