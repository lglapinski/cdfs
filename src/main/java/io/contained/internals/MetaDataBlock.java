package io.contained.internals;

import io.contained.internals.util.ByteArrayTransformer;

import java.util.Arrays;

public class MetaDataBlock extends DataBlock {
    public static final int META_BYTES = 261 + DataBlock.META_BYTES; //256 + 4 + 1 + 12;

    private byte[] name;
    private int dataFullSize;
    private final boolean isDir;

    public MetaDataBlock(byte[] name) {
        this(name, 0, true, Configuration.noAddressMarker, Configuration.noAddressMarker, new byte[0]);

    }

    public MetaDataBlock(byte[] name, int dataFullSize, boolean isDir, byte[] data) {
        this(name, dataFullSize, isDir, Configuration.noAddressMarker, Configuration.noAddressMarker, data);
    }

    public MetaDataBlock(byte[] name, int dataFullSize, boolean isDir, int prevBlock, int nextBlock, byte[] data) {
        super(prevBlock, nextBlock, data);
        this.name = name;
        this.dataFullSize = dataFullSize;
        this.isDir = isDir;
    }

    public byte[] getName() {
        return name;
    }

    public void setName(byte[] name) {
        this.name = name;
    }

    public int getDataFullSize() {
        return dataFullSize;
    }

    public void setDataFullSize(int dataFullSize) {
        this.dataFullSize = dataFullSize;
    }

    public boolean isDir() {
        return isDir;
    }

    @Override
    public int getMetaDataSize() {
        return META_BYTES;
    }

    @Override
    public byte[] toByteArray() {
        var bytes = new byte[META_BYTES + getDataSize()];
        var index = 0;

        System.arraycopy(name, 0, bytes, index, Math.min(Configuration.filenameLength, name.length));
        index += Configuration.filenameLength;

        ByteArrayTransformer.fromInt(dataFullSize, bytes, index);
        index += Integer.BYTES;

        bytes[index] = isDir ? (byte) 1 : (byte) 0;
        index += Byte.BYTES;

        super.toByteArray(bytes, index);

        return bytes;
    }

    public static MetaDataBlock fromByteArray(byte[] bytes) {
        var nameLength = 0;
        for (int i = 0; i < Configuration.filenameLength; i++) {
            if (bytes[i] != 0) {
                nameLength++;
            } else {
                break;
            }
        }
        var index = 0;

        var name = Arrays.copyOfRange(bytes, index, index + nameLength);
        index += Configuration.filenameLength;

        var fullSize = ByteArrayTransformer.toInt(bytes, index);
        index += Integer.BYTES;

        var isDir = bytes[index] != 0;
        index += Byte.BYTES;

        var dataBlock = DataBlock.fromByteArray(bytes, index);

        return new MetaDataBlock(name, fullSize, isDir, dataBlock.getPrevBlock(), dataBlock.getNextBlock(), dataBlock.getData());
    }
}
