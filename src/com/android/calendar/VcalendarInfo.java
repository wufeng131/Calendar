package com.android.calendar;

public class VcalendarInfo{
   String eventitle ;
   long starttime;
   long endtime;
   boolean allDay;
   String location ;
   String description;
   String rRule;
   boolean hasAlarm;
   String AlarmMinute;
   String timezone;
   
   @Override
   public String toString() {
       StringBuilder builder = new StringBuilder("VCalendarsinfo: ");
       builder.append("eventitle: ").append(eventitle+" ").
               append("starttime: ").append(starttime+" ").
               append("endtime: ").append(endtime+" ").
               append("allDay:").append(allDay+" ").
               append("rRule: ").append(rRule).
               append("location: ").append(location+" ").
               append("description: ").append(description+" ").
               append("timezone: ").append(timezone+" ").
               append("hasAlarm: ").append(hasAlarm+" ").
               append("AlarmMinute: ").append(AlarmMinute+" ");

       return builder.toString();
   }
}
