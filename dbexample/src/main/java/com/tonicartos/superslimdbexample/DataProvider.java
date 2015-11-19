package com.tonicartos.superslimdbexample;

import com.tonicartos.superslimdbexample.model.Country;
import com.tonicartos.superslimdbexample.model.Region;
import com.tonicartos.superslimdbexample.model.Subregion;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 *
 */
public class DataProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final String AUTHORITY = "com.tonicartos.superslimdbexample.provider";

    private static final int BIT_SINGLE = 1;

    private static final int BIT_REGION = 2;

    private static final int BIT_SUB_REGION = 4;

    private static final int BIT_COUNTRY = 8;

    private static final int BIT_ALL = 16;

    private DatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DatabaseHelper(getContext());

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        final int bits = sUriMatcher.match(uri);
        if ((bits & BIT_SINGLE) == BIT_SINGLE) {
            selection = addIdToSelection(selection);
            selectionArgs = addUriToSelectionArgs(uri, selectionArgs);
        } else {
            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = "_id ASC";
            }
        }

        final String table;
        switch (bits & ~BIT_SINGLE) {
            case BIT_COUNTRY:
                table = Country.TABLE_NAME;
                break;
            case BIT_REGION:
                table = Region.TABLE_NAME;
                break;
            case BIT_SUB_REGION:
                table = Subregion.TABLE_NAME;
                break;
            case BIT_ALL:
                table = Region.TABLE_NAME + " r LEFT OUTER JOIN " + Subregion.TABLE_NAME
                        + " s ON r._id = s.region_id LEFT OUTER JOIN " + Country.TABLE_NAME
                        + " c ON s._id = c.sub_region_id";
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(table, projection, selection, selectionArgs, null, null,
                sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final String firstPart;
        final int bits = sUriMatcher.match(uri);
        if ((bits & BIT_SINGLE) == BIT_SINGLE) {
            firstPart = "vnd.android.cursor.item/vnd.com.tonicartos.superslimdbexample.db.";
        } else {
            firstPart = "vnd.android.cursor.dir/vnd.com.tonicartos.superslimdbexample.db.";
        }

        final String secondPart;
        switch (bits & ~BIT_SINGLE) {
            case BIT_COUNTRY:
                secondPart = "country";
                break;
            case BIT_SUB_REGION:
                secondPart = "region";
                break;
            case BIT_REGION:
                secondPart = "sub_region";
                break;
            case BIT_ALL:
                secondPart = "all";
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return firstPart + secondPart;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int bits = sUriMatcher.match(uri);

        final String table;
        switch (bits & ~BIT_SINGLE) {
            case BIT_COUNTRY:
                table = Country.TABLE_NAME;
                break;
            case BIT_REGION:
                table = Region.TABLE_NAME;
                break;
            case BIT_SUB_REGION:
                table = Subregion.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        final long id = db.insert(table, null, values);

        if (id == -1) {
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        getContext().getContentResolver()
                .notifyChange(Uri.parse("content://com.tonicartos.superslimdbexample.provider/all"),
                        null);
        return Uri.parse(uri.getPath() + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int rowsChanged;

        final int bits = sUriMatcher.match(uri);
        if ((bits & BIT_SINGLE) == BIT_SINGLE) {
            selection = addIdToSelection(selection);
            selectionArgs = addUriToSelectionArgs(uri, selectionArgs);
        }

        final String table;
        switch (bits & ~BIT_SINGLE) {
            case BIT_COUNTRY:
                table = Country.TABLE_NAME;
                break;
            case BIT_SUB_REGION:
                table = Subregion.TABLE_NAME;
                break;
            case BIT_REGION:
                table = Region.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        rowsChanged = db.delete(table, selection, selectionArgs);

        if (rowsChanged > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            getContext().getContentResolver().notifyChange(
                    Uri.parse("content://com.tonicartos.superslimdbexample.provider/all"), null);
        }
        return rowsChanged;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int rowsChanged;

        final int bits = sUriMatcher.match(uri);
        if ((bits & BIT_SINGLE) == BIT_SINGLE) {
            selection = addIdToSelection(selection);
            selectionArgs = addUriToSelectionArgs(uri, selectionArgs);
        }

        final String table;
        switch (bits & ~BIT_SINGLE) {
            case BIT_COUNTRY:
                table = Country.TABLE_NAME;
                break;
            case BIT_SUB_REGION:
                table = Subregion.TABLE_NAME;
                break;
            case BIT_REGION:
                table = Region.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        rowsChanged = db.update(table, values, selection, selectionArgs);

        if (rowsChanged > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            getContext().getContentResolver().notifyChange(
                    Uri.parse("content://com.tonicartos.superslimdbexample.provider/all"), null);
        }

        return rowsChanged;
    }

    private String addIdToSelection(String selection) {
        if (TextUtils.isEmpty(selection)) {
            selection = "_ID = ?";
        } else {
            selection = "_ID = ? AND " + selection;
        }
        return selection;
    }

    private String[] addUriToSelectionArgs(Uri uri, String[] selectionArgs) {
        if (selectionArgs != null && selectionArgs.length > 0) {
            final String[] newSelectionArgs = new String[selectionArgs.length + 1];
            System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
            newSelectionArgs[0] = uri.getLastPathSegment();
            selectionArgs = newSelectionArgs;
        } else {
            selectionArgs = new String[]{uri.getLastPathSegment()};
        }
        return selectionArgs;
    }

    static {
        sUriMatcher.addURI(AUTHORITY, "all", BIT_ALL);
        sUriMatcher.addURI(AUTHORITY, Region.TABLE_NAME, BIT_REGION);
        sUriMatcher.addURI(AUTHORITY, Region.TABLE_NAME + "/#", BIT_REGION | BIT_SINGLE);
        sUriMatcher.addURI(AUTHORITY, Subregion.TABLE_NAME, BIT_SUB_REGION);
        sUriMatcher.addURI(AUTHORITY, Subregion.TABLE_NAME + "/#", BIT_SUB_REGION | BIT_SINGLE);
        sUriMatcher.addURI(AUTHORITY, Country.TABLE_NAME, BIT_COUNTRY);
        sUriMatcher.addURI(AUTHORITY, Country.TABLE_NAME + "/#", BIT_COUNTRY | BIT_SINGLE);
    }
}
