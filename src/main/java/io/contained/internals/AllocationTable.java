package io.contained.internals;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class AllocationTable {
    private final BitSet table;
    private final int blockCount;

    public AllocationTable(int blockCount) {
        this.table = new BitSet(blockCount);
        this.blockCount = blockCount;
    }

    private AllocationTable(byte[] bytes) {
        this.table = BitSet.valueOf(bytes);
        this.blockCount = bytes.length * 8;
    }

    public List<Integer> getAvailableBlocks(int blockCount) {
        if (table.cardinality() + blockCount > this.blockCount) {
            throw new IllegalStateException("Not enough free space to allocate blocks");
        }
        List<Integer> blocks = new ArrayList<>(blockCount);
        var bitIndex = 0;
        for (int i = 0; i < blockCount; i++) {
            bitIndex = table.nextClearBit(bitIndex);
            blocks.add(bitIndex);
        }
        return blocks;
    }

    public void allocateBlocks(List<Integer> blocks) {
        for (Integer block : blocks) {
            table.set(block);
        }
    }

    public void freeBlocks(List<Integer> blocks) {
        for (Integer block : blocks) {
            table.clear(block);
        }
    }

    public int size() {
        return sizeOf(blockCount);
    }

    public static int sizeOf(int blockCount) {
        return blockCount / 8;
    }

    public byte[] toByteArray() {
        var data = new byte[size()];
        var tableAsBytes = table.toByteArray();
        System.arraycopy(tableAsBytes, 0, data, 0, Math.min(data.length, tableAsBytes.length));
        return data;
    }

    public static AllocationTable fromByteArray(byte[] data) {
        return new AllocationTable(data);
    }
}
