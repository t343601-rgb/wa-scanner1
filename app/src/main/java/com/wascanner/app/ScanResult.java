package com.wascanner.app;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Singleton shared state between Service and UI.
 * Uses LinkedHashSet to preserve insertion order and avoid duplicates.
 */
public class ScanResult {

    private static ScanResult instance;

    private final LinkedHashSet<String> unsavedNumbers = new LinkedHashSet<>();
    private int savedContactsCount = 0;
    private boolean isScanning = false;
    private boolean isAutoScrolling = false;
    private OnScanUpdateListener listener;

    private ScanResult() {}

    public static synchronized ScanResult getInstance() {
        if (instance == null) instance = new ScanResult();
        return instance;
    }

    public synchronized boolean addUnsavedNumber(String number) {
        boolean added = unsavedNumbers.add(number);
        if (added && listener != null) listener.onScanUpdated();
        return added;
    }

    public synchronized List<String> getUnsavedNumbers() {
        return new ArrayList<>(unsavedNumbers);
    }

    public synchronized int getUnsavedCount() { return unsavedNumbers.size(); }

    public int getSavedContactsCount() { return savedContactsCount; }
    public void setSavedContactsCount(int count) { savedContactsCount = count; }

    public boolean isScanning() { return isScanning; }
    public void setScanning(boolean scanning) {
        isScanning = scanning;
        if (listener != null) listener.onScanUpdated();
    }

    public boolean isAutoScrolling() { return isAutoScrolling; }
    public void setAutoScrolling(boolean v) {
        isAutoScrolling = v;
        if (listener != null) listener.onScanUpdated();
    }

    public synchronized void clear() {
        unsavedNumbers.clear();
        isScanning = false;
        isAutoScrolling = false;
        if (listener != null) listener.onScanUpdated();
    }

    public void setListener(OnScanUpdateListener l) { this.listener = l; }

    public interface OnScanUpdateListener {
        void onScanUpdated();
    }
}
