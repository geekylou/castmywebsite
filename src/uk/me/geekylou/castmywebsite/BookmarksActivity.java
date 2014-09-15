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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

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
        
        if (this.getItem(position).mIcon != null) imageViewIcon.setImageBitmap(this.getItem(position).mIcon);
        textFooter.setText(this.getItem(position).mFile.toExternalForm());
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
	private ListView mTimeLineView;
	private ArrayAdapter<BookmarkWrapper> mBookmarkArrayAdapter;
	private String mDirectory = "";
	private boolean restore = true;
	private AsyncTask<Void, Void, FileWrapper[]> mJsonRequest;
	private HashMap<URL,Bitmap> iconCache = new HashMap<URL,Bitmap>();
	private VideoCastManager mVideoCastManager;
	private MiniController mMini;
	private Bookmarks bookmarks;
	private BookmarkWrapper e;
	
	private void reload()
	{
		mBookmarkArrayAdapter.clear();
		mBookmarkArrayAdapter = bookmarks.toArrayAdapter(mBookmarkArrayAdapter);
		mTimeLineView.setAdapter(mBookmarkArrayAdapter);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        BaseCastManager.checkGooglePlayServices(this);

        mVideoCastManager = CastMyWebsiteApplication.getVideoCastManager(this);

        super.onCreate(savedInstanceState);

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
    	mVideoCastManager.setContext(this);
    	mVideoCastManager.incrementUiCounter();
    	reload();
    }

    @Override
    protected void onPause()
    {
    	super.onPause();
    	mVideoCastManager.decrementUiCounter();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_contextmenu, menu);
        
        ListView selectedItem = (ListView)v;
        
        // Get the info on which item was selected
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    }
    
    class OkListener implements View.OnClickListener
    {
    	private final Dialog    mDialog;
    	private boolean         mUpdate;
		private BookmarkWrapper mBookmark;
		
    	    public OkListener(Dialog dialog,BookmarkWrapper bookmark) {
    	        this.mDialog    = dialog;
    	        this.mBookmark = bookmark;
    	        this.mUpdate   = true;
    	    }
    	    public OkListener(Dialog dialog) {
    	        this.mDialog    = dialog;
    	        this.mBookmark = new BookmarkWrapper();
    	        this.mUpdate   = false;
    	    }
    	    
        @Override
        public void onClick(View v) 
        {
			TextView mErrorTextView = ((TextView) mDialog.findViewById(R.id.textViewError));
			try {
				TextView mNameTextView = ((TextView) mDialog.findViewById(R.id.editTextName));
				TextView mURLTextView = ((TextView) mDialog.findViewById(R.id.editTextURL));
		    	
				mBookmark.mFile = new URL(mURLTextView.getText().toString());
				mBookmark.mName = mNameTextView.getText().toString();

				if (mUpdate)
					bookmarks.updateEntry(mBookmark);
				else
					bookmarks.insertEntry(mBookmark);
				reload();
	            mDialog.dismiss();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				mErrorTextView.setText("Invalid URL entered!");
			}
        }
    }
    
    void createHostDialog(BookmarkWrapper bookmark)
    {
    	OkListener listener;
    	LayoutInflater inflater = getLayoutInflater();
    	
		AlertDialog.Builder builder = new AlertDialog.Builder(BookmarksActivity.this);
		builder.setView(inflater.inflate(R.layout.host_dialog, null))
		       .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		       {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					/* Do nothing if cancel is pressed. */
				}
		       }).setPositiveButton("Ok", new DialogInterface.OnClickListener()
		       {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					/* Do nothing if cancel is pressed. */
				}
		       });
		
		if (bookmark == null)
		{
			builder.setTitle("Add New Host");
		}
		else
		{
			builder.setTitle("Edit Host");
		}
		AlertDialog dialog = builder.create();
		dialog.show();
		
		Button theButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (bookmark == null)
		{
			theButton.setOnClickListener(new OkListener(dialog));
		}
		else
		{
			((TextView)dialog.findViewById(R.id.editTextName)).setText(bookmark.mName);
			((TextView)dialog.findViewById(R.id.editTextURL)).setText(bookmark.mFile.toExternalForm());
			theButton.setOnClickListener(new OkListener(dialog,bookmark));
		}
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        		
        switch (item.getItemId()) {
            case R.id.itemEdit:
            {
                BookmarkWrapper entry = (BookmarkWrapper)mTimeLineView.getItemAtPosition(info.position);
            	createHostDialog(entry);
/*            	Intent intent = new Intent(BookmarksActivity.this, HostDialog.class);
        		        		
        		intent.setAction(Intent.ACTION_EDIT);
        		intent.putExtra("id", entry.id);
        		startActivity(intent);*/
            }
            	return true;
            case R.id.itemDelete:
            {
            	final BookmarkWrapper entry = (BookmarkWrapper)mTimeLineView.getItemAtPosition(info.position);
            	
        		AlertDialog.Builder builder = new AlertDialog.Builder(BookmarksActivity.this);
        		builder.setMessage("Are you sure you want to delete "+entry.mName+"?")
        		       .setTitle("Delete Entry").setPositiveButton("Ok", new DialogInterface.OnClickListener()
        		       {
						@Override
						public void onClick(DialogInterface dialog,
								int which) 
						{
							bookmarks.deleteEntry(entry);
							reload();
						}
        		       }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        		       {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							/* Do nothing if cancel is pressed. */
						}
        		       });
        		    
        		AlertDialog dialog = builder.create();
        		dialog.show();

            	return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }
    /**
     * Called when your activity's options menu needs to be created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu); 
        mVideoCastManager = CastMyWebsiteApplication.getVideoCastManager(this);
        
        mVideoCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
       return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.item1:
        	createHostDialog(null);
        	/*
  		  	Intent intent = new Intent(BookmarksActivity.this, HostDialog.class);
			  
  		  	intent.setAction(Intent.ACTION_INSERT);
  		  	startActivity(intent);*/
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    

}
