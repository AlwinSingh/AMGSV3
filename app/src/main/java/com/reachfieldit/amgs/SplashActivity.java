package com.reachfieldit.amgs;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.reachfieldit.amgs.util.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class SplashActivity extends AppCompatActivity {
    Constants constants = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        constants = new Constants(SplashActivity.this);

        //A 1 second delay before calling permissionHandler(). This 1 second delay can be removed if you want to. No issues with removing it!
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        boolean areTraditionalPermsValid = permissionHandlerTradition(); //Request for camera, audio, android 10 legacy storage
                        Log.d("[SPLASHACTIVITY]", "ARE TRADITIONAL PERMS VALID: " + areTraditionalPermsValid);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && areTraditionalPermsValid) {
                            if (Environment.isExternalStorageManager()) {
                                //IF perms are given, just check login session
                                Log.d("[SPLASHACTIVITY]", "ANDROID 11 PERMS VALID VALID");
                                checkLoginSession();
                            } else {
                                Log.d("[SPLASHACTIVITY]", "ANDROID 11 PERMS ARE NOT VALID");
                                permissionHandlerNew(); //Request for android 11 special storage
                            }
                        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && areTraditionalPermsValid) {
                            //That means perms are valid for Android 10
                            Log.d("[SPLASHACTIVITY]", "ANDROID 10 PERMS ARE VALID VALID");
                            checkLoginSession();
                        }
                    }
                },
                1000
        );
    }







    // !!!!!!!!!!!!!!!!!!!!!! For Android 11 & ABOVE PERMISSION !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    final static int APP_STORAGE_ACCESS_REQUEST_CODE = 501;
    Handler handler = new Handler();

    private void permissionHandlerNew() {
        Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean storagePermsGiven = Environment.isExternalStorageManager();
            Log.d("[SPLASHACTIVITY]", "ANDROID 11 REQUESTING FOR STORAGE PERMS NOW!!!!");
            startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);
            /*if (storagePermsGiven) {
                Log.d("[SPLASHACTIVITY]", "ANDROID 11 STORAGE PERMS GIVEN GO BACK TO SPLASH ACTIVITY");
                Intent i = new Intent(SplashActivity.this, SplashActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            } else {
                startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);
            }*/
        }
        /*Runnable checkSettingOn = new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    boolean storagePermsGiven = Environment.isExternalStorageManager();
                    if (storagePermsGiven) {
                        Log.d("#####################################", "CONTINUE AS PER USUAL.....");
                        Intent i = new Intent(SplashActivity.this, SplashActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    } else {
                        startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);
                    }
                }
            }
        };
        handler.postDelayed(checkSettingOn, 1000);*/
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(requestCode == APP_STORAGE_ACCESS_REQUEST_CODE && Environment.isExternalStorageManager()) {
                Log.d("[SPLASHACTIVITY]", "ANDROID 11 USER GRANTED PERMS FOR THE APPLICATION STORAGE");
            } else {
                // Permission not granted. Go to runtime fail page.
                Log.d("[SPLASHACTIVITY]", "ANDROID 11 USER DID NOT GRANT PERMS GO TO RUNTIME FAIL");
                //Intent intent=new Intent(SplashActivity.this,RuntimeFailActivity.class);
                //startActivity(intent);
            }
        }
    }
    // !!!!!!!!!!!!!!!!!!!!!! END OF Android 11 & ABOVE PERMISSION !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!






    // !!!!!!!!!!!!!!!!!!!!!! For Android 10 & Below PERMISSION !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private boolean checkPermission(String[] perms) {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), perms[0]);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), perms[1]);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private boolean permissionHandlerTradition() {
        //Check if all permissions are granted
        int permsRequestCode = 200;
        String[] perms = {"android.permission.RECORD_AUDIO", "android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.MANAGE_EXTERNAL_STORAGE"};
        boolean arePermsValid = checkPermission(perms);

        if (arePermsValid) {
            Log.d("[SPLASHACTIVITY]", "PERMISSIONS ARE ALL VALID");
        } else {
            Log.d("[SPLASHACTIVITY]", "REQUESTING PERMISSIONS");
            requestPermissions(perms, permsRequestCode);
        }

        return arePermsValid;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);
        switch (permsRequestCode) {
            case 200:
                boolean audioAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean readExtStorageAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean writeExtStorageAccepted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                boolean manageExtStorageAccepted = grantResults[4] == PackageManager.PERMISSION_GRANTED;
                Log.d("[SPLASHACTIVITY]", "AUDIO PERM ACCEPTED: " +audioAccepted);
                Log.d("[SPLASHACTIVITY]", "CAMERA PERM ACCEPTED: " +cameraAccepted);
                Log.d("[SPLASHACTIVITY]", "READ EXT STORAGE PERM ACCEPTED: " +readExtStorageAccepted);
                Log.d("[SPLASHACTIVITY]", "WRITE EXT STORAGE PERM ACCEPTED: " +writeExtStorageAccepted);
                Log.d("[SPLASHACTIVITY]", "MANAGE EXT STORAGE PERM ACCEPTED: " +manageExtStorageAccepted);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    readExtStorageAccepted = true;
                    writeExtStorageAccepted = true;
                    manageExtStorageAccepted = true;
                }

                if (audioAccepted && cameraAccepted && readExtStorageAccepted && writeExtStorageAccepted && manageExtStorageAccepted) {
                    //Now we check if the media folder is created [SHIFTED TO MAIN ACTIVITY]...
                    //checkMediaFolder();
                    finish();
                    startActivity(getIntent());
                } else {
                    //Redirect to page saying permissions are not valid
                    Intent intent=new Intent(SplashActivity.this,RuntimeFailActivity.class);
                    startActivity(intent);
                }

                break;
            default:
                Log.d("[SPLASHACTIVITY]", "PERMS REQUEST CODE: " + permsRequestCode);
        }
    }
    // !!!!!!!!!!!!!!!!!!!!!! END OF Android 10 & Below PERMISSION !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!






    public void checkMediaFolder(Context ctx) {
        try {
            //Check if media folder is created
            File dir = getApplicationContext().getExternalFilesDir("");

            if (dir.exists() && dir.isDirectory()) {
                Log.d("[SPLASHACTIVITY]", "!!!!!!!!!!!!!!!!!!!!!!!!!!! AMGS MEDIA FOLDER CREATED: TRUE");

                //Delete all content within the folder on app start up
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    new File(dir, children[i]).delete();
                }
                //End of delete all content within folder on app start up
            } else {
                Log.d("[SPLASHACTIVITY]", "!!!!!!!!!!!!!!!!!!!!!!!!!!! FAILED TO CREATE AMGS MEDIA FOLDER");
                Intent intent = new Intent(ctx, RuntimeFailActivity.class);
                ctx.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkLoginSession() {
        String jwtTokenStr = Constants.mPrefs.getString(Constants.userdata_prefs, null);

        //Check if login is still valid
        if (jwtTokenStr == null) {
            Log.d("[SPLASHACTIVITY]", "USER IS NOT ALREADY LOGGED IN");
            Intent intent=new Intent(SplashActivity.this,LoginActivity.class);
            startActivity(intent);
        } else {
            Log.d("[SPLASHACTIVITY]", "USER IS ALREADY LOGGED IN");
            Intent intent=new Intent(SplashActivity.this,MainActivity.class);
            startActivity(intent);
        }
    }
}