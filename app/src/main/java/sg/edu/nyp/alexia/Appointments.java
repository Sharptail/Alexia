package sg.edu.nyp.alexia;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Appointments {

    public String date;
    public String time;
    public String type;

    public String getCheckin() {
        return checkin;
    }

    public String checkin;
    public String doctor;

    public String getRoom() {
        return room;
    }



    public String room;
    public Map<String, Boolean> appoint = new HashMap<>();

    public Appointments() {

    }

    public Appointments( String date, String time, String type, String checkin, String doctor, String room) {
        this.date = date;
        this.time = time;
        this.type = type;
        this.checkin = checkin;
        this.doctor = doctor;
        this.room = room;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("time", time);
        result.put("type", type);
        result.put("checkin", checkin);
        result.put("doctor", doctor);
        result.put("room", room);

        return result;
    }
}
