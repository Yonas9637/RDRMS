package com.aacra.rdrms;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 1001;

    private static final String SHEET_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxPrSe4-TJW68aUSh0g-neM7Qu7xCfM1YcEuWcMITz9mNnhc8hY-I2dcpAtRZApMOjd/exec";

    private static final String PRIMARY_DARK = "#004D40";
    private static final String PRIMARY_ACCENT = "#00796B";
    private static final String BG_SOFT = "#E9F0EE";
    private static final String CARD_WHITE = "#FFFFFF";
    private static final String FIELD_BG = "#F4F8F7";
    private static final String BORDER_SOFT = "#C8D8D5";
    private static final String TEXT_DARK = "#102A27";
    private static final String TEXT_MUTED = "#526B67";

    private LinearLayout formContainer;
    private Button tabDamage, tabPatrol, tabFuel;
    private TextView tvBadge, tvTitle, tvSubtitle;
    private int activeTab = 0;

    // Patrol photo state
    private int currentPhotoTarget = 1; // 1 = violation photo, 2 = corrective photo
    private String violationPhotoBase64 = "";
    private String correctivePhotoBase64 = "";
    private ImageView ivViolationPreview;
    private TextView tvViolationStatus;
    private ImageView ivCorrectivePreview;
    private TextView tvCorrectiveStatus;

    private final ActivityResultLauncher<Void> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    Bitmap resized = resizeBitmap(bitmap, 1024);
                    String base64 = bitmapToBase64(resized, 75);
                    if (currentPhotoTarget == 1) {
                        violationPhotoBase64 = base64;
                        if (ivViolationPreview != null) {
                            ivViolationPreview.setImageBitmap(resized);
                            ivViolationPreview.setVisibility(View.VISIBLE);
                        }
                        if (tvViolationStatus != null) {
                            tvViolationStatus.setText("✓ የጥፋቱ ፎቶ ተነስቷል");
                        }
                    } else {
                        correctivePhotoBase64 = base64;
                        if (ivCorrectivePreview != null) {
                            ivCorrectivePreview.setImageBitmap(resized);
                            ivCorrectivePreview.setVisibility(View.VISIBLE);
                        }
                        if (tvCorrectiveStatus != null) {
                            tvCorrectiveStatus.setText("✓ የማስተካከያው ፎቶ ተነስቷል");
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor(PRIMARY_DARK));
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor(BG_SOFT));
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor(PRIMARY_DARK));
        header.setPadding(dp(20), dp(20), dp(20), dp(20));

        tvBadge = new TextView(this);
        tvBadge.setTextSize(11f);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setTextColor(Color.parseColor("#A7FFEB"));
        tvBadge.setPadding(0, 0, 0, dp(4));
        header.addView(tvBadge);

        tvTitle = new TextView(this);
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.WHITE);
        header.addView(tvTitle);

        tvSubtitle = new TextView(this);
        tvSubtitle.setTextSize(13f);
        tvSubtitle.setTextColor(Color.parseColor("#E0F2F1"));
        tvSubtitle.setPadding(0, dp(4), 0, 0);
        header.addView(tvSubtitle);

        root.addView(header);

        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Color.parseColor("#D8E6E3"));
        tabBar.setPadding(dp(10), dp(10), dp(10), dp(10));

        tabDamage = makeTabButton("የመንገድ ብልሽት");
        tabPatrol = makeTabButton("ፓትሮልና ክትትል");
        tabFuel = makeTabButton("የነዳጅ ክትትል");

        tabBar.addView(tabDamage);
        tabBar.addView(tabPatrol);
        tabBar.addView(tabFuel);
        root.addView(tabBar);

        formContainer = new LinearLayout(this);
        formContainer.setOrientation(LinearLayout.VERTICAL);
        formContainer.setPadding(dp(16), dp(14), dp(16), dp(24));
        root.addView(formContainer);

        tabDamage.setOnClickListener(v -> switchTab(0));
        tabPatrol.setOnClickListener(v -> switchTab(1));
        tabFuel.setOnClickListener(v -> switchTab(2));

        scrollView.addView(root);
        setContentView(scrollView);

        ensureAppPermissions();
        switchTab(0);
    }

    private Button makeTabButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11.5f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(44), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        btn.setLayoutParams(params);
        return btn;
    }

    private void updateTabStyles() {
        Button[] tabs = {tabDamage, tabPatrol, tabFuel};
        for (int i = 0; i < tabs.length; i++) {
            if (i == activeTab) {
                tabs[i].setBackground(makeFieldBox(PRIMARY_ACCENT, PRIMARY_DARK, 8));
                tabs[i].setTextColor(Color.WHITE);
            } else {
                tabs[i].setBackground(makeFieldBox(CARD_WHITE, BORDER_SOFT, 8));
                tabs[i].setTextColor(Color.parseColor(TEXT_DARK));
            }
        }
    }

    private void switchTab(int tabIndex) {
        activeTab = tabIndex;
        updateTabStyles();
        formContainer.removeAllViews();
        if (tabIndex == 0) {
            tvBadge.setText("RDRMS • ROAD DAMAGE REPORTING");
            tvTitle.setText("የመንገድ ብልሽት ጥቆማ ፎርም");
            tvSubtitle.setText("የመንገድ ችግር ሲያጋጥም መረጃውን በትክክል ይሙሉ");
            buildRoadDamageForm();
        } else if (tabIndex == 1) {
            tvBadge.setText("WEST ADDIS ABABA • INFRASTRUCTURE PATROL");
            tvTitle.setText("የመንገድ መሠረተ ልማት ጥበቃና ክትትል ቅጽ");
            tvSubtitle.setText("የፓትሮልና የክትትል መረጃ በትክክል ይመዝግቡ");
            buildPatrolForm();
        } else {
            tvBadge.setText("RDRMS • HEAVY VEHICLE FUEL LOG");
            tvTitle.setText("የከባድ መኪና የነዳጅ ክትትል ቅጽ");
            tvSubtitle.setText("የማሽነሪ/ከባድ መኪና የነዳጅ አሞላልና ንባብ ይመዝግቡ");
            buildFuelForm();
        }
    }

    // ==================== 1. ROAD DAMAGE FORM ====================
    private void buildRoadDamageForm() {
        EditText etLocation = makeInput("ለምሳሌ፡- ቦሌ መድኃኔዓለም / ሜክሲኮ", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("1. የመንገዱ ስም / አካባቢ *", etLocation));

        Spinner spType = makeSpinner(new String[]{
                "ጉድጓድ (Pothole)",
                "ስንጥቅ / መሰንጠቅ",
                "የጎርፍ ወይም የውሃ ጉዳት",
                "የመንገድ ጠርዝ / ዳርቻ መጎዳት",
                "ሌላ ብልሽት"
        });
        formContainer.addView(makeCard("2. የብልሽቱ ዓይነት *", spType));

        LinearLayout severityBox = new LinearLayout(this);
        severityBox.setOrientation(LinearLayout.VERTICAL);
        Spinner spSeverity = makeSpinner(new String[]{
                "መካከለኛ",
                "ከፍተኛ (ለተሽከርካሪ አደገኛ)",
                "አስቸኳይ ጥገና የሚያስፈልገው"
        });
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
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        formContainer.addView(makeCard("3. የብልሽቱ ሁኔታ / አስቸኳይነት *", severityBox));

        EditText etNotes = makeInput("ስለ ብልሽቱ አጭር መግለጫ ያስገቡ...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etNotes.setMinLines(3);
        etNotes.setGravity(Gravity.TOP | Gravity.START);
        formContainer.addView(makeCard("4. ተጨማሪ ማብራሪያ", etNotes));

        EditText etReporter = makeInput("ለምሳሌ፡- 09...", InputType.TYPE_CLASS_PHONE);
        formContainer.addView(makeCard("5. ጠቋሚ ስም ወይም ስልክ (አማራጭ)", etReporter));

        TextView tvGps = makeGpsBox();
        formContainer.addView(makeCard("6. GPS / መጋጠሚያ (ራስ-ሰር)", tvGps));
        TextView tvResult = makeResultBox();
        Button btnSubmit = makeSubmitButton("ጥቆማ አስገባ");
        formContainer.addView(btnSubmit);
        formContainer.addView(tvResult);

        btnSubmit.setOnClickListener(v -> {
            String location = etLocation.getText().toString().trim();
            if (location.isEmpty()) {
                etLocation.setError("እባክዎ የመንገዱን ስም ወይም አካባቢ ያስገቡ");
                etLocation.requestFocus();
                return;
            }
            String coordinates = fetchCurrentCoordinates();
            tvGps.setText(coordinates);

            String type = spType.getSelectedItem().toString();
            String severity = spSeverity.getSelectedItem().toString();
            String notesDisplay = etNotes.getText().toString().trim().isEmpty() ? "ምንም ማብራሪያ አልተሰጠም" : etNotes.getText().toString().trim();
            String reporterDisplay = etReporter.getText().toString().trim().isEmpty() ? "አልተጠቀሰም" : etReporter.getText().toString().trim();

            JSONObject payload = new JSONObject();
            try {
                payload.put("form_type", "road_damage");
                payload.put("road_name", location);
                payload.put("issue_type", type);
                payload.put("severity", severity);
                payload.put("description", notesDisplay);
                payload.put("coordinates", coordinates);
                payload.put("reporter", reporterDisplay);
            } catch (Exception ignored) {}

            String summary = "✓ ጥቆማ በሚገባ ተመዝግቧል!\n\n"
                    + "• አካባቢ፡ " + location + "\n"
                    + "• ዓይነት፡ " + type + "\n"
                    + "• ሁኔታ፡ " + severity + "\n"
                    + "• ማብራሪያ፡ " + notesDisplay + "\n"
                    + "• ጠቋሚ፡ " + reporterDisplay + "\n"
                    + "• GPS፡ " + coordinates;

            submitPayload(payload, summary, btnSubmit, "ጥቆማ አስገባ", tvResult, () -> {
                etLocation.setText("");
                etNotes.setText("");
                etReporter.setText("");
            });
        });
    }

    // ==================== 2. PATROL FORM ====================
    private void buildPatrolForm() {
        violationPhotoBase64 = "";
        correctivePhotoBase64 = "";

        EditText etExpertName = makeInput("የባለሙያ ሙሉ ስም ያስገቡ", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("1. የባለሙያ ስም *", etExpertName));

        EditText etPatrolDate = makeDatePickerInput();
        formContainer.addView(makeCard("2. ፓትሮል የተደረገበት ቀን *", etPatrolDate));

        Spinner spTeam = makeSpinner(new String[]{"ቡድን 1", "ቡድን 2", "ቡድን 3", "ቡድን 4"});
        formContainer.addView(makeCard("3. ፓትሮል ያደረገው ቡድን *", spTeam));

        Spinner spSubCity = makeSpinner(new String[]{
                "1. አዲስ ከተማ ክፍለ ከተማ",
                "2. ኮልፌ ቀራኒዮ ክፍለ ከተማ"
        });
        formContainer.addView(makeCard("4. ክፍለ ከተማ *", spSubCity));

        String[] woredas = new String[15];
        for (int i = 0; i < 15; i++) {
            woredas[i] = String.valueOf(i + 1);
        }
        Spinner spWoreda = makeSpinner(woredas);
        formContainer.addView(makeCard("5. ወረዳ (1 - 15) *", spWoreda));

        EditText etResponsible = makeInput("ጉዳት ያደረሰውን አካል / ድርጅት ያስገቡ", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("6. ጉዳት ያደረሰው አካል", etResponsible));

        EditText etRoadNo = makeInput("የመንገድ ቁጥር ወይም ዋና መንገድ", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("7. ፓትሮል የተደረገበት ቦታ (የመንገድ ቁጥር)", etRoadNo));

        EditText etSpecificLoc = makeInput("ጥፋቱ የተፈጸመበት ልዩ ቦታ", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("8. ጥፋቱ የተፈጸመበት (ልዩ ቦታ) *", etSpecificLoc));

        EditText etViolationType = makeInput("በመንገዱ ላይ የደረሰ የጥፋት ዓይነት", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("9. በመንገዱ ላይ የደረሰ የጥፋት ዓይነት", etViolationType));
        // Photo 1: Violation Photo
        ivViolationPreview = new ImageView(this);
        ivViolationPreview.setAdjustViewBounds(true);
        ivViolationPreview.setMaxHeight(dp(200));
        ivViolationPreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams prevParams1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        prevParams1.setMargins(0, dp(8), 0, 0);
        ivViolationPreview.setLayoutParams(prevParams1);

        tvViolationStatus = new TextView(this);
        tvViolationStatus.setText("ምንም ፎቶ አልተነሳም (አማራጭ)");
        tvViolationStatus.setTextSize(12.5f);
        tvViolationStatus.setTextColor(Color.parseColor(TEXT_MUTED));
        tvViolationStatus.setPadding(0, dp(6), 0, 0);

        Button btnViolationPhoto = makeSecondaryButton("📷 የጥፋቱን ፎቶ አንሳ");
        btnViolationPhoto.setOnClickListener(v -> {
            currentPhotoTarget = 1;
            takePhotoLauncher.launch(null);
        });

        LinearLayout violationPhotoBox = new LinearLayout(this);
        violationPhotoBox.setOrientation(LinearLayout.VERTICAL);
        violationPhotoBox.addView(btnViolationPhoto);
        violationPhotoBox.addView(tvViolationStatus);
        violationPhotoBox.addView(ivViolationPreview);
        formContainer.addView(makeCard("10. የጥፋቱ ዓይነት በፎቶ", violationPhotoBox));

        Spinner spDamageLevel = makeSpinner(new String[]{"ከፍተኛ", "መጠነኛ ያለበት", "ዝቅተኛ"});
        formContainer.addView(makeCard("11. የመንገዱ ጉዳት መጠን", spDamageLevel));

        Spinner spActionTaken = makeSpinner(new String[]{
                "የቃል ማስጠንቀቂያ ተሰጥቷል",
                "የጽሑፍ ማስጠንቀቂያ ተሰጥቷል"
        });
        formContainer.addView(makeCard("12. የተወሰደ እርምጃ", spActionTaken));

        Spinner spCorrective = makeSpinner(new String[]{
                "ጉዳት ያደረሰው አካል የጉዳት ካሳ ተጠይቋል",
                "ንብረቱ እንዲወረስ ተደርጓል"
        });
        formContainer.addView(makeCard("13. በተሰጠው ማስጠንቀቂያ መሠረት የተወሰደ ማስተካከያ", spCorrective));

        // Photo 2: Corrective Action Photo
        ivCorrectivePreview = new ImageView(this);
        ivCorrectivePreview.setAdjustViewBounds(true);
        ivCorrectivePreview.setMaxHeight(dp(200));
        ivCorrectivePreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams prevParams2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        prevParams2.setMargins(0, dp(8), 0, 0);
        ivCorrectivePreview.setLayoutParams(prevParams2);

        tvCorrectiveStatus = new TextView(this);
        tvCorrectiveStatus.setText("ምንም ፎቶ አልተነሳም (አማራጭ)");
        tvCorrectiveStatus.setTextSize(12.5f);
        tvCorrectiveStatus.setTextColor(Color.parseColor(TEXT_MUTED));
        tvCorrectiveStatus.setPadding(0, dp(6), 0, 0);

        Button btnCorrectivePhoto = makeSecondaryButton("📷 የተወሰደ ማስተካከያ በፎቶ አንሳ");
        btnCorrectivePhoto.setOnClickListener(v -> {
            currentPhotoTarget = 2;
            takePhotoLauncher.launch(null);
        });

        LinearLayout correctivePhotoBox = new LinearLayout(this);
        correctivePhotoBox.setOrientation(LinearLayout.VERTICAL);
        correctivePhotoBox.addView(btnCorrectivePhoto);
        correctivePhotoBox.addView(tvCorrectiveStatus);
        correctivePhotoBox.addView(ivCorrectivePreview);
        formContainer.addView(makeCard("14. የተወሰደ ማስተካከያ በፎቶ", correctivePhotoBox));

        EditText etNoWarning = makeInput("ማስጠንቀቂያ ካልተሰጠ ምክንያቱን ያስገቡ", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("15. ማስጠንቀቂያ ካልተሰጠ ያልተሰጠበት ምክንያት", etNoWarning));

        EditText etDistance = makeInput("ለምሳሌ፡- 5.2 km", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("16. የተሸፈነ ርቀት (Distance Coverage)", etDistance));

        TextView tvGps = makeGpsBox();
        formContainer.addView(makeCard("17. GPS / መጋጠሚያ (ራስ-ሰር)", tvGps));
        TextView tvResult = makeResultBox();
        Button btnSubmit = makeSubmitButton("የፓትሮል ሪፖርት አስገባ");
        formContainer.addView(btnSubmit);
        formContainer.addView(tvResult);

        btnSubmit.setOnClickListener(v -> {
            String expertName = etExpertName.getText().toString().trim();
            String specificLoc = etSpecificLoc.getText().toString().trim();
            if (expertName.isEmpty()) {
                etExpertName.setError("እባክዎ የባለሙያውን ስም ያስገቡ");
                etExpertName.requestFocus();
                return;
            }
            if (specificLoc.isEmpty()) {
                etSpecificLoc.setError("እባክዎ ጥፋቱ የተፈጸመበትን ልዩ ቦታ ያስገቡ");
                etSpecificLoc.requestFocus();
                return;
            }

            String coordinates = fetchCurrentCoordinates();
            tvGps.setText(coordinates);

            String patrolDate = etPatrolDate.getText().toString().trim();
            String team = spTeam.getSelectedItem().toString();
            String subCity = spSubCity.getSelectedItem().toString();
            String woreda = spWoreda.getSelectedItem().toString();
            String responsible = etResponsible.getText().toString().trim();
            String roadNo = etRoadNo.getText().toString().trim();
            String violationType = etViolationType.getText().toString().trim();
            String damageLevel = spDamageLevel.getSelectedItem().toString();
            String actionTaken = spActionTaken.getSelectedItem().toString();
            String corrective = spCorrective.getSelectedItem().toString();
            String noWarning = etNoWarning.getText().toString().trim();
            String distance = etDistance.getText().toString().trim();

            long nowMs = System.currentTimeMillis();
            JSONObject payload = new JSONObject();
            try {
                payload.put("form_type", "patrol");
                payload.put("expert_name", expertName);
                payload.put("patrol_date", patrolDate);
                payload.put("patrol_team", team);
                payload.put("sub_city", subCity);
                payload.put("woreda", woreda);
                payload.put("responsible_party", responsible);
                payload.put("road_number", roadNo);
                payload.put("specific_location", specificLoc);
                payload.put("violation_type", violationType);
                payload.put("violation_photo_base64", violationPhotoBase64);
                payload.put("violation_photo_name", "violation_" + nowMs + ".jpg");
                payload.put("damage_level", damageLevel);
                payload.put("action_taken", actionTaken);
                payload.put("corrective_action", corrective);
                payload.put("corrective_photo_base64", correctivePhotoBase64);
                payload.put("corrective_photo_name", "corrective_" + nowMs + ".jpg");
                payload.put("no_warning_reason", noWarning);
                payload.put("distance_coverage", distance);
                payload.put("coordinates", coordinates);
            } catch (Exception ignored) {}

            int photoCount = (violationPhotoBase64.isEmpty() ? 0 : 1) + (correctivePhotoBase64.isEmpty() ? 0 : 1);
            String summary = "✓ የፓትሮል ሪፖርት ተመዝግቧል!\n\n"
                    + "• ባለሙያ፡ " + expertName + "\n"
                    + "• ቀን፡ " + patrolDate + "\n"
                    + "• ቡድን፡ " + team + "\n"
                    + "• ክ/ከተማ፡ " + subCity + " | ወረዳ፡ " + woreda + "\n"
                    + "• ልዩ ቦታ፡ " + specificLoc + "\n"
                    + "• የተያያዙ ፎቶዎች፡ " + photoCount + "\n"
                    + "• GPS፡ " + coordinates;

            submitPayload(payload, summary, btnSubmit, "የፓትሮል ሪፖርት አስገባ", tvResult, () -> {
                etExpertName.setText("");
                etResponsible.setText("");
                etRoadNo.setText("");
                etSpecificLoc.setText("");
                etViolationType.setText("");
                etNoWarning.setText("");
                etDistance.setText("");
                violationPhotoBase64 = "";
                correctivePhotoBase64 = "";
                ivViolationPreview.setVisibility(View.GONE);
                ivCorrectivePreview.setVisibility(View.GONE);
                tvViolationStatus.setText("ምንም ፎቶ አልተነሳም (አማራጭ)");
                tvCorrectiveStatus.setText("ምንም ፎቶ አልተነሳም (አማራጭ)");
            });
        });
    }

    // ==================== 3. FUEL TRACKING FORM ====================
    private void buildFuelForm() {
        EditText etPlate = makeInput("ለምሳሌ፡- AA-3-12345 ወይም Mach-04", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("1. የመኪና / ማሽነሪ ታርጋ ወይም መለያ *", etPlate));

        Spinner spVehicleType = makeSpinner(new String[]{
                "Grader (ግራደር)",
                "Dozer (ዶዘር)",
                "Excavator (ኤክስካቫተር)",
                "Dump Truck (ከባድ የጭነት መኪና)",
                "Roller / Compactor",
                "ሌላ ማሽነሪ / ተሽከርካሪ"
        });
        formContainer.addView(makeCard("2. የማሽነሪው / ተሽከርካሪው ዓይነት *", spVehicleType));

        Spinner spFuelType = makeSpinner(new String[]{"ናፍጣ (Diesel)", "ቤንዚን (Petrol)"});
        formContainer.addView(makeCard("3. የነዳጅ ዓይነት *", spFuelType));

        EditText etLiters = makeInput("ለምሳሌ፡- 150", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        formContainer.addView(makeCard("4. የተሞላው የነዳጅ መጠን (በሊትር) *", etLiters));

        EditText etOdometer = makeInput("የኪሎሜትር (KM) ወይም የሰዓት (Hour Meter) ንባብ", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("5. የኪሎሜትር / ሰዓት ንባብ (Odometer / Hour)", etOdometer));

        EditText etStation = makeInput("የመሥሪያ ቤት ዴፖ ወይም የማደያ ስም", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("6. የነዳጅ ማደያ / ዴፖ", etStation));

        EditText etVoucher = makeInput("የደረሰኝ / ኩፖን / Voucher ቁጥር", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("7. የደረሰኝ / ኩፖን ቁጥር", etVoucher));

        EditText etOperator = makeInput("የአሽከርካሪው / ኦፕሬተሩ ስም", InputType.TYPE_CLASS_TEXT);
        formContainer.addView(makeCard("8. የአሽከርካሪው / ኦፕሬተሩ ስም *", etOperator));

        EditText etNotes = makeInput("የሥራ ቦታ/ፕሮጀክት ወይም ማስታወሻ...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etNotes.setMinLines(2);
        formContainer.addView(makeCard("9. ተጨማሪ ማብራሪያ", etNotes));

        TextView tvGps = makeGpsBox();
        formContainer.addView(makeCard("10. GPS / መጋጠሚያ (ራስ-ሰር)", tvGps));

        TextView tvResult = makeResultBox();
        Button btnSubmit = makeSubmitButton("የነዳጅ መረጃ አስገባ");
        formContainer.addView(btnSubmit);
        formContainer.addView(tvResult);

        btnSubmit.setOnClickListener(v -> {
            String plate = etPlate.getText().toString().trim();
            String liters = etLiters.getText().toString().trim();
            String operator = etOperator.getText().toString().trim();

            if (plate.isEmpty()) {
                etPlate.setError("እባክዎ የመኪናውን/ማሽነሪውን ታርጋ ወይም መለያ ያስገቡ");
                etPlate.requestFocus();
                return;
            }
            if (liters.isEmpty()) {
                etLiters.setError("እባክዎ የተሞላውን ሊትር ያስገቡ");
                etLiters.requestFocus();
                return;
            }
            if (operator.isEmpty()) {
                etOperator.setError("እባክዎ የአሽከርካሪውን/ኦፕሬተሩን ስም ያስገቡ");
                etOperator.requestFocus();
                return;
            }

            String coordinates = fetchCurrentCoordinates();
            tvGps.setText(coordinates);
            String vehicleType = spVehicleType.getSelectedItem().toString();
            String fuelType = spFuelType.getSelectedItem().toString();
            String odometer = etOdometer.getText().toString().trim();
            String station = etStation.getText().toString().trim();
            String voucher = etVoucher.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            JSONObject payload = new JSONObject();
            try {
                payload.put("form_type", "fuel");
                payload.put("plate_number", plate);
                payload.put("vehicle_type", vehicleType);
                payload.put("fuel_type", fuelType);
                payload.put("liters", liters);
                payload.put("odometer", odometer);
                payload.put("fuel_station", station);
                payload.put("voucher_number", voucher);
                payload.put("operator_name", operator);
                payload.put("coordinates", coordinates);
                payload.put("notes", notes);
            } catch (Exception ignored) {}

            String summary = "✓ የነዳጅ መረጃ ተመዝግቧል!\n\n"
                    + "• ታርጋ/መለያ፡ " + plate + " (" + vehicleType + ")\n"
                    + "• ነዳጅ፡ " + liters + " ሊትር (" + fuelType + ")\n"
                    + "• ኦፕሬተር፡ " + operator + "\n"
                    + "• GPS፡ " + coordinates;

            submitPayload(payload, summary, btnSubmit, "የነዳጅ መረጃ አስገባ", tvResult, () -> {
                etPlate.setText("");
                etLiters.setText("");
                etOdometer.setText("");
                etStation.setText("");
                etVoucher.setText("");
                etOperator.setText("");
                etNotes.setText("");
            });
        });
    }

    // ==================== IMAGE HELPERS ====================
    private Bitmap resizeBitmap(Bitmap src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxDim && h <= maxDim) return src;
        float scale = Math.min((float) maxDim / w, (float) maxDim / h);
        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);
        return Bitmap.createScaledBitmap(src, newW, newH, true);
    }

    private String bitmapToBase64(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    // ==================== SUBMIT & NETWORK ====================
    private interface OnClearFields {
        void clear();
    }

    private void submitPayload(
            JSONObject payload,
            String summaryText,
            Button btnSubmit,
            String defaultBtnText,
            TextView tvResult,
            OnClearFields onClear
    ) {
        if (SHEET_WEB_APP_URL.trim().isEmpty()) {
            Toast.makeText(this, "Web App URL አልተገባም", Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("እየተላከ ነው...");

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(SHEET_WEB_APP_URL.trim());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setInstanceFollowRedirects(true);
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }
                int code = conn.getResponseCode();
                InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder sb = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                }
                String responseText = sb.toString();

                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText(defaultBtnText);
                    if (code >= 200 && code < 300 && !responseText.contains("\"status\":\"error\"")) {
                        tvResult.setText(summaryText);
                        tvResult.setVisibility(View.VISIBLE);
                        onClear.clear();
                        Toast.makeText(this, "መረጃውና ፎቶው በትክክል ተልኳል", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "መላክ አልተሳካም (HTTP " + code + ")", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                String err = e.getMessage() != null ? e.getMessage() : "የኔትወርክ ችግር";
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText(defaultBtnText);
                    Toast.makeText(this, "መላክ አልተሳካም፡ " + err, Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ==================== GPS & PERMISSIONS ====================
    private void ensureAppPermissions() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            switchTab(activeTab);
        }
    }

    private String fetchCurrentCoordinates() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return "ፈቃድ አልተሰጠም";

        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return "GPS አልተገኘም";

            Location best = null;
            for (String provider : lm.getProviders(true)) {
                Location loc = lm.getLastKnownLocation(provider);
                if (loc != null && (best == null || loc.getAccuracy() < best.getAccuracy())) {
                    best = loc;
                }
            }
            if (best != null) {
                return String.format(Locale.US, "%.6f, %.6f", best.getLatitude(), best.getLongitude());
            }
            return "GPS አልተገኘም (Location ያብሩ)";
        } catch (Exception e) {
            return "GPS አልተገኘም";
        }
    }
    // ==================== UI HELPERS ====================
    private EditText makeDatePickerInput() {
        EditText et = makeInput("ቀን ይምረጡ (mm/dd/yyyy)", InputType.TYPE_CLASS_TEXT);
        et.setFocusable(false);
        et.setClickable(true);
        Calendar cal = Calendar.getInstance();
        String today = String.format(Locale.US, "%02d/%02d/%04d",
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.YEAR));
        et.setText(today);
        et.setOnClickListener(v -> new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String picked = String.format(Locale.US, "%02d/%02d/%04d", month + 1, dayOfMonth, year);
            et.setText(picked);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show());
        return et;
    }

    private Spinner makeSpinner(String[] items) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        sp.setAdapter(adapter);
        sp.setBackground(makeFieldBox(FIELD_BG, BORDER_SOFT, 10));
        sp.setPadding(dp(10), dp(6), dp(10), dp(6));
        return sp;
    }

    private TextView makeGpsBox() {
        TextView tv = new TextView(this);
        tv.setText(fetchCurrentCoordinates());
        tv.setTextSize(13.5f);
        tv.setTextColor(Color.parseColor(TEXT_DARK));
        tv.setBackground(makeFieldBox(FIELD_BG, BORDER_SOFT, 10));
        tv.setPadding(dp(12), dp(12), dp(12), dp(12));
        return tv;
    }

    private Button makeSecondaryButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(PRIMARY_DARK));
        btn.setTextSize(13.5f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        btn.setBackground(makeFieldBox("#E0F2F1", PRIMARY_ACCENT, 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        btn.setLayoutParams(params);
        return btn;
    }

    private Button makeSubmitButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(15.5f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackground(makeFieldBox(PRIMARY_ACCENT, PRIMARY_DARK, 12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        params.setMargins(0, dp(4), 0, dp(14));
        btn.setLayoutParams(params);
        return btn;
    }

    private TextView makeResultBox() {
        TextView tv = new TextView(this);
        tv.setTextSize(13.5f);
        tv.setTextColor(Color.parseColor("#064E3B"));
        tv.setBackground(makeFieldBox("#D1FAE5", "#34D399", 12));
        tv.setPadding(dp(14), dp(14), dp(14), dp(14));
        tv.setVisibility(View.GONE);
        return tv;
    }

    private LinearLayout makeCard(String labelText, View fieldView) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeFieldBox(CARD_WHITE, BORDER_SOFT, 14));
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(13.5f);
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
        et.setTextSize(14f);
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
