package com.nightscoutwidget.android.download;

import org.slf4j.LoggerFactory;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import ch.qos.logback.classic.Logger;

import com.nightscoutwidget.android.R;
import com.nightscoutwidget.android.settings.SettingsActivity;
import com.nightscoutwidget.android.widget.CGMWidget;

public class ToggleRunnableAction implements Runnable, OnSharedPreferenceChangeListener {
	private Logger log = (Logger)LoggerFactory.getLogger(CGMWidget.class.getName());
	public ComponentName thisWidget = null;
	public RemoteViews views = null;
	public AppWidgetManager manager = null;
	public Context ctx = null;
	public String currentID = null;
	public Handler mHandlerToggleInfo = null;
	public boolean showMBG =  true;
	public boolean showInsulin = true;
	public boolean showUploaderBattery = true;
	public boolean showPumpBattery = true;
	public boolean showDataDifference = true;
	public long creationTime = 0;
	
	public ToggleRunnableAction() {
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		String actID = prefs.getString("widget_uuid","");
		long current = System.currentTimeMillis();
		long diff = 0;
		long lastCreated = prefs.getLong("lastCreatedWidgetDate", 0);
		boolean showSGV = prefs.getBoolean("showSGV", true);
		
		if (this.creationTime < lastCreated){
			mHandlerToggleInfo.removeCallbacks(this);
			log.info("TOGGLE EXIT!");
			return;
		}

		if ( currentID == null || "".equals(currentID) || "".equals(actID)){
			mHandlerToggleInfo.removeCallbacks(this);
			log.info("TOGGLE EXIT!");
			return;
		}
		
		if (!prefs.getBoolean("widgetEnabled", true) || !actID.equals(currentID)){
			mHandlerToggleInfo.removeCallbacks(this);
			log.info("TOGGLE EXIT!");
			return;
			
		}
		
		SharedPreferences.Editor editor = prefs.edit();
		if (prefs.getLong("timeMBG", 0) != 0) {
			diff = current - prefs.getLong("timeMBG", 0);
		} else {
			editor.putLong("timeMBG", current);
		}
		KeyguardManager myKM = (KeyguardManager) ctx
				.getSystemService(Context.KEYGUARD_SERVICE);
		if (myKM.inKeyguardRestrictedInputMode()) {
			if (prefs.getBoolean("showIcon", true)){
    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
    		}else{
    			views.setViewVisibility(R.id.imageButton1, View.GONE);
    		}

		} else {
			String webUri = null;
			if (prefs.contains("web_uri"))
				webUri = prefs.getString("web_uri",
						"http://www.nightscout.info/wiki/welcome");
			if (webUri != null && webUri.length() > 0
					&& webUri.indexOf("http://") >= 0) {
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(webUri));
				PendingIntent pendingIntent = PendingIntent.getActivity(
						 ctx, 7, intent, 0);
				views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
			}
			if (prefs.getBoolean("showIcon", true)){
    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
    		}else{
    			views.setViewVisibility(R.id.imageButton1, View.GONE);
    		}

		}	
		int[] appIDs = manager.getAppWidgetIds(thisWidget);
		for (int id : appIDs){
			
			Intent intent = new Intent(ctx,
					SettingsActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(ctx, id, intent, 0);
			views.setOnClickPendingIntent(R.id.widgetSetting, pendingIntent);
		}
		editor.commit();
		if (prefs.getBoolean("show_mobile_battery", true) && prefs.getBoolean("show_MBG", true)){
			if (!showSGV && diff != current
					&& diff >= 10000) {
				prefs.edit().putBoolean("showSGV", true).commit();
				prefs.edit().putLong("timeMBG", current).commit();
				log.info("TOGGLE1 diff "+diff);
				if (showMBG && (showUploaderBattery)){
					views.setViewVisibility(R.id.linearLayout2, View.VISIBLE);
					views.setViewVisibility(R.id.linearLayout3, View.GONE);
				}
				manager.updateAppWidget(thisWidget, views);
			} else if (showSGV && diff != current
					&& diff >= 10000) {
				prefs.edit().putBoolean("showSGV", false).commit();
				prefs.edit().putLong("timeMBG", current).commit();
				log.info("TOGGLE2 "+diff);
				if (showMBG && (showUploaderBattery)){
					views.setViewVisibility(R.id.linearLayout3, View.VISIBLE);
					views.setViewVisibility(R.id.linearLayout2, View.GONE);
				}
				manager.updateAppWidget(thisWidget, views);
			}

		} else if (prefs.getBoolean("show_MBG", true) && !prefs.getBoolean("show_mobile_battery", true)){
			views.setViewVisibility(R.id.linearLayout3, View.VISIBLE);
			views.setViewVisibility(R.id.linearLayout2, View.GONE);
			manager.updateAppWidget(thisWidget, views);
		}else {
			views.setViewVisibility(R.id.linearLayout3, View.GONE);
			views.setViewVisibility(R.id.linearLayout2, View.VISIBLE);
			manager.updateAppWidget(thisWidget, views);
		}
		mHandlerToggleInfo.removeCallbacks(this);
		mHandlerToggleInfo.postDelayed(this, 10000);
	}
	/**
	 * Check if the phone has internet access.
	 * 
	 * @return Boolean, true if the mobile phone has internet access.
	 */
	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		// TODO Auto-generated method stub
		if (arg1 != null && arg1.equalsIgnoreCase("widget_uuid")){
			if (arg0.contains(arg1) && arg0.getString(arg1, "") != null){
				if (!arg0.getString(arg1, "").equals(currentID)){
					mHandlerToggleInfo.removeCallbacks(this);
					log.info("TOGGLE EXIT!");
					return;
				}
			}else{
				mHandlerToggleInfo.removeCallbacks(this);
				log.info("TOGGLE EXIT!");
				return;
			}
		}
	}
}
