package prasanna.snowfox.synclip;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.snackbar.Snackbar;


public class MainActivity extends AppCompatActivity {
    Toolbar toolbar;
    private WifiManager wifiManager;
    private TextView wifiStatus, animLabel,serverStatus;
    private LottieAnimationView animationView;
    private boolean uiUpdatable, startService;
    private final Handler handler = new Handler();
    private SharedPreferences sp;
    private LocalBroadcastManager localBroadcastManager;
    private Intent cbwService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiStatus = findViewById(R.id.wifistatus);
        serverStatus = findViewById(R.id.serverstatus);
        animLabel = findViewById(R.id.animation_label);
        animationView = findViewById(R.id.animation_view);
        sp = getSharedPreferences(getString(R.string.conf), MODE_PRIVATE);
        IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        IntentFilter localIntentFilter = new IntentFilter(getString(R.string.SCAN_RESULT));

        registerReceiver(wifiStateReceiver, intentFilter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (! android.provider.Settings.canDrawOverlays(getApplicationContext())) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
                finish();
            }
        }
        startService(new Intent(MainActivity.this, FloatingService.class));
        cbwService = new Intent(MainActivity.this, ClipBoardWatcher.class);
        if(sp.getBoolean(getString(R.string.AUTO_START),true)) {
            startService(cbwService);
            startService = true;
        }
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(ScanListener,localIntentFilter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.onoff);
        SwitchCompat mySwitch = item.getActionView().findViewById(R.id.onoff);
        mySwitch.setChecked(startService);
        mySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            startService = isChecked;
            if(isChecked)
                getApplicationContext().startService(cbwService);
            else
                getApplicationContext().stopService(cbwService);
            sp.edit().putBoolean(getString(R.string.AUTO_START),isChecked).apply();
            updateUI();
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings_menu) {
            startActivity(new Intent(this,Settings.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiStateReceiver);
        localBroadcastManager.unregisterReceiver(ScanListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiUpdatable = true;
        Intent ping = new Intent(getString(R.string.PING));
        localBroadcastManager.sendBroadcast(ping);
        if(pendingUpdate)
            updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiUpdatable = false;
    }
    private void updateUI(){
        if(!uiUpdatable) {
            pendingUpdate = true;
            return;
        }
        pendingUpdate = false;

        if (wifiOn)
            wifiStatus.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getApplicationContext(), R.drawable.green), null, null, null);
        else
            wifiStatus.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getApplicationContext(), R.drawable.red), null, null, null);
        wifiStatus.setText(wifiText);
        animLabel.setText(animText);
        if (!sp.getBoolean("autoconnect", true))
            serverStatus.setText(sp.getString("url", null));
        else
            serverStatus.setText(serverIP);
        if (connected)
            serverStatus.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getApplicationContext(), R.drawable.green), null, null, null);
        else
            serverStatus.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getApplicationContext(), R.drawable.red), null, null, null);
        if (playAnim)
            animationView.playAnimation();
        else {
            animationView.setProgress(progress);
            animationView.pauseAnimation();
        }
        if(!startService){
            animationView.setProgress(0);
            animationView.cancelAnimation();
            serverStatus.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getApplicationContext(), R.drawable.gray), null, null, null);
            wifiStatus.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getApplicationContext(), R.drawable.gray), null, null, null);
            animLabel.setText("Sync OFF");
        }
    }
    private String wifiText,animText,serverIP;
    private boolean wifiOn, connected, pendingUpdate, playAnim, wifiConn;
    private float progress;
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int wifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);
            switch (wifiStateExtra) {
                case WifiManager.WIFI_STATE_ENABLED:
                    wifiOn = true;
                    wifiText = "Wi-Fi is ON";
                    animText = "Waiting for WiFi connection";
                    serverIP = "Not Connected";
                    connected = false;
                    updateUI();
                    wifiConn = false;
                    if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                        animText = "Searching";
                        playAnim = true;
                        wifiConn = true;
                        updateUI();
                    }
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    wifiText = "Wi-Fi is OFF";
                    animText = "Disconnected";
                    serverIP = "Not Connected";
                    connected = false;
                    wifiOn = false;
                    progress = 0f;
                    playAnim = false;
                    updateUI();
                    break;
            }

        }
    };
    private final BroadcastReceiver ScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            String error = intent.getStringExtra("error");
            String uri = intent.getStringExtra("uri");
            boolean found = intent.getBooleanExtra("found",false);
            Log.d( "Received data : ", "error: "+error+ ", found: "+found);

            if (found) {
                progress = 1f;
                playAnim = false;
                animText = "CONNECTED";
                connected = true;
                serverIP = uri;
            } else if(wifiConn){
                Snackbar.make(serverStatus,error,Snackbar.LENGTH_LONG).show();
                serverIP = "Not Connected";
                connected = false;
                animText = "Disconnected";
                playAnim = false;
                progress = 0f;
            }
            updateUI();
        }
    };


    public void congif(View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_configure, null);
        dialogBuilder.setView(dialogView);
        EditText editText = dialogView.findViewById(R.id.url);
        SwitchCompat sw = dialogView.findViewById(R.id.autotoggle);
        String url = sp.getString("url", null);
        boolean auto = sp.getBoolean("autoconnect",true);
        sw.setChecked(url == null || auto);
        editText.setEnabled(!(url == null || auto));
        editText.setText(url);
        sw.setOnCheckedChangeListener((compoundButton, b) -> {
            editText.setEnabled(!b);
        });
        Button save = dialogView.findViewById(R.id.savebtn);
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        save.setOnClickListener(btn -> {
            sp.edit().putBoolean("autoconnect", sw.isChecked()).putString("url",editText.getText().toString()).apply();
            updateUI();
            alertDialog.cancel();
        });
        alertDialog.show();
    }
}