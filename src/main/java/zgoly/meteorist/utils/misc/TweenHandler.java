package zgoly.meteorist.utils.misc;

import com.google.common.util.concurrent.AtomicDouble;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.orbit.EventHandler;
import zgoly.meteorist.events.TweenEndEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TweenHandler {
    private static double currentTime;
    private final List<Tween> tweens = new ArrayList<>();

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        currentTime += event.frameTime;
        Iterator<Tween> iterator = tweens.iterator();
        while (iterator.hasNext()) {
            Tween tween = iterator.next();
            tween.update();
            if (tween.completed) iterator.remove();
        }
    }

    /**
     * Plays a tween animation.
     *
     * @param instant           Whether to play the tween instantly.
     * @param reference         The atomic double reference to be tweened.
     * @param endValue          The end value of the tween.
     * @param duration          The duration of the tween.
     * @param easingStyle       The easing style of the tween.
     * @param easingDirection   The easing direction of the tween.
     */
    public void play(boolean instant, AtomicDouble reference, double endValue, double duration, EasingStyle easingStyle, EasingDirection easingDirection) {
        if (instant) {
            stopPreviousTween(reference);
            reference.set(endValue);
        } else {
            play(reference, endValue, duration, easingStyle, easingDirection);
        }
    }

    /**
     * Plays a tween animation.
     *
     * @param reference         The atomic double reference to be tweened.
     * @param endValue          The end value of the tween.
     * @param duration          The duration of the tween.
     * @param easingStyle       The easing style of the tween.
     * @param easingDirection   The easing direction of the tween.
     */
    public void play(AtomicDouble reference, double endValue, double duration, EasingStyle easingStyle, EasingDirection easingDirection) {
        stopPreviousTween(reference);
        Tween tween = new Tween(reference, endValue, duration, easingStyle, easingDirection);
        tweens.add(tween);
    }

    /**
     * Stops the previous tween associated with the given reference.
     *
     * @param reference The atomic double reference.
     */
    private void stopPreviousTween(AtomicDouble reference) {
        tweens.removeIf(tween -> Objects.equals(tween.reference, reference));
    }

    /**
     * Enum representing different easing styles.
     */
    public enum EasingStyle {
        Linear, Sine, Quad, Cubic, Quart, Quint, Expo, Circ, Back, Elastic, Bounce
    }

    /**
     * Enum representing different easing directions.
     */
    public enum EasingDirection {
        In, Out, InOut, OutIn
    }

    /**
     * Represents a tween animation.
     */
    public static class Tween {
        private final AtomicDouble reference;
        private final double startValue;
        private final double endValue;
        private final double duration;
        private final EasingStyle easingStyle;
        private final EasingDirection easingDirection;
        private final double startTime;
        private boolean completed = false;

        /**
         * Constructs a new Tween.
         *
         * @param reference         The atomic double reference to be tweened.
         * @param endValue          The end value of the tween.
         * @param duration          The duration of the tween.
         * @param easingStyle       The easing style of the tween.
         * @param easingDirection   The easing direction of the tween.
         */
        public Tween(AtomicDouble reference, double endValue, double duration, EasingStyle easingStyle, EasingDirection easingDirection) {
            this.reference = reference;
            this.startValue = reference.get();
            this.endValue = endValue;
            this.duration = duration;
            this.easingStyle = easingStyle;
            this.easingDirection = easingDirection;
            this.startTime = currentTime;
        }

        /**
         * Updates the tween animation.
         */
        public void update() {
            double elapsedTime = currentTime - startTime;
            if (elapsedTime < duration) {
                double t = elapsedTime / duration;
                double easedValue = EasingFunctions.ease(easingStyle, easingDirection, t);
                reference.set(startValue + (endValue - startValue) * easedValue);
            } else {
                reference.set(endValue);
                MeteorClient.EVENT_BUS.post(new TweenEndEvent());
                completed = true;
            }
        }
    }

    /**
     * Provides easing functions for tween animations.
     */
    public static class EasingFunctions {
        /**
         * Calculates the eased value based on the easing style and direction.
         *
         * @param easingStyle       The easing style.
         * @param easingDirection   The easing direction.
         * @param x                 The input value.
         * @return                  The eased value.
         */
        public static double ease(EasingStyle easingStyle, EasingDirection easingDirection, double x) {
            return switch (easingDirection) {
                case In -> easeIn(easingStyle, x);
                case Out -> 1 - easeIn(easingStyle, 1 - x);
                case InOut -> x < 0.5 ? easeIn(easingStyle, 2 * x) / 2 : 1 - easeIn(easingStyle, 2 * (1 - x)) / 2;
                case OutIn ->
                        x < 0.5 ? (1 - easeIn(easingStyle, 1 - 2 * x)) / 2 : easeIn(easingStyle, 2 * x - 1) / 2 + 0.5;
            };
        }

        /**
         * Calculates the eased value for the "In" direction.
         *
         * @param easingStyle The easing style.
         * @param x           The input value.
         * @return             The eased value.
         */
        private static double easeIn(EasingStyle easingStyle, double x) {
            switch (easingStyle) {
                case Sine:
                    return 1 - Math.cos((x * Math.PI) / 2);
                case Quad:
                    return x * x;
                case Cubic:
                    return x * x * x;
                case Quart:
                    return x * x * x * x;
                case Quint:
                    return x * x * x * x * x;
                case Expo:
                    return x == 0 ? 0 : Math.pow(2, 10 * x - 10);
                case Circ:
                    return 1 - Math.sqrt(1 - Math.pow(x, 2));
                case Back: {
                    double c1 = 1.70158, c3 = c1 + 1;
                    return c3 * x * x * x - c1 * x * x;
                }
                case Elastic: {
                    double c4 = (2 * Math.PI) / 3;
                    return x == 0 ? 0 : x == 1 ? 1 : -Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * c4);
                }
                case Bounce: {
                    double n1 = 7.5625, d1 = 2.75;
                    if (x < 1 / d1) {
                        return n1 * x * x;
                    } else if (x < 2 / d1) {
                        return n1 * (x -= 1.5 / d1) * x + 0.75;
                    } else if (x < 2.5 / d1) {
                        return n1 * (x -= 2.25 / d1) * x + 0.9375;
                    } else {
                        return n1 * (x -= 2.625 / d1) * x + 0.984375;
                    }
                }
                default:
                    return x;
            }
        }
    }
}
