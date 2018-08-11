package Request;

import PackTool.PackTool;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Writer implements CompletionHandler<Integer, ByteBuffer> {
    //Logger
    private static Logger log = Logger.getLogger(Writer.class.getName());

    LinkedBlockingQueue<ByteBuffer> buffers;
    private int sendTimes = 0;
    private Request req;
    private PackTool packer = new PackTool(new byte[]{'C', 'h', 'a', 't'});
    /**
     * If KeepOpen is true, writer will not close the connection,
     * is was useful when there was a long connection,
     * or this request is for client.
     * The default value is false
     */
    private boolean isSending = false;

    Writer(Request req) {
        this.req = req;
        buffers = new LinkedBlockingQueue<>();
    }

    /**
     * Write the Data to buffer and send
     *
     * @param data is the only data need to send
     */
    void Write(byte[] data) {
        ByteBuffer buffer = packer.Construct(data);
        if (isSending) {
            buffers.offer(buffer);
        } else {
            req.ch.write(buffer, 10, TimeUnit.SECONDS, buffer, this);
        }
    }

    @Override
    public void completed(Integer result, ByteBuffer buffer) {
        sendTimes++;
        if (result != -1) {
            // 如果当前buffer没有发送完则继续发送
            if (buffer.hasRemaining()) {
                if (sendTimes < 4) {
                    req.ch.write(buffer, 10, TimeUnit.SECONDS, buffer, this);
                } else {// if Send too many times
                    req.Close();
                }
            } else {
                // 取出下一条buffer发送
                if (!buffers.isEmpty()) {
                    isSending = true;
                    sendTimes = 0;
                    ByteBuffer buf = buffers.poll();
                    //poll is a nonblocking method
                    req.ch.write(buf, 10, TimeUnit.SECONDS, buf, this);
                } else {
                    isSending = false;
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
        } else if (e instanceof java.io.IOException) {
            log.warning("Connection lost");
            req.Close();
        } else {
            req.Close();
            e.printStackTrace();
        }
    }
}
