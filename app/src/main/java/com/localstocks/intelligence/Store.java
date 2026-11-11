package com.localstocks.intelligence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Store extends SQLiteOpenHelper {
    public Store(Context c) { super(c, "stock_intelligence.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE leads(id INTEGER PRIMARY KEY AUTOINCREMENT, symbol TEXT NOT NULL, headline TEXT NOT NULL, url TEXT NOT NULL UNIQUE, publisher TEXT, published TEXT, discovered TEXT NOT NULL)");
        db.execSQL("CREATE TABLE calls(id INTEGER PRIMARY KEY AUTOINCREMENT, symbol TEXT NOT NULL, direction TEXT NOT NULL, analyst TEXT NOT NULL, institution TEXT, source_url TEXT NOT NULL, source_key TEXT NOT NULL, published TEXT NOT NULL, captured TEXT NOT NULL, entry REAL NOT NULL, target REAL NOT NULL, stop REAL NOT NULL, UNIQUE(symbol,direction,source_key,published))");
        db.execSQL("CREATE TABLE prices(symbol TEXT NOT NULL, day TEXT NOT NULL, open REAL NOT NULL, high REAL NOT NULL, low REAL NOT NULL, close REAL NOT NULL, PRIMARY KEY(symbol,day))");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int oldVersion,int newVersion) {}
    public boolean saveLead(String symbol,String title,String url,String publisher,String published,String discovered) {
        ContentValues v=new ContentValues();
        v.put("symbol",symbol.toUpperCase(Locale.ROOT));v.put("headline",title);v.put("url",url);
        v.put("publisher",publisher);v.put("published",published);v.put("discovered",discovered);
        return getWritableDatabase().insertWithOnConflict("leads",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    public boolean saveCall(String symbol,String direction,String analyst,String institution,String url,String publication,String captured,double entry,double target,double stop) {
        String key=normalize(institution.isEmpty()?analyst:institution+" / "+analyst);
        ContentValues v=new ContentValues();
        v.put("symbol",symbol.trim().toUpperCase(Locale.ROOT));v.put("direction",direction);v.put("analyst",analyst.trim());
        v.put("institution",institution.trim());v.put("source_url",url.trim());v.put("source_key",key);
        v.put("published",publication);v.put("captured",captured);v.put("entry",entry);v.put("target",target);v.put("stop",stop);
        return getWritableDatabase().insertWithOnConflict("calls",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    public void upsertPrice(String symbol,String day,double open,double high,double low,double close) {
        ContentValues v=new ContentValues();v.put("symbol",symbol.trim().toUpperCase(Locale.ROOT));v.put("day",day);
        v.put("open",open);v.put("high",high);v.put("low",low);v.put("close",close);
        getWritableDatabase().insertWithOnConflict("prices",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<Lead> leads() {
        List<Lead> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT symbol,headline,url,publisher,published,discovered FROM leads ORDER BY id DESC LIMIT 150",null)) {
            while(c.moveToNext())out.add(new Lead(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5)));
        }return out;
    }
    public List<Call> calls() {
        List<Call> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT symbol,direction,analyst,institution,source_url,source_key,published,entry,target,stop FROM calls ORDER BY published DESC,id DESC",null)) {
            while(c.moveToNext())out.add(new Call(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getDouble(7),c.getDouble(8),c.getDouble(9)));
        }return out;
    }
    public List<Bar> bars(String symbol,String after) {
        List<Bar> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT day,open,high,low,close FROM prices WHERE symbol=? AND day>=? ORDER BY day",new String[]{symbol,after.substring(0,10)})) {
            while(c.moveToNext())out.add(new Bar(c.getString(0),c.getDouble(1),c.getDouble(2),c.getDouble(3),c.getDouble(4)));
        }return out;
    }
    public static String normalize(String v) {return v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
    public static final class Lead {
        public final String symbol,title,url,publisher,published,discovered;
        Lead(String s,String t,String u,String p,String date,String seen){symbol=s;title=t;url=u;publisher=p;published=date;discovered=seen;}
    }
    public static final class Call {
        public final String symbol,direction,analyst,institution,url,sourceKey,published;
        public final double entry,target,stop;
        Call(String s,String d,String a,String i,String u,String k,String p,double e,double t,double st){symbol=s;direction=d;analyst=a;institution=i;url=u;sourceKey=k;published=p;entry=e;target=t;stop=st;}
    }
    public static final class Bar {
        public final String day;public final double open,high,low,close;
        Bar(String d,double o,double h,double l,double c){day=d;open=o;high=h;low=l;close=c;}
    }
}
