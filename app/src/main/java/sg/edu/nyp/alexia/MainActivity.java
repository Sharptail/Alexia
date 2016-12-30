package sg.edu.nyp.alexia;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.mapbox.mapboxsdk.MapboxAccountManager;

import java.io.File;

public class MainActivity extends Activity {
    private String mapDownloadURL = "https://firebasestorage.googleapis.com/v0/b/mocktest-efa0d.appspot.com/o/singapore7-gh.zip?alt=media&token=752a6e87-1d69-4f23-9701-43e78f872a4b";
    private String targetFilePath = "/sdcard/Download/graphhopper/maps/";

    @Override
    protected void onStart(){
        super.onStart();
        if (isNetworkAvailable()) {
            if(new File(targetFilePath +  getString(R.string.map_file_name)).exists() == false){
                if(new File(targetFilePath + getString(R.string.map_file_name) + ".zip").exists() == false){
                    downloadFiles(getString(R.string.app_name)+" is downloading the necessary files...",mapDownloadURL);
                }else{
                    unzipFiles(getString(R.string.app_name)+" is unzipping the files...",targetFilePath + getString(R.string.map_file_name) + ".zip", targetFilePath);
                }
            }
        } else {
            showDialog("NO INTERNET CONNECTION", "Please connect to an internet to continue");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        MapboxAccountManager.start(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK
        }, 1234);


    }

    //Network checker Method
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showDialog(String title, String message){
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

    private ProgressDialog createProgressDialog(String message){
        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        return progressDialog;
    }

    private void downloadFiles(String message, String url){
        // Jian Wei - declare the dialog as a member field of the activity
        ProgressDialog downloadProgressDialog = createProgressDialog(message);

        // Jian Wei - execute this when the downloader must be fired
        final DownloadTask downloadTask = new DownloadTask(MainActivity.this,downloadProgressDialog, targetFilePath);
        downloadTask.execute(url);
    }

    private void unzipFiles(String message, String zippedFilePath, String targetFilePath){
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
        Intent intent = new Intent(MainActivity.this, CheckInActivity.class);
        startActivity(intent);
    }
}
