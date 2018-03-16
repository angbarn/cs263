package u1606484.banksim.interfaces;

public interface IOtacGenerator {

    String generateOtac(byte[] secretKey, long timeMillis);

    long getTimestamp(long rawTimeMillis, int offset);

}
