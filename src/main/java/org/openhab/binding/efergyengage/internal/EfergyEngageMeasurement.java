package org.openhab.binding.efergyengage.internal;

/**
 * Created by Ondřej Pečta on 10. 8. 2016.
 */
public class EfergyEngageMeasurement {

    private float value = -1;
    private String unit = "W";
    private long milis = 0;

    EfergyEngageMeasurement() {
    }


    EfergyEngageMeasurement(float value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public long getMilis() {
        return milis;
    }

    public void setMilis(long milis) {
        this.milis = milis;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String toString() {
        return value + " " + unit;
    }
}
