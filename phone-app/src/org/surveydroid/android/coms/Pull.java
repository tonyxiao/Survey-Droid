/*---------------------------------------------------------------------------*
 * Pull.java                                                                 *
 *                                                                           *
 * Contains methods to pull data from the website.                           *
 *---------------------------------------------------------------------------*
 * Copyright 2011 Sema Berkiten, Vladimir Costescu, Henry Liu, Diego Vargas, *
 * Austin Walker, and Tony Xiao                                              *
 *                                                                           *
 * This file is part of Survey Droid.                                        *
 *                                                                           *
 * Survey Droid is free software: you can redistribute it and/or modify      *
 * it under the terms of the GNU General Public License as published by      *
 * the Free Software Foundation, either version 3 of the License, or         *
 * (at your option) any later version.                                       *
 *                                                                           *
 * Survey Droid is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 * GNU General Public License for more details.                              *
 *                                                                           *
 * You should have received a copy of the GNU General Public License         *
 * along with Survey Droid.  If not, see <http://www.gnu.org/licenses/>.     *
 *****************************************************************************/
package org.surveydroid.android.coms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;

import static org.surveydroid.android.database.SurveyDroidDB.*;

import org.surveydroid.android.Config;
import org.surveydroid.android.LocationTrackerService;
import org.surveydroid.android.Util;
import org.surveydroid.android.coms.WebClient;
import org.surveydroid.android.coms.WebClient.ApiException;
import org.surveydroid.android.database.SurveyDroidDB;

//TODO move all the database stuff into the ComsDBHelper.  Problem is, that
//would basically render this class a wrapper.  Have to think about it...

/**
 * Provides the ability to snyc the database with the website's.
 *
 * @author Tony Xaio
 * @author Austin Walker
 */
public class Pull
{
	//logging tag
	private static final String TAG = "Pull";

	//pull address to be appended to the server
    private static final String PULL_URL = "/api/pull/";
    
    private static Context c;

    /**
     * Syncs surveys, questions, choices, branches, and conditions with the
     * webserver's database.
     *
     * @param ctx - the Context
     */
    public static void syncWithWeb(Context ctxt)
    {
    	try
    	{
    		c = ctxt;
    		TelephonyManager tManager =
            	(TelephonyManager) ctxt.getSystemService(
            			Context.TELEPHONY_SERVICE);
    		StringBuilder url = new StringBuilder();
    		if (Config.getSetting(ctxt, Config.HTTPS, Config.HTTPS_DEFAULT))
    			url.append("https://");
    		else
    			url.append("http://");
    		url.append(Config.getSetting(ctxt,
    				Config.SERVER, Config.SERVER_DEFAULT));
    		url.append(PULL_URL);
    		String uid = tManager.getDeviceId();
    		if (uid != null)
    		{
	    		url.append(uid);
	    		Util.v(null, TAG, "Pull url: " + url.toString());
	    		JSONObject json;
	    		try
	    		{
	    			json = new JSONObject(WebClient.getUrlContent(ctxt, url.toString()));
	    		}
	    		catch (Exception e)
	    		{
	    			Util.e(ctxt, TAG,
	    					"Unable to communicate with remote server");
	    			try
	    			{
	    				ApiException apiE = (ApiException) e;
	    				Util.e(ctxt, TAG, "Reason: " + apiE.getMessage());
	    				Util.e(null, TAG, "Make sure this device is registered"
	    						+ " and the server is working");
	    			}
	    			catch (Exception unknownE)
	    			{
	    				Util.e(ctxt, TAG, "Unkown Reason: " + Util.fmt(e));
	    			}
	    			return;
	    		}
	            SurveyDroidDB pdb = new SurveyDroidDB(ctxt);
	            SQLiteDatabase sdb = pdb.getWritableDatabase();
	            syncSurveys(sdb, json.getJSONArray("surveys"));
	            syncQuestions(sdb, json.getJSONArray("questions"));
	            syncChocies(sdb, json.getJSONArray("choices"));
	            syncBranches(sdb, json.getJSONArray("branches"));
	            syncConditions(sdb, json.getJSONArray("conditions"));
	            syncConfig(sdb, json.getJSONObject("config"), ctxt);
	            sdb.close();
	            pdb.close();
        	}
        	else
        	{
        		Util.w(ctxt, TAG, "Device ID not available");
        		Util.w(null, TAG, "Will reschedule and try again later");
        	}
        }
    	catch (Exception e)
    	{
            Util.e(ctxt, TAG, Util.fmt(e));
            throw new RuntimeException("FATAL ERROR", e);
    	}
    	finally
    	{
        	c = null;
    	}
    }

    private static void syncSurveys(SQLiteDatabase db, JSONArray surveys)
    {
    	Util.i(null, TAG, "Syncing surveys table");
    	try
    	{
    		Util.d(c, TAG, "Fetched " + surveys.length() + " surveys");
	    	for (int i = 0 ; i < surveys.length(); i++)
	    	{
	    		JSONObject survey = surveys.getJSONObject(i);
	    		ContentValues values = new ContentValues();
	    		int id = survey.getInt("id");
	    		values.put(SurveyTable._ID, id);
	    		values.put(SurveyTable.NAME,
	    				survey.getString(SurveyTable.NAME));
	    		values.put(SurveyTable.CREATED,
	    				survey.getLong(SurveyTable.CREATED));
	    		values.put(SurveyTable.QUESTION_ID,
	    				survey.getInt(SurveyTable.QUESTION_ID));
	    		values.put(SurveyTable.SUBJECT_INIT,
	    				survey.getString(SurveyTable.SUBJECT_INIT));
	    		values.put(SurveyTable.MO, survey.getString(SurveyTable.MO));
	    		values.put(SurveyTable.TU, survey.getString(SurveyTable.TU));
	    		values.put(SurveyTable.WE, survey.getString(SurveyTable.WE));
	    		values.put(SurveyTable.TH, survey.getString(SurveyTable.TH));
	    		values.put(SurveyTable.FR, survey.getString(SurveyTable.FR));
	    		values.put(SurveyTable.SA, survey.getString(SurveyTable.SA));
	    		values.put(SurveyTable.SU, survey.getString(SurveyTable.SU));

	    		//subject variables is special (it's an object)
	    		try
	    		{
	    			JSONObject vars = survey.getJSONObject("subject_variables");
	    			JSONArray keys = vars.names();
		    		if (keys != null)
		    		{
			    		for (int j = 0; i < keys.length(); j++)
			    		{
			    			String key = keys.getString(j);
							Config.putSetting(c, Config.USER_DATA + "#" + id +
									"#" + key, vars.getString(key));
			    		}
		    		}
	    					
	    		}
	    		catch (JSONException e)
	    		{
	    			Util.w(null, TAG, "no or mal-formed subject variables");
	    		}

	    		//TODO change this so that it uses replace()?
	    		db.beginTransaction();
	    		db.delete(SURVEY_TABLE_NAME, SurveyTable._ID + " = ?",
	    				new String[] {Integer.toString(survey.getInt("id"))});
				if (db.insert(SURVEY_TABLE_NAME, null, values) == -1 )
				{
					db.endTransaction();
					throw new RuntimeException("Database insert error");
				}
				db.setTransactionSuccessful();
				db.endTransaction();
	    	}
    	}
    	catch (JSONException e)
    	{
			Util.e(c, TAG, Util.fmt(e));
		}
    }
    private static void syncQuestions(SQLiteDatabase db, JSONArray questions)
    {
    	Util.i(null, TAG, "Syncing questions table");
    	try
    	{
	    	for (int i = 0; i < questions.length(); i++)
	    	{
	    		JSONObject survey = questions.getJSONObject(i);
	    		ContentValues values = new ContentValues();
	    		values.put(QuestionTable._ID, survey.getInt("id"));
	    		values.put(QuestionTable.SURVEY_ID,
	    				survey.getInt(QuestionTable.SURVEY_ID));
	    		values.put(QuestionTable.Q_TEXT,
	    				survey.getString(QuestionTable.Q_TEXT));
	    		int type = survey.getInt(QuestionTable.Q_TYPE);
	    		values.put(QuestionTable.Q_TYPE, type);
	    		switch (type)
	    		{
	    		case SurveyDroidDB.QuestionTable.SCALE_IMG:
	    			values.put(QuestionTable.Q_SCALE_IMG_LOW,
		    				survey.getString(QuestionTable.Q_SCALE_IMG_LOW));
		    		values.put(QuestionTable.Q_SCALE_IMG_HIGH,
		    				survey.getString(QuestionTable.Q_SCALE_IMG_HIGH));
	    			break;
	    		case SurveyDroidDB.QuestionTable.SCALE_TEXT:
	    			values.put(QuestionTable.Q_SCALE_TEXT_LOW,
		    				survey.getString(QuestionTable.Q_SCALE_TEXT_LOW));
		    		values.put(QuestionTable.Q_SCALE_TEXT_HIGH,
		    				survey.getString(QuestionTable.Q_SCALE_TEXT_HIGH));
	    			break;
	    		}
	    		if (db.replace(QUESTION_TABLE_NAME, null, values) == -1 )
				{
					throw new RuntimeException("Database replace error");
				}
	    	}
    	}
    	catch (JSONException e)
    	{
			Util.e(c, TAG, Util.fmt(e));
		}
    }
    private static void syncConditions(SQLiteDatabase db, JSONArray conditions)
    {
    	Util.i(null, TAG, "Syncing conditions table");
    	try
    	{
	    	for (int i = 0; i < conditions.length(); i++)
	    	{
	    		JSONObject survey = conditions.getJSONObject(i);
	    		ContentValues values = new ContentValues();
	    		values.put(ConditionTable._ID, survey.getInt("id"));
	    		values.put(ConditionTable.BRANCH_ID,
	    				survey.getInt(ConditionTable.BRANCH_ID));
	    		values.put(ConditionTable.QUESTION_ID,
	    				survey.getInt(ConditionTable.QUESTION_ID));
	    		values.put(ConditionTable.CHOICE_ID,
	    				survey.getInt(ConditionTable.CHOICE_ID));
	    		values.put(ConditionTable.TYPE, survey.getInt(ConditionTable.TYPE));
	    		if (db.replace(CONDITION_TABLE_NAME, null, values) == -1 )
				{
					throw new RuntimeException("Database replace error");
				}
	    	}
    	}
    	catch (JSONException e)
    	{
			Util.e(c, TAG, Util.fmt(e));
		}
    }
    private static void syncBranches(SQLiteDatabase db, JSONArray branches)
    {
    	Util.i(null, TAG, "Syncing branches table");
    	try
    	{
	    	for (int i=0; i<branches.length(); i++)
	    	{
	    		JSONObject survey = branches.getJSONObject(i);
	    		ContentValues values = new ContentValues();
	    		values.put(BranchTable._ID, survey.getInt("id"));
	    		values.put(BranchTable.QUESTION_ID,
	    				survey.getInt(BranchTable.QUESTION_ID));
	    		values.put(BranchTable.NEXT_Q, survey.getInt(BranchTable.NEXT_Q));
	    		if (db.replace(BRANCH_TABLE_NAME, null, values) == -1 )
				{
					throw new RuntimeException("Database replace error");
				}
	    	}
    	}
    	catch (JSONException e)
    	{
			Util.e(c, TAG, Util.fmt(e));
		}
    }
    private static void syncChocies(SQLiteDatabase db, JSONArray choices)
    {
    	Util.i(null, TAG, "Syncing choices table");
    	try
    	{
	    	for (int i = 0; i < choices.length(); i++)
	    	{
	    		JSONObject survey = choices.getJSONObject(i);
	    		ContentValues values = new ContentValues();
	    		values.put(ChoiceTable._ID, survey.getInt("id"));
	    		values.put(ChoiceTable.QUESTION_ID,
	    				survey.getInt(ChoiceTable.QUESTION_ID));
	    		int type = survey.getInt(ChoiceTable.CHOICE_TYPE);
	    		switch (type)
	    		{
	    		case SurveyDroidDB.ChoiceTable.TEXT_CHOICE:
	    			values.put(ChoiceTable.CHOICE_TEXT,
		    				survey.getString(ChoiceTable.CHOICE_TEXT));
	    			break;
	    		case SurveyDroidDB.ChoiceTable.IMG_CHOICE:
	    			values.put(ChoiceTable.CHOICE_IMG,
		    				survey.getString(ChoiceTable.CHOICE_IMG));
	    			break;
	    		}
	    		values.put(ChoiceTable.CHOICE_TYPE, type);
	    		if (db.replace(CHOICE_TABLE_NAME, null, values) == -1 )
				{
					throw new RuntimeException("Database replace error");
				}
	    	}
    	}
    	catch (JSONException e)
    	{
			Util.e(c, TAG, Util.fmt(e));
		}
    }
    
    private static void syncConfig(SQLiteDatabase db, JSONObject config, Context ctxt)
    {
    	Util.i(ctxt, TAG, "Updating configuration values");
    	try
    	{
    		//do the special keys first
    		
    		//user data
    		try
    		{
	    		JSONObject user_data = config.getJSONObject("subject_variables");
	    		JSONArray udNames = user_data.names();
	    		if (udNames != null)
	    		{
		    		for (int i = 0; i < udNames.length(); i++)
		    		{
		    			String key = udNames.getString(i);
						Config.putSetting(ctxt, Config.USER_DATA + "#" + key,
								user_data.getString(key));
		    		}
	    		}
    		}
    		catch (JSONException e)
    		{
    			Util.w(ctxt, TAG, "No user_data");
    		}
    		
    		try
    		{
	    		//application features
	    		JSONObject features = config.getJSONObject("features_enabled");
	    		if (features.optInt("survey", 0) == 1)
	    			Config.putSetting(ctxt, Config.SURVEYS_SERVER, true);
	    		else
	    			Config.putSetting(ctxt, Config.SURVEYS_SERVER, false);
	    		if (features.optInt("callog", 0) == 1)
	    		{
	    			Config.putSetting(ctxt, Config.CALL_LOG_SERVER, true);
	    			Util.d(null, TAG, "call log on");
	    		}
	    		else
	    		{
	    			Config.putSetting(ctxt, Config.CALL_LOG_SERVER, false);
	    			Util.d(null, TAG, "call log off");
	    		}
	    		if (features.optInt("location", 0) == 1)
	    			Config.putSetting(ctxt, Config.TRACKING_SERVER, true);
	    		else
	    			Config.putSetting(ctxt, Config.TRACKING_SERVER, false);
	    		config.remove("features_enabled");
			}
			catch (JSONException e)
			{
				Util.w(ctxt, TAG, "No features_enabled");
			}
    		
			try
			{
	    		//location tracked
	    		JSONArray locations = config.getJSONArray("location_tracked");
	    		Config.putSetting(ctxt, Config.NUM_LOCATIONS_TRACKED,
	    				locations.length());
	    		for (int i = 0; i < locations.length(); i++)
	    		{
	    			JSONObject location = locations.getJSONObject(i);
	    			Config.putSetting(ctxt, Config.TRACKED_LONG + i,
	    					(float) location.optDouble("long", 0.0));
	    			Config.putSetting(ctxt, Config.TRACKED_LAT + i,
	    					(float) location.optDouble("lat", 0.0));
	    			Config.putSetting(ctxt, Config.TRACKED_RADIUS + i,
	    					(float) location.optDouble("radius", 0.0));
	    		}
	    		config.remove("location_tracked");
			}
			catch (JSONException e)
			{
				Util.w(ctxt, TAG, "No location_tracked");
				Config.putSetting(ctxt, Config.NUM_LOCATIONS_TRACKED, 0);
			}
    		
			try
			{
	    		//times tracked
	    		JSONArray times = config.getJSONArray("time_tracked");
	    		Config.putSetting(ctxt, LocationTrackerService.TIMES_COALESCED,
	    				false);
	    		Config.putSetting(ctxt, Config.NUM_TIMES_TRACKED,
	    				times.length());
	    		for (int i = 0; i < times.length(); i++)
	    		{
	    			JSONObject time = times.getJSONObject(i);
	    			Config.putSetting(ctxt, Config.TRACKED_START + i,
	    					"" + time.optInt("start", 0));
	    			Config.putSetting(ctxt, Config.TRACKED_END + i,
	    					"" + time.optInt("end", 0));
	    		}
	    		config.remove("time_tracked");
	    		//tell the location tracking service to recalculate
	    		Intent trackingIntent = new Intent(ctxt,
	    				LocationTrackerService.class);
	    		trackingIntent.setAction(
	    				LocationTrackerService.ACTION_START_TRACKING);
	    		ctxt.startService(trackingIntent);
			}
			catch (JSONException e)
			{
				Util.w(ctxt, TAG, "No times_tracked");
				Config.putSetting(ctxt, Config.NUM_TIMES_TRACKED, 0);
			}
    		
    		//voice_format (because it needs to be converted
    		String format = config.optString("voice_format");
    		if (format.equals("mpeg4"))
    			Config.putSetting(ctxt, Config.VOICE_FORMAT,
    					MediaRecorder.OutputFormat.MPEG_4);
    		else if (format.equals("3gp"))
    			Config.putSetting(ctxt, Config.VOICE_FORMAT,
    					MediaRecorder.OutputFormat.THREE_GPP);
    		config.remove("voice_format");
    		
    		//now do the standard ones
    		//note that this requires the keys in the incoming JSON
    		//to be the same as those defined in Config.java
    		JSONArray cNames = config.names();
    		if (cNames == null) return;
    		for (int i = 0; i < cNames.length(); i++)
    		{
    			boolean done = false;
    			String key = cNames.getString(i);
    			Util.v(null, TAG, "Current key: " + key);
    			
    			//check for a string value
    			for (String k : Config.STRINGS)
    			{
    				if (key.equals(k))
    				{
    					Config.putSetting(ctxt, key, config.getString(key));
    					done = true;
    					break;
    				}
    			}
    			if (done) continue;
    			
    			//check for an integer value
    			for (String k : Config.INTS)
    			{
    				if (key.equals(k))
    				{
    					Config.putSetting(ctxt, key, config.getInt(key));
    					done = true;
    					break;
    				}
    			}
    			if (done) continue;
    			
    			//check for a boolean value
    			for (String k : Config.BOOLEANS)
    			{
    				if (key.equals(k))
    				{
    					int val = config.getInt(key);
    					boolean boolVal = true;
    					if (val == 0) boolVal = false;
    					Config.putSetting(ctxt, key, boolVal);
    					done = true;
    					break;
    				}
    			}
    			if (done) continue;
    			
    			//check for a float value
    			for (String k : Config.FLOATS)
    			{
    				if (key.equals(k))
    				{
    					Config.putSetting(ctxt, key,
    							(float) config.getDouble(key));
    					break;
    				}
    			}
    		}
    	}
    	catch (JSONException e)
    	{
    		Util.e(ctxt, TAG, Util.fmt(e));
    	}
    }
}