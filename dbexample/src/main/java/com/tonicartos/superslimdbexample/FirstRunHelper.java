package com.tonicartos.superslimdbexample;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.tonicartos.superslimdbexample.model.Country;
import com.tonicartos.superslimdbexample.model.JsonData;
import com.tonicartos.superslimdbexample.model.Region;
import com.tonicartos.superslimdbexample.model.Subregion;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;

import okio.Okio;

/**
 * A helper to setup the application on the first run.
 */
public class FirstRunHelper {

    private static final String TAG = "First Run Helper";

    private static final String PREFS = "cac";

    private static final String PREF_FIRST_RUN = "is_first_run";

    private static final String PREF_INITIALISED = "initialised";

    /**
     * Default mode for accessing shared preferences.
     */
    private static final int MODE = Context.MODE_PRIVATE;

    public static boolean isFirstRun(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_FIRST_RUN, true);
    }

    public static boolean isInitialised(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_INITIALISED, false);
    }

    public static boolean performInitialisation(SQLiteDatabase db, Context context) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<JsonData> jsonAdapter = moshi.adapter(JsonData.class);
        JsonData jsonData;
        try {
            jsonData = jsonAdapter.fromJson(Okio.buffer(
                    Okio.source(context.getResources().openRawResource(R.raw.init_db_data))));
        } catch (IOException e) {
            String msg = "Error loading initial database data file.";
            Log.e(TAG, msg);
            e.printStackTrace();
            return false;
        }

        db.beginTransaction();
        try {
            for (JsonData.Region r : jsonData.regions) {
                Region region = Region.from(r);
                region.createRecord(db);

                for (JsonData.SubRegion s : r.sub_regions) {
                    Subregion subregion = Subregion.from(s);
                    subregion.setRegion(region);
                    subregion.createRecord(db);

                    for (JsonData.Country c : s.countries) {
                        Country country = Country.from(c);
                        country.setSubRegion(subregion);
                        country.createRecord(db);
                    }
                }
            }

            setIsInitialised(true, context);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return true;
    }

    public static void setFirstRunComplete(boolean value, Context context) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(PREFS, MODE).edit();
        prefsEditor.putBoolean(PREF_FIRST_RUN, false);
        prefsEditor.commit();
    }

    public static void setIsInitialised(boolean value, Context context) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(PREFS, MODE).edit();
        prefsEditor.putBoolean(PREF_INITIALISED, false);
        prefsEditor.commit();
    }
}
