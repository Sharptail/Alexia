package sg.edu.nyp.alexia.checkin;

import android.content.Intent;
import android.content.IntentFilter;
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

public class OTPVerification extends AppCompatActivity {
    private static final String TAG = "OTPVerification";

    // For NRIC
    MyNriceFile MyNricFile = new MyNriceFile();

    // For NRIC Retrieval
    NRICVerification nricVerification = new NRICVerification();

    // EditText views
    static String[] edtid = new String[]{"et1", "et2", "et3", "et4", "et5", "et6"};
    static EditText[] editTextView = new EditText[6];
    int tempid;
    static Button button;
    String output;

    // For SMS receiver
    private SmsReceiver mSMSreceiver;
    private IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.otp_verify);

        button = (Button) findViewById(R.id.gk);

        // Show keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // For loop to attach TextWatcher & KeyListener
        for (int i = 0; i < edtid.length; i++) {
            final int idtag = i;
            // Retrieve EditText ID into tempid
            tempid = getResources().getIdentifier(edtid[i], "id", getPackageName());
            editTextView[i] = (EditText) findViewById(tempid);
            if (i < 5) {
                editTextView[i].addTextChangedListener(new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        if (s.length() == 1) {
                            Log.e(TAG, "Text Change " + editTextView[idtag].getId());
                            editTextView[idtag + 1].setFocusableInTouchMode(true);
                            editTextView[idtag + 1].requestFocus();
                            editTextView[idtag + 1].setSelected(true);
                            editTextView[idtag].setFocusableInTouchMode(false);
                        }
                    }
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {                    }
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
    public void onDestroy() {
        super.onDestroy();

        // Unregister the SMS receiver
        unregisterReceiver(mSMSreceiver);
    }

    // Removed back button
    @Override
    public void onBackPressed() {}

    // Verify all OTP EditText input
    public void display(View view) {
        StringBuilder sb = new StringBuilder();

        // Append all EditText input into a string
        for (int i = 0; i < edtid.length; i++) {
            Log.e(TAG, "Testing this" + editTextView[i].getText());
            sb.append(editTextView[i].getText());
        }
        output = sb.toString();
        Log.e(TAG, "Keyed in " + output);
        Log.e(TAG, output + " = " + nricVerification.getOTP());

        // If EditText input equals OTP
        if (output.equals(nricVerification.getOTP())) {
            MyNricFile.setNric(nricVerification.getNRIC(), OTPVerification.this);
            Intent intent = new Intent(OTPVerification.this, AppointmentChecker.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Invalid OTP!", Toast.LENGTH_SHORT).show();
        }
    }

    public void pinLayout(View view) {
        // Set on clicking EditText view will remove all current input
        // and reset back to first EditText view
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

    public void receivedSms(String message) {
        try {
            // Receive OTP from SMS and fill up EditText view + submit
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