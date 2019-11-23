package com.hc.flutter_local_hotfix.flutter;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

class ResourceCleaner {
    private static final String TAG = "ResourceCleaner";
    private static final long DELAY_MS = 5000L;
    private final Context mContext;

    ResourceCleaner(Context context) {
        this.mContext = context;
    }

    void start() {
        File cacheDir = this.mContext.getCacheDir();
        if (cacheDir != null) {
            final CleanTask task = new CleanTask(cacheDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    boolean result = name.startsWith(".org.chromium.Chromium.");
                    return result;
                }
            }));
            if (task.hasFilesToDelete()) {
                (new Handler()).postDelayed(new Runnable() {
                    public void run() {
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                    }
                }, 5000L);
            }
        }
    }

    private static class CleanTask extends AsyncTask<Void, Void, Void> {
        private final File[] mFilesToDelete;

        CleanTask(File[] filesToDelete) {
            this.mFilesToDelete = filesToDelete;
        }

        boolean hasFilesToDelete() {
            return this.mFilesToDelete != null && this.mFilesToDelete.length > 0;
        }

        protected Void doInBackground(Void... unused) {
            Log.i("ResourceCleaner", "Cleaning " + this.mFilesToDelete.length + " resources.");
            File[] var2 = this.mFilesToDelete;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                File file = var2[var4];
                if (file.exists()) {
                    this.deleteRecursively(file);
                }
            }

            return null;
        }

        private void deleteRecursively(File parent) {
            if (parent.isDirectory()) {
                File[] var2 = parent.listFiles();
                int var3 = var2.length;

                for(int var4 = 0; var4 < var3; ++var4) {
                    File child = var2[var4];
                    this.deleteRecursively(child);
                }
            }

            parent.delete();
        }
    }
}
