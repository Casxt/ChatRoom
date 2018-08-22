import Request.Request;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.Executors;

public class Server {
    private static AsynchronousChannelGroup group;

    public static void main(String[] args) throws IOException {
        System.out.println("Server starting...");
        Server.group = AsynchronousChannelGroup.withFixedThreadPool(2, Executors.defaultThreadFactory());

        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(group);
        server.bind(new InetSocketAddress("0.0.0.0", 12345));

        //ChatServer 是一个全局实例，所有req共享同一个ChatServer
        ChatServer chatServer = new ChatServer();

        Acceptor acceptor = new Acceptor(server, chatServer);
        //进行第一次接受，之后的流程由acceptor接管
        server.accept(new Request(chatServer), acceptor);
    }
}
