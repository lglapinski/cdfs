package io.contained.internals;

import io.contained.internals.util.ByteArrayTransformer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MetaDataBlockTest {
    @Test
    public void testSimpleSingularBlockCreation() {
        var nameBytes = ByteArrayTransformer.fromString("dirName");
        var block = new MetaDataBlock(nameBytes);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getDataFullSize()).isEqualTo(0);
        assertThat(block.getDataSize()).isEqualTo(0);
        assertThat(block.getName()).containsExactly(nameBytes);
        assertThat(block.isDir()).isTrue();
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    public void testRename() {
        var nameBytes = ByteArrayTransformer.fromString("dirName");
        var newNameBytes = ByteArrayTransformer.fromString("newDirName");
        var block = new MetaDataBlock(nameBytes);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getDataFullSize()).isEqualTo(0);
        assertThat(block.getDataSize()).isEqualTo(0);
        assertThat(block.getName()).containsExactly(nameBytes);
        assertThat(block.isDir()).isTrue();
        assertThat(block.hasNextBlock()).isFalse();

        block.setName(newNameBytes);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getDataFullSize()).isEqualTo(0);
        assertThat(block.getDataSize()).isEqualTo(0);
        assertThat(block.getName()).containsExactly(newNameBytes);
        assertThat(block.isDir()).isTrue();
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    public void testResize() {
        var nameBytes = ByteArrayTransformer.fromString("dirName");
        var block = new MetaDataBlock(nameBytes);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getDataFullSize()).isEqualTo(0);
        assertThat(block.getDataSize()).isEqualTo(0);
        assertThat(block.getName()).containsExactly(nameBytes);
        assertThat(block.isDir()).isTrue();
        assertThat(block.hasNextBlock()).isFalse();

        block.setDataFullSize(10);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getDataFullSize()).isEqualTo(10);
        assertThat(block.getDataSize()).isEqualTo(0);
        assertThat(block.getName()).containsExactly(nameBytes);
        assertThat(block.isDir()).isTrue();
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    public void testBlockSerialization() {
        var nameBytes = ByteArrayTransformer.fromString("dirName");
        var block = new MetaDataBlock(nameBytes);
        var blockAsByteArray = block.toByteArray();

        assertThat(blockAsByteArray)
            .containsExactly(100, 105, 114, 78, 97, 109, 101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0);
    }

    @Test
    public void testBlockDeSerialization() {
        var bytes = new byte[]{100, 105, 114, 78, 97, 109, 101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0};
        var nameBytes = ByteArrayTransformer.fromString("dirName");

        var block = MetaDataBlock.fromByteArray(bytes);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getDataFullSize()).isEqualTo(0);
        assertThat(block.getDataSize()).isEqualTo(0);
        assertThat(block.getName()).containsExactly(nameBytes);
        assertThat(block.isDir()).isTrue();
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    public void testBlockDeSerializationWithMaxNameLength() {
        var bytes = new byte[]{100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97,
            109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101,
            100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105,
            114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78,
            97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109,
            101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100,
            105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114,
            78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97,
            109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101,
            100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105,
            114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78,
            97, 109, 101, 100, 105, 114, 78, 0, 0, 0, 0, 1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0};
        var nameBytes = new byte[]{100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78,
            97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109,
            101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100,
            105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114,
            78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97,
            109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101,
            100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105,
            114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78,
            97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109,
            101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100,
            105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114, 78, 97, 109, 101, 100, 105, 114,
            78, 97, 109, 101, 100, 105, 114, 78};

        var block = MetaDataBlock.fromByteArray(bytes);
        assertThat(block.getPrevBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getNextBlock()).isEqualTo(Configuration.noAddressMarker);
        assertThat(block.getDataFullSize()).isEqualTo(0);
        assertThat(block.getDataSize()).isEqualTo(0);
        assertThat(block.getName()).containsExactly(nameBytes);
        assertThat(block.isDir()).isTrue();
        assertThat(block.hasNextBlock()).isFalse();
    }

    @Test
    public void testMetaDataSizeConsistency() {
        DataBlock block = new MetaDataBlock(new byte[0]);
        assertThat(block.getMetaDataSize()).isEqualTo(MetaDataBlock.META_BYTES);
    }
}
