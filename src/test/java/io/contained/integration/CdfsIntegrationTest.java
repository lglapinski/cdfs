package io.contained.integration;

import io.contained.Container;
import io.contained.Filesystem;
import io.contained.internals.util.ByteArrayTransformer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CdfsIntegrationTest {
    @Test
    public void testFilesystemCreationSuccess() throws IOException {
        var testFilePath = Paths.get("testCreateSuccess");
        try (var container = Filesystem.create(testFilePath, 1)) {
            assertThat(container).isNotNull();
            assertThat(Files.exists(testFilePath)).isTrue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Files.delete(testFilePath);
        }
    }

    @Test
    public void testFilesystemCreationFailure() throws IOException {
        var testFilePath = Paths.get("testCreateFailure");
        Files.createFile(testFilePath);
        assertThatThrownBy(() -> {
            try (var c = Filesystem.create(testFilePath, 1)) {
                assertThat(c).isNotNull();
            }
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Filesystem already exists: " + testFilePath);
        Files.delete(testFilePath);
    }

    @Test
    public void testSuccessfulFilesystemOpen() throws Exception {
        var testFilePath = Paths.get("testOpenSuccess");
        var container = Filesystem.create(testFilePath, 1);
        container.close();

        try (var openedContainer = Filesystem.open(testFilePath)) {
            assertThat(openedContainer).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Files.delete(testFilePath);
        }
    }

    @Test
    public void testProjectDirectoryOperations() throws Exception {
        var testFilePath = Paths.get("testProjectDirectoryOperations");
        var filesToDelete = new ArrayList<String>();
        var filesToKeep = new ArrayList<String>();
        var currDirPath = Paths.get("");
        try (var container = Filesystem.create(testFilePath, 3)) {
            var createdFiles = copyFilesAndDirectoriesToContainer(currDirPath, container, testFilePath);
            listContainerContent(currDirPath, container, testFilePath);

            var seventyPercentThreshold = (int) Math.ceil((float) createdFiles.size() * 0.7F);
            filesToDelete.addAll(createdFiles.subList(0, seventyPercentThreshold));
            filesToKeep.addAll(createdFiles.subList(seventyPercentThreshold, createdFiles.size()));
            removeFilesFromContainer(container, filesToDelete);

            var deletedAllFiles = true;
            for (var file : filesToDelete) {
                try {
                    container.read(file);
                    deletedAllFiles = false;
                    break;
                } catch (Exception e) {
                    //Ignore it as we expect to fail to read all of them
                }
            }
            assertThat(deletedAllFiles).isTrue();

            var keptAllFiles = true;
            for (var file : filesToKeep) {
                try {
                    container.read(file);
                } catch (Exception e) {
                    keptAllFiles = false;
                    break;
                }
            }
            assertThat(keptAllFiles).isTrue();

            container.createDir("/newRoot");
            filesToKeep.addAll(copyFilesAndDirectoriesToContainer(currDirPath, container, testFilePath, "newRoot"));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (var container = Filesystem.open(testFilePath)) {
            var deletedAllFiles = true;
            for (var file : filesToDelete) {
                try {
                    container.read(file);
                    deletedAllFiles = false;
                    break;
                } catch (Exception e) {
                    //Ignore it as we expect to fail to read all of them
                }
            }
            assertThat(deletedAllFiles).isTrue();

            var keptAllFiles = true;
            for (var file : filesToKeep) {
                try {
                    var filePathOnDisk = file.replace("newRoot", "");
                    var physicalFile = Files.readAllBytes(Paths.get(currDirPath.toAbsolutePath().toString(), filePathOnDisk));
                    var containedFile = container.read(file);
                    assertThat(ByteArrayTransformer.toString(containedFile.data()))
                        .isEqualTo(ByteArrayTransformer.toString(physicalFile));
                } catch (Exception e) {
                    keptAllFiles = false;
                    break;
                }
            }
            assertThat(keptAllFiles).isTrue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Files.delete(testFilePath);
        }
    }

    private List<String> copyFilesAndDirectoriesToContainer(Path currDir, Container container, Path testFilePath) throws IOException {
        return copyFilesAndDirectoriesToContainer(currDir, container, testFilePath, "");
    }

    private List<String> copyFilesAndDirectoriesToContainer(Path currDir, Container container, Path testFilePath, String parent) throws IOException {
        var createdFiles = new ArrayList<String>();
        try (Stream<Path> stream = Files.walk(currDir)) {
            stream.forEach(path -> {
                var process = !path.equals(currDir)
                    && !path.equals(testFilePath)
                    && !path.toString().startsWith("target")
                    && !path.toString().startsWith(".git");
                if (process) {
                    try {
                        if (Files.isDirectory(path)) {
                            container.createDir(parent + "/" + path);
                        } else {
                            container.createFile(parent + "/" + path, Files.readAllBytes(path));
                            createdFiles.add(parent + "/" + path);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return createdFiles;
    }

    private void listContainerContent(Path currDir, Container container, Path testFilePath) throws IOException {
        try (Stream<Path> stream = Files.walk(currDir)) {
            stream.forEach(path -> {
                var process = !path.equals(currDir)
                    && !path.equals(testFilePath)
                    && !path.toString().startsWith("target")
                    && !path.toString().startsWith(".git");
                if (process) {
                    try {
                        if (Files.isDirectory(path)) {
                            var dir = container.listDir(path.toString());
                            System.out.println(dir);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private void removeFilesFromContainer(Container container, List<String> files) throws IOException {
        for (String file : files) {
            container.delete(file);
        }
    }
}
