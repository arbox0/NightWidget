package com.nightscoutwidget.android.download;

import java.text.DecimalFormat;
import java.util.Locale;

import javax.net.ssl.ManagerFactoryParameters;

import org.json.JSONException;
import org.json.JSONObject;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.nightscoutwidget.android.R;
import com.nightscoutwidget.android.alerts.AlertActivity;
import com.nightscoutwidget.android.medtronic.Constants;

/**
 * This class has the responsability of download the last entries in the MongoDB and process them to match the Widget needs
 * @author lmmarguenda
 *
 */
public class DownloadHelper extends AsyncTask<Object, Void, Void> {

	private static final String TAG = "DownloadHelper";

	Context context;
	private int cgmSelected = Constants.DEXCOMG4;
	private SharedPreferences prefs = null;
	public boolean isCalculating = false;
	public JSONObject finalResult = null;

	/**
	 * Constructor.
	 * @param context, application or base context of the activity
	 * @param prefs, Shared preferences.
	 */
	public DownloadHelper(Context context, SharedPreferences prefs) {
		this(context, Constants.DEXCOMG4, prefs);
	}

	/**
	 * Constructor.
	 * @param context, application or base context of the activity
	 * @param cgmSelected, which type of CGM is selected Medtronic or DexCom
	 * @param prefs, Shared preferences.
	 */
	public DownloadHelper(Context context, int cgmSelected,
			SharedPreferences prefs) {
		this.context = context;
		this.cgmSelected = cgmSelected;
		this.prefs = prefs;
	}

	/**
	 * 
	 * @return
	 */
	private JSONObject doMongoDownload() {

		String dbURI = prefs.getString("MongoDB URI", null);
		String collectionName = prefs.getString("Collection Name", "entries");
		String dsCollectionName = prefs.getString(
				"DeviceStatus Collection Name", "devicestatus");
		// String gdCollectionName = prefs.getString("gcdCollectionName", null);
		String devicesCollectionName = "devices";
		JSONObject result = null;
		if (dbURI != null) {
			result = new JSONObject();
			MongoClient client = null;
			try {

				// connect to db
				MongoClientURI uri = new MongoClientURI(dbURI.trim());
				client = new MongoClient(uri);

				// get db
				DB db = client.getDB(uri.getDatabase());

				// get collection
				DBCollection dexcomData = null;
				// DBCollection glucomData = null;
				DBCollection deviceData = db
						.getCollection(devicesCollectionName);
				DBObject medtronicDevice = null;
				DBObject record = null;
				if (deviceData != null
						&& cgmSelected == Constants.MEDTRONIC_CGM) {
					DBCursor deviceCursor = deviceData
							.find(new BasicDBObject("deviceId", prefs
									.getString("medtronic_cgm_id", "")));
					if (deviceCursor.hasNext()) {
						medtronicDevice = deviceCursor.next();
						if (medtronicDevice.containsField("insulinLeft")) {
							result.put("insulinLeft",
									medtronicDevice.get("insulinLeft"));
						}
						if (medtronicDevice.containsField("alarm")) {
							result.put("alarm", medtronicDevice.get("alarm"));
						}
						if (medtronicDevice.containsField("batteryStatus")) {
							result.put("batteryStatus",
									medtronicDevice.get("batteryStatus"));
						}
						if (medtronicDevice.containsField("batteryVoltage")) {
							result.put("batteryVoltage",
									medtronicDevice.get("batteryVoltage"));
						}
						if (medtronicDevice.containsField("isWarmingUp")) {
							result.put("isWarmingUp",
									medtronicDevice.get("isWarmingUp"));
						}
					}
				}
				if (collectionName != null) {
					dexcomData = db.getCollection(collectionName.trim());
					DBCursor dexcomCursor = dexcomData.find()
							.sort(new BasicDBObject("date", -1)).limit(1);
					if (dexcomCursor.hasNext()) {
						record = dexcomCursor.next();
						if (record.containsField("date"))
							result.put("date", record.get("date"));
						if (record.containsField("dateString"))
							result.put("dateString", record.get("dateString"));
						if (record.containsField("device"))
							result.put("device", record.get("device"));
						if (record.containsField("sgv"))
							result.put("sgv", record.get("sgv"));
						if (record.containsField("direction"))
							result.put("direction", record.get("direction"));

						if (cgmSelected == Constants.MEDTRONIC_CGM) {
							if (record.containsField("calibrationStatus"))
								result.put("calibrationStatus",
										record.get("calibrationStatus"));
							if (record.containsField("isCalibrating"))
								result.put("isCalibrating",
										record.get("isCalibrating"));
						}
					}
				}
				/*
				 * if (gdCollectionName != null){ glucomData =
				 * db.getCollection(gdCollectionName.trim());
				 * glucomData.find().sort(new BasicDBObject("_id",-1)).limit(1);
				 * }
				 */

				DBCollection dsCollection = db.getCollection(dsCollectionName);
				DBObject deviceStatus = null;
				if (dsCollection != null) {
					DBCursor cursorDeviceStatus = dsCollection.find().sort(
							new BasicDBObject("created_at", -1));
					if (cursorDeviceStatus.hasNext()) {
						deviceStatus = cursorDeviceStatus.next();
						if (deviceStatus.containsField("uploaderBattery"))
							result.put("uploaderBattery",
									deviceStatus.get("uploaderBattery"));
						if (deviceStatus.containsField("created_at"))
							result.put("created_at",
									deviceStatus.get("created_at"));
					}
					cursorDeviceStatus.close();
				}
				// Uploading devicestatus
				client.close();
			} catch (Exception e) {
				if (client != null)
					client.close();

				StringBuffer sb1 = new StringBuffer("");
				sb1.append("EXCEPTION!!!!!! " + e.getMessage() + " "
						+ e.getCause());
				for (StackTraceElement st : e.getStackTrace()) {
					sb1.append(st.toString());
				}
				if (e.toString() != null
						&& e.toString().indexOf("authenticate") >= 0) {
					try {
						result.put("sgv", "AUTH?");
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
				}
				Log.e(TAG, sb1.toString());
			}
		}
		return result;
	}

	@Override
	protected Void doInBackground(Object... arg0) {
		if (arg0.length == 3) {
			ComponentName thisWidget = null;
			AppWidgetManager manager = null;
			RemoteViews views = null;
			if (arg0[0] instanceof ComponentName) {
				thisWidget = (ComponentName) arg0[0];
			} else
				return null;
			if (arg0[1] instanceof AppWidgetManager) {
				manager = (AppWidgetManager) arg0[1];
			} else
				return null;
			if (arg0[2] instanceof RemoteViews) {
				views = (RemoteViews) arg0[2];
			} else
				return null;
			finalResult = doMongoDownload();
			if (finalResult != null && isOnline())
				updateValues(finalResult, views);
			else{
				if (!isOnline()){
					views.setTextColor(R.id.sgv_id, Color.GRAY);
					views.setInt(R.id.sgv_id, "setPaintFlags",
						Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
				}
			}
			if (isOnline())
				manager.updateAppWidget(thisWidget, views);
		}
		return null;
	}

	/**
	 * Check if the phone has internet access.
	 * @return Boolean, true if the mobile phone has internet access.
	 */
	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	/**
	 * This method, process the downloaded entry and updates the widget to show
	 * the new data.
	 * @param result, JsonObject, downloaded entry.
	 * @param views, Access to the Widget UI.
	 */
	public void updateValues(JSONObject result, RemoteViews views) {
		String sgv = "";
		String direction = "";
		int calibrationStatus = -1;
		boolean isCalibrating = false;
		String calib = "---";
		String batteryStatus = "Normal";
		String itemSelected = prefs.getString("reservoir_ins_units", "2");
		int max_ins_units = 300;
		boolean isWarmingUp = false;
		try {
			isWarmingUp = result.getBoolean("isWarmingUp");
		} catch (Exception e) {

		}
		if ("1".equalsIgnoreCase(itemSelected))
			max_ins_units = 176;
		else
			max_ins_units = 300;
		try {
			if (result.has("insulinLeft")) {
				views.setViewVisibility(R.id.insulin_data_id, View.VISIBLE);
				views.setViewVisibility(R.id.resIcon, View.VISIBLE);
				double percentage = (result.getDouble("insulinLeft") / max_ins_units) * 100.0;
				if (percentage > 75)
					views.setImageViewResource(R.id.resIcon,
							R.drawable.res_full);
				else if (percentage < 75 && percentage > 50)
					views.setImageViewResource(R.id.resIcon,
							R.drawable.res_green);
				else if (percentage < 50 && percentage > 25)
					views.setImageViewResource(R.id.resIcon,
							R.drawable.res_yellow);
				else
					views.setImageViewResource(R.id.resIcon, R.drawable.res_red);
				views.setTextViewText(
						R.id.insulin_data_id,
						"" + new DecimalFormat("###").format(Math.floor(result
										.getDouble("insulinLeft")))+" U");
			} else {
				views.setViewVisibility(R.id.insulin_data_id, View.GONE);
				views.setViewVisibility(R.id.resIcon, View.GONE);
			}
		} catch (Exception e) {
			views.setViewVisibility(R.id.insulin_data_id, View.GONE);
			views.setViewVisibility(R.id.resIcon, View.GONE);
		}
		try {
			if (result.has("batteryStatus")) {
				views.setViewVisibility(R.id.devBattery, View.VISIBLE);
				batteryStatus = (String) result.get("batteryStatus");
				boolean isLow = false;
				if (batteryStatus.toLowerCase(Locale.getDefault()).indexOf(
						"normal") >= 0) {
					views.setImageViewResource(R.id.devBattery,
							R.drawable.battery_full_icon);
				} else {
					isLow = true;
					views.setImageViewResource(R.id.devBattery,
							R.drawable.battery_low_icon);
				}
				if (result.has("batteryVoltage")) {
					views.setViewVisibility(R.id.device_battery_text_id,
							View.VISIBLE);
					String batteryVolt = (String) result.get("batteryVoltage");
					views.setTextViewText(R.id.device_battery_text_id,
							batteryVolt + "v");
					if (batteryVolt != null && batteryVolt.length() > 0) {
						double val = 0;
						try {
							val = Double.parseDouble(batteryVolt);
						} catch (Exception e) {
							// val is still 0
						}
						if (val > 0) {
							if (val > 1.35) {
								if (!isLow)
									views.setImageViewResource(R.id.devBattery,
											R.drawable.battery_full_icon);
							} else if ((val < 1.3) && (val >= 1.2)) {
								if (!isLow)
									views.setImageViewResource(R.id.devBattery,
											R.drawable.battery_half_icon);
							} else if (val < 1.2) {
								views.setImageViewResource(R.id.devBattery,
										R.drawable.battery_low_icon);
							}
						}
					}
				} else {
					views.setViewVisibility(R.id.device_battery_text_id,
							View.GONE);
				}
				
			} else {
				views.setViewVisibility(R.id.devBattery, View.GONE);
				views.setViewVisibility(R.id.device_battery_text_id, View.GONE);
			}
		} catch (Exception e) {
			views.setViewVisibility(R.id.devBattery, View.GONE);
			views.setViewVisibility(R.id.device_battery_text_id, View.GONE);
		}
		try {
			if (cgmSelected == Constants.MEDTRONIC_CGM) {
				try {
					if (result.has("calibrationStatus"))
						calibrationStatus = result.getInt("calibrationStatus");
				} catch (Exception e) {
				}
				if (result.has("isCalibrating"))
					isCalibrating = result.getBoolean("isCalibrating");
			}
		} catch (Exception e) {
		}
		try {
			if (result.has("sgv") && result.getString("sgv") != null) {
				sgv = result.getString("sgv");
				if (sgv != null && !"".equals(sgv) && !"---".equals(sgv)
						&& !isWarmingUp){
					if (isCalibrating)
						calib = "*";
					else{
						if (cgmSelected == Constants.MEDTRONIC_CGM)
							calib = Constants.getWidgetCalAppend(calibrationStatus);
						else
							calib = "";
						if (calib.indexOf("NC") >= 0 || calib.indexOf("DB")>=0)
							sgv =calib;
					}
					processSGVValue(sgv, views);
				}else if ("".equals(sgv) || "---".equals(sgv)){
					if (cgmSelected == Constants.MEDTRONIC_CGM)
						calib = Constants.getWidgetCalAppend(calibrationStatus);
					else
						calib = "";
					if (calib.indexOf("NC") >= 0 || calib.indexOf("DB")>=0)
						sgv =calib;
				}
			}
		} catch (Exception e) {
		}
		try {
			if (result.has("direction") & result.getString("direction") != null)
				direction = result.getString("direction");
		} catch (Exception e) {
		}
		try {
			if (result.has("uploaderBattery")) {
				int phoneBatt = result.getInt("uploaderBattery");
				if (phoneBatt >= 0) {
					views.setViewVisibility(R.id.phoneBattery, View.VISIBLE);
					views.setViewVisibility(R.id.phone_battery_label_id,
							View.VISIBLE);
					views.setViewVisibility(R.id.phone_battery_text_id,
							View.VISIBLE);
					if (phoneBatt > 50) {
						views.setImageViewResource(R.id.phoneBattery,
								R.drawable.battery_full_icon);
					} else if (phoneBatt > 25 && phoneBatt < 50) {
						views.setImageViewResource(R.id.phoneBattery,
								R.drawable.battery_half_icon);
					} else
						views.setImageViewResource(R.id.phoneBattery,
								R.drawable.battery_low_icon);
					views.setTextViewText(R.id.phone_battery_text_id, phoneBatt
							+ "%");
				} else {
					views.setViewVisibility(R.id.phoneBattery, View.GONE);
					views.setViewVisibility(R.id.phone_battery_label_id,
							View.GONE);
					views.setViewVisibility(R.id.phone_battery_text_id,
							View.GONE);
				}
			} else {
				views.setViewVisibility(R.id.phoneBattery, View.GONE);
				views.setViewVisibility(R.id.phone_battery_label_id, View.GONE);
				views.setViewVisibility(R.id.phone_battery_text_id, View.GONE);
			}
		} catch (Exception e) {
			views.setViewVisibility(R.id.phoneBattery, View.GONE);
			views.setViewVisibility(R.id.phone_battery_label_id, View.GONE);
			views.setViewVisibility(R.id.phone_battery_text_id, View.GONE);
		}
		if (isWarmingUp) {
			calib = "";
			sgv = "W_Up";
		}
		views.setTextViewText(R.id.sgv_id, sgv + calib);
		long date = 0;
		if (result.has("date")) {
			try {
				date = result.getLong("date");
			} catch (Exception e) {
				date = 0;
			}
		}else
			date = 0;

		long current = System.currentTimeMillis();
		long diff = current - date;
		String type = prefs.getString("minrefreshPeriod", "2");
		long maxTime = Constants.TIME_15_MIN_IN_MS;
		if ("1".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_10_MIN_IN_MS;
		} else if ("3".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_20_MIN_IN_MS;
		} else if ("4".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_25_MIN_IN_MS;
		} else if ("5".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS;
		} else if ("6".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS
					+ Constants.TIME_5_MIN_IN_MS;
		} else if ("7".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS
					+ Constants.TIME_10_MIN_IN_MS;
		} else if ("8".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS
					+ Constants.TIME_15_MIN_IN_MS;
		} else if ("9".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS
					+ Constants.TIME_20_MIN_IN_MS;
		} else if ("10".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS
					+ Constants.TIME_25_MIN_IN_MS;
		} else if ("11".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_60_MIN_IN_MS;
		} else
			maxTime = Constants.TIME_15_MIN_IN_MS;
		boolean lostTimeAlarmRaised = false;
		boolean alarms_active = prefs.getBoolean("alarms_active", true);
		boolean raiseLostAlarm = prefs.getBoolean("alarm_lost", true);
		if (alarms_active){
			if (prefs.contains("lostTimeAlarmRaised"))
				lostTimeAlarmRaised = prefs.getBoolean("lostTimeAlarmRaised",
						false);
			if (diff == current || diff >= maxTime) {
				views.setTextColor(R.id.sgv_id, Color.GRAY);
				views.setInt(R.id.sgv_id, "setPaintFlags",
						Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
				if (!lostTimeAlarmRaised && raiseLostAlarm) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt("alarmType", Constants.CONNECTION_LOST);
					editor.putBoolean("lostTimeAlarmRaised", true);
					editor.commit();
					// intent to call the activity which shows on ringing
					Intent intent = new Intent(context.getApplicationContext(),
							AlertActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.getApplicationContext().startActivity(intent);

					// display that alarm is ringing

				} else if (!raiseLostAlarm) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt("alarmType", Constants.CONNECTION_LOST);
					editor.putBoolean("lostTimeAlarmRaised", true);
					editor.commit();
				}
			} else {
				views.setInt(R.id.sgv_id, "setPaintFlags",
						Paint.ANTI_ALIAS_FLAG);
				if (diff < Constants.TIME_10_MIN_IN_MS) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.remove("lostTimeAlarmRaised");
					editor.commit();
				}
			}
		}else{
			if (diff == current || diff >= maxTime) {
				views.setTextColor(R.id.sgv_id, Color.GRAY);
				views.setInt(R.id.sgv_id, "setPaintFlags",
						Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
			}
		}
		if (diff == current || (diff / 60000 < 1)){
			views.setViewVisibility(R.id.minute_id, View.GONE);
		}else{
			views.setViewVisibility(R.id.minute_id, View.VISIBLE);
			if (diff > maxTime)
				views.setTextColor(R.id.minute_id, Color.RED);
			else if (diff > (maxTime - Constants.TIME_5_MIN_IN_MS))
				views.setTextColor(R.id.minute_id, Constants.YELLOW);
			else
				views.setTextColor(R.id.minute_id, Color.WHITE);
			if (diff / 60000 <= 60)
				views.setTextViewText(R.id.minute_id, " "+((int)(diff / 60000))+" min.");
			else if (diff / Constants.TIME_60_MIN_IN_MS <= 24)
				views.setTextViewText(R.id.minute_id, " > 1 h.");
			else 
				views.setTextViewText(R.id.minute_id, " > 1 d.");
		}
		views.setTextViewText(R.id.arrow_id, getArrow(direction));
	}

	/**
	 * This method helps to process the last sgv value received. It also raise alarms if needed.
	 * @param sgv, last sgv value
	 * @param views, access to the Widget UI.
	 */
	private void processSGVValue(String sgv, RemoteViews views) {
		Log.i("processSGVValue", "processSGVValue " + sgv);
		Integer sgvInt = -1;
		boolean alarms_active = prefs.getBoolean("alarms_active", true);
		
		try {
			if (alarms_active){
				if (!Constants.checkSgvErrorValue(sgv))
					sgvInt = Integer.parseInt(sgv);
				else{
					boolean alarm_error = prefs.getBoolean("alarm_error", false);
					boolean errorsgv_raised = prefs.getBoolean("error_sgvraised", false);
					String alarmerror_ringtone = prefs.getString("alarmerror_ringtone", "");
					if (alarm_error && !errorsgv_raised && alarmerror_ringtone != null && !alarmerror_ringtone.equals("")){
						SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean("error_sgvraised", true);
						editor.putInt("alarmType", Constants.ALARM_SGV_ERROR);
						editor.putString("sgv", sgv);
						editor.commit();
						Intent intent = new Intent(context.getApplicationContext(),
								AlertActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.getApplicationContext().startActivity(intent);
					} else if (!alarm_error) {
						SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean("error_sgvraised", true);
						editor.commit();
					}
				}
			}
				
		} catch (Exception e) {
		}
		Log.i("processSGVValue", "processSGVValueINT " + sgvInt);
		if (sgvInt <= 0) {
			views.setTextColor(R.id.sgv_id, Color.WHITE);
			return;
		} else {
			Log.i("processSGVValue", "processSGVValueInside!! ");
			boolean sound_alarm = prefs.getBoolean("sound_alarm", true);
			boolean sound_warning = prefs.getBoolean("sound_warning", false);
			boolean alarmRaised = prefs.getBoolean("alarmRaised", false);
			boolean warningRaised = prefs.getBoolean("alarmRaised", false);
			String alarm_ringtone = prefs.getString("alarm_ringtone", "");
			String warning_ringtone = prefs.getString("warning_ringtone", "");
			int upperwarning = 0;
			int lowerwarning = 0;
			int upperalarm = 0;
			int loweralarm = 0;
			int color = Color.WHITE;
			try {
				upperwarning = Integer.parseInt(prefs.getString(
						"upper_warning_color", "140"));
			} catch (Exception e) {

			}
			try {
				lowerwarning = Integer.parseInt(prefs.getString(
						"lower_warning_color", "80"));
			} catch (Exception e) {

			}
			try {
				upperalarm = Integer.parseInt(prefs.getString(
						"upper_alarm_color", "170"));

			} catch (Exception e) {

			}
			try {
				loweralarm = Integer.parseInt(prefs.getString(
						"lower_alarm_color", "70"));
			} catch (Exception e) {

			}

			Log.i("processSGVValue", "UW " + upperwarning + " LW "
					+ lowerwarning + " UA " + upperalarm + " LA " + loweralarm);

			if (upperwarning > 0) {
				color = Color.GREEN;
				if (sgvInt >= upperwarning)
					color = Constants.YELLOW;
			}
			if (upperalarm > 0) {
				if (color == Color.WHITE)
					color = Color.GREEN;
				if (sgvInt >= upperalarm)
					color = Color.RED;
			}
			if (lowerwarning > 0) {
				if (color == Color.WHITE)
					color = Color.GREEN;
				if (sgvInt <= lowerwarning)
					color = Constants.YELLOW;
			}
			if (loweralarm > 0) {
				if (color == Color.WHITE)
					color = Color.GREEN;
				if (sgvInt <= loweralarm)
					color = Color.RED;
			}
			Log.i("processSGVValue", "Wraised " + warningRaised
					+ " Alarmraside " + alarmRaised);
			views.setTextColor(R.id.sgv_id, color);
			if (alarms_active){
				if (!alarmRaised && color == Color.RED && sound_alarm
						&& alarm_ringtone != null && !alarm_ringtone.equals("")) {
	
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean("alarmRaised", true);
					editor.putInt("alarmType", Constants.ALARM);
					editor.putString("sgv", sgv);
					editor.commit();
					Intent intent = new Intent(context.getApplicationContext(),
							AlertActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.getApplicationContext().startActivity(intent);
				} else if (!sound_alarm) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean("alarmRaised", true);
					editor.commit();
				}
				if (!alarmRaised && !warningRaised && color == Constants.YELLOW
						&& sound_warning && warning_ringtone != null
						&& !warning_ringtone.equals("")) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt("alarmType", Constants.WARNING);
					editor.putBoolean("warningRaised", true);
					editor.putString("sgv", sgv);
					editor.commit();
					Intent intent = new Intent(context.getApplicationContext(),
							AlertActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.getApplicationContext().startActivity(intent);
	
				} else if (!sound_warning) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean("warningRaised", true);
					editor.commit();
				}
			}

			if (color == Color.GREEN || color == Color.WHITE) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.remove("alarmRaised");
				editor.remove("warningRaised");
				editor.commit();
			}

		}
	}
	/**
	 * Changes the arrow label, to the arrow icon.
	 * @param direction, String, label of the arrow direction
	 * @return, String, Arrow to draw.
	 */
	public String getArrow(String direction) {
		if (direction.equalsIgnoreCase("NONE"))
			return "\u2194";

		if (direction.equalsIgnoreCase("DoubleUp"))
			return "\u21C8";

		if (direction.equalsIgnoreCase("SingleUp"))
			return "\u2191";

		if (direction.equalsIgnoreCase("FortyFiveUp"))
			return "\u2197";

		if (direction.equalsIgnoreCase("Flat"))
			return "\u2192";

		if (direction.equalsIgnoreCase("FortyFiveDown"))
			return "\u2198";

		if (direction.equalsIgnoreCase("SingleDown"))
			return "\u2193";

		if (direction.equalsIgnoreCase("DoubleDown"))
			return "\u21CA";

		if (direction.equalsIgnoreCase("NOT COMPUTABLE"))
			return "\u2194";

		return "\u2194";
	}
	
	public void setPrefs(SharedPreferences prefs) {
		this.prefs = prefs;
	}
}
