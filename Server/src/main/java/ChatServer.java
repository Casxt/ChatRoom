import Request.Request;
import Request.RequestCallback;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ChatServer implements RequestCallback {
    //Logger
    private static Logger log = Logger.getLogger(ChatServer.class.getName());

    static ConcurrentHashMap<String, Request> Clients = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> Sessions = new ConcurrentHashMap<>();

    ChatServer() {
    }

    private static void Route(Request req, JSONObject json) {
        System.out.println(json.toString());
        switch (json.getString("Action")) {
            case "SignIn":
                SignIn(req, json);
                break;
            case "SignOut":
                SignOut(req, json);
                break;
            case "SendMsg":
                SendMsg(req, json);
                break;
            case "BroadCastMsg":
                BroadCastMsg(req, json);
                break;
        }

    }

    /**
     * BroadCastMsg BroadCastMsg
     * req Struct{
     * Action BroadCastMsg
     * Name
     * SessionID
     * Except String[] 可选参数
     * Message
     * }
     * res {
     * Action BroadCastMsg
     * State
     * Msg
     * }
     * res2 {
     * Action BroadCast
     * State
     * From
     * Msg
     * }
     *
     * @param req     req
     * @param reqJSON reqJSON
     */
    private static void BroadCastMsg(Request req, JSONObject reqJSON) {
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "BroadCastMsg");

        if (!reqJSON.has("Name") || !reqJSON.has("SessionID") || !reqJSON.has("Message")) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Invalid Request Parameter");
            req.Response(resJSON);
            return;
        }

        String name = reqJSON.getString("Name");
        String sessionID = reqJSON.getString("SessionID");
        String message = reqJSON.getString("Message");
        String[] exceptNames = null;
        if (reqJSON.has("Except")) {
            JSONArray array = reqJSON.getJSONArray("Except");
            int l = array.length();
            exceptNames = new String[l];
            for (int i = 0; i < l; i++) {
                exceptNames[i] = array.getString(i);
            }
        }

        if (!Sessions.containsKey(name) || !Sessions.get(name).equals(sessionID)) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Invalid Request Parameter");
            req.Response(resJSON);
            return;
        }

        resJSON.put("State", "Success")
                .put("Msg", "Success");
        req.Response(resJSON);

        if (exceptNames == null) {
            BroadCast(message);
            return;
        }
        BroadCastExcept(exceptNames, message);
    }

    /**
     * SendMsg to specific user
     * req Struct{
     * Action SendMsg
     * Name
     * SessionID
     * To
     * Message
     * }
     * res {
     * Action SendMsg
     * State
     * Msg
     * }
     * res2 {
     * Action Message
     * State
     * From
     * Msg
     * }
     *
     * @param req     req
     * @param reqJSON reqJSON
     */
    private static void SendMsg(Request req, JSONObject reqJSON) {
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "SendMsg");

        if (!reqJSON.has("Name") || !reqJSON.has("SessionID") || !reqJSON.has("To") || !reqJSON.has("Message")) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Invalid Request Parameter");
            req.Response(resJSON);
            return;
        }

        String name = reqJSON.getString("Name");
        String sessionID = reqJSON.getString("SessionID");
        String to = reqJSON.getString("To");
        String message = reqJSON.getString("Message");

        if (!Sessions.containsKey(name) || !Sessions.get(name).equals(sessionID)) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Invalid Request Parameter");
            req.Response(resJSON);
            return;
        }

        if (!Clients.containsKey(to)) {
            resJSON.put("State", "Failed")
                    .put("Msg", to + " is not online.");
            req.Response(resJSON);
            return;
        }

        if (to.equals(name)) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Stop talking to yourself!");
            req.Response(resJSON);
            return;
        }

        //对比builder和format
        //https://stackoverflow.com/questions/44117788/performance-between-string-format-and-stringbuilder
        resJSON.put("State", "Success")
                .put("Msg", "Success");
        req.Response(resJSON);

        JSONObject sendJSON = new JSONObject();
        sendJSON.put("Action", "Message");
        sendJSON.put("State", "Success")
                .put("From", name)
                .put("Msg", message);
        Clients.get(to).Response(sendJSON);

    }

    /**
     * SignIn
     * req Struct{
     * Action SignIn
     * Name
     * }
     * res {
     * Action SignIn
     * State
     * Msg
     * SessionID
     * }
     *
     * @param req     req
     * @param reqJSON json
     */
    private static void SignIn(Request req, JSONObject reqJSON) {
        String name = reqJSON.getString("Name");
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "SignIn");

        if (!reqJSON.has("Name")) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Invalid Request Parameter");
            req.Response(resJSON);
            return;
        }

        if (Clients.containsKey(name)) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Name exist, please choose another name");
            req.Response(resJSON);
            return;
        }
        Clients.put(name, req);
        Sessions.put(name, UUID.randomUUID().toString());
        resJSON.put("State", "Success")
                .put("Msg", "You have logined")
                .put("SessionID", Sessions.get(name));
        req.Response(resJSON);
        BroadCastExcept(new String[]{name}, name + " has logined");
    }

    /**
     * SignOut
     * req Struct{
     * Action SignIn
     * Name
     * SessionID
     * }
     * res {
     * Action SignIn
     * State
     * Msg
     * }
     *
     * @param req     req
     * @param reqJSON reqJSON
     */
    private static void SignOut(Request req, JSONObject reqJSON) {
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "SignOut");

        if (!reqJSON.has("Name") || !reqJSON.has("SessionID")) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Invalid Request Parameter");
            req.Response(resJSON);
            return;
        }

        String name = reqJSON.getString("Name");
        String sessionID = reqJSON.getString("SessionID");

        if (!Sessions.containsKey(name) || !Sessions.get(name).equals(sessionID)) {
            resJSON.put("State", "Failed")
                    .put("Msg", "Invalid Request Parameter");
            req.Response(resJSON);
            return;
        }

        Sessions.remove(name);
        Clients.remove(name);

        resJSON.put("State", "Success")
                .put("Msg", "quit success");
        req.Response(resJSON);

        BroadCast(name + " has quit.");

    }

    /**
     * BroadCastExcept specific user
     * res {
     * Action BroadCast
     * State Success
     * From Server
     * Msg
     * }
     *
     * @param names specific user
     * @param Msg   BoardCast Msg
     */
    private static void BroadCastExcept(String[] names, String Msg) {
        assert names != null : "names can not be null";
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "BroadCast")
                .put("State", "Success")
                .put("From", "Server")
                .put("Msg", Msg);

        boolean except;
        for (ConcurrentHashMap.Entry<String, Request> entry : Clients.entrySet()) {
            except = false;
            for (String n : names) {
                if (entry.getKey().equals(n)) {
                    except = true;
                    break;
                }
            }
            if (!except) {
                entry.getValue().Response(resJSON);
            }
        }

    }

    /**
     * BroadCast
     * res {
     * Action BroadCast
     * State Success
     * From Server
     * Msg
     * }
     *
     * @param Msg msg to BroadCast
     */
    private static void BroadCast(String Msg) {
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "BroadCast")
                .put("State", "Success")
                .put("From", "Server")
                .put("Msg", Msg);
        for (ConcurrentHashMap.Entry<String, Request> entry : Clients.entrySet()) {
            entry.getValue().Response(resJSON);
        }

    }

    @Override
    public void onCreate(Request req) {

    }

    @Override
    public void onRequest(Request req, JSONObject json) {
        ChatServer.Route(req, json);
    }

    /**
     * 承担了意外退出的处理功能
     *
     * @param req req
     */
    @Override
    public void onReqClose(Request req) {
        String name = Clients.search(1, (k, v) -> v == req ? k : null);
        if (name == null) {
            log.info("UnSignIn User Quit");
            return;
        }
        log.info(name + " Quit");
        Clients.remove(name);
        Sessions.remove(name);
        BroadCast(name + " has quit.");
    }
}

