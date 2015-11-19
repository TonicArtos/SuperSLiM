package com.tonicartos.superslimdbexample.model;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 *
 */
public abstract class DbModel {

    protected static final String KEY_ID = "_id";

    protected long mId = -1;

    /**
     * Flag to tell if the object matches the database version.
     */
    private boolean mIsDirty = true;

    public void createRecord(SQLiteDatabase db) {
        mId = db.insert(getTableName(), null, getValues());
    }

    public void deleteRecord(SQLiteDatabase db) {
        db.delete(getTableName(), "WHERE _id = " + mId, null);
    }

    public long getId() {
        return mId;
    }

    public abstract ContentValues getValues();

    public boolean isDirty() {
        return mIsDirty;
    }

    public void markAsClean() {
        mIsDirty = false;
    }

    public void markAsDirty() {
        mIsDirty = true;
    }

    public void updateRecord(SQLiteDatabase db) {
        if (mIsDirty) {
            db.update(getTableName(), getValues(), "WHERE _id = " + mId, null);
        }
    }

    protected abstract String getTableName();
}
