package sg.edu.nyp.alexia.model;

import android.content.Context;
import android.content.SharedPreferences;

import static sg.edu.nyp.alexia.model.MyNriceFile.PREFS_NRIC;

/**
 * Created by Spencer on 14/2/2017.
 */

public class HelpInfo {

    public static final String PREFS_HELP = "Help";

    public void setHelp(String text, Context ctx) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS_HELP, 1).edit();
        editor.putString("Help", text);
        editor.commit();
    }

    public String getHelp(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_HELP, 1);
        String helpFile = prefs.getString("Help", null);
        return helpFile;
    }
}