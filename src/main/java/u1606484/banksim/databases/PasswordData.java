package u1606484.banksim.databases;

/**
 * A container for a database security record
 */
public class PasswordData {

    /**
     * The hash of the password
     */
    private final byte[] passwordHash;
    /**
     * The salt the password was generated
     */
    private final byte[] passwordSalt;
    /**
     * The number of iterations used to calculate the hash
     */
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
