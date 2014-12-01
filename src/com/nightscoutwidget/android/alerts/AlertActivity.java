package com.nightscoutwidget.android.alerts;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

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
	
	/**
	 * Creates activity.
	 */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        //Set the pattern for vibration   
        long pattern[]={0,200,100,300,400};
        String label = "ALARM";
        String text = "Connection Lost!";
        String ringTone = prefs.getString("alarmlost_ringtone", "");
        String alarm_ringtone = prefs.getString("alarm_ringtone", "");
        String alarmerror_ringtone = prefs.getString("alarmerror_ringtone", "");
		String warning_ringtone = prefs.getString("warning_ringtone", "");
		String sgv = prefs.getString("sgv", "");
		vibrationActive = prefs.getBoolean("vibrationActive", true);
		int type = prefs.getInt("alarmType", Constants.CONNECTION_LOST);
		
		if (vibrationActive){
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
	    userVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
	    mMediaPlayer = new MediaPlayer();
		
        if (type == Constants.ALARM){
        	ringTone = alarm_ringtone;
        	label = "ALARM";
        	text = "Limit exceeded\ncurrent value:"+sgv+"mg/dl";
		} else if (type == Constants.WARNING){
        	ringTone = warning_ringtone;
        	label = "WARNING";
        	text = "Limit exceeded\ncurrent value:"+sgv+"mg/dl";
        }else if (type == Constants.ALARM_SGV_ERROR){
        	ringTone = alarmerror_ringtone;
        	label = "ALARM";
        	text = "Error value on SGV\nreceived value:"+sgv;
        }
        TextView tvlabel = (TextView)findViewById(R.id.alarm_label);
        tvlabel.setText(label);
        TextView tv = (TextView)findViewById(R.id.alarm_text);
        tv.setText(text);
        Button b1 =  (Button) findViewById(R.id.button1);
        b1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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
    	if (mMediaPlayer != null){
    		stopSound();
    		if (vibrationActive)
    			vibrator.cancel();
    		mMediaPlayer = null;
    	}
    	super.onDestroy();
    }
}

