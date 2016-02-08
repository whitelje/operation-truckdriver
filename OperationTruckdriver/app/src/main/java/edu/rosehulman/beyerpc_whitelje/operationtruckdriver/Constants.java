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

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final int MAX_MESSAGE_SIZE = 250;
    public static final int MESSAGE_RX_OBD = 8;
    public static final String OBD_PID = "obd_pid";
    public static final String OBD_DATA = "obd_data";
    public static final int MESSAGE_STATS_OBD = 6;
    public static final String STATS_COUNT = "stats_count";
}
