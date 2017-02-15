package sg.edu.nyp.alexia;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.io.File;

import sg.edu.nyp.alexia.checkin.AppointmentChecker;
import sg.edu.nyp.alexia.checkin.NRICVerification;
import sg.edu.nyp.alexia.model.MyNriceFile;
import sg.edu.nyp.alexia.services.SensorService;

public class MainActivity extends Activity {
    // For map download
    private String mapDownloadURL = "https://firebasestorage.googleapis.com/v0/b/mocktest-efa0d.appspot.com/o/singapore.zip?alt=media&token=9bf94819-10b1-4bbd-91bd-eba2c372b70e";
    private String targetFilePath = "/sdcard/Download/graphhopper/maps/";
    private File mapsFolder;

    // For NRIC verification
    public static String nricLog;
    MyNriceFile MyNricFile = new MyNriceFile();

    // For SensorService
    Intent mServiceIntent;
    private SensorService mSensorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // Initialize SensorService
        mSensorService = new SensorService(this);
        mServiceIntent = new Intent(this, mSensorService.getClass());
        if (MyNricFile.getNric(this) != null) {
            if (!isMyServiceRunning(mSensorService.getClass())) {
                startService(mServiceIntent);
            }
        }
        // Permission requests
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
        }, 1234);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Jian Wei - set graphhopper maps' folder
        boolean greaterOrEqKitkat = Build.VERSION.SDK_INT >= 19;
        if (greaterOrEqKitkat) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                logUser("GraphHopper is not usable without an external storage!");
                return;
            }
            mapsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "/graphhopper/maps/");
        } else
            mapsFolder = new File(Environment.getExternalStorageDirectory(), "/graphhopper/maps/");

        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }

        if (isNetworkAvailable()) {
            if (new File(targetFilePath + getString(R.string.map_file_name)).exists() == false) {
                if (new File(targetFilePath + getString(R.string.map_file_name) + ".zip").exists() == false) {
                    downloadFiles(getString(R.string.app_name) + " is downloading the necessary files...", mapDownloadURL);
                } else {
                    unzipFiles(getString(R.string.app_name) + " is unzipping the files...", targetFilePath + getString(R.string.map_file_name) + ".zip", targetFilePath);
                }
            }
        } else {
            showDialog("NO INTERNET CONNECTION", "Please connect to an internet to continue");
        }
    }

    @Override
    protected void onDestroy() {
        // Destroy SensorService to ensure service reboot
        stopService(mServiceIntent);
        Log.i("MAINACT", "onDestroy!");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Exit Application to phone's homescreen
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //Check if SensorService is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    //Network checker Method
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .show();
    }

    private ProgressDialog createProgressDialog(String message) {
        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        return progressDialog;
    }

    private void downloadFiles(String message, String url) {
        // Jian Wei - declare the dialog as a member field of the activity
        ProgressDialog downloadProgressDialog = createProgressDialog(message);

        // Jian Wei - execute this when the downloader must be fired
        final DownloadTask downloadTask = new DownloadTask(MainActivity.this, downloadProgressDialog, targetFilePath);
        downloadTask.execute(url);
    }

    private void unzipFiles(String message, String zippedFilePath, String targetFilePath) {
        // Jian Wei - declare the dialog as a member field of the activity
        ProgressDialog unzipProgressDialog = createProgressDialog(message);

        // Jian Wei - execute this when the unzipper must be fired
        final Unzip unzip = new Unzip(MainActivity.this, new File(zippedFilePath), new File(targetFilePath), unzipProgressDialog);
        unzip.execute();
    }

    public void goToRouting(View view) {
        Intent intent = new Intent(MainActivity.this, RoutingActivity.class);
        startActivity(intent);
    }

    public void goToCheckIn(View view) {
        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == -1){
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1234);
        } else {
            // Check for stored NRIC under shared preferences
            if (MyNricFile.getNric(this) != null) {
                Intent intent = new Intent(MainActivity.this, AppointmentChecker.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(MainActivity.this, NRICVerification.class);
                startActivity(intent);
            }
        }
    }

    // Jian Wei - is to Log a string and Toast at the same time for easier debugging
    private void log(String str) {
        Log.e("GH", str);
    }

    private void log(String str, Throwable t) {
        Log.e("GH", str, t);
    }

    private void logUser(String str) {
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}