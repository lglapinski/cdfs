package io.contained;

import io.contained.internals.*;
import io.contained.internals.util.ByteArrayTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ContainerTest {
    private final Partition partition = Mockito.mock(Partition.class);
    private final ContainerDescriptor descriptor = new ContainerDescriptor(1);

    @BeforeEach
    public void setup() {
        Mockito.reset(partition);
    }

    @Test
    public void testCreateDir() {
        try (var container = createContainer()) {
            var rootDirBlock = new MetaDataBlock(new byte[0]);
            var rootOffset = container.getMasterBlockSize();
            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());

            container.createDir("/dirPath");

            var inodes = List.of(new Inode("dirPath", 1, true));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            rootDirBlock.setDataFullSize(inodesByteArray.length);
            rootDirBlock.setData(inodesByteArray);

            var newDirBlock = new MetaDataBlock(ByteArrayTransformer.fromString("dirPath"));
            var newOffset = container.getMasterBlockSize() + descriptor.getBlockSize();

            verify(partition, times(1)).writeBytes(newDirBlock.toByteArray(), newOffset);
            verify(partition, times(1)).writeBytes(rootDirBlock.toByteArray(), rootOffset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCreateFile() {
        try (var container = createContainer()) {
            var data = "Dummy data".getBytes(StandardCharsets.US_ASCII);
            var rootDirBlock = new MetaDataBlock(new byte[0]);
            var rootOffset = container.getMasterBlockSize();
            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());

            container.createFile("/filePath", data);

            var inodes = List.of(new Inode("filePath", 1, false));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            rootDirBlock.setDataFullSize(inodesByteArray.length);
            rootDirBlock.setData(inodesByteArray);

            var newFileBlock = new MetaDataBlock(ByteArrayTransformer.fromString("filePath"), data.length, false, data);
            var newOffset = container.getMasterBlockSize() + descriptor.getBlockSize();

            verify(partition, times(1)).writeBytes(newFileBlock.toByteArray(), newOffset);
            verify(partition, times(1)).writeBytes(rootDirBlock.toByteArray(), rootOffset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testWriteToFile() {
        try (var container = createContainer()) {
            var data = "Updated dummy data".getBytes(StandardCharsets.US_ASCII);

            var inodes = List.of(new Inode("filePath", 1, false));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            var rootDirBlock = new MetaDataBlock(new byte[0], inodesByteArray.length, true, inodesByteArray);

            var currentData = "Dummy data".getBytes(StandardCharsets.US_ASCII);
            var fileBlock = new MetaDataBlock(ByteArrayTransformer.fromString("filePath"), currentData.length, false, currentData);
            var rootOffset = container.getMasterBlockSize();
            var fileOffset = container.getMasterBlockSize() + descriptor.getBlockSize();

            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());

            when(partition.readBytes(eq(fileOffset), eq(descriptor.getBlockSize())))
                .thenReturn(fileBlock.toByteArray());

            container.write("/filePath", data);

            fileBlock.setDataFullSize(data.length);
            fileBlock.setData(data);

            verify(partition, times(1)).writeBytes(fileBlock.toByteArray(), fileOffset);
            verify(partition, never()).writeBytes(any(), eq(rootOffset));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAppendToFile() {
        try (var container = createContainer()) {
            var data = " appended".getBytes(StandardCharsets.US_ASCII);

            var inodes = List.of(new Inode("filePath", 1, false));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            var rootDirBlock = new MetaDataBlock(new byte[0], inodesByteArray.length, true, inodesByteArray);

            var currentData = "Dummy data".getBytes(StandardCharsets.US_ASCII);
            var fileBlock = new MetaDataBlock(ByteArrayTransformer.fromString("filePath"), currentData.length, false, currentData);
            var rootOffset = container.getMasterBlockSize();
            var fileOffset = container.getMasterBlockSize() + descriptor.getBlockSize();

            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());

            when(partition.readBytes(eq(fileOffset), eq(descriptor.getBlockSize())))
                .thenReturn(fileBlock.toByteArray());

            container.append("/filePath", data);

            fileBlock.setDataFullSize(fileBlock.getDataFullSize() + data.length);
            fileBlock.setData("Dummy data appended".getBytes(StandardCharsets.US_ASCII));

            verify(partition, times(1)).writeBytes(fileBlock.toByteArray(), fileOffset);
            verify(partition, never()).writeBytes(any(), eq(rootOffset));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testListDir() {
        try (var container = createContainer()) {
            var inodes = List.of(new Inode("filePath", 1, false), new Inode("dirPath", 1, true));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            var rootDirBlock = new MetaDataBlock(new byte[0], inodesByteArray.length, true, inodesByteArray);
            var rootOffset = container.getMasterBlockSize();

            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());

            var dir = container.listDir("/");
            assertThat(dir.subDirs()).containsExactly("dirPath");
            assertThat(dir.files()).containsExactly("filePath");
            assertThat(dir.name()).isEqualTo("");
            assertThat(dir.path()).isEqualTo("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testReadFile() {
        try (var container = createContainer()) {
            var inodes = List.of(new Inode("filePath", 1, false));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            var rootDirBlock = new MetaDataBlock(new byte[0], inodesByteArray.length, true, inodesByteArray);

            var data = "Dummy data".getBytes(StandardCharsets.US_ASCII);
            var fileBlock = new MetaDataBlock(ByteArrayTransformer.fromString("filePath"), data.length, false, data);
            var rootOffset = container.getMasterBlockSize();
            var fileOffset = container.getMasterBlockSize() + descriptor.getBlockSize();

            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());

            when(partition.readBytes(eq(fileOffset), eq(descriptor.getBlockSize())))
                .thenReturn(fileBlock.toByteArray());

            var file = container.read("/filePath");
            assertThat(file.data()).containsExactly(data);
            assertThat(file.name()).isEqualTo("filePath");
            assertThat(file.path()).isEqualTo("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeleteDir() {
        try (var container = createContainer()) {
            var inodes = List.of(new Inode("dirPath", 1, true));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            var rootDirBlock = new MetaDataBlock(new byte[0], inodesByteArray.length, true, inodesByteArray);

            var dirBlock = new MetaDataBlock(ByteArrayTransformer.fromString("dirPath"));
            var rootOffset = container.getMasterBlockSize();
            var dirOffset = container.getMasterBlockSize() + descriptor.getBlockSize();

            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());
            when(partition.readBytes(eq(dirOffset), eq(descriptor.getBlockSize())))
                .thenReturn(dirBlock.toByteArray());

            container.deleteDir("/dirPath");

            rootDirBlock.setDataFullSize(0);
            rootDirBlock.setData(new byte[0]);

            verify(partition, times(1)).writeBytes(rootDirBlock.toByteArray(), rootOffset);
            verify(partition, never()).writeBytes(any(), eq(dirOffset));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeleteRoot() {
        try (var container = createContainer()) {
            assertThatThrownBy(() -> container.deleteDir("/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete root directory");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeleteFile() {
        try (var container = createContainer()) {
            var inodes = List.of(new Inode("filePath", 1, true));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            var rootDirBlock = new MetaDataBlock(new byte[0], inodesByteArray.length, true, inodesByteArray);

            var data = "Dummy data".getBytes(StandardCharsets.US_ASCII);
            var fileBlock = new MetaDataBlock(ByteArrayTransformer.fromString("filePath"), data.length, false, data);
            var rootOffset = container.getMasterBlockSize();
            var fileOffset = container.getMasterBlockSize() + descriptor.getBlockSize();

            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());
            when(partition.readBytes(eq(fileOffset), eq(descriptor.getBlockSize())))
                .thenReturn(fileBlock.toByteArray());

            container.delete("/filePath");

            rootDirBlock.setDataFullSize(0);
            rootDirBlock.setData(new byte[0]);

            verify(partition, times(1)).writeBytes(rootDirBlock.toByteArray(), rootOffset);
            verify(partition, never()).writeBytes(any(), eq(fileOffset));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRename() {
        try (var container = createContainer()) {
            var inodes = List.of(new Inode("filePath", 1, true));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            var rootDirBlock = new MetaDataBlock(new byte[0], inodesByteArray.length, true, inodesByteArray);

            var data = "Dummy data".getBytes(StandardCharsets.US_ASCII);
            var fileBlock = new MetaDataBlock(ByteArrayTransformer.fromString("filePath"), data.length, false, data);
            var rootOffset = container.getMasterBlockSize();
            var fileOffset = container.getMasterBlockSize() + descriptor.getBlockSize();

            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());
            when(partition.readBytes(eq(fileOffset), eq(descriptor.getBlockSize())))
                .thenReturn(fileBlock.toByteArray());

            container.rename("/filePath", "newFilePath");

            fileBlock.setName("newFilePath".getBytes(StandardCharsets.US_ASCII));
            inodes.getFirst().setName("newFilePath");
            inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            rootDirBlock.setDataFullSize(inodesByteArray.length);
            rootDirBlock.setData(inodesByteArray);

            verify(partition, times(1)).writeBytes(fileBlock.toByteArray(), fileOffset);
            verify(partition, times(1)).writeBytes(rootDirBlock.toByteArray(), rootOffset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMove() {
        try (var container = createContainer()) {
            var inodes = List.of(new Inode("dirPath", 1, true));
            var inodesByteArray = ByteArrayTransformer.fromInodesList(inodes);
            var rootDirBlock = new MetaDataBlock(new byte[0], inodesByteArray.length, true, inodesByteArray);

            var dirInodes = List.of(new Inode("filePath", 2, true));
            inodesByteArray = ByteArrayTransformer.fromInodesList(dirInodes);
            var dirBlock = new MetaDataBlock(ByteArrayTransformer.fromString("dirPath"), inodesByteArray.length, true, inodesByteArray);
            var rootOffset = container.getMasterBlockSize();
            var dirOffset = container.getMasterBlockSize() + descriptor.getBlockSize();
            var fileOffset = container.getMasterBlockSize() + descriptor.getBlockSize() * 2;

            when(partition.readBytes(eq(rootOffset), eq(descriptor.getBlockSize())))
                .thenReturn(rootDirBlock.toByteArray());
            when(partition.readBytes(eq(dirOffset), eq(descriptor.getBlockSize())))
                .thenReturn(dirBlock.toByteArray());

            container.move("/dirPath/filePath", "/");

            dirBlock.setDataFullSize(0);
            dirBlock.setData(new byte[0]);

            var inodesAfterMove = List.of(new Inode("dirPath", 1, true),
                new Inode("filePath", 2, true));
            inodesByteArray = ByteArrayTransformer.fromInodesList(inodesAfterMove);

            rootDirBlock.setDataFullSize(inodesByteArray.length);
            rootDirBlock.setData(inodesByteArray);

            verify(partition, times(1)).writeBytes(rootDirBlock.toByteArray(), rootOffset);
            verify(partition, times(1)).writeBytes(dirBlock.toByteArray(), dirOffset);
            verify(partition, never()).writeBytes(any(), eq(fileOffset));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Container createContainer() {
        var allocationTable = new AllocationTable(descriptor.getBlockCount());
        allocationTable.allocateBlocks(List.of(0));
        return new Container(partition, descriptor, allocationTable);
    }
}
