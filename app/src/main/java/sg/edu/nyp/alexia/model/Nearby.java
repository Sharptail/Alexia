package sg.edu.nyp.alexia.model;

/**
 * Created by Jeffry on 1/2/17.
 */

public class Nearby {
    private String name;
    private int icon;

    public Nearby() {
    }

    public Nearby(String name, int icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
