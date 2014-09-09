package uk.me.geekylou.castmywebsite;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.sample.castcompanionlibrary.cast.BaseCastManager;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.widgets.MiniController;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

class BookmarkChooserImageViewAdapter extends ArrayAdapter<BookmarkWrapper>
{
	private static LayoutInflater inflater=null;
	
	public BookmarkChooserImageViewAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);		
		
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.itemb, null);
        
        TextView textBody=(TextView)vi.findViewById(R.id.textViewBody);
        
        textBody.setText(this.getItem(position).mName);
        TextView textFooter=(TextView)vi.findViewById(R.id.textViewFooter);

        ImageView imageViewIcon = (ImageView)vi.findViewById(R.id.imageView1);
        imageViewIcon.setImageBitmap(this.getItem(position).mIcon);
//        textFooter.setText(new Date(this.getItem(position).mFile.lastModified()).toString());
        return vi;
	}
}

class BookmarkWrapper
{
	int     id;
	URL 	mFile;
	Bitmap  mIcon;
	String  mName;

	BookmarkWrapper()
	{
		
	}
	BookmarkWrapper(URL file,String name,Bitmap icon)
	{
		mName        = name;
		mFile        = file;
		mIcon 		 = icon;
	}
}

public class BookmarksActivity extends ActionBarActivity {
	private static final String APPLICATION_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
	private static VideoCastManager mCastMgr;
	private ListView mTimeLineView;
	private ArrayAdapter<BookmarkWrapper> mBookmarkArrayAdapter;
	private String mDirectory = "";
	private boolean restore = true;
	private AsyncTask<Void, Void, FileWrapper[]> mJsonRequest;
	private HashMap<URL,Bitmap> iconCache = new HashMap<URL,Bitmap>();
	private VideoCastManager mVideoCastManager;
	private MiniController mMini;
	private Bookmarks bookmarks;
	
	private void reload()
	{
		mBookmarkArrayAdapter.clear();
		mBookmarkArrayAdapter = bookmarks.toArrayAdapter(mBookmarkArrayAdapter);
		mTimeLineView.setAdapter(mBookmarkArrayAdapter);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        BaseCastManager.checkGooglePlayServices(this);

        mVideoCastManager = getVideoCastManager(this);
        
        setContentView(R.layout.filechooser);
        
        bookmarks = new Bookmarks(this);
        
        mMini = (MiniController) findViewById(R.id.miniController1);
        mVideoCastManager.addMiniController(mMini);

        // Initialise the array adapter for the list view.
        mBookmarkArrayAdapter = new BookmarkChooserImageViewAdapter(this, R.layout.itemb);
        
        mTimeLineView = (ListView) findViewById(R.id.listView1);
        registerForContextMenu(mTimeLineView);
        
        mTimeLineView.setOnItemClickListener (new AdapterView.OnItemClickListener()
        {
        	  @Override
        	  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        		  BookmarkWrapper selectedFile = mBookmarkArrayAdapter.getItem(position);
        		  
    			  Intent intent = new Intent(BookmarksActivity.this, StreamListingActivity.class);
	        		
    			  intent.setAction(Intent.ACTION_VIEW);
    			  intent.putExtra("url",selectedFile.mFile.toExternalForm());
          		
    			  startActivity(intent);
        	  }
        	});
        
        // Initialise the array adapter for the list view.
        reload();
   }
	protected void onDestroy()
	{
		mVideoCastManager.removeMiniController(mMini);
		super.onDestroy();
	}
    @Override
    protected void onResume()
    {
    	super.onResume();
    	mVideoCastManager.incrementUiCounter();
    	reload();
    }

    @Override
    protected void onPause()
    {
    	super.onPause();
    	mVideoCastManager.decrementUiCounter();
    }

    /**
     * Called when your activity's options menu needs to be created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu); 
        mCastMgr.addMediaRouterButton(menu, R.id.media_route_menu_item);
       return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.item1:
  		  	Intent intent = new Intent(BookmarksActivity.this, HostDialog.class);
			  
  		  	intent.setAction(Intent.ACTION_INSERT);
  		  	startActivity(intent);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public static VideoCastManager getVideoCastManager(Context ctx) 
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
