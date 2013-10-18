package com.android.calendar.widget;

import com.android.calendar.R;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.TextView;

public class MyTextView extends TextView {
	
	private int m_rotate;  
    public MyTextView(Context context) {
            super(context);                        
    }
    
    public MyTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
//            TypedArray array=context.obtainStyledAttributes(attrs,R.styleable.MyTextView); 
//            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MyTextView);
            this.m_rotate=90;
    }
    
    public MyTextView(Context context, AttributeSet attrs,int defStyle) {
            super(context, attrs,defStyle);                        
    }
    
    public int getMrotate(){
            return m_rotate;
    }
    public void setMrotate(int mrotate){
            m_rotate=mrotate;
    }
    
    protected void onDraw(Canvas canvas){
            canvas.translate(getHeight(), 0);
            canvas.rotate(m_rotate);
            super.onDraw(canvas);
    }
    
	protected void onMeasury(int widthMeasureSpec,int heightMeasureSpec){
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(200, 200);
    }
}
