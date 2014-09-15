/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.me.geekylou.castmywebsite;

import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGD;
import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.sample.castcompanionlibrary.cast.BaseCastManager;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.OnFailedListener;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.sample.castcompanionlibrary.cast.player.IVideoCastController;
import com.google.sample.castcompanionlibrary.cast.player.OnVideoCastControllerListener;
import com.google.sample.castcompanionlibrary.widgets.IMiniController;

import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A compound component that provides a superset of functionalities required for the global access
 * requirement. This component provides an image for the album art, a play/pause button, a seekbar
 * for trick-play with current time and duration and a mute/unmute button. Clients can add this
 * compound component to their layout xml and register that with the instance of
 * {@link VideoCastManager} by using the following pattern:<br/>
 *
 * <pre>
 * mMiniController = (MiniController) findViewById(R.id.miniController1);
 * mCastManager.addMiniController(mMiniController);
 * mMiniController.setOnMiniControllerChangedListener(mCastManager);
 * </pre>
 *
 * Then the {@link VideoCastManager} will manage the behavior, including its state and metadata and
 * interactions.
 */
public class MiniController extends RelativeLayout implements IMiniController {
    private VideoCastManager mCastManager;
	private Handler mHandler;
    private static final String TAG = "MiniController";
    protected ImageView mIcon;
    protected TextView mTitle;
    protected TextView mSubTitle;
    protected ImageView mPlayPause;
    protected ProgressBar mLoading;
    public static final int PLAYBACK = 1;
    public static final int PAUSE = 2;
    public static final int IDLE = 3;
    private com.google.sample.castcompanionlibrary.widgets.MiniController.OnMiniControllerChangedListener mListener;
    private Uri mIconUri;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private View mContainer;
    private int mStreamType = MediaInfo.STREAM_TYPE_BUFFERED;
    private Drawable mStopDrawable;
	private SeekBar mSeekBar;
	private Context mContext;
	private TextView mCurrentTime;
	private Timer mSeekbarTimer;
	private int mPlaybackState;
    /**
     * @param context
     * @param attrs
     */
    public MiniController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.mini_controller, this);
        mPauseDrawable = getResources().getDrawable(R.drawable.ic_mini_controller_pause);
        mPlayDrawable = getResources().getDrawable(R.drawable.ic_mini_controller_play);
        mStopDrawable = getResources().getDrawable(R.drawable.ic_mini_controller_stop);
        loadViews();
        setupCallbacks();
    }

    /**
     * Sets the listener that should be notified when a relevant event is fired from this component.
     * Clients can register the {@link VideoCastManager} instance to be the default listener so it
     * can control the remote media playback.
     *
     * @param listener
     */
	@Override
	public void setOnMiniControllerChangedListener(
			com.google.sample.castcompanionlibrary.widgets.MiniController.OnMiniControllerChangedListener listener) {
        if (null != listener) {
            this.mListener = listener;
        }
    }

    /**
     * Removes the listener that was registered by {@link setOnMiniControllerChangedListener}
     *
     * @param listener
     */
    public void removeOnMiniControllerChangedListener(OnMiniControllerChangedListener listener) {
        if (null != listener && this.mListener == listener) {
            this.mListener = null;
        }
    }

    @Override
    public void setStreamType(int streamType) {
        this.mStreamType = streamType;
    }

    private void setupCallbacks() {

        mPlayPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    setLoadingVisibility(true);
                    try {
                        mListener.onPlayPauseClicked(v);
                    } catch (CastException e) {
                        mListener.onFailed(R.string.failed_perform_action, -1);
                    } catch (TransientNetworkDisconnectionException e) {
                        mListener.onFailed(R.string.failed_no_connection_trans, -1);
                    } catch (NoConnectionException e) {
                        mListener.onFailed(R.string.failed_no_connection, -1);
                    }
                }
            }
        });

        mContainer.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (null != mListener) {
                    setLoadingVisibility(false);
                    try {
                        mListener.onTargetActivityInvoked(mIcon.getContext());
                    } catch (Exception e) {
                        mListener.onFailed(R.string.failed_perform_action, -1);
                    }
                }

            }
        });
        
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mCurrentTime.setText(getPositionString(progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
				try {
					mCastManager.seek(seekBar.getProgress() * 1000);
				} catch (TransientNetworkDisconnectionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoConnectionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}});

        /*
			@Override
			public void onClick(View v) {
				
				
			}});*/
    }

    /**
     * Constructor
     *
     * @param context
     */
    public MiniController(Context context) {
        super(context);
        loadViews();
    }

    @Override
    final public void setIcon(Bitmap bm) {
        mIcon.setImageBitmap(bm);
    }

    @Override
    public void setIcon(Uri uri) {
        if (null != mIconUri && mIconUri.equals(uri)) {
            return;
        }

        mIconUri = uri;
        new Thread(new Runnable() {
            Bitmap bm = null;

            @Override
            public void run() {
                try {
                    URL imgUrl = new URL(mIconUri.toString());
                    bm = BitmapFactory.decodeStream(imgUrl.openStream());
                } catch (Exception e) {
                    LOGE(TAG, "setIcon(): Failed to load the image with url: " +
                            mIconUri + ", using the default one", e);
                    bm = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);
                }
                mIcon.post(new Runnable() {

                    @Override
                    public void run() {
                        setIcon(bm);
                    }
                });

            }
        }).start();
    }

    @Override
    public void setTitle(String title) {
        mTitle.setText(title);
    }

    @Override
    public void setSubTitle(String subTitle) {
        mSubTitle.setText(subTitle);
    }

    @Override
    public void setPlaybackStatus(int state, int idleReason) 
    {
    	mPlaybackState = state;
        switch (state) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(getPauseStopButton());
                setLoadingVisibility(false);
                restartTrickplayTimer();
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                setLoadingVisibility(false);
                stopTrickplayTimer();
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                switch (mStreamType) {
                    case MediaInfo.STREAM_TYPE_BUFFERED:
                        mPlayPause.setVisibility(View.INVISIBLE);
                        setLoadingVisibility(false);
                        break;
                    case MediaInfo.STREAM_TYPE_LIVE:
                        if (idleReason == MediaStatus.IDLE_REASON_CANCELED) {
                            mPlayPause.setVisibility(View.VISIBLE);
                            mPlayPause.setImageDrawable(mPlayDrawable);
                            setLoadingVisibility(false);
                        } else {
                            mPlayPause.setVisibility(View.INVISIBLE);
                            setLoadingVisibility(false);
                        }
                        break;
                }
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                mPlayPause.setVisibility(View.INVISIBLE);
                setLoadingVisibility(true);
                break;
            default:
                mPlayPause.setVisibility(View.INVISIBLE);
                setLoadingVisibility(false);
                stopTrickplayTimer();
                break;
        }
    }

    @Override
    public boolean isVisible() {
        return isShown();
    }

    private void loadViews() 
    {
    	/* TODO move to somewhere more sensible.*/
        mHandler = new Handler();
        mCastManager = CastMyWebsiteApplication.getVideoCastManager(mContext);

        mIcon = (ImageView) findViewById(R.id.iconView);
        mTitle = (TextView) findViewById(R.id.titleView);
        mSubTitle = (TextView) findViewById(R.id.subTitleView);
        mCurrentTime = (TextView) findViewById(R.id.textViewCurrentTime1);
        mPlayPause = (ImageView) findViewById(R.id.playPauseView);
        mLoading = (ProgressBar) findViewById(R.id.loadingView);
        mContainer = findViewById(R.id.bigContainer);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar1);
    }

    private void setLoadingVisibility(boolean show) {
        mLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private Drawable getPauseStopButton() {
        switch (mStreamType) {
            case MediaInfo.STREAM_TYPE_BUFFERED:
                return mPauseDrawable;
            case MediaInfo.STREAM_TYPE_LIVE:
                return mStopDrawable;
            default:
                return mPauseDrawable;
        }
    }

    /**
     * The interface for a listener that will be called when user interacts with the
     * {@link MiniController}, like clicking on the play/pause button, etc.
     */
    public interface OnMiniControllerChangedListener extends OnFailedListener {

        /**
         * Notification that user has clicked on the Play/Pause button
         *
         * @param v
         * @throws TransientNetworkDisconnectionException
         * @throws NoConnectionException
         * @throws CastException
         */
        public void onPlayPauseClicked(View v) throws CastException,
                TransientNetworkDisconnectionException, NoConnectionException;

        /**
         * Notification that the user has clicked on the album art
         *
         * @param context
         * @throws NoConnectionException
         * @throws TransientNetworkDisconnectionException
         */
        public void onTargetActivityInvoked(Context context)
                throws TransientNetworkDisconnectionException, NoConnectionException;

    }

	public void updateSeekbar(int position, int duration) {
		mSeekBar.setProgress(position);
		mSeekBar.setMax(duration);
		mCurrentTime.setText(getPositionString(position));
	}
	
	private String getPositionString(int position)
	{
		int    minutesInt = position / 60;
		int    secondsInt = position % 60;
		
		String secondsString = Integer.toString(secondsInt);
		
		if (secondsInt < 10)
		{
			secondsString = "0" + secondsString; 
		}

		String minString = Integer.toString(minutesInt % 60);

		if (minutesInt >= 60)
		{
			if ((minutesInt % 60) < 10)
				minString = "0" + minString;
			
			return Integer.toString(minutesInt / 60) + ":" + minString + ":" + secondsString;
		}
		else
		{
			return minString + ":" + secondsString;
		}
	}

    private void stopTrickplayTimer() {
        LOGD(TAG, "Stopped TrickPlay Timer");
        if (null != mSeekbarTimer) {
            mSeekbarTimer.cancel();
        }
    }

    private void restartTrickplayTimer() {
        stopTrickplayTimer();
        mSeekbarTimer = new Timer();
        mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), 100, 1000);
        LOGD(TAG, "Restarted TrickPlay Timer");
    }
    
    private class UpdateSeekbarTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {

				@Override
                public void run() {
                    int currentPos = 0;
                    if (mPlaybackState == MediaStatus.PLAYER_STATE_BUFFERING) {
                        return;
                    }
                    if (!mCastManager.isConnected()) {
                        return;
                    }
                    try {
                        int duration = (int) mCastManager.getMediaDuration();
                        if (duration > 0) {
                            try {
                                currentPos = (int) mCastManager.getCurrentMediaPosition();
                                updateSeekbar(currentPos / 1000, duration / 1000);
                            } catch (Exception e) {
                                LOGE(TAG, "Failed to get current media position", e);
                            }
                        }
                    } catch (TransientNetworkDisconnectionException e) {
                        LOGE(TAG, "Failed to update the progress bar due to network issues", e);
                    } catch (NoConnectionException e) {
                        LOGE(TAG, "Failed to update the progress bar due to network issues", e);
                    }

                }
            });
        }
    }

}
