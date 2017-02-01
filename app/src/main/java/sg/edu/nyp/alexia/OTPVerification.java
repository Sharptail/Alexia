package sg.edu.nyp.alexia;

import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import static java.security.AccessController.getContext;

public class OTPVerification extends AppCompatActivity {

    public static final String PREFS_NRIC = "MyNricFile";
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

        button = (Button)findViewById(R.id.gk);

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
            // Store to shared preferences
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NRIC, 0).edit();
            editor.putString("Nric", nricVerification.getNRIC() );
            editor.commit();

            Intent intent = new Intent(OTPVerification.this, AppointmentChecker.class);
            startActivity(intent);
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