package com.hc.flutter_local_hotfix;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.hc.flutter_local_hotfix.flutter.HotFixFlutterActivity;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends HotFixFlutterActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {
  private static final String CHANNEL = "com.hc.flutter";


  private static final String[] my_permission =
          {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

  private static final int RC_LOCATION_CONTACTS_PERM = 124;


  private static final String TAG = "hcc";
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.i(TAG, "onCreate: ");
    EasyPermissions.requestPermissions(
            this,
            "读写权限",
            RC_LOCATION_CONTACTS_PERM,
            my_permission);


    GeneratedPluginRegistrant.registerWith(this);
    new MethodChannel(getFlutterView(), CHANNEL).setMethodCallHandler(
            new MethodChannel.MethodCallHandler() {
              @Override
              public void onMethodCall(MethodCall call, MethodChannel.Result result) {

                if(call.method.equals( "load")){
                  Toast.makeText(MainActivity.this,"开始加载补丁 bugfix",Toast.LENGTH_SHORT).show();
                  install_patch();
                }

                if(call.method.equals("restart")){
                  Log.i(TAG, "onMethodCall: kill myself");
                  kill_myself();
                }

              }
            });
  }


  //加载补丁
  public void install_patch( ) {
    Toast.makeText(getApplicationContext(), "开始打补丁", Toast.LENGTH_SHORT).show();
    Log.i("hcc", "install_patch: "  );
    TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), Environment.getExternalStorageDirectory().getAbsolutePath() + "/patch_signed_7zip.apk");
    Toast.makeText(getApplicationContext(), "补丁完成", Toast.LENGTH_SHORT).show();
//        TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), getCacheDir().getAbsolutePath() + "/patch_signed_7zip.apk");
  }






    //杀进程
  public void kill_myself( ) {
    ShareTinkerInternals.killAllOtherProcess(getApplicationContext());
    android.os.Process.killProcess(android.os.Process.myPid());
  }


  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // Forward results to EasyPermissions
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }

  @Override
  public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
   //  Toast.makeText(getApplicationContext(),"成功获取权限:" + perms.get(0),Toast.LENGTH_SHORT).show();

  }

  @Override
  public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

  }

  @Override
  public void onRationaleAccepted(int requestCode) {

  }

  @Override
  public void onRationaleDenied(int requestCode) {

  }
}
