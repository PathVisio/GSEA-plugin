package org.pathvisio.ora.plugin;

import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.Preference;
import org.pathvisio.core.preferences.PreferenceManager;

/**
 * Preferences related to this plug-in that will be stored together with
 * other PathVisio preferences.
 */
public enum StatisticsPreference implements Preference
{
	STATS_DIR_LAST_USED_PATHWAY (PreferenceManager.getCurrent().get(GlobalPreference.DIR_PWFILES)),
	STATS_DIR_LAST_USED_RESULTS (PreferenceManager.getCurrent().get(GlobalPreference.DIR_LAST_USED_PGEX)),
	MAPPFINDER_COMPATIBILITY (Boolean.toString(true)),
	STATS_RESULT_INCLUDE_FILENAME(Boolean.toString(false));

	StatisticsPreference (String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	private String defaultValue;

	public String getDefault() {
		return defaultValue;
	}

}