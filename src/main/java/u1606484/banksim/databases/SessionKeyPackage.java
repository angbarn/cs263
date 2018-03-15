package u1606484.banksim.databases;

import u1606484.banksim.interfaces.IPair;

public class SessionKeyPackage {

    private final IPair<String, Integer> pair;

    SessionKeyPackage(String sessionKey, int otacLevel) {
        pair = new GenericImmutablePair<>(sessionKey, otacLevel);
    }

    public String getSessionKey() {
        return pair.getFirst();
    }

    public int getOtacLevel() {
        return pair.getSecond();
    }
}
