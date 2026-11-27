package com.aacra.rdrms;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#F5F7FA"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        layout.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("የመንገድ ብልሽት ጥቆማ ፎርም");
        title.setTextSize(22f);
        title.setTextColor(Color.parseColor("#1A237E"));
        title.setPadding(0, 0, 0, dp(6));
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("የመንገድ ችግር ሲያጋጥም መረጃውን ከታች ይሙሉ");
        subtitle.setTextSize(14f);
        subtitle.setTextColor(Color.parseColor("#546E7A"));
        subtitle.setPadding(0, 0, 0, dp(16));
        layout.addView(subtitle);

        // 1. የመንገዱ ስም ወይም አካባቢ
        layout.addView(makeLabel("የመንገዱ ስም / አካባቢ *"));
        EditText etLocation = makeInput("ለምሳሌ፡- ቦሌ መድኃኔዓለም / ሜክሲኮ", InputType.TYPE_CLASS_TEXT);
        layout.addView(etLocation);

        // 2. የብልሽቱ ዓይነት
        layout.addView(makeLabel("የብልሽቱ ዓይነት *"));
        Spinner spType = new Spinner(this);
        String[] damageTypes = {"ጉድጓድ (Pothole)", "ስንጥቅ / መሰንጠቅ", "የጎርፍ ወይም የውሃ ጉዳት", "የመንገድ ጠርዝ / ዳርቻ መጎዳት", "ሌላ"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, damageTypes);
        spType.setAdapter(adapter);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        spinnerParams.setMargins(0, dp(4), 0, dp(14));
        spType.setLayoutParams(spinnerParams);
        layout.addView(spType);

        // 3. የብልሽቱ መጠን / አስቸኳይነት
        layout.addView(makeLabel("የብልሽቱ ሁኔታ / አስቸኳይነት *"));
        Spinner spSeverity = new Spinner(this);
        String[] severities = {"መካከለኛ", "ከፍተኛ (ለተሽከርካሪ አደገኛ)", "አስቸኳይ ጥገና የሚያስፈልገው"};
        ArrayAdapter<String> sevAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, severities);
        spSeverity.setAdapter(sevAdapter);
        spSeverity.setLayoutParams(spinnerParams);
        layout.addView(spSeverity);

        // 4. ተጨማሪ ማብራሪያ
        layout.addView(makeLabel("ተጨማሪ ማብራሪያ"));
        EditText etNotes = makeInput("ስለ ብልሽቱ አጭር መግለጫ ያስገቡ...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etNotes.setMinLines(3);
        layout.addView(etNotes);

        // 5. ጠቋሚ ስም / ስልክ (አማራጭ)
        layout.addView(makeLabel("ጠቋሚ ስም ወይም ስልክ (አማራጭ)"));
        EditText etReporter = makeInput("ለምሳሌ፡- 09...", InputType.TYPE_CLASS_PHONE);
        layout.addView(etReporter);

        // የማስገቢያ ቁልፍ
        Button btnSubmit = new Button(this);
        btnSubmit.setText("ጥቆማ አስገባ");
        btnSubmit.setBackgroundColor(Color.parseColor("#1A237E"));
        btnSubmit.setTextColor(Color.WHITE);
        btnSubmit.setTextSize(16f);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        btnParams.setMargins(0, dp(8), 0, dp(16));
        btnSubmit.setLayoutParams(btnParams);
        layout.addView(btnSubmit);
        // የተመዘገበውን ጥቆማ ማሳያ ሳጥን
        TextView tvResult = new TextView(this);
        tvResult.setTextSize(14f);
        tvResult.setTextColor(Color.parseColor("#1B5E20"));
        tvResult.setBackgroundColor(Color.parseColor("#E8F5E9"));
        tvResult.setPadding(dp(14), dp(14), dp(14), dp(14));
        tvResult.setVisibility(View.GONE);
        layout.addView(tvResult);

        btnSubmit.setOnClickListener(v -> {
            String location = etLocation.getText().toString().trim();
            if (location.isEmpty()) {
                etLocation.setError("እባክዎ የመንገዱን ስም ወይም አካባቢ ያስገቡ");
                etLocation.requestFocus();
                return;
            }

            String type = spType.getSelectedItem().toString();
            String severity = spSeverity.getSelectedItem().toString();
            String notes = etNotes.getText().toString().trim();
            String reporter = etReporter.getText().toString().trim();

            if (notes.isEmpty()) notes = "ምንም ማብራሪያ አልተሰጠም";
            if (reporter.isEmpty()) reporter = "አልተጠቀሰም";

            String summary = "ጥቆማ ተመዝግቧል!\n\n"
                    + "• አካባቢ፡ " + location + "\n"
                    + "• ዓይነት፡ " + type + "\n"
                    + "• ሁኔታ፡ " + severity + "\n"
                    + "• ማብራሪያ፡ " + notes + "\n"
                    + "• ጠቋሚ፡ " + reporter;

            tvResult.setText(summary);
            tvResult.setVisibility(View.VISIBLE);

            etLocation.setText("");
            etNotes.setText("");
            etReporter.setText("");

            Toast.makeText(this, "የመንገድ ብልሽት ጥቆማ በሚገባ ተመዝግቧል", Toast.LENGTH_SHORT).show();
        });

        scrollView.addView(layout);
        setContentView(scrollView);
    }

    private TextView makeLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setTextColor(Color.parseColor("#263238"));
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private EditText makeInput(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextColor(Color.BLACK);
        et.setHintTextColor(Color.GRAY);
        et.setBackgroundColor(Color.WHITE);
        et.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        et.setLayoutParams(params);
        return et;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
