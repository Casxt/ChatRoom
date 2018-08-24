package Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * Request.Request will format a socket data into request
 */
public class Request {
    AsynchronousSocketChannel ch = null;

    //callback 由初始化函数初始化
    private RequestCallback callback;
    private Reader reader = new Reader(this);
    private Writer writer = new Writer(this);
    private String waitAction = null;
    private boolean blocking = false;
    private JSONObject tempRes = null;

    public Request(RequestCallback cb) {
        callback = cb;
        callback.onCreate(this);
    }

    /**
     * Send 用于服务器返回消息
     *
     * @param res 返回相应
     */
    public void Send(JSONObject res) {
        writer.Write(res.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Send 用于向服务器请求，可以阻塞
     * 如果是阻塞请求，通过tempRes方式将结果返回
     *
     * @param block   阻塞/非阻塞模式
     * @param timeout the maximum time to wait in milliseconds.
     * @return JSONObject
     * @throws InterruptedException 未测试
     */
    public JSONObject Send(JSONObject req, boolean block, long timeout) throws InterruptedException {
        assert !(blocking && block) : "不能在阻塞调用期间再次发起阻塞调用";
        writer.Write(req.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (block) {
            this.blocking = true;

            waitAction = req.getString("Action");
            //此处阻塞，等待结果返回
            synchronized (this) {
                this.wait(timeout);
            }
            //清空tempRes，防止数据无法释放
            JSONObject temp = tempRes;
            tempRes = null;
            return temp;
        }
        return null;
    }

    /**
     * When reader read complete it will call this function
     * 当读取出一条消息时，会调用这个函数，将数据作为参数传入
     * @param data is the reader return back
     */
    void DataReadComplete(byte[] data) {
        String s = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        JSONObject json;
        try {
            json = new JSONObject(s);
        } catch (JSONException e) {
            return;
        }

        //判断是否是等待的结果
        if (blocking && json.getString("Action").equals(waitAction)) {
            tempRes = json;
            synchronized (this) {
                this.notify();
            }
        }
        //依旧会发起onRequest 事件
        callback.onRequest(this, json);
    }

    /**
     * Bundle Will Start to collect data until complete
     * 将Request实例与一个channel绑定
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
     * 关闭 channel 并触发回调
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
