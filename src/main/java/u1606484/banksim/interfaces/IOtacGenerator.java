package u1606484.banksim.interfaces;

import java.util.stream.Stream;

public interface IOtacGenerator {

    String generateOtac(byte[] secretKey, int stepOffset);

    int getWindowSize();

    default String generateOtac(byte[] secretKey) {
        return generateOtac(secretKey, 0);
    }

    default boolean verifyOtac(String attempt, byte[] secretKey) {
        return Stream
                .iterate(0, x -> x + 1)
                .limit(getWindowSize())
                .map(i -> generateOtac(secretKey, i))
                .map(attempt::equals)
                .reduce((aBoolean, aBoolean2) -> aBoolean || aBoolean2)
                .orElse(false);
    }
}
