import Request.Request;
import Request.RequestCallback;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class Client implements RequestCallback {
    private SocketAddress address;
    private AsynchronousSocketChannel serverCh = null;
    private String SessionID = null;
    private String UserName = null;
    private Request request = new Request(this);
    private Scanner scanner = new Scanner(System.in);
    private boolean endProgramFlag = false;
    //考虑到随机访问的需求，使用ArrayList
    private List<String> history = new ArrayList<>(200);

    Client(SocketAddress address) {
        this.address = address;
    }

    private static String _FUNC_() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        return traceElement.getMethodName();
    }


    /**
     * SignIn
     *
     * @param UserName to sign in
     */
    private void SignIn(String UserName) {
        if (!UserName.matches("^\\S+$")) {
            System.out.println("Invalid UserName");
        }
        JSONObject reqJSON = new JSONObject();
        reqJSON.put("Action", "SignIn")
                .put("Name", UserName);
        JSONObject res;
        try {
            res = request.Send(reqJSON, true, 10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        if (res == null) {
            System.out.println(_FUNC_() + " request error");
            return;
        }

        if (res.getString("State").equals("Success")) {
            this.UserName = UserName;
            this.SessionID = res.getString("SessionID");
            System.out.println(res.getString("Msg"));
        } else {
            System.out.println(res.getString("Msg"));
        }

    }

    /**
     * SignOut 退出
     */
    private void SignOut() {
        if (UserName == null) {
            endProgramFlag = true;
            return;
        }

        JSONObject reqJSON = new JSONObject(), res;
        reqJSON.put("Action", "SignOut")
                .put("Name", UserName)
                .put("SessionID", SessionID);
        try {
            res = request.Send(reqJSON, true, 10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        if (res == null) {
            System.out.println(_FUNC_() + " request error");
            return;
        }

        if (res.getString("State").equals("Success")) {
            endProgramFlag = true;
            System.out.println(res.getString("Msg"));
        } else {
            System.out.println(res.getString("Msg"));
        }
    }

    /**
     * SendTo 向指定用户定向发送消息
     *
     * @param Name 指定用户的用户名
     * @param Msg  消息
     * @return 是否发送成功
     */
    private boolean SendTo(String Name, String Msg) {
        JSONObject reqJSON = new JSONObject(), res;
        reqJSON.put("Action", "SendMsg")
                .put("Name", UserName)
                .put("SessionID", SessionID)
                .put("To", Name)
                .put("Message", Msg);
        try {
            res = request.Send(reqJSON, true, 10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        if (res == null) {
            System.out.println(_FUNC_() + " request error");
            return false;
        }

        if (res.getString("State").equals("Success")) {
            return true;
        }
        System.out.println(res.getString("Msg"));
        return false;
    }

    /**
     * BroadCast 向指定用户之外的用户广播消息
     *
     * @param Except 要去除的用户
     * @param Msg    消息
     * @return 是否成功
     */
    private boolean BroadCast(String[] Except, String Msg) {
        JSONObject reqJSON = new JSONObject(), res;
        reqJSON.put("Action", "BroadCastMsg")
                .put("Name", UserName)
                .put("SessionID", SessionID)
                .put("Message", Msg);
        if (Except != null) {
            JSONArray exceptArray = new JSONArray();
            for (String e : Except) {
                exceptArray.put(e);
            }
            reqJSON.put("Except", exceptArray);
        }
        try {
            res = request.Send(reqJSON, true, 10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        if (res == null) {
            System.out.println(_FUNC_() + " request error");
            return false;
        }

        if (res.getString("State").equals("Success")) {
            return true;
        }
        System.out.println(res.getString("Msg"));
        return false;
    }

    /**
     * ListUsers 列出已登陆用户
     */
    private void ListUsers() {
        JSONObject reqJSON = new JSONObject(), res;
        reqJSON.put("Action", "ListUsers")
                .put("Name", UserName)
                .put("SessionID", SessionID);
        try {
            res = request.Send(reqJSON, true, 10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        if (res == null) {
            System.out.println(_FUNC_() + " request error");
            return;
        }

        if (res.getString("State").equals("Success")) {
            System.out.println(res.getString("Msg"));
        } else {
            System.out.println(res.getString("Msg"));
        }
    }

    void Start() {
        String cmd;
        try {
            serverCh = AsynchronousSocketChannel.open();
            serverCh.connect(address).get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Connect Failed...");
            e.printStackTrace();
        }
        request.Bundle(serverCh);
        System.out.println("Please login");

        //考虑到命令格式都比较简单，故不使用正则匹配，另一方面也更加高效
        while (!endProgramFlag) {
            cmd = scanner.nextLine();
            if (UserName == null && cmd.startsWith("/login ")) {
                //登陆
                SignIn(cmd.substring("/login ".length()));
            } else if (cmd.startsWith("/quit")) {
                //退出
                SignOut();
            } else if (UserName == null) {
                //建立连接后的第一个命令，如果输入任何其他的除了login/quit，都要报错：Invalid command。
                System.out.println("Invalid Command");
            } else if (cmd.startsWith("/who")) {
                ListUsers();
            } else if (cmd.startsWith("/history")) {
                String[] arg = cmd.split(" ", 3);
                int len = history.size();
                switch (arg.length) {
                    case 1:
                        for (int i = len > 50 ? len - 50 : 0; i < len; i++) {
                            System.out.print(String.format("第%s条--", i + 1));
                            System.out.println(history.get(i));
                        }
                        break;
                    case 3:
                        int s, e;
                        try {
                            s = Integer.parseInt(arg[1]);
                            e = Integer.parseInt(arg[2]);
                        } catch (NumberFormatException E) {
                            System.out.println("Invalid Number");
                            break;
                        }
                        if (s < 0 || e > len || !(s <= e)) {
                            System.out.println("Number out of range");
                            break;
                        }
                        for (int i = s - 1; i < e; i++) {
                            System.out.print(String.format("第%s条--", i + 1));
                            System.out.println(history.get(i));
                        }
                        break;
                    default:
                        System.out.println("Invalid Command");
                        break;
                }
            } else if (cmd.startsWith("/to ")) {
                //定向发送
                String[] arg = cmd.split(" ", 3);
                if (arg.length < 3) {
                    System.out.println("Invalid Command, should be /to user_name");
                    return;
                }
                //送给对方
                if (SendTo(arg[1], String.format("%s对你说：%s", UserName, arg[2]))) {
                    //显示给自己
                    String t = String.format("你对%s说：%s", arg[1], arg[2]);
                    System.out.println(t);
                    history.add(t);
                }
            } else if (cmd.equals("//hi") || cmd.startsWith("//hi ")) {
                //预设消息:hi
                String[] arg = cmd.split(" ", 2);
                switch (arg.length) {
                    case 1:
                        if (BroadCast(new String[]{UserName}, UserName + "向大家打招呼，“Hi，大家好！我来咯~”。")) {
                            System.out.println("你向大家打招呼，“Hi，大家好！我来咯~”。");
                            history.add("你向大家打招呼，“Hi，大家好！我来咯~”。");
                        }

                        break;
                    case 2:
                        if (SendTo(arg[1], String.format("%s向你打招呼：“Hi，你好啊~”。", UserName))) {
                            if (BroadCast(new String[]{UserName, arg[1]}, String.format("%s向%s打招呼：“Hi，你好啊~”", UserName, arg[1]))) {
                                String t = String.format("你向%s打招呼：“Hi，你好啊~”。", arg[1]);
                                System.out.println(t);
                                history.add(t);
                            }
                        }
                        break;
                }
            } else if (!cmd.startsWith("/")) {
                //广播消息
                if (BroadCast(new String[]{UserName}, String.format("%s说：%s", UserName, cmd))) {
                    //显示给自己
                    String t = "你说：" + cmd;
                    System.out.println(t);
                    history.add(t);
                }
            } else {
                System.out.println("Invalid Command");
            }
        }

    }

    @Override
    public void onCreate(Request req) {

    }

    /**
     * onRequest 充当router
     *
     * @param req  req
     * @param json json
     */
    @Override
    public void onRequest(Request req, JSONObject json) {
        if (!json.getString("State").equals("Success")) {
            return;
        }
        String m = json.getString("Msg");
        switch (json.getString("Action")) {
            case "Message":
                System.out.println(m);
                history.add(m);
                break;
            case "BroadCast":
                System.out.println(m);
                history.add(m);
                break;
        }

    }

    @Override
    public void onReqClose(Request req) {

    }
}
