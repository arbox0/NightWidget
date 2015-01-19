package com.nightscoutwidget.android.widget;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
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
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        for (int i=0; i<N; i++) {
	            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_main);
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
	                service = PendingIntent.getService(context, 0, in, PendingIntent.FLAG_CANCEL_CURRENT);  
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
	    public void onDisabled(Context context)  
	    {  
	        final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);  
	  
	        m.cancel(service);  
	    }  
	
}
