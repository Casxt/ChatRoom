import Request.Request;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private static LinkedBlockingQueue<Request> ReqQueue;
    private static ConcurrentHashMap<String, Request> Clients;
    private static AsynchronousServerSocketChannel server;
    private static AsynchronousChannelGroup group;
    private static Acceptor acceptor;

    public static void main(String[] args) throws IOException {
        System.out.println("Server starting...");
        Server.group = AsynchronousChannelGroup.withFixedThreadPool(2, Executors.defaultThreadFactory());
        Server.ReqQueue = new LinkedBlockingQueue<>();
        Server.Clients = new ConcurrentHashMap<>();

        server = AsynchronousServerSocketChannel.open(group);
        server.bind(new InetSocketAddress("0.0.0.0", 12345));
        acceptor = new Acceptor(server, ReqQueue);
        server.accept(new Request(ReqQueue), acceptor);
    }
}
