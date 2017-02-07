package shishkoam.weather.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by User on 07.02.2017
 */

public class DBHelper extends SQLiteOpenHelper {

    private final static String LAT = "lat";
    private final static String LON = "lon";
    private final static String ID = "id";
    private final static String DATE = "date";
    private final static String REQUEST = "request";


    public DBHelper(Context context) {
        super(context, "myDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create table
        db.execSQL("create table mytable ("
                + ID + " integer primary key autoincrement,"
                + LAT + " double,"
                + LON + " double,"
                + DATE + " long,"
                + REQUEST + " text" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long addData(double lat, double lon, long date, String request) {
        SQLiteDatabase db = getWritableDatabase();
        // create object for data
        ContentValues cv = new ContentValues();
        // prepare pairs to insert
        cv.put(LAT, lat);
        cv.put(LON, lon);
        cv.put(DATE, date);
        cv.put(REQUEST, request);
        //insert object to db
        long rowID = db.insert("mytable", null, cv);
        close();
        return rowID;
    }

    public DBObject readFirstData() {
        SQLiteDatabase db = getWritableDatabase();
        // request all data (cursor) from table
        Cursor c = db.query("mytable", null, null, null, null, null, null);
        DBObject object = null;
        // check that table has data
        if (c.moveToFirst()) {
            // get column index by name
            int idColIndex = c.getColumnIndex(ID);
            int latColIndex = c.getColumnIndex(LAT);
            int lonColIndex = c.getColumnIndex(LON);
            int dateColIndex = c.getColumnIndex(DATE);
            int requestColIndex = c.getColumnIndex(REQUEST);

            // get data by column indexes
            int id = c.getInt(idColIndex);
            double lat = c.getDouble(latColIndex);
            double lon = c.getDouble(lonColIndex);
            long date = c.getLong(dateColIndex);
            String request = c.getString(requestColIndex);
            object = new DBObject(id, lat, lon, date, request);
        }
        c.close();
        close();
        return object;
    }

    public int clearDataBase() {
        SQLiteDatabase db = getWritableDatabase();
        int clearCount = db.delete("mytable", null, null);
        close();
        return clearCount;
    }
}
