package com.nightscoutwidget.android.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.util.Log;

import com.nightscoutwidget.android.R;

public class SettingsFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {
	int mAppWidgetId = 0;
	SettingsActivity sact = null;
	Context context;

	public SettingsFragment(int mAppWidgetId, SettingsActivity sact) {
		this.mAppWidgetId = mAppWidgetId;
		this.sact = sact;
		this.context = sact;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* set preferences */
		addPreferencesFromResource(R.xml.preferences);
		addMedtronicOptionsListener();
		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
		// iterate through all preferences and update to saved value
		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
			initSummary(getPreferenceScreen().getPreference(i));
		}

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		updatePrefSummary(findPreference(key));
	}

	@Override
	public void onResume() {

		super.onResume();
		getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {

		getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onDestroy() {
		// sact.finishSettings();

		super.onDestroy();
	}

	// iterate through all preferences and update to saved value
	private void initSummary(Preference p) {

		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}

		} else if (p instanceof PreferenceScreen) {
			PreferenceScreen pSc = (PreferenceScreen) p;
			for (int i = 0; i < pSc.getPreferenceCount(); i++) {
				initSummary(pSc.getPreference(i));
			}

		} else {

			updatePrefSummary(p);
		}
	}

	// update preference summary
	private void updatePrefSummary(Preference p) {

		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		}
		if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}
		if (p instanceof MultiSelectListPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}
		if (p instanceof RingtonePreference) {

			RingtonePreference rtPref = (RingtonePreference) p;
			p.setSummary(rtPref.getTitle());
		}

	}

	private void addMedtronicOptionsListener() {
		final ListPreference mon_type = (ListPreference) findPreference("monitor_type");
		final EditTextPreference med_id = (EditTextPreference) findPreference("medtronic_cgm_id");
		final ListPreference res_units = (ListPreference) findPreference("reservoir_ins_units");
		final ListPreference metric_type = (ListPreference) findPreference("metric_preference");
		
		int index = mon_type.findIndexOfValue(mon_type.getValue());

		if (index == 1) {
			med_id.setEnabled(true);
		} else {
			med_id.setEnabled(false);
		}
		res_units.setEnabled(med_id.isEnabled());
		mon_type.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				final String val = newValue.toString();
				int index = mon_type.findIndexOfValue(val);
				if (index == 1)
					med_id.setEnabled(true);
				else
					med_id.setEnabled(false);
				res_units.setEnabled(med_id.isEnabled());
				return true;
			}
		});
		metric_type.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				final String val = newValue.toString();
				int index = metric_type.findIndexOfValue(val);
				double divisor = 1;
				if (index == 1)
					divisor = 18;
				else 
					divisor = 1.0/18.0;
				double upperwarning = 0;
				double lowerwarning = 0;
				double upperalarm = 0;
				double loweralarm = 0;
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				SharedPreferences.Editor editor = prefs.edit();
				try {
					upperwarning = Integer.parseInt(prefs.getString(
							"upper_warning_color", ""+((int)(140))));
					editor.putString("upper_warning_color", ""+((int)Math.ceil(upperwarning/divisor)));
					Preference p = findPreference("upper_warning_color");
					EditTextPreference editTextPref = (EditTextPreference) p;
					editTextPref.setText( ""+((int)(Math.ceil(upperwarning/divisor))));
					p.setSummary(editTextPref.getText());
				} catch (Exception e) {

				}
				try {
					lowerwarning = Integer.parseInt(prefs.getString(
							"lower_warning_color", ""+((int)(80))));
					editor.putString("lower_warning_color", ""+((int)Math.floor(lowerwarning/divisor)));
					Preference p = findPreference("lower_warning_color");
					EditTextPreference editTextPref = (EditTextPreference) p;
					editTextPref.setText( ""+((int)Math.floor(lowerwarning/divisor)));
					p.setSummary(editTextPref.getText());

				} catch (Exception e) {

				}
				try {
					upperalarm = Integer.parseInt(prefs.getString(
							"upper_alarm_color", ""+((int)(170))));
					
					editor.putString("upper_alarm_color", ""+((int)Math.ceil(upperalarm/divisor)));
					Preference p = findPreference("upper_alarm_color");
					EditTextPreference editTextPref = (EditTextPreference) p;
					editTextPref.setText( ""+((int)Math.ceil(upperalarm/divisor)));
					p.setSummary(editTextPref.getText());
				} catch (Exception e) {

				}
				try {
					loweralarm = Integer.parseInt(prefs.getString(
							"lower_alarm_color", ""+((int)(70))));
					editor.putString("lower_alarm_color", ""+((int)Math.floor(loweralarm/divisor)));
					Preference p = findPreference("lower_alarm_color");
					EditTextPreference editTextPref = (EditTextPreference) p;
					editTextPref.setText( ""+((int)Math.floor(loweralarm/divisor)));
					p.setSummary(editTextPref.getText());
				} catch (Exception e) {

				}
				editor.commit();
				return true;
			}
		});
		
	}
}