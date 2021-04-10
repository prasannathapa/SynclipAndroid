package prasanna.snowfox.synclip;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;

class PortScanner extends AsyncTask<Void, Void, WebSocket> {

    public static final String TAG = "PORT SCANNER task";

    private final WeakReference<Context> mContextRef;
    private final SocketResult socketResult;
    private final WebSocketFactory factory;
    private String url, error;


    public PortScanner(Context context, SocketResult socketResult) {
        mContextRef = new WeakReference<>(context);
        this.socketResult = socketResult;
        factory = new WebSocketFactory();
    }
    public PortScanner(Context context, SocketResult socketResult, String url) {
        mContextRef = new WeakReference<>(context);
        this.socketResult = socketResult;
        factory = new WebSocketFactory();
        this.url = url;
    }

    @Override
    protected WebSocket doInBackground(Void... voids) {
        Log.d(TAG, "Let's sniff the network");
        if(url != null){
            try {
                WebSocket webSocket = factory.createSocket(url, 1000);
                webSocket.connect();
                error  = null;
                return webSocket;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                error = e.getLocalizedMessage();
                return null;
            }
        }
        else {
            try {
                Context context = mContextRef.get();
                if (context != null) {
                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    int ipAddress = connectionInfo.getIpAddress();
                    String ipString = Formatter.formatIpAddress(ipAddress);

                    Log.d(TAG, "activeNetwork: " + activeNetwork);
                    Log.d(TAG, "ipString: " + ipString);

                    String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
                    Log.d(TAG, "prefix: " + prefix);

                    for (int i = 1; i <= 255; i++) {
                        String ip = prefix + i;
                        WebSocket webSocket = factory.createSocket("ws://" + ip + ":8080", 50);
                        try {
                            webSocket.connect();
                            error = null;
                            return webSocket;
                        } catch (WebSocketException e) {
                            Log.e(TAG, e.getMessage());
                            error = "SERVER NOT FOUND";
                        }
                    }
                }
            } catch (Exception t) {
                Log.e(TAG, "Well that's not good.", t);
                error = t.getLocalizedMessage();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(WebSocket socket) {
        super.onPostExecute(socket);
        socketResult.onScanFinish(socket, socket != null, error);
    }
}