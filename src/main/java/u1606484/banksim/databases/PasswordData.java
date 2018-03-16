package u1606484.banksim.databases;

public class PasswordData {

    private final int securityId;
    private final byte[] passwordHash;
    private final byte[] passwordSalt;
    private final int passes;

    PasswordData(int securityId, byte[] passwordHash,
            byte[] passwordSalt,
            int passes) {
        this.securityId = securityId;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.passes = passes;
    }

    public int getSecurityId() {
        return securityId;
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
