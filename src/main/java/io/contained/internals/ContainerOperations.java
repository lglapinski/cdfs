package io.contained.internals;

import io.contained.internals.util.ByteArrayTransformer;

import java.io.IOException;
import java.util.*;

public abstract class ContainerOperations implements AutoCloseable {
    private final ContainerDescriptor descriptor;
    private final AllocationTable allocationTable;
    private final int masterBlockSize;
    private final Partition partition;

    protected ContainerOperations(ContainerDescriptor descriptor, AllocationTable allocationTable,
                                  int masterBlockSize, Partition partition) {
        this.descriptor = descriptor;
        this.allocationTable = allocationTable;
        this.masterBlockSize = masterBlockSize;
        this.partition = partition;
    }

    //TODO: improve encapsulation
    public int getMasterBlockSize() {
        return masterBlockSize;
    }

    protected int relativePosition(int position) {
        return position * descriptor.getBlockSize() + masterBlockSize;
    }

    protected MetaDataBlock readMetaDataBlock(int position) throws IOException {
        byte[] bytes = partition.readBytes(relativePosition(position), descriptor.getBlockSize());
        return MetaDataBlock.fromByteArray(bytes);
    }

    protected DataBlock readDataBlock(int position) throws IOException {
        byte[] bytes = partition.readBytes(relativePosition(position), DataBlock.META_BYTES);
        return DataBlock.fromByteArray(bytes);
    }

    protected void writeBlock(DataBlock dataBlock, int position) throws IOException {
        partition.writeBytes(dataBlock.toByteArray(), relativePosition(position));
    }

    protected List<Inode> readInodes(MetaDataBlock metaDataBlock) throws IOException {
        byte[] data = readAllBytes(metaDataBlock);
        return ByteArrayTransformer.toInodesList(data);
    }

    protected byte[] readAllBytes(MetaDataBlock metaDataBlock) throws IOException {
        byte[] data = null;
        if (metaDataBlock.getDataFullSize() > metaDataBlock.getDataSize()) {
            data = new byte[metaDataBlock.getDataFullSize()];
            DataBlock dataBlock = metaDataBlock;
            var currentPosition = 0;

            while (dataBlock.hasNextBlock()) {
                System.arraycopy(dataBlock.getData(), 0, data, currentPosition, dataBlock.getDataSize());
                currentPosition += dataBlock.getDataSize();
                dataBlock = readDataBlock(dataBlock.getNextBlock());
            }

            System.arraycopy(dataBlock.getData(), 0, data, currentPosition, dataBlock.getDataSize());

        } else if (metaDataBlock.getDataSize() > 0) {
            data = metaDataBlock.getData();
        }
        return data;
    }

    protected MetaDataBlock traverseTo(Path path) throws IOException {
        var currentBlock = 0;
        MetaDataBlock metaDataBlock;
        var i = 0;
        do {
            metaDataBlock = readMetaDataBlock(currentBlock);

            if (i < path.size() - 1) {
                currentBlock = getChildBlock(metaDataBlock, path.getPart(i + 1));
            }
            i++;
        } while (i < path.size());
        return metaDataBlock;
    }

    protected int getChildBlock(MetaDataBlock parent, String name) throws IOException {
        if (!parent.isDir()) {
            throw new IllegalArgumentException("Specified path is not a directory");
        }

        var inodes = readInodes(parent);
        int currentBlock = inodes.stream()
            .filter(inode -> inode.getName().equals(name))
            .findAny()
            .map(Inode::getBlock)
            .orElse(Configuration.noAddressMarker);

        if (currentBlock == Configuration.noAddressMarker) {
            throw new IllegalArgumentException("Directory not found");
        }
        return currentBlock;
    }

    protected int occupyBlocks(int dataFullSize) {
        var dataInDataBlocksSize = dataFullSize - (descriptor.getBlockSize() - MetaDataBlock.META_BYTES);
        if (dataInDataBlocksSize <= 0) {
            return 1;
        }
        var occupiedBlocks = (int) Math.ceil((double) dataInDataBlocksSize / descriptor.getBlockSize() - DataBlock.META_BYTES);
        return occupiedBlocks + 1;
    }

    protected List<Integer> getAvailableBlocks(int numberOfBlocks) {
        return allocationTable.getAvailableBlocks(numberOfBlocks);
    }

    protected void allocateBlocks(List<Integer> blocks) {
        allocationTable.allocateBlocks(blocks);
    }

    protected void freeBlocks(List<Integer> blocks) {
        allocationTable.freeBlocks(blocks);
    }

    protected void writeFile(String name, byte[] bytes, List<Integer> blocks) throws IOException {
        if (blocks.size() == 1) {
            var dataBlock = new MetaDataBlock(
                ByteArrayTransformer.fromString(name),
                bytes.length,
                false,
                Configuration.noAddressMarker,
                Configuration.noAddressMarker,
                Arrays.copyOfRange(bytes, 0, bytes.length)
            );
            writeBlock(dataBlock, blocks.getFirst());
        } else {
            var dataIndex = 0;
            for (int i = 0; i < blocks.size() - 1; i++) {
                int capacity;
                if (i == 0) {
                    capacity = descriptor.getBlockSize() - MetaDataBlock.META_BYTES;
                    var dataBlock = new MetaDataBlock(
                        ByteArrayTransformer.fromString(name),
                        bytes.length,
                        false,
                        Configuration.noAddressMarker,
                        blocks.get(i + 1),
                        Arrays.copyOfRange(bytes, dataIndex, dataIndex + capacity)
                    );
                    writeBlock(dataBlock, blocks.get(i));
                } else {
                    capacity = descriptor.getBlockSize() - DataBlock.META_BYTES;
                    var dataBlock = new DataBlock(
                        blocks.get(i - 1),
                        blocks.get(i + 1),
                        Arrays.copyOfRange(bytes, dataIndex, dataIndex + capacity)
                    );
                    writeBlock(dataBlock, blocks.get(i));
                }
                dataIndex += capacity;
            }

            var dataBlock = new DataBlock(
                blocks.get(blocks.size() - 2),
                Arrays.copyOfRange(bytes, dataIndex, bytes.length)
            );
            writeBlock(dataBlock, blocks.getLast());
        }
    }

    protected void writeFile(MetaDataBlock metaDataBlock, int position, byte[] bytes) throws IOException {
        if (metaDataBlock.getDataFullSize() <= bytes.length) {
            write(metaDataBlock, position, bytes, true);
        } else if (metaDataBlock.getDataFullSize() > bytes.length) {
            shrink(metaDataBlock, position, bytes);
        }
    }

    protected void appendFile(MetaDataBlock metaDataBlock, int position, byte[] bytes) throws IOException {
        Map<Integer, DataBlock> blocksToWrite = new HashMap<>();
        metaDataBlock.setDataFullSize(metaDataBlock.getDataFullSize() + bytes.length);
        blocksToWrite.put(position, metaDataBlock);

        DataBlock dataBlock = metaDataBlock;
        var currentPosition = position;
        var dataIndex = 0;

        while (dataBlock.hasNextBlock()) {
            dataBlock = readDataBlock(currentPosition);
            currentPosition = dataBlock.getNextBlock();
        }

        var capacity = descriptor.getBlockSize() - dataBlock.getMetaDataSize();
        List<Integer> availableBlocks = new ArrayList<>();

        if (dataBlock.getDataSize() + bytes.length < capacity) {
            var tempBytes = new byte[dataBlock.getDataSize() + bytes.length];
            System.arraycopy(dataBlock.getData(), 0, tempBytes, 0, dataBlock.getDataSize());
            System.arraycopy(bytes, dataIndex, tempBytes, dataBlock.getDataSize(), bytes.length);

            dataBlock.setData(tempBytes);
            blocksToWrite.put(currentPosition, dataBlock);
        } else {
            var tempBytes = new byte[capacity];
            System.arraycopy(dataBlock.getData(), 0, tempBytes, 0, dataBlock.getDataSize());
            System.arraycopy(bytes, dataIndex, tempBytes, dataBlock.getDataSize(), capacity);

            dataBlock.setData(tempBytes);
            dataIndex += capacity;
            blocksToWrite.put(currentPosition, dataBlock);

            var requiredBlocks = occupyBlocks(bytes.length - dataIndex);
            availableBlocks = getAvailableBlocks(requiredBlocks);
            capacity = descriptor.getBlockSize() - DataBlock.META_BYTES;
            for (var block : availableBlocks) {
                dataBlock.setNextBlock(block);
                dataBlock = new DataBlock(currentPosition, Arrays.copyOfRange(bytes, dataIndex, Math.min(bytes.length, dataIndex + capacity)));
                currentPosition = block;
                blocksToWrite.put(block, dataBlock);
            }
        }

        for (var entry : blocksToWrite.entrySet()) {
            writeBlock(entry.getValue(), entry.getKey());
        }

        allocateBlocks(availableBlocks);
    }

    protected void writeDir(MetaDataBlock metaDataBlock, int position, byte[] bytes) throws IOException {
        if (metaDataBlock.getDataFullSize() <= bytes.length) {
            write(metaDataBlock, position, bytes, metaDataBlock.getDataFullSize() == bytes.length);
        } else if (metaDataBlock.getDataFullSize() > bytes.length) {
            shrink(metaDataBlock, position, bytes);
        }
    }

    protected void deleteDirOrFile(MetaDataBlock metaDataBlock, int position) throws IOException {
        List<Integer> orphanedBlocks = new ArrayList<>();
        orphanedBlocks.add(position);

        DataBlock dataBlock = metaDataBlock;
        while (dataBlock.hasNextBlock()) {
            orphanedBlocks.add(dataBlock.getNextBlock());
            dataBlock = readDataBlock(dataBlock.getNextBlock());
        }

        freeBlocks(orphanedBlocks);
    }

    private void shrink(MetaDataBlock metaDataBlock, int position, byte[] bytes) throws IOException {
        List<Integer> orphanedBlocks = new ArrayList<>();

        Map<Integer, DataBlock> blocksToWrite = new HashMap<>();
        metaDataBlock.setDataFullSize(bytes.length);
        blocksToWrite.put(position, metaDataBlock);

        DataBlock dataBlock = metaDataBlock;
        var currentPosition = position;
        var dataIndex = 0;

        while (dataBlock.hasNextBlock()) {
            dataBlock = readDataBlock(currentPosition);
            dataBlock.setData(Arrays.copyOfRange(bytes, dataIndex, Math.min(bytes.length, dataIndex + dataBlock.getDataSize())));
            blocksToWrite.put(currentPosition, dataBlock);

            currentPosition = dataBlock.getNextBlock();
            dataIndex += dataBlock.getDataSize();
            if (dataIndex >= bytes.length) {
                dataBlock.setNextBlock(Configuration.noAddressMarker);
                orphanedBlocks.add(currentPosition);
            }
        }
        dataBlock = readDataBlock(currentPosition);
        while (dataBlock.hasNextBlock()) {
            orphanedBlocks.add(dataBlock.getNextBlock());
            dataBlock = readDataBlock(dataBlock.getNextBlock());
        }

        for (var entry : blocksToWrite.entrySet()) {
            writeBlock(entry.getValue(), entry.getKey());
        }

        freeBlocks(orphanedBlocks);
    }

    private void write(MetaDataBlock metaDataBlock, int position, byte[] bytes, boolean overwrite) throws IOException {
        Map<Integer, DataBlock> blocksToWrite = new HashMap<>();
        metaDataBlock.setDataFullSize(bytes.length);
        blocksToWrite.put(position, metaDataBlock);

        DataBlock dataBlock = metaDataBlock;
        var currentPosition = position;
        var dataIndex = 0;

        while (dataBlock.hasNextBlock()) {
            dataBlock = readDataBlock(currentPosition);

            if (overwrite) {
                dataBlock.setData(Arrays.copyOfRange(bytes, dataIndex, dataIndex + dataBlock.getDataSize()));
                blocksToWrite.put(currentPosition, dataBlock);
            }

            currentPosition = dataBlock.getNextBlock();
            dataIndex += dataBlock.getDataSize();
        }

        var capacity = descriptor.getBlockSize() - dataBlock.getMetaDataSize();
        List<Integer> availableBlocks = new ArrayList<>();

        if (bytes.length - dataIndex < capacity) {
            dataBlock.setData(Arrays.copyOfRange(bytes, dataIndex, bytes.length));
            blocksToWrite.put(currentPosition, dataBlock);
        } else {
            dataBlock.setData(Arrays.copyOfRange(bytes, dataIndex, dataIndex + capacity));
            dataIndex += capacity;
            blocksToWrite.put(currentPosition, dataBlock);

            var requiredBlocks = occupyBlocks(bytes.length - dataIndex);
            availableBlocks = getAvailableBlocks(requiredBlocks);
            capacity = descriptor.getBlockSize() - DataBlock.META_BYTES;
            for (var block : availableBlocks) {
                dataBlock.setNextBlock(block);
                dataBlock = new DataBlock(currentPosition, Arrays.copyOfRange(bytes, dataIndex, Math.min(bytes.length, dataIndex + capacity)));
                currentPosition = block;
                blocksToWrite.put(block, dataBlock);
            }
        }

        for (var entry : blocksToWrite.entrySet()) {
            writeBlock(entry.getValue(), entry.getKey());
        }

        allocateBlocks(availableBlocks);
    }

    @Override
    public void close() throws Exception {
        partition.writeBytes(allocationTable.toByteArray(), ContainerDescriptor.BYTES);
        partition.close();
    }
}
