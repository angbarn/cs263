package u1606484.banksim.databases;

class SessionKeyPackage {

    private final String sessionKey;
    private final int otacStage;

    SessionKeyPackage(String sessionKey, int otacStage) {
        this.sessionKey = sessionKey;
        this.otacStage = otacStage;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public int getOtacStage() {
        return otacStage;
    }

    public String toString() {
        StringBuilder r = new StringBuilder();
        r.append("[").append(getSessionKey()).append(",");
        if (getOtacStage() == 0) {
            r.append("no otac");
        } else if (getOtacStage() == 1) {
            r.append("otac authenticated");
        }
        r.append("]");
        return r.toString();
    }
}
