package com.android.calendar;


import org.apache.commons.codec.DecoderException;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.pim.EventRecurrence;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import android.util.Log;


public class Vcalendar
{
  public static final String TAG = "EventInfoActivity";
  private Context mContext;
  public static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String FILE_EXT = ".vcs";
  private final ByteArrayOutputStream mOut;
  
  static final int SECONDS = 1000;
  static final int MINUTES = SECONDS*60;
  static final int HOURS = MINUTES*60;
  static final long DAYS = HOURS*24;
  private static byte ESCAPE_CHAR = '=';
  static final int VCLNDR_NUM_OF_DAYS = 7;
  
  
  private static final int DOES_NOT_REPEAT = 0;
  private static final int REPEATS_DAILY = 1;
  private static final int REPEATS_EVERY_WEEKDAY = 2;
  private static final int REPEATS_WEEKLY_ON_DAY = 3;
  private static final int REPEATS_MONTHLY_ON_DAY_COUNT = 4;
  private static final int REPEATS_MONTHLY_ON_DAY = 5;
  private static final int REPEATS_YEARLY = 6;
  private static final int REPEATS_CUSTOM = 7;
  
  
  static final String[] sTypeToFreq =
      new String[] {"DAILY", "WEEKLY", "MONTHLY","YEARLY"};

  static final String[] sTwoCharacterNumbers =
        new String[] {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
  static final TimeZone sGmtTimeZone = TimeZone.getTimeZone("GMT");

  static final String[] sDayTokens =
      new String[] {"SU", "MO", "TU", "WE", "TH", "FR", "SA"};
  
  /** Converts a String to UTF-8 */
  public static byte[] toUtf8(String s) {
      if (s == null) {
          return null;
      }
      final ByteBuffer buffer = UTF_8.encode(CharBuffer.wrap(s));
      final byte[] bytes = new byte[buffer.limit()];
      buffer.get(bytes);
      return bytes;
  }
  
  static String formatTwo(int num) {
      if (num <= 12) {
          return sTwoCharacterNumbers[num];
      } else
          return Integer.toString(num);
  }

  static String utcOffsetString(int offsetMinutes) {
      StringBuilder sb = new StringBuilder();
      int hours = offsetMinutes / 60;
      if (hours < 0) {
          sb.append('-');
          hours = 0 - hours;
      } else {
          sb.append('+');
      }
      int minutes = offsetMinutes % 60;
      if (hours < 10) {
          sb.append('0');
      }
      sb.append(hours);
      if (minutes < 10) {
          sb.append('0');
      }
      sb.append(minutes);
      return sb.toString();
  }
  
  public class VCalendarsinfo
  {
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
   
   VCalendarsinfo()
   {
       
   }
   
   @Override
   public String toString() {
       StringBuilder builder = new StringBuilder("VCalendarsinfo: ");
       builder.append("eventitle: ").append(eventitle+" ").
               append("starttime: ").append(starttime+" ").
               append("endtime: ").append(endtime+" ").
               append("allDay:").append(allDay+" ").
               append("location: ").append(location+" ").
               append("description: ").append(description+" ").
               append("timezone: ").append(timezone+" ").
               append("hasAlarm: ").append(hasAlarm+" ").
               append("AlarmMinute: ").append(AlarmMinute+" ").
               append("rRule: ").append(rRule);
       return builder.toString();
   }
  }
 

   String transitionMillisToVCalendarTime(long millis, TimeZone tz, boolean dst) {
        StringBuilder sb = new StringBuilder();

        /*int rawOffsetMinutes = tz.getRawOffset() / MINUTES;
        String standardOffsetString = utcOffsetString(rawOffsetMinutes);
        Log.i(TAG,  "transitionMillisToVCalendarTime"+standardOffsetString);*/

        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(millis);
        int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET);  
        int dstOffset = cal.get(java.util.Calendar.DST_OFFSET);  
        cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset));  

        sb.append(cal.get(Calendar.YEAR));
        sb.append(formatTwo(cal.get(Calendar.MONTH) + 1));
        sb.append(formatTwo(cal.get(Calendar.DAY_OF_MONTH)));
        sb.append('T');
        sb.append(formatTwo(cal.get(Calendar.HOUR_OF_DAY)));
        sb.append(formatTwo(cal.get(Calendar.MINUTE)));
        sb.append(formatTwo(cal.get(Calendar.SECOND)));
        sb.append('Z');
        

        return sb.toString();
    }
   
  long  transitionVCalendarTimeToMillis(String VCalendarTime,TimeZone timezone)
  {
      
      if (TextUtils.isEmpty(VCalendarTime)) {
          return 0;
      }
      
       String  date = VCalendarTime ;
       
        GregorianCalendar cal = new GregorianCalendar(Integer.parseInt(date.substring(0, 4)),
              Integer.parseInt(date.substring(4, 6)) - 1, Integer.parseInt(date.substring(6, 8)),
              Integer.parseInt(date.substring(9, 11)), Integer.parseInt(date.substring(11, 13)),
              Integer.parseInt(date.substring(13, 15)));
      
      cal.setTimeZone(timezone);
      return cal.getTimeInMillis();
  }
  

  /** Build a String from UTF-8 bytes */
  public static String fromUtf8(byte[] b) {
      if (b == null) {
          return null;
      }
      final CharBuffer cb = UTF_8.decode(ByteBuffer.wrap(b));
      return new String(cb.array(), 0, cb.length());
  }

  
  public Vcalendar(Context paramContext)
  {
    this.mContext = paramContext;
    this.mOut = new ByteArrayOutputStream();
  }

   public void writeFileData(String fileName,String message){ 
       try{ 
            FileOutputStream fout =mContext.openFileOutput(fileName, mContext.MODE_PRIVATE);
            byte [] bytes = message.getBytes(); 
            fout.write(bytes); 
            fout.close(); 
        }catch(Exception e){ 
        e.printStackTrace(); 
       } 
   }
   
 
   public byte [] readFileSdcard(InputStream in){

       byte [] buffer=null; 
       try{ 
        int length = in.available();
        buffer = new byte[length]; 
        in.read(buffer);
        in.close();
       }catch(Exception e){ 
        e.printStackTrace(); 
       } 
       return buffer; 
  }
   public void writeFileSdcard(String fileName,byte [] bytes){ 
       
       File saveFile=new File("mnt/sdcard/bluetooth/"+fileName);
       Log.i(TAG,  "writeFileSdcard"+saveFile);
       String pathName="mnt/sdcard/bluetooth/";  
       File path = new File(pathName); 
       File file = new File(pathName + fileName);  
  
       try{ 
           if( !path.exists()) {  
               Log.d("TestFile", "Create the path:" + pathName);  
               path.mkdir();  
           }  
           
           if( !file.exists()) {  
               Log.d("TestFile", "Create the file:" + fileName);  
               file.createNewFile();  
           }  
     
           FileOutputStream fout = new FileOutputStream(saveFile);
            fout.write(bytes); 
             fout.close(); 
            }catch(Exception e){ 
            e.printStackTrace(); 
       } 
   }
   public void writeFileData(String fileName,byte [] bytes){ 
       try{ 
            FileOutputStream fout =mContext.openFileOutput(fileName, mContext.MODE_PRIVATE);
            fout.write(bytes); 
            fout.close(); 
        } 
       catch(Exception e){ 
        e.printStackTrace(); 
       }  
   }
    void writeLine(String string) {
       int numBytes = 0;
       for (byte b : toUtf8(string)) {

           mOut.write(b);
           numBytes++;
       }
       mOut.write((byte) '\r');
       mOut.write((byte) '\n');
   }

   
   public void writeTag(String name, String value) {
       // Belt and suspenders here; don't crash on null value; just return
       if (TextUtils.isEmpty(value)) {
           return;
       }
       
       writeLine(name + ":" + value);
       Log.i(TAG, name + ":" + value);
   }

   public byte[] getVcalendarBytes() {
       try {
           mOut.flush();
       } catch (IOException wonthappen) {
       }
       return mOut.toByteArray();
   }
   
   public String VcalendartoString() {
       return fromUtf8(getVcalendarBytes());
   }

   private String encodeQuotedPrintable(final String str) {
       if (TextUtils.isEmpty(str)) {
           return "";
       }

       final StringBuilder builder = new StringBuilder();
       int index = 0;
       byte[] strArray = null;

       try {
           strArray = str.getBytes("UTF-8");
       } catch (UnsupportedEncodingException e) {
           Log.e(TAG, "Charset " + "UTF-8" + " cannot be used. "
                   + "Try default charset");
           strArray = str.getBytes();
       }
       index = 0 ;
       while (index < strArray.length) {
           builder.append(String.format("=%02X", strArray[index]));
           index += 1;
       }
       return builder.toString();
   }
   
   private  String decodeQuotedPrintable(String str ) throws DecoderException {
       if (TextUtils.isEmpty(str)) {
           return null;
       }
       
       byte[] bytes ;
      
       try {
           bytes =str.getBytes("UTF-8");
       } catch (UnsupportedEncodingException e) {
           Log.e(TAG, "Charset " + "UTF-8" + " cannot be used. "
                   + "Try default charset");
           bytes = str.getBytes();
       }
       
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      
       for (int i = 0; i < bytes.length; i++) {
           int b = bytes[i];
           if (b == ESCAPE_CHAR) {
               try {
                   int u = Character.digit((char) bytes[++i], 16);
                   int l = Character.digit((char) bytes[++i], 16);
                   if (u == -1 || l == -1) {
                       throw new DecoderException("Invalid quoted-printable encoding");
                   }
                   buffer.write((char) ((u << 4) + l));
               } catch (ArrayIndexOutOfBoundsException e) {
                   throw new DecoderException("Invalid quoted-printable encoding");
               }
           } else {
               buffer.write(b);
           }
       }
       return buffer.toString();
   }
   
   String tokenFromRrule(String rrule, String token) {
       int start = rrule.indexOf(token);
       if (start < 0) return null;
       int len = rrule.length();
       start += token.length();
       int end = start;
       char c;
       do {
           c = rrule.charAt(end++);
           if ((c == ';') || (end == len)) {
               if (end == len) end++;
               return rrule.substring(start, end -1);
           }
       } while (true);
   }
   
   static String generateEasDayOfWeek(String dow) {
       int bits = 0;
       int bit = 1;
       for (String token: sDayTokens) {
           // If we can find the day in the dow String, add the bit to our bits value
           if (dow.indexOf(token) >= 0) {
               bits |= bit;
           }
           bit <<= 1;
       }
       return Integer.toString(bits);
   }
   
   public String encodeRrule(String rrule){
       if (TextUtils.isEmpty(rrule)) {
           return " ";
       }
       StringBuilder sb = new StringBuilder();
       String freq = tokenFromRrule(rrule, "FREQ=");
       // If there's no FREQ=X, then we don't write a recurrence
       // Note that we duplicate s.start(Tags.CALENDAR_RECURRENCE); s.end(); to prevent the
       // possibility of writing out a partial recurrence stanza
       if (freq != null) {
           if (freq.equals("DAILY")) {
               sb.append("D1 #0");
           } else if (freq.equals("WEEKLY")) {
               String byDay = tokenFromRrule(rrule, "BYDAY=");
               if (byDay != null) {
                  // sb.append("W1 "+generateEasDayOfWeek(byDay)+" #0");
                   sb.append("W1 "+byDay+" #0");
               }else{
                   sb.append("W1 #0");
               }
    
           } else if (freq.equals("MONTHLY")) {
               String byMonthDay = tokenFromRrule(rrule, "BYMONTHDAY=");
               if (byMonthDay != null) {
                   // The nth day of the month
                   sb.append("MD1 "+byMonthDay+" #0");
               } else {
                   String byDay = tokenFromRrule(rrule, "BYDAY=");
                   Log.i(TAG, "encodeRrule:MONTHLY "+byDay);  
                   if (byDay != null) {
                       sb.append("MP1 "+byDay+" #0");
                       // This can be 1WE (1st Wednesday) or -1FR (last Friday)
                      /* int wom = byDay.charAt(0);
                        String bareByDay;
                       if (wom == '-') {
                           // -1 is the only legal case (last week) Use "5" for EAS
                           wom = 5;
                           bareByDay = byDay.substring(2);
                       } else {
                           wom = wom - '0';
                           bareByDay = byDay.substring(1);
                       }
           
                       sb.append("MD1 "+Integer.toString(wom)+generateEasDayOfWeek(bareByDay)+" #0");*/
                     }
                   }
           } else if (freq.equals("YEARLY")) {
             sb.append("YM1 #0");
           }
       }
    return sb.toString();
   }
   

   public String VcalendarCreate(VcalendarInfo info ){

        // Create our iCalendar writer and start generating tags
        /* BEGIN:VCALENDAR */
        writeTag("BEGIN", "VCALENDAR");
        /* VERSION */
        writeTag("VERSION", "1.0");
        /* BEGIN:VEVENT/VTODO */
        writeTag("BEGIN", "VEVENT");
        /* DTSTART: */
        String starttime=transitionMillisToVCalendarTime(info.starttime, TimeZone.getTimeZone(info.timezone), !info.allDay);
        writeTag("DTSTART",starttime);
        /* DTEND */
        String endtime=transitionMillisToVCalendarTime(info.endtime, TimeZone.getTimeZone(info.timezone), !info.allDay);
        writeTag("DTEND", endtime);

       /* SUMMARY: */
        String title = info.eventitle;
        if (TextUtils.isEmpty(title)) title = "SprdVcalendar";
        writeTag("SUMMARY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8", encodeQuotedPrintable(title));
 
        /* DESCRIPTION: */
        String text=info.description;
        if(!TextUtils.isEmpty(text)){
        writeTag("DESCRIPTION;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8", encodeQuotedPrintable(text));
        }
        /* RRULE: */
        String rrule = info.rRule;
       if (!TextUtils.isEmpty(rrule)) {
        writeTag("RRULE", encodeRrule(rrule));
        }
        /* Alarm */
        //writeTag("DALARM",  info.starttime);
        if(info.hasAlarm){
        String[] mintues  = info.AlarmMinute.trim().split(";");
        for(String i:mintues){
        	if(!"".equals(i))
        	{
        		writeTag("AALARM", transitionMillisToVCalendarTime(info.starttime-Integer.valueOf(i)*60*1000, TimeZone.getTimeZone(info.timezone), !info.allDay));
        	}
//            writeTag("AALARM", transitionMillisToVCalendarTime(info.starttime-Integer.valueOf(i)*60*1000, TimeZone.getTimeZone(info.timezone), !info.allDay));
        }

        }
       /* Category */
        
        /* Priority */
        
        /* Location */
        String location = info.location;
        writeTag("LOCATION;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8", encodeQuotedPrintable(location));
        /* END:VEVENT/VTODO */
        writeTag("END", "VEVENT");
        /* END:VCALENDAR */
        writeTag("END", "VCALENDAR");
        
        String filename = ("SprdVcalendar"+FILE_EXT);
        //fortest
        writeFileSdcard(filename,getVcalendarBytes());
        //writeFileData(filename,getVcalendarBytes());
        return new String(filename);
    }
   
   private class UnterminatedBlockException extends IOException {
       private static final long serialVersionUID = 1L;
       UnterminatedBlockException(String name) {
           super(name);
       }
   }
   private class BlockHash {
       String name;
       HashMap<String, String> hash = new HashMap<String, String>();
       ArrayList<BlockHash> blocks = new ArrayList<BlockHash>();

       BlockHash (String _name, BufferedReader reader) throws IOException {
           name = _name;
           String lastField = null;
           String lastValue = null;
           while (true) {
               // Get a line; we're done if it's null
               String line = reader.readLine();
               if (line == null) {
                   throw new UnterminatedBlockException(name);
               }
               int length = line.length();
               if (length == 0) {
                   // We shouldn't ever see an empty line
                   throw new IllegalArgumentException();
               }
               // A line starting with tab is a continuation
               if (line.charAt(0) == '\t') {
                   // Remember the line and length
                   lastValue = line.substring(1);
                   // Save the concatenation of old and new values
                   hash.put(lastField, hash.get(lastField) + lastValue);
                   continue;
               }
               // Find the field delimiter
               int pos = line.indexOf(':');
               // If not found, or at EOL, this is a bad ics
               if (pos < 0 || pos >= length) {
                  // throw new IllegalArgumentException();
                  continue;
               }
               // Remember the field, value, and length
               lastField = line.substring(0, pos);
               lastValue = line.substring(pos + 1);
               
               if (lastField.equals("BEGIN")) {
                   //blocks.add(new BlockHash(lastValue, reader));
                   continue;
               } else if (lastField.equals("END")) {
                   //if (!lastValue.equals(name)) {
                      // throw new UnterminatedBlockException(name);
                   ///}
                   break;
               }

               // Save it away and continue
               if(hash.containsKey(lastField)){
                hash.put(lastField,hash.get(lastField)+";"+lastValue);
               }else{
               hash.put(lastField, lastValue);
               }

               if(lastField.startsWith("SUMMARY")){
                 hash.put("SUMMARY", lastValue);
               }else if(lastField.startsWith("DESCRIPTION")){
                hash.put("DESCRIPTION", lastValue);
               }else if(lastField.startsWith("LOCATION")){
                hash.put("LOCATION", lastValue);
               }
               Log.d(TAG, lastField+":"+ lastValue);  
           }
       }

       String get(String field) {
           return hash.get(field);
       }
       
       int size() {
           return hash.size();
       }
       
   }

   private BlockHash parseIcsContent(byte[] bytes) throws IOException {
       BufferedReader reader = new BufferedReader(new StringReader(fromUtf8(bytes)));
       String line = reader.readLine();
       if (!line.equals("BEGIN:VCALENDAR")) {
           throw new IllegalArgumentException();
       }
       return new BlockHash("VCALENDAR", reader);
   }

   

   void VcalendarImport(ContentResolver cr,Uri uri,VcalendarInfo mVCalendarsinfo)
   {
      BlockHash vcalendar;
      try{ 
           InputStream in = cr.openInputStream(uri);
           byte [] buffer= readFileSdcard(in);
           vcalendar = parseIcsContent(buffer);
           Log.i(TAG, "blocks size:"+vcalendar.blocks.size()+vcalendar.size());  
           
           String timezone =vcalendar.get("TZ");
           Log.i(TAG, "TZ:"+timezone); 

           mVCalendarsinfo.timezone =TimeZone.getTimeZone("GMT").getID();
           Log.i(TAG, ""+mVCalendarsinfo.timezone); 
           
           String starttime= vcalendar.get("DTSTART");
           String endtime= vcalendar.get("DTEND");
           mVCalendarsinfo.starttime = transitionVCalendarTimeToMillis(starttime,  TimeZone.getTimeZone("GMT"+timezone));
           mVCalendarsinfo.endtime = transitionVCalendarTimeToMillis(endtime,  TimeZone.getTimeZone("GMT"+timezone));

           Log.i(TAG, "DTSTART:"+mVCalendarsinfo.starttime ); 
           
           Log.i(TAG, "DTEND:"+mVCalendarsinfo.endtime ); 
           
           String title =vcalendar.get("SUMMARY");
           if (!TextUtils.isEmpty(title)){
               if (title.endsWith("=")) {
                   title = title.substring(0, title.length() - 1);
               }
               Log.i(TAG, "SUMMARY:"+title); 
               mVCalendarsinfo.eventitle = decodeQuotedPrintable(title); 
           }

           String text = vcalendar.get("DESCRIPTION");
           if (!TextUtils.isEmpty(text)){
               if (text.endsWith("=")) {
                   text = text.substring(0, text.length() - 1);
               }
               Log.i(TAG, "DESCRIPTION:"+text); 
               mVCalendarsinfo.description =decodeQuotedPrintable(text);
           }
           
           String location = vcalendar.get("LOCATION");
           if (!TextUtils.isEmpty(location)){
               if (location.endsWith("=")) {
                   location = location.substring(0, location.length() - 1);
               }
               Log.i(TAG, "LOCATION:"+location); 
               mVCalendarsinfo.location =decodeQuotedPrintable(location);
           }
  
           String rrules = vcalendar.get("RRULE");
           Log.i(TAG, "RRULE:"+rrules); 
           if (!TextUtils.isEmpty(rrules)){
               int index = DOES_NOT_REPEAT;
            
               if(rrules.startsWith("D"))
               {
                   index = REPEATS_DAILY ;
               }else if(rrules.startsWith("W")){
                   //index = REPEATS_EVERY_WEEKDAY ;
                   index = REPEATS_WEEKLY_ON_DAY;
       
               }else if(rrules.startsWith("MD")){
                   index = REPEATS_MONTHLY_ON_DAY ;
               }else if(rrules.startsWith("MP")){
                   index = REPEATS_MONTHLY_ON_DAY_COUNT;
               }else if(rrules.startsWith("YM")){
                   index = REPEATS_YEARLY ;
               }
               
               mVCalendarsinfo.rRule = updateRecurrenceRule(mVCalendarsinfo.starttime,index);
               Log.i(TAG, "rRule:"+mVCalendarsinfo.rRule); 
           }
           
           String alarm = vcalendar.get("AALARM");
           if (!TextUtils.isEmpty(alarm)){
               mVCalendarsinfo.hasAlarm = true;
               StringBuilder sb = new StringBuilder();
               String[] alarmstr  = alarm.trim().split(";");
               int mintues;
               for(String i:alarmstr){
                   mintues =(int) (mVCalendarsinfo.starttime -transitionVCalendarTimeToMillis(i, TimeZone.getTimeZone("GMT"+timezone)))/(60*1000);
                   if(mintues> 0){
                   sb.append(Integer.toString(mintues));
                   sb.append(";");
                   }
               }
               
               mVCalendarsinfo.AlarmMinute = sb.toString();
               Log.i(TAG, "AALARM:"+mVCalendarsinfo.AlarmMinute); 
           }
               
       }catch(Exception e){ 
       e.printStackTrace(); 
      } 

   }
   
   private String updateRecurrenceRule( long mStartMillis,   int selection) {
       EventRecurrence mEventRecurrence = new EventRecurrence();

       Time mStartTime = new Time();
       mStartTime.set(mStartMillis);

      if (selection == DOES_NOT_REPEAT) {
          return null;
      } else if (selection == REPEATS_CUSTOM) {
          // Keep custom recurrence as before.
          return null;
      } else if (selection == REPEATS_DAILY) {
          mEventRecurrence.freq = EventRecurrence.DAILY;
      } else if (selection == REPEATS_EVERY_WEEKDAY) {
          mEventRecurrence.freq = EventRecurrence.WEEKLY;
          int dayCount = 5;
          int[] byday = new int[dayCount];
          int[] bydayNum = new int[dayCount];

          byday[0] = EventRecurrence.MO;
          byday[1] = EventRecurrence.TU;
          byday[2] = EventRecurrence.WE;
          byday[3] = EventRecurrence.TH;
          byday[4] = EventRecurrence.FR;
          for (int day = 0; day < dayCount; day++) {
              bydayNum[day] = 0;
          }

          mEventRecurrence.byday = byday;
          mEventRecurrence.bydayNum = bydayNum;
          mEventRecurrence.bydayCount = dayCount;
      } else if (selection == REPEATS_WEEKLY_ON_DAY) {
          mEventRecurrence.freq = EventRecurrence.WEEKLY;
          int[] days = new int[1];
          int dayCount = 1;
          int[] dayNum = new int[dayCount];

          days[0] = EventRecurrence.timeDay2Day(mStartTime.weekDay);
          // not sure why this needs to be zero, but set it for now.
          dayNum[0] = 0;

          mEventRecurrence.byday = days;
          mEventRecurrence.bydayNum = dayNum;
          mEventRecurrence.bydayCount = dayCount;
      } else if (selection == REPEATS_MONTHLY_ON_DAY) {
          mEventRecurrence.freq = EventRecurrence.MONTHLY;
          mEventRecurrence.bydayCount = 0;
          mEventRecurrence.bymonthdayCount = 1;
          int[] bymonthday = new int[1];
          bymonthday[0] = mStartTime.monthDay;
          mEventRecurrence.bymonthday = bymonthday;
      } else if (selection == REPEATS_MONTHLY_ON_DAY_COUNT) {
          mEventRecurrence.freq = EventRecurrence.MONTHLY;
          mEventRecurrence.bydayCount = 1;
          mEventRecurrence.bymonthdayCount = 0;

          int[] byday = new int[1];
          int[] bydayNum = new int[1];
          // Compute the week number (for example, the "2nd" Monday)
          int dayCount = 1 + ((mStartTime.monthDay - 1) / 7);
          if (dayCount == 5) {
              dayCount = -1;
          }
          bydayNum[0] = dayCount;
          byday[0] = EventRecurrence.timeDay2Day(mStartTime.weekDay);
          mEventRecurrence.byday = byday;
          mEventRecurrence.bydayNum = bydayNum;
      } else if (selection == REPEATS_YEARLY) {
          mEventRecurrence.freq = EventRecurrence.YEARLY;
      }

      // Set the week start day.
      mEventRecurrence.wkst = EventRecurrence.calendarDay2Day(Calendar.getInstance().getFirstDayOfWeek());
       return mEventRecurrence.toString();
  }


}
