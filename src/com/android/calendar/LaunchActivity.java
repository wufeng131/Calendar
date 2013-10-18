/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Calendar;
import android.text.format.Time;
import android.util.Log;

public class LaunchActivity extends Activity {
    private static final String TAG = "LaunchActivity";

    static final String KEY_DETAIL_VIEW = "DETAIL_VIEW";
    static final String KEY_VIEW_TYPE = "VIEW";
    static final String VIEW_TYPE_DAY = "DAY";
    static final int CALENDAR_ADD = 1;
    static final int CALENDAR_DEL = 2;
    private Bundle mExtras;
    private static final String[] PROJECTION= new String[]{Calendar.Calendars._ID,
        Calendar.Calendars.DISPLAY_NAME,
        Calendar.Calendars.OWNER_ACCOUNT};
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mExtras = getIntent().getExtras();

        // Our UI is not something intended for the user to see.  We just
        // stick around until we can figure out what to do next based on
        // the current state of the system.
        // Removed because it causes draw problems when entering in landscape orientation
        // TODO: Figure out draw problem. Original reason for removal due to b/2008662
        // setVisible(false);

        // Only try looking for an account if this is the first launch.
        if (icicle == null) {
            Account[] accounts = AccountManager.get(this).getAccounts();
            if(accounts.length > 0) {
                // If the only account is an account that can't use Calendar we let the user into
                // Calendar, but they can't create any events until they add an account with a
                // Calendar.
//                 checkDefaultCalendar(CALENDAR_DEL);
                 launchCalendarView();
            } else {
                // If we failed to find a valid Calendar, bounce the user to the account settings
                // screen. Using the Calendar authority has the added benefit of only showing
                // account types that use Calendar when you enter the add account screen from here.
               /* final Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {
                    Calendar.AUTHORITY
                });
                startActivityForResult(intent, 0);*/
//                checkDefaultCalendar(CALENDAR_ADD);
                launchCalendarView();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Account[] accounts = AccountManager.get(this).getAccounts();
        if(accounts.length > 0) {
            // If the only account is an account that can't use Calendar we let the user into
            // Calendar, but they can't create any events until they add an account with a
            // Calendar.
            launchCalendarView();
        } else {
            finish();
        }
    }

    private  void checkDefaultCalendar(int flag){
        String account , name;
        long CalendarId;

        final ContentResolver cr = getContentResolver();
        Cursor c = cr.query(Calendar.Calendars.CONTENT_URI,PROJECTION,
            null, null, Calendar.Calendars.DEFAULT_SORT_ORDER);
        Log.i(TAG, "checkDefaultCalendar " +flag+Time.getCurrentTimezone());
         if (c != null && c.getCount() > 0) {
                try {
                    Log.i(TAG, "moveToFirst c != null");
                    if (c.moveToFirst()) {
                          Log.i(TAG, "moveToFirst true" );
                         do {
                                CalendarId = c.getLong(0);
                                name = c.getString(1);
                                account = c.getString(2);
                                Log.i(TAG, "checkDefaultCalendar: "+name+" account:"+account );
                                if("sprdcalendar".equals(name)&&"sprduser@m.google.com".equals(account)){
                                     Log.i(TAG, "del default sprdcalendar " + CalendarId);
                                    if(flag==CALENDAR_DEL) cr.delete(Calendar.Calendars.CONTENT_URI, String.valueOf(CalendarId),new String[] {"sprduser@m.google.com"});
                                    break;
                                }
                        } while (c.moveToNext());
                    }
                } finally {
                    c.close();
                }
            }else{
                if(flag==CALENDAR_ADD){
                    int  id =  insertCalendar("sprdcalendar",Time.getCurrentTimezone(),"sprduser@m.google.com");
                    Log.i(TAG, "add  default sprdcalendar " + id);
                }
            }
    }
    
    private int insertCalendar(String name, String timezone, String account) {
        ContentValues m = new ContentValues();
        m.put(Calendar.Calendars.NAME, name);
        m.put(Calendar.Calendars.DISPLAY_NAME, name);
        m.put(Calendar.Calendars.COLOR, "-14069085");
        m.put(Calendar.Calendars.TIMEZONE, timezone);
        m.put(Calendar.Calendars.SELECTED, 1);
        m.put(Calendar.Calendars.HIDDEN, 0);
        //m.put(Calendar.Calendars.URL, "http://www.google.com/calendar");
        m.put(Calendar.Calendars.OWNER_ACCOUNT, account);
        m.put(Calendar.Calendars._SYNC_ACCOUNT,  account);
        m.put(Calendar.Calendars._SYNC_ACCOUNT_TYPE,"com.android.exchange");
        m.put(Calendar.Calendars.SYNC_EVENTS,  1);
        m.put(Calendar.Calendars.ACCESS_LEVEL, "700");
        Uri url = getContentResolver().insert(Calendar.Calendars.CONTENT_URI, m);
        String id = url.getLastPathSegment();
        return Integer.parseInt(id);
    }

    private void launchCalendarView() {
        // Get the data for from this intent, if any
        Intent myIntent = getIntent();
        Uri myData = myIntent.getData();

        // Set up the intent for the start activity
        Intent intent = new Intent();
        if (myData != null) {
            intent.setData(myData);
        }

        String defaultViewKey = CalendarPreferenceActivity.KEY_START_VIEW;
        if (mExtras != null) {
            intent.putExtras(mExtras);
            if (mExtras.getBoolean(KEY_DETAIL_VIEW, false)) {
                defaultViewKey = CalendarPreferenceActivity.KEY_DETAILED_VIEW;
            } else if (VIEW_TYPE_DAY.equals(mExtras.getString(KEY_VIEW_TYPE))) {
                defaultViewKey = VIEW_TYPE_DAY;
            }
        }
        intent.putExtras(myIntent);

        SharedPreferences prefs = CalendarPreferenceActivity.getSharedPreferences(this);
        String startActivity;
        if (defaultViewKey.equals(VIEW_TYPE_DAY)) {
            startActivity = CalendarApplication.ACTIVITY_NAMES[CalendarApplication.DAY_VIEW_ID];
        } else if (defaultViewKey.equals(CalendarPreferenceActivity.KEY_DETAILED_VIEW)) {
            startActivity = prefs.getString(defaultViewKey,
                    CalendarPreferenceActivity.DEFAULT_DETAILED_VIEW);
        } else {
            startActivity = prefs.getString(defaultViewKey,
                    CalendarPreferenceActivity.DEFAULT_START_VIEW);
        }

        intent.setClassName(this, startActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        startActivity(intent);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
