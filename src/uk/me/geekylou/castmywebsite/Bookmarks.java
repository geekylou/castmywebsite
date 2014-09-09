package uk.me.geekylou.castmywebsite;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.util.Log;

public class Bookmarks extends SQLiteOpenHelper 
{
    private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "timeline.db";
	
	private static final String TABLE_NAME = "entries";

	Bookmarks(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

	public void onCreate(SQLiteDatabase db) {
		
		db.execSQL("CREATE TABLE " + TABLE_NAME + " ("+
				"_id INTEGER PRIMARY KEY," +
				"name CHAR(40)," +
				"url  TEXT," +
				"icon BLOB);");
    }
	
	public BookmarkWrapper getEntry(int id)
	{
		SQLiteDatabase db = getWritableDatabase();
		
		Cursor c;
		String args[];
		BookmarkWrapper entry = null;
		
		c = db.rawQuery("SELECT * FROM entries WHERE _id='"+Integer.toString(id)+"'", null);
		
		if (c.moveToNext())
		{
			entry = new BookmarkWrapper();
			
			entry.mName = c.getString(c.getColumnIndex("name"));
			Log.d("CastMyWebsite", "Bookmarks.toArrayAdapter " + entry.mName);

				try {
					entry.mFile = new URL(c.getString(c.getColumnIndex("url")));
					entry.id = c.getInt(c.getColumnIndex("_id"));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					db.close();
					return null;
				}
		}
		db.close();
		
		return entry;
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
	
	public void insertEntry(BookmarkWrapper entry)
	{
		SQLiteDatabase db = getWritableDatabase();
		
		/* Store timeline entry. */
		ContentValues values = new ContentValues();
		
		values.put("name", entry.mName);
		values.put("url", entry.mFile.toExternalForm());
		db.insert("entries", null, values);
		db.close();
	}
	
	public void updateEntry(BookmarkWrapper entry)
	{
		SQLiteDatabase db = getWritableDatabase();
		
		ContentValues values = new ContentValues();

		values.put("name", entry.mName);
		values.put("url", entry.mFile.toExternalForm());

		db.update("entries", values, "_id='"+Integer.toString(entry.id)+"'", null);
							
		db.close();
	}

	public void deleteEntry(BookmarkWrapper entry)
	{
		SQLiteDatabase db = getWritableDatabase();

		db.delete("entries","_id='"+Integer.toString(entry.id)+"'", null);
	}
	
	public ArrayAdapter<BookmarkWrapper> toArrayAdapter(ArrayAdapter<BookmarkWrapper> mTimelineArrayAdapter)
	{
		SQLiteDatabase db = getWritableDatabase();
		
		Cursor c;
		String args[];
		
		c = db.rawQuery("SELECT * FROM entries", null);
		
		while (c.moveToNext())
		{
			BookmarkWrapper entry = new BookmarkWrapper();
			
			entry.mName = c.getString(c.getColumnIndex("name"));
			Log.d("CastMyWebsite", "Bookmarks.toArrayAdapter " + entry.mName);
			try {
				entry.mFile = new URL(c.getString(c.getColumnIndex("url")));
				entry.id = c.getInt(c.getColumnIndex("_id"));
								
				mTimelineArrayAdapter.add(entry);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		db.close();
		
		return mTimelineArrayAdapter;
	}	
}