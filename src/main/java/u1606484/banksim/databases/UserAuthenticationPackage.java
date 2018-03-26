package u1606484.banksim.databases;

import u1606484.banksim.interfaces.IPair;


/**
 * A wrapper for {@link GenericImmutablePair} allowing field namings
 *
 * <p>Specifically, this class holds the user ID and the current OTAC-level of a
 * customer.
 */
public class UserAuthenticationPackage {

    private final IPair<Integer, Integer> pair;

    UserAuthenticationPackage(int userId, int otacLevel) {
        pair = new GenericImmutablePair<>(userId, otacLevel);
    }

    public int getUserId() {
        return pair.getFirst();
    }

    public int getOtacLevel() {
        return pair.getSecond();
    }
}
