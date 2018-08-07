package Request;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Request.Request will format a socket data into request
 */
public class Request {
    public AsynchronousSocketChannel ch = null;
    private LinkedBlockingQueue<Request> ReqQueue;
    private Reader reader = new Reader(this);
    private Writer writer = new Writer(this);

    public Request(LinkedBlockingQueue<Request> ReqQueue) {
        this.ReqQueue = ReqQueue;
    }

    public void Response(JSONObject res) {
        writer.WriteOnce(res.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * When reader read complete it will call this function
     *
     * @param data is the reader return back
     */
    void DataReadComplete(byte[] data) {
        String s = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        JSONObject body = new JSONObject(s);
        // offer is a none block method,
        // put is a block method
        ReqQueue.offer(this);
        reader.Reset();
    }

    /**
     * call GetReq Will Start to collect data until complete
     *
     * @param ch are socket bound to this
     */
    public void GetReq(AsynchronousSocketChannel ch) {
        // Set Socket
        this.ch = ch;

        // data should be sanded in 20s after connection accepted
        this.ch.read(reader.Buff, 20, TimeUnit.SECONDS, this.ch, reader);
    }

    /**
     * if KeepOpen is true, the socket will not close after response
     * can use to reuse connection
     */
    public void KeepOpen(boolean keepOpen) {
        writer.keepOpen = keepOpen;
    }

    /**
     * Close the channel
     */
    void Close() {
        try {
            ch.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
