
package com.android.calendar;


import java.io.InputStream;

import com.android.calendar.Vcalendar.VCalendarsinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface.OnClickListener;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.pim.EventRecurrence;
import android.provider.Calendar;
import android.provider.Calendar.Events;
import android.provider.Calendar.Reminders;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class ImportVcalendar extends Activity
{
  public static final String TAG = "EventInfoActivity";
  AlertDialog mDlg;
  Uri mUri = null;
  ContentResolver mContentResolver = null;
  private long mEventId;
  private long mStartMillis;
  private long mEndMillis;
  
  public void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    Uri uri = getIntent().getData();
    this.mUri = uri;
    this.mContentResolver = getContentResolver();
    Log.d(TAG, "ImportVcalendar:" + mUri.toString());

    
    mDlg =new AlertDialog.Builder(ImportVcalendar.this)  
           .setTitle(R.string.import_vcalendar) 
           .setMessage(R.string.import_vcalendar) 
           .setNegativeButton(android.R.string.cancel,new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
               finish();
               }
           })
           .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                   Vcalendar nVcalendar = new Vcalendar(ImportVcalendar.this);
                   VcalendarInfo minfo = new VcalendarInfo();

                   nVcalendar.VcalendarImport(mContentResolver,mUri,minfo);
                   if(minfo.starttime == 0){
                       Time time = new Time();
                       time.setToNow();
                       minfo.starttime = time.toMillis(false);
                       minfo.endtime = time.toMillis(false) + 3600000;
                   }
                   
                   mStartMillis = minfo.starttime ;
                   mEndMillis = minfo.endtime;
                   
                   if((mEndMillis-mStartMillis)%(24 * 3600000)==0)
                   {
                       minfo.allDay= true ;
                   }else{
                       minfo.allDay= false ;
                   }
                   Uri eventUri = doInsert(minfo,getDefaultCalendarId());
                   mEventId = ContentUris.parseId(eventUri);
                   doEdit();
                   if(minfo.hasAlarm){
                   saveReminders(mEventId,minfo.AlarmMinute);
                   }
                   
               }}) 
           .show(); 

  }
  
  protected int getDefaultCalendarId() {
      Cursor calendarsCursor;
      int defaultCalendarId ;
      calendarsCursor = getContentResolver().query(Calendar.Calendars.CONTENT_URI, null, null, null, null);

      if (calendarsCursor != null && calendarsCursor.getCount() > 0) {
        calendarsCursor.moveToNext();
        defaultCalendarId = calendarsCursor.getInt(calendarsCursor.getColumnIndex("_id"));
        calendarsCursor.close();
      }else{
           defaultCalendarId= insertDefaultCalendar("sprdcalendar",Time.getCurrentTimezone(),"sprduser@m.google.com");
      }
      
     return defaultCalendarId ;
  }      

    private int insertDefaultCalendar(String name, String timezone, String account) {
        ContentValues m = new ContentValues();
        m.put(Calendar.Calendars.NAME, name);
        m.put(Calendar.Calendars.DISPLAY_NAME, name);
        m.put(Calendar.Calendars.COLOR, "0");
        m.put(Calendar.Calendars.TIMEZONE, timezone);
        m.put(Calendar.Calendars.SELECTED, 1);
        m.put(Calendar.Calendars.HIDDEN, 1);
        //m.put(Calendar.Calendars.URL, "http://www.google.com/calendar");
        m.put(Calendar.Calendars.OWNER_ACCOUNT, account);
        m.put(Calendar.Calendars._SYNC_ACCOUNT,  account);
        m.put(Calendar.Calendars._SYNC_ACCOUNT_TYPE,"com.android.exchange");
        m.put(Calendar.Calendars.SYNC_EVENTS,  0);
        m.put(Calendar.Calendars.ACCESS_LEVEL, Calendar.Calendars.OWNER_ACCESS);
        Uri url = getContentResolver().insert(Calendar.Calendars.CONTENT_URI, m);
        String id = url.getLastPathSegment();
        Log.d(TAG, "insertDefaultCalendar:" + id);  
        return Integer.parseInt(id);
    }
    
  private void doEdit() {
      Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
      Intent intent = new Intent(Intent.ACTION_EDIT, uri);
      intent.putExtra(Calendar.EVENT_BEGIN_TIME, mStartMillis);
      intent.putExtra(Calendar.EVENT_END_TIME, mEndMillis);
      intent.setClass(this, EditEvent.class);
      startActivity(intent);
      finish();
  }
  
  
  private Uri doInsert(VcalendarInfo info,int calendarId) {

      ContentResolver cr = getContentResolver();
      Log.d(TAG, "doInsert calendarId :" + calendarId);  
      Log.d(TAG, "doInsert VcalendarInfo:" + info);  
      ContentValues values = new ContentValues();

      values.put(Events.TITLE, info.eventitle);
      values.put(Events.CALENDAR_ID, calendarId);
      values.put(Events.DTSTART, info.starttime);
      values.put(Events.DTEND, info.endtime);
      values.put(Events.RRULE, info.rRule);
      values.put(Events.DESCRIPTION, info.description);
      values.put(Events.EVENT_TIMEZONE, info.timezone);
      values.put(Events.EVENT_LOCATION, info.location);
      values.put(Events.ALL_DAY, info.allDay ? 1 : 0);
      values.put(Events.HAS_ALARM, info.hasAlarm ? 1 : 0);
      
      // Create a recurrence exception
       return cr.insert(Events.CONTENT_URI, values);
      }
  
   boolean saveReminders( long eventId,String reminderMinutes) {
       
     if (TextUtils.isEmpty(reminderMinutes)) return false ;
     
      ContentValues values = new ContentValues();

      String[] alarmstr  = reminderMinutes.trim().split(";");
      
      for(String i:alarmstr){
          int minutes = Integer.parseInt(i);
          values.clear();
          values.put(Reminders.MINUTES, minutes);
          values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
          values.put(Reminders.EVENT_ID, eventId);
          getContentResolver().insert(Reminders.CONTENT_URI,values);
      }
      return true;
  }
   
}