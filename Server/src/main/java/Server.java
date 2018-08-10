import Request.Request;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class Server {
    private static ConcurrentHashMap<String, Request> Clients = null;
    private static AsynchronousChannelGroup group;

    public static void main(String[] args) throws IOException {
        System.out.println("Server starting...");
        Server.group = AsynchronousChannelGroup.withFixedThreadPool(2, Executors.defaultThreadFactory());
        ChatServer chatServer = new ChatServer();
        Server.Clients = ChatServer.Clients;

        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(group);
        server.bind(new InetSocketAddress("0.0.0.0", 12345));
        Acceptor acceptor = new Acceptor(server, chatServer);
        server.accept(new Request(chatServer), acceptor);
    }
}
