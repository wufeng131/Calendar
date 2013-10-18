package com.android.calendar;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

import com.android.calendar.widget.MyTabHost;

public class MainActivity extends ActivityGroup {
	
	//boat add by pengwufeng start 20131011
	private ImageButton btn_add;
	private Button btn_today;
	private final static String TITLE_BTN_ACTION = "title_btn_action";
	//boat add by pengwufeng end 20131011
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		final MyTabHost tabHost = (MyTabHost) findViewById(R.id.tabhost);
		tabHost.setup(getLocalActivityManager());

		tabHost.addTab(tabHost.newTabSpec("MONTH")
				.setIndicator(getResources().getString(R.string.boat_month))
				.setContent(new Intent(this,MonthActivity.class)));
    	tabHost.addTab(tabHost.newTabSpec("WEEK")  
				.setIndicator(getResources().getString(R.string.boat_week))
				.setContent(new Intent(this,WeekActivity.class)));
    	tabHost.addTab(tabHost.newTabSpec("DAY")
				.setIndicator(getResources().getString(R.string.boat_day))
				.setContent(new Intent(this,DayActivity.class)));
    	tabHost.addTab(tabHost.newTabSpec("AGENDA")
				.setIndicator(getResources().getString(R.string.boat_agenda))
				.setContent(new Intent(this,AgendaActivity.class)));
    	btn_add = (ImageButton) findViewById(R.id.title_add);
    	btn_today = (Button) findViewById(R.id.title_today);
    	btn_add.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent mIntent = new Intent(TITLE_BTN_ACTION); 
                mIntent.putExtra("onClickBtn", "btn_add");
                mIntent.putExtra("curTab", tabHost.getCurrentTabTag());
                sendBroadcast(mIntent); 	 
			}
		});
    	btn_today.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent mIntent = new Intent(TITLE_BTN_ACTION); 
                mIntent.putExtra("onClickBtn", "btn_today");
                mIntent.putExtra("curTab", tabHost.getCurrentTabTag());
                sendBroadcast(mIntent); 
			}
		});
	}
}