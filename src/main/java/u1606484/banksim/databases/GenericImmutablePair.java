package u1606484.banksim.databases;

import u1606484.banksim.interfaces.IPair;

/**
 * A generic pair class for use when returning two values at once is necessary.
 *
 * @param <T1> The type of the first field
 * @param <T2> The type of the second field
 */
class GenericImmutablePair<T1, T2> implements IPair<T1, T2> {

    /**
     * The first field in the pair
     */
    private final T1 first;
    /**
     * The second field in the pair
     */
    private final T2 second;

    GenericImmutablePair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Converts to a string in the format {@code [first,second]}
     *
     * @return A string representation of the pair
     */
    public String toString() {
        return "[" + first + "," + second + "]";
    }

    /**
     * Fetches the first field of the pair
     * @return The first field of the pair
     */
    public T1 getFirst() {
        return first;
    }

    /**
     * Fetches the second field of the pair
     * @return The second field of the pair
     */
    public T2 getSecond() {
        return second;
    }
}
