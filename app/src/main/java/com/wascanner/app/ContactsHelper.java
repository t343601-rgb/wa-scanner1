package com.wascanner.app;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactsHelper {

    private static final String TAG = "ContactsHelper";

    /** Load all saved phone numbers as digit-only strings for fast lookup. */
    public static Set<String> loadSavedNumbers(Context ctx) {
        Set<String> saved = new HashSet<>();
        try (Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null)) {
            if (c != null) {
                int col = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (c.moveToNext()) {
                    String num = c.getString(col);
                    if (num != null) {
                        String d = digitsOnly(num);
                        saved.add(d);
                        if (d.length() >= 10) saved.add(d.substring(d.length() - 10));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadSavedNumbers error", e);
        }
        return saved;
    }

    public static boolean isNumberSaved(String number, Set<String> saved) {
        if (number == null || saved == null) return false;
        String d = digitsOnly(number);
        if (saved.contains(d)) return true;
        if (d.length() >= 10) return saved.contains(d.substring(d.length() - 10));
        return false;
    }

    /**
     * Save numbers with pattern: baseName + " " + sequential index
     * e.g.  baseName="Client"  →  Client 1, Client 2 ... Client 150
     *
     * @param startIndex  starting counter (1-based, usually 1)
     */
    public static void saveWithPattern(
            Context ctx,
            List<String> numbers,
            String baseName,
            int startIndex,
            SaveCallback cb) {

        int total = numbers.size();
        int savedOk = 0;

        for (int i = 0; i < total; i++) {
            String name = baseName.trim() + " " + (startIndex + i);
            boolean ok = saveOne(ctx, name, numbers.get(i));
            if (ok) savedOk++;
            if (cb != null) cb.onProgress(i + 1, total, name, numbers.get(i), ok);
        }
        if (cb != null) cb.onDone(savedOk);
    }

    public static boolean saveOne(Context ctx, String name, String phone) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name).build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build());
        try {
            ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "saveOne failed: " + name, e);
            return false;
        }
    }

    public static String buildVCF(List<String> numbers, String baseName, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numbers.size(); i++) {
            String name = baseName.trim() + " " + (startIndex + i);
            sb.append("BEGIN:VCARD\nVERSION:3.0\n")
              .append("FN:").append(name).append("\n")
              .append("TEL;TYPE=CELL:").append(numbers.get(i)).append("\n")
              .append("END:VCARD\n\n");
        }
        return sb.toString();
    }

    public static String buildTXT(List<String> numbers, String baseName, int startIndex) {
        StringBuilder sb = new StringBuilder("WA Scanner Export\n=================\n\n");
        for (int i = 0; i < numbers.size(); i++) {
            sb.append(baseName.trim()).append(" ").append(startIndex + i)
              .append(": ").append(numbers.get(i)).append("\n");
        }
        return sb.toString();
    }

    private static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    public interface SaveCallback {
        void onProgress(int current, int total, String name, String number, boolean ok);
        void onDone(int totalSaved);
    }
}
