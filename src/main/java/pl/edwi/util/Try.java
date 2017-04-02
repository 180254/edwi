package pl.edwi.util;

public class Try {

    public static <T> T do1(TrySupplier1<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return null;
        }
    }

    @FunctionalInterface
    public interface TrySupplier1<T> {
        T get() throws Exception;
    }

    public static void do2(TrySupplier2 supplier) {
        try {
            supplier.get();
        } catch (Exception ignored) {
        }
    }

    @FunctionalInterface
    public interface TrySupplier2 {
        void get() throws Exception;
    }
}
