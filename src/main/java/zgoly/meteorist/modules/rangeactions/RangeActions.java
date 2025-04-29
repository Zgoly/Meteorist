package zgoly.meteorist.modules.rangeactions;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.modules.rangeactions.rangeactions.BaseRangeAction;
import zgoly.meteorist.modules.rangeactions.rangeactions.CommandsRangeAction;
import zgoly.meteorist.modules.rangeactions.rangeactions.DespawnerRangeAction;
import zgoly.meteorist.modules.rangeactions.rangeactions.InteractionRangeAction;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static zgoly.meteorist.Meteorist.*;
import static zgoly.meteorist.utils.MeteoristUtils.calculateFov;

public class RangeActions extends Module {
    public static List<BaseRangeAction> rangeActions = new ArrayList<>();
    private final RangeActionFactory factory = new RangeActionFactory();
    private final Map<CommandsRangeAction, Integer> commandDelayTimers = new HashMap<>();
    private final Map<CommandsRangeAction, Integer> commandIndex = new HashMap<>();

    private boolean ignoreStartBreakingBlock, ignoreInteractBlock, ignoreAttackEntity, ignoreInteractEntity;

    public RangeActions() {
        super(Meteorist.CATEGORY, "range-actions", "Combined functionality of different range actions.");
    }

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        NbtList list = new NbtList();
        for (BaseRangeAction rangeAction : rangeActions) {
            NbtCompound mTag = new NbtCompound();
            mTag.putString("type", rangeAction.getTypeName());
            mTag.put("rangeAction", rangeAction.toTag());

            list.add(mTag);
        }
        tag.put("rangeActions", list);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);

        rangeActions.clear();

        NbtList list = tag.getListOrEmpty("rangeActions");

        for (NbtElement tagII : list) {
            NbtCompound tagI = (NbtCompound) tagII;
            String type = tagI.getString("type", "");
            BaseRangeAction rangeAction = factory.createRangeAction(type);

            if (rangeAction != null) {
                NbtCompound rangeActionTag = (NbtCompound) tagI.get("rangeAction");
                if (rangeActionTag != null) rangeAction.fromTag(rangeActionTag);
                rangeActions.add(rangeAction);
            }
        }

        return this;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        list.clear();

        for (BaseRangeAction rangeAction : rangeActions) {
            list.add(theme.settings(rangeAction.settings)).expandX();

            WContainer container = list.add(theme.horizontalList()).expandX().widget();
            if (rangeActions.size() > 1) {
                int index = rangeActions.indexOf(rangeAction);
                if (index > 0) {
                    WButton moveUp = container.add(theme.button(ARROW_UP)).widget();
                    moveUp.tooltip = "Move range action up.";
                    moveUp.action = () -> {
                        rangeActions.remove(index);
                        rangeActions.add(index - 1, rangeAction);
                        fillWidget(theme, list);
                    };
                }

                if (index < rangeActions.size() - 1) {
                    WButton moveDown = container.add(theme.button(ARROW_DOWN)).widget();
                    moveDown.tooltip = "Move range action down.";
                    moveDown.action = () -> {
                        rangeActions.remove(index);
                        rangeActions.add(index + 1, rangeAction);
                        fillWidget(theme, list);
                    };
                }
            }

            WButton copy = container.add(theme.button(COPY)).widget();
            copy.tooltip = "Duplicate range action.";
            copy.action = () -> {
                rangeActions.add(rangeActions.indexOf(rangeAction), rangeAction.copy());
                fillWidget(theme, list);
            };

            WMinus remove = container.add(theme.minus()).widget();
            remove.tooltip = "Remove range action.";
            remove.action = () -> {
                rangeActions.remove(rangeAction);
                fillWidget(theme, list);
            };
        }

        list.add(theme.horizontalSeparator()).expandX();
        WTable controls = list.add(theme.table()).expandX().widget();

        WButton createInteraction = controls.add(theme.button("New Interaction")).expandX().widget();
        createInteraction.action = () -> {
            InteractionRangeAction interactionAction = new InteractionRangeAction();
            rangeActions.add(interactionAction);
            fillWidget(theme, list);
        };

        WButton createDespawner = controls.add(theme.button("New Despawner")).expandX().widget();
        createDespawner.action = () -> {
            DespawnerRangeAction rangeAction = new DespawnerRangeAction();
            rangeActions.add(rangeAction);
            fillWidget(theme, list);
        };

        WButton createCommands = controls.add(theme.button("New Commands")).expandX().widget();
        createCommands.action = () -> {
            CommandsRangeAction rangeAction = new CommandsRangeAction();
            rangeActions.add(rangeAction);
            fillWidget(theme, list);
        };

        WButton removeAll = controls.add(theme.button("Remove All Range Actions")).expandX().widget();
        removeAll.action = () -> {
            rangeActions.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ignoreStartBreakingBlock = ignoreInteractBlock = ignoreAttackEntity = ignoreInteractEntity = false;

        boolean shouldSneak = false;

        for (BaseRangeAction rangeAction : rangeActions) {
            Entity entity = TargetUtils.get(target -> entityCheck(target, rangeAction), rangeAction.priority.get());

            if (entity == null) continue;

            switch (rangeAction) {
                case InteractionRangeAction interactionAction -> {
                    if (interactionAction.ignoreStartBreakingBlock.get())
                        ignoreStartBreakingBlock = true;
                    if (interactionAction.ignoreInteractBlock.get())
                        ignoreInteractBlock = true;
                    if (interactionAction.ignoreAttackEntity.get())
                        ignoreAttackEntity = true;
                    if (interactionAction.ignoreInteractEntity.get())
                        ignoreInteractEntity = true;
                    if (interactionAction.enableSneak.get())
                        shouldSneak = true;
                }
                case DespawnerRangeAction despawnerAction -> {
                    if (despawnerAction.checkRoof.get() && !mc.world.isSkyVisible(mc.player.getBlockPos().up()))
                        continue;
                    mc.player.setVelocity(mc.player.getVelocity().add(0, despawnerAction.upVelocity.get(), 0));
                }
                case CommandsRangeAction commandsAction -> {
                    int delay = commandsAction.delay.get();
                    int commandsPerTick = commandsAction.commandsPerTick.get();
                    commandDelayTimers.putIfAbsent(commandsAction, 0);
                    commandIndex.putIfAbsent(commandsAction, 0);
                    int timer = commandDelayTimers.get(commandsAction);
                    int index = commandIndex.get(commandsAction);

                    if (timer >= delay) {
                        for (int i = 0; i < commandsPerTick && index < commandsAction.commands.get().size(); i++) {
                            ChatUtils.sendPlayerMsg(commandsAction.commands.get().get(index));
                            index++;
                        }
                        commandIndex.put(commandsAction, index);
                        commandDelayTimers.put(commandsAction, 0);
                    } else {
                        commandDelayTimers.put(commandsAction, timer + 1);
                    }

                    if (index >= commandsAction.commands.get().size()) {
                        commandIndex.put(commandsAction, 0);
                    }
                }
                default -> {
                }
            }
        }

        if (shouldSneak) mc.options.sneakKey.setPressed(true);
    }

    private boolean entityCheck(Entity entity, BaseRangeAction rangeAction) {
        if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return false;
        if ((entity instanceof LivingEntity livingEntity && livingEntity.isDead()) || !entity.isAlive()) return false;

        if (PlayerUtils.isWithin(entity, rangeAction.rangeFrom.get())
                || !PlayerUtils.isWithin(entity, rangeAction.rangeTo.get())) return false;

        if (!rangeAction.entities.get().contains(entity.getType())) return false;
        if (rangeAction.ignoreNamed.get() && entity.hasCustomName()) return false;

        if (rangeAction.ignoreTamed.get()) {
            if (entity instanceof Tameable tameable
                    && tameable.getOwner() != null
                    && tameable.getOwner().equals(mc.player)) {
                return false;
            }
        }

        if (rangeAction.ignorePassive.get()) {
            switch (entity) {
                case EndermanEntity enderman when !enderman.isAngry() -> {
                    return false;
                }
                case ZombifiedPiglinEntity piglin when !piglin.isAttacking() -> {
                    return false;
                }
                case WolfEntity wolf when !wolf.isAttacking() -> {
                    return false;
                }
                default -> {
                }
            }
        }

        if (entity instanceof PlayerEntity player) {
            if (rangeAction.ignoreCreative.get() && player.isCreative()) return false;
            if (rangeAction.ignoreFriends.get() && !Friends.get().shouldAttack(player)) return false;
            if (rangeAction.ignoreShield.get() && player.isBlocking()) return false;
        }

        if (entity instanceof AnimalEntity animal) {
            return switch (rangeAction.mobAgeFilter.get()) {
                case Baby -> animal.isBaby();
                case Adult -> !animal.isBaby();
                case Both -> true;
            };
        }

        if (rangeAction.useFovRange.get() && calculateFov(mc.player, entity) > rangeAction.fovRange.get()) return false;
        return rangeAction.ignoreWalls.get() || PlayerUtils.canSeeEntity(entity);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (ignoreStartBreakingBlock) event.cancel();
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (ignoreInteractBlock) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(AttackEntityEvent event) {
        if (ignoreAttackEntity) event.cancel();
    }

    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        if (ignoreInteractEntity) event.cancel();
    }
}
