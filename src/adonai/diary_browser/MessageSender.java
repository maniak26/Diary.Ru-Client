package adonai.diary_browser;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import adonai.diary_browser.entities.Post;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class MessageSender extends Activity implements OnClickListener, OnCheckedChangeListener
{
	TextView titleText;
	TextView contentText;
	TextView themesText;
	TextView musicText;
	TextView moodText;
	Button mPublish;
	CheckBox mShowOptionals;
	CheckBox mShowPoll;
	
	TextView mPollTitle;
	TextView mPollChoice1;
	TextView mPollChoice2;
	TextView mPollChoice3;
	TextView mPollChoice4;
	TextView mPollChoice5;
	TextView mPollChoice6;
	TextView mPollChoice7;
	TextView mPollChoice8;
	TextView mPollChoice9;
	TextView mPollChoice10;
	
	List<View> optionals = new ArrayList<View>();
	List<View> pollScheme = new ArrayList<View>();
	
	String mSignature = null;
	String mDiaryId = null;
	
	DiaryHttpClient mDHCL;
	Post mPost;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mDHCL = Globals.mDHCL;
        mPost = new Post();
        
        setContentView(R.layout.message_sender_a);
        
    	titleText = (TextView) findViewById(R.id.message_title);
    	contentText = (TextView) findViewById(R.id.message_content);
    	themesText = (TextView) findViewById(R.id.message_themes);
    	musicText = (TextView) findViewById(R.id.message_music);
    	moodText = (TextView) findViewById(R.id.message_mood);
    	mPublish = (Button)findViewById(R.id.message_publish);
    	mPublish.setOnClickListener(this);
    	
    	mPollTitle = (TextView) findViewById(R.id.message_poll_title);
    	mPollChoice1 = (TextView) findViewById(R.id.message_poll_1);
    	mPollChoice2 = (TextView) findViewById(R.id.message_poll_2);
    	mPollChoice3 = (TextView) findViewById(R.id.message_poll_3);
    	mPollChoice4 = (TextView) findViewById(R.id.message_poll_4);
    	mPollChoice5 = (TextView) findViewById(R.id.message_poll_5);
    	mPollChoice6 = (TextView) findViewById(R.id.message_poll_6);
    	mPollChoice7 = (TextView) findViewById(R.id.message_poll_7);
    	mPollChoice8 = (TextView) findViewById(R.id.message_poll_8);
    	mPollChoice9 = (TextView) findViewById(R.id.message_poll_9);
    	mPollChoice10 = (TextView) findViewById(R.id.message_poll_10);
    	
    	mShowOptionals = (CheckBox) findViewById(R.id.message_optional);
    	mShowOptionals.setOnCheckedChangeListener(this);
    	mShowPoll = (CheckBox) findViewById(R.id.message_poll);
    	mShowPoll.setOnCheckedChangeListener(this);
    	
    	optionals.add(findViewById(R.id.message_themes_hint));
    	optionals.add(themesText);
    	optionals.add(findViewById(R.id.message_music_hint));
    	optionals.add(musicText);
    	optionals.add(findViewById(R.id.message_mood_hint));
    	optionals.add(moodText);
    	
    	pollScheme.add(mPollTitle);
    	pollScheme.add(mPollChoice1);
    	pollScheme.add(mPollChoice2);
    	pollScheme.add(mPollChoice3);
    	pollScheme.add(mPollChoice4);
    	pollScheme.add(mPollChoice5);
    	pollScheme.add(mPollChoice6);
    	pollScheme.add(mPollChoice7);
    	pollScheme.add(mPollChoice8);
    	pollScheme.add(mPollChoice9);
    	pollScheme.add(mPollChoice10);
    	
    	
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart()
    {
        Intent intent = getIntent();
        
        // запущено без запроса
        if(intent == null)
        	finish();
        
        mSignature = intent.getStringExtra("signature");
        mDiaryId = intent.getStringExtra("DiaryId");
        
        super.onStart();
    }

	public void onClick(View view) 
	{
		switch(view.getId())
		{
			case R.id.message_publish:
				
				// TODO: Сохранение в черновики
				// Задел на будущее - для сохранения в черновики
				mPost.set_title(titleText.getText().toString());
				mPost.set_text(Html.fromHtml(contentText.getText().toString()));
				mPost.set_themes(themesText.getText().toString());
				mPost.set_music(musicText.getText().toString());
				mPost.set_mood(moodText.getText().toString());

				try 
				{
					List<NameValuePair> postParams = new ArrayList<NameValuePair>();
					postParams.add(new BasicNameValuePair("act", "new_post_post"));
					postParams.add(new BasicNameValuePair("module", "journal"));
					postParams.add(new BasicNameValuePair("post_id", ""));
					postParams.add(new BasicNameValuePair("journal_id", mDiaryId));
					postParams.add(new BasicNameValuePair("referer", mDHCL.lastURL));
					postParams.add(new BasicNameValuePair("action", "dosend"));
					postParams.add(new BasicNameValuePair("post_type", ""));
					
					postParams.add(new BasicNameValuePair("title", mPost.get_title()));
					postParams.add(new BasicNameValuePair("message", mPost.get_text().toString()));
					
					if(mShowOptionals.isChecked())
					{
						postParams.add(new BasicNameValuePair("themes", mPost.get_themes()));
						postParams.add(new BasicNameValuePair("current_music", mPost.get_music()));
						postParams.add(new BasicNameValuePair("current_mood", mPost.get_mood()));
					}
					else
					{
						postParams.add(new BasicNameValuePair("themes", ""));
						postParams.add(new BasicNameValuePair("current_music", ""));
						postParams.add(new BasicNameValuePair("current_mood", ""));
					}
					
					postParams.add(new BasicNameValuePair("attachment", ""));
					postParams.add(new BasicNameValuePair("close_text", ""));
					
					if(mShowPoll.isChecked())
					{
						postParams.add(new BasicNameValuePair("poll_title", mPollTitle.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_1", mPollChoice1.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_2", mPollChoice2.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_3", mPollChoice3.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_4", mPollChoice4.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_5", mPollChoice5.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_6", mPollChoice6.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_7", mPollChoice7.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_8", mPollChoice8.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_9", mPollChoice9.getText().toString()));
						postParams.add(new BasicNameValuePair("poll_answer_10", mPollChoice10.getText().toString()));
					}
					else
					{
						postParams.add(new BasicNameValuePair("poll_title", ""));
						postParams.add(new BasicNameValuePair("poll_answer_1", ""));
						postParams.add(new BasicNameValuePair("poll_answer_2", ""));
						postParams.add(new BasicNameValuePair("poll_answer_3", ""));
						postParams.add(new BasicNameValuePair("poll_answer_4", ""));
						postParams.add(new BasicNameValuePair("poll_answer_5", ""));
						postParams.add(new BasicNameValuePair("poll_answer_6", ""));
						postParams.add(new BasicNameValuePair("poll_answer_7", ""));
						postParams.add(new BasicNameValuePair("poll_answer_8", ""));
						postParams.add(new BasicNameValuePair("poll_answer_9", ""));
						postParams.add(new BasicNameValuePair("poll_answer_10", ""));
					}
					
					postParams.add(new BasicNameValuePair("rewrite", "rewrite"));
					postParams.add(new BasicNameValuePair("save_type", "js2"));
					postParams.add(new BasicNameValuePair("signature", mSignature));
					
					mDHCL.postPage(mDHCL.lastURL + "diary.php", new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
					
					// Пост опубликован, возвращаемся
					Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
					returnIntent.putExtra("reloadContent", true);
					returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(returnIntent);
					finish();
					
				} 
				catch (UnsupportedEncodingException e) 
				{
					e.printStackTrace();
				}
				
			break;
		}
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
	{
		switch(buttonView.getId())
		{
			case R.id.message_optional:
				if(isChecked)
					for(View view : optionals)
						view.setVisibility(View.VISIBLE);
				else
					for(View view : optionals)
						view.setVisibility(View.GONE);
			break;
			case R.id.message_poll:
				if(isChecked)
					for(View view : pollScheme)
						view.setVisibility(View.VISIBLE);
				else
					for(View view : pollScheme)
						view.setVisibility(View.GONE);
			break;
			default:
			break;
		}
	}
    
}
