package com.nightscoutwidget.android.widget;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import org.json.JSONObject;

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
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.nightscoutwidget.android.R;
import com.nightscoutwidget.android.download.DownloadHelper;
import com.nightscoutwidget.android.medtronic.Constants;
/**
 * 
 * @author lmmarguenda
 *
 */
public class CGMWidgetUpdater extends Service {

	public static int UPDATE_FREQUENCY_SEC = 10;
	SharedPreferences prefs;
	DownloadHelper dwHelper = null;
	private int cgmSelected = Constants.DEXCOMG4;
	private boolean iUnderstand = false;

	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		if (prefs.contains("IUNDERSTAND"))
			iUnderstand = prefs.getBoolean("IUNDERSTAND", false);
		if (prefs.contains("monitor_type")) {
			String type = prefs.getString("monitor_type", "1");
			if ("2".equalsIgnoreCase(type)) {
				cgmSelected = Constants.MEDTRONIC_CGM;
			} else {
				cgmSelected = Constants.DEXCOMG4;
			}
		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (prefs.contains("IUNDERSTAND")) {
			iUnderstand = prefs.getBoolean("IUNDERSTAND", false);
		} else if (prefs.contains("monitor_type")) {
			String type = prefs.getString("monitor_type", "1");
			if ("2".equalsIgnoreCase(type)) {
				cgmSelected = Constants.MEDTRONIC_CGM;
			} else {
				cgmSelected = Constants.DEXCOMG4;
			}
		}
		buildUpdate();

		return super.onStartCommand(intent, flags, startId);
	}

	private void buildUpdate() {

		RemoteViews views = null;
		KeyguardManager myKM = (KeyguardManager) getBaseContext()
				.getSystemService(Context.KEYGUARD_SERVICE);
		if (myKM.inKeyguardRestrictedInputMode()) {
			views = new RemoteViews(getPackageName(), R.layout.widget_main);
			if (prefs.getBoolean("showIcon", true)){
    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
    		}else{
    			views.setViewVisibility(R.id.imageButton1, View.GONE);
    		}

		} else {
			views = new RemoteViews(getPackageName(), R.layout.widget_main);
			String webUri = null;
			if (prefs.contains("web_uri"))
				webUri = prefs.getString("web_uri",
						"http://www.nightscout.info/wiki/welcome");
			if (webUri != null && webUri.length() > 0
					&& webUri.indexOf("http://") >= 0) {
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(webUri));
				PendingIntent pendingIntent = PendingIntent.getActivity(
						getBaseContext(), 7, intent, 0);
				views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
			}
			if (prefs.getBoolean("showIcon", true)){
    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
    		}else{
    			views.setViewVisibility(R.id.imageButton1, View.GONE);
    		}

		}
		if (!iUnderstand) {
			views.setTextViewText(R.id.sgv_id, "Please, Accept Disclaimer!!");
			return;
		}
		dwHelper = new DownloadHelper(getBaseContext(), cgmSelected, prefs);
		dwHelper.setPrefs(prefs);
		ComponentName thisWidget = new ComponentName(this, CGMWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);

		Object list[] = { thisWidget, manager, views };
		dwHelper.execute(list);

		// Push update for this widget to the home screen

	}

	public JSONObject loadClassFile(File f) {
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(f));
			Object o = ois.readObject();
			ois.close();
			return (JSONObject) o;
		} catch (Exception ex) {
			Log.w("CGMWidget", " unable to loadEGVRecord");
			try {
				if (ois != null)
					ois.close();
			} catch (Exception e) {
				Log.e("CGMWidget", " Error closing ObjectInputStream");
			}
		}
		return new JSONObject();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}