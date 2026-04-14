package gcewing.sgcraft;

import net.minecraft.core.BlockPos;

/**
 * Ported addressing logic from SGCraft (legacy).
 * Converts between world coordinates/dimensions and Stargate addresses.
 */
public class SGAddressing {

    public static final String SYMBOL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final int NUM_SYMBOLS = SYMBOL_CHARS.length();
    public static final int NUM_COORD_SYMBOLS = 7;
    public static final int NUM_DIMENSION_SYMBOLS = 2;
    public static final int MAX_ADDRESS_LENGTH = NUM_COORD_SYMBOLS + NUM_DIMENSION_SYMBOLS;

    public static final int MAX_COORD = 139967;
    public static final int MIN_COORD = -MAX_COORD;
    public static final int COORD_RANGE = MAX_COORD - MIN_COORD + 1;
    
    public static final int MAX_DIMENSION_INDEX = 1295;
    public static final int DIMENSION_RANGE = MAX_DIMENSION_INDEX + 1;

    // Hashing constants from original SGCraft
    static final long mc = COORD_RANGE + 2;
    static final long pc = 93563;
    static final long qc = 153742;
    static final long md = DIMENSION_RANGE + 1;
    static final long pd = 953;
    static final long qd = 788;

    public static boolean isValidSymbolChar(char c) {
        return SYMBOL_CHARS.indexOf(Character.toUpperCase(c)) >= 0;
    }

    public static char symbolToChar(int i) {
        return SYMBOL_CHARS.charAt(i);
    }

    public static int charToSymbol(char c) {
        return SYMBOL_CHARS.indexOf(Character.toUpperCase(c));
    }

    public static String formatAddress(String address, String sep1, String sep2) {
        if (address.length() < NUM_COORD_SYMBOLS) return address;
        String coord = address.substring(0, NUM_COORD_SYMBOLS);
        String dimen = address.length() > NUM_COORD_SYMBOLS ? address.substring(NUM_COORD_SYMBOLS) : "";
        int i = (NUM_COORD_SYMBOLS + 1) / 2;
        String result = coord.substring(0, i) + sep1 + coord.substring(i);
        if (!dimen.isEmpty())
            result += sep2 + dimen;
        return result;
    }

    public static String padAddress(String address, String caret, int maxLength) {
        if (maxLength < NUM_COORD_SYMBOLS) maxLength = NUM_COORD_SYMBOLS;
        StringBuilder sb = new StringBuilder(address);
        while (sb.length() < maxLength) {
            sb.append("-");
        }
        return formatAddress(sb.toString(), " ", " ");
    }

    /**
     * placeholder for generating an address from a block position and dimension index.
     * In a full implementation, this would involve hashing coordinates.
     */
    public static String addressForLocation(BlockPos pos, int dimensionIndex) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        if (chunkX < MIN_COORD || chunkX > MAX_COORD || chunkZ < MIN_COORD || chunkZ > MAX_COORD)
            return "OUT-RANGE";

        long c = interleaveCoords(hash(chunkX - MIN_COORD, pc, mc), hash(chunkZ - MIN_COORD, pc, mc));
        int dp = permuteDimension(c, dimensionIndex % DIMENSION_RANGE);
        int d = hash(dp, pd, md);

        return longToSymbols(c, NUM_COORD_SYMBOLS) + intToSymbols(d, NUM_DIMENSION_SYMBOLS);
    }

    protected static long interleaveCoords(int x, int z) {
        long p6 = 1;
        long c = 0;
        while (x > 0 || z > 0) {
            c += p6 * (x % 6); x /= 6; p6 *= 6;
            c += p6 * (z % 6); z /= 6; p6 *= 6;
        }
        return c;
    }

    protected static int hash(int i, long f, long m) {
        return (int)(((i + 1) * f) % m) - 1;
    }

    protected static int permuteDimension(long c, int d) {
        return (int)((d + c) % DIMENSION_RANGE);
    }

    protected static String longToSymbols(long i, int n) {
        StringBuilder s = new StringBuilder();
        while (n-- > 0) {
            s.insert(0, symbolToChar((int) (i % NUM_SYMBOLS)));
            i /= NUM_SYMBOLS;
        }
        return s.toString();
    }

    protected static String intToSymbols(int i, int n) {
        return longToSymbols(i, n);
    }

    // --- Validation ---
    public static boolean isValidSymbolChar(String c) {
        return Character.isLetterOrDigit(c.charAt(0)) && SYMBOL_CHARS.indexOf(c.toUpperCase()) >= 0;
    }
}
