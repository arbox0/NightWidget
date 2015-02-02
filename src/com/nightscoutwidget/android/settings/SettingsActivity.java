package com.nightscoutwidget.android.settings;

import java.util.Locale;

import org.slf4j.LoggerFactory;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import ch.qos.logback.classic.Logger;

import com.nightscoutwidget.android.widget.CGMWidget;

public class SettingsActivity extends PreferenceActivity {
	private Logger log = (Logger)LoggerFactory.getLogger(CGMWidget.class.getName());
	public static String CONFIGURE_ACTION="android.appwidget.action.APPWIDGET_CONFIGURE";
	 int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    @Override
    public void onCreate(Bundle icicle) {

    	log.info("CONFIG ACTIVITY START ");
       setResult(RESULT_CANCELED);
       Intent intent = getIntent();
       Bundle extras = intent.getExtras();

       if (extras != null) {
           mAppWidgetId = extras.getInt(
                   AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
           SharedPreferences settings = getSharedPreferences("widget_prefs", 0);

           log.info("CONFIG ACTIVITY "+"widget_configuring_"+mAppWidgetId);
           settings.edit().putBoolean("widget_configuring_"+mAppWidgetId, true).commit();
		    /*AppWidgetHost appWidgetHost = new AppWidgetHost(getBaseContext(), 1); 
		    int[] appWidgetIDs = AppWidgetManager.getInstance(getBaseContext())
				     .getAppWidgetIds(new ComponentName(getBaseContext(), CGMWidget.class));


	         final Intent in = new Intent(getBaseContext(), CGMWidgetUpdater.class);  
	   
			 boolean alarmUp = (PendingIntent.getService(getBaseContext(), 27, in, 
				        PendingIntent.FLAG_NO_CREATE) != null);

			Log.i("on Activity", "start Config "+ alarmUp);
			 for (int i = 0; i < appWidgetIDs.length; i++) {
			     int id = appWidgetIDs[i];
			     String key = String.format(Locale.US,"appwidget%d_configured", id);
			     if (!alarmUp && (mAppWidgetId != id)){
			    	 Log.i("M","onUPDATE KILLING "+key);
			         appWidgetHost.deleteAppWidgetId(id);
			         settings.edit().remove("widget_ops_"+id).commit();
			         settings.edit().remove("widget_configuring_"+id).commit();
			     }
			 }*/
       }
        super.onCreate(icicle);
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
       
        // Find the widget id from the intent. 
       
        /* set fragment */
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment(mAppWidgetId, this)).commit();
    }
    @Override
    public void onDestroy(){

    	log.info( "Ondestroy "+ getIntent().getAction());
		if (CONFIGURE_ACTION.equals(getIntent().getAction())) {

			Intent intent=getIntent();
			Bundle extras=intent.getExtras();

			if (extras!=null) {

				log.info("NOT NULL!!");
				int id=extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
															AppWidgetManager.INVALID_APPWIDGET_ID);
				

				log.info("ID!!!--> " + id);
				AppWidgetManager.getInstance(this);

				Intent result=new Intent();
				result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
				setResult(RESULT_OK, result);
				String key = String.format(Locale.US,"appwidget%d_configured", id);
				    SharedPreferences prefs = getSharedPreferences("widget_prefs", 0);
				    prefs.edit().putBoolean(key, true).commit();
				sendBroadcast(new Intent(this, CGMWidget.class));
			}else{
				setResult(RESULT_OK);
				sendBroadcast(new Intent(this, CGMWidget.class));
			}

		}
		SharedPreferences prefs = getSharedPreferences("widget_prefs", 0);
		prefs.edit().putBoolean("widget_configuring_"+mAppWidgetId,false).commit();
		super.onDestroy();
    }
    @Override
	public void onBackPressed() {

    	log.info("BACK pressed "+ getIntent().getAction());
		if (CONFIGURE_ACTION.equals(getIntent().getAction())) {

			log.info("first if");
			Intent intent=getIntent();
			Bundle extras=intent.getExtras();

			if (extras!=null) {

				log.info("NOT NULL EXTRas");
				int id=extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
															AppWidgetManager.INVALID_APPWIDGET_ID);
				log.info("ID!!!--> " + id);
				AppWidgetManager.getInstance(this);

				Intent result=new Intent();
				result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
				setResult(RESULT_OK, result);
				String key = String.format(Locale.US,"appwidget%d_configured", id);
				    SharedPreferences settings = getSharedPreferences("widget_prefs", 0);
				    settings.edit().putBoolean(key, true).commit();
				sendBroadcast(new Intent(this, CGMWidget.class));
			}else{
				setResult(RESULT_OK);
				sendBroadcast(new Intent(this, CGMWidget.class));
			}

		}
		SharedPreferences settings = getSharedPreferences("widget_prefs", 0);
		settings.edit().putBoolean("widget_configuring_"+mAppWidgetId,false).commit();
		super.onBackPressed();
	} 
}