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

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;

public class DayActivity extends CalendarActivity implements ViewSwitcher.ViewFactory {
    /**
     * The view id used for all the views we create. It's OK to have all child
     * views have the same ID. This ID is used to pick which view receives
     * focus when a view hierarchy is saved / restore
     */
    private static final int VIEW_ID = 1;
    //boat add by pengwufeng start 20131008
    private Button dayLeftBtn;
    private Button dayRightBtn;
    //boat add by pengwufeng start 20131008

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.day_activity);

        mSelectedDay = Utils.timeFromIntent(getIntent());
        mViewSwitcher = (ViewSwitcher) findViewById(R.id.switcher);
        mViewSwitcher.setFactory(DayActivity.this);
        mViewSwitcher.getCurrentView().requestFocus();
//        mProgressBar = (ProgressBar) findViewById(R.id.progress_circular);
        //boat add by pengwufeng start 20131009
        dayLeftBtn = (Button) findViewById(R.id.day_left);
        dayLeftBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
//				DayView view = (DayView) mViewSwitcher.getCurrentView();
				((CalendarView) mViewSwitcher.getCurrentView()).switchCalView(-1);
			}
		});
        dayRightBtn = (Button) findViewById(R.id.day_right);
        dayRightBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
//				DayView view = (DayView) makeView();
				((CalendarView) mViewSwitcher.getCurrentView()).switchCalView(1);
			}
		});
        //boat add by pengwufeng end 20131009
    }

    public View makeView() {
        DayView view = new DayView(DayActivity.this);
        view.setId(VIEW_ID);
        view.setLayoutParams(new ViewSwitcher.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        view.setSelectedDay(mSelectedDay);
        return view;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        CalendarView view = (CalendarView) mViewSwitcher.getCurrentView();
        mSelectedDay = view.getSelectedDay();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Record Day View as the (new) default detailed view.
        Utils.setDefaultView(DayActivity.this, CalendarApplication.DAY_VIEW_ID);
    }
}
