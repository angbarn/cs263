package u1606484.banksim.interfaces;

/**
 * Generic interface for a pair
 *
 * @param <T1> The first type for the pair
 * @param <T2> The second type for the pair
 */
public interface IPair<T1, T2> {

    /**
     * Gets the first element
     *
     * @return The first element in the pair
     */
    T1 getFirst();

    /**
     * Gets the second element
     * @return The second element in the pair
     */
    T2 getSecond();

}
