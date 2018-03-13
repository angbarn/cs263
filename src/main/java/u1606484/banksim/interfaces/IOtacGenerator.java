package u1606484.banksim.interfaces;

public interface IOtacGenerator {

    String generateOtac(byte[] secretKey, int stepOffset);

    default String generateOtac(byte[] secretKey) {
        return generateOtac(secretKey, 0);
    }
}
