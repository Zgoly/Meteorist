package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Vector3d;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.mixin.MobSpawnerLogicAccessor;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NerdVision extends Module {
    private final SettingGroup sgSpawn = settings.createGroup("Spawn");
    private final SettingGroup sgTurtleEggs = settings.createGroup("Turtle Eggs");
    private final SettingGroup sgSpawners = settings.createGroup("Spawners");
    private final SettingGroup sgIronGolems = settings.createGroup("Iron Golems");

    private final Setting<Boolean> spawnRangesEnabled = sgSpawn.add(new BoolSetting.Builder()
            .name("spawn-ranges-enabled")
            .description("Enables spawn range rendering.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SphereType> spawnRangesSphereType = sgSpawn.add(new EnumSetting.Builder<SphereType>()
            .name("spawn-range-sphere-type")
            .description("Choose how spawn ranges are visualized: default spherical or cubic grid representation.")
            .defaultValue(SphereType.Default)
            .visible(spawnRangesEnabled::get)
            .build()
    );
    private final Setting<CenterPositionType> centerPositionType = sgSpawn.add(new EnumSetting.Builder<CenterPositionType>()
            .name("center-position-type")
            .description("Selects the type of position for the center.")
            .defaultValue(CenterPositionType.Pos)
            .visible(() -> spawnRangesEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Vector3d> centerPosition = sgSpawn.add(new Vector3dSetting.Builder()
            .name("center-position")
            .description("Sets the position for the center.")
            .defaultValue(new Vector3d(0, 0, 0))
            .visible(() -> spawnRangesEnabled.get() && centerPositionType.get() == CenterPositionType.Pos)
            .build()
    );
    private final Setting<BlockPos> centerBlockPosition = sgSpawn.add(new BlockPosSetting.Builder()
            .name("center-block-position")
            .description("Sets the block position for the center.")
            .defaultValue(new BlockPos(0, 0, 0))
            .visible(() -> spawnRangesEnabled.get() && centerPositionType.get() == CenterPositionType.BlockPos)
            .build()
    );
    private final Setting<Integer> spawnRangeSpherePhiCount = sgSpawn.add(new IntSetting.Builder()
            .name("spawn-range-sphere-phi-count")
            .description("Sets the sphere phi count for the spawn range.")
            .defaultValue(16)
            .min(4)
            .sliderRange(4, 128)
            .visible(() -> spawnRangesEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> spawnRangeSphereThetaCount = sgSpawn.add(new IntSetting.Builder()
            .name("spawn-range-sphere-theta-count")
            .description("Sets the sphere theta count for the spawn range.")
            .defaultValue(16)
            .min(4)
            .sliderRange(4, 128)
            .visible(() -> spawnRangesEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> idleRangeSpherePhiCount = sgSpawn.add(new IntSetting.Builder()
            .name("idle-range-sphere-phi-count")
            .description("Sets the sphere phi count for the idle range.")
            .defaultValue(24)
            .min(4)
            .sliderRange(4, 128)
            .visible(() -> spawnRangesEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> idleRangeSphereThetaCount = sgSpawn.add(new IntSetting.Builder()
            .name("idle-range-sphere-theta-count")
            .description("Sets the sphere theta count for the idle range.")
            .defaultValue(24)
            .min(4)
            .sliderRange(4, 128)
            .visible(() -> spawnRangesEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> despawnRangeSpherePhiCount = sgSpawn.add(new IntSetting.Builder()
            .name("despawn-range-sphere-phi-count")
            .description("Sets the sphere phi count for the despawn range.")
            .defaultValue(32)
            .min(4)
            .sliderRange(4, 128)
            .visible(() -> spawnRangesEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> despawnRangeSphereThetaCount = sgSpawn.add(new IntSetting.Builder()
            .name("despawn-range-sphere-theta-count")
            .description("Sets the sphere theta count for the despawn range.")
            .defaultValue(32)
            .min(4)
            .sliderRange(4, 128)
            .visible(() -> spawnRangesEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<CenterPositionMode> centerPositionMode = sgSpawn.add(new EnumSetting.Builder<CenterPositionMode>()
            .name("center-position-mode")
            .description("Determines how the center is calculated: relative to the player or absolute.")
            .defaultValue(CenterPositionMode.Relative)
            .visible(spawnRangesEnabled::get)
            .build()
    );
    private final Setting<Boolean> displayCenterPosition = sgSpawn.add(new BoolSetting.Builder()
            .name("display-center-position")
            .description("Displays the center position indicator.")
            .defaultValue(true)
            .visible(spawnRangesEnabled::get)
            .build()
    );
    private final Setting<Vector3d> centerPositionSize = sgSpawn.add(new Vector3dSetting.Builder()
            .name("center-position-size")
            .description("Sets the size of the center position indicator.")
            .defaultValue(new Vector3d(1, 0, 1))
            .visible(() -> spawnRangesEnabled.get() && displayCenterPosition.get())
            .build()
    );
    private final Setting<ShapeMode> centerPositionShapeMode = sgSpawn.add(new EnumSetting.Builder<ShapeMode>()
            .name("center-position-shape-mode")
            .description("Sets the shape mode for the center position.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnRangesEnabled.get() && displayCenterPosition.get())
            .build()
    );
    private final Setting<SettingColor> centerPositionSideColor = sgSpawn.add(new ColorSetting.Builder()
            .name("center-position-side-color")
            .description("Sets the center position side color.")
            .defaultValue(new SettingColor(0, 255, 255, 1))
            .visible(() -> spawnRangesEnabled.get() && displayCenterPosition.get())
            .build()
    );
    private final Setting<SettingColor> centerPositionLineColor = sgSpawn.add(new ColorSetting.Builder()
            .name("center-position-line-color")
            .description("Sets the center position line color.")
            .defaultValue(new SettingColor(0, 255, 255, 255))
            .visible(() -> spawnRangesEnabled.get() && displayCenterPosition.get())
            .build()
    );
    private final Setting<Boolean> spawnRangeEnabled = sgSpawn.add(new BoolSetting.Builder()
            .name("spawn-range-enabled")
            .description("Enables spawn range rendering.")
            .defaultValue(true)
            .visible(spawnRangesEnabled::get)
            .build()
    );
    private final Setting<Double> spawnSphereRange = sgSpawn.add(new DoubleSetting.Builder()
            .name("spawn-sphere-range")
            .description("Sets the spawn sphere range.")
            .defaultValue(24)
            .min(0)
            .sliderRange(0, 24)
            .visible(() -> spawnRangesEnabled.get() && spawnRangeEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> spawnBlockSphereRange = sgSpawn.add(new IntSetting.Builder()
            .name("spawn-block-sphere-range")
            .description("Sets the spawn sphere range.")
            .defaultValue(24)
            .min(1)
            .sliderRange(1, 24)
            .visible(() -> spawnRangesEnabled.get() && spawnRangeEnabled.get() && spawnRangesSphereType.get() == SphereType.Cubic)
            .build()
    );
    private final Setting<ShapeMode> spawnRangeShapeMode = sgSpawn.add(new EnumSetting.Builder<ShapeMode>()
            .name("spawn-range-shape-mode")
            .description("Sets the shape mode for the spawn range.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnRangesEnabled.get() && spawnRangeEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnRangeSideColor = sgSpawn.add(new ColorSetting.Builder()
            .name("spawn-range-side-color")
            .description("Sets the spawn range side color.")
            .defaultValue(new SettingColor(255, 0, 0, 10))
            .visible(() -> spawnRangesEnabled.get() && spawnRangeEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnRangeLineColor = sgSpawn.add(new ColorSetting.Builder()
            .name("spawn-range-line-color")
            .description("Sets the spawn range line color.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(() -> spawnRangesEnabled.get() && spawnRangeEnabled.get())
            .build()
    );
    private final Setting<Boolean> idleRangeEnabled = sgSpawn.add(new BoolSetting.Builder()
            .name("idle-range-enabled")
            .description("Enables idle range rendering.")
            .defaultValue(true)
            .visible(spawnRangesEnabled::get)
            .build()
    );
    private final Setting<Double> idleSphereRange = sgSpawn.add(new DoubleSetting.Builder()
            .name("idle-sphere-range")
            .description("Sets the idle sphere range.")
            .defaultValue(32)
            .min(0)
            .sliderRange(0, 32)
            .visible(() -> spawnRangesEnabled.get() && idleRangeEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> idleBlockSphereRange = sgSpawn.add(new IntSetting.Builder()
            .name("idle-block-sphere-range")
            .description("Sets the idle sphere range.")
            .defaultValue(32)
            .min(1)
            .sliderRange(1, 32)
            .visible(() -> spawnRangesEnabled.get() && idleRangeEnabled.get() && spawnRangesSphereType.get() == SphereType.Cubic)
            .build()
    );
    private final Setting<ShapeMode> idleRangeShapeMode = sgSpawn.add(new EnumSetting.Builder<ShapeMode>()
            .name("idle-range-shape-mode")
            .description("Sets the shape mode for the idle range.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnRangesEnabled.get() && idleRangeEnabled.get())
            .build()
    );
    private final Setting<SettingColor> idleRangeSideColor = sgSpawn.add(new ColorSetting.Builder()
            .name("idle-range-side-color")
            .description("Sets the idle range side color.")
            .defaultValue(new SettingColor(255, 255, 0, 10))
            .visible(() -> spawnRangesEnabled.get() && idleRangeEnabled.get())
            .build()
    );
    private final Setting<SettingColor> idleRangeLineColor = sgSpawn.add(new ColorSetting.Builder()
            .name("idle-range-line-color")
            .description("Sets the idle range line color.")
            .defaultValue(new SettingColor(255, 255, 0, 255))
            .visible(() -> spawnRangesEnabled.get() && idleRangeEnabled.get())
            .build()
    );
    private final Setting<Boolean> despawnRangeEnabled = sgSpawn.add(new BoolSetting.Builder()
            .name("despawn-range-enabled")
            .description("Enables despawn range rendering.")
            .defaultValue(true)
            .visible(spawnRangesEnabled::get)
            .build()
    );
    private final Setting<Double> despawnSphereRange = sgSpawn.add(new DoubleSetting.Builder()
            .name("despawn-sphere-range")
            .description("Sets the despawn sphere range.")
            .defaultValue(128)
            .min(0)
            .sliderRange(0, 128)
            .visible(() -> spawnRangesEnabled.get() && despawnRangeEnabled.get() && spawnRangesSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> despawnBlockSphereRange = sgSpawn.add(new IntSetting.Builder()
            .name("despawn-block-sphere-range")
            .description("Sets the despawn sphere range.")
            .defaultValue(128)
            .min(1)
            .sliderRange(1, 128)
            .visible(() -> spawnRangesEnabled.get() && despawnRangeEnabled.get() && spawnRangesSphereType.get() == SphereType.Cubic)
            .build()
    );
    private final Setting<ShapeMode> despawnRangeShapeMode = sgSpawn.add(new EnumSetting.Builder<ShapeMode>()
            .name("despawn-range-shape-mode")
            .description("Sets the shape mode for the despawn range.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnRangesEnabled.get() && despawnRangeEnabled.get())
            .build()
    );
    private final Setting<SettingColor> despawnRangeSideColor = sgSpawn.add(new ColorSetting.Builder()
            .name("despawn-range-side-color")
            .description("Sets the despawn range side color.")
            .defaultValue(new SettingColor(0, 255, 0, 10))
            .visible(() -> spawnRangesEnabled.get() && despawnRangeEnabled.get())
            .build()
    );
    private final Setting<SettingColor> despawnRangeLineColor = sgSpawn.add(new ColorSetting.Builder()
            .name("despawn-range-line-color")
            .description("Sets the despawn range line color.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .visible(() -> spawnRangesEnabled.get() && despawnRangeEnabled.get())
            .build()
    );

    private final Setting<Boolean> turtleEggsRenderEnabled = sgTurtleEggs.add(new BoolSetting.Builder()
            .name("enabled")
            .description("Enables turtle eggs rendering.")
            .defaultValue(false)
            .onChanged((value) -> onActivate())
            .build()
    );
    private final Setting<Vector3d> turtleEggsRangeSize = sgTurtleEggs.add(new Vector3dSetting.Builder()
            .name("range-size")
            .description("Sets the size of the turtle eggs range.")
            .defaultValue(new Vector3d(47, 7, 47))
            .visible(turtleEggsRenderEnabled::get)
            .build()
    );
    private final Setting<Boolean> turtleEggsRangeEnabled = sgTurtleEggs.add(new BoolSetting.Builder()
            .name("range-enabled")
            .description("Enables turtle eggs range rendering.")
            .defaultValue(true)
            .onChanged((value) -> onActivate())
            .visible(turtleEggsRenderEnabled::get)
            .build()
    );
    private final Setting<ShapeMode> turtleEggsRangeShapeMode = sgTurtleEggs.add(new EnumSetting.Builder<ShapeMode>()
            .name("range-shape-mode")
            .description("Sets the shape mode for the turtle eggs range.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> turtleEggsRangeEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> turtleEggsRangeSideColor = sgTurtleEggs.add(new ColorSetting.Builder()
            .name("range-side-color")
            .description("Sets the turtle eggs range side color.")
            .defaultValue(new SettingColor(255, 255, 255, 10))
            .visible(() -> turtleEggsRangeEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> turtleEggsRangeLineColor = sgTurtleEggs.add(new ColorSetting.Builder()
            .name("range-line-color")
            .description("Sets the turtle eggs range line color.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> turtleEggsRangeEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<Boolean> turtleEggsZombiesRangeEnabled = sgTurtleEggs.add(new BoolSetting.Builder()
            .name("zombies-range-enabled")
            .description("Enables turtle eggs zombies range rendering.")
            .defaultValue(false)
            .visible(turtleEggsRenderEnabled::get)
            .build()
    );
    private final Setting<ShapeMode> turtleEggsZombiesRangeShapeMode = sgTurtleEggs.add(new EnumSetting.Builder<ShapeMode>()
            .name("zombies-range-shape-mode")
            .description("Sets the shape mode for the turtle eggs zombies range.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> turtleEggsZombiesRangeEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> turtleEggsZombiesRangeSideColor = sgTurtleEggs.add(new ColorSetting.Builder()
            .name("zombies-range-side-color")
            .description("Sets the turtle eggs zombies range side color.")
            .defaultValue(new SettingColor(255, 0, 0, 10))
            .visible(() -> turtleEggsZombiesRangeEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> turtleEggsZombiesRangeLineColor = sgTurtleEggs.add(new ColorSetting.Builder()
            .name("zombies-range-line-color")
            .description("Sets the turtle eggs zombies range line color.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(() -> turtleEggsZombiesRangeEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<Boolean> turtleEggsAggressionRenderEnabled = sgTurtleEggs.add(new BoolSetting.Builder()
            .name("aggression-render-enabled")
            .description("Enables turtle eggs aggression render.")
            .defaultValue(true)
            .onChanged((value) -> onActivate())
            .visible(turtleEggsRenderEnabled::get)
            .build()
    );
    private final Setting<Boolean> turtleEggsAggressionEggRenderEnabled = sgTurtleEggs.add(new BoolSetting.Builder()
            .name("aggression-egg-render-enabled")
            .description("Enables turtle eggs aggression egg render.")
            .defaultValue(true)
            .visible(() -> turtleEggsAggressionRenderEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<ShapeMode> turtleEggsAggressionEggRenderShapeMode = sgTurtleEggs.add(new EnumSetting.Builder<ShapeMode>()
            .name("aggression-egg-render-shape-mode")
            .description("Sets the shape mode for the turtle eggs aggression render.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> turtleEggsAggressionEggRenderEnabled.get() && turtleEggsAggressionRenderEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> turtleEggsAggressionEggRenderSideColor = sgTurtleEggs.add(new ColorSetting.Builder()
            .name("aggression-egg-render-side-color")
            .description("Sets the turtle eggs aggression render side color.")
            .defaultValue(new SettingColor(255, 255, 0, 10))
            .visible(() -> turtleEggsAggressionEggRenderEnabled.get() && turtleEggsAggressionRenderEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<Boolean> turtleEggsAggressionZombieRenderEnabled = sgTurtleEggs.add(new BoolSetting.Builder()
            .name("aggression-zombie-render-enabled")
            .description("Enables turtle eggs aggression zombie render.")
            .defaultValue(true)
            .visible(() -> turtleEggsAggressionRenderEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> turtleEggsAggressionEggRenderLineColor = sgTurtleEggs.add(new ColorSetting.Builder()
            .name("aggression-egg-render-line-color")
            .description("Sets the turtle eggs aggression render line color.")
            .defaultValue(new SettingColor(255, 255, 0, 255))
            .visible(() -> turtleEggsAggressionEggRenderEnabled.get() && turtleEggsAggressionRenderEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<ShapeMode> turtleEggsAggressionZombieRenderShapeMode = sgTurtleEggs.add(new EnumSetting.Builder<ShapeMode>()
            .name("aggression-zombie-render-shape-mode")
            .description("Sets the shape mode for the turtle eggs aggression render.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> turtleEggsAggressionZombieRenderEnabled.get() && turtleEggsAggressionRenderEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> turtleEggsAggressionZombieRenderSideColor = sgTurtleEggs.add(new ColorSetting.Builder()
            .name("aggression-zombie-render-side-color")
            .description("Sets the turtle eggs aggression render side color.")
            .defaultValue(new SettingColor(255, 0, 0, 10))
            .visible(() -> turtleEggsAggressionZombieRenderEnabled.get() && turtleEggsAggressionRenderEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> turtleEggsAggressionZombieRenderLineColor = sgTurtleEggs.add(new ColorSetting.Builder()
            .name("aggression-zombie-render-line-color")
            .description("Sets the turtle eggs aggression render line color.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(() -> turtleEggsAggressionZombieRenderEnabled.get() && turtleEggsAggressionRenderEnabled.get() && turtleEggsRenderEnabled.get())
            .build()
    );

    private final Setting<Boolean> spawnersRenderEnabled = sgSpawners.add(new BoolSetting.Builder()
            .name("enabled")
            .description("Enables spawners rendering.")
            .defaultValue(false)
            .onChanged((value) -> onActivate())
            .build()
    );
    private final Setting<Boolean> spawnersRenderSphereEnabled = sgSpawners.add(new BoolSetting.Builder()
            .name("render-sphere-enabled")
            .description("Enables spawners render sphere rendering.")
            .defaultValue(true)
            .visible(spawnersRenderEnabled::get)
            .build()
    );
    private final Setting<SphereType> spawnersRenderSphereType = sgSpawners.add(new EnumSetting.Builder<SphereType>()
            .name("render-sphere-visualization-type")
            .description("Choose how spawners render spheres are visualized: default spherical or cubic grid representation.")
            .defaultValue(SphereType.Default)
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderSphereEnabled.get())
            .build()
    );
    private final Setting<Integer> spawnersRenderSpherePhiCount = sgSpawners.add(new IntSetting.Builder()
            .name("render-sphere-phi-count")
            .description("Sets the sphere phi count for the spawners render sphere.")
            .defaultValue(16)
            .min(4)
            .sliderRange(4, 128)
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderSphereEnabled.get() && spawnersRenderSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<Integer> spawnersRenderSphereThetaCount = sgSpawners.add(new IntSetting.Builder()
            .name("render-sphere-theta-count")
            .description("Sets the sphere theta count for the spawners render sphere.")
            .defaultValue(16)
            .min(4)
            .sliderRange(4, 128)
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderSphereEnabled.get() && spawnersRenderSphereType.get() == SphereType.Default)
            .build()
    );
    private final Setting<ShapeMode> spawnersRenderSphereShapeMode = sgSpawners.add(new EnumSetting.Builder<ShapeMode>()
            .name("render-sphere-shape-mode")
            .description("Sets the shape mode for the spawners render sphere.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderSphereEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderSphereSideColor = sgSpawners.add(new ColorSetting.Builder()
            .name("render-sphere-side-color")
            .description("Sets the spawners render sphere side color.")
            .defaultValue(new SettingColor(0, 255, 0, 10))
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderSphereEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderSphereLineColor = sgSpawners.add(new ColorSetting.Builder()
            .name("render-sphere-line-color")
            .description("Sets the spawners render sphere line color.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderSphereEnabled.get())
            .build()
    );
    private final Setting<Boolean> spawnersRenderDetectingZoneEnabled = sgSpawners.add(new BoolSetting.Builder()
            .name("detecting-zone-enabled")
            .description("Enables spawners detecting zone rendering.")
            .defaultValue(true)
            .visible(spawnersRenderEnabled::get)
            .build()
    );
    private final Setting<ShapeMode> spawnersRenderDetectingZoneShapeMode = sgSpawners.add(new EnumSetting.Builder<ShapeMode>()
            .name("detecting-zone-shape-mode")
            .description("Sets the shape mode for the spawners detecting zone.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnersRenderDetectingZoneEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderDetectingZoneSideColor = sgSpawners.add(new ColorSetting.Builder()
            .name("detecting-zone-side-color")
            .description("Sets the spawners detecting zone side color.")
            .defaultValue(new SettingColor(255, 255, 255, 10))
            .visible(() -> spawnersRenderDetectingZoneEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderDetectingZoneLineColor = sgSpawners.add(new ColorSetting.Builder()
            .name("detecting-zone-line-color")
            .description("Sets the spawners detecting zone line color.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> spawnersRenderDetectingZoneEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<Boolean> spawnersRenderSpawningZoneEnabled = sgSpawners.add(new BoolSetting.Builder()
            .name("spawning-zone-enabled")
            .description("Enables spawners spawning zone rendering.")
            .defaultValue(true)
            .visible(spawnersRenderEnabled::get)
            .build()
    );
    private final Setting<Boolean> spawnersRenderSpawningZoneRounding = sgSpawners.add(new BoolSetting.Builder()
            .name("spawning-zone-rounding")
            .description("Round spawning zone size to block size.")
            .defaultValue(false)
            .visible(() -> spawnersRenderSpawningZoneEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<ShapeMode> spawnersRenderSpawningZoneShapeMode = sgSpawners.add(new EnumSetting.Builder<ShapeMode>()
            .name("spawning-zone-shape-mode")
            .description("Sets the shape mode for the spawners spawning zone.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnersRenderSpawningZoneEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderSpawningZoneSideColor = sgSpawners.add(new ColorSetting.Builder()
            .name("spawning-zone-side-color")
            .description("Sets the spawners spawning zone side color.")
            .defaultValue(new SettingColor(255, 0, 0, 10))
            .visible(() -> spawnersRenderSpawningZoneEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderSpawningZoneLineColor = sgSpawners.add(new ColorSetting.Builder()
            .name("spawning-zone-line-color")
            .description("Sets the spawners spawning zone line color.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(() -> spawnersRenderSpawningZoneEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<Boolean> spawnersRenderActiveSpawnerEnabled = sgSpawners.add(new BoolSetting.Builder()
            .name("active-spawner-enabled")
            .description("Enables spawners active spawner rendering.")
            .defaultValue(true)
            .visible(spawnersRenderEnabled::get)
            .build()
    );
    private final Setting<ShapeMode> spawnersRenderActiveSpawnerShapeMode = sgSpawners.add(new EnumSetting.Builder<ShapeMode>()
            .name("active-spawner-shape-mode")
            .description("Sets the shape mode for the spawners active spawner.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnersRenderActiveSpawnerEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderActiveSpawnerSideColor = sgSpawners.add(new ColorSetting.Builder()
            .name("active-spawner-side-color")
            .description("Sets the spawners active spawner side color.")
            .defaultValue(new SettingColor(0, 255, 0, 10))
            .visible(() -> spawnersRenderActiveSpawnerEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderActiveSpawnerLineColor = sgSpawners.add(new ColorSetting.Builder()
            .name("active-spawner-line-color")
            .description("Sets the spawners active spawner line color.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .visible(() -> spawnersRenderActiveSpawnerEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<Boolean> spawnersRenderInactiveSpawnerEnabled = sgSpawners.add(new BoolSetting.Builder()
            .name("inactive-spawner-enabled")
            .description("Enables spawners inactive spawner rendering.")
            .defaultValue(true)
            .visible(spawnersRenderEnabled::get)
            .build()
    );
    private final Setting<ShapeMode> spawnersRenderInactiveSpawnerShapeMode = sgSpawners.add(new EnumSetting.Builder<ShapeMode>()
            .name("inactive-spawner-shape-mode")
            .description("Sets the shape mode for the spawners inactive spawner.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> spawnersRenderInactiveSpawnerEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderInactiveSpawnerSideColor = sgSpawners.add(new ColorSetting.Builder()
            .name("inactive-spawner-side-color")
            .description("Sets the spawners inactive spawner side color.")
            .defaultValue(new SettingColor(255, 0, 0, 10))
            .visible(() -> spawnersRenderInactiveSpawnerEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> spawnersRenderInactiveSpawnerLineColor = sgSpawners.add(new ColorSetting.Builder()
            .name("inactive-spawner-line-color")
            .description("Sets the spawners inactive spawner line color.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(() -> spawnersRenderInactiveSpawnerEnabled.get() && spawnersRenderEnabled.get())
            .build()
    );
    private final Setting<Boolean> spawnersRenderDebugInfoEnabled = sgSpawners.add(new BoolSetting.Builder()
            .name("render-debug-info-enabled")
            .description("Enables debug info rendering for the spawners render.")
            .defaultValue(true)
            .visible(spawnersRenderEnabled::get)
            .build()
    );
    private final Setting<Vector3d> spawnersRenderDebugInfoOffset = sgSpawners.add(new Vector3dSetting.Builder()
            .name("render-debug-info-offset")
            .description("Sets the spawners render debug info offset.")
            .defaultValue(new Vector3d(0, 0, 0))
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderDebugInfoEnabled.get())
            .build()
    );
    private final Setting<Double> spawnersRenderDebugInfoSize = sgSpawners.add(new DoubleSetting.Builder()
            .name("render-debug-info-size")
            .description("Sets the spawners render debug info size.")
            .defaultValue(1)
            .min(0)
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderDebugInfoEnabled.get())
            .build()
    );
    private final Setting<Double> spawnersRenderDebugInfoMargin = sgSpawners.add(new DoubleSetting.Builder()
            .name("render-debug-info-margin")
            .description("Sets the vertical margin between debug info pairs.")
            .defaultValue(1)
            .min(0)
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderDebugInfoEnabled.get())
            .build()
    );
    private final Setting<List<String>> spawnersRenderDebugInfoAllowedKeys = sgSpawners.add(new StringListSetting.Builder()
            .name("render-debug-info-allowed-keys")
            .description("Sets the spawners render debug info allowed keys.")
            .defaultValue(List.of("Delay", "RequiredPlayerRange"))
            .visible(() -> spawnersRenderEnabled.get() && spawnersRenderDebugInfoEnabled.get())
            .build()
    );

    private final Setting<Boolean> ironGolemsRenderEnabled = sgIronGolems.add(new BoolSetting.Builder()
            .name("enabled")
            .description("Enables iron golems rendering.")
            .defaultValue(false)
            .onChanged((value) -> onActivate())
            .build()
    );
    private final Setting<Boolean> ironGolemsRenderDetectingZoneEnabled = sgIronGolems.add(new BoolSetting.Builder()
            .name("detecting-zone-enabled")
            .description("Enables iron golems detecting zone rendering.")
            .defaultValue(true)
            .visible(ironGolemsRenderEnabled::get)
            .build()
    );
    private final Setting<Vector3d> ironGolemsRenderDetectingZoneSize = sgIronGolems.add(new Vector3dSetting.Builder()
            .name("detecting-zone-size")
            .description("Sets the iron golems detecting zone size.")
            .defaultValue(new Vector3d(32, 32, 32))
            .visible(() -> ironGolemsRenderDetectingZoneEnabled.get() && ironGolemsRenderEnabled.get())
            .build()
    );
    private final Setting<ShapeMode> ironGolemsRenderDetectingZoneShapeMode = sgIronGolems.add(new EnumSetting.Builder<ShapeMode>()
            .name("detecting-zone-shape-mode")
            .description("Sets the shape mode for the iron golems detecting zone.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> ironGolemsRenderDetectingZoneEnabled.get() && ironGolemsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> ironGolemsRenderDetectingZoneSideColor = sgIronGolems.add(new ColorSetting.Builder()
            .name("detecting-zone-side-color")
            .description("Sets the iron golems detecting zone side color.")
            .defaultValue(new SettingColor(255, 255, 255, 10))
            .visible(() -> ironGolemsRenderDetectingZoneEnabled.get() && ironGolemsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> ironGolemsRenderDetectingZoneLineColor = sgIronGolems.add(new ColorSetting.Builder()
            .name("detecting-zone-line-color")
            .description("Sets the iron golems detecting zone line color.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> ironGolemsRenderDetectingZoneEnabled.get() && ironGolemsRenderEnabled.get())
            .build()
    );
    private final Setting<Boolean> ironGolemsRenderSpawningZoneEnabled = sgIronGolems.add(new BoolSetting.Builder()
            .name("spawning-zone-enabled")
            .description("Enables iron golems spawning zone rendering.")
            .defaultValue(true)
            .visible(ironGolemsRenderEnabled::get)
            .build()
    );
    private final Setting<Vector3d> ironGolemsRenderSpawningZoneSize = sgIronGolems.add(new Vector3dSetting.Builder()
            .name("spawning-zone-size")
            .description("Sets the iron golems spawning zone size.")
            .defaultValue(new Vector3d(17, 13, 17))
            .visible(() -> ironGolemsRenderSpawningZoneEnabled.get() && ironGolemsRenderEnabled.get())
            .build()
    );
    private final Setting<ShapeMode> ironGolemsRenderSpawningZoneShapeMode = sgIronGolems.add(new EnumSetting.Builder<ShapeMode>()
            .name("spawning-zone-shape-mode")
            .description("Sets the shape mode for the iron golems spawning zone.")
            .defaultValue(ShapeMode.Both)
            .visible(() -> ironGolemsRenderSpawningZoneEnabled.get() && ironGolemsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> ironGolemsRenderSpawningZoneSideColor = sgIronGolems.add(new ColorSetting.Builder()
            .name("spawning-zone-side-color")
            .description("Sets the iron golems spawning zone side color.")
            .defaultValue(new SettingColor(0, 255, 0, 10))
            .visible(() -> ironGolemsRenderSpawningZoneEnabled.get() && ironGolemsRenderEnabled.get())
            .build()
    );
    private final Setting<SettingColor> ironGolemsRenderSpawningZoneLineColor = sgIronGolems.add(new ColorSetting.Builder()
            .name("spawning-zone-line-color")
            .description("Sets the iron golems spawning zone line color.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .visible(() -> ironGolemsRenderSpawningZoneEnabled.get() && ironGolemsRenderEnabled.get())
            .build()
    );

    private static final List<NbtDataDisplay> nbtDataDisplays = new ArrayList<>();
    private final ExecutorService workerThread = Executors.newSingleThreadExecutor();
    private final Set<Block> blocksToTrack = new HashSet<>();
    private final Set<BlockPos> trackedBlocks = new HashSet<>();
    private DimensionType lastDimension;

    public NerdVision() {
        super(Meteorist.CATEGORY, "nerd-vision", "Allows you to visualize various game mechanics/farms.");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!nbtDataDisplays.isEmpty()) {
            for (NbtDataDisplay display : nbtDataDisplays) {

                if (!NametagUtils.to2D(display.pos, display.size)) continue;

                NametagUtils.begin(display.pos);

                TextRenderer text = TextRenderer.get();
                text.begin(display.size);

                String longestPair = "";
                String separator = ": ";

                for (Map.Entry<String, String> entry : display.attributes.entrySet()) {
                    String pair = entry.getKey() + separator + entry.getValue();
                    if (text.getWidth(pair) > text.getWidth(longestPair)) {
                        longestPair = pair;
                    }
                }

                double startX = -text.getWidth(longestPair) / 2;

                double totalHeight = 0;
                for (Map.Entry<String, String> ignored : display.attributes.entrySet()) {
                    totalHeight += text.getHeight();
                }
                totalHeight += (display.attributes.size() - 1) * display.margin;

                double yOffset = -totalHeight / 2;
                for (Map.Entry<String, String> entry : display.attributes.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    text.render(key + separator, startX, yOffset, Color.GREEN, true);
                    text.render(value, startX + text.getWidth(key + separator), yOffset, Color.CYAN, true);

                    yOffset += text.getHeight() + display.margin;
                }

                text.end();
                NametagUtils.end();
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        // YandereDev will be proud of me
        // TODO: Rewrite to something readable

        nbtDataDisplays.clear();

        mc.world.getEntities().forEach(entity -> {
            if (turtleEggsRenderEnabled.get()) {
                if (entity instanceof ZombieEntity && turtleEggsZombiesRangeEnabled.get()) {
                    Vector3d size = turtleEggsRangeSize.get();
                    event.renderer.box(Box.of(entity.getEntityPos(), size.x, size.y, size.z), turtleEggsZombiesRangeSideColor.get(), turtleEggsZombiesRangeLineColor.get(), turtleEggsZombiesRangeShapeMode.get(), 0);
                }
            }

            if (ironGolemsRenderEnabled.get()) {
                if (entity instanceof VillagerEntity villager) {
                    if (ironGolemsRenderDetectingZoneEnabled.get()) {
                        Vector3d size = ironGolemsRenderDetectingZoneSize.get();
                        Box box = Box.of(villager.getEntityPos(), size.x, size.y, size.z);
                        event.renderer.box(box, ironGolemsRenderDetectingZoneSideColor.get(), ironGolemsRenderDetectingZoneLineColor.get(), ironGolemsRenderDetectingZoneShapeMode.get(), 0);
                    }

                    if (ironGolemsRenderSpawningZoneEnabled.get()) {
                        Vector3d size = ironGolemsRenderSpawningZoneSize.get();
                        Box box = Box.of(villager.getEntityPos(), size.x, size.y, size.z);
                        event.renderer.box(box, ironGolemsRenderSpawningZoneSideColor.get(), ironGolemsRenderSpawningZoneLineColor.get(), ironGolemsRenderSpawningZoneShapeMode.get(), 0);
                    }
                }
            }
        });

        trackedBlocks.forEach(blockPos -> {
            Block block = mc.world.getBlockState(blockPos).getBlock();
            Vec3d centerPos = blockPos.toCenterPos();

            if (block == Blocks.TURTLE_EGG && turtleEggsRenderEnabled.get()) {
                if (turtleEggsRangeEnabled.get()) {
                    Vector3d size = turtleEggsRangeSize.get();
                    Box box = Box.of(centerPos, size.x, size.y, size.z);
                    event.renderer.box(box, turtleEggsRangeSideColor.get(), turtleEggsRangeLineColor.get(), turtleEggsRangeShapeMode.get(), 0);
                }

                if (turtleEggsAggressionRenderEnabled.get()) {
                    Vector3d size = turtleEggsRangeSize.get();
                    Box box = Box.of(centerPos, size.x, size.y, size.z);

                    boolean isInDanger = false;
                    for (Entity entity : mc.world.getEntities()) {
                        if (entity instanceof ZombieEntity z) {
                            Box boundingBox = entity.getBoundingBox();
                            if (box.intersects(boundingBox)) {
                                isInDanger = true;
                                if (turtleEggsAggressionZombieRenderEnabled.get()) {
                                    event.renderer.box(boundingBox, turtleEggsAggressionZombieRenderSideColor.get(), turtleEggsAggressionZombieRenderLineColor.get(), turtleEggsAggressionZombieRenderShapeMode.get(), 0);
                                }
                            }
                        }
                    }

                    if (isInDanger && turtleEggsAggressionEggRenderEnabled.get()) {
                        event.renderer.box(blockPos, turtleEggsAggressionEggRenderSideColor.get(), turtleEggsAggressionEggRenderLineColor.get(), turtleEggsAggressionEggRenderShapeMode.get(), 0);
                    }
                }
            } else if (block == Blocks.SPAWNER && spawnersRenderEnabled.get()) {
                BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
                if (blockEntity instanceof MobSpawnerBlockEntity mobSpawner) {
                    MobSpawnerLogic spawnerLogic = mobSpawner.getLogic();
                    int spawnRange = ((MobSpawnerLogicAccessor) spawnerLogic).getSpawnRange();

                    if (spawnersRenderSphereEnabled.get()) {
                        int requiredPlayerRange = ((MobSpawnerLogicAccessor) spawnerLogic).getRequiredPlayerRange();
                        if (spawnersRenderSphereType.get() == SphereType.Default) {
                            renderQuadSphere(event, centerPos, requiredPlayerRange, spawnersRenderSpherePhiCount.get(), spawnersRenderSphereThetaCount.get(), spawnersRenderSphereSideColor.get(), spawnersRenderSphereLineColor.get(), spawnersRenderSphereShapeMode.get());
                        } else {
                            renderCubicSphere(event, centerPos, requiredPlayerRange, spawnersRenderSphereSideColor.get(), spawnersRenderSphereLineColor.get(), spawnersRenderSphereShapeMode.get());
                        }
                    }

                    if (spawnersRenderDetectingZoneEnabled.get()) {
                        Box box = new Box(blockPos).expand(spawnRange);
                        event.renderer.box(box, spawnersRenderDetectingZoneSideColor.get(), spawnersRenderDetectingZoneLineColor.get(), spawnersRenderDetectingZoneShapeMode.get(), 0);
                    }

                    if (spawnersRenderSpawningZoneEnabled.get()) {
                        Entity entity = spawnerLogic.getRenderedEntity(mc.world, blockPos);
                        Vec3d size = new Vec3d(spawnRange, 1, spawnRange).multiply(2);

                        if (entity != null) {
                            Box spawnBox = entity.getBoundingBox();
                            size = size.add(spawnBox.getLengthX(), spawnBox.getLengthY(), spawnBox.getLengthZ());
                        }

                        Vec3d bottomCenterPos = blockPos.toBottomCenterPos();
                        if (spawnersRenderSpawningZoneRounding.get()) {
                            size = new Vec3d(Math.ceil(size.x), Math.ceil(size.y), Math.ceil(size.z));
                            if (size.x % 2 == 0) size = size.add(1, 0, 0);
                            if (size.z % 2 == 0) size = size.add(0, 0, 1);
                        }

                        Vec3d pos = bottomCenterPos.add(0, -1 + (size.y / 2), 0);
                        Box box = Box.of(pos, size.x, size.y, size.z);

                        event.renderer.box(box, spawnersRenderSpawningZoneSideColor.get(), spawnersRenderSpawningZoneLineColor.get(), spawnersRenderSpawningZoneShapeMode.get(), 0);
                    }

                    boolean isPlayerInRange = ((MobSpawnerLogicAccessor) spawnerLogic).invokeIsPlayerInRange(mc.world, blockPos);

                    if (isPlayerInRange && spawnersRenderActiveSpawnerEnabled.get()) {
                        event.renderer.box(blockPos, spawnersRenderActiveSpawnerSideColor.get(), spawnersRenderActiveSpawnerLineColor.get(), spawnersRenderActiveSpawnerShapeMode.get(), 0);
                    } else if (!isPlayerInRange && spawnersRenderInactiveSpawnerEnabled.get()) {
                        event.renderer.box(blockPos, spawnersRenderInactiveSpawnerSideColor.get(), spawnersRenderInactiveSpawnerLineColor.get(), spawnersRenderInactiveSpawnerShapeMode.get(), 0);
                    }

                    if (spawnersRenderDebugInfoEnabled.get()) {
                        Vector3d debugInfoOffset = spawnersRenderDebugInfoOffset.get();
                        Vector3d debugInfoPos = new Vector3d(centerPos.x + debugInfoOffset.x, centerPos.y + debugInfoOffset.y, centerPos.z + debugInfoOffset.z);

                        Map<String, String> debugInfo = new HashMap<>();
                        NbtCompound nbt = mobSpawner.createNbtWithIdentifyingData(mc.world.getRegistryManager());

                        spawnersRenderDebugInfoAllowedKeys.get().forEach(key -> {
                            if (nbt.contains(key)) {
                                debugInfo.put(key, String.valueOf(nbt.get(key).asString()));
                            }
                        });

                        nbtDataDisplays.add(new NbtDataDisplay(spawnersRenderDebugInfoSize.get(), spawnersRenderDebugInfoMargin.get(), debugInfoPos, debugInfo));
                    }
                }
            }
        });

        if (spawnRangesEnabled.get()) {
            Vec3d pos;

            if (centerPositionType.get() == CenterPositionType.Pos) {
                Vector3d centerPos = centerPosition.get();

                pos = new Vec3d(centerPos.x, centerPos.y, centerPos.z);
                if (centerPositionMode.get() == CenterPositionMode.Relative) {
                    pos = pos.add(mc.player.getEntityPos());
                } else {
                    pos = pos.add(0.5, 1, 0.5);
                }
            } else {
                pos = centerBlockPosition.get().toBottomCenterPos();
                if (centerPositionMode.get() == CenterPositionMode.Relative) {
                    pos = pos.add(Vec3d.of(mc.player.getBlockPos()));
                } else {
                    pos = pos.add(0, 1, 0);
                }
            }

            if (despawnRangeEnabled.get()) {
                if (spawnRangesSphereType.get() == SphereType.Default) {
                    renderQuadSphere(event, pos, despawnSphereRange.get(), despawnRangeSpherePhiCount.get(), despawnRangeSphereThetaCount.get(), despawnRangeSideColor.get(), despawnRangeLineColor.get(), despawnRangeShapeMode.get());
                } else {
                    renderCubicSphere(event, pos, despawnBlockSphereRange.get(), despawnRangeSideColor.get(), despawnRangeLineColor.get(), despawnRangeShapeMode.get());
                }
            }

            if (idleRangeEnabled.get()) {
                if (spawnRangesSphereType.get() == SphereType.Default) {
                    renderQuadSphere(event, pos, idleSphereRange.get(), idleRangeSpherePhiCount.get(), idleRangeSphereThetaCount.get(), idleRangeSideColor.get(), idleRangeLineColor.get(), idleRangeShapeMode.get());
                } else {
                    renderCubicSphere(event, pos, idleBlockSphereRange.get(), idleRangeSideColor.get(), idleRangeLineColor.get(), idleRangeShapeMode.get());
                }
            }

            if (spawnRangeEnabled.get()) {
                if (spawnRangesSphereType.get() == SphereType.Default) {
                    renderQuadSphere(event, pos, spawnSphereRange.get(), spawnRangeSpherePhiCount.get(), spawnRangeSphereThetaCount.get(), spawnRangeSideColor.get(), spawnRangeLineColor.get(), spawnRangeShapeMode.get());
                } else {
                    renderCubicSphere(event, pos, spawnBlockSphereRange.get(), spawnRangeSideColor.get(), spawnRangeLineColor.get(), spawnRangeShapeMode.get());
                }
            }

            if (displayCenterPosition.get()) {
                Vector3d centerPos = centerPositionSize.get();
                event.renderer.box(Box.of(pos, centerPos.x, centerPos.y, centerPos.z), centerPositionSideColor.get(), centerPositionLineColor.get(), centerPositionShapeMode.get(), 0);
            }
        }
    }

    private void renderCubicSphere(Render3DEvent event, Vec3d center, int radius, SettingColor sideColor, SettingColor lineColor, ShapeMode shapeMode) {
        for (BlockPos blockPos : getSphereBlocks(BlockPos.ofFloored(center), radius)) {
            event.renderer.box(blockPos, sideColor, lineColor, shapeMode, 0);
        }
    }

    private Set<BlockPos> getSphereBlocks(BlockPos center, int radius) {
        Set<BlockPos> blocks = new HashSet<>();

        double radiusDouble = radius + 0.5;
        final double invRadius = 1.0 / radiusDouble;
        final int ceilRadius = (int) Math.ceil(radiusDouble);

        for (int x = 0; x <= ceilRadius; ++x) {
            for (int y = 0; y <= ceilRadius; ++y) {
                for (int z = 0; z <= ceilRadius; ++z) {
                    double xn = x * invRadius;
                    double yn = y * invRadius;
                    double zn = z * invRadius;

                    double distanceSq = xn * xn + yn * yn + zn * zn;
                    if (distanceSq > 1) break;

                    double nextXSq = (x + 1) * invRadius * (x + 1) * invRadius;
                    double nextYSq = (y + 1) * invRadius * (y + 1) * invRadius;
                    double nextZSq = (z + 1) * invRadius * (z + 1) * invRadius;

                    if (nextXSq + yn * yn + zn * zn <= 1 && xn * xn + nextYSq + zn * zn <= 1 && xn * xn + yn * yn + nextZSq <= 1) {
                        continue;
                    }

                    blocks.add(center.add(x, y, z));
                    blocks.add(center.add(-x, y, z));
                    blocks.add(center.add(x, -y, z));
                    blocks.add(center.add(x, y, -z));
                    blocks.add(center.add(-x, -y, z));
                    blocks.add(center.add(x, -y, -z));
                    blocks.add(center.add(-x, y, -z));
                    blocks.add(center.add(-x, -y, -z));
                }
            }
        }

        return blocks;
    }

    private void renderQuadSphere(Render3DEvent event, Vec3d center, double radius, int phiCount, int thetaCount, SettingColor sideColor, SettingColor lineColor, ShapeMode shapeMode) {
        double phiStep = Math.PI / phiCount;
        double thetaStep = 2 * Math.PI / thetaCount;

        for (int i = 0; i < phiCount; i++) {
            double phi = i * phiStep;
            double phiNext = (i + 1) * phiStep;

            for (int j = 0; j < thetaCount; j++) {
                double theta = j * thetaStep;
                double thetaNext = (j + 1) * thetaStep;

                double y1 = center.y + radius * Math.cos(phi);

                double x1 = center.x + radius * Math.sin(phi) * Math.cos(theta);
                double z1 = center.z + radius * Math.sin(phi) * Math.sin(theta);

                double x2 = center.x + radius * Math.sin(phi) * Math.cos(thetaNext);
                double z2 = center.z + radius * Math.sin(phi) * Math.sin(thetaNext);

                double y2 = center.y + radius * Math.cos(phiNext);

                double x3 = center.x + radius * Math.sin(phiNext) * Math.cos(thetaNext);
                double z3 = center.z + radius * Math.sin(phiNext) * Math.sin(thetaNext);

                double x4 = center.x + radius * Math.sin(phiNext) * Math.cos(theta);
                double z4 = center.z + radius * Math.sin(phiNext) * Math.sin(theta);

                if (shapeMode.sides()) event.renderer.quad(x1, y1, z1, x2, y1, z2, x3, y2, z3, x4, y2, z4, sideColor);

                if (shapeMode.lines()) {
                    event.renderer.line(x1, y1, z1, x2, y1, z2, lineColor);
                    event.renderer.line(x2, y1, z2, x3, y2, z3, lineColor);
                }
            }
        }
    }

    @Override
    public void onActivate() {
        blocksToTrack.clear();

        if (turtleEggsRenderEnabled.get()) blocksToTrack.add(Blocks.TURTLE_EGG);

        if (spawnersRenderEnabled.get()) blocksToTrack.add(Blocks.SPAWNER);

        if (!blocksToTrack.isEmpty() && mc.world != null) {
            trackedBlocks.clear();

            for (Chunk chunk : Utils.chunks()) {
                searchChunk(chunk);
            }

            lastDimension = mc.world.getDimension();
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (!blocksToTrack.isEmpty()) {
            Block newBlock = event.newState.getBlock();
            Block oldBlock = event.oldState.getBlock();

            if (blocksToTrack.contains(newBlock)) {
                trackedBlocks.add(event.pos);
            } else if (blocksToTrack.contains(oldBlock)) {
                trackedBlocks.remove(event.pos);
            }
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (!blocksToTrack.isEmpty()) {
            DimensionType dimension = mc.world.getDimension();
            if (lastDimension != dimension) {
                lastDimension = dimension;
                onActivate();
            }
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        if (!blocksToTrack.isEmpty()) {
            searchChunk(event.chunk());
        }
    }

    private void searchChunk(Chunk chunk) {
        workerThread.submit(() -> {
            ChunkPos chunkPos = chunk.getPos();

            for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
                for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
                    for (int y = mc.world.getBottomY(); y < mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (blocksToTrack.contains(chunk.getBlockState(pos).getBlock())) {
                            trackedBlocks.add(pos);
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!blocksToTrack.isEmpty() && event.packet instanceof UnloadChunkS2CPacket(ChunkPos pos)) {
            int startX = pos.getStartX();
            int startZ = pos.getStartZ();
            int endX = pos.getEndX();
            int endZ = pos.getEndZ();

            trackedBlocks.removeIf(blockPos -> blockPos.getX() >= startX && blockPos.getX() <= endX && blockPos.getZ() >= startZ && blockPos.getZ() <= endZ);
        }
    }

    // Enums
    private enum CenterPositionMode {
        Relative,
        Absolute
    }

    private enum CenterPositionType {
        BlockPos,
        Pos
    }

    private enum SphereType {
        Default,
        Cubic
    }

    // Settings
    public record NbtDataDisplay(Double size, Double margin, Vector3d pos, Map<String, String> attributes) {
    }
}
