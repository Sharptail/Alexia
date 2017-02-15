package sg.edu.nyp.alexia.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

import static android.R.attr.name;

@IgnoreExtraProperties
public class Timeslot {

    public String date;
    public String slot1;
    public String slot2;
    public String slot3;

    public Timeslot() {}

    public Timeslot(String date, String slot1, String slot2, String slot3) {
        this.date = date;
        this.slot1 = slot1;
        this.slot2 = slot2;
        this.slot3 = slot3;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("slot1", slot1);
        result.put("slot2", slot2);
        result.put("slot3", slot3);
        return result;
    }
}



