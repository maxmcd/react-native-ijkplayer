package com.github.chadsmith.RCTIJKPlayer;

import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import bolts.Task;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RCTIJKPlayer extends SurfaceView implements LifecycleEventListener, SurfaceHolder.Callback, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnCompletionListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener {

    public enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_STALLED("onVideoBuffer"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_PAUSE("onVideoPause"),
        EVENT_STOP("onVideoStop"),
        EVENT_END("onVideoEnd");
        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    private static final double MIN_PROGRESS_INTERVAL = 0.1;

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";

    public static final String EVENT_PROP_ERROR = "error";
    public static final String EVENT_PROP_WHAT = "what";
    public static final String EVENT_PROP_EXTRA = "extra";

    public static final String EVENT_PROP_BUFFERING_PROG = "progress";

    private ThemedReactContext mThemedReactContext;
    private RCTEventEmitter mEventEmitter;

    private String mSrcUriString = null;
    private ArrayList<Object> mSrcOptions = null;
    private boolean mPaused = false;
    private boolean mMuted = false;
    private float mVolume = 1.0f;
    private boolean mLoaded = false;
    private boolean mStalled = false;
    private double mPrevProgress = 0.0;

    private SurfaceHolder mSurfaceHolder;
    private int mVideoWidth;
    private int mVideoHeight;
    private int rootViewWidth;
    private int rootViewHeight;
    private int orientation;

    private IjkMediaPlayer ijkMediaPlayer;

    private float mProgressUpdateInterval = 250.0f;

    private Handler mProgressUpdateHandler = new Handler();
    private Runnable mProgressUpdateRunnable = null;


    public RCTIJKPlayer(ThemedReactContext themedReactContext) {
        super(themedReactContext);

        mThemedReactContext = themedReactContext;
        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
        themedReactContext.addLifecycleEventListener(this);
        orientation = mThemedReactContext.getResources().getConfiguration().orientation;

        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {

                if (ijkMediaPlayer != null && ijkMediaPlayer.isPlaying() &&!mPaused) {
                    WritableMap event = Arguments.createMap();
                    event.putDouble(EVENT_PROP_CURRENT_TIME, ijkMediaPlayer.getCurrentPosition() / 1000.0);
                    event.putDouble(EVENT_PROP_DURATION, ijkMediaPlayer.getDuration() / 1000.0); //TODO:mBufferUpdateRunnable
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);

                    // Check for update after an interval
                    mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, Math.round(mProgressUpdateInterval));
                }
            }
        };
    }

    private void createPlayer(ArrayList<Object> options) {
        if(ijkMediaPlayer != null)
            return;

        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        ijkMediaPlayer = new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_ERROR);
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-sync",1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change",1);
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"safe",0)
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist ", "ffconcat,file,crypto,async,https");
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user-agent", this.mUserAgent);
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        ijkMediaPlayer.setDisplay(this.getHolder());
        ijkMediaPlayer.setScreenOnWhilePlaying(true);
        ijkMediaPlayer.setLogEnabled(true);
        ijkMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        ijkMediaPlayer.setOnPreparedListener(this);
        ijkMediaPlayer.setOnErrorListener(this);
        ijkMediaPlayer.setOnCompletionListener(this);
        ijkMediaPlayer.setOnInfoListener(this);
        ijkMediaPlayer.setOnBufferingUpdateListener(this);
        ijkMediaPlayer.setOnVideoSizeChangedListener(this);
        ijkMediaPlayer.setOnSeekCompleteListener(this);
    }

    private void releasePlayer() {
        if(ijkMediaPlayer != null)
            Task.callInBackground(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    getHolder().setKeepScreenOn(false);
                    ijkMediaPlayer.setOnPreparedListener(null);
                    ijkMediaPlayer.setOnErrorListener(null);
                    ijkMediaPlayer.setOnCompletionListener(null);
                    ijkMediaPlayer.setOnInfoListener(null);
                    ijkMediaPlayer.setOnBufferingUpdateListener(null);
                    ijkMediaPlayer.setOnVideoSizeChangedListener(null);
                    ijkMediaPlayer.setOnSeekCompleteListener(null);
                    ijkMediaPlayer.stop();
                    ijkMediaPlayer.release();
                    ijkMediaPlayer = null;
                    return null;
                }

            });
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (orientation != newConfig.orientation) {
            int swap = rootViewHeight;
            rootViewHeight = rootViewWidth;
            rootViewWidth = swap;
            orientation = newConfig.orientation;
        }
    }

    public void setSrc(final String uriString, final ArrayList<Object> options) throws IOException {
        if(ijkMediaPlayer != null) {
            ijkMediaPlayer.stop();
            if(options != mSrcOptions)
                releasePlayer();
        }


        if(uriString == null)
            return;

        mSrcUriString = uriString;
        mSrcOptions = options;

        if(mSurfaceHolder == null)
            return;

        createPlayer(options);

        ijkMediaPlayer.setDataSource(uriString, null);

        mLoaded = false;
        mStalled = false;

        WritableMap src = Arguments.createMap();
        src.putString(RCTIJKPlayerManager.PROP_SRC_URI, uriString);
        WritableMap event = Arguments.createMap();
        event.putMap(RCTIJKPlayerManager.PROP_SRC, src);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD_START.toString(), event);

        ijkMediaPlayer.prepareAsync();
    }

    public void setPausedModifier(final boolean paused) {
        mPaused = paused;
        if (ijkMediaPlayer == null || !ijkMediaPlayer.isPlayable()) return;
        if (mPaused) {
            if (ijkMediaPlayer.isPlaying()) {
                ijkMediaPlayer.pause();
            }
        } else {
            if (!ijkMediaPlayer.isPlaying()) {
                ijkMediaPlayer.start();
                mProgressUpdateHandler.post(mProgressUpdateRunnable);
            }
        }
    }

    public void setMutedModifier(final boolean muted) {
        mMuted = muted;
        if (ijkMediaPlayer == null || !ijkMediaPlayer.isPlayable()) return;
        if (mMuted) {
            ijkMediaPlayer.setVolume(0, 0);
        } else {
            ijkMediaPlayer.setVolume(mVolume, mVolume);
        }
    }

    public void applyModifiers() {
        setPausedModifier(mPaused);
        setMutedModifier(mMuted);
    }

    @Override
    protected void onDetachedFromWindow() {
        releasePlayer();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        try {
            setSrc(mSrcUriString, mSrcOptions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        WritableMap event = Arguments.createMap();
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), event);
    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int frameworkErr, int implErr) {
        Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "ON_ERROR: " + frameworkErr + ", " + implErr);
        WritableMap event = Arguments.createMap();
        WritableMap error = Arguments.createMap();
        error.putInt(EVENT_PROP_WHAT, frameworkErr);
        event.putMap(EVENT_PROP_ERROR, error);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
        releasePlayer();
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int message, int val) {
        switch (message) {
            case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_VIDEO_TRACK_LAGGING");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_VIDEO_RENDERING_START");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_BUFFERING_START");
                mStalled = true;
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_BUFFERING_END");
                mStalled = false;
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_NETWORK_BANDWIDTH: " + val);
                break;
            case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_BAD_INTERLEAVING");
                break;
            case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_NOT_SEEKABLE");
                break;
            case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_METADATA_UPDATE");
                break;
            case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_UNSUPPORTED_SUBTITLE");
                break;
            case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_SUBTITLE_TIMED_OUT");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + val);
                break;
            case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                Log.d(RCTIJKPlayer.this.getClass().getSimpleName(), "MEDIA_INFO_AUDIO_RENDERING_START");
                break;
        }
        return true;
    }

    public void onProgress() {

    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        if(mLoaded)
            return;
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_DURATION, ijkMediaPlayer.getDuration() / 1000.0);
        event.putDouble(EVENT_PROP_CURRENT_TIME, ijkMediaPlayer.getCurrentPosition() / 1000.0);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), event);
        mLoaded = true;
        ijkMediaPlayer.start();
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int percent) {
        if(!mStalled)
            return;
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_BUFFERING_PROG, percent / 100);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_STALLED.toString(), event);
    }

    @Override
    public void onSeekComplete(IMediaPlayer iMediaPlayer) {

    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int width, int height, int sarNum, int sarDen) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void onHostPause() {
        if(ijkMediaPlayer != null) {
            ijkMediaPlayer.pause();
        }
    }

    @Override
    public void onHostResume() {
        applyModifiers();
    }

    @Override
    public void onHostDestroy() {
    }

}
