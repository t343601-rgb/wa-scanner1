package com.wascanner.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SaveContactsActivity extends Activity {

    private EditText etName;
    private EditText etStart;
    private ProgressBar progressBar;
    private TextView tvProgress, tvCount, tvPreview;
    private Button btnSave, btnCancel;

    private List<String> numbers;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler main = new Handler(Looper.getMainLooper());
    private boolean saving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_contacts);

        etName     = findViewById(R.id.etBaseName);
        etStart    = findViewById(R.id.etStartIndex);
        progressBar= findViewById(R.id.progressSave);
        tvProgress = findViewById(R.id.tvSaveProgress);
        tvCount    = findViewById(R.id.tvCount);
        tvPreview  = findViewById(R.id.tvPreview);
        btnSave    = findViewById(R.id.btnStartSave);
        btnCancel  = findViewById(R.id.btnCancelSave);

        numbers = ScanResult.getInstance().getUnsavedNumbers();
        tvCount.setText(numbers.size() + " unsaved numbers ready to save");
        progressBar.setMax(numbers.size());

        // Live preview
        etName.addTextChangedListener(new android.text.TextWatcher() {
            public void afterTextChanged(android.text.Editable s) { updatePreview(); }
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
        });
        etStart.addTextChangedListener(new android.text.TextWatcher() {
            public void afterTextChanged(android.text.Editable s) { updatePreview(); }
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
        });

        btnSave.setOnClickListener(v -> startSaving());
        btnCancel.setOnClickListener(v -> { if (!saving) finish(); });
    }

    private void updatePreview() {
        String base = etName.getText().toString().trim();
        int start = getStartIndex();
        if (!base.isEmpty()) {
            int last = start + numbers.size() - 1;
            tvPreview.setText("Preview: " + base + " " + start
                    + "  →  " + base + " " + last);
        } else {
            tvPreview.setText("");
        }
    }

    private int getStartIndex() {
        try { return Integer.parseInt(etStart.getText().toString().trim()); }
        catch (Exception e) { return 1; }
    }

    private void startSaving() {
        String base = etName.getText().toString().trim();
        if (base.isEmpty()) {
            Toast.makeText(this, "Enter a base name first", Toast.LENGTH_SHORT).show();
            return;
        }
        int startIdx = getStartIndex();
        saving = true;
        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);

        executor.execute(() ->
            ContactsHelper.saveWithPattern(this, numbers, base, startIdx,
                new ContactsHelper.SaveCallback() {
                    @Override
                    public void onProgress(int cur, int total, String name, String num, boolean ok) {
                        main.post(() -> {
                            progressBar.setProgress(cur);
                            tvProgress.setText("Saving: " + name + "  (" + cur + "/" + total + ")");
                        });
                    }
                    @Override
                    public void onDone(int totalSaved) {
                        main.post(() -> {
                            saving = false;
                            // Refresh service contacts list
                            if (WhatsAppScannerService.getInstance() != null)
                                WhatsAppScannerService.getInstance().refreshContacts();

                            Toast.makeText(SaveContactsActivity.this,
                                    "✅ Saved " + totalSaved + " contacts!", Toast.LENGTH_LONG).show();
                            ScanResult.getInstance().clear();
                            setResult(RESULT_OK);
                            finish();
                        });
                    }
                })
        );
    }

    @Override public void onBackPressed() { if (!saving) super.onBackPressed(); }
    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }
}
