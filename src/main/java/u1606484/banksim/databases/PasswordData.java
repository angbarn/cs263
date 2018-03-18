package u1606484.banksim.databases;

public class PasswordData {

    private final byte[] passwordHash;
    private final byte[] passwordSalt;
    private final int passes;

    PasswordData(byte[] passwordHash, byte[] passwordSalt, int passes) {
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.passes = passes;
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public byte[] getPasswordSalt() {
        return passwordSalt;
    }

    public int getPasses() {
        return passes;
    }
}
