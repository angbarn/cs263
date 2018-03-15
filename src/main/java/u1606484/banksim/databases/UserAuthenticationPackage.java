package u1606484.banksim.databases;

import u1606484.banksim.interfaces.IPair;

public class UserAuthenticationPackage {

    private final IPair<Integer, Integer> pair;

    public UserAuthenticationPackage(int userId, int otacLevel) {
        pair = new GenericImmutablePair<>(userId, otacLevel);
    }

    public int getUserId() {
        return pair.getFirst();
    }

    public int getOtacLevel() {
        return pair.getSecond();
    }
}
