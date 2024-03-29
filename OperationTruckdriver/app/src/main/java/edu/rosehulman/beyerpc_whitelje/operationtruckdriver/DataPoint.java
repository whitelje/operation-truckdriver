package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by jakesorz on 2/16/16.
 */
public class DataPoint implements Parcelable, Comparable<DataPoint> {
    @JsonIgnore
    String key;

    double posLat;
    double posLng;

    long time;

    double vehicleSpeed;
    double rpm;
    double oilPressure;
    double distance;
    double odometer;
    double tripMpg;
    double engineTemp;

    int dtcCount;

    // for Firebase
    DataPoint() {

    }

    protected DataPoint(Parcel in) {
        key = in.readString();
        posLat = in.readDouble();
        posLng = in.readDouble();
        time = in.readLong();
        vehicleSpeed = in.readDouble();
        rpm = in.readDouble();
        oilPressure = in.readDouble();
        distance = in.readDouble();
        odometer = in.readDouble();
        tripMpg = in.readDouble();
        engineTemp = in.readDouble();
        dtcCount = in.readInt();
    }

    public static final Creator<DataPoint> CREATOR = new Creator<DataPoint>() {
        @Override
        public DataPoint createFromParcel(Parcel in) {
            return new DataPoint(in);
        }

        @Override
        public DataPoint[] newArray(int size) {
            return new DataPoint[size];
        }
    };

    public double getPosLat() {
        return posLat;
    }

    public void setPosLat(double posLat) {
        this.posLat = posLat;
    }

    public double getPosLng() {
        return posLng;
    }

    public void setPosLng(double posLng) {
        this.posLng = posLng;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getVehicleSpeed() {
        return vehicleSpeed;
    }

    public void setVehicleSpeed(double vehicleSpeed) {
        this.vehicleSpeed = vehicleSpeed;
    }

    public double getRpm() {
        return rpm;
    }

    public void setRpm(double rpm) {
        this.rpm = rpm;
    }

    public double getOilPressure() {
        return oilPressure;
    }

    public void setOilPressure(double oilPressure) {
        this.oilPressure = oilPressure;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getOdometer() {
        return odometer;
    }

    public void setOdometer(double odometer) {
        this.odometer = odometer;
    }

    public double getTripMpg() {
        return tripMpg;
    }

    public void setTripMpg(double tripMpg) {
        this.tripMpg = tripMpg;
    }

    public double getEngineTemp() {
        return engineTemp;
    }

    public void setEngineTemp(double engineTemp) {
        this.engineTemp = engineTemp;
    }

    public int getDtcCount() {
        return dtcCount;
    }

    public void setDtcCount(int dtcCount) {
        this.dtcCount = dtcCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeDouble(posLat);
        dest.writeDouble(posLng);
        dest.writeLong(time);
        dest.writeDouble(vehicleSpeed);
        dest.writeDouble(rpm);
        dest.writeDouble(oilPressure);
        dest.writeDouble(distance);
        dest.writeDouble(odometer);
        dest.writeDouble(tripMpg);
        dest.writeDouble(engineTemp);
        dest.writeInt(dtcCount);
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public int compareTo(DataPoint another) {
        return time < another.time ? -1 : time == another.time ? 0 : 1;
    }
}
