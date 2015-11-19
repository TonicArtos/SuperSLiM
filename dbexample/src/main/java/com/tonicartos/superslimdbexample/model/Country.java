package com.tonicartos.superslimdbexample.model;

import android.content.ContentValues;

/**
 * Country database model.
 */
public class Country extends DbModel {

    private static final String KEY_SUB_REGION_ID = "sub_region_id";

    private static final String KEY_NAME = "name";

    public static final String TABLE_NAME = "country";

    public static final java.lang.String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            KEY_ID + " integer primary key, " +
            KEY_SUB_REGION_ID + " integer, " +
            KEY_NAME + " text " +
            ");";

    protected long mSubRegionId;

    protected String mName;

    public static Country from(JsonData.Country source) {
        Country c = new Country();
        c.mName = source.name;
        return c;
    }

    public static Country from(ContentValues values) {
        Country c = new Country();

        if (values.containsKey(KEY_ID)) {
            c.mId = values.getAsLong(KEY_ID);
        }

        if (values.containsKey(KEY_NAME)) {
            c.mName = values.getAsString(KEY_NAME);
        }

        if (values.containsKey(KEY_SUB_REGION_ID)) {
            c.mSubRegionId = values.getAsLong(KEY_SUB_REGION_ID);
        }

        return c;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
        markAsDirty();
    }

    public long getSubRegionId() {
        return mSubRegionId;
    }

    public void setSubRegionId(long subRegionId) {
        mSubRegionId = subRegionId;
        markAsDirty();
    }

    @Override
    public ContentValues getValues() {
        ContentValues values = new ContentValues();
        values.put(KEY_SUB_REGION_ID, mSubRegionId);
        values.put(KEY_NAME, mName);
        return values;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    public void setSubRegion(Subregion subregion) {
        mSubRegionId = subregion.getId();
    }
}
