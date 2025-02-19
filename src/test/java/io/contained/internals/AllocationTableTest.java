package io.contained.internals;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class AllocationTableTest {
    @Test
    public void testFullBlockSpaceAvailability() {
        var allocationTable = new AllocationTable(5);
        var availableBlocks = allocationTable.getAvailableBlocks(5);
        assertThat(availableBlocks).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    public void testLowSpaceAllocationError() {
        var allocationTable = new AllocationTable(5);
        allocationTable.allocateBlocks(List.of(0));

        assertThatThrownBy(() -> allocationTable.getAvailableBlocks(5)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not enough free space to allocate blocks");
    }

    @Test
    public void testBlockDeAllocation() {
        var allocationTable = new AllocationTable(5);
        allocationTable.allocateBlocks(List.of(0, 1, 2));

        var availableBlocks = allocationTable.getAvailableBlocks(1);
        assertThat(availableBlocks).containsExactly(3);

        allocationTable.freeBlocks(List.of(0, 1, 2));

        availableBlocks = allocationTable.getAvailableBlocks(1);
        assertThat(availableBlocks).containsExactly(0);
    }
}
