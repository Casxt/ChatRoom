package Request;

import PackTool.PackTool;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Reader implements CompletionHandler<Integer, AsynchronousSocketChannel> {
    //Logger
    private static Logger log = Logger.getLogger(Reader.class.getName());

    ByteBuffer Buff = ByteBuffer.allocate(2048);
    private Request req;
    private PackTool parser = new PackTool(new byte[]{'C', 'h', 'a', 't'});
    private int readTimes = 0;

    Reader(Request req) {
        this.req = req;
    }

    @Override
    public void completed(Integer result, AsynchronousSocketChannel ch) {
        readTimes++;
        if (result != -1) {
            Buff.flip();
            byte[] data = parser.Deconstruct(Buff);
            Buff.compact();
            if (data != null) {

                req.DataReadComplete(data);
                //准备接收下一次请求,并且不会超时
                ch.read(Buff, ch, this);
                Reset();
            } else {// if data incomplete, read more
                if (readTimes < 4) {//if read too many times
                    ch.read(Buff, 10, TimeUnit.SECONDS, ch, this);
                } else {
                    req.Close();
                }
            }

        } else {
            req.Close();
        }
    }


    @Override
    public void failed(Throwable e, AsynchronousSocketChannel ch) {
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

    /**
     * Reset buffer and read counter
     */
    private void Reset() {
        Buff.clear();
        readTimes = 0;
    }
}
