package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.entity.DamageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import zgoly.meteorist.Meteorist;

import java.util.ArrayList;

public class ZKillaura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onJump = sgGeneral.add(new BoolSetting.Builder()
            .name("on-jump")
            .description("Attack on jump.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> fallRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("fall-range")
            .description("Fall range.")
            .defaultValue(0.4)
            .range(0.1, 1)
            .sliderRange(0.1, 1)
            .visible(onJump::get)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Range.")
            .defaultValue(3.5)
            .range(1, 10)
            .sliderRange(1, 128)
            .build()
    );

    private final Setting<Double> lookRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("look-range")
            .description("Look range.")
            .defaultValue(5)
            .range(1, 10)
            .sliderRange(1, 128)
            .build()
    );

    public ZKillaura() {
        super(Meteorist.CATEGORY, "ZKillaura", "Killaura.");
    }

    ArrayList<LivingEntity> list = new ArrayList<>();

    @Override
    public void onActivate() {
        list.clear();
    }

    @EventHandler
    private void onDamage(DamageEvent event) {
        if (!list.contains(event.entity)) {
            list.add(event.entity);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.isDead() || mc.world == null) return;
        if (onJump.get() && !(mc.player.fallDistance > fallRange.get())) return;
        if ((int) (mc.player.getAttackCooldownProgress(0.0F) * 17.0F) < 16) return;
        for (LivingEntity entity : list) {
            if (entity != mc.targetedEntity) return;
            if (mc.player.distanceTo(entity) <= range.get() && entity.getHealth() > 0 && entity != mc.player) {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }
}