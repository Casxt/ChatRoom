import Request.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


class ChatServerTest {
    private ChatServer chatServer = new ChatServer();

    @BeforeAll
    static void init() {
    }


    /**
     * 测试Route模块
     */
    @Test
    void Route() {
        ChatServer chatServer = new ChatServer();
        Request req = mock(Request.class);
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        JSONObject json = new JSONObject()
                .put("Action", "Test");
        chatServer.onRequest(req, json);
        verify(req).Send(captor.capture());
        assertEquals("Failed", captor.getValue().get("State"));
    }

    /**
     * 测试Signin模块
     */
    @Test
    void SignIn() {
        //正常请求
        Request req = mock(Request.class);
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        JSONObject json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser");
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        String sessionID = captor.getValue().getString("SessionID");

        //缺失字段
        chatServer.onRequest(req, new JSONObject().put("Action", "SignIn"));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //名字重复
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //退出
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser")
                .put("SessionID", sessionID);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));
    }

    @Test
    void SignOut() {
        Request req = mock(Request.class);
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        JSONObject json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser");
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        String sessionID = captor.getValue().getString("SessionID");

        //缺失字段
        chatServer.onRequest(req, new JSONObject().put("Action", "SignOut"));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //退出
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser")
                .put("SessionID", sessionID);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        //指定用户未登录
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //sessionID 错误
        chatServer.onRequest(req, json.put("SessionID", ""));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));
    }

    @Test
    void BroadCastMsg() {
        //登陆一个用户
        Request req = mock(Request.class);
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        JSONObject json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser");
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        String sessionID = captor.getValue().getString("SessionID");

        //再登陆一个用户
        Request req2 = mock(Request.class);
        ArgumentCaptor<JSONObject> captor2 = ArgumentCaptor.forClass(JSONObject.class);
        json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser2");
        chatServer.onRequest(req2, json);
        verify(req2, atLeastOnce()).Send(captor2.capture());
        assertEquals("Success", captor2.getValue().getString("State"));

        String sessionID2 = captor2.getValue().getString("SessionID");

        //缺失字段
        chatServer.onRequest(req, new JSONObject().put("Action", "BroadCastMsg"));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //用户1发起一次广播
        json = new JSONObject()
                .put("Action", "BroadCastMsg")
                .put("Name", "TestUser")
                .put("SessionID", sessionID)
                .put("Message", "TestMsg");
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        //检查用户2是否有收到广播
        verify(req2, atLeastOnce()).Send(captor2.capture());
        assertEquals("TestMsg", captor2.getValue().getString("Msg"));

        //sessionID 错误
        chatServer.onRequest(req, json.put("SessionID", ""));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //用户1发起一次选择性广播
        JSONArray arr = new JSONArray()
                .put("TestUser2");
        json = new JSONObject()
                .put("Action", "BroadCastMsg")
                .put("Name", "TestUser")
                .put("SessionID", sessionID)
                .put("Message", "TestMsg2")
                .put("Except", arr);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        //检查用户2是否有收到广播
        verify(req2, atLeastOnce()).Send(captor2.capture());
        //Msg 还是TestMsg而不是TestMsg2说明没有收到消息，被正确排除
        assertEquals("TestMsg", captor2.getValue().getString("Msg"));


        //退出用户1
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser")
                .put("SessionID", sessionID);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        //退出用户2
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser2")
                .put("SessionID", sessionID2);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));
    }

    @Test
    void SendMsg() {
        //登陆一个用户
        Request req = mock(Request.class);
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        JSONObject json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser");
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        String sessionID = captor.getValue().getString("SessionID");

        //再登陆一个用户2
        Request req2 = mock(Request.class);
        ArgumentCaptor<JSONObject> captor2 = ArgumentCaptor.forClass(JSONObject.class);
        json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser2");
        chatServer.onRequest(req2, json);
        verify(req2, atLeastOnce()).Send(captor2.capture());
        assertEquals("Success", captor2.getValue().getString("State"));

        String sessionID2 = captor2.getValue().getString("SessionID");

        //再登陆一个用户3
        Request req3 = mock(Request.class);
        ArgumentCaptor<JSONObject> captor3 = ArgumentCaptor.forClass(JSONObject.class);
        json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser3");
        chatServer.onRequest(req3, json);
        verify(req3, atLeastOnce()).Send(captor3.capture());
        assertEquals("Success", captor3.getValue().getString("State"));

        String sessionID3 = captor3.getValue().getString("SessionID");

        //缺失字段
        chatServer.onRequest(req, new JSONObject().put("Action", "SendMsg"));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //用户1发出一次消息
        json = new JSONObject()
                .put("Action", "SendMsg")
                .put("Name", "TestUser")
                .put("SessionID", sessionID)
                .put("To", "TestUser2")
                .put("Message", "TestMsg");
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        //检查用户2是否有收到消息
        verify(req2, atLeastOnce()).Send(captor2.capture());
        assertEquals("TestMsg", captor2.getValue().getString("Msg"));

        //检查用户3是否有收到消息
        //verify通过说明用户3只进行过登陆动作
        verify(req3, times(1)).Send(captor3.capture());

        //sessionID 错误
        chatServer.onRequest(req, json.put("SessionID", ""));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //退出用户1
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser")
                .put("SessionID", sessionID);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        //退出用户2
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser2")
                .put("SessionID", sessionID2);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        //退出用户3
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser3")
                .put("SessionID", sessionID3);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));
    }

    @Test
    void ListUsers() {
        //登陆一个用户
        Request req = mock(Request.class);
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        JSONObject json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser");
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        String sessionID = captor.getValue().getString("SessionID");

        //再登陆一个用户2
        Request req2 = mock(Request.class);
        json = new JSONObject()
                .put("Action", "SignIn")
                .put("Name", "TestUser2");
        chatServer.onRequest(req2, json);
        verify(req2, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        String sessionID2 = captor.getValue().getString("SessionID");

        //缺失字段
        chatServer.onRequest(req, new JSONObject().put("Action", "ListUsers"));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //用户1查询用户列表
        json = new JSONObject()
                .put("Action", "ListUsers")
                .put("Name", "TestUser")
                .put("SessionID", sessionID);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));
        String usrs[] = captor.getValue().getString("Msg").split("\r\n");
        assertTrue(usrs[0].equals("TestUser") || usrs[1].equals("TestUser"));
        assertTrue(usrs[0].equals("TestUser2") || usrs[1].equals("TestUser2"));
        assertEquals("Total online user: 2", usrs[2]);

        //sessionID 错误
        chatServer.onRequest(req, json.put("SessionID", ""));
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Failed", captor.getValue().getString("State"));

        //退出用户1
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser")
                .put("SessionID", sessionID);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));

        //退出用户2
        json = json
                .put("Action", "SignOut")
                .put("Name", "TestUser2")
                .put("SessionID", sessionID2);
        chatServer.onRequest(req, json);
        verify(req, atLeastOnce()).Send(captor.capture());
        assertEquals("Success", captor.getValue().getString("State"));
    }
}