import Request.Request;
import Request.RequestCallback;
import org.json.JSONObject;

public class ClientEvent implements RequestCallback {
    private ChatServer chatServer = null;

    public ClientEvent(ChatServer cs) {
        chatServer = cs;
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
