package com.acesse.youchat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class YouChatContactsHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME ="youchatcontacts.db";
    private static final int DATABASE_VERSION =1;
   
    public YouChatContactsHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }


    @Override
    public void onCreate(SQLiteDatabase db) {
        YouChatContactsTable.onCreate(db);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        YouChatContactsTable.onUpgrade(db, oldVersion, newVersion);
    }

}
