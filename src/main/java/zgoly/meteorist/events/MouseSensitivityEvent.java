package zgoly.meteorist.events;

public class MouseSensitivityEvent {
    private static final MouseSensitivityEvent INSTANCE = new MouseSensitivityEvent();

    public double sensitivity;

    public static MouseSensitivityEvent get(double sensitivity) {
        INSTANCE.sensitivity = sensitivity;
        return INSTANCE;
    }
}