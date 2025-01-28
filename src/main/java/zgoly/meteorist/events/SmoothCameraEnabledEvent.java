package zgoly.meteorist.events;

public class SmoothCameraEnabledEvent {
    private static final SmoothCameraEnabledEvent INSTANCE = new SmoothCameraEnabledEvent();

    public boolean enabled;

    public static SmoothCameraEnabledEvent get(boolean enabled) {
        INSTANCE.enabled = enabled;
        return INSTANCE;
    }
}
