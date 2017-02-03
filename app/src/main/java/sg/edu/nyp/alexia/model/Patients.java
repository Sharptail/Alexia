package sg.edu.nyp.alexia.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Patients {

    public String name;
    public String gender;
    public String birthdate;
    public String age;

    public Patients() {

    }

    public Patients(String name, String gender, String birthdate, String age){
        this.name = name;
        this.gender = gender;
        this.birthdate = birthdate;
        this.age = age;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("gender", gender);
        result.put("birthdate", birthdate);
        result.put("age", age);
        return result;
    }
}
