package com.aacra.rdrms;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("RDRMS Ready");
        textView.setTextSize(24f);
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.CENTER);

        setContentView(textView);
    }
}
