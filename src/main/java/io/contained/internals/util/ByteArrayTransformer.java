package io.contained.internals.util;

import io.contained.internals.Configuration;
import io.contained.internals.Inode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ByteArrayTransformer {
    private ByteArrayTransformer() {
    }

    public static int toInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
            ((bytes[offset + 1] & 0xFF) << 16) |
            ((bytes[offset + 2] & 0xFF) << 8) |
            ((bytes[offset + 3] & 0xFF));
    }

    public static void fromInt(int value, byte[] bytes, int offset) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static byte[] fromString(String string) {
        return string.getBytes(StandardCharsets.US_ASCII);
    }

    public static List<Inode> toInodesList(byte[] bytes) {
        List<Inode> inodes = new ArrayList<>();
        if (bytes != null) {
            for (int i = 0; i < bytes.length; i += Inode.BYTES) {
                var buff = ByteBuffer.wrap(bytes, i, Configuration.filenameLength);
                var name = StandardCharsets.US_ASCII.decode(buff).toString().trim();
                var block = ByteArrayTransformer.toInt(bytes, i + Configuration.filenameLength);
                var isDir = bytes[i + Configuration.filenameLength + Integer.BYTES] != 0;
                inodes.add(new Inode(name, block, isDir));
            }
        }
        return inodes;
    }

    public static byte[] fromInodesList(List<Inode> inodes) {
        if (inodes.isEmpty()) {
            return new byte[0];
        }
        var bytes = new byte[inodes.size() * Inode.BYTES];

        var index = 0;
        for (var inode : inodes) {
            var buff = StandardCharsets.US_ASCII.encode(inode.getName());
            buff.get(bytes, index, buff.remaining());
            index += Configuration.filenameLength;

            ByteArrayTransformer.fromInt(inode.getBlock(), bytes, index);
            index += Integer.BYTES;

            bytes[index] = inode.isDir() ? (byte) 1 : (byte) 0;
            index += Byte.BYTES;
        }
        return bytes;
    }
}
