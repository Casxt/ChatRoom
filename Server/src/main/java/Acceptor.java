import Request.Request;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

public class Acceptor implements CompletionHandler<AsynchronousSocketChannel, Request> {
    //Logger
    private static Logger log = Logger.getLogger(Acceptor.class.getName());

    private ChatServer chatServer;
    private AsynchronousServerSocketChannel Server;

    Acceptor(AsynchronousServerSocketChannel Server, ChatServer chatServer) {
        this.Server = Server;
        this.chatServer = chatServer;
    }

    @Override
    public void completed(AsynchronousSocketChannel ch, Request req) {
        //Deal with this conn
        req.Bundle(ch);
        //After accept a conn, we need to reset acceptor
        Server.accept(new Request(new ClientEvent(chatServer)), this);
    }

    @Override
    public void failed(Throwable e, Request req) {
        if (e instanceof java.nio.channels.ClosedChannelException) {
            log.severe("Server Stop!");
            e.getStackTrace();
        } else {
            log.severe(e.toString());
            Server.accept(new Request(new ClientEvent(chatServer)), this);
        }
    }

}