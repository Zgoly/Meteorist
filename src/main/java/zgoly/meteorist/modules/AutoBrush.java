package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import zgoly.meteorist.Meteorist;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoBrush extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("Range to search for suspicious sand and gravel.")
            .defaultValue(4)
            .min(1)
            .max(6)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Turns to the block itself. When off, you have to aim at it yourself.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing")
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

    private final Setting<Mode> filterMode = sgFilter.add(new EnumSetting.Builder<Mode>()
            .name("filter-mode")
            .description("None - brush everything; Whitelist - only brush blocks holding listed loot; Blacklist - skip blocks holding listed loot.")
            .defaultValue(Mode.None)
            .build()
    );
    private final Setting<List<Item>> whitelist = sgFilter.add(new ItemListSetting.Builder()
            .name("whitelist")
            .description("Only blocks holding these items or blocks are brushed. Blocks are items too, so minecraft:diamond_block works as well.")
            .visible(() -> filterMode.get() == Mode.Whitelist)
            .build()
    );
    private final Setting<List<Item>> blacklist = sgFilter.add(new ItemListSetting.Builder()
            .name("blacklist")
            .description("Blocks holding these items or blocks are skipped. Blocks are items too, so minecraft:diamond_block works as well.")
            .visible(() -> filterMode.get() == Mode.Blacklist)
            .build()
    );

    private final Setting<Boolean> renderTarget = sgRender.add(new BoolSetting.Builder()
            .name("render-target")
            .description("Renders the block that is currently being brushed.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the target render box.")
            .defaultValue(new SettingColor(255, 200, 0, 40))
            .visible(renderTarget::get)
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the target render box.")
            .defaultValue(new SettingColor(255, 200, 0, 150))
            .visible(renderTarget::get)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How to render the target box.")
            .defaultValue(ShapeMode.Both)
            .visible(renderTarget::get)
            .build()
    );
    private final Setting<Boolean> markRejected = sgRender.add(new BoolSetting.Builder()
            .name("mark-rejected")
            .description("Renders the blocks skipped by the loot filter.")
            .defaultValue(true)
            .visible(() -> filterMode.get() != Mode.None)
            .build()
    );
    private final Setting<SettingColor> rejectedSideColor = sgRender.add(new ColorSetting.Builder()
            .name("rejected-side-color")
            .description("The color of the sides of the skipped block render box.")
            .defaultValue(new SettingColor(255, 60, 60, 40))
            .visible(markRejected::get)
            .build()
    );
    private final Setting<SettingColor> rejectedLineColor = sgRender.add(new ColorSetting.Builder()
            .name("rejected-line-color")
            .description("The color of the lines of the skipped block render box.")
            .defaultValue(new SettingColor(255, 60, 60, 150))
            .visible(markRejected::get)
            .build()
    );

    public static boolean suspendRelease;
    private final Set<BlockPos> rejected = new HashSet<>();
    private BlockPos target;

    public AutoBrush() {
        super(Meteorist.CATEGORY, "auto-brush", "Automatically brushes suspicious sand and gravel with the brush. Idea by p80_sayshello.");
    }

    @Override
    public void onDeactivate() {
        releaseUse();
        target = null;
        InvUtils.swapBack();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (target != null && (!isBrushable(target) || !allowed(target))) target = null;
        if (target == null) target = findTarget();
        if (target == null) {
            releaseUse();
            return;
        }

        FindItemResult brush = InvUtils.findInHotbar(Items.BRUSH);
        if (!brush.found()) {
            releaseUse();
            return;
        }
        int slot = brush.slot();

        BlockPos pos = target;
        suspendRelease = true;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 10, true, () -> {
                if (!mc.player.isUsingItem()) use(pos, slot);
            });
        } else if (!mc.player.isUsingItem()) {
            use(pos, slot);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (markRejected.get()) {
            for (BlockPos pos : rejected) event.renderer.box(pos, rejectedSideColor.get(), rejectedLineColor.get(), ShapeMode.Both, 0);
        }

        if (target == null || !renderTarget.get()) return;

        event.renderer.box(target, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private void use(BlockPos pos, int slot) {
        InvUtils.swap(slot, swapBack.get());
        InteractionHand hand = slot == SlotUtils.OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        BlockUtils.interact(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), hand, swing.get());
    }

    private void releaseUse() {
        if (!suspendRelease) return;
        suspendRelease = false;
        mc.gameMode.releaseUsingItem(mc.player);
    }

    private BlockPos findTarget() {
        rejected.clear();

        if (!rotate.get()) {
            return mc.hitResult instanceof BlockHitResult hit && isBrushable(hit.getBlockPos()) && allowed(hit.getBlockPos())
                    ? hit.getBlockPos()
                    : null;
        }

        int r = range.get();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.withinManhattan(mc.player.blockPosition(), r, r, r)) {
            if (!isBrushable(pos)) continue;
            if (!allowed(pos)) {
                rejected.add(pos.immutable());
                continue;
            }

            double dist = pos.distToCenterSqr(mc.player.position());
            if (dist < bestDist) {
                bestDist = dist;
                best = pos.immutable();
            }
        }

        return best;
    }

    private boolean allowed(BlockPos pos) {
        if (filterMode.get() == Mode.None) return true;
        if (!(mc.level.getBlockEntity(pos) instanceof BrushableBlockEntity be) || be.getItem().isEmpty()) return true;

        if (filterMode.get() == Mode.Whitelist) return whitelist.get().contains(be.getItem().getItem());
        return !blacklist.get().contains(be.getItem().getItem());
    }

    private boolean isBrushable(BlockPos pos) {
        return pos != null && mc.level.getBlockState(pos).getBlock() instanceof BrushableBlock;
    }

    public enum Mode {
        None, Whitelist, Blacklist
    }
}
