package uk.me.geekylou.castmywebsite;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;

import android.app.Application;
import android.content.Context;

public class CastMyWebsiteApplication extends Application {
    private static VideoCastManager mCastMgr = null;
	private static final String APPLICATION_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

	public static synchronized VideoCastManager getVideoCastManager(Context ctx) 
    {
    	if (null == mCastMgr) 
    	{
    		mCastMgr = VideoCastManager.initialize(ctx, APPLICATION_ID, null, null); 
    		mCastMgr.enableFeatures(VideoCastManager.FEATURE_NOTIFICATION | VideoCastManager.FEATURE_LOCKSCREEN |
    				VideoCastManager.FEATURE_WIFI_RECONNECT | VideoCastManager.FEATURE_DEBUGGING);
    	 }
    	 mCastMgr.setContext(ctx);
    	 return mCastMgr;
    	 }

}
