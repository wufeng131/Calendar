package com.android.calendar;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

class EventInfo extends LinearLayout{
        public EventInfo(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }
        public EventInfo(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
        long begin;
        long end;

        public long getBegin(){
            return begin;
        }

        public void setBegin(long begin){
            this.begin = begin;
        }

        public long getEnd(){
            return end;
        }

        public void setEnd(long end){
            this.end = end;
        }
    }