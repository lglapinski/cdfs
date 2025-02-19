package io.contained;

import io.contained.internals.AllocationTable;
import io.contained.internals.ContainerDescriptor;
import io.contained.internals.MetaDataBlock;
import io.contained.internals.Partition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class Filesystem {
    public Container create(Path path, int size) throws IOException {
        if (Files.exists(path)) {
            throw new IllegalArgumentException("Filesystem already exists: " + path);
        }

        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }

        var descriptor = new ContainerDescriptor(size);
        var allocationTable = new AllocationTable(descriptor.getBlockCount());

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        var output = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        var input = Files.newByteChannel(path, StandardOpenOption.READ);


        var partition = new Partition(input, output);
        partition.writeBytes(descriptor.toByteArray(), 0);
        partition.writeBytes(allocationTable.toByteArray(), ContainerDescriptor.BYTES);

        var container = new Container(partition, descriptor, allocationTable);

        var rootMetaData = new MetaDataBlock(new byte[0], true);
        partition.writeBytes(rootMetaData.toByteArray(), container.getMasterBlockSize());

        return container;
    }

    public Container open(Path path) throws IOException {
        if (Files.notExists(path)) {
            throw new IllegalArgumentException("Filesystem not found: " + path);
        }
        var output = Files.newByteChannel(path, StandardOpenOption.WRITE);
        var input = Files.newByteChannel(path, StandardOpenOption.READ);

        var partition = new Partition(input, output);

        var descriptorBytes = partition.readBytes(0, ContainerDescriptor.BYTES);
        var descriptor = ContainerDescriptor.fromByteArray(descriptorBytes);

        var allocationTableBytes = partition
            .readBytes(ContainerDescriptor.BYTES, AllocationTable.sizeOf(descriptor.getBlockCount()));
        var allocationTable = AllocationTable.fromByteArray(allocationTableBytes);
        //TODO: validate partition

        return new Container(partition, descriptor, allocationTable);
    }
}
