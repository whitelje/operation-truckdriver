package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Created by jakesorz on 2/17/16.
 */
public class User {
    @JsonIgnore
    private String key;

    private String name;
    private Map<String, Boolean> points;

    public User() {

    }

    public User(String s) {
        name = s;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Boolean> getPoints() {
        return points;
    }

    public void setPoints(Map<String, Boolean> points) {
        this.points = points;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
