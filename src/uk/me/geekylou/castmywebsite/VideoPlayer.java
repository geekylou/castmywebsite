package uk.me.geekylou.castmywebsite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPlayer extends Activity {
    private ActionBar actionBar;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.video_player);
        Intent intent = getIntent();
        
        VideoView vidView = (VideoView)findViewById(R.id.videoView1);
        MediaController vidControl = new FullScreenMediaController(this,vidView);
        
        vidControl.setAnchorView(vidView);
        vidView.setMediaController(vidControl);
                
        String vidAddress = intent.getExtras().getString("url");
        Uri vidUri = Uri.parse(vidAddress);
        
        vidView.setVideoURI(vidUri);
        
        vidView.start();
        if (savedInstanceState != null)
        {
        	vidView.seekTo(savedInstanceState.getInt("VideoPosition", 0));
        }
    }
    
    static class FullScreenMediaController extends MediaController {
        View mVideoView;
        public FullScreenMediaController(Context context, View video) {
            super(context);
            mVideoView = video;
        }
        @Override
        public void show() {
            super.show();
        	
            if (mVideoView != null) {
                mVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
        @Override
        public void hide() {
            if (mVideoView != null) {
                mVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
            super.hide();
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      
      VideoView vidView = (VideoView)findViewById(R.id.videoView1);
      if (vidView != null)
      {
    	  savedInstanceState.putInt("VideoPosition", vidView.getCurrentPosition());
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
