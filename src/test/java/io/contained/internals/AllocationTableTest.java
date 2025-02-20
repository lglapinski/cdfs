package io.contained.internals;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class AllocationTableTest {
    @Test
    public void testFullBlockSpaceAvailability() {
        var allocationTable = new AllocationTable(8);
        var availableBlocks = allocationTable.getAvailableBlocks(8);
        assertThat(availableBlocks).containsExactly(0, 1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void testLowSpaceAllocationError() {
        var allocationTable = new AllocationTable(8);
        allocationTable.allocateBlocks(List.of(0));

        assertThatThrownBy(() -> allocationTable.getAvailableBlocks(8))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not enough free space to allocate blocks");
    }

    @Test
    public void testBlockDeAllocation() {
        var allocationTable = new AllocationTable(8);
        allocationTable.allocateBlocks(List.of(0, 1, 2));

        var availableBlocks = allocationTable.getAvailableBlocks(1);
        assertThat(availableBlocks).containsExactly(3);

        allocationTable.freeBlocks(List.of(0, 1, 2));

        availableBlocks = allocationTable.getAvailableBlocks(1);
        assertThat(availableBlocks).containsExactly(0);
    }

    @Test
    public void testTableSerialization() {
        var allocationTable = new AllocationTable(8);
        allocationTable.allocateBlocks(List.of(0, 1, 2));

        var bytes = allocationTable.toByteArray();
        assertThat(bytes).hasSize(1).containsExactly(7);
    }

    @Test
    public void testTableDeSerialization() {
        var allocationTable = AllocationTable.fromByteArray(new byte[] {7});
        assertThat(allocationTable.size()).isEqualTo(1);
        var availableBlocks = allocationTable.getAvailableBlocks(1);
        assertThat(availableBlocks).containsExactly(3);
    }
}
