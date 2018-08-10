import Request.Request;
import Request.RequestCallback;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer implements RequestCallback {
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
     * @param json json
     */
    private static void SignIn(Request req, JSONObject json) {
        String name = json.getString("Name");
        JSONObject resJSON = new JSONObject();
        resJSON.put("Action", "SignIn");
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

    @Override
    public void onCreate(Request req) {

    }

    @Override
    public void onRequest(Request req, JSONObject json) {
        ChatServer.Route(req, json);
    }

    @Override
    public void onReqClose(Request req) {

    }
}

