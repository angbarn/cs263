package u1606484.banksim.databases;

import u1606484.banksim.interfaces.IPair;

class GenericImmutablePair<T1, T2> implements IPair<T1, T2> {

    private final T1 first;
    private final T2 second;

    GenericImmutablePair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return "[" + first + "," + second + "]";
    }

    public T1 getFirst() {
        return first;
    }

    public T2 getSecond() {
        return second;
    }
}
