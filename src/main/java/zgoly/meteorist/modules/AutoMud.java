package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import zgoly.meteorist.Meteorist;

import java.util.function.Predicate;

public class AutoMud extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("Range to use water bottle.")
            .defaultValue(4)
            .min(0)
            .sliderRange(0, 10)
            .build()
    );
    private final Setting<Boolean> fillBottle = sgGeneral.add(new BoolSetting.Builder()
            .name("fill-bottle")
            .description("Fill bottle with water when there are no water bottles remaining.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Swing hand client-side.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Swap back when everything is done.")
            .defaultValue(true)
            .build()
    );

    public AutoMud() {
        super(Meteorist.CATEGORY, "auto-mud", "Automatically uses water bottle on dirt variants to get mud.");
    }

    private BlockPos findBlockPos(Predicate<BlockState> predicate) {
        for (BlockPos blockPos : BlockPos.iterateOutwards(mc.player.getBlockPos(), range.get(), range.get(), range.get())) {
            BlockState blockState = mc.world.getBlockState(blockPos);
            if (predicate.test(blockState)) {
                return blockPos;
            }
        }
        return null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        FindItemResult waterBottle = InvUtils.findInHotbar(itemStack -> itemStack.getComponents().getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).matches(Potions.WATER));
        FindItemResult emptyBottle = InvUtils.findInHotbar(Items.GLASS_BOTTLE);

        if (waterBottle.found()) {
            BlockPos target = findBlockPos(blockState -> blockState.isIn(BlockTags.CONVERTABLE_TO_MUD));
            if (target == null) return;

            InvUtils.swap(waterBottle.slot(), swapBack.get());
            BlockUtils.interact(new BlockHitResult(Utils.vec3d(target), Direction.UP, target, false), Hand.MAIN_HAND, swingHand.get());
        } else if (emptyBottle.found() && fillBottle.get()) {
            BlockPos target = findBlockPos(blockState -> blockState.getFluidState().isStill());
            if (target == null) return;

            InvUtils.swap(emptyBottle.slot(), swapBack.get());
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 10, true, () -> {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            });
        }
    }
}
