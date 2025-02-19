package io.contained.internals;

import io.contained.internals.util.ByteArrayTransformer;

import java.util.Arrays;

public class ContainerDescriptor {
    public static final int BYTES = 12;
    private final byte[] signature;
    private final int blockSize;
    private final int blockCount;

    public ContainerDescriptor(int sizeInMegaBytes) {
        this.signature = Configuration.signature;
        this.blockSize = Configuration.blockSize;

        var sizeInBytes = sizeInMegaBytes * 1048576;
        this.blockCount = sizeInBytes / blockSize;
    }

    private ContainerDescriptor(byte[] signature, int blockSize, int blockCount) {
        this.signature = signature;
        this.blockSize = blockSize;
        this.blockCount = blockCount;
    }

    public byte[] getSignature() {
        return signature;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public byte[] toByteArray() {
        var bytes = new byte[BYTES];

        System.arraycopy(signature, 0, bytes, 0, signature.length);
        ByteArrayTransformer.fromInt(blockSize, bytes, signature.length);
        ByteArrayTransformer.fromInt(blockCount, bytes, signature.length + Integer.BYTES);

        return bytes;
    }

    public static ContainerDescriptor fromByteArray(byte[] bytes) {
        var signature = Arrays.copyOfRange(bytes, 0, 4);
        var blockSize = ByteArrayTransformer.toInt(bytes, 4);
        var blockCount = ByteArrayTransformer.toInt(bytes, 8);

        return new ContainerDescriptor(signature, blockSize, blockCount);
    }
}
