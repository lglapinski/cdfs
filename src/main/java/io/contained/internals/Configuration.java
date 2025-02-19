package io.contained.internals;

public abstract class Configuration {
    public static final byte[] signature = new byte[]{99, 100, 102, 115};
    public static final int blockSize = 4096;
    public static final int filenameLength = 256;
    public static final int noAddressMarker = -1;
}
