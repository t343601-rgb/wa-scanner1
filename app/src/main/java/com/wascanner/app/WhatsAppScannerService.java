package com.wascanner.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Core Accessibility Service.
 *
 * TWO MODES:
 *  1. PASSIVE  — Detects numbers as user scrolls WhatsApp manually
 *  2. AUTO-SCROLL — On command, automatically scrolls the WhatsApp chat list
 *                   down repeatedly, scanning every batch of contacts
 *
 * PRIVACY: Only reads chat list contact NAMES shown on screen.
 *          Does NOT read any message content.
 */
public class WhatsAppScannerService extends AccessibilityService {

    private static final String TAG = "WAScanner";
    private static final String WA = "com.whatsapp";
    private static final String WAB = "com.whatsapp.w4b";

    // Auto-scroll settings
    private static final int SCROLL_INTERVAL_MS = 1200; // ms between each scroll
    private static final int SCROLL_REPEATS = 120;      // max scrolls (covers ~600 chats)

    private Set<String> savedNumbers = new HashSet<>();
    private Set<String> seenThisSession = new HashSet<>();
    private boolean contactsLoaded = false;

    private Handler scrollHandler = new Handler(Looper.getMainLooper());
    private int scrollCount = 0;
    private boolean autoScrollActive = false;

    // Static reference so MainActivity can trigger auto-scroll
    private static WhatsAppScannerService instance;

    public static WhatsAppScannerService getInstance() { return instance; }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "Service connected");
        ScanResult.getInstance().setScanning(true);
        loadContacts();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        String pkg = String.valueOf(event.getPackageName());
        if (!WA.equals(pkg) && !WAB.equals(pkg)) return;

        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {

            if (!contactsLoaded) loadContacts();

            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                scanTree(root);
                root.recycle();
            }
        }
    }

    private void scanTree(AccessibilityNodeInfo node) {
        if (node == null) return;
        processText(node.getText());
        processText(node.getContentDescription());
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            scanTree(child);
            if (child != null) child.recycle();
        }
    }

    private void processText(CharSequence raw) {
        if (raw == null || raw.length() < 6) return;
        String text = raw.toString().trim();
        if (!PhoneNumberDetector.isPhoneNumberText(text)) return;

        String number = PhoneNumberDetector.normalize(text);
        if (number == null) return;
        if (seenThisSession.contains(number)) return;
        seenThisSession.add(number);

        if (!ContactsHelper.isNumberSaved(number, savedNumbers)) {
            ScanResult.getInstance().addUnsavedNumber(number);
            Log.d(TAG, "Unsaved: " + number);
        }
    }

    // ─── AUTO SCROLL ─────────────────────────────────────────────────────────

    /** Called from MainActivity when user taps "Auto Scroll & Scan". */
    public void startAutoScroll() {
        if (autoScrollActive) return;
        autoScrollActive = true;
        scrollCount = 0;
        ScanResult.getInstance().setAutoScrolling(true);
        Log.i(TAG, "Auto-scroll started");
        scheduleNextScroll();
    }

    public void stopAutoScroll() {
        autoScrollActive = false;
        scrollHandler.removeCallbacksAndMessages(null);
        ScanResult.getInstance().setAutoScrolling(false);
        Log.i(TAG, "Auto-scroll stopped");
    }

    private void scheduleNextScroll() {
        scrollHandler.postDelayed(() -> {
            if (!autoScrollActive) return;
            if (scrollCount >= SCROLL_REPEATS) {
                stopAutoScroll();
                return;
            }
            performScroll();
            scrollCount++;
            scheduleNextScroll();
        }, SCROLL_INTERVAL_MS);
    }

    private void performScroll() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use gesture: swipe up on screen to scroll list down
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int screenH = getResources().getDisplayMetrics().heightPixels;

            float startX = screenW / 2f;
            float startY = screenH * 0.7f;
            float endY   = screenH * 0.25f;

            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(startX, endY);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
            dispatchGesture(builder.build(), null, null);
        } else {
            // Fallback for Android < 7: try scroll on root node
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                root.recycle();
            }
        }
    }

    private void loadContacts() {
        try {
            savedNumbers = ContactsHelper.loadSavedNumbers(this);
            ScanResult.getInstance().setSavedContactsCount(savedNumbers.size());
            contactsLoaded = true;
            Log.i(TAG, "Loaded " + savedNumbers.size() + " saved numbers");
        } catch (Exception e) {
            Log.e(TAG, "loadContacts error", e);
        }
    }

    /** Call after saving new contacts to refresh the saved list. */
    public void refreshContacts() {
        contactsLoaded = false;
        seenThisSession.clear();
        loadContacts();
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "Service interrupted");
        stopAutoScroll();
        ScanResult.getInstance().setScanning(false);
        instance = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAutoScroll();
        ScanResult.getInstance().setScanning(false);
        instance = null;
    }
}
