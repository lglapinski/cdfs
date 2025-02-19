package io.contained;

import java.util.List;

public class ContainedDir {
    private String name;
    private String path;
    private List<String> subDirs;
    private List<String> files;

    public ContainedDir(String name, String parentPath, List<String> subDirs, List<String> files) {
        this.name = name;
        this.path = parentPath;
        this.subDirs = subDirs;
        this.files = files;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public List<String> getSubDirs() {
        return subDirs;
    }

    public List<String> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        return "ContainedDir{" +
            "name='" + name + '\'' +
            ", path='" + path + '\'' +
            ", subDirs=" + subDirs +
            ", files=" + files +
            '}';
    }
}
