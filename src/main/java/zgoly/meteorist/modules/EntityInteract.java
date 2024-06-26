package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import zgoly.meteorist.Meteorist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EntityInteract extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> oneInteractionPerTick = sgGeneral.add(new BoolSetting.Builder()
            .name("one-interaction-per-tick")
            .description("One interaction per tick.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to interact with.")
            .defaultValue(EntityType.SHEEP)
            .onlyAttackable()
            .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Interact range.")
            .min(0)
            .defaultValue(4)
            .build()
    );
    private final Setting<Hand> hand = sgGeneral.add(new EnumSetting.Builder<Hand>()
            .name("hand")
            .description("The hand to use when interacting.")
            .defaultValue(Hand.MAIN_HAND)
            .build()
    );
    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Swing hand client-side.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreBabies = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-babies")
            .description("Ignore baby entities.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> oneTime = sgGeneral.add(new BoolSetting.Builder()
            .name("one-time")
            .description("Interact with each entity only one time.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Sends rotation packets to the server when interacting with entity.")
            .defaultValue(true)
            .build()
    );
    private final List<Entity> used = new ArrayList<>();

    public EntityInteract() {
        super(Meteorist.CATEGORY, "entity-interact", "Automatically interacts with entities in range.");
    }

    @Override
    public void onActivate() {
        used.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)
                    || !(entities.get().contains(entity.getType()))
                    || mc.player.getMainHandStack().isEmpty()
                    || oneTime.get() && used.contains(entity)
                    || mc.player.distanceTo(entity) > range.get()
                    || ignoreBabies.get() && ((LivingEntity) entity).isBaby()) continue;

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), 10, null);
            if (swingHand.get()) mc.player.swingHand(hand.get());
            if (oneTime.get()) used.add(entity);

            mc.interactionManager.interactEntity(mc.player, entity, hand.get());

            if (oneInteractionPerTick.get()) break;
        }
    }
}