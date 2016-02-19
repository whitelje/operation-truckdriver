package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by jakesorz on 2/16/16.
 */
public class SharedPreferencesUtils {

    public static String getCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
        return prefs.getString(Constants.UID_KEY, "");
    }

    public static void setCurrentUser(Context context, String uid) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.UID_KEY, uid);
        editor.commit();
    }

    public static void removeCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Constants.UID_KEY);
        editor.apply();
    }
}
