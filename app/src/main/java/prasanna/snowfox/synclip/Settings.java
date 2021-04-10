package prasanna.snowfox.synclip;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings extends AppCompatActivity {
    EditText name, port;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences sp;
        name = findViewById(R.id.namesss);
        port = findViewById(R.id.portsss);
        sp = getSharedPreferences(getString(R.string.conf), MODE_PRIVATE);
        name.setText(sp.getString("name", android.os.Build.MODEL));
        port.setText(sp.getString("port", "8080"));

        MaterialButton save = findViewById(R.id.savesss);
        save.setOnClickListener(view -> {
            if (name.getText().toString() != null)
                sp.edit().putString("name", name.getText().toString()).apply();
            if (onlyDigits(port.getText().toString())) {
                sp.edit().putString("port", port.getText().toString()).apply();
            }

            finish();
        });
    }

    public static boolean onlyDigits(String str) {
        String regex = "[0-9]+";
        Pattern p = Pattern.compile(regex);
        if (str == null) {
            return false;
        }
        Matcher m = p.matcher(str);
        return m.matches();
    }

    public void reset(View view) {
        port.setText("8080");
    }
}