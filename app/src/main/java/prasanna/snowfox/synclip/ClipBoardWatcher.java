package prasanna.snowfox.synclip;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.system.Os;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;
import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.util.List;
import java.util.Map;

public class ClipBoardWatcher extends Service implements SocketResult {
    private Context mContext;
    private ClipboardManager clipboardManager;
    private PortScanner portScanner;
    private SharedPreferences sp;
    private LocalBroadcastManager localBroadcastManager;
    private boolean wifiConn, found;
    private final Handler handler = new Handler();
    private String error,uri, update;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    private final BroadcastReceiver pingPong = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            Intent localIntent = new Intent(getString(R.string.SCAN_RESULT));
            localIntent.putExtra("found",found && socket != null);
            localIntent.putExtra("error",error);
            localIntent.putExtra("uri", uri);
            localBroadcastManager.sendBroadcast(localIntent);
        }
    };
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int wifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            wifiConn = false;
            switch (wifiStateExtra) {
                case WifiManager.WIFI_STATE_ENABLED:
                    if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                        wifiConn = true;
                        if (portScanner != null) {
                            portScanner.cancel(true);
                            portScanner = null;
                        }

                        if(!sp.getBoolean("autoconnect",true))
                            portScanner = new PortScanner(getApplicationContext(), ClipBoardWatcher.this,sp.getString("url",null));
                        else
                            portScanner = new PortScanner(getApplicationContext(), ClipBoardWatcher.this);
                        portScanner.execute();
                    }
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    if(portScanner != null)
                        portScanner.cancel(true);
                    portScanner = null;
                    break;
            }
        }
    };
    private WebSocket socket;
    private WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState){

        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers){

        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException cause){

        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer){
            Intent localIntent = new Intent(getString(R.string.SCAN_RESULT));
            localIntent.putExtra("found",false);
            localIntent.putExtra("error","closed by server: "+closedByServer);
            localBroadcastManager.sendBroadcast(localIntent);
            socket.removeListener(webSocketListener);
            socket = null;
        }

        @Override
        public void onFrame(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onTextFrame(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onCloseFrame(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onPingFrame(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onTextMessage(WebSocket websocket, String text){

            JSONObject obj = null;
            try {
                obj = new JSONObject(text);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                update = obj.getString("data");
                update = new String(Base64.decode(update, Base64.NO_WRAP), StandardCharsets.UTF_8);
                ClipData clip = ClipData.newPlainText("Synclip", update);
                CharSequence charSequence = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                if(!String.valueOf(charSequence).equals(update))
                    clipboard.setPrimaryClip(clip);
                Log.d("My App", obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onTextMessage(WebSocket websocket, byte[] data){
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) {
        }

        @Override
        public void onSendingFrame(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onFrameSent(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame){

        }

        @Override
        public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread){

        }

        @Override
        public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread){

        }

        @Override
        public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread){

        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause){
            Toast.makeText(getApplicationContext(), cause.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame){

        }

        @Override
        public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames){

        }

        @Override
        public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed){

        }

        @Override
        public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data){
        }

        @Override
        public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame){
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause){
            Toast.makeText(getApplicationContext(), cause.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause){

        }

        @Override
        public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers){

        }
    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private final ClipboardManager.OnPrimaryClipChangedListener listener = this::performClipboardCheck;
    IntentFilter intentFilter;
    @Override
    public void onCreate() {
        Log.v("CB WATCHER", "onCreate CBService");
        mContext = this;
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(listener);
        super.onCreate();
        sp = getSharedPreferences(getString(R.string.conf), MODE_PRIVATE);
        if(!sp.getBoolean("autoconnect",true))
            portScanner = new PortScanner(this, this,sp.getString("url",null));
        else
            portScanner = new PortScanner(this, this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, intentFilter);
        IntentFilter intentFilter = new IntentFilter(getString(R.string.PING));
        localBroadcastManager.registerReceiver(pingPong,intentFilter);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(getApplicationContext(), "Destroy", Toast.LENGTH_SHORT).show();
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).removePrimaryClipChangedListener(listener);
        if(portScanner != null)
            portScanner.cancel(true);
        if(socket != null){
            socket.sendClose();
            socket.removeListener(webSocketListener);
            webSocketListener = null;
            socket = null;
        }
        unregisterReceiver(wifiStateReceiver);
        localBroadcastManager.unregisterReceiver(pingPong);
        super.onDestroy();
    }
    private void performClipboardCheck() {
        Log.v("CB Watcher", "performClipboardCheck");
        if (!clipboardManager.hasPrimaryClip()) return;
        String clipString;
        try {
            CharSequence charSequence = clipboardManager.getPrimaryClip().getItemAt(0).getText();
            clipString = String.valueOf(charSequence);
        } catch (Error ignored) {
            Toast.makeText(mContext, ignored.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        if(clipString.equals(update)){
            update = null;
            return;
        }
        clipString = new String(Base64.encode(clipString.getBytes(),Base64.NO_WRAP));
        if(socket != null)
            socket.sendText("{\"type\":\"text\", \"RSApublicKey\":null, \"data\":\""+clipString+"\",\"reqType\":\"sync\", \"encrypted\":false}");
        //Toast.makeText(mContext, clipString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScanFinish(WebSocket socket, boolean found, String error) {
        this.found = found;
        this.error = error;
        Intent localIntent = new Intent(getString(R.string.SCAN_RESULT));
        localIntent.putExtra("found",found);
        localIntent.putExtra("error",error);
        this.socket = socket;
        if (found) {
            localIntent.putExtra("uri",socket.getURI().toASCIIString());
            this.uri = socket.getURI().toASCIIString();
            this.socket.addListener(webSocketListener);
            this.socket.sendText("{\"name\":\""+sp.getString("name", Build.MODEL)+"\",\"RSApublicKey\":"+getPublicKey()+", \"reqType\":\"update\"}");
            this.socket.setPingInterval(5000);

        } else if(wifiConn){
            this.uri = "Not Connected";
            handler.postDelayed(() -> {
                // Do something after 5s = 5000ms
                if (portScanner != null) {
                    portScanner.cancel(true);
                    portScanner = null;
                }
                if (!sp.getBoolean("autoconnect", true))
                    portScanner = new PortScanner(getApplicationContext(), this, sp.getString("url", null));
                else
                    portScanner = new PortScanner(getApplicationContext(), this);
                portScanner.execute();
            }, 5000);

        }
        localBroadcastManager.sendBroadcast(localIntent);
    }

    private String getPublicKey() {
        return "null";
    }
}
