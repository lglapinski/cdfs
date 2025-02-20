package io.contained.internals.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ByteArrayTransformerTest {
    @Test
    public void testFromIntTransformation() {
        var bytes = new byte[Integer.BYTES];
        ByteArrayTransformer.fromInt(1, bytes, 0);
        assertThat(bytes).containsExactly(0, 0, 0, 1);

        ByteArrayTransformer.fromInt(256, bytes, 0);
        assertThat(bytes).containsExactly(0, 0, 1, 0);

        ByteArrayTransformer.fromInt(65536, bytes, 0);
        assertThat(bytes).containsExactly(0, 1, 0, 0);

        ByteArrayTransformer.fromInt(16777216, bytes, 0);
        assertThat(bytes).containsExactly(1, 0, 0, 0);
    }

    @Test
    public void testToIntTransformation() {
        var bytes = new byte[]{0, 0, 0, 1};
        var transformed = ByteArrayTransformer.toInt(bytes, 0);
        assertThat(transformed).isEqualTo(1);

        bytes[3] = 0;
        bytes[2] = 1;
        transformed = ByteArrayTransformer.toInt(bytes, 0);
        assertThat(transformed).isEqualTo(256);

        bytes[2] = 0;
        bytes[1] = 1;
        transformed = ByteArrayTransformer.toInt(bytes, 0);
        assertThat(transformed).isEqualTo(65536);

        bytes[1] = 0;
        bytes[0] = 1;
        transformed = ByteArrayTransformer.toInt(bytes, 0);
        assertThat(transformed).isEqualTo(16777216);
    }

    @Test
    public void testFromStringTransformation() {
        var str = "abcd";
        var bytes = ByteArrayTransformer.fromString(str);
        assertThat(bytes).containsExactly(97, 98, 99, 100);
    }

    @Test
    public void testToStringTransformation() {
        var bytes = new byte[]{97, 98, 99, 100};
        var str = ByteArrayTransformer.toString(bytes);
        assertThat(str).isEqualTo("abcd");
    }
}
