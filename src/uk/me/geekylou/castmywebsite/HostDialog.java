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

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.host_dialog);
        
    	mNameTextView = ((TextView) findViewById(R.id.editTextName));
    	mURLTextView = ((TextView) findViewById(R.id.editTextURL));
    	mErrorTextView = ((TextView) findViewById(R.id.textViewError));
    	mCreateButton = (Button) findViewById(R.id.buttonOk);
    	mCreateButton.setOnClickListener(new OnClickListener()
        {

			@Override
			public void onClick(View v) {
				
				BookmarkWrapper bookmark = new BookmarkWrapper();
				bookmark.mName = mNameTextView.getText().toString();
				try {
					bookmark.mFile = new URL(mURLTextView.getText().toString());
					
					new Bookmarks(HostDialog.this).insertEntry(bookmark);
					finish();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					mErrorTextView.setText("Invalid URL entered!");
				}
				//db.dumpDB(FileChooser.this, mDirectory + File.separator + filename + ".blob");
			}  	
        });

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
