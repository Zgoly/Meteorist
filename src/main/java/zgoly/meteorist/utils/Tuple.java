package zgoly.meteorist.utils;

import java.util.Objects;

/// Custom Tuple implementation.
///
/// Special thanks to Mojang for removing `net.minecraft.util.Tuple` and forcing me to write my own.
public class Tuple<A, B> {
    private A a;
    private B b;

    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return this.a;
    }

    public B getB() {
        return this.b;
    }

    public void setA(A a) {
        this.a = a;
    }

    public void setB(B b) {
        this.b = b;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof Tuple<?, ?> tuple)) {
            return false;
        } else {
            return Objects.equals(this.a, tuple.a) && Objects.equals(this.b, tuple.b);
        }
    }

    public int hashCode() {
        return 31 * Objects.hashCode(this.a) + Objects.hashCode(this.b);
    }
}
