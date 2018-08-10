package Request;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * Request.Request will format a socket data into request
 */
public class Request {
    public AsynchronousSocketChannel ch = null;
    //private LinkedBlockingQueue<Request> ReqQueue;
    private Reader reader = new Reader(this);
    private Writer writer = new Writer(this);
    public RequestCallback callback = null;

    public Request(RequestCallback cb) {
        callback = cb;
        callback.onCreate(this);
    }

    public void Response(JSONObject res) {
        writer.Write(res.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * When reader read complete it will call this function
     *
     * @param data is the reader return back
     */
    void DataReadComplete(byte[] data) {
        String s = new String(data, java.nio.charset.StandardCharsets.UTF_8);

        callback.onRequest(this, new JSONObject(s));
    }

    /**
     * Bundle Will Start to collect data until complete
     *
     * @param ch are socket bound to this
     */
    public void Bundle(AsynchronousSocketChannel ch) {
        // Set Socket
        this.ch = ch;

        // data should be sanded in 20s after connection accepted
        this.ch.read(reader.Buff, 20, TimeUnit.SECONDS, this.ch, reader);
    }

    /**
     * Close the channel and trigger callback
     */
    void Close() {

        callback.onReqClose(this);

        try {
            ch.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
