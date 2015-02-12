package com.nightscoutwidget.android.alerts;

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.nightscoutwidget.android.R;
import com.nightscoutwidget.android.medtronic.Constants;
/**
 * Class which raise an alarm. It shows a front panel and makes the phone sound and vibrate.
 * @author lmmarguenda
 *
 */
public class AlertActivity extends Activity {
	MediaPlayer mMediaPlayer = null;
	SharedPreferences prefs = null;
	AudioManager mAudioManager;
	Vibrator vibrator = null;
	int userVolume;
	boolean vibrationActive = true;
	static AlertActivity alertActivity = null;
	/**
	 * Creates activity.
	 */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        alertActivity = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        //Set the pattern for vibration   
        long pattern[]={1000,300};
        String label = "ALARM";
        String text = "Connection Lost!";
        String ringTone = prefs.getString("alarmlost_ringtone_widget", "");
        String alarm_ringtone = prefs.getString("alarm_ringtone_widget", "");
        String alarmerror_ringtone = prefs.getString("alarmerror_ringtone_widget", "");
		String warning_ringtone = prefs.getString("warning_ringtone_widget", "");
		String sgv = prefs.getString("sgv_widget", "");
		vibrationActive = prefs.getBoolean("vibrationActive_widget", true);
		int type = prefs.getInt("alarmType_widget", Constants.CONNECTION_LOST);
		Log.i("MEDTRONIC","EOOOOO FUERA");
		if (vibrationActive){
			Log.i("MEDTRONIC","EOOOOO DENTRO");
	        //Start the vibration
	        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
	        //start vibration with repeated count, use -1 if you don't want to repeat the vibration
	        vibrator.vibrate(pattern, 0);
		}
        
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        setContentView(R.layout.widget_alarm_pane);
        /**
         * Stop playing the sound.
         */
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	    userVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	    mMediaPlayer = new MediaPlayer();
	    final EditText etvData =  (EditText)findViewById(R.id.time_text);
        if (type == Constants.ALARM){
        	ringTone = alarm_ringtone;
        	label = "ALARM";
        	text = "Limit exceeded\ncurrent value:"+sgv+"mg/dl";
        	etvData.setText(""+(prefs.getLong("alarm_reenable_widget", 120*60000)/60000));
		} else if (type == Constants.WARNING){
        	ringTone = warning_ringtone;
        	label = "WARNING";
        	text = "Limit exceeded\ncurrent value:"+sgv+"mg/dl";
        	etvData.setText(""+(prefs.getLong("warning_reenable_widget", 120*60000)/60000));
        }else if (type == Constants.ALARM_SGV_ERROR){
        	ringTone = alarmerror_ringtone;
        	label = "ALARM";
        	text = "Error value on SGV\nreceived value:"+sgv;
        	etvData.setText(""+(prefs.getLong("alarm_sgv_reenable_widget", 120*60000)/60000));
        }
        TextView tvlabel = (TextView)findViewById(R.id.alarm_label);
        tvlabel.setText(label);
        TextView tv = (TextView)findViewById(R.id.alarm_text);
        tv.setText(text);
        Button b1 =  (Button) findViewById(R.id.button1);
        final TextView tvAfter = (TextView)findViewById(R.id.alarm_after);
        final TextView tvMin = (TextView)findViewById(R.id.alarm_min);
       
        final CheckBox cbEnable = (CheckBox)findViewById(R.id.enableAlarm);
        b1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	int type = prefs.getInt("alarmType_widget", Constants.CONNECTION_LOST);
            	SharedPreferences.Editor editor  = prefs.edit();
            	if (type != Constants.CONNECTION_LOST)
            	{
	            	if (cbEnable.isChecked()){
	            		int waitValue = -1;
	            		try {
							waitValue = Integer.parseInt(etvData.getText().toString());
						} catch (Exception e) {
							// TODO: handle exception
							Log.e("MED", "error parsing time", e);
							waitValue = -1;
						}
	            		
	            		if (type == Constants.ALARM){
	            			editor.putBoolean("alarmEnableActive_widget", true);
	            			if (waitValue < 0)
	            				editor.remove("alarm_reenable_widget");
	            			else
	            				editor.putLong("alarm_reenable_widget", waitValue*60000);
	        			} else if (type == Constants.WARNING){
	        				editor.putBoolean("warningEnableActive_widget", true);
	        				if (waitValue < 0)
	            				editor.remove("warning_reenable_widget");
	            			else
	            				editor.putLong("warning_reenable_widget", waitValue*60000);
	        	        }else if (type == Constants.ALARM_SGV_ERROR){
	        	        	editor.putBoolean("alarmSgvEnableActive_widget", true);
	        	        	if (waitValue < 0)
	            				editor.remove("alarm_sgv_reenable_widget");
	            			else
	            				editor.putLong("alarm_sgv_reenable_widget", waitValue*60000);
	        	        }
	            	}else{
	            		
	            		if (type == Constants.ALARM){
	            			editor.putBoolean("alarmEnableActive_widget", false);
	        	        	editor.remove("alarm_reenable_widget");
	        			} else if (type == Constants.WARNING){
	        				editor.putBoolean("warningEnableActive_widget", false);
	        				editor.remove("warning_reenable_widget");
	        	        }else if (type == Constants.ALARM_SGV_ERROR){
	        	        	editor.putBoolean("alarmSgvEnableActive_widget", false);
	        	        	editor.remove("alarm_sgv_reenable_widget");
	        	        }		
	            	}
            	}
            	editor.commit();
            	if (mMediaPlayer != null){
            		stopSound();
            		if (vibrationActive){
	            	    vibrator.cancel();
            		}
            		mMediaPlayer = null;
            	}
            	finish();
            }
        });
        	String notText = "";
			Notification.Builder mBuilder = null;
			if (type != Constants.CONNECTION_LOST){
				notText = label+" sgv: "+sgv;
			        mBuilder = new Notification.Builder(getBaseContext())
			        .setSmallIcon(R.drawable.ic_launcher_little_w32)
			        .setContentTitle(label+": "+sgv)
			        .setContentText(sgv)
			        .setTicker("SGV: "+sgv)
			        .setLargeIcon((((BitmapDrawable)getBaseContext().getResources().getDrawable(R.drawable.ic_launcher)).getBitmap()));
			}else{
				notText = label+": conn. lost";
			    mBuilder = new Notification.Builder(getBaseContext())
		        .setSmallIcon(R.drawable.ic_launcher_little_w32)
		        .setContentTitle(label+": conn. lost")
		        .setContentText(sgv)
		        .setTicker("SGV: conn. lost")
		        .setLargeIcon((((BitmapDrawable)getBaseContext().getResources().getDrawable(R.drawable.ic_launcher)).getBitmap()));
			}
			 RemoteViews contentView=new RemoteViews(getBaseContext().getPackageName(), R.layout.not_layout);
		     mBuilder.setContent(contentView);
			NotificationManager mNotificationManager =
			    (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			  //this is the intent that is supposed to be called when the 
		    //button is clicked
		    Intent notButtonIntent = new Intent(this, NotButtonListener.class);
		    PendingIntent pendingSwitchIntent = PendingIntent.getBroadcast(this, 0, 
		    		notButtonIntent, 0);

		    contentView.setTextViewText(R.id.notText, notText);
			contentView.setOnClickPendingIntent(R.id.notCloseButton, pendingSwitchIntent);
			SharedPreferences settings = getBaseContext().getSharedPreferences("widget_prefs", 0);
			String mTag = settings.getString("widgetTag", "nightWidget_030215");
			int mId =  settings.getInt("widgetId", 1717030215);
			mNotificationManager.notify(mTag, mId, mBuilder.build());
	
        if (type == Constants.CONNECTION_LOST){
        	cbEnable.setChecked(false);
        	cbEnable.setVisibility(View.GONE);
        	tvAfter.setVisibility(View.GONE);
            tvMin.setVisibility(View.GONE);
            etvData.setVisibility(View.GONE);
        }else{
        	cbEnable.setVisibility(View.VISIBLE);
        	tvAfter.setVisibility(View.VISIBLE);
            tvMin.setVisibility(View.VISIBLE);
            etvData.setVisibility(View.VISIBLE);
        }
        if (!cbEnable.isChecked()){
        	tvAfter.setVisibility(View.GONE);
            tvMin.setVisibility(View.GONE);
            etvData.setVisibility(View.GONE);
        }else{
        	tvAfter.setVisibility(View.VISIBLE);
            tvMin.setVisibility(View.VISIBLE);
            etvData.setVisibility(View.VISIBLE);
        }
        	
        cbEnable.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if ( !isChecked )
                {
                	tvAfter.setVisibility(View.GONE);
                    tvMin.setVisibility(View.GONE);
                    etvData.setVisibility(View.GONE);
                }else{
                	tvAfter.setVisibility(View.VISIBLE);
                    tvMin.setVisibility(View.VISIBLE);
                    etvData.setVisibility(View.VISIBLE);
                }

            }
        });
        try {
	    	 Uri alert =  Uri.parse(ringTone);
	    	 mMediaPlayer = new MediaPlayer();
	    	 mMediaPlayer.setDataSource(this, alert);
	    	 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	    	 mMediaPlayer.setLooping(true);
	    	 mMediaPlayer.prepare();
	    	 mMediaPlayer.start();
        	 
        } catch(Exception e) {
        }   
    }
    
    /**
     * Stop playing the sound.
     */
    public void stopSound(){
    	// reset the volume to what it was before we changed it.
	    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, userVolume, AudioManager.FLAG_PLAY_SOUND);
	    mMediaPlayer.stop();
	    mMediaPlayer.reset();

    }
    
    /**
     * release player resource.
     */
    public void releasePlayer(){
    	mMediaPlayer.release();
    }
    
    /**
     * destroy activity
     */
    @Override 
    public void onDestroy(){
         final EditText etvData =  (EditText)findViewById(R.id.time_text);
         final CheckBox cbEnable = (CheckBox)findViewById(R.id.enableAlarm);
         int type = prefs.getInt("alarmType_widget", Constants.CONNECTION_LOST);
     	SharedPreferences.Editor editor  = prefs.edit();
     	if (type != Constants.CONNECTION_LOST){
	     	if (cbEnable.isChecked()){
	     		int waitValue = -1;
	     		try {
						waitValue = Integer.parseInt(etvData.getText().toString());
					} catch (Exception e) {
						// TODO: handle exception
						Log.e("MED", "error parsing time", e);
						waitValue = -1;
					}
	     		
	     		if (type == Constants.ALARM){
	     			editor.putBoolean("alarmEnableActive_widget", true);
	     			if (waitValue < 0)
	     				editor.remove("alarm_reenable_widget");
	     			else
	     				editor.putLong("alarm_reenable_widget", waitValue*60000);
	 			} else if (type == Constants.WARNING){
	 				editor.putBoolean("warningEnableActive_widget", true);
	 				if (waitValue < 0)
	     				editor.remove("warning_reenable_widget");
	     			else
	     				editor.putLong("warning_reenable_widget", waitValue*60000);
	 	        }else if (type == Constants.ALARM_SGV_ERROR){
	 	        	editor.putBoolean("alarmSgvEnableActive_widget", true);
	 	        	if (waitValue < 0)
	     				editor.remove("alarm_sgv_reenable_widget");
	     			else
	     				editor.putLong("alarm_sgv_reenable_widget", waitValue*60000);
	 	        }
	     	}else{
	     		
	     		if (type == Constants.ALARM){
	     			editor.putBoolean("alarmEnableActive_widget", false);
	 	        	editor.remove("alarm_reenable_widget");
	 			} else if (type == Constants.WARNING){
	 				editor.putBoolean("warningEnableActive_widget", false);
	 				editor.remove("warning_reenable_widget");
	 	        }else if (type == Constants.ALARM_SGV_ERROR){
	 	        	editor.putBoolean("alarmSgvEnableActive_widget", false);
	 	        	editor.remove("alarm_sgv_reenable_widget");
	 	        }
	     		
	     		
	     	}
     	}
     	editor.commit();
    	if (mMediaPlayer != null){
    		stopSound();
    		if (vibrationActive)
    			vibrator.cancel();
    		mMediaPlayer = null;
    	}
    	super.onDestroy();
    }
    



public static class NotButtonListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Here", "I am here");
        AlertActivity.alertActivity.finish();
        SharedPreferences settings = context.getSharedPreferences("widget_prefs", 0);
    	String mTag = settings.getString("widgetTag", "nightWidget_030215");
		int mId =  settings.getInt("widgetId", 1717030215);
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mTag, mId);
        
            
    }
}
}

