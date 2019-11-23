package com.hc.flutter_local_hotfix.flutter;

/**
 * Time: 2019/11/18
 * Author: wanghaichao
 * Description:
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;


import com.hc.flutter_local_hotfix.BuildConfig;

import java.util.ArrayList;

import io.flutter.app.FlutterActivityEvents;
import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.util.Preconditions;
import io.flutter.view.FlutterMain;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.FlutterRunArguments;
import io.flutter.view.FlutterView;

public class HotFixFlutterActivityDelegate implements FlutterActivityEvents, FlutterView.Provider, PluginRegistry {
    private static final String SPLASH_SCREEN_META_DATA_KEY = "io.flutter.app.android.SplashScreenUntilFirstFrame";
    private static final String TAG = "hcc";
    private static final WindowManager.LayoutParams matchParent = new WindowManager.LayoutParams(-1, -1);
    private final Activity activity;
    private final HotFixFlutterActivityDelegate.ViewFactory viewFactory;
    private FlutterView flutterView;
    private View launchView;

    public HotFixFlutterActivityDelegate(Activity activity, HotFixFlutterActivityDelegate.ViewFactory viewFactory) {
        this.activity = (Activity) Preconditions.checkNotNull(activity);
        this.viewFactory = (HotFixFlutterActivityDelegate.ViewFactory) Preconditions.checkNotNull(viewFactory);
    }

    @Override
    public FlutterView getFlutterView() {
        return this.flutterView;
    }

    @Override
    public boolean hasPlugin(String key) {
        return this.flutterView.getPluginRegistry().hasPlugin(key);
    }

    @Override
    public <T> T valuePublishedByPlugin(String pluginKey) {
        return this.flutterView.getPluginRegistry().valuePublishedByPlugin(pluginKey);
    }

    @Override
    public Registrar registrarFor(String pluginKey) {
        return this.flutterView.getPluginRegistry().registrarFor(pluginKey);
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        return this.flutterView.getPluginRegistry().onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return this.flutterView.getPluginRegistry().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.activity.getWindow();
            window.addFlags(-2147483648);
            window.setStatusBarColor(1073741824);
            window.getDecorView().setSystemUiVisibility(1280);
        }

        String[] args = getArgsFromIntent(this.activity.getIntent());

        if(BuildConfig.DEBUG){
            Log.i(TAG, "onCreate: 是 debug 模式");
            FlutterMain.startInitialization(this.activity.getApplicationContext());
            FlutterMain.ensureInitializationComplete(this.activity.getApplicationContext(), args);
        }else {
            Log.i(TAG, "onCreate: delegate 中 是 release 模式 ");
            MyFlutterMain.startInitialization(this.activity.getApplicationContext());
            MyFlutterMain.ensureInitializationComplete(this.activity.getApplicationContext(), args);
        }




        this.flutterView = this.viewFactory.createFlutterView(this.activity);
        if (this.flutterView == null) {
            FlutterNativeView nativeView = this.viewFactory.createFlutterNativeView();
            this.flutterView = new FlutterView(this.activity, (AttributeSet)null, nativeView);
            this.flutterView.setLayoutParams(matchParent);
            this.activity.setContentView(this.flutterView);
            this.launchView = this.createLaunchView();
            if (this.launchView != null) {
                this.addLaunchView();
            }
        }

        if (!this.loadIntent(this.activity.getIntent())) {

            String appBundlePath = MyFlutterMain.findAppBundlePath();

            if(BuildConfig.DEBUG){
                appBundlePath = FlutterMain.findAppBundlePath();
                Log.i(TAG, "onCreate: delegate 加载debug 包");
            }

       //    String appBundlePath = MyFlutterMain.findAppBundlePath();



            if (appBundlePath != null) {
                this.runBundle(appBundlePath);
            }




        }
    }

    public void onNewIntent(Intent intent) {
        if (!this.isDebuggable() || !this.loadIntent(intent)) {
            this.flutterView.getPluginRegistry().onNewIntent(intent);
        }

    }

    private boolean isDebuggable() {
        return (this.activity.getApplicationInfo().flags & 2) != 0;
    }

    public void onPause() {
        Application app = (Application)this.activity.getApplicationContext();
        if (app instanceof FlutterApplication) {
            FlutterApplication flutterApp = (FlutterApplication)app;
            if (this.activity.equals(flutterApp.getCurrentActivity())) {
                flutterApp.setCurrentActivity((Activity)null);
            }
        }

        if (this.flutterView != null) {
            this.flutterView.onPause();
        }

    }

    public void onStart() {
        if (this.flutterView != null) {
            this.flutterView.onStart();
        }

    }

    public void onResume() {
        Application app = (Application)this.activity.getApplicationContext();
        if (app instanceof FlutterApplication) {
            FlutterApplication flutterApp = (FlutterApplication)app;
            flutterApp.setCurrentActivity(this.activity);
        }

    }

    public void onStop() {
        this.flutterView.onStop();
    }

    public void onPostResume() {
        if (this.flutterView != null) {
            this.flutterView.onPostResume();
        }

    }

    public void onDestroy() {
        Application app = (Application)this.activity.getApplicationContext();
        if (app instanceof FlutterApplication) {
            FlutterApplication flutterApp = (FlutterApplication)app;
            if (this.activity.equals(flutterApp.getCurrentActivity())) {
                flutterApp.setCurrentActivity((Activity)null);
            }
        }

        if (this.flutterView != null) {
            boolean detach = this.flutterView.getPluginRegistry().onViewDestroy(this.flutterView.getFlutterNativeView());
            if (!detach && !this.viewFactory.retainFlutterNativeView()) {
                this.flutterView.destroy();
            } else {
                this.flutterView.detach();
            }
        }

    }

    public boolean onBackPressed() {
        if (this.flutterView != null) {
            this.flutterView.popRoute();
            return true;
        } else {
            return false;
        }
    }

    public void onUserLeaveHint() {
        this.flutterView.getPluginRegistry().onUserLeaveHint();
    }

    public void onTrimMemory(int level) {
        if (level == 10) {
            this.flutterView.onMemoryPressure();
        }

    }

    public void onLowMemory() {
        this.flutterView.onMemoryPressure();
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    private static String[] getArgsFromIntent(Intent intent) {
        ArrayList<String> args = new ArrayList();
        if (intent.getBooleanExtra("trace-startup", false)) {
            args.add("--trace-startup");
        }

        if (intent.getBooleanExtra("start-paused", false)) {
            args.add("--start-paused");
        }

        if (intent.getBooleanExtra("disable-service-auth-codes", false)) {
            args.add("--disable-service-auth-codes");
        }

        if (intent.getBooleanExtra("use-test-fonts", false)) {
            args.add("--use-test-fonts");
        }

        if (intent.getBooleanExtra("enable-dart-profiling", false)) {
            args.add("--enable-dart-profiling");
        }

        if (intent.getBooleanExtra("enable-software-rendering", false)) {
            args.add("--enable-software-rendering");
        }

        if (intent.getBooleanExtra("skia-deterministic-rendering", false)) {
            args.add("--skia-deterministic-rendering");
        }

        if (intent.getBooleanExtra("trace-skia", false)) {
            args.add("--trace-skia");
        }

        if (intent.getBooleanExtra("trace-systrace", false)) {
            args.add("--trace-systrace");
        }

        if (intent.getBooleanExtra("dump-skp-on-shader-compilation", false)) {
            args.add("--dump-skp-on-shader-compilation");
        }

        if (intent.getBooleanExtra("verbose-logging", false)) {
            args.add("--verbose-logging");
        }

        int observatoryPort = intent.getIntExtra("observatory-port", 0);
        if (observatoryPort > 0) {
            args.add("--observatory-port=" + Integer.toString(observatoryPort));
        }

        if (intent.getBooleanExtra("disable-service-auth-codes", false)) {
            args.add("--disable-service-auth-codes");
        }

        if (intent.hasExtra("dart-flags")) {
            args.add("--dart-flags=" + intent.getStringExtra("dart-flags"));
        }

        if (!args.isEmpty()) {
            String[] argsArray = new String[args.size()];
            return (String[])args.toArray(argsArray);
        } else {
            return null;
        }
    }

    private boolean loadIntent(Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.RUN".equals(action)) {
            String route = intent.getStringExtra("route");
            String appBundlePath = intent.getDataString();
            if (appBundlePath == null) {
                appBundlePath = MyFlutterMain.findAppBundlePath();
                if(BuildConfig.DEBUG){
                    appBundlePath = FlutterMain.findAppBundlePath();
                }
            }

            if (route != null) {
                this.flutterView.setInitialRoute(route);
            }

            this.runBundle(appBundlePath);
            return true;
        } else {
            return false;
        }
    }

    private void runBundle(String appBundlePath) {
        if (!this.flutterView.getFlutterNativeView().isApplicationRunning()) {
            FlutterRunArguments args = new FlutterRunArguments();
            args.bundlePath = appBundlePath;
            args.entrypoint = "main";
            this.flutterView.runFromBundle(args);
        }

    }

    private View createLaunchView() {
        if (!this.showSplashScreenUntilFirstFrame()) {
            return null;
        } else {
            Drawable launchScreenDrawable = this.getLaunchScreenDrawableFromActivityTheme();
            if (launchScreenDrawable == null) {
                return null;
            } else {
                View view = new View(this.activity);
                view.setLayoutParams(matchParent);
                view.setBackground(launchScreenDrawable);
                return view;
            }
        }
    }

    private Drawable getLaunchScreenDrawableFromActivityTheme() {
        TypedValue typedValue = new TypedValue();
        if (!this.activity.getTheme().resolveAttribute(16842836, typedValue, true)) {
            return null;
        } else if (typedValue.resourceId == 0) {
            return null;
        } else {
            try {
                return this.activity.getResources().getDrawable(typedValue.resourceId);
            } catch (Resources.NotFoundException var3) {
                Log.e("FlutterActivityDelegate", "Referenced launch screen windowBackground resource does not exist");
                return null;
            }
        }
    }

    private Boolean showSplashScreenUntilFirstFrame() {
        try {
            @SuppressLint("WrongConstant") ActivityInfo activityInfo = this.activity.getPackageManager().getActivityInfo(this.activity.getComponentName()  ,PackageManager.GET_META_DATA|PackageManager.GET_ACTIVITIES);
            Bundle metadata = activityInfo.metaData;
            return metadata != null && metadata.getBoolean("io.flutter.app.android.SplashScreenUntilFirstFrame");
        } catch (PackageManager.NameNotFoundException var3) {
            return false;
        }
    }

    private void addLaunchView() {
        if (this.launchView != null) {
            this.activity.addContentView(this.launchView, matchParent);
            this.flutterView.addFirstFrameListener(new FlutterView.FirstFrameListener() {
                public void onFirstFrame() {
                    HotFixFlutterActivityDelegate.this.launchView.animate().alpha(0.0F).setListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            ((ViewGroup)HotFixFlutterActivityDelegate.this.launchView.getParent()).removeView(HotFixFlutterActivityDelegate.this.launchView);
                            HotFixFlutterActivityDelegate.this.launchView = null;
                        }
                    });
                    HotFixFlutterActivityDelegate.this.flutterView.removeFirstFrameListener(this);
                }
            });
            activity.setTheme(android.R.style.Theme_Black_NoTitleBar);
        }
    }

    public interface ViewFactory {
        FlutterView createFlutterView(Context var1);

        FlutterNativeView createFlutterNativeView();

        boolean retainFlutterNativeView();
    }
}