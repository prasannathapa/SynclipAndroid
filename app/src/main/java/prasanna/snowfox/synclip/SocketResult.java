package prasanna.snowfox.synclip;

import com.neovisionaries.ws.client.WebSocket;

public interface SocketResult {
    void onScanFinish(WebSocket socket, boolean found, String error);
}
