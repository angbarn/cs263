package u1606484.banksim;

import java.util.Arrays;

public enum LogMessages {
    FAIL_LOGIN_1(
            "Attempt to login to account ID %s from %s failed"),
    FAIL_LOGIN_2(
            "Attempt to login to account ID %s from %s failed at OTAC stage"),
    SUCCEED_LOGIN_1(
            "Account ID %s from %s entered username and password successfully"),
    SUCCEED_LOGIN_2(
            "Account ID %s from %s OTAC authenticated successfully"),
    LOGOUT(
            "Account ID %s from %s logged out"),
    ERROR(
            "[ERROR] caught exception - %s");

    private String messageStructure;

    LogMessages(String messageStructure) {
        this.messageStructure = messageStructure;
    }

    public String get(Object... parameters) {
        String[] stringParameters = Arrays.stream(parameters)
                .map(Object::toString)
                .toArray(String[]::new);

        return String.format(messageStructure, (Object[]) stringParameters);
    }
}
