package me.vilsol.gamecontroller.client;

import me.vilsol.gamecontroller.common.CallbackData;
import me.vilsol.gamecontroller.common.GsonUtils;
import me.vilsol.gamecontroller.common.keys.KeyAction;
import me.vilsol.gamecontroller.common.messages.*;
import me.vilsol.gamecontroller.common.mouse.MouseAction;
import me.vilsol.gamecontroller.common.mouse.MousePositionType;
import org.java_websocket.client.WebSocketClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Player {

    private final Map<String, CallbackData<?, ?>> payloadCallbacks = new HashMap<>();
    private final Map<String, CallbackData<?, ?>> eventCallbacks = new HashMap<>();

    private String name;
    private String endpoint;
    private WebSocketClient webSocketClient;

    public Player(String name, String endpoint){
        this.name = name;
        this.endpoint = endpoint;

        try{
            this.webSocketClient = new WebsocketHandler(this, new URI(endpoint));
        }catch(URISyntaxException e){
            throw new RuntimeException(e);
        }

        this.webSocketClient.connect();
    }

    public void pressKeys(String... keys){
        executeKeyAction(KeyAction.PRESSED, null, keys);
    }

    public void pressKeys(Object payload, String... keys){
        executeKeyAction(KeyAction.PRESSED, payload, keys);
    }

    public void releaseKeys(String... keys){
        executeKeyAction(KeyAction.RELEASED, null, keys);
    }

    public void releaseKeys(Object payload, String... keys){
        executeKeyAction(KeyAction.RELEASED, payload, keys);
    }

    public void clickKeys(String... keys){
        executeKeyAction(KeyAction.CLICKED, null, keys);
    }

    public void clickKeys(Object payload, String... keys){
        executeKeyAction(KeyAction.CLICKED, payload, keys);
    }

    public void executeKeyAction(KeyAction action, Object payload, String... keys){
        KeyboardMessage message = new KeyboardMessage(name, new ArrayList<>());
        String payloadString = null;

        if(payload != null){
            payloadString = GsonUtils.GSON.toJson(payload);
        }

        for(String key : keys){
            message.getActions().add(new KeyboardMessage.Action(action, key, payloadString));
        }

        webSocketClient.send(MessageType.KEYBOARD.ordinal() + GsonUtils.GSON.toJson(message));
    }

    public <T> void pressMouse(MousePositionType positionType, int x, int y, T payload){
        executeMouseAction(MouseAction.PRESSED, positionType, x, y, payload);
    }

    public <T> void releaseMouse(MousePositionType positionType, int x, int y, T payload){
        executeMouseAction(MouseAction.RELEASED, positionType, x, y, payload);
    }

    public <T> void clickMouse(MousePositionType positionType, int x, int y, T payload){
        executeMouseAction(MouseAction.CLICKED, positionType, x, y, payload);
    }

    public <T> void moveMouse(MousePositionType positionType, int x, int y, T payload){
        executeMouseAction(MouseAction.MOVED, positionType, x, y, payload);
    }

    public <T> void executeMouseAction(MouseAction action, MousePositionType positionType, int x, int y, T payload){
        MouseMessage mouseMessage = new MouseMessage(name, action, new MouseMessage.Position(positionType, x, y), GsonUtils.GSON.toJson(payload));
        webSocketClient.send(MessageType.MOUSE.ordinal() + GsonUtils.GSON.toJson(mouseMessage));
    }

    public <T> void onPayload(String payloadType, Class<T> payloadClass, Consumer<T> callback){
        payloadCallbacks.put(payloadType, new CallbackData(callback, null, payloadClass, null));
    }

    protected <T> void processPayload(PayloadMessage message){
        CallbackData<?, ?> callback = payloadCallbacks.get(message.getPayloadType());

        if(callback == null){
            return;
        }

        if(callback.getAType() == null){
            callback.execute(null, null);
            return;
        }

        T payload = GsonUtils.GSON.fromJson(message.getPayload(), (Type) callback.getAType());
        ((CallbackData<T, ?>) callback).execute(payload, null);
    }

    public <T> void sendPayload(String type, T payload){
        PayloadMessage payloadMessage = new PayloadMessage(name, GsonUtils.GSON.toJson(payload), type);
        webSocketClient.send(MessageType.PAYLOAD.ordinal() + GsonUtils.GSON.toJson(payloadMessage));
    }

    public <T> void onEvent(String event, Class<T> payloadClass, Consumer<T> callback){
        eventCallbacks.put(event, new CallbackData(callback, null, payloadClass, null));
    }

    protected <T> void processEvent(EventMessage message){
        CallbackData<?, ?> callback = eventCallbacks.get(message.getEvent());

        if(callback == null){
            return;
        }

        if(callback.getAType() == null){
            callback.execute(null, null);
            return;
        }

        T payload = GsonUtils.GSON.fromJson(message.getPayload(), (Type) callback.getAType());
        ((CallbackData<T, ?>) callback).execute(payload, null);
    }

    public <T> void sendEvent(String event, T payload){
        EventMessage eventMessage = new EventMessage(name, event, GsonUtils.GSON.toJson(payload));
        webSocketClient.send(MessageType.EVENT.ordinal() + GsonUtils.GSON.toJson(eventMessage));
    }

}
