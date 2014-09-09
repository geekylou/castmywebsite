package uk.me.geekylou.castmywebsite;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class HostDialog extends Activity {
    private ActionBar actionBar;
	private Button mCreateButton;
	private TextView mNameTextView;
	private TextView mURLTextView;
	private TextView mErrorTextView;
	private Button mCancelButton;
	private Bookmarks mBookmarks;
	private BookmarkWrapper mBookmark = null;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.host_dialog);
        
        final Intent intent = getIntent();
        String action = intent.getAction();
        mBookmarks = new Bookmarks(HostDialog.this);
        
    	mNameTextView = ((TextView) findViewById(R.id.editTextName));
    	mURLTextView = ((TextView) findViewById(R.id.editTextURL));
    	mErrorTextView = ((TextView) findViewById(R.id.textViewError));
    	mCreateButton = (Button) findViewById(R.id.buttonOk);
    	mCreateButton.setOnClickListener(new OnClickListener()
        {

			@Override
			public void onClick(View v) {
				
				try {
					
					if (mBookmark != null)
					{
						mBookmark.mFile = new URL(mURLTextView.getText().toString());
						mBookmark.mName = mNameTextView.getText().toString();
						mBookmarks.updateEntry(mBookmark);
					}
					else
					{
						BookmarkWrapper bookmark = new BookmarkWrapper();
						bookmark.mFile = new URL(mURLTextView.getText().toString());
						bookmark.mName = mNameTextView.getText().toString();

						mBookmarks.insertEntry(bookmark);
					}
					finish();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					mErrorTextView.setText("Invalid URL entered!");
				}
			}  	
        });
    	mCancelButton = (Button) findViewById(R.id.buttonCancel);
    	mCancelButton.setOnClickListener(new OnClickListener()
        {

			@Override
			public void onClick(View v) 
			{	
				finish();
			}  	
        });
    	
    	if (action != null && action.equals(Intent.ACTION_EDIT))
        {
        	mBookmark = mBookmarks.getEntry(intent.getIntExtra("id",-1));
        	mNameTextView.setText(mBookmark.mName);
        	mURLTextView.setText(mBookmark.mFile.toExternalForm());
        }

    }
	protected void onPause()
	{
		super.onPause();
	}
	
	protected void onResume()
	{
		super.onResume();
	}
}
