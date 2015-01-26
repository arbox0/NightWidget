package com.nightscoutwidget.android.widget;

import java.util.Calendar;
import java.util.UUID;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.nightscoutwidget.android.R;
import com.nightscoutwidget.android.medtronic.Constants;
/**
 * 
 * @author lmmarguenda
 *
 */
public class CGMWidget extends AppWidgetProvider {
	private PendingIntent service = null;  
	
	 public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	        final int N = appWidgetIds.length;
	        Log.i("M","ON UPDATE WIDGET!!!");
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        SharedPreferences.Editor editor= prefs.edit();
	        editor.putBoolean("widgetEnabled", true);
	        if (!prefs.contains("widget_uuid"))
	        	editor.putString("widget_uuid", UUID.randomUUID().toString());
	        editor.commit();
	        for (int i=0; i<N; i++) {
	        	Log.i("M","N "+N);
	            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_main);
	            
	            if (prefs.getBoolean("showSGV", true)){
					views.setViewVisibility(R.id.linearLayout2, View.VISIBLE);
					
					views.setViewVisibility(R.id.linearLayout3, View.GONE);	
				}else if (!prefs.getBoolean("showSGV", true)){
					
					views.setViewVisibility(R.id.linearLayout2, View.GONE);
					
					views.setViewVisibility(R.id.linearLayout3, View.VISIBLE);	
				}
	            String webUri = null;
	    		if (prefs.contains("web_uri"))
	    			webUri = prefs.getString("web_uri", "http://www.nightscout.info/wiki/welcome");
	    		if (webUri != null && webUri.length() > 0 && webUri.indexOf("http://")>=0){
		    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
			        PendingIntent pendingIntent = PendingIntent.getActivity(context, 7, intent, 0);
			        views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
	    		}
	    		if (prefs.getBoolean("showIcon", true)){
	    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
	    		}else{
	    			views.setViewVisibility(R.id.imageButton1, View.GONE);
	    		}

	            final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);  
	            
	            final Calendar TIME = Calendar.getInstance();  
	            TIME.set(Calendar.MINUTE, 0);  
	            TIME.set(Calendar.SECOND, 0);  
	            TIME.set(Calendar.MILLISECOND, 0);  
	      
	            final Intent in = new Intent(context, CGMWidgetUpdater.class);  
	      
	            if (service == null)  
	            {  
	                service = PendingIntent.getService(context, 27, in, PendingIntent.FLAG_CANCEL_CURRENT);  
	            }  
	            if (prefs.contains("refreshPeriod")){
	            	String type = prefs.getString("refreshPeriod", "2");
	            	long time = Constants.TIME_5_MIN_IN_MS;
	            	if (type.equalsIgnoreCase("1"))
	            		time = Constants.TIME_1_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("3"))
	            		time = Constants.TIME_10_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("4"))
	            		time = Constants.TIME_20_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("5"))
	            		time = Constants.TIME_25_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("6"))
	            		time = Constants.TIME_30_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("7"))
	            		time = Constants.TIME_60_MIN_IN_MS;
	            	else
	            		time = Constants.TIME_5_MIN_IN_MS;
	            	m.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), time, service);
	            }
	        }
	    }
	 @Override
	 public void onDeleted(Context context, int[] AppWidgetIds){
		 Log.i("M","onDeleted");
		 final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);  
		 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        SharedPreferences.Editor editor= prefs.edit();
	        editor.putBoolean("widgetEnabled", false);
	        editor.putString("widget_prev_uuid", prefs.getString("widget_uuid", ""));
	        editor.remove("widget_uuid");
	        editor.commit();
	        m.cancel(service);
	 }
	 @Override  
	    public void onDisabled(Context context)  
	    {  
		 Log.i("M","onDisabled");
		 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        SharedPreferences.Editor editor= prefs.edit();
	        editor.putBoolean("widgetEnabled", false);
	        editor.putString("widget_prev_uuid", prefs.getString("widget_uuid", ""));
	        editor.remove("widget_uuid");
	        editor.commit();
	        final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);  
	  
	        m.cancel(service);  
	        
	    }  
	
}
