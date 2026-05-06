package com.wascanner.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements ScanResult.OnScanUpdateListener {

    private static final int REQ_CONTACTS = 1;
    private static final int REQ_STORAGE  = 2;
    private static final int REQ_SAVE     = 3;

    // Setup cards
    private LinearLayout cardAccessibility, cardContacts;

    // Stats
    private LinearLayout layoutStats;
    private TextView tvSaved, tvUnsaved, tvLive;

    // Auto-scroll section
    private LinearLayout layoutReady;
    private Button btnAutoScroll, btnStopScroll;
    private TextView tvScrollStatus;

    // Results section
    private LinearLayout layoutActions;
    private TextView tvListHeader;
    private ListView listView;
    private TextView tvEmpty;

    // Action buttons
    private Button btnSaveAll, btnExportVcf, btnExportTxt, btnClear;

    private NumberAdapter adapter;
    private List<String> numbers = new ArrayList<>();
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable ticker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        adapter = new NumberAdapter(this, numbers);
        listView.setAdapter(adapter);
        setupButtons();
        ScanResult.getInstance().setListener(this);

        ticker = () -> { refreshUI(); uiHandler.postDelayed(ticker, 800); };
    }

    @Override protected void onResume()  { super.onResume();  uiHandler.post(ticker); refreshUI(); }
    @Override protected void onPause()   { super.onPause();   uiHandler.removeCallbacks(ticker); }
    @Override protected void onDestroy() { super.onDestroy(); ScanResult.getInstance().setListener(null); }

    private void bindViews() {
        cardAccessibility = findViewById(R.id.cardAccessibility);
        cardContacts      = findViewById(R.id.cardContacts);
        layoutStats       = findViewById(R.id.layoutStats);
        tvSaved           = findViewById(R.id.tvSavedCount);
        tvUnsaved         = findViewById(R.id.tvUnsavedCount);
        tvLive            = findViewById(R.id.tvStatus);
        layoutReady       = findViewById(R.id.layoutReady);
        btnAutoScroll     = findViewById(R.id.btnAutoScroll);
        btnStopScroll     = findViewById(R.id.btnStopScroll);
        tvScrollStatus    = findViewById(R.id.tvScrollStatus);
        layoutActions     = findViewById(R.id.layoutActions);
        tvListHeader      = findViewById(R.id.tvListHeader);
        listView          = findViewById(R.id.listUnsaved);
        tvEmpty           = findViewById(R.id.tvEmpty);
        btnSaveAll        = findViewById(R.id.btnSaveAll);
        btnExportVcf      = findViewById(R.id.btnExportVcf);
        btnExportTxt      = findViewById(R.id.btnExportTxt);
        btnClear          = findViewById(R.id.btnClearScan);
    }

    private void setupButtons() {
        findViewById(R.id.btnAccessibility).setOnClickListener(v -> openAccessibility());
        findViewById(R.id.btnContacts).setOnClickListener(v ->
                requestPermissions(new String[]{
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS}, REQ_CONTACTS));

        btnAutoScroll.setOnClickListener(v -> {
            WhatsAppScannerService svc = WhatsAppScannerService.getInstance();
            if (svc == null) {
                Toast.makeText(this,
                        "Please open WhatsApp first, then come back and tap this button.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            // Show instructions then start
            new AlertDialog.Builder(this)
                    .setTitle("Auto Scroll & Scan")
                    .setMessage("The app will now switch to WhatsApp and automatically scroll through all your chats.\n\n"
                              + "• Keep WhatsApp open on the main chat list\n"
                              + "• Don't touch the screen while scrolling\n"
                              + "• It scans ~600 chats automatically\n\n"
                              + "Ready?")
                    .setPositiveButton("Start", (d, w) -> {
                        // Open WhatsApp then start scrolling
                        Intent wa = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                        if (wa == null) wa = getPackageManager().getLaunchIntentForPackage("com.whatsapp.w4b");
                        if (wa != null) startActivity(wa);
                        // Delay scroll start to let WhatsApp open
                        uiHandler.postDelayed(() -> {
                            WhatsAppScannerService s = WhatsAppScannerService.getInstance();
                            if (s != null) s.startAutoScroll();
                        }, 2000);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnStopScroll.setOnClickListener(v -> {
            WhatsAppScannerService svc = WhatsAppScannerService.getInstance();
            if (svc != null) svc.stopAutoScroll();
        });

        btnSaveAll.setOnClickListener(v -> {
            if (ScanResult.getInstance().getUnsavedCount() == 0) {
                Toast.makeText(this, "No unsaved numbers found yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivityForResult(new Intent(this, SaveContactsActivity.class), REQ_SAVE);
        });

        btnExportVcf.setOnClickListener(v -> doExport(true));
        btnExportTxt.setOnClickListener(v -> doExport(false));

        btnClear.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Clear Results?")
                        .setMessage("Remove all detected numbers from this list?")
                        .setPositiveButton("Clear", (d, w) -> ScanResult.getInstance().clear())
                        .setNegativeButton("Cancel", null).show());
    }

    private void refreshUI() {
        boolean accOk  = isAccessibilityEnabled();
        boolean ctxOk  = hasContacts();
        boolean ready  = accOk && ctxOk;

        ScanResult scan  = ScanResult.getInstance();
        int unsaved      = scan.getUnsavedCount();
        int saved        = scan.getSavedContactsCount();
        boolean scrolling = scan.isAutoScrolling();
        boolean hasData  = unsaved > 0;

        // Setup cards
        cardAccessibility.setVisibility(accOk ? View.GONE : View.VISIBLE);
        cardContacts.setVisibility(ctxOk ? View.GONE : View.VISIBLE);

        if (!ready) {
            layoutStats.setVisibility(View.GONE);
            layoutReady.setVisibility(View.GONE);
            layoutActions.setVisibility(View.GONE);
            tvListHeader.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            return;
        }

        // Stats
        layoutStats.setVisibility(View.VISIBLE);
        tvSaved.setText(String.valueOf(saved));
        tvUnsaved.setText(String.valueOf(unsaved));
        tvLive.setVisibility(View.VISIBLE);

        // Ready / scroll controls
        layoutReady.setVisibility(View.VISIBLE);
        btnAutoScroll.setVisibility(scrolling ? View.GONE : View.VISIBLE);
        btnStopScroll.setVisibility(scrolling ? View.VISIBLE : View.GONE);
        tvScrollStatus.setVisibility(scrolling ? View.VISIBLE : View.GONE);
        if (scrolling) tvScrollStatus.setText("⏳ Auto-scrolling WhatsApp... " + unsaved + " found so far");

        // Results
        if (hasData) {
            tvListHeader.setVisibility(View.VISIBLE);
            tvListHeader.setText("Unsaved Numbers Found (" + unsaved + "):");
            listView.setVisibility(View.VISIBLE);
            layoutActions.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            adapter.refresh(scan.getUnsavedNumbers());
            setListHeight(listView);
        } else {
            tvListHeader.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
            layoutActions.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(scrolling
                    ? "Scanning... numbers will appear here"
                    : "Open WhatsApp and scroll chats\n— or tap Auto Scroll below —");
        }
    }

    @Override public void onScanUpdated() { runOnUiThread(this::refreshUI); }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private boolean isAccessibilityEnabled() {
        try {
            String enabled = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabled != null && enabled.contains(
                    getPackageName() + "/" + WhatsAppScannerService.class.getName());
        } catch (Exception e) { return false; }
    }

    private boolean hasContacts() {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void openAccessibility() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Accessibility")
                .setMessage("1. Tap 'Installed apps'\n2. Find 'WA Number Scanner'\n3. Toggle ON → Allow\n4. Come back here")
                .setPositiveButton("Open Settings", (d,w) ->
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton("Cancel", null).show();
    }

    private void doExport(boolean vcf) {
        List<String> nums = ScanResult.getInstance().getUnsavedNumbers();
        if (nums.isEmpty()) {
            Toast.makeText(this, "Nothing to export.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        // Ask for base name before export
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Base name (e.g. Client)");
        new AlertDialog.Builder(this)
                .setTitle("Export Name")
                .setView(et)
                .setPositiveButton("Export", (d,w) -> {
                    String base = et.getText().toString().trim();
                    if (base.isEmpty()) base = "Contact";
                    String path = vcf
                            ? FileExporter.exportVCF(this, nums, base, 1)
                            : FileExporter.exportTXT(this, nums, base, 1);
                    if (path != null) {
                        Toast.makeText(this, "✅ Saved to Downloads/WA_Scanner/", Toast.LENGTH_LONG).show();
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType(vcf ? "text/vcard" : "text/plain");
                        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + path));
                        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(share, "Share via"));
                    } else {
                        Toast.makeText(this, "Export failed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        refreshUI();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        refreshUI();
    }

    private void setListHeight(ListView lv) {
        if (lv.getAdapter() == null) return;
        int h = 0;
        for (int i = 0; i < lv.getAdapter().getCount(); i++) {
            View item = lv.getAdapter().getView(i, null, lv);
            item.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            h += item.getMeasuredHeight();
        }
        android.view.ViewGroup.LayoutParams p = lv.getLayoutParams();
        p.height = h + lv.getDividerHeight() * (lv.getAdapter().getCount() - 1);
        lv.setLayoutParams(p);
        lv.requestLayout();
    }
}
