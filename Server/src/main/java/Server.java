import Request.Request;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class Server {
    private static ConcurrentHashMap<String, Request> Clients;
    private static AsynchronousServerSocketChannel server;
    private static AsynchronousChannelGroup group;
    private static Acceptor acceptor;
    private static ChatServer chatServer;

    public static void main(String[] args) throws IOException {
        System.out.println("Server starting...");
        Server.group = AsynchronousChannelGroup.withFixedThreadPool(2, Executors.defaultThreadFactory());
        Server.Clients = new ConcurrentHashMap<>();
        chatServer = new ChatServer();

        server = AsynchronousServerSocketChannel.open(group);
        server.bind(new InetSocketAddress("0.0.0.0", 12345));
        acceptor = new Acceptor(server, chatServer);
        server.accept(new Request(new ClientEvent(chatServer)), acceptor);
    }
}
