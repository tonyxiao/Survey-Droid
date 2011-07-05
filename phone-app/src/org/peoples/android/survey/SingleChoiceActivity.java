/*---------------------------------------------------------------------------*
 * SingleChoiceActivty.java                                                  *
 *                                                                           *
 * Shows the user a question with set options to pick from.  This type of    *
 * question only allows the user to pick a single answer.                    *
 *---------------------------------------------------------------------------*/
package org.peoples.android.survey;

import java.util.ArrayList;
import java.util.Collection;

import org.peoples.android.Config;
import org.peoples.android.R;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Shows the user a question with set options to pick from.  This type of
 * question only allows the user to pick a single answer.
 * 
 * @author Austin Walker
 */
public class SingleChoiceActivity extends QuestionActivity
{
	//the logging tag
	private static final String TAG = "SingleChoiceActivity";
	
	//the main list where choices are shown
	private ListView listView;
	
	@Override
	protected void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		//FIXME set to proper views once we get horizontal/vertical ones made
		setContentView(R.layout.multiple_choice);
		
		//set the buttons up
		findViewById(R.id.multiple_choice_backButton).setOnClickListener(
				prevListener);
		findViewById(R.id.multiple_choice_nextButton).setOnClickListener(
				nextListener);
	}

	@Override
	protected void onSurveyLoaded()
	{
		//set the question text
		TextView qText = (TextView) findViewById(R.id.multiple_choice_question);
		qText.setText(survey.getText());
		
		Choice[] choices = survey.getChoices();
		Object[][] list = new Object[choices.length][2];
		for (int i = 0; i < choices.length; i++)
		{
			list[i][ImageOrTextAdapter.IMG_POS] = choices[i].getImg();
			list[i][ImageOrTextAdapter.STRING_POS] = choices[i].getText();
		}
		//FIXME still doesn't look like items are selected
		listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(new ImageOrTextAdapter(this, list));
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}
	
	@Override
	protected void answer()
	{
		Collection<Choice> answer = new ArrayList<Choice>();
		Choice[] choices = survey.getChoices();
		answer.add(choices[listView.getCheckedItemPosition()]);
		survey.answer(answer);
	}

	@Override
	protected boolean isAnswered()
	{
		if (Config.D)
			Log.d(TAG, "Answer index: " + listView.getCheckedItemPosition());
		if (listView.getCheckedItemPosition() == ListView.INVALID_POSITION)
			return false;
		return true;
	}
	
	@Override
	protected String getInvalidAnswerMsg()
	{
		return "You must select a choice";
	}
}