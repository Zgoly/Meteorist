package zgoly.meteorist.events;

public class HandRenderEvent {
    private static final HandRenderEvent INSTANCE = new HandRenderEvent();

    public boolean renderHand;

    public static HandRenderEvent get(boolean renderHand) {
        INSTANCE.renderHand = renderHand;
        return INSTANCE;
    }
}
