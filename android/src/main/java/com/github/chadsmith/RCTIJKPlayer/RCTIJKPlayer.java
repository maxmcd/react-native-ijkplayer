package com.github.chadsmith.RCTIJKPlayer;

import android.os.Handler;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Task;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RCTIJKPlayer extends FrameLayout implements LifecycleEventListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnCompletionListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnBufferingUpdateListener {

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

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";

    public static final String EVENT_PROP_ERROR = "error";
    public static final String EVENT_PROP_WHAT = "what";
    public static final String EVENT_PROP_EXTRA = "extra";

    public static final String EVENT_PROP_BUFFERING_PROG = "progress";

    private RCTEventEmitter mEventEmitter;

    private boolean mPaused = false;
    private boolean mMuted = false;
    private float mVolume = 1.0f;
    private boolean mLoaded = false;
    private boolean mStalled = false;

    private float mProgressUpdateInterval = 250.0f;

    private Handler mProgressUpdateHandler = new Handler();
    private Runnable mProgressUpdateRunnable = null;

    private IjkVideoView ijkVideoView;


    public RCTIJKPlayer(ThemedReactContext themedReactContext) {
        super(themedReactContext);

        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
        themedReactContext.addLifecycleEventListener(this);

        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        ijkVideoView = new IjkVideoView(themedReactContext);

        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (ijkVideoView != null && ijkVideoView.isPlaying() && !mPaused) {
                    WritableMap event = Arguments.createMap();
                    event.putDouble(EVENT_PROP_CURRENT_TIME, ijkVideoView.getCurrentPosition() / 1000.0);
                    event.putDouble(EVENT_PROP_DURATION, ijkVideoView.getDuration() / 1000.0);
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
                    mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, Math.round(mProgressUpdateInterval));
                }
            }
        };

        ijkVideoView.setOnPreparedListener(this);
        ijkVideoView.setOnErrorListener(this);
        ijkVideoView.setOnCompletionListener(this);
        ijkVideoView.setOnInfoListener(this);
        ijkVideoView.setOnBufferingUpdateListener(this);

        addView(ijkVideoView);
    }

    private void releasePlayer() {
        if(ijkVideoView != null)
            Task.callInBackground(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    ijkVideoView.setOnPreparedListener(null);
                    ijkVideoView.setOnErrorListener(null);
                    ijkVideoView.setOnCompletionListener(null);
                    ijkVideoView.setOnInfoListener(null);
                    ijkVideoView.setOnBufferingUpdateListener(null);
                    ijkVideoView.stopPlayback();
                    ijkVideoView = null;
                    return null;
                }

            });
    }

    public void setSrc(final String uriString, final ReadableMap readableMap) {
        if(uriString == null)
            return;

        mLoaded = false;
        mStalled = false;

        WritableMap src = Arguments.createMap();
        src.putString(RCTIJKPlayerManager.PROP_SRC_URI, uriString);
        WritableMap event = Arguments.createMap();
        event.putMap(RCTIJKPlayerManager.PROP_SRC, src);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD_START.toString(), event);

        if(readableMap != null) {
            Map<String, String> headerMap = new HashMap<>();
            ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();
                ReadableType type = readableMap.getType(key);
                switch (type) {
                    case String:
                        headerMap.put(key, readableMap.getString(key));
                        break;
                }
            }
            ijkVideoView.setVideoPath(uriString, headerMap);
        }
        else
            ijkVideoView.setVideoPath(uriString);
    }

    public void setPausedModifier(final boolean paused) {
        mPaused = paused;
        if (ijkVideoView == null) return;
        if (mPaused) {
            if (ijkVideoView.isPlaying()) {
                ijkVideoView.pause();
            }
        } else {
            if (!ijkVideoView.isPlaying()) {
                ijkVideoView.start();
                mProgressUpdateHandler.post(mProgressUpdateRunnable);
            }
        }
    }

    public void setSeekModifier(final double seekTime) {
        if(ijkVideoView != null)
            ijkVideoView.seekTo((int) (seekTime * 1000));
    }

    public void setMutedModifier(final boolean muted) {
        mMuted = muted;
        if (ijkVideoView == null) return;
        if (mMuted) {
            ijkVideoView.setVolume(0, 0);
        } else {
            ijkVideoView.setVolume(mVolume, mVolume);
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
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        WritableMap event = Arguments.createMap();
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), event);
    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int frameworkErr, int implErr) {
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
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                mStalled = true;
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                mStalled = false;
                break;
        }
        return true;
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        if(mLoaded)
            return;

        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_DURATION, ijkVideoView.getDuration() / 1000.0);
        event.putDouble(EVENT_PROP_CURRENT_TIME, ijkVideoView.getCurrentPosition() / 1000.0);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), event);
        mLoaded = true;

        applyModifiers();
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
    public void onHostPause() {
    }

    @Override
    public void onHostResume() {
        applyModifiers();
    }

    @Override
    public void onHostDestroy() {
    }

}
