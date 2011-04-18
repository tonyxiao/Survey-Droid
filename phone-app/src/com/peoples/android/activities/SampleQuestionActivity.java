package com.peoples.android.activities;

import com.peoples.android.R;
import com.peoples.android.processTest.LocationTestActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class SampleQuestionActivity extends Activity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multiple3choiceview);
        
        TextView q1 = (TextView)this.findViewById(R.id.question_textView);
        q1.setText("Who is your favorite actress?");
        RadioButton q1r1 = (RadioButton)this.findViewById(R.id.radio1);
        q1r1.setText("Keira Knightley");
        RadioButton q1r2 = (RadioButton)this.findViewById(R.id.radio2);
        q1r2.setText("Natalie Portman");
        RadioButton q1r3 = (RadioButton)this.findViewById(R.id.radio3);
        q1r3.setText("Emmanuelle Chiriqui");
        
        Button next = (Button) findViewById(R.id.button1);
        next.setText("Next Question");
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	Intent myIntent = new Intent(view.getContext(), SampleQuestionActivity2.class);
                startActivityForResult(myIntent, 0);
            }
        });

    }

}