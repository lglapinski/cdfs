package io.contained.internals;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerDescriptorTest {

    @Test
    public void testDefaultValuesAssignmentAndSizeCalculation() {
        var bytesPerMB = 1024 * 1024;
        var descriptor = new ContainerDescriptor(1);
        assertThat(descriptor.getBlockSize()).isEqualTo(Configuration.blockSize);
        assertThat(descriptor.getSignature()).isEqualTo(Configuration.signature);
        assertThat(descriptor.getBlockCount()).isEqualTo(bytesPerMB / descriptor.getBlockSize());
    }

    //TODO: improve this?
    @Test
    public void testByteArrayConversion() {
        var descriptor = new ContainerDescriptor(1);
        var bytes = descriptor.toByteArray();
        assertThat(bytes).hasSize(ContainerDescriptor.BYTES);

        var retrievedDescriptor = ContainerDescriptor.fromByteArray(bytes);

        assertThat(retrievedDescriptor).usingRecursiveComparison().isEqualTo(descriptor);
    }
}
