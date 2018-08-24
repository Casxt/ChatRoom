package PackTool;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackToolTest {
    @Test
    void testAll() {
        PackTool packTool = new PackTool(new byte[]{'t', 'e', 's', 't'});

        byte[] rawMsg = "TestMsg".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buff = packTool.Construct(rawMsg);
        assertTrue(Arrays.equals(rawMsg, packTool.Deconstruct(buff)));

        PackTool packTool2 = new PackTool(new byte[]{'a', 'b'});

        rawMsg = "TestMsg".getBytes(StandardCharsets.UTF_8);
        buff = packTool2.Construct(rawMsg);
        assertNull(packTool.Deconstruct(buff));
    }
}