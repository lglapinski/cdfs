package io.contained.integration;

import io.contained.Container;
import io.contained.Filesystem;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class CdfsIntegrationTest {
    @Test
    public void cdfsTest() throws Exception {
        var fs = new Filesystem();
        Container container;
        try {
            container = fs.open(Paths.get("test.txt"));
        } catch (Exception e) {
            container = fs.create(Paths.get("test.txt"), 1);
        }
//        container.createFile("file1", "hello".getBytes());
        var dir = container.listDir("");
        System.out.println(dir.toString());
//        container.createDir("dir2/dir22");
//        container.rename("dir1", dir1"dir2");
//        container.createDir("dir4");
        dir = container.listDir("dir2");
        System.out.println(dir.toString());
        var file = container.read("file1");
        System.out.println(file.toString());
        container.close();
    }
}
