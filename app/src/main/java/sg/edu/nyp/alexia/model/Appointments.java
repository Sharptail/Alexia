package sg.edu.nyp.alexia.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Appointments implements Serializable{

    public String date;
    public String time;
    public String type;
    public String checkin;
    public String doctor;
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

    public String getType() {
        return type;
    }

    public String getCheckin() {
        return checkin;
    }

    public String getRoom() {
        return room;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
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
