package io.contained;

import java.util.List;

public record ContainedDir(String name, String path, List<String> subDirs, List<String> files) {
    @Override
    public String toString() {
        return "ContainedDir {" +
            "name='" + name + '\'' +
            ", path='" + path + '\'' +
            ", subDirs=" + subDirs +
            ", files=" + files +
            '}';
    }
}
