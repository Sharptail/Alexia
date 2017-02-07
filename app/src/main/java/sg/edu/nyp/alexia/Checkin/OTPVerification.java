package sg.edu.nyp.alexia.checkin;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import sg.edu.nyp.alexia.R;
import sg.edu.nyp.alexia.receivers.SmsReceiver;
import sg.edu.nyp.alexia.model.MyNriceFile;
import sg.edu.nyp.alexia.services.SensorService;

public class OTPVerification extends AppCompatActivity {

    MyNriceFile MyNricFile = new MyNriceFile();
    NRICVerification nricVerification = new NRICVerification();
    private static final String TAG = "OTPVerification";
    String output;
    int tempid;
    static String[] edtid = new String[]{"et1", "et2", "et3", "et4", "et5", "et6"};
    static EditText[] editTextView = new EditText[6];

    static EditText otp1;
    static EditText otp2;
    static EditText otp3;
    static EditText otp4;
    static EditText otp5;
    static EditText otp6;
    static Button button;

    private SmsReceiver mSMSreceiver;
    private IntentFilter mIntentFilter;
    // For SensorService
    Intent mServiceIntent;
    private SensorService mSensorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.otp_verify);

        otp1 = (EditText) findViewById(R.id.et1);
        otp2 = (EditText) findViewById(R.id.et2);
        otp3 = (EditText) findViewById(R.id.et3);
        otp4 = (EditText) findViewById(R.id.et4);
        otp5 = (EditText) findViewById(R.id.et5);
        otp6 = (EditText) findViewById(R.id.et6);

        button = (Button) findViewById(R.id.gk);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);


        for (int i = 0; i < edtid.length; i++) {
            final int idtag = i;
            tempid = getResources().getIdentifier(edtid[i], "id", getPackageName());
            editTextView[i] = (EditText) findViewById(tempid);
            if (i < 5) {
                editTextView[i].addTextChangedListener(new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        // TODO Auto-generated method stub
                        if (s.length() == 1) {
                            Log.e(TAG, "Text Change " + editTextView[idtag].getId());
                            editTextView[idtag + 1].setFocusableInTouchMode(true);
                            editTextView[idtag + 1].requestFocus();
                            editTextView[idtag + 1].setSelected(true);
                            editTextView[idtag].setFocusableInTouchMode(false);
                        }
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count,
                                                  int after) {
                        // TODO Auto-generated method stub
                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // TODO Auto-generated method stub
                    }
                });
            }
            if (i > 0) {
                editTextView[i].setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (event.getAction() != KeyEvent.ACTION_DOWN) {
                            if (keyCode == KeyEvent.KEYCODE_DEL) {
                                Log.e(TAG, "OnKey " + editTextView[idtag].getId());
                                editTextView[idtag].setSelected(false);
                                editTextView[idtag - 1].setFocusableInTouchMode(true);
                                editTextView[idtag - 1].setText("");
                                editTextView[idtag - 1].requestFocus();
                                editTextView[idtag - 1].setSelected(true);
                                editTextView[idtag].setFocusableInTouchMode(false);
                                return true;
                            }
                        }
                        return false;
                    }
                });
            }
        }

        // Register SMS Receiver
        mSMSreceiver = new SmsReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(mSMSreceiver, mIntentFilter);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // Unregister the SMS receiver
        unregisterReceiver(mSMSreceiver);
    }

    @Override
    public void onBackPressed() {    }

    public void display(View view) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < edtid.length; i++) {
            Log.e(TAG, "Testing this" + editTextView[i].getText());
            sb.append(editTextView[i].getText());
        }
        output = sb.toString();
        Log.e(TAG, "Keyed in " + output);
        Log.e(TAG, output + " = " + nricVerification.getOTP());
        if (output.equals(nricVerification.getOTP())) {

            MyNricFile.setNric(nricVerification.getNRIC(), OTPVerification.this);

            Intent intent = new Intent(OTPVerification.this, AppointmentChecker.class);
            startActivity(intent);
            finish();
            // Initialize SensorService
            mSensorService = new SensorService(this);
            mServiceIntent = new Intent(this, mSensorService.getClass());
            if (!isMyServiceRunning(mSensorService.getClass())) {
               startService(mServiceIntent);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Invalid OTP!", Toast.LENGTH_SHORT).show();
        }
    }

    public void pinLayout(View view) {

        for (int i = 0; i < edtid.length; i++) {
            editTextView[i].setText("");
            if (i > 0) {
                Log.e(TAG, "This is " + editTextView[i].getId());
                editTextView[i].setFocusableInTouchMode(false);
            }
        }
        editTextView[0].setFocusableInTouchMode(true);
        editTextView[0].requestFocus();
        editTextView[0].setSelected(true);
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

    public void receivedSms(String message) {
        try {
            Log.d("message is receive", "Space=" + message);
            String[] ecs = message.split("");
            for (int i = 1; i <= ecs.length; i++) {
                editTextView[i - 1].setText(ecs[i]);
                if (i == 6) {
                    button.performClick();
                }
            }
        } catch (Exception e) {
            Log.e("message not receive", e.getMessage() + "");
        }
    }
}