/*
 * Copyright (C) 2006 The Android Open Source Project
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.DatePicker;
import android.widget.Gallery.LayoutParams;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MonthActivity extends Activity implements ViewSwitcher.ViewFactory,
        Navigator, AnimationListener,
        DatePickerDialog.OnDateSetListener{
    private static final int INITIAL_HEAP_SIZE = 4 * 1024 * 1024;
    private Animation mInAnimationPast;
    private Animation mInAnimationFuture;
    private Animation mOutAnimationPast;
    private Animation mOutAnimationFuture;
    private ViewSwitcher mSwitcher;
    private Time mTime;

    private ContentResolver mContentResolver;
    EventLoader mEventLoader;
    private int mStartDay;

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

//    private ProgressBar mProgressBar;
    //boat add by pengwufeng start 20131008
    private Button boatBtLastMonth;
    private Button boatBtNextMonth;
    String[] month_nos;
    private final static String TITLE_BTN_ACTION = "title_btn_action";//boat add by pengwufeng 20131011
    //boat add by pengwufeng end 20131008
    private static final int DIALOG_DATEPICKER = 0;

    // This gets run if the time zone is updated in the db
    private Runnable mUpdateTZ = new Runnable() {
        @Override
        public void run() {
            // We want mTime to stay on the same day, so we swap the tz
            mTime.timezone = Utils.getTimeZone(MonthActivity.this, this);
            mTime.normalize(true);
            updateTitle(mTime);
        }
    };

    private static final int DAY_OF_WEEK_LABEL_IDS[] = {
        R.id.day0, R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6
    };
    private static final int DAY_OF_WEEK_KINDS[] = {
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    };

    protected void startProgressSpinner() {
        // start the progress spinner
//        mProgressBar.setVisibility(View.VISIBLE);
    }

    protected void stopProgressSpinner() {
        // stop the progress spinner
//        mProgressBar.setVisibility(View.GONE);
    }

    /* ViewSwitcher.ViewFactory interface methods */
    public View makeView() {
        MonthView mv = new MonthView(this, this);
        mv.setLayoutParams(new ViewSwitcher.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mv.setSelectedTime(mTime);
        return mv;
    }

    public void updateTitle(Time time) {
        TextView title = (TextView) findViewById(R.id.title);
        StringBuffer date = new StringBuffer(Utils.formatMonthYear(this, time));
        if (!TextUtils.equals(Utils.getTimeZone(this, mUpdateTZ), Time.getCurrentTimezone())) {
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(this)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            long start = System.currentTimeMillis();
            String tz = Utils.getTimeZone(this, mUpdateTZ);
            boolean isDST = time.isDst != 0;
            TimeZone timeZone = TimeZone.getTimeZone(tz);
            date.append(" (").append(Utils.formatDateRange(this, start, start, flags)).append(" ")
                    .append(timeZone.getDisplayName(isDST, TimeZone.SHORT, Locale.getDefault()))
                    .append(")");
        }
        title.setText(date.toString());
    }
    
    //boat add by pengwufeng start 20131010
    public void updateBtn(Time time) {
    	int cm = time.month;
        Log.d("pwf", "curMonth==" + cm);
        if(cm == 0) {
  			boatBtLastMonth.setText(month_nos[11]);
  			boatBtNextMonth.setText(month_nos[cm + 1]);
  		} else if(cm == 11) {
  			boatBtLastMonth.setText(month_nos[cm - 1]);
  			boatBtNextMonth.setText(month_nos[0]);
  		} else {
  			boatBtLastMonth.setText(month_nos[cm - 1]);
  			boatBtNextMonth.setText(month_nos[cm + 1]);
  		}
    }
    //boat add by pengwufeng end 20131010
    
    /* Navigator interface methods */
    public void goTo(Time time, boolean animate) {
        updateTitle(time);
       

        MonthView current = (MonthView) mSwitcher.getCurrentView();
        current.dismissPopup();

        Time currentTime = current.getTime();
        //boat add by pengwufeng start 20131009
        updateBtn(time);
      //boat add by pengwufeng end 20131009
        // Compute a month number that is monotonically increasing for any
        // two adjacent months.
        // This is faster than calling getSelectedTime() because we avoid
        // a call to Time#normalize().
        if (animate) {
            int currentMonth = currentTime.month + currentTime.year * 12;
            int nextMonth = time.month + time.year * 12;
            if (nextMonth < currentMonth) {
                mSwitcher.setInAnimation(mInAnimationPast);
                mSwitcher.setOutAnimation(mOutAnimationPast);
            } else {
                mSwitcher.setInAnimation(mInAnimationFuture);
                mSwitcher.setOutAnimation(mOutAnimationFuture);
            }
        }

        MonthView next = (MonthView) mSwitcher.getNextView();
        next.setSelectionMode(current.getSelectionMode());
        next.setSelectedTime(time);
        next.reloadEvents();
        next.animationStarted();
        mSwitcher.showNext();
        next.requestFocus();
        mTime = time;
    }

    public void goToToday() {
        Time now = new Time(Utils.getTimeZone(this, mUpdateTZ));
        now.set(System.currentTimeMillis());
        now.minute = 0;
        now.second = 0;
        now.normalize(false);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText(Utils.formatMonthYear(this, now));
        mTime = now;

        MonthView view = (MonthView) mSwitcher.getCurrentView();
        view.setSelectedTime(now);
        view.reloadEvents();
        updateBtn(now);//boat add by pengwufeng 20131010
    }

    public void goToSelectDay(long selectTime) {
    	Time when = new Time(Utils.getTimeZone(this, mUpdateTZ));
    	when.set(selectTime);
    	when.minute = 0;
    	when.second = 0;
    	when.normalize(false);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText(Utils.formatMonthYear(this, when));
        mTime = when;

        MonthView view = (MonthView) mSwitcher.getCurrentView();
        view.setSelectedTime(when);
        view.reloadEvents();
    }
    
    public void selectDate() {
	    removeDialog(DIALOG_DATEPICKER);
	    showDialog(DIALOG_DATEPICKER);
    }
    
    public long getSelectedTime() {
        MonthView mv = (MonthView) mSwitcher.getCurrentView();
        return mv.getSelectedTimeInMillis();
    }

    public boolean getAllDay() {
        return false;
    }

    int getStartDay() {
        return mStartDay;
    }

    void eventsChanged() {
        MonthView view = (MonthView) mSwitcher.getCurrentView();
        view.reloadEvents();
    }

    /**
     * Listens for intent broadcasts
     */
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_CHANGED)
                    || action.equals(Intent.ACTION_DATE_CHANGED)
                    || action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                eventsChanged();
            }
        }
    };
    
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
				if(tabTag.equals("MONTH")) {
					String clickBtn = intent.getStringExtra("onClickBtn");
					if(clickBtn.equals("btn_add")) {
						long startMillis = MonthActivity.this.getSelectedTime();
		                long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
		                Intent mIntent = new Intent(Intent.ACTION_EDIT);
		                mIntent.setClassName(MonthActivity.this, EditEvent.class.getName());
		                mIntent.putExtra(EVENT_BEGIN_TIME, startMillis);
		                mIntent.putExtra(EVENT_END_TIME, endMillis);
		                mIntent.putExtra(EditEvent.EVENT_ALL_DAY, MonthActivity.this.getAllDay());
		                startActivity(mIntent);
					} else if(clickBtn.equals("btn_today")) {
						MonthActivity.this.goToToday();
						
					}
				}
			}
		}
	};
    //boat add by pengwufeng end 20131010
    
    // Create an observer so that we can update the views whenever a
    // Calendar event changes.
    private ContentObserver mObserver = new ContentObserver(new Handler())
    {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            eventsChanged();
        }
    };

    public void onAnimationStart(Animation animation) {
    }

    // Notifies the MonthView when an animation has finished.
    public void onAnimationEnd(Animation animation) {
        MonthView monthView = (MonthView) mSwitcher.getCurrentView();
        monthView.animationFinished();
    }

    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Eliminate extra GCs during startup by setting the initial heap size to 4MB.
        // TODO: We should restore the old heap size once the activity reaches the idle state
        VMRuntime.getRuntime().setMinimumHeapSize(INITIAL_HEAP_SIZE);

        setContentView(R.layout.month_activity);
        mContentResolver = getContentResolver();

        long time;
        if (icicle != null) {
            time = icicle.getLong(EVENT_BEGIN_TIME);
        } else {
            time = Utils.timeFromIntentInMillis(getIntent());
        }

        mTime = new Time(Utils.getTimeZone(this, mUpdateTZ));
        mTime.set(time);
        mTime.normalize(true);

        // Get first day of week based on locale and populate the day headers
        mStartDay = Calendar.getInstance().getFirstDayOfWeek();
        int diff = mStartDay - Calendar.SUNDAY - 1;
        final int startDay = Utils.getFirstDayOfWeek();
        final int sundayColor = getResources().getColor(R.color.sunday_text_color);
        final int saturdayColor = getResources().getColor(R.color.saturday_text_color);

        for (int day = 0; day < 7; day++) {
            final String dayString = DateUtils.getDayOfWeekString(
                    (DAY_OF_WEEK_KINDS[day] + diff) % 7 + 1, DateUtils.LENGTH_MEDIUM);
            final TextView label = (TextView) findViewById(DAY_OF_WEEK_LABEL_IDS[day]);
            label.setText(dayString);
            if (Utils.isSunday(day, startDay)) {
                label.setTextColor(sundayColor);
            } else if (Utils.isSaturday(day, startDay)) {
                label.setTextColor(saturdayColor);
            }
        }

        // Set the initial title
        TextView title = (TextView) findViewById(R.id.title);
        title.setText(Utils.formatMonthYear(this, mTime));

        mEventLoader = new EventLoader(this);
//        mProgressBar = (ProgressBar) findViewById(R.id.progress_circular);
      

        mSwitcher = (ViewSwitcher) findViewById(R.id.switcher);
        mSwitcher.setFactory(this);
        mSwitcher.getCurrentView().requestFocus();

        mInAnimationPast = AnimationUtils.loadAnimation(this, R.anim.slide_down_in);
        mOutAnimationPast = AnimationUtils.loadAnimation(this, R.anim.slide_down_out);
        mInAnimationFuture = AnimationUtils.loadAnimation(this, R.anim.slide_up_in);
        mOutAnimationFuture = AnimationUtils.loadAnimation(this, R.anim.slide_up_out);

        mInAnimationPast.setAnimationListener(this);
        mInAnimationFuture.setAnimationListener(this);
      //boat add by pengwufeng start 20131008
        registerBoradcastReceiver();
        month_nos = getResources().getStringArray(R.array.month_btn_number);
        int curMonth = ((MonthView) mSwitcher.getCurrentView()).getCurMonth();
        boatBtLastMonth = (Button) findViewById(R.id.month_bt_left);
        if(curMonth == 0) {
        	boatBtLastMonth.setText(month_nos[11]);
        } else {
        	boatBtLastMonth.setText(month_nos[curMonth - 1]);
        }
        boatBtLastMonth.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				((MonthView) mSwitcher.getCurrentView()).gotoLastMonth();
			}
		});
        boatBtNextMonth = (Button) findViewById(R.id.month_bt_rigjht);
        if(curMonth == 11) {
        	boatBtNextMonth.setText(month_nos[0]);
        } else {
        	boatBtNextMonth.setText(month_nos[curMonth + 1]);
        }
        boatBtNextMonth.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				((MonthView) mSwitcher.getCurrentView()).gotoNextMonth();
			}
		});
        //boat add by pengwufeng end 20131008
    }

    @Override
    protected void onNewIntent(Intent intent) {
        long timeMillis = Utils.timeFromIntentInMillis(intent);
        if (timeMillis > 0) {
            Time time = new Time(Utils.getTimeZone(this, mUpdateTZ));
            time.set(timeMillis);
            goTo(time, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            mEventLoader.stopBackgroundThread();
        }
        mContentResolver.unregisterContentObserver(mObserver);
        unregisterReceiver(mIntentReceiver);

        MonthView view = (MonthView) mSwitcher.getCurrentView();
        view.dismissPopup();
        view = (MonthView) mSwitcher.getNextView();
        view.dismissPopup();
        mEventLoader.stopBackgroundThread();
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(titleBtnOnclickReceiver);//boat add by pengwufeng 20131011
	}

    @Override
    protected void onResume() {
        super.onResume();
        mUpdateTZ.run();
        mEventLoader.startBackgroundThread();
        eventsChanged();

        MonthView view1 = (MonthView) mSwitcher.getCurrentView();
        MonthView view2 = (MonthView) mSwitcher.getNextView();
        SharedPreferences prefs = CalendarPreferenceActivity.getSharedPreferences(this);
        String str = prefs.getString(CalendarPreferenceActivity.KEY_DETAILED_VIEW,
                CalendarPreferenceActivity.DEFAULT_DETAILED_VIEW);
        view1.updateView();
        view2.updateView();
        view1.setDetailedView(str);
        view2.setDetailedView(str);

        // Register for Intent broadcasts
        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mIntentReceiver, filter);

        mContentResolver.registerContentObserver(Events.CONTENT_URI,
                true, mObserver);

        // Record Month View as the (new) start view
        Utils.setDefaultView(this, CalendarApplication.MONTH_VIEW_ID);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EVENT_BEGIN_TIME, mTime.toMillis(true));
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

//        Account[] accounts = AccountManager.get(activity).getAccounts();
//        if(accounts.length > 0){
            menu.setGroupVisible(MENU_GROUP_SELECT_CALENDARS, true);
            menu.setGroupEnabled(MENU_GROUP_SELECT_CALENDARS, true);
//        }else{
//            menu.setGroupVisible(MENU_GROUP_SELECT_CALENDARS, false);
//            menu.setGroupEnabled(MENU_GROUP_SELECT_CALENDARS, false);
//        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuHelper.onCreateOptionsMenu(menu,null);
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
                intent.setClass(MonthActivity.this, SelectCalendarsActivity.class);
                startActivity(intent);
                return true;
            }
            case MENU_GOTO_TODAY:
                MonthActivity.this.goToToday();
                return true;
            case MENU_PREFERENCES:
                Utils.startActivity(MonthActivity.this, CalendarPreferenceActivity.class.getName(), MonthActivity.this.getSelectedTime());
                return true;
            case MENU_AGENDA:
                Utils.startActivity(MonthActivity.this, AgendaActivity.class.getName(), MonthActivity.this.getSelectedTime());
                return true;
            case MENU_DAY:
                Utils.startActivity(MonthActivity.this, DayActivity.class.getName(), MonthActivity.this.getSelectedTime());
                return true;
            case MENU_WEEK:
                Utils.startActivity(MonthActivity.this, WeekActivity.class.getName(), MonthActivity.this.getSelectedTime());
                return true;
            case MENU_MONTH:
                Utils.startActivity(MonthActivity.this, MonthActivity.class.getName(), MonthActivity.this.getSelectedTime());
                return true;
            case MENU_EVENT_CREATE: {
                long startMillis = MonthActivity.this.getSelectedTime();
                long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setClassName(MonthActivity.this, EditEvent.class.getName());
                intent.putExtra(EVENT_BEGIN_TIME, startMillis);
                intent.putExtra(EVENT_END_TIME, endMillis);
                intent.putExtra(EditEvent.EVENT_ALL_DAY, MonthActivity.this.getAllDay());
                startActivity(intent);
                return true;
            }
            case MENU_SELECT_DATE:
                MonthActivity.this.selectDate();
                return true;

            case MENU_SEARCH_AGENDA:
                onSearchRequested();
                return true;
            case MENU_AGENDA_DELETE_ALL_EVENTS:
                    new AlertDialog.Builder(MonthActivity.this)
                    .setTitle(R.string.delete_title)
                    .setMessage(R.string.delete_all_event_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub
                                    execEventsDel(MonthActivity.this);
                                }
                            }).setNegativeButton(android.R.string.cancel, null).show();
                    return true;
            }
//            return false;
        return super.onOptionsItemSelected(item);
    }

    private static void execEventsDel(Activity activity) {
        final DeleteEventHelper mDeleteEventHelper = new DeleteEventHelper(activity, false);
        mDeleteEventHelper.deleteAllEvents();
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, false);
        return true;
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
        java.util.Calendar c = java.util.Calendar.getInstance();

        if(year <= 2038 && year >=1970){
            if(year == 2038){
                if(month >= 1){
                    Toast toast = Toast.makeText(this, R.string.out_range, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if(month == 0 && day >18){
                    Toast toast = Toast.makeText(this, R.string.out_range, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
            }else{
                c.set(java.util.Calendar.YEAR, year);
                c.set(java.util.Calendar.MONTH, month);
                c.set(java.util.Calendar.DAY_OF_MONTH, day);
                long when = c.getTimeInMillis();
                goToSelectDay(when);
            }
        }else {
            Toast toast = Toast.makeText(this, R.string.out_range, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
    }
    //add for bug 20327 20328 begin
}
