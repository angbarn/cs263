package u1606484.banksim.databases;

import u1606484.banksim.interfaces.IPair;

/**
 * A wrapper for {@link GenericImmutablePair} allowing field namings.
 *
 * <p>Specifically, this class holds the session key and the OTAC-stage of a
 * single session.
 */
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
