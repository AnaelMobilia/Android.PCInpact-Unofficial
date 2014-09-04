/*
 * Copyright 2014 Anael Mobilia
 * 
 * This file is part of NextINpact-Unofficial.
 * 
 * NextINpact-Unofficial is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * NextINpact-Unofficial is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with NextINpact-Unofficial. If not, see <http://www.gnu.org/licenses/>
 */
package com.nextinpact;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.pcinpact.R;

/**
 * Options de l'application
 * 
 * @author Anael
 * 
 */
public class OptionsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Je lance l'activit�
		setTheme(NextInpact.THEME);
		super.onCreate(savedInstanceState);
		// TODO : 2014-07-21 - Anael - PreferenceActivity est partiellement
		// deprecated. PreferenceFragment serait mieux, mais API v11.
		addPreferencesFromResource(R.xml.options);
	}

}
