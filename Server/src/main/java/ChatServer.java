import Request.Request;
import Request.RequestCallback;
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
        }

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
     * @param req  req
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
        BroadCastExcept(name, name + " has logined");
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
     * @param req
     * @param reqJSON
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
        BroadCast(name + " has quit.");

    }


    /**
     * BroadCastExcept specific user
     * res {
     * Action BroadCast
     * State Success
     * Msg
     * }
     *
     * @param name specific user
     * @param Msg  BoardCast Msg
     */
    private static void BroadCastExcept(String name, String Msg) {
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "BroadCast")
                .put("State", "Success")
                .put("Msg", Msg);
        for (ConcurrentHashMap.Entry<String, Request> entry : Clients.entrySet()) {
            if (entry.getKey().equals(name)) {
                continue;
            }
            entry.getValue().Response(resJSON);
        }

    }

    /**
     * BroadCast
     * res {
     * Action BroadCast
     * State Success
     * Msg
     * }
     *
     * @param Msg
     */
    private static void BroadCast(String Msg) {
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "BroadCast")
                .put("State", "Success")
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
     * @param req
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

