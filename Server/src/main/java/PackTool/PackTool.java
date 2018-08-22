package PackTool;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class PackTool {
    private byte[] ringBuf;
    private byte[] head;
    private int count = 0;
    private int len;

    public PackTool(byte[] head) {
        len = head.length;
        this.head = head;
        ringBuf = new byte[len];
    }

    /**
     * Find the first Head in the buffer, if find return true
     * pos of buf will be set to first byte behind head,
     * so data can be get directly.
     *
     * @param buffer if the buffer wait to find the buffer
     * @return true if found.
     */
    private boolean MatchHead(ByteBuffer buffer) {
        //init the sum of count
        count = len - 1;
        //First fill the RingBuff
        // if there is no enough byte in buffer return false
        for (int i = 0; i < len; i++) {
            if (buffer.remaining() > 0) {
                ringBuf[i] = buffer.get();
            } else {
                return false;
            }
        }
        if (isMatch()) {
            return true;
        }

        // circle match part
        while (buffer.remaining() > 0) {
            count++;
            ringBuf[count % len] = buffer.get();
            if (isMatch()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deconstruct data in buff, data not used will rest in buff
     * only one msg will be Deconstruct with one call
     * it will test try to read out whole msg and check with crc
     * if data in buff is not enough, will return null
     *
     * @param buffer wait to Deconstruct
     * @return byte[] of data
     */
    public byte[] Deconstruct(ByteBuffer buffer) {
        if (MatchHead(buffer)) {

            //mark the position of first byte behind Head
            buffer.mark();

            //Gte Data Len
            int dataLen = buffer.getInt();

            //dataLen 的定义为不包含long int效验码的data部分的长度
            if (buffer.remaining() >= (dataLen + 8)) {

                byte[] data = new byte[dataLen];
                buffer.get(data);
                long crcVerifyCode = buffer.getLong();

                CRC32 crc32 = new CRC32();
                crc32.update(data);

                if (crcVerifyCode == crc32.getValue()) {
                    return data;
                }

                return null;

            } else {//Head has been found but data not receive completely

                //return back to first byte
                buffer.reset();

                //return back to pos of begin of head
                buffer.position(buffer.position() - head.length);
                return null;
            }

        } else {
            //没有匹配到包头
            //此时不会回溯任何数据
            return null;
        }
    }

    /**
     * Construct will Constructor data struct
     *
     * @param data is data wait to be pack
     * @return the buffer wait to be send
     */
    public ByteBuffer Construct(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);

        ByteBuffer b = ByteBuffer.allocate((head.length + 4 + data.length + 8));
        b.put(head);
        b.putInt(data.length);
        b.put(data);
        b.putLong(crc32.getValue());
        b.flip();
        return b;
    }

    /**
     * test data in ring buffer, if it match head, return true
     *
     * @return true if match
     */
    private boolean isMatch() {
        for (int i = 0; i < len; i++) {
            // the match will begin from the last byte and end with the first byte
            if (ringBuf[(count - i + len) % len] != head[len - 1 - i]) {
                return false;
            }
        }
        return true;
    }
}
