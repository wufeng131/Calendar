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

import static android.provider.Calendar.EVENT_BEGIN_TIME;
import static android.provider.Calendar.EVENT_END_TIME;
import dalvik.system.VMRuntime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Calendar.Events;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class AgendaActivity extends Activity implements Navigator,
        DatePickerDialog.OnDateSetListener{

    private static final String TAG = "AgendaActivity";

    private static final int MENU_GROUP_AGENDA = 1;
    private static final int MENU_GROUP_DAY = 2;
    private static final int MENU_GROUP_WEEK = 3;
    private static final int MENU_GROUP_MONTH = 4;
    private static final int MENU_GROUP_EVENT_CREATE = 5;
    private static final int MENU_GROUP_TODAY = 6;
    private static final int MENU_GROUP_SELECT_CALENDARS = 7;
    private static final int MENU_GROUP_PREFERENCES = 8;
    private static final int MENU_GROUP_DELETE_ALL_EVENTS = 10;//add for bug 11161
    private static final int MENU_GROUP_SELECT_DATE = 11;//add for bug 16313
    private static final int MENU_GROUP_SEARCH_AGENDA = 12;

    public static final int MENU_GOTO_TODAY = 1;
    public static final int MENU_AGENDA = 2;
    public static final int MENU_DAY = 3;
    public static final int MENU_WEEK = 4;
    public static final int MENU_EVENT_VIEW = 5;
    public static final int MENU_EVENT_CREATE = 6;
    public static final int MENU_EVENT_EDIT = 7;
    public static final int MENU_EVENT_DELETE = 8;
    public static final int MENU_MONTH = 9;
    public static final int MENU_SELECT_CALENDARS = 10;
    public static final int MENU_PREFERENCES = 11;
    private static final int MENU_AGENDA_DELETE_ALL_EVENTS = 13; //add for bug 11161
    private static final int MENU_SELECT_DATE = 14;//add for bug 16313
    private static final int MENU_SEARCH_AGENDA = 15;
    private static boolean DEBUG = false;

    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";

    private static final long INITIAL_HEAP_SIZE = 4*1024*1024;

    private ContentResolver mContentResolver;

    private AgendaListView mAgendaListView;

    private Time mTime;

    private String mTitle;
    
    private static final int DIALOG_DATEPICKER = 0;
    //boat add by pengwufeng 20131010
    private final static String TITLE_BTN_ACTION = "title_btn_action";

    // This gets run if the time zone is updated in the db
    private Runnable mUpdateTZ = new Runnable() {
        @Override
        public void run() {
            long time = mTime.toMillis(true);
            mTime = new Time(Utils.getTimeZone(AgendaActivity.this, this));
            mTime.set(time);
            updateTitle();
        }
    };

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_CHANGED)
                    || action.equals(Intent.ACTION_DATE_CHANGED)
                    || action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                mAgendaListView.refresh(true);
            }
        }
    };

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            mAgendaListView.refresh(true);
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Eliminate extra GCs during startup by setting the initial heap size to 4MB.
        // TODO: We should restore the old heap size once the activity reaches the idle state
        VMRuntime.getRuntime().setMinimumHeapSize(INITIAL_HEAP_SIZE);

        mAgendaListView = new AgendaListView(this);
        setContentView(mAgendaListView);

        mContentResolver = getContentResolver();

        mTitle = getResources().getString(R.string.agenda_view);

        long millis = 0;
        mTime = new Time(Utils.getTimeZone(this, mUpdateTZ));
        if (icicle != null) {
            // Returns 0 if key not found
            millis = icicle.getLong(BUNDLE_KEY_RESTORE_TIME);
            if (DEBUG) {
                Log.v(TAG, "Restore value from icicle: " + millis);
            }
        }

        if (millis == 0) {
            // Returns 0 if key not found
            millis = getIntent().getLongExtra(EVENT_BEGIN_TIME, 0);
            if (DEBUG) {
                Time time = new Time();
                time.set(millis);
                Log.v(TAG, "Restore value from intent: " + time.toString());
            }
        }

        if (millis == 0) {
            if (DEBUG) {
                Log.v(TAG, "Restored from current time");
            }
            millis = System.currentTimeMillis();
        }
        mTime.set(millis);
        updateTitle();
        registerBoradcastReceiver();//boat add by pengwufeng 20131010
    }
    
  //boat add by pengwufeng start 20131010
    public void registerBoradcastReceiver(){ 
        IntentFilter myIntentFilter = new IntentFilter(); 
        myIntentFilter.addAction(TITLE_BTN_ACTION); 
        registerReceiver(titleBtnOnclickReceiver, myIntentFilter); 
    } 
    
    private BroadcastReceiver titleBtnOnclickReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(TITLE_BTN_ACTION)) {
				String tabTag = intent.getStringExtra("curTab");
				if(tabTag.equals("AGENDA")) {
					String clickBtn = intent.getStringExtra("onClickBtn");
					if(clickBtn.equals("btn_add")) {
						long startMillis = AgendaActivity.this.getSelectedTime();
		                long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
		                Intent mIntent = new Intent(Intent.ACTION_EDIT);
		                mIntent.setClassName(AgendaActivity.this, EditEvent.class.getName());
		                mIntent.putExtra(EVENT_BEGIN_TIME, startMillis);
		                mIntent.putExtra(EVENT_END_TIME, endMillis);
		                mIntent.putExtra(EditEvent.EVENT_ALL_DAY, AgendaActivity.this.getAllDay());
		                startActivity(mIntent);
					} else if(clickBtn.equals("btn_today")) {
						AgendaActivity.this.goToToday();
					}
				}
			}
		}
	};
    //boat add by pengwufeng end 20131010

    private void updateTitle() {
        StringBuilder title = new StringBuilder(mTitle);
        String tz = Utils.getTimeZone(this, mUpdateTZ);
        if (!TextUtils.equals(tz, Time.getCurrentTimezone())) {
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(this)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            boolean isDST = mTime.isDst != 0;
            long start = System.currentTimeMillis();
            TimeZone timeZone = TimeZone.getTimeZone(tz);
            title.append(" (").append(Utils.formatDateRange(this, start, start, flags)).append(" ")
                    .append(timeZone.getDisplayName(isDST, TimeZone.SHORT, Locale.getDefault()))
                    .append(")");
        }
        setTitle(title.toString());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        long time = Utils.timeFromIntentInMillis(intent);
        if (time > 0) {
            mTime.set(time);
            goTo(mTime, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) {
            Log.v(TAG, "OnResume to " + mTime.toString());
        }

        SharedPreferences prefs = CalendarPreferenceActivity.getSharedPreferences(
                getApplicationContext());
        boolean hideDeclined = prefs.getBoolean(
                CalendarPreferenceActivity.KEY_HIDE_DECLINED, false);

        mAgendaListView.setHideDeclinedEvents(hideDeclined);
        mAgendaListView.goTo(mTime, true);
        mAgendaListView.onResume();

        // Register for Intent broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mIntentReceiver, filter);

        mContentResolver.registerContentObserver(Events.CONTENT_URI, true, mObserver);
        mUpdateTZ.run();

        // Record Agenda View as the (new) default detailed view.
        Utils.setDefaultView(this, CalendarApplication.AGENDA_VIEW_ID);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        long firstVisibleTime = mAgendaListView.getFirstVisibleTime();
        if (firstVisibleTime > 0) {
            mTime.set(firstVisibleTime);
            outState.putLong(BUNDLE_KEY_RESTORE_TIME, firstVisibleTime);
            if (DEBUG) {
                Log.v(TAG, "onSaveInstanceState " + mTime.toString());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mAgendaListView.onPause();
        mContentResolver.unregisterContentObserver(mObserver);
        unregisterReceiver(mIntentReceiver);
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(titleBtnOnclickReceiver);//boat add by pengwufeng 20131011
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuHelper.onPrepareOptionsMenu(this, menu);

          menu.setGroupVisible(MENU_GROUP_AGENDA, true);
          menu.setGroupEnabled(MENU_GROUP_AGENDA, true);

          menu.setGroupVisible(MENU_GROUP_DAY, true);
          menu.setGroupEnabled(MENU_GROUP_DAY, true);

          menu.setGroupVisible(MENU_GROUP_WEEK, true);
          menu.setGroupEnabled(MENU_GROUP_WEEK, true);

          menu.setGroupVisible(MENU_GROUP_MONTH, true);
          menu.setGroupEnabled(MENU_GROUP_MONTH, true);

//      Account[] accounts = AccountManager.get(activity).getAccounts();
//      if(accounts.length > 0){
          menu.setGroupVisible(MENU_GROUP_SELECT_CALENDARS, true);
          menu.setGroupEnabled(MENU_GROUP_SELECT_CALENDARS, true);
//      }else{
//          menu.setGroupVisible(MENU_GROUP_SELECT_CALENDARS, false);
//          menu.setGroupEnabled(MENU_GROUP_SELECT_CALENDARS, false);
//      }
      return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuHelper.onCreateOptionsMenu(menu, this);
        MenuItem item;
        //boat modify by pengwufeng start 20131010
//        item = menu.add(MENU_GROUP_DAY, MENU_DAY, 0, R.string.day_view);
//        item.setIcon(android.R.drawable.ic_menu_day);
//        item.setAlphabeticShortcut('d');
//
//        item = menu.add(MENU_GROUP_WEEK, MENU_WEEK, 0, R.string.week_view);
//        item.setIcon(android.R.drawable.ic_menu_week);
//        item.setAlphabeticShortcut('w');
//
//        item = menu.add(MENU_GROUP_MONTH, MENU_MONTH, 0, R.string.month_view);
//        item.setIcon(android.R.drawable.ic_menu_month);
//        item.setAlphabeticShortcut('m');
//
//        item = menu.add(MENU_GROUP_AGENDA, MENU_AGENDA, 0, R.string.agenda_view);
//        item.setIcon(android.R.drawable.ic_menu_agenda);
//        item.setAlphabeticShortcut('a');
//
//        item = menu.add(MENU_GROUP_TODAY, MENU_GOTO_TODAY, 0, R.string.goto_today);
//        item.setIcon(android.R.drawable.ic_menu_today);
//        item.setAlphabeticShortcut('t');
//
//        item = menu.add(MENU_GROUP_EVENT_CREATE, MENU_EVENT_CREATE, 0, R.string.event_create);
//        item.setIcon(android.R.drawable.ic_menu_add);
//        item.setAlphabeticShortcut('n');

        item = menu.add(MENU_GROUP_SELECT_DATE,MENU_SELECT_DATE,0,R.string.select_date);
        item.setIcon(getResources().getDrawable(R.drawable.cal_menu_goto));
        item = menu.add(MENU_GROUP_DELETE_ALL_EVENTS,MENU_AGENDA_DELETE_ALL_EVENTS,0,R.string.delete_all_events);
        item.setIcon(getResources().getDrawable(R.drawable.cal_menu_delete));
        item = menu.add(MENU_GROUP_SEARCH_AGENDA,MENU_SEARCH_AGENDA,0,R.string.search);
        item.setIcon(getResources().getDrawable(R.drawable.cal_menu_search));
//        item = menu.add(MENU_GROUP_SELECT_CALENDARS, MENU_SELECT_CALENDARS,
//                0, R.string.menu_select_calendars);
//        item.setIcon(android.R.drawable.ic_menu_manage);

        item = menu.add(MENU_GROUP_PREFERENCES, MENU_PREFERENCES, 0, R.string.menu_preferences);
        item.setIcon(getResources().getDrawable(R.drawable.cal_menu_settings));
        item.setAlphabeticShortcut('p');

        //boat modify by pengwufeng end 20131010
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        MenuHelper.onOptionsItemSelected(this, item, this);
        switch (item.getItemId()) {
            case MENU_SELECT_CALENDARS: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(AgendaActivity.this, SelectCalendarsActivity.class);
                startActivity(intent);
                return true;
            }
            case MENU_GOTO_TODAY:
                AgendaActivity.this.goToToday();
                return true;
            case MENU_PREFERENCES:
                Utils.startActivity(AgendaActivity.this, CalendarPreferenceActivity.class.getName(), AgendaActivity.this.getSelectedTime());
                return true;
            case MENU_AGENDA:
                Utils.startActivity(AgendaActivity.this, AgendaActivity.class.getName(), AgendaActivity.this.getSelectedTime());
                return true;
            case MENU_DAY:
                Utils.startActivity(AgendaActivity.this, DayActivity.class.getName(), AgendaActivity.this.getSelectedTime());
                return true;
            case MENU_WEEK:
                Utils.startActivity(AgendaActivity.this, WeekActivity.class.getName(), AgendaActivity.this.getSelectedTime());
                return true;
            case MENU_MONTH:
                Utils.startActivity(AgendaActivity.this, MonthActivity.class.getName(), AgendaActivity.this.getSelectedTime());
                return true;
            case MENU_EVENT_CREATE: {
                long startMillis = AgendaActivity.this.getSelectedTime();
                long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setClassName(AgendaActivity.this, EditEvent.class.getName());
                intent.putExtra(EVENT_BEGIN_TIME, startMillis);
                intent.putExtra(EVENT_END_TIME, endMillis);
                intent.putExtra(EditEvent.EVENT_ALL_DAY, AgendaActivity.this.getAllDay());
                startActivity(intent);
                return true;
            }
            case MENU_SELECT_DATE:
                AgendaActivity.this.selectDate();
                return true;

            case MENU_SEARCH_AGENDA:
                onSearchRequested();
                return true;
            case MENU_AGENDA_DELETE_ALL_EVENTS:
                    new AlertDialog.Builder(AgendaActivity.this)
                    .setTitle(R.string.delete_title)
                    .setMessage(R.string.delete_all_event_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    execEventsDel(AgendaActivity.this);
                                }
                            }).setNegativeButton(android.R.string.cancel, null).show();
                    return true;
            }
        return super.onOptionsItemSelected(item);
    }

    private static void execEventsDel(Activity activity) {
        final DeleteEventHelper mDeleteEventHelper = new DeleteEventHelper(activity, false);
        mDeleteEventHelper.deleteAllEvents();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                // Delete the currently selected event (if any)
                mAgendaListView.deleteSelectedEvent();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /* Navigator interface methods */
    public void goToToday() {
        Time now = new Time(Utils.getTimeZone(this, mUpdateTZ));
        now.setToNow();
        mAgendaListView.goTo(now, true); // Force refresh
    }

    public void goTo(Time time, boolean animate) {
        mAgendaListView.goTo(time, false);
    }

    public long getSelectedTime() {
        return mAgendaListView.getSelectedTime();
    }

    public boolean getAllDay() {
        return false;
    }
    
    public void selectDate() {
	    removeDialog(DIALOG_DATEPICKER);
	    showDialog(DIALOG_DATEPICKER);
    }
    
    public void goToSelectDay(long selectTime){
    	Time when = new Time(Utils.getTimeZone(this, mUpdateTZ));
    	when.set(selectTime);
//    	when.setToNow();
        mAgendaListView.goTo(when, true); // Force refresh
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
        Dialog d;
        switch (id) {
        case DIALOG_DATEPICKER: {
            final Calendar calendar = Calendar.getInstance();
            d = new DatePickerDialog(
                this,
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
            break;
        }
        default:
            d = null;
            break;
        }
        return d;
    }
    
    public void onDateSet(DatePicker view, int year, int month, int day) {
    Calendar c = Calendar.getInstance();

    //add for bug 20327 20328 begin
    if(year > 2037 || year < 1970){
        Toast toast = Toast.makeText(this, R.string.out_range, Toast.LENGTH_SHORT);
        toast.show();
        return;
    }else{
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();
        goToSelectDay(when);
        }
    //add for bug 20327 20328 end
    }
}

