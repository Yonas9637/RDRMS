package com.aacra.rdrms;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // የGoogle Apps Script Web App URL
    private static final String SHEET_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxPrSe4-TJW68aUSh0g-neM7Qu7xCfM1YcEuWcMITz9mNnhc8hY-I2dcpAtRZApMOjd/exec";

    // አዲስ የቀለም ንድፍ (Deep Teal & Warm Slate)
    private static final String PRIMARY_DARK = "#004D40";
    private static final String PRIMARY_ACCENT = "#00796B";
    private static final String BG_SOFT = "#E9F0EE";
    private static final String CARD_WHITE = "#FFFFFF";
    private static final String FIELD_BG = "#F4F8F7";
    private static final String BORDER_SOFT = "#C8D8D5";
    private static final String TEXT_DARK = "#102A27";
    private static final String TEXT_MUTED = "#526B67";

    private TextView tvGpsStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Status bar ቀለምን ከ Header ጋር ማጣጣም
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor(PRIMARY_DARK));
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor(BG_SOFT));
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Top Header Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor(PRIMARY_DARK));
        header.setPadding(dp(20), dp(22), dp(20), dp(22));

        TextView badge = new TextView(this);
        badge.setText("RDRMS • ROAD DAMAGE REPORTING");
        badge.setTextSize(11f);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextColor(Color.parseColor("#A7FFEB"));
        badge.setPadding(0, 0, 0, dp(6));
        header.addView(badge);

        TextView title = new TextView(this);
        title.setText("የመንገድ ብልሽት ጥቆማ ፎርም");
        title.setTextSize(22f);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("የመንገድ ችግር ሲያጋጥም መረጃውን በትክክል ከታች ይሙሉ");
        subtitle.setTextSize(13.5f);
        subtitle.setTextColor(Color.parseColor("#E0F2F1"));
        subtitle.setPadding(0, dp(4), 0, 0);
        header.addView(subtitle);

        root.addView(header);

        // Content container
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        // Card 1: የመንገዱ ስም ወይም አካባቢ
        EditText etLocation = makeInput("ለምሳሌ፡- ቦሌ መድኃኔዓለም / ሜክሲኮ", InputType.TYPE_CLASS_TEXT);
        content.addView(makeCard("1. የመንገዱ ስም / አካባቢ *", etLocation));

        // Card 2: የብልሽቱ ዓይነት
        Spinner spType = new Spinner(this);
        String[] damageTypes = {
                "ጉድጓድ (Pothole)",
                "ስንጥቅ / መሰንጠቅ",
                "የጎርፍ ወይም የውሃ ጉዳት",
                "የመንገድ ጠርዝ / ዳርቻ መጎዳት",
                "ሌላ ብልሽት"
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, damageTypes);
        spType.setAdapter(typeAdapter);
        spType.setBackground(makeFieldBox(FIELD_BG, BORDER_SOFT, 10));
        spType.setPadding(dp(10), dp(6), dp(10), dp(6));
        content.addView(makeCard("2. የብልሽቱ ዓይነት *", spType));

        // Card 3: የብልሽቱ ሁኔታ + የቀለም ማሳያ
        LinearLayout severityBox = new LinearLayout(this);
        severityBox.setOrientation(LinearLayout.VERTICAL);

        Spinner spSeverity = new Spinner(this);
        String[] severities = {
                "መካከለኛ",
                "ከፍተኛ (ለተሽከርካሪ አደገኛ)",
                "አስቸኳይ ጥገና የሚያስፈልገው"
        };
        ArrayAdapter<String> sevAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, severities);
        spSeverity.setAdapter(sevAdapter);
        spSeverity.setBackground(makeFieldBox(FIELD_BG, BORDER_SOFT, 10));
        spSeverity.setPadding(dp(10), dp(6), dp(10), dp(6));
        severityBox.addView(spSeverity);

        TextView tvSeverityTag = new TextView(this);
        tvSeverityTag.setTextSize(12.5f);
        tvSeverityTag.setTypeface(null, Typeface.BOLD);
        tvSeverityTag.setGravity(Gravity.CENTER);
        tvSeverityTag.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tagParams.setMargins(0, dp(10), 0, 0);
        tvSeverityTag.setLayoutParams(tagParams);
        severityBox.addView(tvSeverityTag);

        spSeverity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 2) {
                    tvSeverityTag.setText("ደረጃ፡ አስቸኳይ");
                    tvSeverityTag.setTextColor(Color.parseColor("#991B1B"));
                    tvSeverityTag.setBackground(makeFieldBox("#FEE2E2", "#F87171", 20));
                } else if (position == 1) {
                    tvSeverityTag.setText("ደረጃ፡ ከፍተኛ");
                    tvSeverityTag.setTextColor(Color.parseColor("#9A3412"));
                    tvSeverityTag.setBackground(makeFieldBox("#FFEDD5", "#FB923C", 20));
                } else {
                    tvSeverityTag.setText("ደረጃ፡ መካከለኛ");
                    tvSeverityTag.setTextColor(Color.parseColor("#004D40"));
                    tvSeverityTag.setBackground(makeFieldBox("#CCFBF1", "#2DD4BF", 20));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        content.addView(makeCard("3. የብልሽቱ ሁኔታ / አስቸኳይነት *", severityBox));

        // Card 4: ተጨማሪ ማብራሪያ
        EditText etNotes = makeInput("ስለ ብልሽቱ አጭር መግለጫ ያስገቡ...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etNotes.setMinLines(3);
        etNotes.setGravity(Gravity.TOP | Gravity.START);
        content.addView(makeCard("4. ተጨማሪ ማብራሪያ", etNotes));

        // Card 5: ጠቋሚ ስም ወይም ስልክ
        EditText etReporter = makeInput("ለምሳሌ፡- 09...", InputType.TYPE_CLASS_PHONE);
        content.addView(makeCard("5. ጠቋሚ ስም ወይም ስልክ (አማራጭ)", etReporter));
        // Card 6: የGPS መጋጠሚያ ሁኔታ
        tvGpsStatus = new TextView(this);
        tvGpsStatus.setTextSize(13.5f);
        tvGpsStatus.setTextColor(Color.parseColor(TEXT_DARK));
        tvGpsStatus.setBackground(makeFieldBox(FIELD_BG, BORDER_SOFT, 10));
        tvGpsStatus.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(makeCard("6. GPS / መጋጠሚያ (ራስ-ሰር)", tvGpsStatus));

        // Submit Button
        Button btnSubmit = new Button(this);
        btnSubmit.setText("ጥቆማ አስገባ");
        btnSubmit.setTextColor(Color.WHITE);
        btnSubmit.setTextSize(16f);
        btnSubmit.setTypeface(null, Typeface.BOLD);
        btnSubmit.setBackground(makeFieldBox(PRIMARY_ACCENT, PRIMARY_DARK, 12));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        btnParams.setMargins(0, dp(4), 0, dp(16));
        btnSubmit.setLayoutParams(btnParams);
        content.addView(btnSubmit);

        // Result Summary Card
        TextView tvResult = new TextView(this);
        tvResult.setTextSize(14f);
        tvResult.setTextColor(Color.parseColor("#064E3B"));
        tvResult.setBackground(makeFieldBox("#D1FAE5", "#34D399", 12));
        tvResult.setPadding(dp(16), dp(16), dp(16), dp(16));
        tvResult.setVisibility(View.GONE);
        content.addView(tvResult);

        btnSubmit.setOnClickListener(v -> {
            String location = etLocation.getText().toString().trim();
            if (location.isEmpty()) {
                etLocation.setError("እባክዎ የመንገዱን ስም ወይም አካባቢ ያስገቡ");
                etLocation.requestFocus();
                return;
            }

            if (SHEET_WEB_APP_URL.startsWith("PASTE_YOUR_WEB_APP_URL_HERE") || SHEET_WEB_APP_URL.trim().isEmpty()) {
                Toast.makeText(this, "እባክዎ መጀመሪያ Google Apps Script Web App URL ያስገቡ", Toast.LENGTH_LONG).show();
                return;
            }

            String type = spType.getSelectedItem().toString();
            String severity = spSeverity.getSelectedItem().toString();
            String notesRaw = etNotes.getText().toString().trim();
            String reporterRaw = etReporter.getText().toString().trim();
            String coordinates = fetchCurrentCoordinates();
            tvGpsStatus.setText(coordinates);

            String notesDisplay = notesRaw.isEmpty() ? "ምንም ማብራሪያ አልተሰጠም" : notesRaw;
            String reporterDisplay = reporterRaw.isEmpty() ? "አልተጠቀሰም" : reporterRaw;

            String summary = "✓ ጥቆማ በሚገባ ተመዝግቧል!\n\n"
                    + "• አካባቢ፡ " + location + "\n"
                    + "• ዓይነት፡ " + type + "\n"
                    + "• ሁኔታ፡ " + severity + "\n"
                    + "• ማብራሪያ፡ " + notesDisplay + "\n"
                    + "• ጠቋሚ፡ " + reporterDisplay + "\n"
                    + "• GPS፡ " + coordinates;

            btnSubmit.setEnabled(false);
            btnSubmit.setText("እየተላከ ነው...");

            sendReportToGoogleSheet(
                    location,
                    type,
                    severity,
                    notesDisplay,
                    coordinates,
                    reporterDisplay,
                    () -> runOnUiThread(() -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("ጥቆማ አስገባ");

                        tvResult.setText(summary);
                        tvResult.setVisibility(View.VISIBLE);

                        etLocation.setText("");
                        etNotes.setText("");
                        etReporter.setText("");

                        Toast.makeText(this, "ጥቆማው ወደ Google Sheet ተልኳል", Toast.LENGTH_SHORT).show();
                    }),
                    errorMsg -> runOnUiThread(() -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("ጥቆማ አስገባ");
                        Toast.makeText(this, "መላክ አልተሳካም፡ " + errorMsg, Toast.LENGTH_LONG).show();
                    })
            );
        });
        root.addView(content);
        scrollView.addView(root);
        setContentView(scrollView);

        ensureLocationPermissionAndRefresh();
    }

    private void ensureLocationPermissionAndRefresh() {
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            tvGpsStatus.setText("የLocation ፈቃድ በመጠበቅ ላይ...");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            tvGpsStatus.setText(fetchCurrentCoordinates());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (tvGpsStatus != null) {
                tvGpsStatus.setText(fetchCurrentCoordinates());
            }
        }
    }

    private String fetchCurrentCoordinates() {
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            return "ፈቃድ አልተሰጠም";
        }

        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                return "GPS አልተገኘም";
            }

            Location bestLocation = null;
            for (String provider : locationManager.getProviders(true)) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) continue;
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }

            if (bestLocation != null) {
                return String.format(Locale.US, "%.6f, %.6f", bestLocation.getLatitude(), bestLocation.getLongitude());
            }
            return "GPS አልተገኘም (Location ያብሩ)";
        } catch (SecurityException e) {
            return "ፈቃድ አልተሰጠም";
        } catch (Exception e) {
            return "GPS አልተገኘም";
        }
    }

    private interface OnSuccessCallback {
        void onSuccess();
    }

    private interface OnErrorCallback {
        void onError(String message);
    }

    private void sendReportToGoogleSheet(
            String roadName,
            String issueType,
            String severity,
            String description,
            String coordinates,
            String reporter,
            OnSuccessCallback onSuccess,
            OnErrorCallback onError
    ) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(SHEET_WEB_APP_URL.trim());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setInstanceFollowRedirects(true);
                conn.setDoOutput(true);
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                JSONObject payload = new JSONObject();
                payload.put("road_name", roadName);
                payload.put("issue_type", issueType);
                payload.put("severity", severity);
                payload.put("description", description);
                payload.put("coordinates", coordinates);
                payload.put("reporter", reporter);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int code = conn.getResponseCode();
                InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder responseBuilder = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                }

                String responseText = responseBuilder.toString();
                if (code >= 200 && code < 300) {
                    if (responseText.contains("\"status\":\"error\"")) {
                        onError.onError("Apps Script ስህተት አሳይቷል");
                    } else {
                        onSuccess.onSuccess();
                    }
                } else {
                    onError.onError("HTTP " + code);
                }
            } catch (Exception e) {
                onError.onError(e.getMessage() != null ? e.getMessage() : "የኔትወርክ ችግር");
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private LinearLayout makeCard(String labelText, View fieldView) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeFieldBox(CARD_WHITE, BORDER_SOFT, 14));
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(14f);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(Color.parseColor(TEXT_DARK));
        label.setPadding(0, 0, 0, dp(8));
        card.addView(label);

        card.addView(fieldView);
        return card;
    }

    private EditText makeInput(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextColor(Color.parseColor(TEXT_DARK));
        et.setHintTextColor(Color.parseColor(TEXT_MUTED));
        et.setTextSize(14.5f);
        et.setBackground(makeFieldBox(FIELD_BG, BORDER_SOFT, 10));
        et.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        et.setLayoutParams(params);
        return et;
    }

    private GradientDrawable makeFieldBox(String fillHex, String strokeHex, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(fillHex));
        gd.setStroke(dp(1), Color.parseColor(strokeHex));
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
