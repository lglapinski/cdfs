package io.contained;

import java.nio.charset.StandardCharsets;

public record ContainedFile(String name, String path, byte[] data) {
    @Override
    public String toString() {
        return "ContainedFile {" +
            "name='" + name + '\'' +
            ", path='" + path + '\'' +
            ", data=" + new String(data, StandardCharsets.US_ASCII) +
            '}';
    }
}
