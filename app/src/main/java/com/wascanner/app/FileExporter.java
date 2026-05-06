package com.wascanner.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileExporter {

    private static final String TAG = "FileExporter";
    private static final String DIR = "WA_Scanner";

    public static String exportVCF(Context ctx, List<String> numbers, String baseName, int startIndex) {
        String content = ContactsHelper.buildVCF(numbers, baseName, startIndex);
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return write(ctx, "WA_Unsaved_" + ts + ".vcf", content);
    }

    public static String exportTXT(Context ctx, List<String> numbers, String baseName, int startIndex) {
        String content = ContactsHelper.buildTXT(numbers, baseName, startIndex);
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return write(ctx, "WA_Unsaved_" + ts + ".txt", content);
    }

    private static String write(Context ctx, String fileName, String content) {
        try {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIR);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, fileName);
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();

            ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            Log.i(TAG, "Exported: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return null;
        }
    }
}
