package Request;

import org.json.JSONObject;

public interface RequestCallback {
    void onCreate(Request req);

    void onRequest(Request req, JSONObject json);

    void onReqClose(Request req);
}
