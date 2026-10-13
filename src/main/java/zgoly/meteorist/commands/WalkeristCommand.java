package zgoly.meteorist.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.BlockPos;
import zgoly.meteorist.modules.walkerist.Walkerist;
import zgoly.meteorist.modules.walkerist.Waypoint;
import zgoly.meteorist.utils.movement.WalkeristUtils;

import java.util.List;

public class WalkeristCommand extends Command {
    private final Walkerist module;

    public WalkeristCommand() {
        super("walkerist", "Adds, removes and controls waypoints for the Walkerist module.");
        module = Modules.get().get(Walkerist.class);
        WalkeristUtils.setModule(module);
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(literal("add")
                .then(literal("here")
                        .executes(_ -> addWaypoint(mc.player.blockPosition(), true))
                        .then(argument("autoplay", BoolArgumentType.bool())
                                .executes(context -> addWaypoint(mc.player.blockPosition(), BoolArgumentType.getBool(context, "autoplay")))
                        )
                )
                .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("y", IntegerArgumentType.integer())
                                .then(argument("z", IntegerArgumentType.integer())
                                        .executes(context -> addWaypoint(new BlockPos(
                                                IntegerArgumentType.getInteger(context, "x"),
                                                IntegerArgumentType.getInteger(context, "y"),
                                                IntegerArgumentType.getInteger(context, "z")), true))
                                        .then(argument("autoplay", BoolArgumentType.bool())
                                                .executes(context -> addWaypoint(new BlockPos(
                                                        IntegerArgumentType.getInteger(context, "x"),
                                                        IntegerArgumentType.getInteger(context, "y"),
                                                        IntegerArgumentType.getInteger(context, "z")),
                                                        BoolArgumentType.getBool(context, "autoplay")))
                                        )
                                )
                        )
                )
        );

        builder.then(literal("remove")
                .then(argument("index", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            int index = IntegerArgumentType.getInteger(context, "index");
                            if (index >= WalkeristUtils.getWaypoints().size()) {
                                error("Waypoint index (highlight)%d(default) out of range", index);
                                return SINGLE_SUCCESS;
                            }
                            WalkeristUtils.queueRemove(index);
                            info("Removed waypoint (highlight)%d(default)", index);
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("move")
                .then(argument("from", IntegerArgumentType.integer(0))
                        .then(argument("to", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int from = IntegerArgumentType.getInteger(context, "from");
                                    int to = IntegerArgumentType.getInteger(context, "to");
                                    int size = WalkeristUtils.getWaypoints().size();
                                    if (from >= size || to >= size) {
                                        error("Invalid indices: from (highlight)%d(default), to (highlight)%d(default)", from, to);
                                        return SINGLE_SUCCESS;
                                    }
                                    WalkeristUtils.queueMove(from, to);
                                    info("Moved waypoint (highlight)%d(default) to (highlight)%d(default)", from, to);
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
        );

        builder.then(literal("select")
                .then(argument("index", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            int index = IntegerArgumentType.getInteger(context, "index");
                            if (index >= WalkeristUtils.getWaypoints().size()) {
                                error("Waypoint index (highlight)%d(default) out of range", index);
                                return SINGLE_SUCCESS;
                            }
                            WalkeristUtils.queueSelect(index);
                            info("Selected waypoint (highlight)%d(default)", index);
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("deselect").executes(_ -> {
            if (WalkeristUtils.getCurrent() == -1) {
                error("No waypoint is selected");
                return SINGLE_SUCCESS;
            }
            WalkeristUtils.queueDeselect();
            info("Deselected waypoint");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("set")
                .then(argument("index", IntegerArgumentType.integer(0))
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .executes(context -> {
                                                    int index = IntegerArgumentType.getInteger(context, "index");
                                                    if (index >= WalkeristUtils.getWaypoints().size()) {
                                                        error("Waypoint index (highlight)%d(default) out of range", index);
                                                        return SINGLE_SUCCESS;
                                                    }
                                                    BlockPos pos = new BlockPos(
                                                            IntegerArgumentType.getInteger(context, "x"),
                                                            IntegerArgumentType.getInteger(context, "y"),
                                                            IntegerArgumentType.getInteger(context, "z"));
                                                    WalkeristUtils.queueSet(index, pos);
                                                    info("Set waypoint (highlight)%d(default) to (highlight)%d, %d, %d(default)",
                                                            index, pos.getX(), pos.getY(), pos.getZ());
                                                    return SINGLE_SUCCESS;
                                                }))
                                )
                        )
                )
        );

        builder.then(literal("list").executes(_ -> {
            List<Waypoint> waypoints = WalkeristUtils.getWaypoints();
            if (waypoints.isEmpty()) {
                info("No waypoints in the queue");
                return SINGLE_SUCCESS;
            }
            info("Waypoints ((highlight)%d(default)):", waypoints.size());
            for (int i = 0; i < waypoints.size(); i++) {
                BlockPos pos = waypoints.get(i).pos.get();
                info("(highlight)%d(default). %d, %d, %d - %s", i, pos.getX(), pos.getY(), pos.getZ(), WalkeristUtils.statusText(i));
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("clear").executes(_ -> {
            WalkeristUtils.queueClear();
            info("Cleared waypoint queue");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("autoplay")
                .executes(_ -> {
                    toggleAutoPlay();
                    return SINGLE_SUCCESS;
                })
                .then(argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            setAutoPlay(BoolArgumentType.getBool(context, "enabled"));
                            return SINGLE_SUCCESS;
                        }))
        );

        builder.then(literal("play").executes(_ -> {
            if (WalkeristUtils.getWaypoints().isEmpty()) {
                error("Queue is empty");
                return SINGLE_SUCCESS;
            }
            WalkeristUtils.queuePlay();
            info("Started walking waypoint queue");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("pause").executes(_ -> {
            if (WalkeristUtils.isPaused()) {
                error("Already paused");
                return SINGLE_SUCCESS;
            }
            WalkeristUtils.togglePaused();
            info("Paused");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("resume").executes(_ -> {
            if (!WalkeristUtils.isPaused()) {
                error("Not paused");
                return SINGLE_SUCCESS;
            }
            WalkeristUtils.togglePaused();
            info("Resumed");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("status").executes(_ -> {
            info("State: (highlight)%s(default)%s", WalkeristUtils.getState().name(), WalkeristUtils.isPaused() ? " (paused)" : "");
            BlockPos target = WalkeristUtils.getTarget();
            if (target != null) {
                info("Target: (highlight)%d, %d, %d(default)", target.getX(), target.getY(), target.getZ());
            }
            int current = WalkeristUtils.getCurrent();
            info("Queue: (highlight)%d(default) waypoints, current (highlight)%s(default)",
                    WalkeristUtils.getWaypoints().size(), current == -1 ? "none" : String.valueOf(current));
            return SINGLE_SUCCESS;
        }));
    }

    private int addWaypoint(BlockPos pos, boolean autoStart) {
        WalkeristUtils.queueAdd(pos, autoStart);
        info("Added waypoint (highlight)%d, %d, %d(default)%s", pos.getX(), pos.getY(), pos.getZ(),
                autoStart ? "" : " (autoplay off)");
        return SINGLE_SUCCESS;
    }

    private void toggleAutoPlay() {
        setAutoPlay(!module.autoPlay.get());
    }

    private void setAutoPlay(boolean enabled) {
        module.autoPlay.set(enabled);
        info("Autoplay (highlight)%s(default)", enabled ? "enabled" : "disabled");
        if (enabled && WalkeristUtils.getCurrent() == -1) WalkeristUtils.queuePlay();
    }
}
