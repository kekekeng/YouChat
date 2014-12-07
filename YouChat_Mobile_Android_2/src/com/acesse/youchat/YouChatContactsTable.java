package com.acesse.youchat;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class YouChatContactsTable {
    
 // Database table
    public static final String TABLE_YOUCHATCONTACTS = "youchatcontacts";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_YOUCHATID = "youchatid";
    public static final String COLUMN_CONTACT = "contact";
    public static final String COLUMN_CONTACT_PICTURE_URL = "contactpictureurl";
    public static final String COLUMN_CONTACT_PICTURE = "contactpicture";
    public static final String COLUMN_LAST_MODIFIED_DATE ="lastmodifieddate";
    public static final String TAG = "YOUC";

    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table " 
        + TABLE_YOUCHATCONTACTS
        + "(" 
        + COLUMN_ID + " integer primary key autoincrement, " 
        + COLUMN_YOUCHATID + " text not null, " 
        + COLUMN_CONTACT + " text not null,"
        + COLUMN_CONTACT_PICTURE_URL + " text,"
        + COLUMN_CONTACT_PICTURE +" blob,"
        + COLUMN_LAST_MODIFIED_DATE + " text );";

    public static void onCreate(SQLiteDatabase database) {
      database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
        int newVersion) {
      Log.w(TAG, "Upgrading database " + TABLE_YOUCHATCONTACTS +"from version "
          + oldVersion + " to " + newVersion
          + ", which will destroy all old data");
      database.execSQL("DROP TABLE IF EXISTS " +  TABLE_YOUCHATCONTACTS);
      onCreate(database);
    }

}
