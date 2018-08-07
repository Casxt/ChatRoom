import Request.Request;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


public class Acceptor implements CompletionHandler<AsynchronousSocketChannel, Request> {
    //Logger
    private static Logger log = Logger.getLogger(Acceptor.class.getName());

    private LinkedBlockingQueue<Request> ReqQueue;
    private AsynchronousServerSocketChannel Server;

    Acceptor(AsynchronousServerSocketChannel Server, LinkedBlockingQueue<Request> ReqQueue) {
        this.ReqQueue = ReqQueue;
        this.Server = Server;
    }

    @Override
    public void completed(AsynchronousSocketChannel ch, Request req) {
        //Deal with this conn
        req.GetReq(ch);
        //After accept a conn, we need to reset acceptor
        Server.accept(new Request(ReqQueue), this);
    }

    @Override
    public void failed(Throwable e, Request req) {
        if (e instanceof java.nio.channels.ClosedChannelException) {
            log.severe("Server Stop!");
            e.getStackTrace();
        } else {
            log.severe(e.toString());
            Server.accept(new Request(ReqQueue), this);
        }
    }

}