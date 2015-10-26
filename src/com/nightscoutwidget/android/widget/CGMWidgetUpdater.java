package com.nightscoutwidget.android.widget;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import ch.qos.logback.classic.Logger;

import com.nightscoutwidget.android.R;
import com.nightscoutwidget.android.download.DownloadHelper;
import com.nightscoutwidget.android.download.ToggleRunnableAction;
import com.nightscoutwidget.android.medtronic.Constants;
/**
 * 
 * @author lmmarguenda
 *
 */
public class CGMWidgetUpdater extends Service {

	public static int UPDATE_FREQUENCY_SEC = 10;
	private Logger log = (Logger)LoggerFactory.getLogger(CGMWidget.class.getName());
	
	SharedPreferences prefs;
	DownloadHelper dwHelper = null;
	private int cgmSelected = Constants.DEXCOMG4;
	private boolean iUnderstand = false;
	private int currentAction = Constants.ACTION_SHOW_PHONEDATA;
	private String uuid = null;
	private SharedPreferences settings = null;
	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		settings  = getBaseContext().getSharedPreferences("widget_prefs", 0);
		log.info("CREATEEEEEEE");
		 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	        // Perform this loop procedure for each App Widget that belongs to this provider
	        SharedPreferences.Editor editor= prefs.edit();
	        uuid = UUID.randomUUID().toString();
	        editor.putString("widget_uuid", uuid);
	        editor.commit();
		if (prefs.contains("IUNDERSTAND_widget"))
			iUnderstand = prefs.getBoolean("IUNDERSTAND_widget", false);
		if (prefs.contains("monitor_type_widget")) {
			String type = prefs.getString("monitor_type_widget", "1");
			if ("2".equalsIgnoreCase(type)) {
				cgmSelected = Constants.MEDTRONIC_CGM;
			} else {
				cgmSelected = Constants.DEXCOMG4;
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log.info("onStartCommand");
		if (prefs.contains("IUNDERSTAND_widget")) {
			iUnderstand = prefs.getBoolean("IUNDERSTAND_widget", false);
		} else if (prefs.contains("monitor_type_widget")) {
			String type = prefs.getString("monitor_type_widget", "1");
			if ("2".equalsIgnoreCase(type)) {
				cgmSelected = Constants.MEDTRONIC_CGM;
			} else {
				cgmSelected = Constants.DEXCOMG4;
			}
		}
		if (!prefs.getBoolean("widgetEnabled", true)){
			this.stopSelf();
			if (dwHelper!= null){
				log.info("remove toggle");
				dwHelper.mHandlerToggleInfo.removeCallbacks(dwHelper.mToggleRunnableAction);
			}
			return super.onStartCommand(intent, flags, startId);
		}
		
		if (dwHelper!= null){
			log.info("remove toggle");
			dwHelper.mHandlerToggleInfo.removeCallbacks(dwHelper.mToggleRunnableAction);
		}
		buildUpdate();

		return super.onStartCommand(intent, flags, startId);
	}

	private void buildUpdate() {
		log.info("buildUpdate");
		RemoteViews views = null;
		KeyguardManager myKM = (KeyguardManager) getBaseContext()
				.getSystemService(Context.KEYGUARD_SERVICE);
		if (myKM.inKeyguardRestrictedInputMode()) {
			views = new RemoteViews(getPackageName(), R.layout.widget_main);
			if (prefs.getBoolean("showIcon_widget", true)){
    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
    		}else{
    			views.setViewVisibility(R.id.imageButton1, View.GONE);
    		}

		} else {
			SharedPreferences settings = getSharedPreferences("widget_prefs", 0);
			int screen = settings.getInt("widget_screen", 0);
            views = new RemoteViews(getPackageName(), R.layout.widget_main);
            if (screen == 0){
            	views = new RemoteViews(getPackageName(), R.layout.widget_main);
            }else if (screen == 1){
            	views = new RemoteViews(getPackageName(), R.layout.tablet_main_124_315);
            }else if (screen == 2){
            	views = new RemoteViews(getPackageName(), R.layout.tablet_main_194_315);
            }else{
            	views = new RemoteViews(getPackageName(), R.layout.tablet_main_264_315);
            }
			 String webUri = null;
		 		if (prefs.getString("web_uri_widget","").trim().equalsIgnoreCase(""))
		 			webUri = "http://www.nightscout.info/wiki/welcome";
		 		else
		 			webUri = prefs.getString("web_uri_widget","");
		 			
		 		if (webUri != null && webUri.length() > 0 && webUri.indexOf("http://")>=0){
			    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
				        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 7, intent, 0);
				        views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
		 		}
			if (prefs.getBoolean("showIcon_widget", true)){
    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
    		}else{
    			views.setViewVisibility(R.id.imageButton1, View.GONE);
    		}

		}
		
		ComponentName thisWidget = new ComponentName(this, CGMWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		
		if (!iUnderstand) {
			views.setTextViewText(R.id.sgv_id, "Please, Accept Disclaimer!!");
			manager.updateAppWidget(thisWidget, views);
			return;
		}
		
		dwHelper = new DownloadHelper(getBaseContext(), cgmSelected, prefs, settings);
		dwHelper.setPrefs(prefs);
		SharedPreferences settings = getBaseContext().getSharedPreferences("widget_prefs", 0);
		
	
		dwHelper.mToggleRunnableAction = new ToggleRunnableAction();
		dwHelper.mToggleRunnableAction.ctx = getBaseContext();
		dwHelper.mToggleRunnableAction.initScreen = settings.getInt("widget_screen", 0);
		dwHelper.mToggleRunnableAction.thisWidget = thisWidget;
		dwHelper.mToggleRunnableAction.manager = manager;
		dwHelper.mToggleRunnableAction.views = views;
		dwHelper.mToggleRunnableAction.currentID = uuid;
		dwHelper.mToggleRunnableAction.mHandlerToggleInfo = dwHelper.mHandlerToggleInfo;
		Object list[] = { thisWidget, manager, views };
		dwHelper.execute(list);
		
		//mHandlerToggleInfo.removeCallbacks(aux);
		// Push update for this widget to the home screen

	}
	@Override
	public void onDestroy(){
		log.info("ON DESTROY UPDATER");
		if (dwHelper!= null){
			dwHelper.mHandlerToggleInfo.removeCallbacks(dwHelper.mToggleRunnableAction);
		}
	}
	
	public JSONObject loadClassFile(File f) {

		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(f));
			Object o = ois.readObject();
			ois.close();
			return (JSONObject) o;
		} catch (Exception ex) {
			try {
				if (ois != null)
					ois.close();
			} catch (Exception e) {
				log.error("Error", e);
			}
		}
		return new JSONObject();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}