package u1606484.banksim.databases;

import u1606484.banksim.interfaces.IPair;

public class SessionKeyPackage {

    private final IPair<String, Integer> pair;

    SessionKeyPackage(String sessionKey, int otacStage) {
        pair = new GenericImmutablePair<>(sessionKey, otacStage);
    }

    public String getSessionKey() {
        return pair.getFirst();
    }

    public int getOtacStage() {
        return pair.getSecond();
    }
}
