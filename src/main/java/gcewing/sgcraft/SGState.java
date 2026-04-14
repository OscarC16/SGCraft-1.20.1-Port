package gcewing.sgcraft;

/**
 * Represents the operational state of a Stargate.
 * Ported from the original SGCraft mod.
 */
public enum SGState {
    Idle,
    Dialling,
    Transient,
    Connected,
    Disconnecting,
    InterDialling;

    private static final SGState[] VALUES = values();

    public static SGState valueOf(int i) {
        if (i >= 0 && i < VALUES.length)
            return VALUES[i];
        return Idle;
    }
}
