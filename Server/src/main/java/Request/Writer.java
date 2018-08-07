package Request;

import PackTool.PackTool;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

public class Writer implements CompletionHandler<Integer, ByteBuffer> {
    /**
     * If KeepOpen is true, writer will not close the connection,
     * is was useful when there was a long connection,
     * or this request is for client.
     * The default value is false
     */
    boolean keepOpen = false;
    private int sendTimes = 0;
    private Request req;
    private PackTool packer = new PackTool(new byte[]{'C', 'h', 'a', 't'});


    Writer(Request req) {
        this.req = req;
        //buffer = ByteBuffer.allocate(2048);
        //packer = new PackTool(new byte[]{'G', 'r', 'a', 'm', 'b', 'l', 'e'});
    }

    /*
     * Write Data into buffer, can be call more than once.
     * this method work with Send(),
     * And the Write-Send can not use with WriteOnce
     *
     * @param data waite to be send

    public void Write(byte[] data) {
        buffer.put(data);
    }
     */
    /*
     * Send the data in buffer which written by Write()

    public void Send() {
        buffer.flip();
        packer.Construct(buffer);
        req.ch.write(buffer, buffer, this);
    }
     */

    /**
     * WriteOnce the Data to buffer and send and close connection
     * Only send the data that given, the data write by the Write() will not be send.
     * faster than Write-Send
     *
     * @param data is the only data need to send
     */
    void WriteOnce(byte[] data) {
        ByteBuffer buffer = packer.Construct(data);
        req.ch.write(buffer, 10, TimeUnit.SECONDS, buffer, this);
    }

    @Override
    public void completed(Integer result, ByteBuffer buffer) {
        sendTimes++;
        if (result != -1) {
            if (buffer.hasRemaining()) {
                if (sendTimes < 4) {
                    req.ch.write(buffer, 10, TimeUnit.SECONDS, buffer, this);
                } else {// if Send too many times
                    req.Close();
                }
            } else {
                //如果设置了KeepOn标记, 在一次传输完成后将不会关闭请求
                if (!keepOpen) {
                    req.Close();
                }
            }
        } else {
            req.Close();
        }

    }

    @Override
    public void failed(Throwable e, ByteBuffer buffer) {
        if (e instanceof java.nio.channels.InterruptedByTimeoutException) {
            req.Close();
        } else {
            req.Close();
            e.printStackTrace();
        }
    }
}
