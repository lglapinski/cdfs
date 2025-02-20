package io.contained;

import io.contained.internals.*;
import io.contained.internals.util.ByteArrayTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Container extends ContainerOperations {

    Container(Partition partition, ContainerDescriptor descriptor, AllocationTable allocationTable) {
        super(descriptor, allocationTable, ContainerDescriptor.BYTES + allocationTable.size(), partition);
    }

    public void createDir(String path) throws IOException {
        var pathToDir = new Path(path);
        var parentPath = pathToDir.getParentPath();
        var grandParentPath = parentPath.getParentPath();
        var parentPosition = 0;
        MetaDataBlock parentMetaDataBlock;

        if (grandParentPath != null) {
            parentMetaDataBlock = traverseTo(grandParentPath);
            parentPosition = getChildBlock(parentMetaDataBlock, parentPath.getName());
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        } else {
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        }

        if (!parentMetaDataBlock.isDir()) {
            throw new IOException(String.format("%s is not a dir", path));
        }

        var inodes = readInodes(parentMetaDataBlock);
        if (inodes.stream().anyMatch(inode -> inode.getName().equals(pathToDir.getName()))) {
            throw new IOException(String.format("%s already exists", path));
        }

        var newDirMetaDataBlock = new MetaDataBlock(ByteArrayTransformer.fromString(pathToDir.getName()));
        List<Integer> blocks = getAvailableBlocks(1);

        writeBlock(newDirMetaDataBlock, blocks.getFirst());
        allocateBlocks(blocks); //TODO: weak spot it may fail when updating indices leaving this block not accessible
        //I could pass this new dir block to expand method add to map and allocate block for it there... or pass information
        //there on how many blocks has to be allocated for new file/dir and save everything at the end

        inodes.add(new Inode(pathToDir.getName(), blocks.getFirst(), true));

        var inodesAsBytes = ByteArrayTransformer.fromInodesList(inodes);
        writeDir(parentMetaDataBlock, parentPosition, inodesAsBytes);
    }

    public void createFile(String path, byte[] data) throws IOException {
        var numberOfBlocksNeeded = occupyBlocks(data.length);
        List<Integer> blocks = getAvailableBlocks(numberOfBlocksNeeded);

        var pathToFile = new Path(path);

        var parentPath = pathToFile.getParentPath();
        var grandParentPath = parentPath.getParentPath();
        var parentPosition = 0;
        MetaDataBlock parentMetaDataBlock;

        if (grandParentPath != null) {
            parentMetaDataBlock = traverseTo(grandParentPath);
            parentPosition = getChildBlock(parentMetaDataBlock, parentPath.getName());
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        } else {
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        }

        if (!parentMetaDataBlock.isDir()) {
            throw new IOException(String.format("%s is not a dir", path));
        }

        var inodes = readInodes(parentMetaDataBlock);
        if (inodes.stream().anyMatch(inode -> inode.getName().equals(pathToFile.getName()))) {
            throw new IOException(String.format("%s already exists", path));
        }

        writeFile(pathToFile.getName(), data, blocks);
        allocateBlocks(blocks); //TODO: weak spot it may fail when updating indices leaving this block not accessible
        //I could pass this new dir block to expand method add to map and allocate block for it there... or pass information
        //there on how many blocks has to be allocated for new file/dir and save everything at the end

        inodes.add(new Inode(pathToFile.getName(), blocks.getFirst(), false));

        var inodesAsBytes = ByteArrayTransformer.fromInodesList(inodes);
        writeDir(parentMetaDataBlock, parentPosition, inodesAsBytes);
    }

    public void write(String path, byte[] data) throws IOException {
        var pathToFile = new Path(path);
        var metaDataBlock = traverseTo(pathToFile.getParentPath());
        var fileBlock = getChildBlock(metaDataBlock, pathToFile.getName());

        metaDataBlock = readMetaDataBlock(fileBlock);

        if (metaDataBlock.isDir()) {
            throw new IOException(String.format("%s is not a file", path));
        }
        writeFile(metaDataBlock, fileBlock, data);
    }

    public void append(String path, byte[] data) throws IOException {
        var pathToFile = new Path(path);
        var metaDataBlock = traverseTo(pathToFile.getParentPath());
        var fileBlock = getChildBlock(metaDataBlock, pathToFile.getName());

        metaDataBlock = readMetaDataBlock(fileBlock);

        if (metaDataBlock.isDir()) {
            throw new IOException(String.format("%s is not a file", path));
        }
        appendFile(metaDataBlock, fileBlock, data);
    }

    public ContainedDir listDir(String path) throws IOException {
        var pathToDir = new Path(path);
        var metaDataBlock = traverseTo(pathToDir);

        if (!metaDataBlock.isDir()) {
            throw new IOException(String.format("%s is not a dir", pathToDir));
        }

        var inodes = readInodes(metaDataBlock);
        var subDirs = new ArrayList<String>();
        var files = new ArrayList<String>();

        for (var inode : inodes) {
            if (inode.isDir()) {
                subDirs.add(inode.getName());
            } else {
                files.add(inode.getName());
            }
        }

        var parent = pathToDir.getParentPath();
        return new ContainedDir(pathToDir.getName(), parent != null ? parent.toString() : pathToDir.toString(), subDirs, files);
    }

    public ContainedFile read(String path) throws IOException {
        var pathToFile = new Path(path);
        var metaDataBlock = traverseTo(pathToFile);

        if (metaDataBlock.isDir()) {
            throw new IOException(String.format("%s is not a file", path));
        }

        var data = readAllBytes(metaDataBlock);

        return new ContainedFile(pathToFile.getName(), pathToFile.getParentPath().toString(), data);
    }

    public void deleteDir(String path) throws IOException {
        deleteDir(path, false);
    }

    public void deleteDir(String path, boolean recursive) throws IOException {
        var pathToDir = new Path(path);

        if (pathToDir.size() == 0) {
            throw new IllegalArgumentException("Cannot delete root directory");
        }

        var parentPath = pathToDir.getParentPath();
        var grandParentPath = parentPath.getParentPath();
        var parentPosition = 0;
        MetaDataBlock parentMetaDataBlock;

        if (grandParentPath != null) {
            parentMetaDataBlock = traverseTo(grandParentPath);
            parentPosition = getChildBlock(parentMetaDataBlock, parentPath.getName());
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        } else {
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        }

        var block = getChildBlock(parentMetaDataBlock, pathToDir.getName());
        var metaDataBlock = readMetaDataBlock(block);

        if (!metaDataBlock.isDir() && !recursive) {
            throw new IOException(String.format("%s is not a dir", path));
        }
        if (!recursive && metaDataBlock.getDataSize() > 0) {
            throw new IOException(String.format("%s is not empty", path));
        }

        if (metaDataBlock.getDataSize() > 0) {
            var inodes = readInodes(metaDataBlock);
            for (var inode : inodes) {
                if (inode.isDir()) {
                    deleteDir(pathToDir.join(inode.getName()), recursive);
                } else {
                    var fileBlock = getChildBlock(metaDataBlock, inode.getName());
                    var fileMetadataBlock = readMetaDataBlock(fileBlock);
                    deleteDirOrFile(fileMetadataBlock, fileBlock);
                }
            }
        }

        var parentInodes = readInodes(parentMetaDataBlock);
        parentInodes.removeIf(inode -> inode.getName().equals(pathToDir.getName()));
        var inodesAsBytes = ByteArrayTransformer.fromInodesList(parentInodes);
        writeDir(parentMetaDataBlock, parentPosition, inodesAsBytes);

        deleteDirOrFile(metaDataBlock, block);
    }

    public void delete(String path) throws IOException {
        var pathToFile = new Path(path);

        if (pathToFile.size() == 0) {
            throw new IllegalArgumentException("Cannot delete root directory");
        }

        var parentPath = pathToFile.getParentPath();
        var grandParentPath = parentPath.getParentPath();
        var parentPosition = 0;
        MetaDataBlock parentMetaDataBlock;

        if (grandParentPath != null) {
            parentMetaDataBlock = traverseTo(grandParentPath);
            parentPosition = getChildBlock(parentMetaDataBlock, parentPath.getName());
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        } else {
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        }

        var block = getChildBlock(parentMetaDataBlock, pathToFile.getName());
        var metaDataBlock = readMetaDataBlock(block);

        if (metaDataBlock.isDir()) {
            throw new IOException(String.format("%s is not a file", path));
        }

        var parentInodes = readInodes(parentMetaDataBlock);
        parentInodes.removeIf(inode -> inode.getName().equals(pathToFile.getName()));
        var inodesAsBytes = ByteArrayTransformer.fromInodesList(parentInodes);
        writeDir(parentMetaDataBlock, parentPosition, inodesAsBytes);

        deleteDirOrFile(metaDataBlock, block);
    }

    public void rename(String path, String newName) throws IOException {
        var pathToFileOrDir = new Path(path);

        if (pathToFileOrDir.size() == 0) {
            throw new IllegalArgumentException("Cannot rename root directory");
        }

        var parentPath = pathToFileOrDir.getParentPath();
        var grandParentPath = parentPath.getParentPath();
        var parentPosition = 0;
        MetaDataBlock parentMetaDataBlock;

        if (grandParentPath != null) {
            parentMetaDataBlock = traverseTo(grandParentPath);
            parentPosition = getChildBlock(parentMetaDataBlock, parentPath.getName());
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        } else {
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        }

        var block = getChildBlock(parentMetaDataBlock, pathToFileOrDir.getName());
        var inodes = readInodes(parentMetaDataBlock);
        inodes.stream()
            .filter(inode -> inode.getName().equals(pathToFileOrDir.getName()))
            .findFirst()
            .ifPresent(inode -> inode.setName(newName));

        var inodesAsBytes = ByteArrayTransformer.fromInodesList(inodes);
        writeDir(parentMetaDataBlock, parentPosition, inodesAsBytes);

        //TODO: this needs transactional handling too
        var metaDataBlock = readMetaDataBlock(block);
        metaDataBlock.setName(ByteArrayTransformer.fromString(newName));
        writeBlock(metaDataBlock, block);
    }

    public void move(String from, String to) throws IOException {
        var pathToDir = new Path(from);
        var parentPath = pathToDir.getParentPath();
        var grandParentPath = parentPath.getParentPath();
        var parentPosition = 0;
        MetaDataBlock parentMetaDataBlock;

        if (grandParentPath != null) {
            parentMetaDataBlock = traverseTo(grandParentPath);
            parentPosition = getChildBlock(parentMetaDataBlock, parentPath.getName());
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        } else {
            parentMetaDataBlock = readMetaDataBlock(parentPosition);
        }

        var dstPath = new Path(to);
        var dstParentPath = dstPath.getParentPath();
        var dstPosition = 0;
        if (dstParentPath != null) {
            var dstParentMetaData = traverseTo(dstPath.getParentPath());
            dstPosition = getChildBlock(dstParentMetaData, dstPath.getName());
        }
        var dstMetaDataBlock = readMetaDataBlock(dstPosition);

        if (!dstMetaDataBlock.isDir()) {
            throw new IOException(String.format("%s is not a dir", dstPath));
        }

        var srcInodes = readInodes(parentMetaDataBlock);
        var inodeToMove = srcInodes.stream()
            .filter(inode -> inode.getName().equals(pathToDir.getName()))
            .findAny()
            .orElse(null);

        var dstInodes = readInodes(dstMetaDataBlock);
        dstInodes.add(inodeToMove);
        writeDir(dstMetaDataBlock, dstPosition, ByteArrayTransformer.fromInodesList(dstInodes));

        srcInodes.removeIf(inode -> inode.getName().equals(pathToDir.getName()));
        writeDir(parentMetaDataBlock, parentPosition, ByteArrayTransformer.fromInodesList(srcInodes));
    }
}
