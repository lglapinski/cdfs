package io.contained.internals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class Partition implements AutoCloseable {
    private final SeekableByteChannel input;
    private final SeekableByteChannel output;

    public Partition(SeekableByteChannel input, SeekableByteChannel output) {
        this.input = input;
        this.output = output;
    }

    public byte[] readBytes(int offset, int length) throws IOException {
        var buffer = ByteBuffer.allocate(length);
        input.position(offset);
        input.read(buffer);

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public void writeBytes(byte[] bytes, int offset) throws IOException {
        output.position(offset);
        output.write(ByteBuffer.wrap(bytes));
    }

    @Override
    public void close() throws Exception {
        input.close();
        output.close();
    }
}
