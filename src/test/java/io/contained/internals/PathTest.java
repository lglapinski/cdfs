package io.contained.internals;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PathTest {
    @Test
    public void testUnixDelimiter() {
        var stringPath = "/foo/bar";
        var path = new Path(stringPath);
        assertThat(path.size()).isEqualTo(3);
        assertThat(path.getName()).isEqualTo("bar");
        assertThat(path.toString()).isEqualTo(stringPath);
    }

    @Test
    public void testWindowsDelimiter() {
        var stringPath = "\\foo\\bar";
        var path = new Path(stringPath);
        assertThat(path.size()).isEqualTo(3);
        assertThat(path.getName()).isEqualTo("bar");
        assertThat(path.toString()).isEqualTo(stringPath);
    }

    @Test
    public void testRootDirectory() {
        var path1 = new Path("");
        var path2 = new Path("/");

        assertThat(path1.size())
            .isEqualTo(path2.size())
            .isEqualTo(0);
        assertThat(path1.getName())
            .isEqualTo(path2.getName())
            .isEqualTo("");

        assertThat(path1.getParentPath())
            .isEqualTo(path2.getParentPath())
            .isEqualTo(null);

        assertThat(path1.toString())
            .isEqualTo(path2.toString())
            .isEqualTo("/");
    }

    @Test
    public void testFirstLevelPath() {
        var path = new Path("foo");

        assertThat(path.size()).isEqualTo(2);
        assertThat(path.getName()).isEqualTo("foo");
        assertThat(path.getParentPath().toString()).isEqualTo("/");
    }

    @Test
    public void testJoin() {
        var path = new Path("/foo");
        var joined = path.join("bar");

        assertThat(joined).isEqualTo("/foo/bar");

        path = new Path("");

        joined = path.join("foo");
        assertThat(joined).isEqualTo("/foo");
    }

    @Test
    public void testExtractingParts() {
        var path = new Path("/foo/bar");
        var part = path.getPart(0);
        assertThat(part).isEqualTo("");
        part = path.getPart(1);
        assertThat(part).isEqualTo("foo");
        part = path.getPart(2);
        assertThat(part).isEqualTo("bar");
    }
}
