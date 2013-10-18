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
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Calendar;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher;


/**
 * This is the base class for Day and Week Activities.
 */
public class CalendarActivity extends Activity implements Navigator,
        DatePickerDialog.OnDateSetListener{

    private static final long INITIAL_HEAP_SIZE = 4*1024*1024;
    private static final long ANIMATION_DURATION = 400;

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

    protected static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
  //boat add by pengwufeng 20131010
    private final static String TITLE_BTN_ACTION = "title_btn_action";

    private ContentResolver mContentResolver;

//    protected ProgressBar mProgressBar;
    protected ViewSwitcher mViewSwitcher;
    protected Animation mInAnimationForward;
    protected Animation mOutAnimationForward;
    protected Animation mInAnimationBackward;
    protected Animation mOutAnimationBackward;
    EventLoader mEventLoader;

    Time mSelectedDay;

    private static final int DIALOG_DATEPICKER = 0;

    // This gets run if the time zone is updated in the db
    private Runnable mUpdateTZ = new Runnable() {
        @Override
        public void run() {
            // We want this to keep the same day so we swap the tz
            mSelectedDay.timezone = Utils.getTimeZone(CalendarActivity.this, this);
            mSelectedDay.normalize(true);
        }
    };

    /* package */ GestureDetector mGestureDetector;

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

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mSelectedDay = new Time(Utils.getTimeZone(this, mUpdateTZ));

        // Eliminate extra GCs during startup by setting the initial heap size to 4MB.
        // TODO: We should restore the old heap size once the activity reaches the idle state
        VMRuntime.getRuntime().setMinimumHeapSize(INITIAL_HEAP_SIZE);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        mContentResolver = getContentResolver();

        mInAnimationForward = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        mOutAnimationForward = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        mInAnimationBackward = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        mOutAnimationBackward = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

        mGestureDetector = new GestureDetector(this, new CalendarGestureListener());
        mEventLoader = new EventLoader(this);
        registerBoradcastReceiver();//boat add by pengwufeng 20131010
    }
    
  //boat add by pengwufeng start 20131010
    public void registerBoradcastReceiver(){ 
        IntentFilter myIntentFilter = new IntentFilter(); 
        myIntentFilter.addAction(TITLE_BTN_ACTION); 
        registerReceiver(titleBtnOnclickReceiver, myIntentFilter);//boat add by pengwufeng 20131010
    } 
    
    private BroadcastReceiver titleBtnOnclickReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(TITLE_BTN_ACTION)) {
				String tabTag = intent.getStringExtra("curTab");
				if(tabTag.equals("WEEK") || tabTag.equals("DAY")) {
					String clickBtn = intent.getStringExtra("onClickBtn");
					if(clickBtn.equals("btn_add")) {
						long startMillis = CalendarActivity.this.getSelectedTime();
		                long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
		                Intent mIntent = new Intent(Intent.ACTION_EDIT);
		                mIntent.setClassName(CalendarActivity.this, EditEvent.class.getName());
		                mIntent.putExtra(EVENT_BEGIN_TIME, startMillis);
		                mIntent.putExtra(EVENT_END_TIME, endMillis);
		                mIntent.putExtra(EditEvent.EVENT_ALL_DAY, CalendarActivity.this.getAllDay());
		                startActivity(mIntent);
					} else if(clickBtn.equals("btn_today")) {
						CalendarActivity.this.goToToday();
					}
				}
			}
		}
	};
    //boat add by pengwufeng end 20131010

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        Time time = new Time(Utils.getTimeZone(this, mUpdateTZ));
        time.set(savedInstanceState.getLong(BUNDLE_KEY_RESTORE_TIME));
        view.setSelectedDay(time);
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
    protected void onResume() {
        super.onResume();
        mEventLoader.startBackgroundThread();
        eventsChanged();
        CalendarView view = (CalendarView) mViewSwitcher.getNextView();
        view.updateIs24HourFormat();
        view.updateView();

        view = (CalendarView) mViewSwitcher.getCurrentView();
        view.updateIs24HourFormat();
        view.updateView();

        // Register for Intent broadcasts
        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mIntentReceiver, filter);

        mContentResolver.registerContentObserver(Calendar.Events.CONTENT_URI,
                true, mObserver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(BUNDLE_KEY_RESTORE_TIME, getSelectedTimeInMillis());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mContentResolver.unregisterContentObserver(mObserver);
        unregisterReceiver(mIntentReceiver);

        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        view.cleanup();
        view = (CalendarView) mViewSwitcher.getNextView();
        view.cleanup();
        mEventLoader.stopBackgroundThread();
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(titleBtnOnclickReceiver);//boat add by pengwufeng 20131011
	}

    void startProgressSpinner() {
        // start the progress spinner
//        mProgressBar.setVisibility(View.VISIBLE);
    }

    void stopProgressSpinner() {
        // stop the progress spinner
//        mProgressBar.setVisibility(View.GONE);
    }

    /* Navigator interface methods */
    public void goTo(Time time, boolean animate) {
        if (animate) {
            CalendarView current = (CalendarView) mViewSwitcher.getCurrentView();
            if (current.getSelectedTime().before(time)) {
                mViewSwitcher.setInAnimation(mInAnimationForward);
                mViewSwitcher.setOutAnimation(mOutAnimationForward);
            } else {
                mViewSwitcher.setInAnimation(mInAnimationBackward);
                mViewSwitcher.setOutAnimation(mOutAnimationBackward);
            }
        }

        CalendarView next = (CalendarView) mViewSwitcher.getNextView();
        next.setSelectedDay(time);
        next.reloadEvents();
        mViewSwitcher.showNext();
        next.requestFocus();
    }

    /**
     * Returns the selected time in milliseconds. The milliseconds are measured
     * in UTC milliseconds from the epoch and uniquely specifies any selectable
     * time.
     *
     * @return the selected time in milliseconds
     */
    public long getSelectedTimeInMillis() {
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        return view.getSelectedTimeInMillis();
    }

    public long getSelectedTime() {
        return getSelectedTimeInMillis();
    }

    public void goToToday() {
        mSelectedDay.set(System.currentTimeMillis());
        mSelectedDay.normalize(true);
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        view.setSelectedDay(mSelectedDay);
        view.reloadEvents();
    }

    public void goToSelectDay(long selectTime) {
        mSelectedDay.set(selectTime);
        mSelectedDay.normalize(true);
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        view.setSelectedDay(mSelectedDay);
        view.reloadEvents();
    }
    
    public void selectDate() {
    	removeDialog(DIALOG_DATEPICKER);
	    showDialog(DIALOG_DATEPICKER);
    }
    
    public boolean getAllDay() {
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        return view.mSelectionAllDay;
    }

    void eventsChanged() {
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        view.clearCachedEvents();
        view.reloadEvents();
    }

    Event getSelectedEvent() {
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        return view.getSelectedEvent();
    }

    boolean isEventSelected() {
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        return view.isEventSelected();
    }

    Event getNewEvent() {
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        return view.getNewEvent();
    }

    public CalendarView getNextView() {
        return (CalendarView) mViewSwitcher.getNextView();
    }

    public View switchViews(boolean forward, float xOffSet, float width) {
        float progress = Math.abs(xOffSet) / width;
        if (progress > 1.0f) {
            progress = 1.0f;
        }

        float inFromXValue, inToXValue;
        float outFromXValue, outToXValue;
        if (forward) {
            inFromXValue = 1.0f - progress;
            inToXValue = 0.0f;
            outFromXValue = -progress;
            outToXValue = -1.0f;
        } else {
            inFromXValue = progress - 1.0f;
            inToXValue = 0.0f;
            outFromXValue = progress;
            outToXValue = 1.0f;
        }

        // We have to allocate these animation objects each time we switch views
        // because that is the only way to set the animation parameters.
        TranslateAnimation inAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, inFromXValue,
                Animation.RELATIVE_TO_SELF, inToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        TranslateAnimation outAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, outFromXValue,
                Animation.RELATIVE_TO_SELF, outToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        // Reduce the animation duration based on how far we have already swiped.
        long duration = (long) (ANIMATION_DURATION * (1.0f - progress));
        inAnimation.setDuration(duration);
        outAnimation.setDuration(duration);
        mViewSwitcher.setInAnimation(inAnimation);
        mViewSwitcher.setOutAnimation(outAnimation);

        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        view.cleanup();
        mViewSwitcher.showNext();
        view = (CalendarView) mViewSwitcher.getCurrentView();
        view.requestFocus();
        view.reloadEvents();
        return view;
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

//    Account[] accounts = AccountManager.get(activity).getAccounts();
//    if(accounts.length > 0){
        menu.setGroupVisible(MENU_GROUP_SELECT_CALENDARS, true);
        menu.setGroupEnabled(MENU_GROUP_SELECT_CALENDARS, true);
//    }else{
//        menu.setGroupVisible(MENU_GROUP_SELECT_CALENDARS, false);
//        menu.setGroupEnabled(MENU_GROUP_SELECT_CALENDARS, false);
//    }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        if (!MenuHelper.onCreateOptionsMenu(menu,null)) {
//            return false;
//        }
//        return super.onCreateOptionsMenu(menu);
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
//        if (MenuHelper.onOptionsItemSelected(this, item, this)) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case MENU_SELECT_CALENDARS: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(CalendarActivity.this, SelectCalendarsActivity.class);
                startActivity(intent);
                return true;
            }
            case MENU_GOTO_TODAY:
                CalendarActivity.this.goToToday();
                return true;
            case MENU_PREFERENCES:
                Utils.startActivity(CalendarActivity.this, CalendarPreferenceActivity.class.getName(), CalendarActivity.this.getSelectedTime());
                return true;
            case MENU_AGENDA:
                Utils.startActivity(CalendarActivity.this, AgendaActivity.class.getName(), CalendarActivity.this.getSelectedTime());
                return true;
            case MENU_DAY:
                Utils.startActivity(CalendarActivity.this, DayActivity.class.getName(), CalendarActivity.this.getSelectedTime());
                return true;
            case MENU_WEEK:
                Utils.startActivity(CalendarActivity.this, WeekActivity.class.getName(), CalendarActivity.this.getSelectedTime());
                return true;
            case MENU_MONTH:
                Utils.startActivity(CalendarActivity.this, MonthActivity.class.getName(), CalendarActivity.this.getSelectedTime());
                return true;
            case MENU_EVENT_CREATE: {
                long startMillis = CalendarActivity.this.getSelectedTime();
                long endMillis = startMillis + DateUtils.HOUR_IN_MILLIS;
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setClassName(CalendarActivity.this, EditEvent.class.getName());
                intent.putExtra(EVENT_BEGIN_TIME, startMillis);
                intent.putExtra(EVENT_END_TIME, endMillis);
                intent.putExtra(EditEvent.EVENT_ALL_DAY, CalendarActivity.this.getAllDay());
                startActivity(intent);
                return true;
            }
            case MENU_SELECT_DATE:
                CalendarActivity.this.selectDate();
                return true;

            case MENU_SEARCH_AGENDA:
                onSearchRequested();
                return true;
            case MENU_AGENDA_DELETE_ALL_EVENTS:
                    new AlertDialog.Builder(CalendarActivity.this)
                    .setTitle(R.string.delete_title)
                    .setMessage(R.string.delete_all_event_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub
                                    execEventsDel(CalendarActivity.this);
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
    public boolean onTouchEvent(MotionEvent ev) {
        if (mGestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

    class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
            view.doSingleTapUp(ev);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
            CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
            view.doLongPress(ev);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
            view.doScroll(e1, e2, distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
            view.doFling(e1, e2, velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
            view.doDown(ev);
            return true;
        }
        
    }
    @Override
    public Dialog onCreateDialog(int id) {
        Dialog d;
        switch (id) {
        case DIALOG_DATEPICKER: {
            final java.util.Calendar calendar = java.util.Calendar.getInstance();
            d = new DatePickerDialog(
                this,
                this,
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH));
            break;
        }
        default:
            d = null;
            break;
        }
        return d;
    }
      
//      public void onDateSet(DatePicker view, int year, int month, int day) {
//      	java.util.Calendar c = java.util.Calendar.getInstance();
//
//          //add for bug 20327 20328 begin
//        if(year > 2037 || year < 1970){
//            Toast toast = Toast.makeText(this, R.string.out_range, Toast.LENGTH_SHORT);
//            toast.show();
//            return;
//            }else{
//                c.set(java.util.Calendar.YEAR, year);
//                c.set(java.util.Calendar.MONTH, month);
//                c.set(java.util.Calendar.DAY_OF_MONTH, day);
//                long when = c.getTimeInMillis();
//                goToSelectDay(when);
//                }
//      }
//      //add for bug 20327 20328 end
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
      //add for bug 20327 20328 end
}

