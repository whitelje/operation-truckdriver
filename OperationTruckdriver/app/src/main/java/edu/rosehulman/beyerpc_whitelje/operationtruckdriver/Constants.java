package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

/**
 * Created by jakesorz on 2/1/16.
 */
public class Constants {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_STATS_OBD = 6;
    public static final int MESSAGE_STATS_J1939 = 7;
    public static final int MESSAGE_RX_OBD = 8;
    public static final int MESSAGE_RX_J1939 = 9;


    public static final int MAX_MESSAGE_SIZE = 250;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String OBD_PID = "obd_pid";
    public static final String OBD_DATA = "obd_data";
    public static final String STATS_COUNT = "stats_count";
    public static final String J1939_PGN = "j1939_pgn";
    public static final String J1939_VALUE = "j1939_value";
    public static final String J1939_SPN = "j1939_spn";
    public static final String J1939_FMI = "j1939_fmi";
    public static final String J1939_OC = "j1939_oc";
    public static final String J1939_ACTIVE = "j1939_active";
    public static final String J1939_MAKE = "j1939_make";
    public static final String J1939_MODEL = "j1939_model";
    public static final String J1939_SERIAL = "j1939_serial";
    public static final String J1939_VIN = "j1939_vin";

    public static final String FIREBASE_URL = "https://truckdriver.firebase.io/";
    public static final String FIREBASE_COMPANIES = "companies";
    public static final String FIREBASE_USERS = "users";
    public static final String FIREBASE_TRIPS = "trips";
    public static final String FIREBASE_POINTS = "points";
    public static final String TAG = "TD";
    public static final String PREFS = "PREFS";
    public static final String UID_KEY = "UID_KEY";
}
