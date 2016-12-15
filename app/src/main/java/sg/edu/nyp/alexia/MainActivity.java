package sg.edu.nyp.alexia;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.mapbox.mapboxsdk.MapboxAccountManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        MapboxAccountManager.start(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        requestPermissions(new String[] {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1234);
    }

    public void goToRouting(View view){
        Intent intent = new Intent(MainActivity.this, RoutingActivity.class);
        startActivity(intent);
    }

    public void goToCheckIn(View view){
        Intent intent = new Intent(MainActivity.this, CheckInActivity.class);
        startActivity(intent);
    }

}
