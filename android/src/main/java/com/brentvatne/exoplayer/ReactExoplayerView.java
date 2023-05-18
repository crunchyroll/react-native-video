package com.brentvatne.exoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.util.DisplayMetrics;
import android.content.Intent;

import com.brentvatne.react.R;
import com.brentvatne.receiver.AudioBecomingNoisyReceiver;
import com.brentvatne.receiver.BecomingNoisyListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.util.RNLog;
import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.drm.MediaDrmCallbackException;
import com.google.android.exoplayer2.drm.DrmSession.DrmSessionException;
import com.brentvatne.exoplayer.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManagerProvider;
import com.google.android.exoplayer2.drm.DrmSessionEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallbackException;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.dash.manifest.Period;
import com.google.android.exoplayer2.source.dash.manifest.AdaptationSet;
import com.google.android.exoplayer2.source.dash.manifest.Representation;
import com.google.android.exoplayer2.source.dash.manifest.Descriptor;

// Mux
import com.mux.stats.sdk.core.model.CustomerData;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.model.CustomerViewData;
import com.mux.stats.sdk.core.model.CustomerViewerData;
import com.mux.stats.sdk.core.model.CustomData;

import com.mux.stats.sdk.muxstats.MuxErrorException;
import com.mux.stats.sdk.muxstats.MuxStatsExoPlayer;
import com.mux.stats.sdk.muxstats.monitorWithMuxData;
// End Mux

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.lang.Thread;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.lang.Integer;
import java.lang.Float;
import java.lang.Double;
import java.lang.reflect.Method;

import com.facebook.react.uimanager.util.ReactFindViewUtil;


import com.brentvatne.exoplayer.PlaybackHandler;

@SuppressLint("ViewConstructor")
public class ReactExoplayerView extends FrameLayout implements
        AdEventListener,
        LifecycleEventListener,
        Player.Listener,
        BandwidthMeter.EventListener,
        BecomingNoisyListener,
        AudioManager.OnAudioFocusChangeListener,
        DrmSessionEventListener,
        PlaybackHandler {

    public static final double DEFAULT_MAX_HEAP_ALLOCATION_PERCENT = 1;
    public static final double DEFAULT_MIN_BACK_BUFFER_MEMORY_RESERVE = 0;
    public static final double DEFAULT_MIN_BUFFER_MEMORY_RESERVE = 0;

    public static final String NATIVE_SOURCE_URI = "native://mediaSource";

    private static final String TAG = "ReactExoplayerView";

    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    private static final int SHOW_PROGRESS = 1;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private final VideoEventEmitter eventEmitter;
    private final ReactExoplayerConfig config;
    private DefaultBandwidthMeter bandwidthMeter;
    private PlayerControlView playerControlView;
    private View playPauseControlContainer;
    private Player.Listener eventListener;
    private MediaSourceEventListener mediaSourceEventListener;

    public ExoPlayerView exoPlayerView;
    public static FrameLayout truexOverlayFrameLayout;
    public ImaAdsLoader adsLoader;
    private AdsLoader googleAdsLoader;
    private TruexAdManager truexAdManager;
    private FrameLayout truexViewGroup;
    private ImaSdkSettings imaSettings;
    private boolean shouldPlayAdBeforeStartPosition;
    private Ad activeAd;
    private ArrayList<Double> adMarkers;
    private boolean isAdsManagerListenerAdded = false;
    private Timeline playerTimeline;

    private DataSource.Factory mediaDataSourceFactory;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private boolean playerNeedsSource;

    private int resumeWindow;
    private long resumePosition;
    private long startPosition;
    private boolean loadVideoStarted;
    private boolean isFullscreen;
    private boolean isInBackground;
    private boolean isPaused;
    private boolean isBuffering;
    private boolean muted = false;
    private boolean hasAudioFocus = false;
    private float rate = 1f;
    private float audioVolume = 1f;
    private int minLoadRetryCount = 3;
    private int maxBitRate = 0;
    private long seekTime = C.TIME_UNSET;
    private boolean hasDrmFailed = false;
    private boolean isUsingContentResolution = false;
    private boolean selectTrackWhenReady = false;
    private boolean limitMaxResolution = false;

    private int minBufferMs = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;
    private int maxBufferMs = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
    private int bufferForPlaybackMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
    private int bufferForPlaybackAfterRebufferMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
    private double maxHeapAllocationPercent = ReactExoplayerView.DEFAULT_MAX_HEAP_ALLOCATION_PERCENT;
    private double minBackBufferMemoryReservePercent = ReactExoplayerView.DEFAULT_MIN_BACK_BUFFER_MEMORY_RESERVE;
    private double minBufferMemoryReservePercent = ReactExoplayerView.DEFAULT_MIN_BUFFER_MEMORY_RESERVE;
    private double enableBackBufferAvailableMemory = -1f;
    private Handler mainHandler;
    public List<Segment> segments;

    // Props from React
    private int backBufferDurationMs = DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS;
    private Uri srcUri;
    private String extension;
    private boolean isCSAIEnabled = false;
    private boolean isTruexEnabled = false;
    private String adTagUrl;
    private boolean repeat;
    private String audioTrackType;
    private Dynamic audioTrackValue;
    private String videoTrackType;
    private Dynamic videoTrackValue;
    private String uiLanguage = "en";
    private String textTrackType;
    private Dynamic textTrackValue;
    private ReadableArray textTracks;
    private boolean disableFocus;
    private boolean disableBuffering;
    private long contentStartTime = -1L;
    private boolean disableDisconnectError;
    private boolean preventsDisplaySleepDuringVideoPlayback = true;
    private float mProgressUpdateInterval = 250.0f;
    private boolean playInBackground = false;
    private Map<String, String> requestHeaders;
    private boolean mReportBandwidth = false;
    private String cdnUrl = null;
    private UUID drmUUID = null;
    private String drmLicenseUrl = null;
    private String[] drmLicenseHeader = null; 
    private boolean controls;
    private boolean disableTruexRateLimit = false;

    // Mux
    private boolean isMuxEnabled = false;
    private ReadableMap muxOptions = null;
    private CustomerData muxCustomerVideoData = null;
    private MuxStatsExoPlayer muxStats = null;

    // \ End props

    // DASH Offline Source
    public static MediaSource offlineMediaSource;
    public static CacheDataSource.Factory offlineCacheDataSourceFactory;

    // React
    private final ThemedReactContext themedReactContext;
    private final AudioManager audioManager;
    private final AudioBecomingNoisyReceiver audioBecomingNoisyReceiver;

    private final Handler progressHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS:
                    if (player != null
                            && player.getPlaybackState() == Player.STATE_READY
                            && player.getPlayWhenReady()
                            ) {
                        if (cdnUrl != null) {
                            eventEmitter.updateCdn(cdnUrl);
                        }
                        long pos = player.getCurrentPosition();
                        long bufferedDuration = player.getBufferedPercentage() * player.getDuration() / 100;
                        long duration = player.getDuration();
                        eventEmitter.progressChanged(pos, bufferedDuration, player.getDuration(), getPositionInFirstPeriodMsForCurrentWindow(pos), duration);
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, Math.round(mProgressUpdateInterval));

                        if (isCSAIEnabled) {
                            WritableMap payload = Arguments.createMap();
                            WritableMap adInfo = getAdInfo();
                            payload.putMap("adInfo", adInfo);
                            eventEmitter.adEvent("AD_INFO_UPDATE", payload);
                        }  
                    }
                    break;
            }
        }
    };

    public double getPositionInFirstPeriodMsForCurrentWindow(long currentPosition) {
        Timeline.Window window = new Timeline.Window();
        if(!player.getCurrentTimeline().isEmpty()) {
            player.getCurrentTimeline().getWindow(player.getCurrentMediaItemIndex(), window);
        }
        return window.windowStartTimeMs + currentPosition;
    }

    public ReactExoplayerView(ThemedReactContext context, ReactExoplayerConfig config) {
        super(context);
        this.themedReactContext = context;
        this.eventEmitter = new VideoEventEmitter(context);
        this.config = config;
        this.bandwidthMeter = config.getBandwidthMeter();
        createViews();

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        themedReactContext.addLifecycleEventListener(this);
        audioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver(themedReactContext);
        this.mediaSourceEventListener = new MediaSourceListener();
        this.segments = new ArrayList<Segment>();
    }


    @Override
    public void setId(int id) {
        super.setId(id);
        eventEmitter.setViewId(id);
    }

    private boolean isUriNativeSource(Uri src) {
        if (src == null) {
            return false;
        }
        return src.toString().equals(ReactExoplayerView.NATIVE_SOURCE_URI);
    }

    private void createViews() {
        clearResumePosition();
        mediaDataSourceFactory = buildDataSourceFactory(true);
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        // ExoPlayer view init
        LayoutParams layoutParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT);
        exoPlayerView = new ExoPlayerView(getContext());
        exoPlayerView.setLayoutParams(layoutParams);
        // Add Exoplayer view
        addView(exoPlayerView, 0, layoutParams);

        mainHandler = new Handler();
    }

    private WritableMap getAdInfo() {
        WritableMap data = Arguments.createMap();
        
        // Get ad markers
        Timeline timeline = player.getCurrentTimeline();
        if (timeline != null) {
            updateAdCuePoints(timeline);
        }
        if (adMarkers != null) {
            WritableArray adMarkersWritableArray = Arguments.createArray();
            for (Double marker : adMarkers) {
                adMarkersWritableArray.pushDouble(marker.doubleValue());
            }
            data.putArray("adMarkers", adMarkersWritableArray);
        }

        if (activeAd == null) {
            data.putString("error", "No activeAd!");
            return data;
        }

        // Get ad based data
        int adPodPosition = 0;
        int adPodIndex = 0;
        int adPodTotalAds = 0;
        double adPodMaxDuration = 0;
        double adDuration = 0;
        if (activeAd != null) {
            AdPodInfo podInfo = activeAd.getAdPodInfo();
            adDuration = activeAd.getDuration();
            if (podInfo != null) {
                adPodIndex = podInfo.getPodIndex();
                adPodTotalAds = podInfo.getTotalAds();
                adPodPosition = podInfo.getAdPosition();
                adPodMaxDuration = podInfo.getMaxDuration();
            }
        }

        data.putInt("adPodIndex", adPodIndex);
        data.putInt("adPodTotalAds", adPodTotalAds);
        data.putInt("adPodPosition", adPodPosition);
        data.putDouble("adPodMaxDuration", adPodMaxDuration);
        data.putDouble("adDuration", adDuration);
        return data;
    }

    /**
     * TrueX Playback Handlers
     */
    @Override
    public void resumeStream() {
        if (player == null) {
            return;
        }

        WritableMap payload = Arguments.createMap();
        eventEmitter.adEvent("ENDED_TRUEX", payload);

        adsLoader.discardAdBreak();
        startPlayback();
    }

    @Override
    public void closeStream() {
        releasePlayer();
    }

    @Override
    public void displayLinearAds() {
        if (this.player == null) {
            return;
        }

        WritableMap payload = Arguments.createMap();
        eventEmitter.adEvent("ENDED_TRUEX", payload);
        adsLoader.skipAd();
        startPlayback();      
    }

    @Override
    public void handlePopup(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        getContext().startActivity(browserIntent);
    }

    /**
     * Display TrueX interactive Ad
     */
    private void displayInteractiveAd(String vastUrl) {
        if (player == null) {
            return;
        }
        WritableMap payload = Arguments.createMap();
        eventEmitter.adEvent("STARTED_TRUEX", payload);

        // Pause the stream and display a true[X] engagement
        Long position = player.getCurrentPosition();
        if (position > 0) resumePosition = position;
        pausePlayback();

        // Start the true[X] engagement
        ViewGroup viewGroup = (ViewGroup) truexOverlayFrameLayout;
        truexAdManager = new TruexAdManager(getContext(), this);
        truexAdManager.startAd(viewGroup, vastUrl, disableTruexRateLimit);
    }

    public void handleCheckTruex(AdEvent event) {
        if (activeAd == null) {
            return;
        }
        boolean isTrueXAd = activeAd.getAdSystem().contains("trueX");
        if (isTrueXAd && isTruexEnabled) {
            String vastUrl = activeAd.getDescription();
            displayInteractiveAd(vastUrl);
        } else if (isTrueXAd && !isTruexEnabled) {
            // Don't display interactive ads if TrueX is disabled
            adsLoader.skipAd();
        }
        reLayout(exoPlayerView);
    }

    @Override
    public void onAdEvent(AdEvent event) {
        if (muxStats != null) {
            // muxStats.getAdsImaSdkListener().onAdEvent(event);
        }
        if (event == null || !isCSAIEnabled) {
            return;
        }
        // Get ad data
        activeAd = event.getAd();
        WritableMap adInfo = getAdInfo();
        
        WritableMap payload = Arguments.createMap();
        payload.putMap("adInfo", adInfo);

        AdEvent.AdEventType eventType = event.getType();
        switch(eventType) {
            case STARTED:
                eventEmitter.adEvent("STARTED", payload);
                handleCheckTruex(event);
                break;
            case CONTENT_RESUME_REQUESTED:
                activeAd = null;
                eventEmitter.adEvent("CONTENT_RESUME_REQUESTED", payload);
                break;
            case TAPPED:
                eventEmitter.videoClickEvent();
                break;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initializePlayer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        /* We want to be able to continue playing audio when switching tabs.
         * Leave this here in case it causes issues.
         */
        // stopPlayback();
    }

    // LifecycleEventListener implementation

    @Override
    public void onHostResume() {
        if (!playInBackground || !isInBackground) {
            setPlayWhenReady(!isPaused);
        }
        isInBackground = false;
    }

    @Override
    public void onHostPause() {
        isInBackground = true;
        if (playInBackground) {
            return;
        }
        setPlayWhenReady(false);
    }

    @Override
    public void onHostDestroy() {
        stopPlayback();
    }

    public void cleanUpResources() {
        stopPlayback();
    }

    //BandwidthMeter.EventListener implementation
    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        if (mReportBandwidth) {
            if (player == null) {
                eventEmitter.bandwidthReport(bitrate, 0, 0, "-1");
            } else {
                Format videoFormat = player.getVideoFormat();
                int width = videoFormat != null ? videoFormat.width : 0;
                int height = videoFormat != null ? videoFormat.height : 0;
                String trackId = videoFormat != null ? videoFormat.id : "-1";
                eventEmitter.bandwidthReport(bitrate, height, width, trackId);
            }
        }
    }

    // Internal methods

    /**
     * Toggling the visibility of the player control view
     */
    private void togglePlayerControlVisibility() {
        if(player == null || playerControlView == null) return;
        reLayout(playerControlView);
        if (playerControlView.isVisible()) {
            playerControlView.hide();
        } else {
            playerControlView.show();
        }
    }

    /**
     * Initializing Player control
     */
    private void initializePlayerControl() {
        if (!controls) {
            return;
        }
        if (playerControlView == null) {
            playerControlView = new PlayerControlView(getContext());
        }

        // Setting the player for the playerControlView
        playerControlView.setPlayer(player);
        playerControlView.show();
        playPauseControlContainer = playerControlView.findViewById(R.id.exo_play_pause_container);

        // Invoking onClick event for exoplayerView
        exoPlayerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayerControlVisibility();
            }
        });

        //Handling the playButton click event
        ImageButton playButton = playerControlView.findViewById(R.id.exo_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.getPlaybackState() == Player.STATE_ENDED) {
                    player.seekTo(0);
                }
                setPausedModifier(false);
            }
        });

        //Handling the pauseButton click event
        ImageButton pauseButton = playerControlView.findViewById(R.id.exo_pause);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPausedModifier(true);
            }
        });

        // Invoking onPlaybackStateChanged and onPlayWhenReadyChanged events for Player
        eventListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                reLayout(playPauseControlContainer);
                //Remove this eventListener once its executed. since UI will work fine once after the reLayout is done
                player.removeListener(eventListener);
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                reLayout(playPauseControlContainer);
                //Remove this eventListener once its executed. since UI will work fine once after the reLayout is done
                player.removeListener(eventListener);
            }
        };
        player.addListener(eventListener);
    }

    /**
     * Adding Player control to the frame layout
     */
    private void addPlayerControl() {
        if(player == null || playerControlView == null) return;
        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        playerControlView.setLayoutParams(layoutParams);
        int indexOfPC = indexOfChild(playerControlView);
        if (indexOfPC != -1) {
            removeViewAt(indexOfPC);
        }
        addView(playerControlView, 1, layoutParams);
    }

    /**
     * Update the layout
     * @param view  view needs to update layout
     *
     * This is a workaround for the open bug in react-native: https://github.com/facebook/react-native/issues/17968
     */
    private void reLayout(View view) {
        if (view == null) return;
        view.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
        view.layout(view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    private class RNVLoadControl extends DefaultLoadControl {
        private int availableHeapInBytes = 0;
        private Runtime runtime;
        public RNVLoadControl(DefaultAllocator allocator, int minBufferMs, int maxBufferMs, int bufferForPlaybackMs, int bufferForPlaybackAfterRebufferMs, int targetBufferBytes, boolean prioritizeTimeOverSizeThresholds, int backBufferDurationMs, boolean retainBackBufferFromKeyframe) {
            super(allocator,
                    minBufferMs,
                    maxBufferMs,
                    bufferForPlaybackMs,
                    bufferForPlaybackAfterRebufferMs,
                    targetBufferBytes,
                    prioritizeTimeOverSizeThresholds,
                    backBufferDurationMs,
                    retainBackBufferFromKeyframe);
            runtime = Runtime.getRuntime();
            ActivityManager activityManager = (ActivityManager) themedReactContext.getSystemService(themedReactContext.ACTIVITY_SERVICE);
            availableHeapInBytes = (int) Math.floor(activityManager.getMemoryClass() * maxHeapAllocationPercent * 1024 * 1024);
        }

        @Override
        public boolean shouldContinueLoading(long playbackPositionUs, long bufferedDurationUs, float playbackSpeed) {
            if (ReactExoplayerView.this.disableBuffering) {
                return false;
            }
            int loadedBytes = getAllocator().getTotalBytesAllocated();
            boolean isHeapReached = availableHeapInBytes > 0 && loadedBytes >= availableHeapInBytes;
            if (isHeapReached) {
                return false;
            }
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long freeMemory = runtime.maxMemory() - usedMemory;
            long reserveMemory = (long)minBufferMemoryReservePercent * runtime.maxMemory();
            long bufferedMs = bufferedDurationUs / (long)1000;
            if (reserveMemory > freeMemory && bufferedMs > 2000) {
                // We don't have enough memory in reserve so we stop buffering to allow other components to use it instead
                return false;
            }
            if (runtime.freeMemory() == 0) {
                Log.w("ExoPlayer Warning", "Free memory reached 0, forcing garbage collection");
                runtime.gc();
                return false;
            }
            return super.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed);
        }
    }

    /**
     * class that holds the data of a segment loaded
     */
    private final class Segment {
        public long mediaStartTimeMs;
        public long mediaEndTimeMs;
        public Uri uri;
        Segment(long mediaEndTimeMs, long mediaStartTimeMs, Uri uri){
            this.mediaEndTimeMs = mediaEndTimeMs;
            this.mediaStartTimeMs = mediaStartTimeMs;
            this.uri = uri;
        }
    }

    /**
     * class that listens the events of segments being loaded
     */
    private final class MediaSourceListener implements MediaSourceEventListener {
        @Override
        public void onLoadCompleted​(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            segments.add(new Segment(mediaLoadData.mediaEndTimeMs, mediaLoadData.mediaStartTimeMs, loadEventInfo.uri));
            if (loadEventInfo.uri != null) {
                cdnUrl = loadEventInfo.uri.toString();
            }
            removePassedSegments();
        }

        @Override
        public void onLoadError​(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
            eventEmitter.error("Failed loading a segment", error, "20001", loadEventInfo.uri.toString());
            Log.e("ExoPlayer Exception", "Failed loading a segment" + error.toString());
            segments.add(new Segment(mediaLoadData.mediaEndTimeMs, mediaLoadData.mediaStartTimeMs, loadEventInfo.uri));
            removePassedSegments();
        }
    }

    private void startBufferCheckTimer() {
        Player player = this.player;
        VideoEventEmitter eventEmitter = this.eventEmitter;
        Handler mainHandler = this.mainHandler;

    }

    private void initializePlayer() {
        ReactExoplayerView self = this;
        Activity activity = themedReactContext.getCurrentActivity();
        if (activity == null) {
            // if there is no activity, the player is most likely getting destroyed, in that case we don't need to initialize it
            return;
        }
        // This ensures all props have been settled, to avoid async racing conditions.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (player == null) {
                        // Initialize core configuration and listeners
                        initializePlayerCore(self);
                    }
                    if (playerNeedsSource && srcUri != null) {
                        self.initializeMuxData(self);
                        // Init Ads Loader
                        if (self.isCSAIEnabled) {
                            imaSettings = ImaSdkFactory.getInstance().createImaSdkSettings();
                            imaSettings.setLanguage(uiLanguage);
                            adsLoader = new ImaAdsLoader.Builder(getContext())
                                .setAdEventListener(self)
                                // .setAdErrorListener(self.isMuxEnabled ? self.muxStats.getAdsImaSdkListener() : null)
                                .setImaSdkSettings(imaSettings)
                                .setPlayAdBeforeStartPosition(shouldPlayAdBeforeStartPosition)
                                .build();
                        }
                        exoPlayerView.invalidateAspectRatio();
                        // DRM session manager creation must be done on a different thread to prevent crashes so we start a new thread
                        ExecutorService es = Executors.newSingleThreadExecutor();
                        es.execute(new Runnable() {
                            ExecutorService parentEs = es;
                            @Override
                            public void run() {
                                // DRM initialization must run on a different thread
                                DrmSessionManager drmSessionManager = initializePlayerDrm(self);
                                if (drmSessionManager == null && self.drmUUID != null) {
                                    // Failed to intialize DRM session manager - cannot continue
                                    Log.e("ExoPlayer Exception", "Failed to initialize DRM Session Manager Framework!");
                                    eventEmitter.error("Failed to initialize DRM Session Manager Framework!", new Exception("DRM Session Manager Framework failure!"), "3003");
                                    return;
                                }

                                // Initialize handler to run on the main thread
                                activity.runOnUiThread(new Runnable() {
                                    ExecutorService es = parentEs;
                                    public void run() {
                                        try {
                                            // Source initialization must run on the main thread
                                            initializePlayerSource(self, drmSessionManager);
                                        } catch (Exception ex) {
                                            self.playerNeedsSource = true;
                                            Log.e("ExoPlayer Exception", "Failed to initialize Player!");
                                            Log.e("ExoPlayer Exception", ex.toString());
                                            self.eventEmitter.error(ex.toString(), ex, "1001");
                                        }
                                        es.shutdown();
                                    }
                                });
                            }
                        });
                    } else if (srcUri != null) {
                        initializePlayerSource(self, null);
                    }
                } catch (Exception ex) {
                    self.playerNeedsSource = true;
                    Log.e("ExoPlayer Exception", "Failed to initialize Player!");
                    Log.e("ExoPlayer Exception", ex.toString());
                    eventEmitter.error(ex.toString(), ex, "1001");
                }
            }
        }, 1);
    }

    private String getUriOfCurrentSegment() {
        long currentPosition = player.getCurrentPosition();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            if (segment.mediaStartTimeMs < currentPosition && currentPosition < segment.mediaEndTimeMs) {
                return segment.uri.toString();
            }
        }
        return "";
    }

    private void removePassedSegments() {
        if (player == null) {
            // Cannot remove segment if player is already destroyed
            return;
        }
        long currentPosition = player.getCurrentPosition();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            if (segment.mediaEndTimeMs < currentPosition) {
                segments.remove(i);
            }
        }
    }

    private void initializePlayerCore(ReactExoplayerView self) {

        ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        self.trackSelector = new DefaultTrackSelector(getContext(), videoTrackSelectionFactory);
        self.trackSelector.setParameters(trackSelector.buildUponParameters()
                .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long freeMemory = runtime.maxMemory() - usedMemory;
        boolean hasNativeSource = isUriNativeSource(self.srcUri);

        int backBufferMs = backBufferDurationMs;
        if (freeMemory < (self.enableBackBufferAvailableMemory * 1000 * 1000)) {
            Log.w("LoadControl", "Available memory is less than required to enable back buffer, setting to 0ms!");
            backBufferMs = 0;
        }

        DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        RNVLoadControl loadControl = new RNVLoadControl(
                allocator,
                minBufferMs,
                maxBufferMs,
                bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs,
                -1,
                true,
                backBufferMs,
                DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME
        );
        DefaultRenderersFactory renderersFactory =
                new DefaultRenderersFactory(getContext())
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
        if (hasNativeSource) {
            player = new ExoPlayer.Builder(getContext(), renderersFactory)
                    .setTrackSelector(self.trackSelector)
                    .setLoadControl(loadControl)
                    .build();
        } else {
            if (self.bandwidthMeter == null) {
                self.bandwidthMeter = self.config.getBandwidthMeter();
            }
            player = new ExoPlayer.Builder(getContext(), renderersFactory)
                    .setBandwidthMeter(self.bandwidthMeter)
                    .setTrackSelector(self.trackSelector)
                    .setLoadControl(loadControl)
                    .build();
        }

        player.addListener(self);
        exoPlayerView.setPlayer(player);
        audioBecomingNoisyReceiver.setListener(self);
        setPlayWhenReady(!isPaused);
        playerNeedsSource = true;

        PlaybackParameters params = new PlaybackParameters(rate, 1f);
        player.setPlaybackParameters(params);
    }

    private DrmSessionManager initializePlayerDrm(ReactExoplayerView self) {
        DrmSessionManager drmSessionManager = null;
        if (self.drmUUID != null) {
            try {
                drmSessionManager = self.buildDrmSessionManager(self.drmUUID, self.drmLicenseUrl,
                        self.drmLicenseHeader);
            } catch (UnsupportedDrmException e) {
                int errorStringId = Util.SDK_INT < 18 ? R.string.error_drm_not_supported
                        : (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                        ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
                eventEmitter.error(getResources().getString(errorStringId), e, "3003");
                return null;
            }
        }
        return drmSessionManager;
    }

    private void initializeMuxData(ReactExoplayerView self) {
        if (!self.isMuxEnabled || self.muxOptions == null) { return; }

        // Player data
        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        customerPlayerData.setPlayerName(self.muxOptions.getString("player_name"));
        customerPlayerData.setPlayerVersion(self.muxOptions.getString("player_version"));
        if (self.muxOptions.getString("player_init_time") != null) {
            Long initTime = Long.parseLong(self.muxOptions.getString("player_init_time"));
            customerPlayerData.setPlayerInitTime(initTime);
        }
        
        // Video data
        CustomerVideoData customerVideoData = new CustomerVideoData();
        customerVideoData.setVideoTitle(self.muxOptions.getString("video_title"));
        customerVideoData.setVideoId(self.muxOptions.getString("video_id"));
        customerVideoData.setVideoSeries(self.muxOptions.getString("video_series"));
        if (self.muxOptions.getString("video_duration") != null) {
            Long videoDuration = Long.parseLong(self.muxOptions.getString("video_duration"));
            customerVideoData.setVideoDuration(videoDuration);
        }
        customerVideoData.setVideoStreamType(self.muxOptions.getString("video_stream_type"));
        customerVideoData.setVideoCdn(self.muxOptions.getString("video_cdn"));
        // customerVideoData.setExperimentName(self.muxOptions.getString("experiment_name"));
        // customerVideoData.setSubPropertyId(self.muxOptions.getString("sub_property_id"));

        if (self.srcUri != null) {
            customerVideoData.setVideoSourceUrl(self.srcUri.toString());
        }

        // View data
        CustomerViewData customerViewData = new CustomerViewData();
        customerViewData.setViewDrmType(self.muxOptions.getString("video_drm_type"));
        customerViewData.setViewSessionId(UUID.randomUUID().toString());

        // Viewer data
        CustomerViewerData customerViewerData = new CustomerViewerData();
        // customerViewerData.setViewerUserId(self.muxOptions.getString("viewer_user_id"));

        // Custom data
        CustomData customData = new CustomData();

        self.muxCustomerVideoData = new CustomerData(
            customerPlayerData,
            customerVideoData,
            customerViewData,
            customerViewerData,
            customData
        );

        // Initialize Mux stats
        self.muxStats = self.exoPlayer.monitorWithMuxData(
            self.themedReactContext,
            self.muxOptions.getString("env_key"),
            self.exoPlayerView,
            self.muxCustomerVideoData
        );
        /*self.muxStats = new MuxStatsExoPlayer(
            self.themedReactContext,
            self.muxOptions.getString("env_key"),
            self.player,
            self.muxCustomerVideoData
        );*/
        // self.muxStats.setPlayerView(self.exoPlayerView);
        self.muxStats.enableMuxCoreDebug(true, false);
    }

    private void initializePlayerSource(ReactExoplayerView self, DrmSessionManager drmSessionManager) {
        ArrayList<MediaSource> mediaSourceList = buildTextSources();
        MediaSource videoSource = buildMediaSource(self.srcUri, self.extension, drmSessionManager);

        videoSource.addEventListener(this.mainHandler, this.mediaSourceEventListener);

        MediaSource mediaSource;
        if (mediaSourceList.size() == 0) {
            mediaSource = videoSource;
        } else {
            mediaSourceList.add(0, videoSource);
            MediaSource[] textSourceArray = mediaSourceList.toArray(
                    new MediaSource[mediaSourceList.size()]
            );
            mediaSource = new MergingMediaSource(textSourceArray);
        }

        // wait for player to be set
        while (player == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                Log.e("ExoPlayer Exception", ex.toString());
            }
        }

        boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
        if (haveResumePosition) {
            player.seekTo(resumeWindow, resumePosition);
        }
        if (startPosition > 0) {
            player.seekTo(0, startPosition);
            haveResumePosition = true;
        }
        player.prepare(mediaSource, !haveResumePosition, false);
        playerNeedsSource = false;

        if (self.isCSAIEnabled) {
            adsLoader.setPlayer(player);
        }

        reLayout(exoPlayerView);
        eventEmitter.loadStart();
        loadVideoStarted = true;

        finishPlayerInitialization();
    }

    private void finishPlayerInitialization() {
        // Initializing the playerControlView
        initializePlayerControl();
        setControls(controls);
        applyModifiers();
        startBufferCheckTimer();
    }

    private DrmSessionManager buildDrmSessionManager(UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray) throws UnsupportedDrmException {
        return buildDrmSessionManager(uuid, licenseUrl, keyRequestPropertiesArray, 0);
    }

    private DrmSessionManager buildDrmSessionManager(UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray, int retryCount) throws UnsupportedDrmException {
        if (Util.SDK_INT < 18) {
            return null;
        }
        try {
            HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
                    buildHttpDataSourceFactory(false));
            if (keyRequestPropertiesArray != null) {
                for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                    drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i], keyRequestPropertiesArray[i + 1]);
                }
            }
            FrameworkMediaDrm mediaDrm = FrameworkMediaDrm.newInstance(uuid);
            if (hasDrmFailed) {
                // When DRM fails using L1 we want to switch to L3
                mediaDrm.setPropertyString("securityLevel", "L3");
            }
            return new DefaultDrmSessionManager(uuid, mediaDrm, drmCallback, null, false, 3);
        } catch(UnsupportedDrmException ex) {
            // Unsupported DRM exceptions are handled by the calling method
            throw ex;
        } catch (Exception ex) {
            if (retryCount < 3) {
                // Attempt retry 3 times in case where the OS Media DRM Framework fails for whatever reason
                return buildDrmSessionManager(uuid, licenseUrl, keyRequestPropertiesArray, ++retryCount);
            }
            // Handle the unknow exception and emit to JS
            eventEmitter.error(ex.toString(), ex, "3006");
            return null;
        }
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension, DrmSessionManager drmSessionManager) {
        if (uri == null) {
            throw new IllegalStateException("Invalid video uri");
        }
        if (isUriNativeSource(uri)) {
            if (ReactExoplayerView.offlineMediaSource == null) {
                throw new IllegalStateException("Invalid native source");
            }
            return ReactExoplayerView.offlineMediaSource;
        }
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        config.setDisableDisconnectError(this.disableDisconnectError);

        MediaItem mediaItem;
        if (isCSAIEnabled) {
            // CSAI ad tag uri
            Uri adTagUri = Uri.parse(adTagUrl);
            mediaItem = new MediaItem.Builder()
                .setUri(uri)
                .setAdsConfiguration(new MediaItem.AdsConfiguration.Builder(adTagUri).build())
                .build();
        } else {
            mediaItem = new MediaItem.Builder().setUri(uri).build();
        }
        DrmSessionManagerProvider drmProvider = null;
        if (drmSessionManager != null) {
            drmProvider = new DrmSessionManagerProvider() {
                @Override
                public DrmSessionManager get(MediaItem mediaItem) {
                    return drmSessionManager;
                }
            };
        } else {
            drmProvider = new DefaultDrmSessionManagerProvider();
        }
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false)
                ).setDrmSessionManagerProvider(drmProvider)
                 .setLoadErrorHandlingPolicy(
                        config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                ).createMediaSource(mediaItem);
            case C.TYPE_DASH:
                if(isCSAIEnabled) {
                    return new DefaultMediaSourceFactory(mediaDataSourceFactory)
                        .setDataSourceFactory(buildDataSourceFactory(false))
                        .setDrmSessionManagerProvider(drmProvider)
                        .setLoadErrorHandlingPolicy(
                            config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                        )
                        .setAdsLoaderProvider(unusedAdTagUri -> adsLoader)
                        .setAdViewProvider(exoPlayerView)
                        .createMediaSource(mediaItem);
                }
            
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false)
                ).setDrmSessionManagerProvider(drmProvider)
                 .setLoadErrorHandlingPolicy(
                        config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                ).createMediaSource(mediaItem);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(
                        mediaDataSourceFactory
                ).setDrmSessionManagerProvider(drmProvider)
                 .setLoadErrorHandlingPolicy(
                        config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                ).createMediaSource(mediaItem);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(
                        mediaDataSourceFactory
                ).setDrmSessionManagerProvider(drmProvider)
                 .setLoadErrorHandlingPolicy(
                        config.buildLoadErrorHandlingPolicy(minLoadRetryCount)
                ).createMediaSource(mediaItem);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private ArrayList<MediaSource> buildTextSources() {
        ArrayList<MediaSource> textSources = new ArrayList<>();
        if (textTracks == null) {
            return textSources;
        }

        for (int i = 0; i < textTracks.size(); ++i) {
            ReadableMap textTrack = textTracks.getMap(i);
            String language = textTrack.getString("language");
            String title = textTrack.hasKey("title")
                    ? textTrack.getString("title") : language + " " + i;
            Uri uri = Uri.parse(textTrack.getString("uri"));
            MediaSource textSource = buildTextSource(title, uri, textTrack.getString("type"),
                    language);
            if (textSource != null) {
                textSources.add(textSource);
            }
        }
        return textSources;
    }

    private MediaSource buildTextSource(String title, Uri uri, String mimeType, String language) {
        MediaItem.SubtitleConfiguration subtitleConfiguration = new MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mimeType)
                .setLanguage(language)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                .setLabel(title)
                .build();
        return new SingleSampleMediaSource.Factory(mediaDataSourceFactory)
                .createMediaSource(subtitleConfiguration, C.TIME_UNSET);
    }

    private void releasePlayer() {
        if (adsLoader != null) {
            adsLoader.setPlayer(null);
            adsLoader.release();
            adsLoader = null;
            imaSettings = null;
            shouldPlayAdBeforeStartPosition = true;
            truexOverlayFrameLayout = null;
        }
        if (player != null) {
            updateResumePosition();
            player.setPlayWhenReady(false);
            player.stop(true);
            player.seekTo(0);
            player.release();
            player.removeListener(this);
            trackSelector = null;
            player = null;
            exoPlayerView.setPlayer(null);
            if (playerControlView != null) {
                playerControlView.setPlayer(null);
            }
            if (muxStats != null) {
                muxStats.release();
            }
        }
        segments.clear();
        progressHandler.removeMessages(SHOW_PROGRESS);
        themedReactContext.removeLifecycleEventListener(this);
        audioBecomingNoisyReceiver.removeListener();
        if (bandwidthMeter != null) {
            bandwidthMeter.removeEventListener(this);
            bandwidthMeter = null;
        }
        Runtime runtime = Runtime.getRuntime();
        if (runtime != null) {
            runtime.gc();
        }
    }

    private boolean requestAudioFocus() {
        if (disableFocus || srcUri == null || this.hasAudioFocus) {
            return true;
        }
        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void setPlayWhenReady(boolean playWhenReady) {
        if (player == null) {
            return;
        }

        if (playWhenReady) {
            this.hasAudioFocus = requestAudioFocus();
            if (this.hasAudioFocus) {
                player.setPlayWhenReady(true);
            }
        } else {
            player.setPlayWhenReady(false);
        }
    }

    public void startPlayback() {
        if (player != null) {
            switch (player.getPlaybackState()) {
                case Player.STATE_IDLE:
                case Player.STATE_ENDED:
                    if (isUriNativeSource(this.srcUri)) {
                        // Native source does not need to be initialized again
                        break;
                    }
                    initializePlayer();
                    break;
                case Player.STATE_BUFFERING:
                case Player.STATE_READY:
                    if (!player.getPlayWhenReady()) {
                        setPlayWhenReady(true);
                    }
                    break;
                default:
                    break;
            }
        } else {
            initializePlayer();
        }
        if (!disableFocus) {
            setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
        }
    }

    private void pausePlayback() {
        if (player != null) {
            if (player.getPlayWhenReady()) {
                setPlayWhenReady(false);
            }
        }
        setKeepScreenOn(false);
    }

    private void stopPlayback() {
        onStopPlayback();
        releasePlayer();
    }

    private void onStopPlayback() {
        if (isFullscreen) {
            setFullscreen(false);
        }
        audioManager.abandonAudioFocus(this);
    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentMediaItemIndex();
        resumePosition = player.isCurrentMediaItemSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return DataSourceUtil.getDefaultDataSourceFactory(this.themedReactContext,
                useBandwidthMeter ? bandwidthMeter : null, requestHeaders);
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return DataSourceUtil.getDefaultHttpDataSourceFactory(this.themedReactContext, useBandwidthMeter ? bandwidthMeter : null, requestHeaders);
    }


    // AudioManager.OnAudioFocusChangeListener implementation

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                this.hasAudioFocus = false;
                eventEmitter.audioFocusChanged(false);
                pausePlayback();
                audioManager.abandonAudioFocus(this);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                eventEmitter.audioFocusChanged(false);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                this.hasAudioFocus = true;
                eventEmitter.audioFocusChanged(true);
                break;
            default:
                break;
        }

        if (player != null) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
                if (!muted) {
                    player.setVolume(audioVolume * 0.8f);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Raise it back to normal
                if (!muted) {
                    player.setVolume(audioVolume * 1);
                }
            }
        }
    }

    // AudioBecomingNoisyListener implementation

    @Override
    public void onAudioBecomingNoisy() {
        eventEmitter.audioBecomingNoisy();
    }

    // Player.Listener implementation

    @Override
    public void onIsLoadingChanged(boolean isLoading) {
        // Do nothing.
    }

    @Override
    public void onEvents(Player player, Player.Events events) {
        if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
            int playbackState = player.getPlaybackState();
            boolean playWhenReady = player.getPlayWhenReady();
            String text = "onStateChanged: playWhenReady=" + playWhenReady + ", playbackState=";
            switch (playbackState) {
                case Player.STATE_IDLE:
                    text += "idle";
                    eventEmitter.idle();
                    clearProgressMessageHandler();
                    if (!player.getPlayWhenReady()) {
                        setKeepScreenOn(false);
                    }
                    break;
            case Player.STATE_BUFFERING:
                text += "buffering";
                onBuffering(true);
                clearProgressMessageHandler();
                setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
                break;
            case Player.STATE_READY:
                text += "ready";
                eventEmitter.ready();
                onBuffering(false);
                startProgressHandler();
                videoLoaded();
                if (selectTrackWhenReady && isUsingContentResolution) {
                    selectTrackWhenReady = false;
                    setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
                }
                // Setting the visibility for the playerControlView
                if (playerControlView != null) {
                    playerControlView.show();
                }
                setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
                reLayout(exoPlayerView);
                break;
            case Player.STATE_ENDED:
                text += "ended";
                eventEmitter.end();
                onStopPlayback();
                setKeepScreenOn(false);
                break;
            default:
                text += "unknown";
                break;
            }
        }
    }

    private void startProgressHandler() {
        progressHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    /*
        The progress message handler will duplicate recursions of the onProgressMessage handler
        on change of player state from any state to STATE_READY with playWhenReady is true (when
        the video is not paused). This clears all existing messages.
     */
    private void clearProgressMessageHandler() {
         progressHandler.removeMessages(SHOW_PROGRESS);
    }

    private void videoLoaded() {
        if (loadVideoStarted) {
            loadVideoStarted = false;
            setSelectedAudioTrack(audioTrackType, audioTrackValue);
            setSelectedVideoTrack(videoTrackType, videoTrackValue);
            setSelectedTextTrack(textTrackType, textTrackValue);
            Format videoFormat = player.getVideoFormat();
            int width = videoFormat != null ? videoFormat.width : 0;
            int height = videoFormat != null ? videoFormat.height : 0;
            long bitrate = 0L;
            if (videoFormat != null) {
                bitrate = videoFormat.bitrate == Format.NO_VALUE ? 0L : videoFormat.bitrate;
            }
            String trackId = videoFormat != null ? videoFormat.id : "-1";

            if (isUriNativeSource(this.srcUri) && mReportBandwidth) {
                // Offline playback has only one video track so we report bandwidth only once without initializing bandwidth meter
                eventEmitter.bandwidthReport(bitrate, height, width, trackId);
            }

            // Properties that must be accessed on the main thread
            long duration = player.getDuration();
            long currentPosition = player.getCurrentPosition();
            WritableArray audioTrackInfo = getAudioTrackInfo();
            WritableArray textTrackInfo = getTextTrackInfo();
            int trackRendererIndex = getTrackRendererIndex(C.TRACK_TYPE_VIDEO);

            ExecutorService es = Executors.newSingleThreadExecutor();
            es.execute(new Runnable() {
                ExecutorService parentEs = es;
                @Override
                public void run() {
                    // To prevent ANRs caused by getVideoTrackInfo we run this on a different thread and notify the player only when we're done
                    eventEmitter.load(duration, currentPosition, width, height,
                        audioTrackInfo, textTrackInfo, getVideoTrackInfo(trackRendererIndex), trackId);
                    parentEs.shutdown();
                }
            });
        }
    }

    private WritableArray getAudioTrackInfo() {
        WritableArray audioTracks = Arguments.createArray();

        if (trackSelector == null) {
            // Likely player is unmounting so no audio tracks are available anymore
            return audioTracks;
        }

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(C.TRACK_TYPE_AUDIO);
        if (info == null || index == C.INDEX_UNSET) {
            return audioTracks;
        }

        TrackGroupArray groups = info.getTrackGroups(index);
        for (int i = 0; i < groups.length; ++i) {
            Format format = groups.get(i).getFormat(0);
            WritableMap audioTrack = Arguments.createMap();
            audioTrack.putInt("index", i);
            audioTrack.putString("title", format.id != null ? format.id : "");
            audioTrack.putString("type", format.sampleMimeType);
            audioTrack.putString("language", format.language != null ? format.language : "");
            audioTrack.putString("bitrate", format.bitrate == Format.NO_VALUE ? ""
                                    : String.format(Locale.US, "%.2fMbps", format.bitrate / 1000000f));
            audioTracks.pushMap(audioTrack);
        }
        return audioTracks;
    }
    private WritableArray getVideoTrackInfo(int trackRendererIndex) {

        if (this.contentStartTime != -1L) {
            WritableArray contentVideoTracks = this.getVideoTrackInfoFromManifest();
            if (contentVideoTracks != null) {
                isUsingContentResolution = true;
                return contentVideoTracks;
            }
        }

        WritableArray videoTracks = Arguments.createArray();
        if (trackSelector == null) {
            // player is unmounting so no video tracks are available anymore
            return videoTracks;
        }

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();

        if (info == null || trackRendererIndex == C.INDEX_UNSET) {
            return videoTracks;
        }

        TrackGroupArray groups = info.getTrackGroups(trackRendererIndex);
        for (int i = 0; i < groups.length; ++i) {
            TrackGroup group = groups.get(i);

            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);

                int shortestFormatSide = format.height < format.width ? format.height : format.width;
                int shortestScreenSize = this.getScreenShortestSide(this.themedReactContext);
                int longestFormatSide = format.height > format.width ? format.height : format.width;
                int longestScreenSize = this.getScreenLongestSide(this.themedReactContext);
                
                if (this.limitMaxResolution && (shortestFormatSide > shortestScreenSize && longestFormatSide > longestScreenSize)) {
                    // This video track is larger than screen resolution so we do not include it in the list of video tracks
                    continue;
                }
                if (isFormatSupported(format)) {
                    WritableMap videoTrack = Arguments.createMap();
                    videoTrack.putInt("width", format.width == Format.NO_VALUE ? 0 : format.width);
                    videoTrack.putInt("height",format.height == Format.NO_VALUE ? 0 : format.height);
                    videoTrack.putInt("bitrate", format.bitrate == Format.NO_VALUE ? 0 : format.bitrate);
                    videoTrack.putString("codecs", format.codecs != null ? format.codecs : "");
                    videoTrack.putString("trackId", format.id == null ? String.valueOf(trackIndex) : format.id);
                    videoTracks.pushMap(videoTrack);
                }
            }
        }

        return videoTracks;
    }

    private WritableMap getScreenDimensions(ThemedReactContext context) {
        if (
          context == null ||
          context.getCurrentActivity() == null ||
          context.getCurrentActivity().getWindowManager() == null
        ) {
            // No context, activity or window manager, we'll return the max int
            WritableMap unknownDimensions = Arguments.createMap();
            unknownDimensions.putInt("width", 2147483647);
            unknownDimensions.putInt("height", 2147483647);
            return unknownDimensions;
        }
        Display display = context.getCurrentActivity().getWindowManager().getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17){
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            //Reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);

            } catch (Exception e) {
                //This may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }

        } else {
            //This should be close, as lower API devices should not have window navigation bars
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }
        WritableMap dimensions = Arguments.createMap();
        dimensions.putInt("width", realWidth);
        dimensions.putInt("height", realHeight);
        return dimensions;
    }

    private int getScreenShortestSide(ThemedReactContext context) {
        WritableMap dimensions = getScreenDimensions(context);

        int realWidth = dimensions.getInt("width");
        int realHeight = dimensions.getInt("height");

        return realHeight < realWidth ? realHeight : realWidth;
    }

    private int getScreenLongestSide(ThemedReactContext context) {
        WritableMap dimensions = getScreenDimensions(context);

        int realWidth = dimensions.getInt("width");
        int realHeight = dimensions.getInt("height");

        return realHeight > realWidth ? realHeight : realWidth;
    }

    private WritableArray getVideoTrackInfoFromManifest() {
        return this.getVideoTrackInfoFromManifest(0);
    }

    // We need retry count to in case where minefest request fails from poor network conditions
    private WritableArray getVideoTrackInfoFromManifest(int retryCount) {
        ExecutorService es = Executors.newSingleThreadExecutor();
        if (this.mediaDataSourceFactory == null || isUriNativeSource(this.srcUri)) {
            // Data source factory was not yet initialised - we can't proceed without it!
            // Native source URI is intended for offline playback - so there is no manifest info
            return null;
        }
        final DataSource dataSource = this.mediaDataSourceFactory.createDataSource();
        final Uri sourceUri = this.srcUri;
        final long startTime = this.contentStartTime * 1000 - 100; // s -> ms with 100ms offset
        int shortestScreenSide = this.getScreenShortestSide(this.themedReactContext);
        int longestScreenSide = this.getScreenLongestSide(this.themedReactContext);

        boolean limitMaxRes = this.limitMaxResolution;

        Future<WritableArray> result = es.submit(new Callable<WritableArray>() {
            DataSource ds = dataSource;
            Uri uri = sourceUri;
            long startTimeUs = startTime * 1000; // ms -> us
            int shortestScreenSize = shortestScreenSide;
            int longestScreenSize = longestScreenSide;
            boolean limitMaxResolution = limitMaxRes;

            public WritableArray call() throws Exception {
                WritableArray videoTracks = Arguments.createArray();
                try  {
                    DashManifest manifest = DashUtil.loadManifest(this.ds, this.uri);
                    int periodCount = manifest.getPeriodCount();
                    for (int i = 0; i < periodCount; i++) {
                        Period period = manifest.getPeriod(i);
                        for (int adaptationIndex = 0; adaptationIndex < period.adaptationSets.size(); adaptationIndex++) {
                            AdaptationSet adaptation = period.adaptationSets.get(adaptationIndex);
                            if (adaptation.type != C.TRACK_TYPE_VIDEO) {
                                continue;
                            }
                            boolean hasFoundContentPeriod = false;
                            for (int representationIndex = 0; representationIndex < adaptation.representations.size(); representationIndex++) {
                                Representation representation = adaptation.representations.get(representationIndex);
                                Format format = representation.format;
                                if (representation.presentationTimeOffsetUs <= startTimeUs) {
                                    break;
                                }
                                hasFoundContentPeriod = true;
                                WritableMap videoTrack = Arguments.createMap();

                                int shortestFormatSide = format.height < format.width ? format.height : format.width;
                                int longestFormatSide = format.height > format.width ? format.height : format.width;
                                if (limitMaxResolution && (shortestFormatSide > shortestScreenSize && longestFormatSide > longestScreenSize)) {
                                    // This video track is larger than screen resolution so we do not include it in the list of video tracks
                                    continue;
                                }

                                videoTrack.putInt("width", format.width == Format.NO_VALUE ? 0 : format.width);
                                videoTrack.putInt("height",format.height == Format.NO_VALUE ? 0 : format.height);
                                videoTrack.putInt("bitrate", format.bitrate == Format.NO_VALUE ? 0 : format.bitrate);
                                videoTrack.putString("codecs", format.codecs != null ? format.codecs : "");
                                videoTrack.putString("trackId",
                                        format.id == null ? String.valueOf(representationIndex) : format.id);
                                if (isFormatSupported(format)) {
                                    videoTracks.pushMap(videoTrack);
                                }
                            }
                            if (hasFoundContentPeriod) {
                                return videoTracks;
                            }
                        }
                    }
                } catch (Exception e) {}
                return null;
            }
        });

        try {
            WritableArray results = result.get(3000, TimeUnit.MILLISECONDS);
            if (results == null && retryCount < 1) {
                return this.getVideoTrackInfoFromManifest(++retryCount);
            }
            es.shutdown();
            return results;
        } catch (Exception e) {}

        return null;
    }

    private WritableArray getTextTrackInfo() {
        WritableArray textTracks = Arguments.createArray();
        if (trackSelector == null) {
            // Likely player is unmounting so no text tracks are available anymore
            return textTracks;
        }

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(C.TRACK_TYPE_TEXT);
        if (info == null || index == C.INDEX_UNSET) {
            return textTracks;
        }

        TrackGroupArray groups = info.getTrackGroups(index);
        for (int i = 0; i < groups.length; ++i) {
             Format format = groups.get(i).getFormat(0);
             WritableMap textTrack = Arguments.createMap();
             textTrack.putInt("index", i);
             textTrack.putString("title", format.id != null ? format.id : "");
             textTrack.putString("type", format.sampleMimeType);
             textTrack.putString("language", format.language != null ? format.language : "");
             textTracks.pushMap(textTrack);
        }
        return textTracks;
    }

    private void onBuffering(boolean buffering) {
        if (isBuffering == buffering) {
            return;
        }

        isBuffering = buffering;
        if (buffering) {
            eventEmitter.buffering(true);
        } else {
            eventEmitter.buffering(false);
        }
    }

    @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
        if (playerNeedsSource) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition();
        }
        if (isUsingContentResolution) {
            // Discontinuity events might have a different track list so we update the selected track
            setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
            selectTrackWhenReady = true;
        }
        // When repeat is turned on, reaching the end of the video will not cause a state change
        // so we need to explicitly detect it.
        if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION
                && player.getRepeatMode() == Player.REPEAT_MODE_ONE) {
            eventEmitter.end();
        }

    }

    public void updateAdCuePoints(Timeline timeline) {
        // Go through the timeline and find ad markers
        if (isCSAIEnabled) {
            int periodCount = timeline.getPeriodCount();
            adMarkers = new ArrayList<Double>();
            for (int i = 0; i < periodCount; i++) {
                Timeline.Period period = timeline.getPeriod(i, new Timeline.Period());
                if (period != null) {
                    int adGroupCount = period.getAdGroupCount();
                    if (adGroupCount > 0) {
                        for (int k = 0; k < adGroupCount; k++) {
                            long adGroupTimeUs = period.getAdGroupTimeUs(k);
                            long adGroupTimeMs = TimeUnit.MICROSECONDS.toMillis(adGroupTimeUs);
                            boolean isAdGroupPlayed = period.hasPlayedAdGroup(k);
                            if (!isAdGroupPlayed) {
                                adMarkers.add((double)adGroupTimeMs);
                            }
                        }
                        
                    }
                }
            }
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, int reason) {
        if (timeline.isEmpty()) {
            // The player is being reset or contains no media.
            return;
        }
        updateAdCuePoints(timeline);

    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_READY && seekTime != C.TIME_UNSET) {
            eventEmitter.seek(player.getCurrentPosition(), seekTime);
            seekTime = C.TIME_UNSET;
            if (isUsingContentResolution) {
                // We need to update the selected track to make sure that it still matches user selection if track list has changed in this period
                setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
            }
        }
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        // Do nothing.
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        // Do nothing.
    }

    @Override
    public void onTracksChanged(Tracks tracks) {
        // Do nothing.
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters params) {
        eventEmitter.playbackRateChange(params.speed);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        eventEmitter.playbackStateChanged(isPlaying);
    }

    @Override
    public void onPlayerError(PlaybackException e) {
        if (e == null) {
            return;
        }
        String errorString = "ExoPlaybackException: " + PlaybackException.getErrorCodeName(e.errorCode);
        String errorCode = "2" + String.valueOf(e.errorCode);
        boolean needsReInitialization = false;
        switch(e.errorCode) {
            case PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED:
            case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED:
            case PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED:
            case PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR:
            case PlaybackException.ERROR_CODE_DRM_UNSPECIFIED:
                if (!hasDrmFailed) {
                    // When DRM fails to reach the app level certificate server it will fail with a source error so we assume that it is DRM related and try one more time
                    hasDrmFailed = true;
                    playerNeedsSource = true;
                    updateResumePosition();
                    initializePlayer();
                    setPlayWhenReady(true);
                    return;
                }
                break;
            default:
                break;
        }
        eventEmitter.error(errorString, e, errorCode, getUriOfCurrentSegment());
        playerNeedsSource = true;
        if (isBehindLiveWindow(e)) {
            clearResumePosition();
            initializePlayer();
        } else {
            updateResumePosition();
            if (needsReInitialization) {
                initializePlayer();
            }
        }
    }

    private static boolean isBehindLiveWindow(PlaybackException e) {
        return e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW;
    }

    public int getTrackRendererIndex(int trackType) {
        if (player != null) {
            int rendererCount = player.getRendererCount();
            for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
                if (player.getRendererType(rendererIndex) == trackType) {
                    return rendererIndex;
                }
            }
        }
        return C.INDEX_UNSET;
    }

    @Override
    public void onMetadata(Metadata metadata) {
        eventEmitter.timedMetadata(metadata);
    }

    // ReactExoplayerViewManager public api

    public void setSrc(final Uri uri, final String extension, Map<String, String> headers, float startTime) {
        if (uri != null) {
            if (startTime > 0) {
                resumeWindow = 0;
                resumePosition = Math.max(0, (long) startTime);
                startPosition = (long) startTime;
            }
            boolean hasNativeSource = isUriNativeSource(uri);
            boolean isSourceEqual = uri.equals(srcUri) && !hasNativeSource;
            hasDrmFailed = false;
            this.srcUri = uri;
            this.extension = extension;
            this.requestHeaders = headers;
            if (this.bandwidthMeter == null && !hasNativeSource) {
                this.bandwidthMeter = config.getBandwidthMeter();
                this.bandwidthMeter.addEventListener(new Handler(), this);
            }
            if (hasNativeSource) {
                this.mediaDataSourceFactory = ReactExoplayerView.offlineCacheDataSourceFactory;
            } else {
                this.mediaDataSourceFactory =
                    DataSourceUtil.getDefaultDataSourceFactory(this.themedReactContext, bandwidthMeter,
                            this.requestHeaders);
            }

            if (!isSourceEqual) {
                reloadSource();
            }
        }
    }

    public void clearSrc() {
        if (srcUri != null) {
            player.stop();
            player.clearMediaItems();
            this.srcUri = null;
            this.extension = null;
            this.requestHeaders = null;
            this.mediaDataSourceFactory = null;
            ReactExoplayerView.offlineMediaSource = null;
            ReactExoplayerView.offlineCacheDataSourceFactory = null;
            clearResumePosition();
        }
        releasePlayer();
    }

    public void setStartTime(final float startTime) {
        if (startTime > 0) {
            resumeWindow = 0;
            resumePosition = Math.max(0, (long) startTime);
            startPosition = (long) startTime;
        }
    }

    public void setProgressUpdateInterval(final float progressUpdateInterval) {
        mProgressUpdateInterval = progressUpdateInterval;
    }

    public void setReportBandwidth(boolean reportBandwidth) {
        mReportBandwidth = reportBandwidth;
    }

    public void setRawSrc(final Uri uri, final String extension) {
        if (uri != null) {
            boolean hasNativeSource = isUriNativeSource(uri);
            boolean isSourceEqual = uri.equals(srcUri) && !hasNativeSource;
            this.srcUri = uri;
            this.extension = extension;
            this.mediaDataSourceFactory = buildDataSourceFactory(!hasNativeSource);

            if (!isSourceEqual) {
                reloadSource();
            }
        }
    }

    public void setTextTracks(ReadableArray textTracks) {
        this.textTracks = textTracks;
        reloadSource();
    }

    private void reloadSource() {
        playerNeedsSource = true;
        initializePlayer();
    }

    public void setResizeModeModifier(@ResizeMode.Mode int resizeMode) {
        exoPlayerView.setResizeMode(resizeMode);
    }

    public void setLimitMaxResolution(boolean limitMaxResolution) {
        this.limitMaxResolution = limitMaxResolution;
    }

    private void applyModifiers() {
        setRepeatModifier(repeat);
        setMutedModifier(muted);
    }

    public void setRepeatModifier(boolean repeat) {
        if (player != null) {
            if (repeat) {
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
            } else {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        }
        this.repeat = repeat;
    }

    public void setPreventsDisplaySleepDuringVideoPlayback(boolean preventsDisplaySleepDuringVideoPlayback) {
        this.preventsDisplaySleepDuringVideoPlayback = preventsDisplaySleepDuringVideoPlayback;
    }

    public void setSelectedTrack(int trackType, String type, Dynamic value) {
        if (player == null) return;
        int rendererIndex = getTrackRendererIndex(trackType);
        if (rendererIndex == C.INDEX_UNSET) {
            return;
        }
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        if (info == null) {
            return;
        }

        TrackGroupArray groups = info.getTrackGroups(rendererIndex);
        int groupIndex = C.INDEX_UNSET;
        List<Integer> tracks = new ArrayList<>();
        tracks.add(0);

        if (TextUtils.isEmpty(type)) {
            type = "default";
        }

        DefaultTrackSelector.Parameters disableParameters = trackSelector.getParameters()
                .buildUpon()
                .setRendererDisabled(rendererIndex, true)
                .build();

        if (type.equals("disabled")) {
            trackSelector.setParameters(disableParameters);
            return;
        } else if (type.equals("language")) {
            for (int i = 0; i < groups.length; ++i) {
                Format format = groups.get(i).getFormat(0);
                if (format.language != null && format.language.equals(value.asString())) {
                    groupIndex = i;
                    break;
                }
            }
        } else if (type.equals("title")) {
            for (int i = 0; i < groups.length; ++i) {
                Format format = groups.get(i).getFormat(0);
                if (format.id != null && format.id.equals(value.asString())) {
                    groupIndex = i;
                    break;
                }
            }
        } else if (type.equals("index")) {
            if (value.asInt() < groups.length) {
                groupIndex = value.asInt();
            }
        } else if (type.equals("resolution")) {
            int height = value.asInt();
            for (int i = 0; i < groups.length; ++i) { // Search for the exact height
                TrackGroup group = groups.get(i);
                Format closestFormat = null;
                int closestTrackIndex = -1;
                boolean usingExactMatch = false;
                for (int j = 0; j < group.length; j++) {
                    Format format = group.getFormat(j);
                    if (format.height == height) {
                        groupIndex = i;
                        tracks.set(0, j);
                        closestFormat = null;
                        closestTrackIndex = -1;
                        usingExactMatch = true;
                        break;
                    } else if (isUsingContentResolution) {
                        // When using content resolution rather than ads, we need to try and find the closest match if there is no exact match
                        if (closestFormat != null) {
                            if ((format.bitrate > closestFormat.bitrate || format.height > closestFormat.height) && format.height < height) {
                                // Higher quality match
                                closestFormat = format;
                                closestTrackIndex = j;
                            }
                        } else if(format.height < height) {
                            closestFormat = format;
                            closestTrackIndex = j;
                        }
                    }
                }
                // This is a fallback if the new period contains only higher resolutions than the user has selected
                if (closestFormat == null && isUsingContentResolution && !usingExactMatch) {
                    // No close match found - so we pick the lowest quality
                    int minHeight = Integer.MAX_VALUE;
                    for (int j = 0; j < group.length; j++) {
                        Format format = group.getFormat(j);
                        if (format.height < minHeight) {
                            minHeight = format.height;
                            groupIndex = i;
                            tracks.set(0, j);
                        }
                    }
                }
                // Selecting the closest match found
                if (closestFormat != null && closestTrackIndex != -1) {
                    // We found the closest match instead of an exact one
                    groupIndex = i;
                    tracks.set(0, closestTrackIndex);
                }
            }
        } else if (trackType == C.TRACK_TYPE_TEXT && Util.SDK_INT > 18) { // Text default
            // Use system settings if possible
            CaptioningManager captioningManager
                    = (CaptioningManager)themedReactContext.getSystemService(Context.CAPTIONING_SERVICE);
            if (captioningManager != null && captioningManager.isEnabled()) {
                groupIndex = getGroupIndexForDefaultLocale(groups);
            }
        } else if (rendererIndex == C.TRACK_TYPE_AUDIO) { // Audio default
            groupIndex = getGroupIndexForDefaultLocale(groups);
        }

        if (groupIndex == C.INDEX_UNSET && trackType == C.TRACK_TYPE_VIDEO && groups.length != 0) { // Video auto
            // Add all tracks as valid options for ABR to choose from
            TrackGroup group = groups.get(0);
            tracks = new ArrayList<Integer>(group.length);
            ArrayList<Integer> allTracks = new ArrayList<Integer>(group.length);
            groupIndex = 0;
            for (int j = 0; j < group.length; j++) {
                allTracks.add(j);
            }
            
            // Valiate list of all tracks and add only supported formats
            int supportedFormatLength = 0;
            ArrayList<Integer> supportedTrackList = new ArrayList<Integer>();
            for (int g = 0; g < allTracks.size(); g++) {
                Format format = group.getFormat(g);
                if (isFormatSupported(format)) {
                    supportedFormatLength++;
                }
            }
            if (allTracks.size() == 1) {
                // With only one tracks we can't remove any tracks so attempt to play it anyway
                tracks = allTracks;
            } else {
                tracks =  new ArrayList<>(supportedFormatLength + 1);
                for (int k = 0; k < allTracks.size(); k++) {
                    Format format = group.getFormat(k);
                    if (isFormatSupported(format)) {
                        tracks.add(allTracks.get(k));
                        supportedTrackList.add(allTracks.get(k));
                    }
                }
            }
        }

        if (groupIndex == C.INDEX_UNSET) {
            trackSelector.setParameters(disableParameters);
            return;
        }

        TrackSelectionOverride selectionOverride = new TrackSelectionOverride(groups.get(groupIndex), tracks);

        DefaultTrackSelector.Parameters selectionParameters = trackSelector.getParameters()
            .buildUpon()
            .setRendererDisabled(rendererIndex, false)
            .addOverride(selectionOverride)
            .build();
        trackSelector.setParameters(selectionParameters);
    }

    private boolean isFormatSupported(Format format) {
        int width = format.width == Format.NO_VALUE ? 0 : format.width;
        int height = format.height == Format.NO_VALUE ? 0 : format.height;
        float frameRate = format.frameRate == Format.NO_VALUE ? 0 : format.frameRate;
        String mimeType = format.sampleMimeType;
        if (mimeType == null) {
            return true;
        }
        boolean isSupported = false;
        try {
            MediaCodecInfo codecInfo = MediaCodecUtil.getDecoderInfo(mimeType, false, false);
            isSupported = codecInfo.isVideoSizeAndRateSupportedV21(width, height, frameRate);
        } catch (Exception e) {
            // Failed to get decoder info - assume it is supported
            isSupported = true;
        }
        return isSupported;
    }

    private int getGroupIndexForDefaultLocale(TrackGroupArray groups) {
        if (groups.length == 0){
            return C.INDEX_UNSET;
        }

        int groupIndex = 0; // default if no match
        String locale2 = Locale.getDefault().getLanguage(); // 2 letter code
        String locale3 = Locale.getDefault().getISO3Language(); // 3 letter code
        for (int i = 0; i < groups.length; ++i) {
            Format format = groups.get(i).getFormat(0);
            String language = format.language;
            if (language != null && (language.equals(locale2) || language.equals(locale3))) {
                groupIndex = i;
                break;
            }
        }
        return groupIndex;
    }

    public void setSelectedVideoTrack(String type, Dynamic value) {
        videoTrackType = type;
        videoTrackValue = value;
        setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
    }

    public void setSelectedAudioTrack(String type, Dynamic value) {
        audioTrackType = type;
        audioTrackValue = value;
        setSelectedTrack(C.TRACK_TYPE_AUDIO, audioTrackType, audioTrackValue);
    }

    public void setSelectedTextTrack(String type, Dynamic value) {
        textTrackType = type;
        textTrackValue = value;
        setSelectedTrack(C.TRACK_TYPE_TEXT, textTrackType, textTrackValue);
    }

    public void setMuxEnabled(boolean isEnabled) {
        this.isMuxEnabled = isEnabled;
    }

    public void setMuxOptions(ReadableMap muxOptions) {
        this.muxOptions = muxOptions;
    }

    public void setPausedModifier(boolean paused) {
        isPaused = paused;
        if (player != null) {
            if (!paused) {
                startPlayback();
            } else {
                pausePlayback();
            }
        }
    }

    public void setMutedModifier(boolean muted) {
        this.muted = muted;
        if (player != null) {
            player.setVolume(muted ? 0.f : audioVolume);
        }
    }

    public void setVolumeModifier(float volume) {
        audioVolume = volume;
        if (player != null) {
            player.setVolume(audioVolume);
        }
    }

    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
            eventEmitter.seek(player.getCurrentPosition(), positionMs);
        }
    }

    public void setRateModifier(float newRate) {
        rate = newRate;

        if (player != null) {
            PlaybackParameters params = new PlaybackParameters(rate, 1f);
            player.setPlaybackParameters(params);
        }
    }

    public void setMaxBitRateModifier(int newMaxBitRate) {
        maxBitRate = newMaxBitRate;
        if (player != null) {
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));
        }
    }

    public void setMinLoadRetryCountModifier(int newMinLoadRetryCount) {
        minLoadRetryCount = newMinLoadRetryCount;
        releasePlayer();
        initializePlayer();
    }

    public void setPlayInBackground(boolean playInBackground) {
        this.playInBackground = playInBackground;
    }

    public void setDisableFocus(boolean disableFocus) {
        this.disableFocus = disableFocus;
    }

    public void setUiLanguage(String language) {
        this.uiLanguage = language;
        if (imaSettings != null) {
            imaSettings.setLanguage(language);
        }
    }

    public void setPlayAdBeforeStartPosition(boolean shouldPlay) {
        shouldPlayAdBeforeStartPosition = shouldPlay;
    }

    public void setBackBufferDurationMs(int backBufferDurationMs) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long freeMemory = runtime.maxMemory() - usedMemory;
        long reserveMemory = (long)minBackBufferMemoryReservePercent * runtime.maxMemory();
        if (reserveMemory > freeMemory) {
            // We don't have enough memory in reserve so we will 
            Log.w("ExoPlayer Warning", "Not enough reserve memory, setting back buffer to 0ms to reduce memory pressure!");
            this.backBufferDurationMs = 0;
            return;
        }
        this.backBufferDurationMs = backBufferDurationMs;
    }

    public void setContentStartTime(int contentStartTime) {
        this.contentStartTime = (long)contentStartTime;
    }

    public void setDisableBuffering(boolean disableBuffering) {
        this.disableBuffering = disableBuffering;
    }

    public void setDisableDisconnectError(boolean disableDisconnectError) {
        this.disableDisconnectError = disableDisconnectError;
    }

    public void setEnableCSAI(boolean isEnabled) {
        this.isCSAIEnabled = isEnabled;
    }

    public void setEnableTruex(boolean isEnabled) {
        this.isTruexEnabled = isEnabled;
    }

    public void setAdTagUrl(String url) {
        this.adTagUrl = url;
    }

    public void setFullscreen(boolean fullscreen) {
        if (fullscreen == isFullscreen) {
            return; // Avoid generating events when nothing is changing
        }
        isFullscreen = fullscreen;

        Activity activity = themedReactContext.getCurrentActivity();
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        int uiOptions;
        if (isFullscreen) {
            if (Util.SDK_INT >= 19) { // 4.4+
                uiOptions = SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | SYSTEM_UI_FLAG_FULLSCREEN;
            } else {
                uiOptions = SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | SYSTEM_UI_FLAG_FULLSCREEN;
            }
            eventEmitter.fullscreenWillPresent();
            decorView.setSystemUiVisibility(uiOptions);
            eventEmitter.fullscreenDidPresent();
        } else {
            uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            eventEmitter.fullscreenWillDismiss();
            decorView.setSystemUiVisibility(uiOptions);
            eventEmitter.fullscreenDidDismiss();
        }
    }

    public void setUseTextureView(boolean useTextureView) {
        boolean finallyUseTextureView = useTextureView && this.drmUUID == null;
        exoPlayerView.setUseTextureView(finallyUseTextureView);
    }

    public void useSecureView(boolean useSecureView) {
        exoPlayerView.useSecureView(useSecureView);
    }

    public void setHideShutterView(boolean hideShutterView) {
        exoPlayerView.setHideShutterView(hideShutterView);
    }

    public void setBufferConfig(int newMinBufferMs, int newMaxBufferMs, int newBufferForPlaybackMs, int newBufferForPlaybackAfterRebufferMs, double newMaxHeapAllocationPercent, double newMinBackBufferMemoryReservePercent, double newMinBufferMemoryReservePercent, double minAvailableMemoryToEnableBackBuffer) {
        minBufferMs = newMinBufferMs;
        maxBufferMs = newMaxBufferMs;
        bufferForPlaybackMs = newBufferForPlaybackMs;
        bufferForPlaybackAfterRebufferMs = newBufferForPlaybackAfterRebufferMs;
        maxHeapAllocationPercent = newMaxHeapAllocationPercent;
        minBackBufferMemoryReservePercent = newMinBackBufferMemoryReservePercent;
        minBufferMemoryReservePercent = newMinBufferMemoryReservePercent;
        enableBackBufferAvailableMemory = minAvailableMemoryToEnableBackBuffer;
        releasePlayer();
        initializePlayer();
    }

    public void setDrmType(UUID drmType) {
        this.drmUUID = drmType;
    }

    public void setDrmLicenseUrl(String licenseUrl){
        this.drmLicenseUrl = licenseUrl;
    }

    public void setDisableTruexRateLimit(boolean isDisabled) {
        this.disableTruexRateLimit = isDisabled;
    }

    public void setDrmLicenseHeader(String[] header){
        this.drmLicenseHeader = header;
    }


    @Override
    public void onDrmKeysLoaded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        Log.d("DRM Info", "onDrmKeysLoaded");
    }

    @Override
    public void onDrmSessionManagerError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, Exception e) {
        // this method will be invoked if the http request for the license fails
        Log.d("DRM Info", "onDrmSessionManagerError");
        eventEmitter.error("onDrmSessionManagerError", e, "3002");
    }

    @Override
    public void onDrmKeysRestored(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        Log.d("DRM Info", "onDrmKeysRestored");
    }

    @Override
    public void onDrmKeysRemoved(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        Log.d("DRM Info", "onDrmKeysRemoved");
    }

    /**
     * Handling controls prop
     *
     * @param controls  Controls prop, if true enable controls, if false disable them
     */
    public void setControls(boolean controls) {
        this.controls = controls;
        if (player == null || exoPlayerView == null) return;
        if (controls) {
            addPlayerControl();
        } else {
            int indexOfPC = indexOfChild(playerControlView);
            if (indexOfPC != -1) {
                removeViewAt(indexOfPC);
            }
        }
    }
}
