package com.hc.flutter_local_hotfix.flutter;


import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

class ResourceExtractor {
    private static final String TAG = "ResourceExtractor";
    private static final String TIMESTAMP_PREFIX = "res_timestamp-";
    private static final String[] SUPPORTED_ABIS = getSupportedAbis();
    @NonNull
    private final String mDataDirPath;
    @NonNull
    private final String mPackageName;
    @NonNull
    private final PackageManager mPackageManager;
    @NonNull
    private final AssetManager mAssetManager;
    @NonNull
    private final HashSet<String> mResources;
    private ExtractTask mExtractTask;

    static long getVersionCode(@NonNull PackageInfo packageInfo) {
        return VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : (long)packageInfo.versionCode;
    }

    ResourceExtractor(@NonNull String dataDirPath, @NonNull String packageName, @NonNull PackageManager packageManager, @NonNull AssetManager assetManager) {
        this.mDataDirPath = dataDirPath;
        this.mPackageName = packageName;
        this.mPackageManager = packageManager;
        this.mAssetManager = assetManager;
        this.mResources = new HashSet();
    }

   ResourceExtractor addResource(@NonNull String resource) {
        this.mResources.add(resource);
        return this;
    }
        ResourceExtractor addResources(@NonNull Collection<String> resources) {
        this.mResources.addAll(resources);
        return this;
    }

     ResourceExtractor start() {
        if (this.mExtractTask != null) {
            Log.e("ResourceExtractor", "Attempted to start resource extraction while another extraction was in progress.");
        }

        this.mExtractTask = new ExtractTask(this.mDataDirPath, this.mResources, this.mPackageName, this.mPackageManager, this.mAssetManager);
        this.mExtractTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        return this;
    }

    void waitForCompletion() {
        if (this.mExtractTask != null) {
            try {
                this.mExtractTask.get();
            } catch (ExecutionException | InterruptedException | CancellationException var2) {
                deleteFiles(this.mDataDirPath, this.mResources);
            }

        }
    }

    private static String[] getExistingTimestamps(File dataDir) {
        return dataDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("res_timestamp-");
            }
        });
    }

    private static void deleteFiles(@NonNull String dataDirPath, @NonNull HashSet<String> resources) {
        File dataDir = new File(dataDirPath);
        Iterator var3 = resources.iterator();

        while(var3.hasNext()) {
            String resource = (String)var3.next();
            File file = new File(dataDir, resource);
            if (file.exists()) {
                file.delete();
            }
        }

        String[] existingTimestamps = getExistingTimestamps(dataDir);
        if (existingTimestamps != null) {
            String[] var9 = existingTimestamps;
            int var10 = existingTimestamps.length;

            for(int var6 = 0; var6 < var10; ++var6) {
                String timestamp = var9[var6];
                (new File(dataDir, timestamp)).delete();
            }

        }
    }

    private static String checkTimestamp(@NonNull File dataDir, @NonNull PackageManager packageManager, @NonNull String packageName) {
        PackageInfo packageInfo = null;

        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
        } catch (NameNotFoundException var6) {
            return "res_timestamp-";
        }

        if (packageInfo == null) {
            return "res_timestamp-";
        } else {
            String expectedTimestamp = "res_timestamp-" + getVersionCode(packageInfo) + "-" + packageInfo.lastUpdateTime;
            String[] existingTimestamps = getExistingTimestamps(dataDir);
            if (existingTimestamps == null) {
                Log.i("ResourceExtractor", "No extracted resources found");
                return expectedTimestamp;
            } else {
                if (existingTimestamps.length == 1) {
                    Log.i("ResourceExtractor", "Found extracted resources " + existingTimestamps[0]);
                }

                if (existingTimestamps.length == 1 && expectedTimestamp.equals(existingTimestamps[0])) {
                    return null;
                } else {
                    Log.i("ResourceExtractor", "Resource version mismatch " + expectedTimestamp);
                    return expectedTimestamp;
                }
            }
        }
    }

    private static void copy(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        byte[] buf = new byte[16384];

        int i;
        while((i = in.read(buf)) >= 0) {
            out.write(buf, 0, i);
        }

    }

    private static String[] getSupportedAbis() {
        if (VERSION.SDK_INT >= 21) {
            return Build.SUPPORTED_ABIS;
        } else {
            ArrayList<String> cpuAbis = new ArrayList(Arrays.asList(Build.CPU_ABI, Build.CPU_ABI2));
            cpuAbis.removeAll(Arrays.asList(null, ""));
            return (String[])cpuAbis.toArray(new String[0]);
        }
    }

    private static class ExtractTask extends AsyncTask<Void, Void, Void> {
        @NonNull
        private final String mDataDirPath;
        @NonNull
        private final HashSet<String> mResources;
        @NonNull
        private final AssetManager mAssetManager;
        @NonNull
        private final String mPackageName;
        @NonNull
        private final PackageManager mPackageManager;

        ExtractTask(@NonNull String dataDirPath, @NonNull HashSet<String> resources, @NonNull String packageName, @NonNull PackageManager packageManager, @NonNull AssetManager assetManager) {
            this.mDataDirPath = dataDirPath;
            this.mResources = resources;
            this.mAssetManager = assetManager;
            this.mPackageName = packageName;
            this.mPackageManager = packageManager;
        }

        protected Void doInBackground(Void... unused) {
            File dataDir = new File(this.mDataDirPath);
            String timestamp = ResourceExtractor.checkTimestamp(dataDir, this.mPackageManager, this.mPackageName);
            if (timestamp == null) {
                return null;
            } else {
                ResourceExtractor.deleteFiles(this.mDataDirPath, this.mResources);
                if (!this.extractAPK(dataDir)) {
                    return null;
                } else {
                    if (timestamp != null) {
                        try {
                            (new File(dataDir, timestamp)).createNewFile();
                        } catch (IOException var5) {
                            Log.w("ResourceExtractor", "Failed to write resource timestamp");
                        }
                    }

                    return null;
                }
            }
        }

        @WorkerThread
        private boolean extractAPK(@NonNull File dataDir) {
            Iterator var2 = this.mResources.iterator();

            while(var2.hasNext()) {
                String asset = (String)var2.next();

                try {
                    String resource = "assets/" + asset;
                    File output = new File(dataDir, asset);
                    if (!output.exists()) {
                        if (output.getParentFile() != null) {
                            output.getParentFile().mkdirs();
                        }

                        InputStream is = this.mAssetManager.open(asset);
                        Throwable var7 = null;

                        try {
                            OutputStream os = new FileOutputStream(output);
                            Throwable var9 = null;

                            try {
                               ResourceExtractor.copy(is, os);
                            } catch (Throwable var36) {
                                var9 = var36;
                                throw var36;
                            } finally {
                                if (os != null) {
                                    if (var9 != null) {
                                        try {
                                            os.close();
                                        } catch (Throwable var35) {
                                            var9.addSuppressed(var35);
                                        }
                                    } else {
                                        os.close();
                                    }
                                }

                            }
                        } catch (Throwable var38) {
                            var7 = var38;
                            throw var38;
                        } finally {
                            if (is != null) {
                                if (var7 != null) {
                                    try {
                                        is.close();
                                    } catch (Throwable var34) {
                                        var7.addSuppressed(var34);
                                    }
                                } else {
                                    is.close();
                                }
                            }

                        }

                        Log.i("ResourceExtractor", "Extracted baseline resource " + resource);
                    }
                } catch (FileNotFoundException var40) {
                } catch (IOException var41) {
                    Log.w("ResourceExtractor", "Exception unpacking resources: " + var41.getMessage());
                    ResourceExtractor.deleteFiles(this.mDataDirPath, this.mResources);
                    return false;
                }
            }

            return true;
        }
    }
}
