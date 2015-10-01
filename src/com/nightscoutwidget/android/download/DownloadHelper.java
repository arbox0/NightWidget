package com.nightscoutwidget.android.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import ch.qos.logback.classic.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.QueryBuilder;
import com.mongodb.ServerAddress;
import com.nightscoutwidget.android.R;
import com.nightscoutwidget.android.alerts.AlertActivity;
import com.nightscoutwidget.android.medtronic.Constants;
import com.nightscoutwidget.android.settings.SettingsActivity;
import com.nightscoutwidget.android.widget.CGMWidget;

/**
 * This class has the responsability of download the last entries in the MongoDB
 * and process them to match the Widget needs
 * 
 * @author lmmarguenda
 * 
 */
public class DownloadHelper extends AsyncTask<Object, Void, Void> {
	private Logger log = (Logger)LoggerFactory.getLogger(CGMWidget.class.getName());
	private static final String TAG = "DownloadHelper";

	Context context;
	private int cgmSelected = Constants.DEXCOMG4;
	private SharedPreferences prefs = null;
	public boolean isCalculating = false;
	public JSONObject finalResult = null;
	public ToggleRunnableAction mToggleRunnableAction = null;
	public Handler mHandlerToggleInfo = new Handler();

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            , application or base context of the activity
	 * @param prefs
	 *            , Shared preferences.
	 */
	public DownloadHelper(Context context, SharedPreferences prefs) {
		this(context, Constants.DEXCOMG4, prefs);
	}

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            , application or base context of the activity
	 * @param cgmSelected
	 *            , which type of CGM is selected Medtronic or DexCom
	 * @param prefs
	 *            , Shared preferences.
	 */
	public DownloadHelper(Context context, int cgmSelected,
			SharedPreferences prefs) {
		this.context = context;
		this.cgmSelected = cgmSelected;
		this.prefs = prefs;

	}
	
	private JSONArray doGetRequest(HttpClient client, String url, String filter, String sort, String limit, String apiKey){
		JSONArray result = null;
		 URI nUri = null;
		 String query = "";
		 if (filter != null && filter.length() > 0) {
			 query += filter + "&";
		 }
		 if (sort != null && sort.length() > 0) {
			 query += sort + "&";
		 }
		 if (limit != null && limit.length() > 0) {
			 query += limit + "&";
		 }
		    try {
				nUri = new URI("https", null, "api.mongolab.com", 443, url, query + "apiKey="+apiKey,null);
			} catch (URISyntaxException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		    //URIUtils.
		    HttpGet getRequest = new HttpGet(nUri);
		    HttpPost postRequest = null;
		    getRequest.addHeader("accept", "application/json");
		    try {
				HttpResponse response = client.execute(getRequest);
				InputStream instream = response.getEntity().getContent();
		        String sResult = convertStreamToString(instream);
		        // now you have the string representation of the HTML request
		        System.out.println("RESPONSE: " + sResult);
		        instream.close();
		        result = new JSONArray(sResult);
			} catch (ClientProtocolException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		return result;
	}
	
	private boolean doPutRequest(HttpClient client, String url, String filter, String apiKey, JSONObject data){
		 String query = "";
		 if (filter != null && filter.length() > 0) {
			 query += filter + "&";
		 }
	
		try {
			URI nUri = null;
		    try {
				nUri = new URI("https", null, "api.mongolab.com", 443, url, query + "&apiKey="+apiKey,null);
			} catch (URISyntaxException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
				return false;
			}
			HttpPut putRequest = new HttpPut(nUri);
			putRequest.setHeader("Accept", "application/json");
			putRequest.setHeader("Content-type", "application/json");
	        StringEntity se = new StringEntity(data.toString());
	        putRequest.setEntity(se);
	        HttpResponse resp = client.execute(putRequest);
	        if (resp.getStatusLine().getStatusCode() > 201) {
	        	Log.e("UploaderHelper", "The can't be uploaded");
				log.error("The record can't be uploaded Code: "+resp.getStatusLine().getStatusCode());
				return false;
	        }
		}catch(IllegalArgumentException ex){
			log.error("UploaderHelper", "Illegal record");
			return false;
		}catch (Exception e){
			Log.e("UploaderHelper", "The retried can't be uploaded");
			log.error("The retried record can't be uploaded ", e);
			return false;
		}
		return true;
	}
	
	private boolean doPostRequest(HttpClient client, String url, String apiKey, JSONObject data){
		URI nUri = null;
		try {
			nUri = new URI("https", null, "api.mongolab.com", 443, url, "apiKey="+apiKey,null);
			HttpPost postRequest = new HttpPost(nUri);
			postRequest.setHeader("Accept", "application/json");
	        postRequest.setHeader("Content-type", "application/json");
	        StringEntity se = new StringEntity(data.toString());
	        postRequest.setEntity(se);
	        HttpResponse resp = client.execute(postRequest);
	        if (resp.getStatusLine().getStatusCode() > 201) {
	        	Log.e("UploaderHelper", "The can't be uploaded");
				log.error("The record can't be uploaded Code: "+resp.getStatusLine().getStatusCode());
				return false;
	        }
		}catch(IllegalArgumentException ex){
			log.error("UploaderHelper", "Illegal record");
			return false;
		}catch (Exception e){
			Log.e("UploaderHelper", "The retried can't be uploaded");
			log.error("The retried record can't be uploaded ", e);
			return false;
		}
		return true;
	}
	
	private static String convertStreamToString(InputStream is) {

	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();

	    String line = null;
	    try {
	        while ((line = reader.readLine()) != null) {
	            sb.append(line + "\n");
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return sb.toString();
	}

	/**
	 * 
	 * @return
	 */
	private JSONObject doMongoDownload() {
		log.info("doMongoDownload");
		String dbURI = prefs.getString("MongoDB URI_widget", null);
		String collectionName = prefs.getString("Collection Name_widget", "entries");
		String apiKey = prefs.getString("apiKey_widget", "aaaaa");
		String dsCollectionName = prefs.getString(
				"DeviceStatus Collection Name", "devicestatus");
		// String gdCollectionName = prefs.getString("gcdCollectionName", null);
		HttpParams params = new BasicHttpParams();
	    //HttpConnectionParams.setSoTimeout(params, 60000);
	    HttpConnectionParams.setConnectionTimeout(params, 60000);
		String devicesCollectionName = "devices";
		DefaultHttpClient httpclient = new DefaultHttpClient(params);
	    String dbName = "";
	    String[] splitted = dbURI.split(":");
    	if (splitted.length >= 4 ){
    		dbName = prefs.getString("dbName_widget", "");
    	}
    	System.out.println("DATABASE!!!! "+ dbName);
	    String entriesUrl =  "/api/1/databases/"+dbName + "/collections/"+collectionName;
	    String deviceStatusUrl =  "/api/1/databases/"+dbName + "/collections/"+devicesCollectionName;
	    String dsCollectioncUrl =  "/api/1/databases/"+dbName + "/collections/"+dsCollectionName;
	    String filter = "q={'type':{$ne:'mbg'}}";
	    String sort =  "s={'date':-1}";
	    String limit = "l=1&";
		JSONObject result = null;
		JSONObject resultMbg = null;
		JSONObject finalResult = null;
		if (dbURI != null) {
			result = new JSONObject();
			resultMbg = new JSONObject();
			finalResult = new JSONObject();
			MongoClient client = null;
			try {
				if (!prefs.getBoolean("isMongoRest_widget", false)) {
					
							// MongoClientURI uri = new MongoClientURI(dbURI.trim());
							Builder b = MongoClientOptions.builder();
							b.alwaysUseMBeans(false);
							b.connectTimeout(60000);
							b.heartbeatSocketTimeout(60000);
							b.maxWaitTime(60000);
							boolean bAchieved = false;
							String user = "";
							String password = "";
							String source = "";
							String host = "";
							String port = "";
							int iPort = -1;
							if (dbURI.length() > 0) {
								splitted = dbURI.split(":");
								if (splitted.length >= 4) {
									user = splitted[1].substring(2);
									if (splitted[2].indexOf("@") < 0)
										bAchieved = false;
									else {
										password = splitted[2].substring(0,
												splitted[2].indexOf("@"));
										host = splitted[2].substring(
												splitted[2].indexOf("@") + 1,
												splitted[2].length());
										if (splitted[3].indexOf("/") < 0)
											bAchieved = false;
										else {
											port = splitted[3].substring(0,
													splitted[3].indexOf("/"));
											source = splitted[3].substring(
													splitted[3].indexOf("/") + 1,
													splitted[3].length());
											try {
												iPort = Integer.parseInt(port);
											} catch (Exception ne) {
												iPort = -1;
											}
											if (iPort > -1)
												bAchieved = true;
										}
									}
								}
							}
							log.debug("Uri TO CHANGE user " + user + " host "
									+ source + " password " + 
									password);
							
							if (bAchieved) {
								MongoCredential mc = MongoCredential
										.createMongoCRCredential(user, source,
												password.toCharArray());
								ServerAddress sa = new ServerAddress(host, iPort);
								List<MongoCredential> lcredential = new ArrayList<MongoCredential>();
								lcredential.add(mc);
								if (sa != null && sa.getHost() != null
										&& sa.getHost().indexOf("localhost") < 0) {
									client = new MongoClient(sa, lcredential, b.build());

								}
							}
							// client = new MongoClient(uri);
							MongoClientURI uri = new MongoClientURI(dbURI.trim());
							// get db
							DB db = client.getDB(uri.getDatabase());

							// get collection
							DBCollection dexcomData = null;
							// DBCollection glucomData = null;
							DBCollection deviceData = db
									.getCollection(devicesCollectionName);
							DBObject medtronicDevice = null;
							DBObject record = null;
							DBObject recordMbg = null;
							if (deviceData != null
									&& cgmSelected == Constants.MEDTRONIC_CGM) {
								DBCursor deviceCursor = deviceData
										.find(new BasicDBObject("deviceId", prefs
												.getString("medtronic_cgm_id_widget", "")));
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
								log.info("retrieving data");
								DBCursor dexcomCursor = dexcomData
										.find(QueryBuilder.start("type").notEquals("mbg")
												.get()).sort(new BasicDBObject("date", -1))
										.limit(1);
								DBCursor mbgCursor = dexcomData
										.find(QueryBuilder.start("type").is("mbg").get())
										.sort(new BasicDBObject("date", -1)).limit(1);
								log.info("data retrieved");
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
								if (mbgCursor.hasNext()) {
									recordMbg = mbgCursor.next();
									if (recordMbg.containsField("date"))
										resultMbg.put("date", recordMbg.get("date"));
									if (recordMbg.containsField("dateString"))
										resultMbg.put("dateString",
												recordMbg.get("dateString"));
									if (recordMbg.containsField("mbg"))
										resultMbg.put("mbg", recordMbg.get("mbg"));
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
				} else {
					
					if (cgmSelected == Constants.MEDTRONIC_CGM) {
						filter = "q={'deviceId':{$eq:'"+prefs.getString("medtronic_cgm_id_widget", "")+"'}}";
						JSONArray medtronicDeviceCursor = doGetRequest(httpclient, deviceStatusUrl, filter, null, "1", apiKey);
						if (medtronicDeviceCursor != null && medtronicDeviceCursor.length() > 0) {
							JSONObject medtronicDevice = (JSONObject)medtronicDeviceCursor.get(0);
							if (medtronicDevice.has("insulinLeft")) {
								result.put("insulinLeft",
										medtronicDevice.get("insulinLeft"));
							}
							if (medtronicDevice.has("alarm")) {
								result.put("alarm", medtronicDevice.get("alarm"));
							}
							if (medtronicDevice.has("batteryStatus")) {
								result.put("batteryStatus",
										medtronicDevice.get("batteryStatus"));
							}
							if (medtronicDevice.has("batteryVoltage")) {
								result.put("batteryVoltage",
										medtronicDevice.get("batteryVoltage"));
							}
							if (medtronicDevice.has("isWarmingUp")) {
								result.put("isWarmingUp",
										medtronicDevice.get("isWarmingUp"));
							}
						}
					}
					if (collectionName != null) {
						
						log.info("retrieving data");
						filter = "q={'type':{$ne:'mbg'}}";
						sort = "s={'date':-1}";
						JSONArray recordCursor = doGetRequest(httpclient, entriesUrl, filter, sort, "1", apiKey);
						filter = "q={'type':{$eq:'mbg'}}";
						JSONArray recordMbgCursor = doGetRequest(httpclient, entriesUrl, filter, sort, "1", apiKey);
						log.info("data retrieved");
						if (recordCursor != null && recordCursor.length() > 0) {
							JSONObject record = (JSONObject)recordCursor.get(0);
							if (record.has("date"))
								result.put("date", record.get("date"));
							if (record.has("dateString"))
								result.put("dateString", record.get("dateString"));
							if (record.has("device"))
								result.put("device", record.get("device"));
							if (record.has("sgv"))
								result.put("sgv", record.get("sgv"));
							if (record.has("direction"))
								result.put("direction", record.get("direction"));

							if (cgmSelected == Constants.MEDTRONIC_CGM) {
								if (record.has("calibrationStatus"))
									result.put("calibrationStatus",
											record.get("calibrationStatus"));
								if (record.has("isCalibrating"))
									result.put("isCalibrating",
											record.get("isCalibrating"));
							}
						}
						if (recordMbgCursor != null && recordMbgCursor.length() > 0) {
							JSONObject recordMbg = (JSONObject)recordMbgCursor.get(0);
							if (recordMbg.has("date"))
								resultMbg.put("date", recordMbg.get("date"));
							if (recordMbg.has("dateString"))
								resultMbg.put("dateString",
										recordMbg.get("dateString"));
							if (recordMbg.has("mbg"))
								resultMbg.put("mbg", recordMbg.get("mbg"));
						}
					}
					sort = "s={'created_at':-1}";
					JSONArray deviceStatusCursor = doGetRequest(httpclient, dsCollectioncUrl, null, sort, "1", apiKey);
					if (deviceStatusCursor != null && deviceStatusCursor.length() > 0) {
						JSONObject deviceStatus = (JSONObject)deviceStatusCursor.get(0);
						if (deviceStatus.has("uploaderBattery"))
							result.put("uploaderBattery",
									deviceStatus.get("uploaderBattery"));
						if (deviceStatus.has("created_at"))
							result.put("created_at",
									deviceStatus.get("created_at"));
					}
				}
				// connect to db
				
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
				log.error("Error",e);
			}
		}
		if (finalResult == null)
			return null;
		try {
			finalResult.put("sgvResult", result);
			finalResult.put("mbgResult", resultMbg);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return finalResult;
	}

	@Override
	protected Void doInBackground(Object... arg0) {
		log.info("DO IN BACKGROUND");
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
			
			KeyguardManager myKM = (KeyguardManager) mToggleRunnableAction.ctx
					.getSystemService(Context.KEYGUARD_SERVICE);
			if (myKM.inKeyguardRestrictedInputMode()) {
				if (prefs.getBoolean("showIcon_widget", true)){
	    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
	    		}else{
	    			views.setViewVisibility(R.id.imageButton1, View.GONE);
	    		}

			} else {
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
				if (prefs.getBoolean("showIcon_widget", true)){
	    			views.setViewVisibility(R.id.imageButton1, View.VISIBLE);
	    		}else{
	    			views.setViewVisibility(R.id.imageButton1, View.GONE);
	    		}

			}	
			int[] appIDs = manager.getAppWidgetIds(thisWidget);
			for (int id : appIDs){
				
				Intent intent = new Intent(mToggleRunnableAction.ctx,
						SettingsActivity.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(mToggleRunnableAction.ctx, id, intent, 0);
				views.setOnClickPendingIntent(R.id.widgetSetting, pendingIntent);
			}
			finalResult = doMongoDownload();
			JSONObject result = new JSONObject();
			if (finalResult != null && isOnline()){
				log.info("Final Result not null");
				result = updateValues(finalResult, views);
			}else{
				log.info("Final Result is null");
			}
			/*
			 * else{ if (!isOnline()){ views.setTextColor(R.id.sgv_id,
			 * Color.GRAY); views.setInt(R.id.sgv_id, "setPaintFlags",
			 * Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG); } }
			 */
			if (finalResult != null && isOnline()){
				boolean showMBG =  true;
				boolean showInsulin = true;
				boolean showUploaderBattery = true;
				boolean showPumpBattery = true;
				boolean showDataDifference = true;
				try {
					result.get("showMBG");
					result.get("showInsulin");
					result.get("showUploaderBattery");
					result.get("showPumpBattery");
					result.get("showDifference");
				} catch (Exception e) {
					// TODO: handle exception
					log.error("update error", e);
				}
				mToggleRunnableAction.showMBG = showMBG;
				mToggleRunnableAction.showInsulin = showInsulin;
				mToggleRunnableAction.showUploaderBattery = showUploaderBattery;
				mToggleRunnableAction.showPumpBattery = showPumpBattery;
				mToggleRunnableAction.showDataDifference = showDataDifference;
				if (showDataDifference){
					views.setViewVisibility(R.id.difference, View.VISIBLE);
				}else{
					views.setViewVisibility(R.id.difference, View.GONE);
				}
						
				if (showMBG && (!showUploaderBattery)){
					views.setViewVisibility(R.id.linearLayout2, View.GONE);
					views.setViewVisibility(R.id.linearLayout3, View.VISIBLE);
				}else{
					views.setViewVisibility(R.id.linearLayout2, View.VISIBLE);
					views.setViewVisibility(R.id.linearLayout3, View.GONE);
				}
				if (isOnline())
					manager.updateAppWidget(thisWidget, views);
				mHandlerToggleInfo.removeCallbacks(mToggleRunnableAction);
				if ((showMBG && (showUploaderBattery))){
					long current = System.currentTimeMillis();
					mToggleRunnableAction.creationTime = current;
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(mToggleRunnableAction.ctx);
					prefs.edit().putLong("lastCreatedWidgetDate", current).commit();
					mHandlerToggleInfo.postDelayed(mToggleRunnableAction, 10000);
				}
			}else
				if (isOnline())
					manager.updateAppWidget(thisWidget, views);
			
		}
		/*
		 * Intent intent = new Intent(context.getApplicationContext(),
		 * AlertActivity.class); intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		 * context.getApplicationContext().startActivity(intent);
		 */
		return null;
	}

	/**
	 * Check if the phone has internet access.
	 * 
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
	 * 
	 * @param result
	 *            , JsonObject, downloaded entry.
	 * @param views
	 *            , Access to the Widget UI.
	 */
	public JSONObject updateValues(JSONObject resultTotal, RemoteViews views) {
		log.info("updateValues");
		JSONObject updated = new JSONObject();
		String sgv = "";
		String mbg = "";
		String direction = "";
		int calibrationStatus = -1;
		boolean isCalibrating = false;
		boolean showMBG = prefs.getBoolean("show_MBG_widget", true);
		boolean showInsulin = prefs.getBoolean("show_insulin_widget", true);
		boolean showUploaderBattery = prefs.getBoolean("show_mobile_battery_widget", true);
		boolean showPumpBattery = prefs.getBoolean("show_pump_battery_widget", true);
		boolean showDataDifference = true;
		try {
			updated.put("showMBG", showMBG);
			updated.put("showIncrement", showDataDifference);
		} catch (Exception e) {
			log.error("error pushing values on JSON");
		}
		
		String calib = "---";
		String batteryStatus = "Normal";
		String itemSelected = prefs.getString("reservoir_ins_units_widget", "2");
		int max_ins_units = 300;

		JSONObject result = null;
		if (showMBG){
			JSONObject mbgResult = null;
			try {
				mbgResult = resultTotal.getJSONObject("mbgResult");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				mbgResult = null;
			}
			if (mbgResult != null) {
				
				try {
					if (mbgResult.has("mbg") && mbgResult.getString("mbg") != null) {
						mbg = mbgResult.getString("mbg");
						if (mbg != null && !"".equals(mbg) && !"---".equals(mbg)) {
							mbg = processMBGValue(mbg, views);
							log.info("MBG VALUE "+mbg);
						}
					}
				} catch (Exception e) {
					log.error("error",e);
				}
				
					if (mbg == null)
						mbg = "---";
					views.setTextViewText(R.id.mbg_value, " " + mbg);
					long date = 0;
					if (mbgResult.has("date")) {
						try {
							date = mbgResult.getLong("date");
						} catch (Exception e) {
							date = 0;
						}
					} else
						date = 0;
					
		
					long current = System.currentTimeMillis();
					long diff = current - date;
		
					if (diff == current || (diff / 60000 < 1)) {
						views.setViewVisibility(R.id.mbg_time_id, View.GONE);
					} else {
		
						views.setViewVisibility(R.id.mbg_time_id, View.VISIBLE);
						if (diff / 60000 <= 60)
							views.setTextViewText(R.id.mbg_time_id, " "
									+ ((int) (diff / 60000)) + " m. ago");
						else if (diff / Constants.TIME_60_MIN_IN_MS <= 24)
							views.setTextViewText(R.id.mbg_time_id, " > "+((int)diff / Constants.TIME_60_MIN_IN_MS)+" h. ago");
						else
							views.setTextViewText(R.id.mbg_time_id, " > "+((int)((diff / Constants.TIME_60_MIN_IN_MS)/24))+" d. ago");
					}
				}
		}
		try {
			result = resultTotal.getJSONObject("sgvResult");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return updated;
		}
		if (result == null) {
			return updated;
		}
		boolean isWarmingUp = false;
		if (result.has("isWarmingUp")){
			try {
				isWarmingUp = result.getBoolean("isWarmingUp");
			} catch (Exception e) {
				log.error("error",e);
			}
		}
		if ("1".equalsIgnoreCase(itemSelected))
			max_ins_units = 176;
		else
			max_ins_units = 300;
		try {
			if (showInsulin){
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
							""
									+ new DecimalFormat("###").format(Math
											.floor(result.getDouble("insulinLeft")))
									+ " U");
				} else {
					showInsulin = false;
					views.setViewVisibility(R.id.insulin_data_id, View.GONE);
					views.setViewVisibility(R.id.resIcon, View.GONE);
				}
			}else{
				views.setViewVisibility(R.id.insulin_data_id, View.GONE);
				views.setViewVisibility(R.id.resIcon, View.GONE);
			}
		} catch (Exception e) {
			views.setViewVisibility(R.id.insulin_data_id, View.GONE);
			views.setViewVisibility(R.id.resIcon, View.GONE);
		}
		try {
			if (showPumpBattery){
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
				}else {
					showPumpBattery = false;
					views.setViewVisibility(R.id.devBattery, View.GONE);
					views.setViewVisibility(R.id.device_battery_text_id, View.GONE);
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
		long date = 0;
		if (result.has("date")) {
			try {
				date = result.getLong("date");
			} catch (Exception e) {
				date = 0;
			}
		} else
			date = 0;
		try {
			if (result.has("sgv") && result.getString("sgv") != null) {
				sgv = result.getString("sgv");
				if (sgv != null && !"".equals(sgv) && !"---".equals(sgv)
						&& !isWarmingUp) {
					if (isCalibrating)
						calib = "*";
					else {
						if (cgmSelected == Constants.MEDTRONIC_CGM) {
							if (sgv.indexOf("NC") < 0 || sgv.indexOf("DB") < 0)
								calib = Constants
										.getWidgetCalAppend(calibrationStatus);
						} else
							calib = "";

					}
					if (calib.indexOf("NC") >= 0 || calib.indexOf("DB") >= 0)
						sgv = calib;
					else{
						sgv = processSGVValue(sgv, date, views);
					}
				} else if ("".equals(sgv) || "---".equals(sgv)) {
					if (cgmSelected == Constants.MEDTRONIC_CGM)
						calib = Constants.getWidgetCalAppend(calibrationStatus);
					else
						calib = "";
					if (calib.indexOf("NC") >= 0 || calib.indexOf("DB") >= 0)
						sgv = calib;
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
			if (showUploaderBattery)
			{
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
					showUploaderBattery = false;
					views.setViewVisibility(R.id.phoneBattery, View.GONE);
					views.setViewVisibility(R.id.phone_battery_label_id, View.GONE);
					views.setViewVisibility(R.id.phone_battery_text_id, View.GONE);
				}
			}else{
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
			showDataDifference = false;
		}
		if (calib.indexOf("NC") >= 0 || calib.indexOf("DB") >= 0){
			showDataDifference = false;
			views.setTextViewText(R.id.sgv_id, calib);
		}else{
			views.setTextViewText(R.id.sgv_id, sgv + calib);
		}
		

		long current = System.currentTimeMillis();
		long diff = current - date;
		String type = prefs.getString("minrefreshPeriod_widget", "2");
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
			maxTime = Constants.TIME_30_MIN_IN_MS + Constants.TIME_5_MIN_IN_MS;
		} else if ("7".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS + Constants.TIME_10_MIN_IN_MS;
		} else if ("8".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS + Constants.TIME_15_MIN_IN_MS;
		} else if ("9".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS + Constants.TIME_20_MIN_IN_MS;
		} else if ("10".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_30_MIN_IN_MS + Constants.TIME_25_MIN_IN_MS;
		} else if ("11".equalsIgnoreCase(type)) {
			maxTime = Constants.TIME_60_MIN_IN_MS;
		} else
			maxTime = Constants.TIME_15_MIN_IN_MS;
		boolean lostTimeAlarmRaised = false;
		boolean alarms_active = prefs.getBoolean("alarms_active_widget", true);
		boolean raiseLostAlarm = prefs.getBoolean("alarm_lost_widget", true);
		if (alarms_active) {
			if (prefs.contains("lostTimeAlarmRaised_widget"))
				lostTimeAlarmRaised = prefs.getBoolean("lostTimeAlarmRaised_widget",
						false);
			if (diff != current && diff >= maxTime) {
				views.setTextColor(R.id.sgv_id, Color.GRAY);
				views.setInt(R.id.sgv_id, "setPaintFlags",
						Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
				if (!lostTimeAlarmRaised && raiseLostAlarm) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt("alarmType_widget", Constants.CONNECTION_LOST);
					editor.putBoolean("lostTimeAlarmRaised_widget", true);
					editor.commit();
					// intent to call the activity which shows on ringing
					Intent intent = new Intent(context.getApplicationContext(),
							AlertActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.getApplicationContext().startActivity(intent);

					// display that alarm is ringing

				} else if (!raiseLostAlarm) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt("alarmType_widget", Constants.CONNECTION_LOST);
					editor.putBoolean("lostTimeAlarmRaised_widget", true);
					editor.commit();
				}
			} else {
				views.setInt(R.id.sgv_id, "setPaintFlags",
						Paint.ANTI_ALIAS_FLAG); // TODO: I have to test this
												// improvement--> | (~
												// Paint.STRIKE_THRU_TEXT_FLAG));
				if (diff < Constants.TIME_10_MIN_IN_MS) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.remove("lostTimeAlarmRaised_widget");
					editor.commit();
				}
			}
		} else {
			if (diff == current || diff >= maxTime) {
				views.setTextColor(R.id.sgv_id, Color.GRAY);
				views.setInt(R.id.sgv_id, "setPaintFlags",
						Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
			}
		}
		if (diff == current || (diff / 60000 < 1)) {
			views.setViewVisibility(R.id.minute_id, View.GONE);
		} else {

			views.setViewVisibility(R.id.minute_id, View.VISIBLE);

			if (diff > maxTime)
				views.setTextColor(R.id.minute_id, Color.RED);
			else if (diff > (maxTime - Constants.TIME_5_MIN_IN_MS))
				views.setTextColor(R.id.minute_id, Constants.YELLOW);
			else
				views.setTextColor(R.id.minute_id, Color.WHITE);
			if (diff / 60000 <= 60)
				views.setTextViewText(R.id.minute_id, " "
						+ ((int) (diff / 60000)) + " m.");
			else if (diff / Constants.TIME_60_MIN_IN_MS <= 24)
				views.setTextViewText(R.id.minute_id, ((int)diff / Constants.TIME_60_MIN_IN_MS)+" h.");
			else
				views.setTextViewText(R.id.minute_id, ((int)((diff / Constants.TIME_60_MIN_IN_MS)/24))+" d.");
		}
		views.setTextViewText(R.id.arrow_id, getArrow(direction));
		try{
			updated.put("showInsulin", showInsulin);
			updated.put("showMBG", showMBG);
			updated.put("showUploaderBattery", showUploaderBattery);
			updated.put("showPumpBattery", showPumpBattery);
			if (showDataDifference){
				String diffData = prefs.getString("currentDataDiff_widget","");
				log.info("SHOWDATA2 "+diffData);
				if (diffData != null && !diffData.equalsIgnoreCase("")){
					log.info("SHOWDATA3 "+diffData);
					views.setTextViewText(R.id.difference, diffData);
				}else
					showDataDifference = false;
			}
			updated.put("showDifference", showDataDifference);
		}catch(Exception e){
			log.error("Uploader Error",e);
		}
		return updated;
	}

	/**
	 * This method helps to process the last sgv value received. It also raise
	 * alarms if needed.
	 * 
	 * @param sgv
	 *            , last sgv value
	 * @param views
	 *            , access to the Widget UI.
	 */
	private String processSGVValue(String sgv, long date, RemoteViews views) {
		log.info("processSGVValue " + sgv);
		Float sgvInt = -1f;
		boolean alarms_active = prefs.getBoolean("alarms_active_widget", true);
		float divisor = 1;
		DecimalFormat df = null;
		if (prefs.getBoolean("mmolDecimals_widget", false))
			df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
		else
			df = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
		if (prefs.getString("metric_preference_widget", "1").equals("2"))
			divisor = 18;
		try {
			if (alarms_active) {
				if (!Constants.checkSgvErrorValue(sgv)) {
					sgvInt = (float) Integer.parseInt(sgv);
					if (prefs.getString("metric_preference_widget", "1").equals("2"))
						sgvInt = sgvInt / divisor;
				} else {
					StringBuffer notDigits = new StringBuffer();
					try {
						StringBuffer aux = new StringBuffer();
						
						for (int i = 0; i < sgv.length(); i++){
							if (Character.isDigit(sgv.charAt(i))){
								aux.append(sgv.charAt(i));
							}else
								notDigits.append(sgv.charAt(i));
						}
						sgvInt = (float) Integer.parseInt(aux.toString());
						if (prefs.getString("metric_preference_widget", "1").equals("2"))
							sgvInt = sgvInt / divisor;
					} catch (Exception e) {
						// TODO: handle exception
					}
					boolean alarm_error = prefs
							.getBoolean("alarm_error", false);
					boolean errorsgv_raised = prefs.getBoolean(
							"error_sgvraised_widget", false);
					boolean alarm_sgv_enabled = prefs.getBoolean("alarmSgvEnableActive_widget", false);
					long alarmDiff =  prefs.getLong("alarm_sgv_reenable_widget", 0);
					long lastRaised = prefs.getLong("alarm_sgv_time_widget", 0);
					long current = System.currentTimeMillis();
					String alarmerror_ringtone = prefs.getString(
							"alarmerror_ringtone_widget", "");
					if (alarm_error && ((!errorsgv_raised) || ((alarm_sgv_enabled) && ((current - lastRaised) >= alarmDiff)))
							&& alarmerror_ringtone != null
							&& !alarmerror_ringtone.equals("")) {
						SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean("error_sgvraised_widget", true);
						editor.putInt("alarmType_widget", Constants.ALARM_SGV_ERROR);
					    String metric_type = prefs.getString("metric_preference_widget", "1");
				    	if (prefs.getBoolean("mmolDecimals_widget", false))
				    		df = new DecimalFormat("#.##");
				    	else
				    		df = new DecimalFormat("#.#");
				    	if (metric_type.equals("2")){
				    		if (sgvInt > -1){
				    			editor.putString("sgv_widget", df.format(sgvInt)+notDigits.toString());
				    		}
				    	}else
				    		editor.putString("sgv_widget", sgv);
						editor.putLong("alarm_sgv_time_widget", System.currentTimeMillis());
						editor.commit();
						Intent intent = new Intent(
								context.getApplicationContext(),
								AlertActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.getApplicationContext().startActivity(intent);
					} else if (!alarm_error) {
						SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean("error_sgvraised_widget", true);
						editor.commit();
					}
				}
			}else{
				if (!Constants.checkSgvErrorValue(sgv)) {
					sgvInt = (float) Integer.parseInt(sgv);
					if (prefs.getString("metric_preference_widget", "1").equals("2"))
						sgvInt = sgvInt / divisor;
				}
			}

		} catch (Exception e) {
			log.error("error", e);
		}
		log.info("processSGVValueINT " + sgvInt);
		if (sgvInt <= 0) {
			views.setInt(R.id.sgv_id, "setPaintFlags", 0);
			views.setTextColor(R.id.sgv_id, Color.WHITE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.remove("previousSVGVALUE_widget");
			editor.remove("currentDataDiff_widget");
			editor.commit();
			return sgv;
		} else {
			log.info("processSGVValueInside " + sgvInt);
			boolean sound_alarm = prefs.getBoolean("sound_alarm_widget", true);
			boolean sound_warning = prefs.getBoolean("sound_warning_widget", false);
			boolean alarmRaised = prefs.getBoolean("alarmRaised_widget", false);
			boolean warningRaised = prefs.getBoolean("warningRaised_widget", false);
			String alarm_ringtone = prefs.getString("alarm_ringtone_widget", "");
			String warning_ringtone = prefs.getString("warning_ringtone_widget", "");
			float upperwarning = 0;
			float lowerwarning = 0;
			float upperalarm = 0;
			float loweralarm = 0;
			int color = Color.WHITE;

			try {
				if (prefs.getString("metric_preference_widget", "1").equals("1"))
					upperwarning = Integer.parseInt(prefs
							.getString("upper_warning_color_widget", ""
									+ ((int) (140 / divisor))));
				else
					upperwarning = Float.parseFloat(prefs
							.getString("upper_warning_color_widget", ""
									+ ((float) (140 / divisor))).replace(",", "."));
			} catch (Exception e) {
				log.error("error", e);
			}
			try {
				if (prefs.getString("metric_preference_widget", "1").equals("1"))
					lowerwarning = Integer
							.parseInt(prefs.getString("lower_warning_color_widget", ""
									+ ((int) (80 / divisor))));
				else
					lowerwarning = Float
							.parseFloat(prefs.getString("lower_warning_color_widget",
									"" + ((float) (80 / divisor))).replace(",", "."));

			} catch (Exception e) {
				log.error("error", e);
			}
			try {
				if (prefs.getString("metric_preference_widget", "1").equals("1"))
					upperalarm = Integer.parseInt(prefs.getString(
							"upper_alarm_color_widget", "" + ((int) (170 / divisor))));
				else
					upperalarm = Float.parseFloat(prefs.getString(
							"upper_alarm_color_widget", "" + ((float) (170 / divisor))).replace(",", "."));

			} catch (Exception e) {
				log.error("error", e);
			}
			try {
				if (prefs.getString("metric_preference_widget", "1").equals("1"))
					loweralarm = Integer.parseInt(prefs.getString(
							"lower_alarm_color_widget", "" + ((int) (70 / divisor))));
				else
					loweralarm = Float.parseFloat(prefs.getString(
							"lower_alarm_color_widget", "" + ((float) (70 / divisor))).replace(",", "."));

			} catch (Exception e) {
				log.error("error", e);
			}

			log.debug("UW " + upperwarning + " LW "
					+ lowerwarning + " UA " + upperalarm + " LA " + loweralarm);
			if (alarms_active){
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
			}
			log.debug( "Wraised " + warningRaised
					+ " Alarmraside " + alarmRaised);
			views.setInt(R.id.sgv_id, "setPaintFlags", 0);
			views.setTextColor(R.id.sgv_id, color);
			if (alarms_active) {
				boolean alarm_enabled = prefs.getBoolean("alarmEnableActive_widget", false);
				boolean warning_enabled = prefs.getBoolean("warningEnableActive_widget", false);
				long alarmDiff =  prefs.getLong("alarm_reenable_widget", 0);
				long lastRaised = prefs.getLong("alarm_time_widget", 0);
				long warningDiff =  prefs.getLong("warning_reenable_widget", 0);
				long wlastRaised = prefs.getLong("warning_time_widget", 0);
				long current = System.currentTimeMillis();
				if ((!alarmRaised || ((alarm_enabled) && (current - lastRaised) >= alarmDiff)) && color == Color.RED && (sound_alarm )
						&& alarm_ringtone != null && !alarm_ringtone.equals("")) {

					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean("alarmRaised_widget", true);
					editor.putInt("alarmType_widget", Constants.ALARM);
					String metric_type = prefs.getString("metric_preference_widget", "1");
			    	if (prefs.getBoolean("mmolDecimals_widget", false))
			    		df = new DecimalFormat("#.##");
			    	else
			    		df = new DecimalFormat("#.#");
			    	if (metric_type.equals("2")){
			    		if (sgvInt > -1){
			    			editor.putString("sgv_widget", df.format(sgvInt));
			    		}
			    	}else
			    		editor.putString("sgv_widget", "" + sgvInt);
					editor.putLong("alarm_time_widget", current);
					editor.commit();
					Intent intent = new Intent(context.getApplicationContext(),
							AlertActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.getApplicationContext().startActivity(intent);
				} else if (!sound_alarm) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean("alarmRaised_widget", true);
					editor.commit();
				}
				log.debug(" current "+ current+" wlastRaised "+ wlastRaised+" warning_enabled "+warning_enabled+" substract "+(current - wlastRaised)+" warningDIff" +warningDiff);
				if ((!alarmRaised || ((alarm_enabled) && (current - lastRaised) >= alarmDiff)) && (!warningRaised || ((warning_enabled) && (current - wlastRaised) >= warningDiff))&& color == Constants.YELLOW
						&& (sound_warning ) && warning_ringtone != null
						&& !warning_ringtone.equals("")) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt("alarmType_widget", Constants.WARNING);
					editor.putBoolean("warningRaised_widget", true);
					String metric_type = prefs.getString("metric_preference_widget", "1");
			    	if (prefs.getBoolean("mmolDecimals_widget", false))
			    		df = new DecimalFormat("#.##");
			    	else
			    		df = new DecimalFormat("#.#");
			    	if (metric_type.equals("2")){
			    		if (sgvInt > -1){
			    			editor.putString("sgv_widget", df.format(sgvInt));
			    		}
			    	}else
			    		editor.putString("sgv_widget", "" + sgvInt);
					editor.putLong("warning_time_widget", current);
					editor.commit();
					Intent intent = new Intent(context.getApplicationContext(),
							AlertActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.getApplicationContext().startActivity(intent);

				} else if (!sound_warning) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean("warningRaised_widget", true);
					editor.commit();
				}
			}

			if (color == Color.GREEN || color == Color.WHITE) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.remove("alarmRaised_widget");
				editor.remove("warningRaised_widget");
				editor.commit();
			}

		}
		Float diff = null;
		log.debug( "calcSGV diff "+diff);
		if (prefs.contains("previousSVGVALUE_widget") && prefs.contains("previousSGVDATE_widget"))
		{
			log.debug( "calcSGV diff prevvalue");
			Float prevSvg = prefs.getFloat("previousSVGVALUE_widget", -1f);
			if (prefs.getLong("previousSGVDATE_widget", 0) != date){
				String newValue = "";
				if (prefs.getString("metric_preference_widget", "1").equals("2")){
					newValue = df.format(sgvInt);
				}else
					newValue = ""+ (sgvInt.intValue());
				if (prefs.getBoolean("show_Notifications_widget", true))
				{
					Notification.Builder mBuilder =
					        new Notification.Builder(context)
					        .setSmallIcon(R.drawable.ic_launcher_little)
					        .setContentTitle("New SGV received:")
					        .setContentText(newValue)
					        .setTicker("SGV received: "+newValue)
					        .setLargeIcon((((BitmapDrawable)context.getResources().getDrawable(R.drawable.ic_launcher)).getBitmap()));
					
					NotificationManager mNotificationManager =
					    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					// mId allows you to update the notification later on.
					SharedPreferences settings = context.getSharedPreferences("widget_prefs", 0);
					String mTag = settings.getString("widgetTag", "nightWidget_030215");
					int mId =  settings.getInt("widgetId", 1717030215);
					mNotificationManager.notify(mTag, mId, mBuilder.build());
				}				
				
				log.debug( "calcSGV diff prevvalue "+prevSvg);
				SharedPreferences.Editor editor = prefs.edit();
				if (prevSvg > 0){
					 diff = (sgvInt - prevSvg);
				    if (prefs.getString("metric_preference_widget", "1").equals("2")){
						editor.putString("currentDataDiff_widget", df.format(diff)+" mmol/l");
					}else{
						if (diff > 0)
							editor.putString("currentDataDiff_widget","+"+ diff.intValue()+" mg/dl");
						else if (diff< 0)
							editor.putString("currentDataDiff_widget",diff.intValue()+" mg/dl");
					}
				}else{
					editor.remove("currentDataDiff_widget");
				}
				editor.putFloat("previousSVGVALUE_widget", sgvInt);
				editor.putLong("previousSGVDATE_widget", date);
				editor.commit();
			}
		}else{
			SharedPreferences.Editor editor = prefs.edit();
			editor.putFloat("previousSVGVALUE_widget", sgvInt);
			editor.putLong("previousSGVDATE_widget", date);
			editor.commit();
		}
		if (prefs.getString("metric_preference_widget", "1").equals("2")){
			return df.format(sgvInt);
		}else
			return "" + (sgvInt.intValue());
	}

	/**
	 * This method helps to process the last sgv value received. It also raise
	 * alarms if needed.
	 * 
	 * @param sgv
	 *            , last sgv value
	 * @param views
	 *            , access to the Widget UI.
	 */
	private String processMBGValue(String mbg, RemoteViews views) {
		log.debug( "processMBGValue " + mbg +" METRICS "+prefs.getString("metric_preference_widget", "1"));
		Float mbgInt = -1f;
		float divisor = 1;
		DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
		if (prefs.getString("metric_preference_widget", "1").equals("2")){
			divisor = 18;
		}
		try {

			mbgInt =  Float.parseFloat(mbg);
			if (prefs.getString("metric_preference_widget", "1").equals("2"))
				mbgInt = (float) mbgInt / divisor;

		} catch (Exception e) {
			log.error( "error",e);
		}
		log.debug( "processMBGValueINT " + mbgInt);
		if (mbgInt <= 0) {
			views.setTextColor(R.id.mbg_label, Color.WHITE);
			views.setTextColor(R.id.mbg_time_id, Color.WHITE);
			views.setTextColor(R.id.mbg_value, Color.WHITE);
			return mbg;
		} else {
			views.setTextColor(R.id.mbg_label, Color.WHITE);
			views.setTextColor(R.id.mbg_time_id, Color.WHITE);
			views.setTextColor(R.id.mbg_value, Color.WHITE);
		}
		if (prefs.getString("metric_preference_widget", "1").equals("2"))
			return df.format(mbgInt);
		else
			return "" + (mbgInt.intValue());
	}

	/**
	 * Changes the arrow label, to the arrow icon.
	 * 
	 * @param direction
	 *            , String, label of the arrow direction
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
