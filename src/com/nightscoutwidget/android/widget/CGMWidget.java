package com.nightscoutwidget.android.widget;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.nightscoutwidget.android.R;
import com.nightscoutwidget.android.medtronic.Constants;
import com.nightscoutwidget.android.settings.SettingsActivity;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
/**
 * 
 * @author lmmarguenda
 *
 */
public class CGMWidget extends AppWidgetProvider {
	//private Logger log = (Logger)LoggerFactory.getLogger(CGMWidget.class.getName());
	private Logger log = (Logger)LoggerFactory.getLogger(CGMWidget.class.getName());
	private static PendingIntent service = null;  
	private static int MAX_OPORTUNITIES = 4;
	public static String TRIGGER_CONFIGURATION_ACTION = "android.nigthwidget.action.TRIGGER_CONFIGURATION_ACTION";
	public static Handler mHandlerWatchService = new Handler();
	public static WatchServiceAction mWatchAction = null;
	
	 @Override
	 public void onEnabled(Context context) {
		 if (android.os.Build.VERSION.SDK_INT > 9) 
			{
			    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			    StrictMode.setThreadPolicy(policy);
			}
		 SharedPreferences settings = context.getSharedPreferences("widget_prefs", 0);
		 settings.edit().putString("widgetTag", "nightWidget_030215").commit();
		 settings.edit().putInt("widgetId", 1717030215).commit();
		// settings.edit().putLong("widget_ref_watch", System.currentTimeMillis()).commit();
		 AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		 LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		    StatusPrinter.print(lc);
		 int[] appWidgetIDs = appWidgetManager
		     .getAppWidgetIds(new ComponentName(context, CGMWidget.class));
		 if (appWidgetIDs.length > 0)
		 {
			 log.info("ENABLE Length "+appWidgetIDs.length);
			 String key = "widget_configuring_"+appWidgetIDs[0];
			 if (!settings.contains(key))
				 settings.edit().putBoolean(key, true).commit();
		 }
			 log.debug("ON ENABLE WIDGET!!!");
	        CGMWidget.service = null;
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        SharedPreferences.Editor editor= prefs.edit();
	        editor.putBoolean("widgetEnabled", true);
	        editor.commit();
	        
			 settings.edit().putString("widgetTag", "nightWidget_030215").commit();
			 settings.edit().putInt("widgetId", 1717030215).commit();
			// settings.edit().putLong("widget_ref_watch", System.currentTimeMillis()).commit();
			 int screen = settings.getInt("widget_screen", 0);
            RemoteViews views = null;
            if (screen == 0){
            	views = new RemoteViews(context.getPackageName(), R.layout.widget_main);
            }else if (screen == 1){
            	views = new RemoteViews(context.getPackageName(), R.layout.tablet_main_124_315);
            }else if (screen == 2){
            	views = new RemoteViews(context.getPackageName(), R.layout.tablet_main_194_315);
            }else{
            	views = new RemoteViews(context.getPackageName(), R.layout.tablet_main_264_315);
            }
            
            if (prefs.getBoolean("showSGV_widget", true)){
				views.setViewVisibility(R.id.linearLayout2, View.VISIBLE);
				
				views.setViewVisibility(R.id.linearLayout3, View.GONE);	
			}else if (!prefs.getBoolean("showSGV_widget", true)){
				
				views.setViewVisibility(R.id.linearLayout2, View.GONE);
				
				views.setViewVisibility(R.id.linearLayout3, View.VISIBLE);	
			}
            String webUri = null;
    		if (prefs.getString("web_uri_widget","").trim().equalsIgnoreCase(""))
    			webUri = "http://www.nightscout.info/wiki/welcome";
    		else
    			webUri = prefs.getString("web_uri_widget","");
    			
    		if (webUri != null && webUri.length() > 0 && webUri.indexOf("http://")>=0){
	    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
		        PendingIntent pendingIntent = PendingIntent.getActivity(context, 7, intent, 0);
		        views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
    		}
			for (int id : appWidgetIDs){
				
				Intent intent = new Intent(context,
						SettingsActivity.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, 0);
				views.setOnClickPendingIntent(R.id.widgetSetting, pendingIntent);
			}
    		if (prefs.getBoolean("showIcon_widget", true)){
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
            int i = 100;
            
            boolean alarmUp = (PendingIntent.getService(context, 27, in, 
			        PendingIntent.FLAG_NO_CREATE) != null);
            while (alarmUp && i > 0){
            	i--;
            	PendingIntent pI = PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE);
            	if (pI != null)
            		pI.cancel();
            	m.cancel(PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE));
            	alarmUp = (PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE) != null);
            	service = null;
            }
            log.debug("I HAVE KILLED SERVICES!!!");
            if (service == null)  
            {  
                service = PendingIntent.getService(context, 27, in, PendingIntent.FLAG_CANCEL_CURRENT);  
              
	            if (prefs.contains("refreshPeriod_widget")){
	            	String type = prefs.getString("refreshPeriod_widget", "2");
	            	long time = Constants.TIME_2_MIN_IN_MS;
	            	if (type.equalsIgnoreCase("1"))
	            		time = Constants.TIME_1_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("3"))
	            		time = Constants.TIME_3_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("4"))
	            		time = Constants.TIME_4_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("5"))
	            		time = Constants.TIME_5_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("6"))
	            		time = Constants.TIME_10_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("7"))
	            		time = Constants.TIME_20_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("8"))
	            		time = Constants.TIME_25_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("9"))
	            		time = Constants.TIME_30_MIN_IN_MS;
	            	else if (type.equalsIgnoreCase("10"))
	            		time = Constants.TIME_60_MIN_IN_MS;
	            	else
	            		time = Constants.TIME_2_MIN_IN_MS;
	            	settings.edit().putLong("widget_ref_watch", System.currentTimeMillis()).commit();
	            	m.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), time, service);
	            }
            }
            if (mWatchAction != null && mHandlerWatchService != null){
            	mHandlerWatchService.removeCallbacks(mWatchAction);
            }
            mWatchAction = new WatchServiceAction(context);
            mHandlerWatchService.postDelayed(mWatchAction, 15000);
	    }
	 @Override
	 public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		 if (android.os.Build.VERSION.SDK_INT > 9) 
			{
			    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			    StrictMode.setThreadPolicy(policy);
			}
		 AppWidgetHost appWidgetHost = new AppWidgetHost(context, 1); // for removing phantoms
		 SharedPreferences settings = context.getSharedPreferences("widget_prefs", 0);
		 SharedPreferences prefs = PreferenceManager
			.getDefaultSharedPreferences(context);
         final Intent in = new Intent(context, CGMWidgetUpdater.class);  
   
		 boolean alarmUp = (PendingIntent.getService(context, 27, in, 
			        PendingIntent.FLAG_NO_CREATE) != null);
		 long time = Constants.TIME_2_MIN_IN_MS;
         	String type = prefs.getString("refreshPeriod_widget", "2");
         	if (type.equalsIgnoreCase("1"))
         		time = Constants.TIME_1_MIN_IN_MS;
         	else if (type.equalsIgnoreCase("3"))
         		time = Constants.TIME_3_MIN_IN_MS;
         	else if (type.equalsIgnoreCase("4"))
         		time = Constants.TIME_4_MIN_IN_MS;
         	else if (type.equalsIgnoreCase("5"))
         		time = Constants.TIME_5_MIN_IN_MS;
         	else if (type.equalsIgnoreCase("6"))
         		time = Constants.TIME_10_MIN_IN_MS;
         	else if (type.equalsIgnoreCase("7"))
         		time = Constants.TIME_20_MIN_IN_MS;
         	else if (type.equalsIgnoreCase("8"))
         		time = Constants.TIME_25_MIN_IN_MS;
         	else if (type.equalsIgnoreCase("9"))
         		time = Constants.TIME_30_MIN_IN_MS;
         	else if (type.equalsIgnoreCase("10"))
         		time = Constants.TIME_60_MIN_IN_MS;
         	else
         		time = Constants.TIME_2_MIN_IN_MS;
         	 int screen = settings.getInt("widget_screen", 0);
             RemoteViews views = null;
             if (screen == 0){
             	views = new RemoteViews(context.getPackageName(), R.layout.widget_main);
             }else if (screen == 1){
             	views = new RemoteViews(context.getPackageName(), R.layout.tablet_main_124_315);
             }else if (screen == 2){
             	views = new RemoteViews(context.getPackageName(), R.layout.tablet_main_194_315);
             }else{
             	views = new RemoteViews(context.getPackageName(), R.layout.tablet_main_264_315);
             }
             
             if (prefs.getBoolean("showSGV_widget", true)){
    				views.setViewVisibility(R.id.linearLayout2, View.VISIBLE);
    				
    				views.setViewVisibility(R.id.linearLayout3, View.GONE);	
    			}else if (!prefs.getBoolean("showSGV_widget", true)){
    				
    				views.setViewVisibility(R.id.linearLayout2, View.GONE);
    				
    				views.setViewVisibility(R.id.linearLayout3, View.VISIBLE);	
    			}
             String webUri = null;
     		if (prefs.getString("web_uri_widget","").trim().equalsIgnoreCase(""))
     			webUri = "http://www.nightscout.info/wiki/welcome";
     		else
     			webUri = prefs.getString("web_uri_widget","");
     			
     		if (webUri != null && webUri.length() > 0 && webUri.indexOf("http://")>=0){
    	    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
    		        PendingIntent pendingIntent = PendingIntent.getActivity(context, 7, intent, 0);
    		        views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
     		}
    			for (int id : appWidgetIds){
    				
    				Intent intent = new Intent(context,
    						SettingsActivity.class);
    				PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, 0);
    				views.setOnClickPendingIntent(R.id.widgetSetting, pendingIntent);
    			}
     		if (prefs.getBoolean("showIcon_widget", true)){
     			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
     		}else{
     			views.setViewVisibility(R.id.imageButton1, View.GONE);
     		}

		log.info("onUpdate "+ alarmUp);
		 for (int i = 0; i < appWidgetIds.length; i++) {
		     int id = appWidgetIds[i];
		     String key = String.format(Locale.US,"appwidget%d_configured", id);
		     if ((settings.contains("widget_configuring_"+id) && !settings.contains(key))){
			     boolean isConfiguring = settings.getBoolean("widget_configuring_"+id, false);
			     if ((!settings.getBoolean(key, false) && !isConfiguring)) {
			         // delete the phantom appwidget
			    	 log.info("onUPDATE KILLING "+key);
			         appWidgetHost.deleteAppWidgetId(id);
			         settings.edit().remove("widget_ops_"+id).commit();
			         settings.edit().remove("widget_configuring_"+id).commit();
			     }
		     }else{
		    	 int size = settings.getInt("widget_ops_"+id, 0);
		    	 if (!settings.contains(key) && appWidgetIds.length > 1){
			    	 if (size <= MAX_OPORTUNITIES)
			    		 settings.edit().putInt("widget_ops_"+id, ++size).commit();
			    	 else{
			    		 log.info("onUPDATE END OF OPORTUNITIES KILLING "+key);
				         appWidgetHost.deleteAppWidgetId(id);
				         settings.edit().remove("widget_ops_"+id).commit();
				         settings.edit().remove("widget_configuring_"+id).commit();
			    	 }
		    	 }
		     }
		     appWidgetManager.updateAppWidget(id, views);
		 }
		 final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		 if (!alarmUp ){
			 log.warn("ALARM IS DOWN I MUST REACTIVATE");
			 final Calendar TIME = Calendar.getInstance();  
	            TIME.set(Calendar.MINUTE, 0);  
	            TIME.set(Calendar.SECOND, 0);  
	            TIME.set(Calendar.MILLISECOND, 0);  
	         service = PendingIntent.getService(context, 27, in, PendingIntent.FLAG_CANCEL_CURRENT);  
	            	m.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), time, service);
	         log.warn("ALARM IS DOWN I MUST REACTIVATE");
		 }
		
 		
	 }
	 @Override
	 public void onDeleted(Context context, int[] AppWidgetIds){
		 log.info("onDeleted");
		 SharedPreferences settings = context.getSharedPreferences("widget_prefs", 0);
		 for (int id : AppWidgetIds){
			 String key = String.format(Locale.US,"appwidget%d_configured", id);
			 settings.edit().remove("widget_ops_"+id).commit();
	         settings.edit().remove("widget_configuring_"+id).commit();
			 log.info("onDeleted "+key);
			 if (settings.contains(key))
				 settings.edit().remove(key).commit();
		 }
	 }
	 
	 @Override
	  public void onAppWidgetOptionsChanged(Context ctxt,
	                                        AppWidgetManager mgr,
	                                        int appWidgetId,
	                                        Bundle newOptions) {
	    
		 
		int  min_height = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
		SharedPreferences settings = ctxt.getSharedPreferences("widget_prefs", 0);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
		 
		String msg=
		        String.format(Locale.getDefault(),
		                      "[%d-%d] x [%d-%d]",
		                      newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
		                      newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
		                      newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
		                      newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
		log.info(msg);
		RemoteViews views = null;
		if (min_height < 124){
			views = new RemoteViews(ctxt.getPackageName(), R.layout.widget_main);
			log.info("SCREEN "+0);
	         settings.edit().putInt("widget_screen", 0).commit();
		}else if (min_height >= 124 && min_height < 194){
			settings.edit().putInt("widget_screen", 1).commit();
			log.info("SCREEN "+1);
			views = new RemoteViews(ctxt.getPackageName(), R.layout.tablet_main_124_315);
		}else if (min_height >= 194 && min_height < 264){
			settings.edit().putInt("widget_screen", 2).commit();
			views = new RemoteViews(ctxt.getPackageName(), R.layout.tablet_main_194_315);
			log.info("SCREEN "+2);
		}else if (min_height >= 264){
			settings.edit().putInt("widget_screen", 3).commit();
			views = new RemoteViews(ctxt.getPackageName(), R.layout.tablet_main_264_315);
			log.info("SCREEN "+3);
		}
		
			
			Intent intent1 = new Intent(ctxt,
					SettingsActivity.class);
			PendingIntent pendingIntent1 = PendingIntent.getActivity(ctxt, appWidgetId, intent1, 0);
			views.setOnClickPendingIntent(R.id.widgetSetting, pendingIntent1);
		
		 if (prefs.getBoolean("showSGV_widget", true)){
				views.setViewVisibility(R.id.linearLayout2, View.VISIBLE);
				
				views.setViewVisibility(R.id.linearLayout3, View.GONE);	
			}else if (!prefs.getBoolean("showSGV_widget", true)){
				
				views.setViewVisibility(R.id.linearLayout2, View.GONE);
				
				views.setViewVisibility(R.id.linearLayout3, View.VISIBLE);	
			}
         String webUri = null;
 		if (prefs.getString("web_uri_widget","").trim().equalsIgnoreCase(""))
 			webUri = "http://www.nightscout.info/wiki/welcome";
 		else
 			webUri = prefs.getString("web_uri_widget","");
 			
 		if (webUri != null && webUri.length() > 0 && webUri.indexOf("http://")>=0){
	    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
		        PendingIntent pendingIntent = PendingIntent.getActivity(ctxt, 7, intent, 0);
		        views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
 		}
 		if (prefs.getBoolean("showIcon_widget", true)){
 			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
 		}else{
 			views.setViewVisibility(R.id.imageButton1, View.GONE);
 		}

		 mgr.updateAppWidget(appWidgetId, views);
	  }
	 
	 
	 @Override  
	    public void onDisabled(Context context)  
	    {  
		 log.info("onDisabled");
		 SharedPreferences settings = context.getSharedPreferences("widget_prefs", 0);
		 AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

		 int[] appWidgetIDs = appWidgetManager
		     .getAppWidgetIds(new ComponentName(context, CGMWidget.class));
		 if (appWidgetIDs.length > 0)
		 {
			 log.info("DISABLE Length "+appWidgetIDs.length);
			 String key = String.format(Locale.US,"appwidget%d_configured", appWidgetIDs[0]);
			 settings.edit().remove("widget_ops_"+appWidgetIDs[0]).commit();
	         settings.edit().remove("widget_configuring_"+appWidgetIDs[0]).commit();
			 if (settings.contains(key))
				 settings.edit().remove(key).commit();
		 }
		
		 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        SharedPreferences.Editor editor= prefs.edit();
	        editor.putBoolean("widgetEnabled", false);
	        editor.remove("widget_uuid");
	        editor.commit();
	        final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);  
	  
	        if (service == null){
				 log.info("ISNULL!!!!");
			 }else
				 m.cancel(service);
	        final Intent in = new Intent(context, CGMWidgetUpdater.class);  
            int i = 100;
            
            boolean alarmUp = (PendingIntent.getService(context, 27, in, 
			        PendingIntent.FLAG_NO_CREATE) != null);
            while (alarmUp && i > 0){
            	log.warn("I AM KILLING SERVICES " +i);
            	i--;
            	PendingIntent pI = PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE);
            	if (pI != null)
            		pI.cancel();
            	m.cancel(PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE));
            	alarmUp = (PendingIntent.getService(context, 27, in, 
    			        PendingIntent.FLAG_NO_CREATE) != null);
            	service = null;
            }
            log.warn("I HAVE KILLED SERVICES ");
            mHandlerWatchService.removeCallbacks(mWatchAction);
	    }  
	 
	
	 class WatchServiceAction implements Runnable{
		 Context context = null;
		 public WatchServiceAction (Context context){
			 this.context = context;
		 }
		@Override
		public void run() {
			// TODO Auto-generated method stub
		
				 SharedPreferences settings = context.getSharedPreferences("widget_prefs", 0);
				 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            	String type = prefs.getString("refreshPeriod_widget", "2");
            	System.out.println("TYPE =" +type);
            	long time = Constants.TIME_2_MIN_IN_MS;
            	if (type.equalsIgnoreCase("1"))
            		time = Constants.TIME_1_MIN_IN_MS;
            	else if (type.equalsIgnoreCase("3"))
            		time = Constants.TIME_3_MIN_IN_MS;
            	else if (type.equalsIgnoreCase("4"))
            		time = Constants.TIME_4_MIN_IN_MS;
            	else if (type.equalsIgnoreCase("5"))
            		time = Constants.TIME_5_MIN_IN_MS;
            	else if (type.equalsIgnoreCase("6"))
            		time = Constants.TIME_10_MIN_IN_MS;
            	else if (type.equalsIgnoreCase("7"))
            		time = Constants.TIME_20_MIN_IN_MS;
            	else if (type.equalsIgnoreCase("8"))
            		time = Constants.TIME_25_MIN_IN_MS;
            	else if (type.equalsIgnoreCase("9"))
            		time = Constants.TIME_30_MIN_IN_MS;
            	else if (type.equalsIgnoreCase("10"))
            		time = Constants.TIME_60_MIN_IN_MS;
            	else
            		time = Constants.TIME_2_MIN_IN_MS;
		         
		         final Intent in = new Intent(context, CGMWidgetUpdater.class);  
		   
				 boolean alarmUp = (PendingIntent.getService(context, 27, in, 
					        PendingIntent.FLAG_NO_CREATE) != null);
				 long current = System.currentTimeMillis();
				 boolean refresh = (current - settings.getLong("widget_ref_watch", current)) >= (time + 15000);
				 System.out.println("refresh?? =" +refresh);
				 
				 final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				 if (refresh){
					 int i = 100;
					 while (alarmUp && i > 0){
			            	log.warn("I AM KILLING SERVICES" +i);
			            	i--;
			            	PendingIntent pI = PendingIntent.getService(context, 27, in, 
			    			        PendingIntent.FLAG_NO_CREATE);
			            	if (pI != null)
			            		pI.cancel();
			            	m.cancel(PendingIntent.getService(context, 27, in, 
			    			        PendingIntent.FLAG_NO_CREATE));
			            	alarmUp = (PendingIntent.getService(context, 27, in, 
			    			        PendingIntent.FLAG_NO_CREATE) != null);
			            	service = null;
			         }
					 settings.edit().putLong("widget_ref_watch", current).commit();
				 }
				Log.i("WatchTask", "WatchTask "+ alarmUp+ " "+(current - settings.getLong("widget_ref_watch", current))+" "+ time);
				log.info("WatchTask "+ alarmUp+ " "+(current - settings.getLong("widget_ref_watch", current))+" "+ time);
				 if (!alarmUp){
					 log.warn("ALARM IS DOWN I MUST REACTIVATE");
					System.out.println("ALARM IS DOWN I MUST REACTIVATE");
					 final Calendar TIME = Calendar.getInstance();  
			            TIME.set(Calendar.MINUTE, 0);  
			            TIME.set(Calendar.SECOND, 0);  
			            TIME.set(Calendar.MILLISECOND, 0);  
			         service = PendingIntent.getService(context, 27, in, PendingIntent.FLAG_CANCEL_CURRENT);  
			         
			         m.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), time, service);
			         
			         log.warn("ALARM REACTIVATED");
				 }
				 mHandlerWatchService.postDelayed(mWatchAction, 15000);
			 }
		
		 
	 }
}
