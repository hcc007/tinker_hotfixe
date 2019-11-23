package com.hc.flutter_local_hotfix.flutter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.flutter.embedding.engine.FlutterJNI;
import io.flutter.util.PathUtils;
import io.flutter.view.FlutterMain;
import io.flutter.view.VsyncWaiter;

public class MyFlutterMain {
    private static final String TAG = "hcc";
    private static final String AOT_SHARED_LIBRARY_NAME = "aot-shared-library-name";
    private static final String SNAPSHOT_ASSET_PATH_KEY = "snapshot-asset-path";
    private static final String VM_SNAPSHOT_DATA_KEY = "vm-snapshot-data";
    private static final String ISOLATE_SNAPSHOT_DATA_KEY = "isolate-snapshot-data";
    private static final String FLUTTER_ASSETS_DIR_KEY = "flutter-assets-dir";
    public static final String PUBLIC_AOT_SHARED_LIBRARY_NAME = FlutterMain.class.getName() + '.' + "aot-shared-library-name";
    public static final String PUBLIC_VM_SNAPSHOT_DATA_KEY = FlutterMain.class.getName() + '.' + "vm-snapshot-data";
    public static final String PUBLIC_ISOLATE_SNAPSHOT_DATA_KEY = FlutterMain.class.getName() + '.' + "isolate-snapshot-data";
    public static final String PUBLIC_FLUTTER_ASSETS_DIR_KEY = FlutterMain.class.getName() + '.' + "flutter-assets-dir";

    private static final String DEFAULT_AOT_SHARED_LIBRARY_NAME = "libapp.so";

    private static final String DEFAULT_VM_SNAPSHOT_DATA = "vm_snapshot_data";
    private static final String DEFAULT_ISOLATE_SNAPSHOT_DATA = "isolate_snapshot_data";
    private static final String DEFAULT_LIBRARY = "libflutter.so";
    private static final String DEFAULT_KERNEL_BLOB = "kernel_blob.bin";
    private static final String DEFAULT_FLUTTER_ASSETS_DIR = "flutter_assets";
    private static boolean isRunningInRobolectricTest = false;
    private static String sAotSharedLibraryName = DEFAULT_AOT_SHARED_LIBRARY_NAME;
    private static String sVmSnapshotData = "vm_snapshot_data";
    private static String sIsolateSnapshotData = "isolate_snapshot_data";
    private static String sFlutterAssetsDir = "flutter_assets";
    private static boolean sInitialized = false;


    ///初始化一次之后，是否还有效的问题，多线程多进程问题。
    public volatile static String fix_app_lib_path ;

    public  volatile  static String fix_app_lib_name = DEFAULT_AOT_SHARED_LIBRARY_NAME;


    public static void setFix_app_lib_name(String name){
        fix_app_lib_name = name;
        Log.i("hcc", "setFix_app_lib_name: 设置动态库名称成功: " + fix_app_lib_name);
    }

    public static void setFixAppLibPath(String libPath){
        fix_app_lib_path = libPath;
        Log.i("hcc", "setFixAppLibPath: 设置动态库路径成功:  " + fix_app_lib_path);
    }


    @Nullable
    private static ResourceExtractor sResourceExtractor;
    @Nullable
    private static FlutterMain.Settings sSettings;

    public MyFlutterMain() {
    }

    @VisibleForTesting
    public static void setIsRunningInRobolectricTest(boolean isRunningInRobolectricTest) {
        isRunningInRobolectricTest = isRunningInRobolectricTest;
    }

    @NonNull
    private static String fromFlutterAssets(@NonNull String filePath) {
        return sFlutterAssetsDir + File.separator + filePath;
    }

    public static void startInitialization(@NonNull Context applicationContext) {
        if (!isRunningInRobolectricTest) {
            startInitialization(applicationContext, new FlutterMain.Settings());
        }
    }

    public static void startInitialization(@NonNull Context applicationContext, @NonNull FlutterMain.Settings settings) {
        if (!isRunningInRobolectricTest) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException("startInitialization must be called on the main thread");
            } else if (sSettings == null) {
                sSettings = settings;
                long initStartTimestampMillis = SystemClock.uptimeMillis();
                initConfig(applicationContext);
                initResources(applicationContext);
                System.loadLibrary("flutter");
                VsyncWaiter.getInstance((WindowManager)applicationContext.getSystemService(Context.WINDOW_SERVICE)).init();
                long initTimeMillis = SystemClock.uptimeMillis() - initStartTimestampMillis;
                FlutterJNI.nativeRecordStartTimestamp(initTimeMillis);
            }
        }
    }

    public static void ensureInitializationComplete(@NonNull Context applicationContext, @Nullable String[] args) {

        Toast.makeText(applicationContext,"当前 Lib 名字是：" + DEFAULT_AOT_SHARED_LIBRARY_NAME,Toast.LENGTH_SHORT).show();

        if (!isRunningInRobolectricTest) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException("ensureInitializationComplete must be called on the main thread");
            } else if (sSettings == null) {
                throw new IllegalStateException("ensureInitializationComplete must be called after startInitialization");
            } else if (!sInitialized) {
                try {
                    if (sResourceExtractor != null) {
                        sResourceExtractor.waitForCompletion();
                    }

                    List<String> shellArgs = new ArrayList();
                    shellArgs.add("--icu-symbol-prefix=_binary_icudtl_dat");
                    ApplicationInfo applicationInfo = getApplicationInfo(applicationContext);
                    shellArgs.add("--icu-native-lib-path=" + applicationInfo.nativeLibraryDir + File.separator + "libflutter.so");
                    if (args != null) {
                        Collections.addAll(shellArgs, args);
                    }

                    String kernelPath = null;

                  //  if(fix_app_lib_name != null && )

                    // shellArgs.add("--aot-shared-library-name=" + sAotSharedLibraryName);

                   // shellArgs.add("--aot-shared-library-name=" + applicationInfo.nativeLibraryDir + File.separator + sAotSharedLibraryName);


                    Log.i("hcc", "ensureInitializationComplete，flutter 原先加载 lib 的目录是:" + applicationInfo.nativeLibraryDir + File.separator + sAotSharedLibraryName);

                    if(fix_app_lib_path != null && fix_app_lib_path != ""){
                        if(new File(fix_app_lib_path).exists()){
                            Log.i("hcc", "ensureInitializationComplete: 热修复的资源名称存在,路径是:" + fix_app_lib_path);
                          //  shellArgs.add("--aot-shared-library-name=" + fix_app_lib_name);
                            shellArgs.add("--aot-shared-library-name=" + fix_app_lib_path);

                        }else {
                            Log.i("hcc", "ensureInitializationComplete: 热修复路径不存在 ");
                            shellArgs.add("--aot-shared-library-name=" + sAotSharedLibraryName);
                            shellArgs.add("--aot-shared-library-name=" + applicationInfo.nativeLibraryDir + File.separator + sAotSharedLibraryName);
                        }
                    }else {

                        Log.i("hcc", "ensureInitializationComplete: 热修复路径为空，或者没有设置值 ");

                        shellArgs.add("--aot-shared-library-name=" + sAotSharedLibraryName);
                        shellArgs.add("--aot-shared-library-name=" + applicationInfo.nativeLibraryDir + File.separator + sAotSharedLibraryName);
                    }


                    shellArgs.add("--cache-dir-path=" + PathUtils.getCacheDirectory(applicationContext));



                    if (sSettings.getLogTag() != null) {
                        shellArgs.add("--log-tag=" + sSettings.getLogTag());
                    }

                    String appStoragePath = PathUtils.getFilesDir(applicationContext);
                    String engineCachesPath = PathUtils.getCacheDirectory(applicationContext);
                    FlutterJNI.nativeInit(applicationContext, (String[])shellArgs.toArray(new String[0]), (String)kernelPath, appStoragePath, engineCachesPath);
                    sInitialized = true;
                } catch (Exception var7) {
                    Log.e("FlutterMain", "Flutter initialization failed.", var7);
                    throw new RuntimeException(var7);
                }
            }
        }
    }






    public static void reEnsureInitializationComplete(@NonNull Context applicationContext, @Nullable String[] args) {

        Toast.makeText(applicationContext,"当前 Lib 名字是：" + DEFAULT_AOT_SHARED_LIBRARY_NAME,Toast.LENGTH_SHORT).show();

        if (!isRunningInRobolectricTest) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException("ensureInitializationComplete must be called on the main thread");
            } else if (sSettings == null) {
                throw new IllegalStateException("ensureInitializationComplete must be called after startInitialization");
            } else  {
                try {
                    if (sResourceExtractor != null) {
                        sResourceExtractor.waitForCompletion();
                    }

                    List<String> shellArgs = new ArrayList();
                    shellArgs.add("--icu-symbol-prefix=_binary_icudtl_dat");
                    ApplicationInfo applicationInfo = getApplicationInfo(applicationContext);
                    shellArgs.add("--icu-native-lib-path=" + applicationInfo.nativeLibraryDir + File.separator + "libflutter.so");
                    if (args != null) {
                        Collections.addAll(shellArgs, args);
                    }

                    String kernelPath = null;

                    //  if(fix_app_lib_name != null && )

                    // shellArgs.add("--aot-shared-library-name=" + sAotSharedLibraryName);

                    // shellArgs.add("--aot-shared-library-name=" + applicationInfo.nativeLibraryDir + File.separator + sAotSharedLibraryName);


                    Log.i("hcc", "ensureInitializationComplete，flutter 原先加载 lib 的目录是:" + applicationInfo.nativeLibraryDir + File.separator + sAotSharedLibraryName);

                    if(fix_app_lib_path != null && fix_app_lib_path != ""){
                        if(new File(fix_app_lib_path).exists()){
                            Log.i("hcc", "ensureInitializationComplete: 热修复的资源名称存在,路径是:" + fix_app_lib_path);
                            shellArgs.add("--aot-shared-library-name=" + fix_app_lib_name);
                            shellArgs.add("--aot-shared-library-name=" + fix_app_lib_path);

                        }else {
                            Log.i("hcc", "ensureInitializationComplete: 热修复路径不存在 ");
                            shellArgs.add("--aot-shared-library-name=" + sAotSharedLibraryName);
                            shellArgs.add("--aot-shared-library-name=" + applicationInfo.nativeLibraryDir + File.separator + sAotSharedLibraryName);
                        }
                    }else {

                        Log.i("hcc", "ensureInitializationComplete: 热修复路径为空，或者没有设置值 ");

                        shellArgs.add("--aot-shared-library-name=" + sAotSharedLibraryName);
                        shellArgs.add("--aot-shared-library-name=" + applicationInfo.nativeLibraryDir + File.separator + sAotSharedLibraryName);
                    }


                    shellArgs.add("--cache-dir-path=" + PathUtils.getCacheDirectory(applicationContext));



                    if (sSettings.getLogTag() != null) {
                        shellArgs.add("--log-tag=" + sSettings.getLogTag());
                    }

                    String appStoragePath = PathUtils.getFilesDir(applicationContext);
                    String engineCachesPath = PathUtils.getCacheDirectory(applicationContext);
                    FlutterJNI.nativeInit(applicationContext, (String[])shellArgs.toArray(new String[0]), (String)kernelPath, appStoragePath, engineCachesPath);
                    sInitialized = true;
                } catch (Exception var7) {
                    Log.e("FlutterMain", "Flutter initialization failed.", var7);
                    throw new RuntimeException(var7);
                }
            }
        }
    }






    public static void ensureInitializationCompleteAsync(@NonNull final Context applicationContext, @Nullable final String[] args, @NonNull final Handler callbackHandler, @NonNull final Runnable callback) {
        if (!isRunningInRobolectricTest) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException("ensureInitializationComplete must be called on the main thread");
            } else if (sSettings == null) {
                throw new IllegalStateException("ensureInitializationComplete must be called after startInitialization");
            } else if (!sInitialized) {
                (new Thread(new Runnable() {
                    public void run() {
                        if (MyFlutterMain.sResourceExtractor != null) {
                            MyFlutterMain.sResourceExtractor.waitForCompletion();
                        }

                        (new Handler(Looper.getMainLooper())).post(new Runnable() {
                            public void run() {
                                FlutterMain.ensureInitializationComplete(applicationContext.getApplicationContext(), args);
                                callbackHandler.post(callback);
                            }
                        });
                    }
                })).start();
            }
        }
    }


    @SuppressLint("WrongConstant")
    @NonNull
    private static ApplicationInfo getApplicationInfo(@NonNull Context applicationContext) {
        try {
            return applicationContext.getPackageManager().getApplicationInfo(applicationContext.getPackageName(), 128);
        } catch (PackageManager.NameNotFoundException var2) {
            throw new RuntimeException(var2);
        }
    }

    private static void initConfig(@NonNull Context applicationContext) {
        Bundle metadata = getApplicationInfo(applicationContext).metaData;
        if (metadata != null) {
            sAotSharedLibraryName = metadata.getString(PUBLIC_AOT_SHARED_LIBRARY_NAME, DEFAULT_AOT_SHARED_LIBRARY_NAME);
            sFlutterAssetsDir = metadata.getString(PUBLIC_FLUTTER_ASSETS_DIR_KEY, "flutter_assets");
            sVmSnapshotData = metadata.getString(PUBLIC_VM_SNAPSHOT_DATA_KEY, "vm_snapshot_data");
            sIsolateSnapshotData = metadata.getString(PUBLIC_ISOLATE_SNAPSHOT_DATA_KEY, "isolate_snapshot_data");
        }
    }

    private static void initResources(@NonNull Context applicationContext) {
        (new ResourceCleaner(applicationContext)).start();
    }

    @NonNull
    public static String findAppBundlePath() {
        return sFlutterAssetsDir;
    }

    /** @deprecated */
    @Deprecated
    @Nullable
    public static String findAppBundlePath(@NonNull Context applicationContext) {
        return sFlutterAssetsDir;
    }

    @NonNull
    public static String getLookupKeyForAsset(@NonNull String asset) {
        return fromFlutterAssets(asset);
    }

    @NonNull
    public static String getLookupKeyForAsset(@NonNull String asset, @NonNull String packageName) {
        return getLookupKeyForAsset("packages" + File.separator + packageName + File.separator + asset);
    }
}
