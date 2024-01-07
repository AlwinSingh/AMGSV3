package com.reachfieldit.amgs.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Constants {
    Context mCtx = null;
    public static SharedPreferences mPrefs = null;
    public SharedPreferences.Editor editor = null;
    public static CertTrustManager ctf = new CertTrustManager();

    public Constants(Context ctx) {
        mCtx = ctx;
        mPrefs = mCtx.getSharedPreferences(Constants.app_prefs, Context.MODE_PRIVATE);
        editor = mPrefs.edit();
    }

    private static String app_prefs = "AMGS";

    /* User shared preference keys */
    public static String userdata_prefs = "USER_DATA";
}
