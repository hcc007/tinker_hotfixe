package com.hc.flutter_local_hotfix;


import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.widget.Toast;

import com.hc.flutter_local_hotfix.flutter.FlutterFileUtils;
import com.hc.flutter_local_hotfix.flutter.MyFlutterMain;
import com.hc.flutter_local_hotfix.tinker.Log.MyLogImp;
import com.hc.flutter_local_hotfix.tinker.utils.TinkerManager;
import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.lib.library.TinkerLoadLibrary;
import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.lib.tinker.TinkerLoadResult;
import com.tencent.tinker.loader.app.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;

import java.io.File;

import io.flutter.view.FlutterMain;

import com.hc.flutter_local_hotfix.BuildConfig;

/**
 * 使用DefaultLifeCycle注解生成Application（这种方式是Tinker官方推荐的）
 */
@SuppressWarnings("unused")
@DefaultLifeCycle(application = "com.hc.flutter_local_hotfix.MyApplication",// application类名。只能用字符串，这个MyApplication文件是不存在的，但可以在AndroidManifest.xml的application标签上使用（name）
        flags = ShareConstants.TINKER_ENABLE_ALL,// tinkerFlags
        loaderClass = "com.tencent.tinker.loader.TinkerLoader",//loaderClassName, 我们这里使用默认即可!（可不写）
        loadVerifyFlag = false)//tinkerLoadVerifyFlag
public class TinkerApplicationLike extends DefaultApplicationLike {

    private static final String TAG = "hcc";
    private Application mApplication;
    private Context mContext;
    private Tinker mTinker;

    // 固定写法
    public TinkerApplicationLike(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);

        Log.i(TAG, "TinkerApplicationLike:测试是否执行 ");

    }

    // 固定写法
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
        getApplication().registerActivityLifecycleCallbacks(callback);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 可以将之前自定义的Application中onCreate()方法所执行的操作搬到这里...

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);
        mApplication = getApplication();
        mContext = getApplication();
        Log.i(TAG, "onBaseContextAttached: 开始安装tinker");
        initTinker(base);
        // 可以将之前自定义的Application中onCreate()方法所执行的操作搬到这里...

        load_library_hack();
       // String str = FlutterFileUtils.copyLibAndWrite(base,"libapp.so");
      //  MyFlutterMain.setFixAppLibPath(str);

        if(BuildConfig.DEBUG){
            FlutterMain.startInitialization(mContext);
        }else {
            MyFlutterMain.startInitialization(mContext);
            Log.i(TAG, "onBaseContextAttached: application 中 release 模式");
        }

    }

    private void initTinker(Context base) {
        Log.i(TAG, "initTinker: ");
        // tinker需要你开启MultiDex
        MultiDex.install(base);
        TinkerManager.setTinkerApplicationLike(this);
        // 设置全局异常捕获
        TinkerManager.initFastCrashProtect();
        //开启升级重试功能（在安装Tinker之前设置）
        TinkerManager.setUpgradeRetryEnable(true);
        //设置Tinker日志输出类
        TinkerInstaller.setLogIml(new MyLogImp());
        //安装Tinker(在加载完multiDex之后，否则你需要将com.tencent.tinker.**手动放到main dex中)
        TinkerManager.installTinker(this);
        mTinker = Tinker.with(getApplication());
    }



    // 使用Hack的方式（测试成功）,flutter 加载，也是通过这种方式成功的。
    public void load_library_hack( ) {
        Log.i(TAG, "load_library_hack: ");
        String CPU_ABI = Build.CPU_ABI;
        // 将tinker library中的 CPU_ABI架构的so 注册到系统的library path中。
        try {
            ///
            Toast.makeText(mContext,"开始加载 so,abi:" + CPU_ABI,Toast.LENGTH_SHORT).show();

            TinkerLoadLibrary.installNavitveLibraryABI(mContext, "armeabi-v7a");

           // TinkerLoadLibrary.loadArmV7Library(mContext, "app");

          //  TinkerLoadLibrary.installNavitveLibraryABI(mContext, "armeabi-v7a");

            //    TinkerLoadLibrary.loadLibraryFromTinker(MainActivity.this, "lib/armeabi", "app");

            Toast.makeText(mContext,"加载 so 完成",Toast.LENGTH_SHORT).show();

            ///data/data/${package_name}/tinker/lib

            Tinker tinker = Tinker.with(mContext);
            TinkerLoadResult loadResult = tinker.getTinkerLoadResultIfPresent();

            if (loadResult.libs == null) {
                Log.i("hcc", "load_library_hack: " + "没有获取到 Libs 的路径。。。");
                return;
            }
            File soDir = new File(loadResult.libraryDirectory, "lib/" + "armeabi-v7a/libapp.so");

            if (soDir.exists()){
                Log.i("hcc", "load_library_hack: ,so 库文件的路径是:" + soDir.getAbsolutePath());

                if(!BuildConfig.DEBUG){
                    Log.i(TAG, "load_library_hack: 开始设置 tinker 路径");
                    MyFlutterMain.setFixAppLibPath(soDir.getAbsolutePath());
                }



            }else {
                Log.i("hcc", "load_library_hack: so 库文件路径不存在。。。 ");
            }


        }catch (Exception e){
            Toast.makeText(mContext,e.toString(),Toast.LENGTH_SHORT).show();
        }



    }



}
