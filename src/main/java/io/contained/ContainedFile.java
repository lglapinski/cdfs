package io.contained;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ContainedFile {
    private String name;
    private String path;
    private byte[] data;

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public byte[] getData() {
        return data;
    }

    public ContainedFile(String name, String parentPath, byte[] data) {
        this.name = name;
        this.path = parentPath;
        this.data = data;
    }

    @Override
    public String toString() {
        return "ContainedFile{" +
            "name='" + name + '\'' +
            ", path='" + path + '\'' +
            ", data=" + new String(data, StandardCharsets.US_ASCII) +
            '}';
    }
}
