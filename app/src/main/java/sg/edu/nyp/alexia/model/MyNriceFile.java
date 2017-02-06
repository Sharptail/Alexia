package sg.edu.nyp.alexia.model;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Spencer on 6/2/2017.
 */

public class MyNriceFile {

    public static final String PREFS_NRIC = "MyNricFile";

    public void setNric(String nric, Context ctx) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS_NRIC, 0).edit();
        editor.putString("Nric", nric);
        editor.commit();
    }

    public String getNric(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NRIC, 0);
        String nricFile = prefs.getString("Nric", null);
        return nricFile;
    }
}
