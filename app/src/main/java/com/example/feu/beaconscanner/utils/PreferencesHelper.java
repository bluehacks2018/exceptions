package  com.example.feu.beaconscanner.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class PreferencesHelper {

    private static final String SHARED_PREF_KEY = "shared_pref";
    private SharedPreferences prefs;

    private static final String TUTO_KEY = "tutoKey";
    private static final String SCAN_ON_OPEN_KEY = "scanOnOpenKey";
    private static final String SCANNING_STATE = "scanningState";

    public PreferencesHelper(Context ctx) {
        prefs = ctx.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
    }

    public void setScanOnOpen(boolean status) {
        prefs.edit().putBoolean(SCAN_ON_OPEN_KEY, status).apply();
    }

    public boolean isScanOnOpen() {
        return prefs.getBoolean(SCAN_ON_OPEN_KEY, false);
    }

    public void setScanningState(boolean state) {
        prefs.edit().putBoolean(SCANNING_STATE, state).apply();
    }

    public boolean wasScanning() {
        return prefs.getBoolean(SCANNING_STATE, false);
    }
}
