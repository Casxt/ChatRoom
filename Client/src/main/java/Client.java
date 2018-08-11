import Request.Request;
import Request.RequestCallback;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class Client implements RequestCallback {
    private SocketAddress address;
    private AsynchronousSocketChannel serverCh = null;
    private String SessionID = null;
    private String UserName = null;
    private Request request = new Request(this);
    private Scanner scanner = new Scanner(System.in);

    public Client(SocketAddress address) {
        this.address = address;

    }

    public void Start() {
        String cmd = "";
        do {

            try {
                serverCh = AsynchronousSocketChannel.open();
                serverCh.connect(address).get();
            } catch (IOException | InterruptedException | ExecutionException e) {
                System.out.println("Connect Failed...");
                e.printStackTrace();
            }
            request.Bundle(serverCh);
            System.out.print("Please login");

            while (true) {
                cmd = scanner.nextLine();
                if (cmd.startsWith("/login ")) {
                    SignIn(cmd.substring("/login ".length()));
                } else if (UserName == null) {
                    //这是建立连接后的第一个命令，如果输入任何其他的，都要报错：Invalid command。
                    System.out.println("Invalid Command");
                } else {
                    System.out.println("Invalid Command");
                }

            }
        } while (true);
    }


    /**
     * SignIn
     *
     * @param UserName
     */
    private void SignIn(String UserName) {
        if (!UserName.matches("^\\S+$")) {
            System.out.println("Invalid UserName");
        }
        JSONObject reqJSON = new JSONObject();
        reqJSON.put("Action", "SignIn")
                .put("Name", UserName);
        JSONObject res = null;
        try {
            res = request.Request(reqJSON, true, 10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
        if (res.getString("State").equals("Success")) {
            this.UserName = UserName;
            this.SessionID = res.getString("SessionID");
        } else {
            System.out.println(res.getString("Msg"));
        }

    }

    @Override
    public void onCreate(Request req) {

    }

    @Override
    public void onRequest(Request req, JSONObject json) {

    }

    @Override
    public void onReqClose(Request req) {

    }
}
