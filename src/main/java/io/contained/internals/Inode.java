package io.contained.internals;

public class Inode {
    public static final int BYTES = Configuration.filenameLength + Integer.BYTES + Byte.BYTES;
    private String name;
    private final int block;
    private final boolean isDir;

    public Inode(String name, int block, boolean isDir) {
        this.name = name;
        this.block = block;
        this.isDir = isDir;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBlock() {
        return block;
    }

    public boolean isDir() {
        return isDir;
    }
}
