package io.contained.internals;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DataBlockTest {
    @Test
    public void testSimpleSingularBlockCreation() {
        var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        var block = new DataBlock(bytes);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getData()).containsExactly(bytes);
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    public void testBlockWithPredecessorCreation() {
        var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        var block = new DataBlock(1, bytes);
        assertThat(block.getPrevBlock()).isEqualTo(1);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getData()).containsExactly(bytes);
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    public void testUpdateBlockData() {
        var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        var block = new DataBlock(bytes);
        assertThat(block.getDataSize()).isEqualTo(bytes.length);
        assertThat(block.getData()).containsExactly(bytes);

        var newBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        block.setData(newBytes);
        assertThat(block.getDataSize()).isEqualTo(newBytes.length);
        assertThat(block.getData()).containsExactly(newBytes);
    }

    @Test
    public void testUpdateNextBlockPointer() {
        var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        var block = new DataBlock(bytes);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.hasNextBlock()).isFalse();

        block.setNextBlock(1);
        assertThat(block.getNextBlock()).isEqualTo(1);
        assertThat(block.hasNextBlock()).isTrue();

    }

    @Test
    public void testBlockSerialization() {
        var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        var block = new DataBlock(bytes);
        var blockAsByteArray = block.toByteArray();

        assertThat(blockAsByteArray)
            .containsExactly(-1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testEmptyBlockSerialization() {
        var bytes = new byte[0];
        var block = new DataBlock(bytes);
        var blockAsByteArray = block.toByteArray();

        assertThat(blockAsByteArray)
            .containsExactly(-1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0);
    }

    @Test
    public void testBlockDeSerialization() {
        var bytes = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var block = DataBlock.fromByteArray(bytes);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getData()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    public void testMetaDataSizeConsistency() {
        var block = new DataBlock(new byte[0]);
        assertThat(block.getMetaDataSize()).isEqualTo(DataBlock.META_BYTES);
    }
}
