package com.nightscoutwidget.android.medtronic;

import android.graphics.Color;

public class Constants {
	
	

	// CGM Types
	public static final int DEXCOMG4 = 0;
	public static final int MEDTRONIC_CGM = 1;
	// General constants
	public static final int CONNECTION_LOST = 0;
	public static final int ALARM = 1;
	public static final int WARNING = 2;
	public static final int ALARM_SGV_ERROR = 3;
	public static final int YELLOW = Color.rgb(251, 173, 0);// Color.rgb(252,
															// 255, 20);

	public static final int ACTION_SHOW_PHONEDATA = 0;
	public static final int ACTION_SHOW_LAST_MBG = 1;
	
	public static final int DEVICE_ID_LENGTH = 3;
	public static final int NUMBER_OF_RETRIES = 5;
	public static final int NUMBER_OF_EGVRECORDS = 20;
	public static final int TIMEOUT = 3000;
	public static final int WAIT_ANSWER = 10000;
	public static final int FIVE_SECONDS__MS = 5000;
	public static final int TIME_1_MIN_IN_MS = 60000;
	public static final int TIME_2_MIN_IN_MS = 120000;
	public static final int TIME_3_MIN_IN_MS = 180000;
	public static final int TIME_4_MIN_IN_MS = 240000;
	public static final int TIME_5_MIN_IN_MS = 300000;
	public static final int TIME_10_MIN_IN_MS = 600000;
	public static final int TIME_15_MIN_IN_MS = 900000;
	public static final int TIME_20_MIN_IN_MS = 1200000;
	public static final int TIME_23_MIN_IN_MS = 1380000;
	public static final int TIME_25_MIN_IN_MS = 1500000;
	public static final int TIME_30_MIN_IN_MS = 1800000;
	public static final int TIME_60_MIN_IN_MS = 3600000;
	public static final int TIME_12_HOURS_IN_MS = 43200000;
	public static final String PREFS_NAME = "MyPrefsFile";

	public static final int CALIBRATION_SENSOR = 0;
	public static final int CALIBRATION_GLUCOMETER = 1;
	public static final int CALIBRATION_MANUAL = 2;

	// Calibration status
	public static final int WITHOUT_ANY_CALIBRATION = 0;
	public static final int CALIBRATED = 1;
	public static final int CALIBRATION_MORE_THAN_12H_OLD = 2;
	public static final int LAST_CALIBRATION_FAILED_USING_PREVIOUS = 3;
	public static final int CALIBRATED_IN_15MIN = 4;
	public static final int CALIBRATING = 5;
	public static final int CALIBRATING2 = 6;

	public static String getWidgetCalAppend(int val) {
		switch (val) {
		case CALIBRATED:
			return "";// perfect!
		case LAST_CALIBRATION_FAILED_USING_PREVIOUS:
			return "?!";// error??
		case CALIBRATED_IN_15MIN:
			return "!";// not ideal
		case CALIBRATION_MORE_THAN_12H_OLD:
			return "?";// not good
		case CALIBRATING:
			return "*";// calibrating
		case CALIBRATING2:
			return "+";// partial calibration
		case WITHOUT_ANY_CALIBRATION:
			return "NC";// not calibrated
		default:
			return "DB?";// database error??
		}
	}
	
	public static boolean checkSgvErrorValue(String val) {
		return (val.toUpperCase().indexOf(Constants.WITHOUT_ANY_CALIBRATION) >= 0) ||
			   (val.indexOf("?") >= 0);
		
	}
}
