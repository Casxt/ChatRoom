import PackTool.PackTool;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerTest {

    @Test
    void main() throws IOException {
        Server server = new Server();

        Server.main(new String[]{});
        //建立连接后直接断开
        try {
            Socket socket = new Socket("localhost", 12345);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //发送不符合规范消息
        try {
            Socket socket = new Socket("localhost", 12345);
            socket.getOutputStream().write("TestErrorMsg".getBytes());
            assertEquals(0, socket.getInputStream().available());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //发送不符合规范的Action
        try {
            PackTool packTool = new PackTool(new byte[]{'C', 'h', 'a', 't'});
            JSONObject json = new JSONObject().put("Action", "Test");
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 12345));
            byte jsonByte[] = json.toString().getBytes(StandardCharsets.UTF_8);
            socketChannel.write(packTool.Construct(jsonByte));
            ByteBuffer buff = ByteBuffer.allocate(128);
            socketChannel.read(buff);
            buff.flip();
            JSONObject res = new JSONObject()
                    .put("Action", json.getString("Action"))
                    .put("State", "Failed")
                    .put("Msg", "Invalid Action");
            assertTrue(Arrays.equals(res.toString().getBytes(StandardCharsets.UTF_8), packTool.Deconstruct(buff)));
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //请求后断开
        try {
            PackTool packTool = new PackTool(new byte[]{'C', 'h', 'a', 't'});
            JSONObject json = new JSONObject().put("Action", "SignIn").put("Name", "TestUser");
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 12345));
            byte jsonByte[] = json.toString().getBytes(StandardCharsets.UTF_8);
            socketChannel.write(packTool.Construct(jsonByte));
            ByteBuffer buff = ByteBuffer.allocate(128);
            socketChannel.read(buff);
            buff.flip();
            assertTrue(buff.hasRemaining());
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //请求中断开
        try {
            PackTool packTool = new PackTool(new byte[]{'C', 'h', 'a', 't'});
            JSONObject json = new JSONObject().put("Action", "SignIn").put("Name", "TestUser");
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 12345));
            byte jsonByte[] = json.toString().getBytes(StandardCharsets.UTF_8);
            socketChannel.write(packTool.Construct(jsonByte));
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}