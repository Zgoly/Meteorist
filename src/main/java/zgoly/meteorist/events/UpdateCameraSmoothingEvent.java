package zgoly.meteorist.events;

public class UpdateCameraSmoothingEvent {
    private static final UpdateCameraSmoothingEvent INSTANCE = new UpdateCameraSmoothingEvent();

    public double timeDelta;

    public static UpdateCameraSmoothingEvent get(double timeDelta) {
        INSTANCE.timeDelta = timeDelta;
        return INSTANCE;
    }
}
