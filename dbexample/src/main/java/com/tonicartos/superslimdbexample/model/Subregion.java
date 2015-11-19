package com.tonicartos.superslimdbexample.model;

import android.content.ContentValues;

/**
 *
 */
public class Subregion extends DbModel {

    private static final String KEY_ID = "_id";

    private static final String KEY_NAME = "name";

    private static final String KEY_REGION_ID = "region_id";

    public static final String TABLE_NAME = "subregion";

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "( " +
            KEY_ID + " integer primary key, " +
            KEY_REGION_ID + " integer, " +
            KEY_NAME + " text " +
            ");";


    protected String mName;

    private long mRegionId;

    public static Subregion from(JsonData.SubRegion source) {
        Subregion s = new Subregion();
        s.mName = source.name;
        return s;
    }

    public static Subregion from(ContentValues values) {
        Subregion s = new Subregion();
        if (values.containsKey(KEY_ID)) {
            s.mId = values.getAsLong(KEY_ID);
        }

        if (values.containsKey(KEY_NAME)) {
            s.mName = values.getAsString(KEY_NAME);
        }

        if (values.containsKey(KEY_REGION_ID)) {
            s.mRegionId = values.getAsLong(KEY_REGION_ID);
        }
        return s;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
        markAsDirty();
    }

    public long getRegionId() {
        return mRegionId;
    }

    public void setRegionId(long regionId) {
        mRegionId = regionId;
    }

    @Override
    public ContentValues getValues() {
        ContentValues values = new ContentValues();
        values.put(KEY_REGION_ID, mRegionId);
        values.put(KEY_NAME, mName);
        return values;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    public void setRegion(Region region) {
        mRegionId = region.getId();
    }
}
