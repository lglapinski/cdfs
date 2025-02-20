package io.contained.internals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


public class PartitionTest {
    private final SeekableByteChannel input = Mockito.mock(SeekableByteChannel.class);
    private final SeekableByteChannel output = Mockito.mock(SeekableByteChannel.class);
    private final Partition partition = new Partition(input, output);

    @Test
    public void testRead() throws IOException {
        var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        var offset = 123;

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((ByteBuffer) args[0]).put(bytes);
            return bytes.length;
        }).when(input).read(any(ByteBuffer.class));

        var readBytes = partition.readBytes(offset, bytes.length);

        verify(input, times(1)).position(offset);
        assertThat(readBytes).containsExactly(bytes);
    }

    @Test
    public void testWrite() throws IOException {
        var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        var offset = 123;

        partition.writeBytes(bytes, offset);

        verify(output, times(1)).position(offset);
        verify(output, times(1)).write(argThat(
            (ByteBuffer bb) -> bb.equals(ByteBuffer.wrap(bytes))
        ));
    }

    @Test
    public void testClose() throws Exception {
        partition.close();
        verify(input, times(1)).close();
        verify(output, times(1)).close();
    }
}
