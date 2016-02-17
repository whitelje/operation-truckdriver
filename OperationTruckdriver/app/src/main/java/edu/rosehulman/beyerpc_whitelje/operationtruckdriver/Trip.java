package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Created by jakesorz on 2/15/16.
 */
public class Trip implements Comparable<Trip> {
    private long date;
    private Map<String, Boolean> points;

    @JsonIgnore
    private String key;


    Trip() {

    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public Map<String, Boolean> getPoints() {
        return points;
    }

    public void setPoints(Map<String, Boolean> points) {
        this.points = points;
    }

    @Override
    public int compareTo(Trip another) {
        return date < another.date ? 1 : date == another.date ? 0 : -1;
    }

    public String getKey() {
        return key;
    }
}
