package com.acesse.youchat.contentprovider;

import java.util.Arrays;
import java.util.HashSet;

import com.acesse.youchat.YouChatContactsHelper;
import com.acesse.youchat.YouChatContactsTable;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class YouChatContactContentProvider extends ContentProvider {

    private YouChatContactsHelper database;
    private static final String TAG ="YOUC";
    private static final int ALL_CONTACTS = 1;
    private static final int SINGLE_CONTACT = 2;

    private static final String AUTHORITY = "com.acesse.youchat.contentprovider";
    private static final String BASE_PATH = "contacts";
    public static final Uri CONTENT_URI  = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "contacts", ALL_CONTACTS);
        uriMatcher.addURI(AUTHORITY, "contacts/#", SINGLE_CONTACT);
    }

    @Override
    public boolean onCreate() {
        database = new YouChatContactsHelper(getContext());
        return false;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id = 0;
        switch (uriType) {
        case ALL_CONTACTS:
            id = sqlDB.insert(YouChatContactsTable.TABLE_YOUCHATCONTACTS, null, values);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // check if the caller has requested a column which does not exists
        checkColumns(projection);
        // Set the table
        queryBuilder.setTables(YouChatContactsTable.TABLE_YOUCHATCONTACTS);
        int uriType = uriMatcher.match(uri);
        switch (uriType) {
        case ALL_CONTACTS:
            break;
        case SINGLE_CONTACT:
            // adding the ID to the original query
            queryBuilder.appendWhere(YouChatContactsTable.COLUMN_ID + "="
                    + uri.getLastPathSegment());
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
        case ALL_CONTACTS:
            rowsUpdated = sqlDB.updateWithOnConflict(YouChatContactsTable.TABLE_YOUCHATCONTACTS, values,selection,
                    selectionArgs,SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG,"Contact Provider:Rows Updated: " + rowsUpdated);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
        case ALL_CONTACTS:
            rowsDeleted = sqlDB.delete(YouChatContactsTable.TABLE_YOUCHATCONTACTS, selection,
                    selectionArgs);
            Log.d(TAG,"Contact Provider:Rows Deleted: " + rowsDeleted);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    private void checkColumns(String[] projection) {
        String[] available = { YouChatContactsTable.COLUMN_YOUCHATID,
                YouChatContactsTable.COLUMN_CONTACT, YouChatContactsTable.COLUMN_ID, YouChatContactsTable.COLUMN_CONTACT_PICTURE_URL,YouChatContactsTable.COLUMN_CONTACT_PICTURE };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

}
