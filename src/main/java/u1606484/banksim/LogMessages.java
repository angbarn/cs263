package u1606484.banksim;

import java.util.Arrays;

/**
 * An enum handling the structure of log reports. This enables all types of log
 * report to be collated in a single place.
 */
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
            "Account ID %s from %s logged out");

    private final String messageStructure;

    LogMessages(String messageStructure) {
        this.messageStructure = messageStructure;
    }

    /**
     * Gets the text representation of the logging event, filling in {@code %s}
     * markers from the provided parameter list.
     *
     * <p>Arbitrary objects are converted to a string via the toString method.
     *
     * @param parameters Parameters to bind to markers in the string.
     * @return A logging message which can be written to a database
     */
    public String get(Object... parameters) {
        String[] stringParameters = Arrays.stream(parameters)
                .map(Object::toString)
                .toArray(String[]::new);

        return String.format(messageStructure, (Object[]) stringParameters);
    }
}
