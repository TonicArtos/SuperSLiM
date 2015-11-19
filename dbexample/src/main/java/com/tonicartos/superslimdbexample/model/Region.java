package com.tonicartos.superslimdbexample.model;

import android.content.ContentValues;

/**
 * Region db model
 */
public class Region extends DbModel {

    private static final String KEY_NAME = "name";

    public static final String TABLE_NAME = "region";

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "( " +
            KEY_ID + " integer primary key, " +
            KEY_NAME + " text " +
            ");";

    protected String mName;

    public static Region from(JsonData.Region source) {
        Region r = new Region();
        r.mName = source.name;
        return r;
    }

    public static Region from(ContentValues values) {
        Region r = new Region();
        r.mId = values.getAsLong(KEY_ID);
        r.mName = values.getAsString(KEY_NAME);
        return r;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
        markAsDirty();
    }

    @Override
    public ContentValues getValues() {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, mName);
        return values;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }
}
