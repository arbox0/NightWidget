package com.nightscoutwidget.android.settings;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.nightscoutwidget.android.widget.CGMWidget;

public class SettingsActivity extends PreferenceActivity {
	private static String CONFIGURE_ACTION="android.appwidget.action.APPWIDGET_CONFIGURE";
	 int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    @Override
    public void onCreate(Bundle icicle) {
       setResult(RESULT_CANCELED);
        super.onCreate(icicle);
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
       
        // Find the widget id from the intent. 
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        /* set fragment */
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment(mAppWidgetId, this)).commit();
    }

    @Override
	public void onBackPressed() {
		if (CONFIGURE_ACTION.equals(getIntent().getAction())) {
			Intent intent=getIntent();
			Bundle extras=intent.getExtras();

			if (extras!=null) {
				int id=extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
															AppWidgetManager.INVALID_APPWIDGET_ID);
				AppWidgetManager.getInstance(this);

				Intent result=new Intent();

				result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
				setResult(RESULT_OK, result);
				sendBroadcast(new Intent(this, CGMWidget.class));
			}
		}
		
		super.onBackPressed();
	} 
}