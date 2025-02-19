package io.contained.internals;

import io.contained.internals.util.ByteArrayTransformer;

import java.util.Arrays;

public class DataBlock {
    public static final int META_BYTES = 12; // 4 + 4 + 4

    private final int prevBlock;
    private int nextBlock;
    private int dataSize;
    private byte[] data;

    public DataBlock(byte[] data) {
        this(Configuration.noAddressMarker, Configuration.noAddressMarker, data);
    }

    public DataBlock(int prevBlock, byte[] data) {
        this(prevBlock, Configuration.noAddressMarker, data);
    }

    public DataBlock(int prevBlock, int nextBlock, byte[] data) {
        this.prevBlock = prevBlock;
        this.nextBlock = nextBlock;

        this.data = data;
        this.dataSize = data.length;
    }

    public int getPrevBlock() {
        return prevBlock;
    }

    public int getNextBlock() {
        return nextBlock;
    }

    public void setNextBlock(int nextBlock) {
        this.nextBlock = nextBlock;
    }

    public int getDataSize() {
        return dataSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        this.dataSize = data.length;
    }

    public boolean hasNextBlock() {
        return getNextBlock() != Configuration.noAddressMarker;
    }

    public int getMetaDataSize() {
        return META_BYTES;
    }

    public byte[] toByteArray() {
        var bytes = new byte[META_BYTES + getDataSize()];
        toByteArray(bytes, 0);
        return bytes;
    }

    protected void toByteArray(byte[] bytes, int index) {
        ByteArrayTransformer.fromInt(getPrevBlock(), bytes, index);
        index += Integer.BYTES;

        ByteArrayTransformer.fromInt(getNextBlock(), bytes, index);
        index += Integer.BYTES;

        ByteArrayTransformer.fromInt(getDataSize(), bytes, index);
        index += Integer.BYTES;

        if (getDataSize() > 0) {
            System.arraycopy(getData(), 0, bytes, index, getDataSize());
        }
    }

    public static DataBlock fromByteArray(byte[] bytes) {
        return fromByteArray(bytes, 0);
    }

    protected static DataBlock fromByteArray(byte[] bytes, int index) {
        var prevBlock = ByteArrayTransformer.toInt(bytes, index);
        index += Integer.BYTES;

        var nextBlock = ByteArrayTransformer.toInt(bytes, index);
        index += Integer.BYTES;

        var dataSize = ByteArrayTransformer.toInt(bytes, index);
        index += Integer.BYTES;

        var data = Arrays.copyOfRange(bytes, index, index + dataSize);

        return new DataBlock(prevBlock, nextBlock, data);
    }
}
