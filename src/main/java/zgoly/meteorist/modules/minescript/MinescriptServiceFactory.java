package zgoly.meteorist.modules.minescript;

public class MinescriptServiceFactory {
    public static MinescriptService create(MinescriptIntegration module) {
        try {
            Class.forName("net.minescript.common.Minescript");
            return new RealMinescriptService(module);
        } catch (ClassNotFoundException ignored) {
            return new StubMinescriptService();
        }
    }
}