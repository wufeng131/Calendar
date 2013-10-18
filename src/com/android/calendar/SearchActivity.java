package com.android.calendar;

import static android.provider.Calendar.EVENT_BEGIN_TIME;
import static android.provider.Calendar.EVENT_END_TIME;

import java.util.Formatter;
import java.util.Locale;

import android.app.Activity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.provider.Calendar;
import android.provider.Calendar.Events;
import android.provider.Calendar.Calendars;
import android.net.Uri;

public class SearchActivity extends Activity{

    private AsyncQueryHandler mQueryHandler;
    private CursorAdapter mCursorAdapter;
    private ContentResolver mContentResolver;
    private static StringBuilder mStringBuilder = new StringBuilder(50);
    private static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

    private static final int EVENT_INDEX_ID = 0;
    private static final int EVENT_INDEX_TITLE = 1;
    private static final int EVENT_INDEX_DESCRIPTION = 2;
    private static final int EVENT_INDEX_EVENT_LOCATION = 3;
    private static final int EVENT_INDEX_ALL_DAY = 4;
    private static final int EVENT_INDEX_DTSTART = 7;
    private static final int EVENT_INDEX_DTEND = 8;
    private static final int EVENT_INDEX_TIMEZONE = 10;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        mContentResolver = this.getContentResolver();
        String searchStringParameter = getIntent().getStringExtra(SearchManager.QUERY);
        if (searchStringParameter == null) {
            searchStringParameter = getIntent().getStringExtra("intent_extra_data_key" /*SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA*/);
        }
        final String searchString =
            searchStringParameter != null ? searchStringParameter.trim() : searchStringParameter;
        ContentResolver cr = getContentResolver();

        listView = (ListView) findViewById(R.id.search_container);
        listView.setItemsCanFocus(true);
        listView.setFocusable(true);
        listView.setClickable(true);
        listView.setOnItemClickListener(mViewListener);
        setTitle("");

        mQueryHandler = new AsyncQueryHandler(cr) {
            protected void onQueryComplete(int token, Object cookie, Cursor c) {
                if(c == null){
                    return;
                }
                int cursorCount = c.getCount();
                setTitle(getResources().getQuantityString(
                        R.plurals.search_results_title,
                        cursorCount,
                        cursorCount,
                        searchString));
                if ( SearchActivity.this.mCursorAdapter != null ) {
                    SearchActivity.this.mCursorAdapter.changeCursor(null);
                    }
                SearchActivity.this.mCursorAdapter = new CursorAdapter(SearchActivity.this,
                        c, false /* no auto-requery */) {
                    @Override
                    public void bindView(View view, Context context, Cursor cursor) {
                        TextView title = (TextView)(view.findViewById(R.id.search_title));
                        TextView when = (TextView)(view.findViewById(R.id.search_time));
                        TextView description = (TextView)(view.findViewById(R.id.search_location));
                        
                        String etitle = cursor.getString(cursor.getColumnIndex(Events.TITLE));
                        String edescription = cursor.getString(cursor.getColumnIndex(Events.DESCRIPTION));
                        String timezone = cursor.getString(cursor.getColumnIndex(Events.EVENT_TIMEZONE));
                        boolean isAllDay = cursor.getInt(cursor.getColumnIndex(Events.ALL_DAY)) !=0;
                        Long start = cursor.getLong(cursor.getColumnIndex(Events.DTSTART));
                        Long end = cursor.getLong(cursor.getColumnIndex(Events.DTEND));
                        String whenS;
                        int flags;
                        if (isAllDay) {
                            flags = DateUtils.FORMAT_UTC | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE;
                        } else {
                            flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE;
                            if(DateFormat.is24HourFormat(SearchActivity.this)){
                                flags |= DateUtils.FORMAT_24HOUR;
                            }
                        }
                        whenS = formatTime(start, end, flags,timezone);
                        if(etitle!=null){
                            title.setText(etitle);
                        }else{
                            title.setText("");
                        }
                        when.setText(whenS);
                        if(edescription != null){
                            description.setText(edescription);
                        }else{
                            description.setVisibility(View.GONE);
                        }
                        }

                    @Override
                    public View newView(Context context, Cursor cursor,
                            ViewGroup parent) {
                        LayoutInflater inflater = LayoutInflater.from(context);
                        View v = inflater.inflate(R.layout.search_item, parent, false);
                        return v;
                        }
                    };

                    listView.setAdapter(SearchActivity.this.mCursorAdapter);
                    listView.setFocusable(true);
                    listView.setFocusableInTouchMode(true);
                    listView.requestFocus();
                    }
            };

            Uri uri = Events.CONTENT_URI;
            String selection = Events.TITLE+" like ?"+" OR "+Events.DESCRIPTION+" like ?"+" OR "
            +Events.EVENT_LOCATION+" like ?";
            String selectiontArg[] =  new String[]{"%" + searchString + "%","%" + searchString + "%","%" + searchString + "%"};
            mQueryHandler.startQuery(0, null, uri, null, selection, selectiontArg, null);
    }

    public String formatTime(long startTime,long endTime,int flags,String timezone){
        mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(SearchActivity.this, mFormatter, startTime, endTime, flags, timezone).toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ( mCursorAdapter != null ) {
            mCursorAdapter.changeCursor(null);
        }
        mStringBuilder.delete(0, mStringBuilder.length());
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    private OnItemClickListener mViewListener = new OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // TODO Auto-generated method stub
            SearchActivity searchActivity = SearchActivity.this;
            Cursor cursor = searchActivity.getItemForView(view);

            Long startMillis = cursor.getLong(cursor.getColumnIndex(Events.DTSTART));
            Long endMillis = cursor.getLong(cursor.getColumnIndex(Events.DTEND));
            Long eventid = cursor.getLong(cursor.getColumnIndex(Events._ID));
            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventid);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setClass(searchActivity, EventInfoActivity.class);
            intent.putExtra(EVENT_BEGIN_TIME, startMillis);
            intent.putExtra(EVENT_END_TIME, endMillis);

            startActivity(intent);
            searchActivity.finish();
        }
    };

    public Cursor getItemForView(View view) {
        int index = listView.getPositionForView(view);
        if (index < 0) {
            return null;
        }
        return (Cursor) listView.getAdapter().getItem(index);
    }

    private void saveRecentSearchHistory(String queryKeyword) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
        SearchSuggestionSampleProvider.AUTHORITY, SearchSuggestionSampleProvider.MODE);
        suggestions.saveRecentQuery(queryKeyword, null);
        }

    private void clearRecentSearchHistory() {
        SearchRecentSuggestions suggestions =new SearchRecentSuggestions(this, 
        SearchSuggestionSampleProvider.AUTHORITY, SearchSuggestionSampleProvider.MODE);
        suggestions.clearHistory();
        }
}
