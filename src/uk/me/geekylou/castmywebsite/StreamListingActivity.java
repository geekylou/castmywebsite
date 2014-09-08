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

import uk.me.geekylou.castmywebsite.R;

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
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

class FileChooserImageViewAdapter extends ArrayAdapter<FileWrapper>
{
	private static LayoutInflater inflater=null;
	
	public FileChooserImageViewAdapter(Context context, int textViewResourceId) {
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

class FileWrapper
{
	URL 	mFile;
	Bitmap  mIcon;
	String  mName,mDirectory;
	boolean mIsDirectory;
	
	FileWrapper(String directory,String name, Bitmap icon)
	{
		mName        = name;
		mDirectory   = directory;
		mIsDirectory = true;
		mIcon 		 = icon;
	}

	FileWrapper(URL file,String name,Bitmap icon)
	{
		mName        = name;
		mFile        = file;
		mIsDirectory = false;
		mIcon 		 = icon;
	}
}

public class StreamListingActivity extends ActionBarActivity {
	private static final String APPLICATION_ID =  CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
	private static VideoCastManager mCastMgr;
	private ListView mTimeLineView;
	private ArrayAdapter<FileWrapper> mTimeLineArrayAdapter;
	private String mDirectory = "";
	private boolean restore = true;
	private Button mCreateButton;
	private TextView mFilenameTextView;
	private AsyncTask<URL, Void, FileWrapper[]> mJsonRequest;
	private HashMap<URL,Bitmap> iconCache = new HashMap<URL,Bitmap>();
	private VideoCastManager mVideoCastManager;
	private MiniController mMini;
	private void reload()
	{
		URL jsonDirectoryListingURL;
		try {
			jsonDirectoryListingURL = new URL("http://192.168.0.79/video/CastVideos-chrome/getDirectoryListing.py?directory="+URLEncoder.encode(mDirectory));

			if (mJsonRequest == null || mJsonRequest.getStatus() == AsyncTask.Status.FINISHED)
			{
				mTimeLineArrayAdapter.clear();
				mJsonRequest = new JSONLoader().execute(jsonDirectoryListingURL);
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	   private class JSONLoader extends AsyncTask<URL, Void, FileWrapper[]> {

		   private Bitmap loadIcon(URL url)
		   {    
			   if (!iconCache.containsKey(url))
			   {
					try {
						Bitmap bitmap =  BitmapFactory.decodeStream(url.openStream());
						
						iconCache.put(url, bitmap);
						
						return bitmap;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return null;
					}
			   }
			   else
			   {
				   return iconCache.get(url);
			   }
		   }
	        @Override
	        protected FileWrapper[] doInBackground(URL... params) {
	    			try {
						URL jsonDirectoryListingURL = params[0];
	    			
	    			InputStream in = jsonDirectoryListingURL.openStream();
	    			
	    			BufferedReader bReader = new BufferedReader(new InputStreamReader(in, "iso-8859-1"), 8);
	                StringBuilder sBuilder = new StringBuilder();

	                String line = null;
	                while ((line = bReader.readLine()) != null) {
	                    sBuilder.append(line + "\n");
	                }

	                in.close();
	                JSONObject result = new JSONObject(sBuilder.toString());
	                
		        	JSONArray directoryJsonArr,videoJsonArr;
					directoryJsonArr = result.getJSONArray("directories");
		            videoJsonArr     = result.getJSONArray("videos");
	            
		            FileWrapper[] files       = new FileWrapper[directoryJsonArr.length() + videoJsonArr.length()];
		            int           files_index = 0;
		            
		            for (int i = 0; i < directoryJsonArr.length(); i++)
		            {
		            	JSONObject directoryItem = directoryJsonArr.getJSONObject(i);
		                JSONArray directoryEntryJsonArr = directoryItem.getJSONArray("directory");
		                String icon = directoryItem.getString("icon");
		            	
		            	files[files_index] = new FileWrapper(directoryEntryJsonArr.getString(0),directoryItem.getString("title"),loadIcon(new URL("http://192.168.0.79"+icon)));
		            	files_index++;
		            }
		            		            
		            for (int i = 0; i < videoJsonArr.length(); i++)
		            {
		            	JSONObject directoryItem = videoJsonArr.getJSONObject(i);
		                JSONArray videoEntryJsonArr = directoryItem.getJSONArray("sources");
		                String icon = directoryItem.getString("icon");
		                
		            	files[files_index] = new FileWrapper(new URL(videoEntryJsonArr.getString(0)),directoryItem.getString("title"),loadIcon(new URL("http://192.168.0.79"+icon)));
		            	files_index++;
		            }

		            return files;
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
	            return null;
	        }

	        @Override
	        protected void onPostExecute(FileWrapper[] items) 
	        {
	        	mTimeLineArrayAdapter.addAll(items);
		        mTimeLineView.setAdapter(mTimeLineArrayAdapter);
	        }

	        @Override
	        protected void onPreExecute() {}

	        @Override
	        protected void onProgressUpdate(Void... values) {}
	    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BaseCastManager.checkGooglePlayServices(this);

        mVideoCastManager = getVideoCastManager(this);
        
        setContentView(R.layout.filechooser);
        
        mMini = (MiniController) findViewById(R.id.miniController1);
        mVideoCastManager.addMiniController(mMini);

        // Initialise the array adapter for the list view.
        mTimeLineArrayAdapter = new FileChooserImageViewAdapter(this, R.layout.itemb);
        
    	mFilenameTextView = ((TextView) findViewById(R.id.editTextFilename));
    	mCreateButton = (Button) findViewById(R.id.buttonCreate);

        final Intent intent = getIntent();
        String action = intent.getAction();

        if (action != null && action.equals(Intent.ACTION_INSERT))
        {
        	restore = false;
        	
            mCreateButton.setOnClickListener(new OnClickListener()
            {

    			@Override
    			public void onClick(View v) {
    				
    				
    				String filename =mFilenameTextView.getText().toString();
    				
    			}  	
            });
        	
        }
        else
        {
        	mFilenameTextView.setEnabled(false);        	
        	mCreateButton.setEnabled(false);
        }
        		
        mTimeLineView = (ListView) findViewById(R.id.listView1);
        registerForContextMenu(mTimeLineView);
        mTimeLineView.setOnItemClickListener (new AdapterView.OnItemClickListener() 
        {
        	  @Override
        	  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        		  FileWrapper selectedFile = mTimeLineArrayAdapter.getItem(position);
        		  
        		  if (selectedFile.mIsDirectory)
        		  {
        			  mDirectory = selectedFile.mDirectory;  
        			  reload();
        		  }
        		  else
        		  {
        			  if (!mVideoCastManager.isConnected())
        			  {
	        			  Intent intent = new Intent(StreamListingActivity.this, VideoPlayer.class);
			        		
	        			  intent.setAction(Intent.ACTION_VIEW);
	        			  intent.putExtra("url",selectedFile.mFile.toExternalForm());
	              		
	        			  startActivity(intent);              		
        			  }
        			  else
        			  {
	        			  MediaMetadata metaData = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
	        			  
	        			  MediaInfo mediaInfo = new MediaInfo.Builder(
	        					    selectedFile.mFile.toExternalForm())
	        					    .setContentType("video/mp4")
	        					    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
	        					    .setMetadata(metaData)
	        					    .build();
	        			  
	        			  mVideoCastManager.startCastControllerActivity(StreamListingActivity.this, mediaInfo, 0, true);
        			  }
        		  }
        	  }
        	});
        
        // Initialise the array adapter for the list view.
        
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