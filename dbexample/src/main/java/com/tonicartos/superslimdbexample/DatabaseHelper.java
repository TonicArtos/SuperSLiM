package com.tonicartos.superslimdbexample;

import com.tonicartos.superslimdbexample.model.Subregion;
import com.tonicartos.superslimdbexample.model.Country;
import com.tonicartos.superslimdbexample.model.Region;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "countries";

    private static final int DATABASE_VERSION = 1;

    private final Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Country.CREATE_TABLE);
        db.execSQL(Region.CREATE_TABLE);
        db.execSQL(Subregion.CREATE_TABLE);

        if (!FirstRunHelper.isInitialised(mContext)) {
            FirstRunHelper.setIsInitialised(
                    FirstRunHelper.performInitialisation(db, mContext), mContext);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
