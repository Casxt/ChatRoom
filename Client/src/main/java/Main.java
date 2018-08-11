import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        //在static中不方便调用callback，故建一个新的类运行main函数。
        Client c = new Client(new InetSocketAddress("127.0.0.1", 12345));
        c.Start();
    }
}
