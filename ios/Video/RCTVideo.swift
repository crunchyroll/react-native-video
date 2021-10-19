import AVFoundation
import AVKit
import MediaAccessibility
import React
import Foundation

let statusKeyPath = "status"
let playbackLikelyToKeepUpKeyPath = "playbackLikelyToKeepUp"
let playbackBufferEmptyKeyPath = "playbackBufferEmpty"
let readyForDisplayKeyPath = "readyForDisplay"
let playbackRate = "rate"
let timedMetadata = "timedMetadata"
let externalPlaybackActive = "externalPlaybackActive"

let RCTVideoUnset = -1

enum RCTVideoError : Int {
    case fromJSPart
    case licenseRequestNotOk
    case noDataFromLicenseRequest
    case noSPC
    case noDataRequest
    case noCertificateData
    case noCertificateURL
    case noFairplayDRM
    case noDRMData
}

// #if DEBUG
// #define DebugLog(...) NSLog(__VA_ARGS__)
// #else
// #define DebugLog(...) (void)0
// #endif

//#if __has_include(<react-native-video/RCTVideoCache.h>)
//@interface RCTVideo : UIView <RCTVideoPlayerViewControllerDelegate, AVAssetResourceLoaderDelegate, DVAssetLoaderDelegatesDelegate>
//#elif TARGET_OS_TV
//@interface RCTVideo : UIView <RCTVideoPlayerViewControllerDelegate, AVAssetResourceLoaderDelegate>
//#else
//@interface RCTVideo : UIView <RCTVideoPlayerViewControllerDelegate, AVAssetResourceLoaderDelegate, AVPictureInPictureControllerDelegate>
//#endif
class RCTVideo: UIView, RCTVideoPlayerViewControllerDelegate, AVAssetResourceLoaderDelegate, URLSessionDelegate {
    // #endif
    private var _player:AVPlayer?
    private var _playerItem:AVPlayerItem?
    private var _source:NSDictionary?
    private var _playerItemObserversSet:Bool = false
    private var _playerBufferEmpty:Bool = true
    private var _playerLayer:AVPlayerLayer?
    private var _playerLayerObserverSet:Bool = false
    private var _playerViewController:RCTVideoPlayerViewController?
    private var _videoURL:NSURL?
    private var _requestingCertificate:Bool = false
    private var _requestingCertificateErrored:Bool = false
    
    /* DRM */
    private var _drm:NSDictionary?
    private var _loadingRequest:AVAssetResourceLoadingRequest?
    
    /* Required to publish events */
    private var _eventDispatcher:RCTEventDispatcher?
    private var _playbackRateObserverRegistered:Bool = false
    private var _isExternalPlaybackActiveObserverRegistered:Bool = false
    private var _videoLoadStarted:Bool = false
    
    
    private var _pendingSeek:Bool = false
    private var _pendingSeekTime:Float = 0.0
    private var _lastSeekTime:Float = 0.0
    
    /* For sending videoProgress events */
    private var _progressUpdateInterval:Float64 = 250
    private var _controls:Bool = false
    private var _timeObserver:Any?
    
    /* Keep track of any modifiers, need to be applied after each play */
    private var _volume:Float = 1.0
    private var _rate:Float = 1.0
    private var _maxBitRate:Float?
    
    private var _automaticallyWaitsToMinimizeStalling:Bool = true
    private var _muted:Bool = false
    private var _paused:Bool = false
    private var _repeat:Bool = false
    private var _allowsExternalPlayback:Bool = true
    private var _textTracks:[AnyObject]?
    private var _selectedTextTrack:NSDictionary?
    private var _selectedAudioTrack:NSDictionary?
    private var _playbackStalled:Bool = false
    private var _playInBackground:Bool = false
    private var _preventsDisplaySleepDuringVideoPlayback:Bool = true
    private var _preferredForwardBufferDuration:Float = 0.0
    private var _playWhenInactive:Bool = false
    private var _ignoreSilentSwitch:String! = "inherit" // inherit, ignore, obey
    private var _mixWithOthers:String! = "inherit" // inherit, mix, duck
    private var _resizeMode:String! = "AVLayerVideoGravityResizeAspectFill"
    private var _fullscreen:Bool = false
    private var _fullscreenAutorotate:Bool = true
    private var _fullscreenOrientation:String! = "all"
    private var _fullscreenPlayerPresented:Bool = false
    private var _filterName:String!
    private var _filterEnabled:Bool = false
    private var _presentingViewController:UIViewController?
    
#if canImport(RCTVideoCache)
    private var _videoCache:RCTVideoCache! = RCTVideoCache.sharedInstance()
#endif
    
#if TARGET_OS_IOS
    private let _pip: RCTPictureInPicture = RCTPictureInPicture(onPictureInPictureStatusChanged: self.onPictureInPictureStatusChanged, onRestoreUserInterfaceForPictureInPictureStop: self.onVideoError)
#endif
    
    // Events
    @objc var onVideoLoadStart: RCTDirectEventBlock?
    @objc var onVideoLoad: RCTDirectEventBlock?
    @objc var onVideoBuffer: RCTDirectEventBlock?
    @objc var onVideoError: RCTDirectEventBlock?
    @objc var onVideoProgress: RCTDirectEventBlock?
    @objc var onBandwidthUpdate: RCTDirectEventBlock?
    @objc var onVideoSeek: RCTDirectEventBlock?
    @objc var onVideoEnd: RCTDirectEventBlock?
    @objc var onTimedMetadata: RCTDirectEventBlock?
    @objc var onVideoAudioBecomingNoisy: RCTDirectEventBlock?
    @objc var onVideoFullscreenPlayerWillPresent: RCTDirectEventBlock?
    @objc var onVideoFullscreenPlayerDidPresent: RCTDirectEventBlock?
    @objc var onVideoFullscreenPlayerWillDismiss: RCTDirectEventBlock?
    @objc var onVideoFullscreenPlayerDidDismiss: RCTDirectEventBlock?
    @objc var onReadyForDisplay: RCTDirectEventBlock?
    @objc var onPlaybackStalled: RCTDirectEventBlock?
    @objc var onPlaybackResume: RCTDirectEventBlock?
    @objc var onPlaybackRateChange: RCTDirectEventBlock?
    @objc var onVideoExternalPlaybackChange: RCTDirectEventBlock?
    @objc var onPictureInPictureStatusChanged: RCTDirectEventBlock?
    @objc var onRestoreUserInterfaceForPictureInPictureStop: RCTDirectEventBlock?
    @objc var onGetLicense: RCTDirectEventBlock?
    
    init(eventDispatcher:RCTEventDispatcher!) {
        super.init(frame: CGRect(x: 0, y: 0, width: 100, height: 100))
        
        _eventDispatcher = eventDispatcher
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(applicationWillResignActive(notification:)),
            name: UIApplication.willResignActiveNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(applicationDidEnterBackground(notification:)),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(applicationWillEnterForeground(notification:)),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(audioRouteChanged(notification:)),
            name: AVAudioSession.routeChangeNotification,
            object: nil
        )
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    
    func createPlayerViewController(player:AVPlayer!, withPlayerItem playerItem:AVPlayerItem!) -> RCTVideoPlayerViewController! {
        let viewController:RCTVideoPlayerViewController! = RCTVideoPlayerViewController()
        viewController.showsPlaybackControls = true
        viewController.rctDelegate = self
        viewController.preferredOrientation = _fullscreenOrientation
        
        viewController.view.frame = self.bounds
        viewController.player = player
        return viewController
    }
    
    /* ---------------------------------------------------------
     **  Get the duration for a AVPlayerItem.
     ** ------------------------------------------------------- */
    
    func playerItemDuration() -> CMTime {
        if let playerItem = _player?.currentItem,
           playerItem.status == .readyToPlay {
            return(playerItem.duration)
        }
        
        return(CMTime.invalid)
    }
    
    func playerItemSeekableTimeRange() -> CMTimeRange {
        if let playerItem = _player?.currentItem,
           playerItem.status == .readyToPlay,
           let firstItem = playerItem.seekableTimeRanges.first {
            return firstItem.timeRangeValue
        }
        
        return (CMTimeRange.zero)
    }
    
    func addPlayerTimeObserver() {
        let progressUpdateIntervalMS:Float64 = _progressUpdateInterval / 1000
        // @see endScrubbing in AVPlayerDemoPlaybackViewController.m
        // of https://developer.apple.com/library/ios/samplecode/AVPlayerDemo/Introduction/Intro.html
        weak var weakSelf:RCTVideo! = self
        _timeObserver = _player?.addPeriodicTimeObserver(
            forInterval: CMTimeMakeWithSeconds(progressUpdateIntervalMS, preferredTimescale: Int32(NSEC_PER_SEC)),
            queue:nil,
            using:{ (time:CMTime) in  weakSelf.sendProgressUpdate() }
        )
    }
    
    /* Cancels the previously registered time observer. */
    func removePlayerTimeObserver() {
        if let timeObserver = _timeObserver {
            _player?.removeTimeObserver(timeObserver)
            _timeObserver = nil
        }
    }
    
    // MARK: - Progress
    
    func dealloc() {
        NotificationCenter.default.removeObserver(self)
        self.removePlayerLayer()
        self.removePlayerItemObservers()
        _player?.removeObserver(self, forKeyPath:playbackRate, context:nil)
        _player?.removeObserver(self, forKeyPath:externalPlaybackActive, context: nil)
    }
    
    // MARK: - App lifecycle handlers
    
    @objc func applicationWillResignActive(notification:NSNotification!) {
        if _playInBackground || _playWhenInactive || _paused {return}
        
        _player?.pause()
        _player?.rate = 0.0
    }
    
    @objc func applicationDidEnterBackground(notification:NSNotification!) {
        if _playInBackground {
            // Needed to play sound in background. See https://developer.apple.com/library/ios/qa/qa1668/_index.html
            _playerLayer?.player = nil
            _playerViewController?.player = nil
        }
    }
    
    @objc func applicationWillEnterForeground(notification:NSNotification!) {
        self.applyModifiers()
        if _playInBackground {
            _playerLayer?.player = _player
            _playerViewController?.player = _player
        }
    }
    
    // MARK: - Audio events
    
    @objc func audioRouteChanged(notification:NSNotification!) {
        if let userInfo = notification.userInfo {
            let reason:AVAudioSession.RouteChangeReason! = userInfo[AVAudioSessionRouteChangeReasonKey] as? AVAudioSession.RouteChangeReason
            //            let previousRoute:NSNumber! = userInfo[AVAudioSessionRouteChangePreviousRouteKey] as? NSNumber
            if reason == .oldDeviceUnavailable, let onVideoAudioBecomingNoisy = onVideoAudioBecomingNoisy {
                onVideoAudioBecomingNoisy(["target": reactTag as Any])
            }
        }
    }
    
    // MARK: - Progress
    
    func sendProgressUpdate() {
        if let video = _player?.currentItem,
           video == nil || video.status != AVPlayerItem.Status.readyToPlay {
            return
        }
        
        let playerDuration:CMTime = self.playerItemDuration()
        if CMTIME_IS_INVALID(playerDuration) {
            return
        }
        
        let currentTime = _player?.currentTime()
        let currentPlaybackTime = _player?.currentItem?.currentDate()
        let duration = CMTimeGetSeconds(playerDuration)
        let currentTimeSecs = CMTimeGetSeconds(currentTime ?? .zero)
        
        NotificationCenter.default.post(name: NSNotification.Name("RCTVideo_progress"), object: nil, userInfo: [
            "progress": NSNumber(value: currentTimeSecs / duration)
        ])
        
        if currentTimeSecs >= 0 {
            onVideoProgress?([
                "currentTime": NSNumber(value: Float(currentTimeSecs)),
                "playableDuration": calculatePlayableDuration(),
                "atValue": NSNumber(value: currentTime?.value ?? .zero),
                "currentPlaybackTime": NSNumber(value: NSNumber(value: floor(currentPlaybackTime?.timeIntervalSince1970 ?? 0 * 1000)).int64Value),
                "target": reactTag,
                "seekableDuration": calculateSeekableDuration()
            ])
        }
    }
    
    /*!
     * Calculates and returns the playable duration of the current player item using its loaded time ranges.
     *
     * \returns The playable duration of the current player item in seconds.
     */
    func calculatePlayableDuration() -> NSNumber {
        let video:AVPlayerItem! = _player?.currentItem
        if video.status == AVPlayerItem.Status.readyToPlay {
            var effectiveTimeRange:CMTimeRange?
            for (_, value) in video.loadedTimeRanges.enumerated() {
                let timeRange:CMTimeRange = value.timeRangeValue
                if CMTimeRangeContainsTime(timeRange, time: video.currentTime()) {
                    effectiveTimeRange = timeRange
                    break
                }
            }
            if let effectiveTimeRange = effectiveTimeRange {
                let playableDuration:Float64 = CMTimeGetSeconds(CMTimeRangeGetEnd(effectiveTimeRange))
                if playableDuration > 0 {
                    return playableDuration as NSNumber
                }
            }
        }
        return 0
    }
    
    func calculateSeekableDuration() -> NSNumber {
        let timeRange:CMTimeRange = self.playerItemSeekableTimeRange()
        if CMTIME_IS_NUMERIC(timeRange.duration)
        {
            return NSNumber(value: CMTimeGetSeconds(timeRange.duration))
        }
        return 0
    }
    
    func addPlayerItemObservers() {
        guard let _playerItem = _playerItem else {
            return
        }
        
        _playerItem.addObserver(self, forKeyPath:statusKeyPath, options:NSKeyValueObservingOptions(), context:nil)
        _playerItem.addObserver(self, forKeyPath:playbackBufferEmptyKeyPath, options:NSKeyValueObservingOptions(), context:nil)
        _playerItem.addObserver(self, forKeyPath:playbackLikelyToKeepUpKeyPath, options:NSKeyValueObservingOptions(), context:nil)
        _playerItem.addObserver(self, forKeyPath:timedMetadata, options:.new, context:nil)
        _playerItemObserversSet = true
    }
    
    /* Fixes https://github.com/brentvatne/react-native-video/issues/43
     * Crashes caused when trying to remove the observer when there is no
     * observer set */
    func removePlayerItemObservers() {
        if _playerItemObserversSet {
            _playerItem?.removeObserver(self, forKeyPath:statusKeyPath)
            _playerItem?.removeObserver(self, forKeyPath:playbackBufferEmptyKeyPath)
            _playerItem?.removeObserver(self, forKeyPath:playbackLikelyToKeepUpKeyPath)
            _playerItem?.removeObserver(self, forKeyPath:timedMetadata)
            _playerItemObserversSet = false
        }
    }
    
    
    func delay(_ delay:Double, closure:@escaping ()->()) {
        let when = DispatchTime.now() + delay
        DispatchQueue.main.asyncAfter(deadline: when, execute: closure)
    }
    // MARK: - Player and source
    
    @objc
    func setSrc(_ source:NSDictionary!) {
        _source = source
        removePlayerLayer()
        removePlayerTimeObserver()
        removePlayerItemObservers()
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + Double(Int64(0)) / Double(NSEC_PER_SEC), execute: { [weak self] in
            guard let self = self else {return}
            // perform on next run loop, otherwise other passed react-props may not be set
            self.playerItemForSource(source: self._source, withCallback:{ (playerItem:AVPlayerItem!) in
                self._playerItem = playerItem
                self.setPreferredForwardBufferDuration(self._preferredForwardBufferDuration)
                self.addPlayerItemObservers()
                self.setFilter(self._filterName)
                if let maxBitRate = self._maxBitRate {
                    self._playerItem?.preferredPeakBitRate = Double(maxBitRate)
                }
                
                
                self._player?.pause()
                
                if self._playbackRateObserverRegistered {
                    self._player?.removeObserver(self, forKeyPath:playbackRate, context:nil)
                    self._playbackRateObserverRegistered = false
                }
                if self._isExternalPlaybackActiveObserverRegistered {
                    self._player?.removeObserver(self, forKeyPath:externalPlaybackActive, context:nil)
                    self._isExternalPlaybackActiveObserverRegistered = false
                }
                
                self._player = AVPlayer(playerItem: self._playerItem)
                self._player?.actionAtItemEnd = .none
                
                self._player?.addObserver(self, forKeyPath:playbackRate, context:nil)
                self._playbackRateObserverRegistered = true
                
                self._player?.addObserver(self, forKeyPath: externalPlaybackActive, context: nil)
                self._isExternalPlaybackActiveObserverRegistered = true
                
                self.addPlayerTimeObserver()
                if #available(iOS 10.0, *) {
                    self.setAutomaticallyWaitsToMinimizeStalling(self._automaticallyWaitsToMinimizeStalling)
                }
                
                //Perform on next run loop, otherwise onVideoLoadStart is nil
                
                let uri:AnyObject! = self._source?["uri"] as AnyObject
                let type:AnyObject! = self._source?["type"] as AnyObject
                self.onVideoLoadStart?([
                    "src": [
                        "uri": uri ?? NSNull(),
                        "type": type ?? NSNull(),
                        "isNetwork": NSNumber(value: self._source?["isNetwork"] as! Bool)
                    ],
                    "drm": self._drm ?? NSNull(),
                    "target": self.reactTag
                ])
                
            })
        })
        _videoLoadStarted = true
    }
    
    @objc
    func setDrm(_ drm:NSDictionary!) {
        _drm = drm
    }
    
    func urlFilePath(filepath:NSString!) -> NSURL! {
        if filepath.contains("file://") {
            return NSURL(string: filepath as String)
        }
        
        // if no file found, check if the file exists in the Document directory
        let paths:[String]! = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
        var relativeFilePath:String! = filepath.lastPathComponent
        // the file may be multiple levels below the documents directory
        let fileComponents:[String]! = filepath.components(separatedBy: "Documents/")
        if fileComponents.count > 1 {
            relativeFilePath = fileComponents[1]
        }
        
        let path:String! = (paths.first! as NSString).appendingPathComponent(relativeFilePath)
        if FileManager.default.fileExists(atPath: path) {
            return NSURL.fileURL(withPath: path) as NSURL
        }
        return nil
    }
    
    func playerItemPrepareText(asset:AVAsset!, assetOptions:NSDictionary?, withCallback handler:(AVPlayerItem?)->Void) {
        if (_textTracks == nil) || _textTracks?.count==0 {
            handler(AVPlayerItem(asset: asset))
            return
        }
        
        // AVPlayer can't airplay AVMutableCompositions
        _allowsExternalPlayback = false
        
        // sideload text tracks
        let mixComposition:AVMutableComposition! = AVMutableComposition()
        
        let videoAsset:AVAssetTrack! = asset.tracks(withMediaType: AVMediaType.video).first
        let videoCompTrack:AVMutableCompositionTrack! = mixComposition.addMutableTrack(withMediaType: AVMediaType.video, preferredTrackID:kCMPersistentTrackID_Invalid)
        do {
            try videoCompTrack.insertTimeRange(
                CMTimeRangeMake(start: .zero, duration: videoAsset.timeRange.duration),
                of: videoAsset,
                at: .zero)
        } catch {
        }
        
        let audioAsset:AVAssetTrack! = asset.tracks(withMediaType: AVMediaType.audio).first
        let audioCompTrack:AVMutableCompositionTrack! = mixComposition.addMutableTrack(withMediaType: AVMediaType.audio, preferredTrackID:kCMPersistentTrackID_Invalid)
        do {
            try audioCompTrack.insertTimeRange(
                CMTimeRangeMake(start: .zero, duration: videoAsset.timeRange.duration),
                of: audioAsset,
                at: .zero)
        } catch {
        }
        
        let validTextTracks:NSMutableArray! = NSMutableArray()
        if let textTracks = _textTracks, let textTrackCount = _textTracks?.count {
            for i in 0..<textTracks.count {
                var textURLAsset:AVURLAsset!
                let textUri:String! = (textTracks[i]["uri"] as! String)
                if textUri.lowercased().hasPrefix("http") {
                    textURLAsset = AVURLAsset(url: NSURL(string: textUri)! as URL, options:(assetOptions as! [String : Any]))
                } else {
                    textURLAsset = AVURLAsset(url: urlFilePath(filepath: textUri as NSString?) as URL, options:nil)
                }
                let textTrackAsset:AVAssetTrack! = textURLAsset.tracks(withMediaType: AVMediaType.text).first
                if (textTrackAsset == nil) {continue} // fix when there's no textTrackAsset
                validTextTracks.add(textTracks[i])
                let textCompTrack:AVMutableCompositionTrack! = mixComposition.addMutableTrack(withMediaType: AVMediaType.text,
                                                                                              preferredTrackID:kCMPersistentTrackID_Invalid)
                do {
                    try textCompTrack.insertTimeRange(
                        CMTimeRangeMake(start: .zero, duration: videoAsset.timeRange.duration),
                        of: textTrackAsset,
                        at: .zero)
                } catch {
                }
            }
        }
        if validTextTracks.count != _textTracks?.count {
            setTextTracks(validTextTracks as [AnyObject]?)
        }
        
        handler(AVPlayerItem(asset: mixComposition))
    }
    
    func playerItemForSource(source:NSDictionary!, withCallback handler:(AVPlayerItem?)->Void) {
        let isNetwork:Bool = RCTConvert.bool(source.object(forKey: "isNetwork"))
        let isAsset:Bool = RCTConvert.bool(source.object(forKey: "isAsset"))
        let shouldCache:Bool = RCTConvert.bool(source.object(forKey: "shouldCache"))
        let uri:String! = (source.object(forKey: "uri") as! String)
        let type:String! = (source.object(forKey: "type") as! String)
        var asset:AVURLAsset!
        if (uri == nil) || (uri == "") {
            //            DebugLog("Could not find video URL in source '%@'", source)
            return
        }
        
        let url:NSURL! = isNetwork || isAsset
        ? NSURL(string: uri)
        : NSURL.init(fileURLWithPath: Bundle.main.path(forResource: uri, ofType:type)!)
        let assetOptions:NSMutableDictionary! = NSMutableDictionary()
        
        if isNetwork {
            let headers:NSDictionary! = (source.object(forKey: "requestHeaders") as! NSDictionary)
            if headers.count > 0 {
                assetOptions.setObject(headers, forKey:"AVURLAssetHTTPHeaderFieldsKey" as NSCopying)
            }
            let cookies:[AnyObject]! = HTTPCookieStorage.shared.cookies
            assetOptions.setObject(cookies, forKey:AVURLAssetHTTPCookiesKey as NSCopying)
#if canImport(RCTVideoCache)
            if shouldCache && ((_textTracks == nil) || !_textTracks.count) {
                /* The DVURLAsset created by cache doesn't have a tracksWithMediaType property, so trying
                 * to bring in the text track code will crash. I suspect this is because the asset hasn't fully loaded.
                 * Until this is fixed, we need to bypass caching when text tracks are specified.
                 */
                DebugLog("Caching is not supported for uri '%@' because text tracks are not compatible with the cache. Checkout https://github.com/react-native-community/react-native-video/blob/master/docs/caching.md", uri)
                self.playerItemForSourceUsingCache(uri, assetOptions:assetOptions, withCallback:handler)
                return
            }
#endif
            
            asset = AVURLAsset(url: url as URL, options:assetOptions as! [String : Any])
        } else if isAsset {
            asset = AVURLAsset(url: url as URL, options:nil)
        } else {
            asset = AVURLAsset(url: NSURL.init(fileURLWithPath:Bundle.main.path(forResource: uri, ofType:type)!) as URL, options:nil)
        }
        // Reset _loadingRequest
        
        _loadingRequest?.finishLoading()
        
        _requestingCertificate = false
        _requestingCertificateErrored = false
        // End Reset _loadingRequest
        if _drm != nil {
            let queue = DispatchQueue(label: "assetQueue")
            asset.resourceLoader.setDelegate(self, queue: queue)
        }
        
        self.playerItemPrepareText(asset: asset, assetOptions:assetOptions, withCallback:handler)
    }
    
#if canImport(RCTVideoCache)
    
    func playerItemForSourceUsingCache(uri:String!, assetOptions options:NSDictionary!, withCallback handler:(AVPlayerItem!)->Void) {
        let url:NSURL! = NSURL.URLWithString(uri)
        _videoCache.getItemForUri(uri, withCallback:{ (videoCacheStatus:RCTVideoCacheStatus,cachedAsset:AVAsset?) in
            switch (videoCacheStatus) {
            case RCTVideoCacheStatusMissingFileExtension:
                DebugLog("Could not generate cache key for uri '%@'. It is currently not supported to cache urls that do not include a file extension. The video file will not be cached. Checkout https://github.com/react-native-community/react-native-video/blob/master/docs/caching.md", uri)
                let asset:AVURLAsset! = AVURLAsset.URLAssetWithURL(url, options:options)
                self.playerItemPrepareText(asset, assetOptions:options, withCallback:handler)
                return
                
            case RCTVideoCacheStatusUnsupportedFileExtension:
                DebugLog("Could not generate cache key for uri '%@'. The file extension of that uri is currently not supported. The video file will not be cached. Checkout https://github.com/react-native-community/react-native-video/blob/master/docs/caching.md", uri)
                let asset:AVURLAsset! = AVURLAsset.URLAssetWithURL(url, options:options)
                self.playerItemPrepareText(asset, assetOptions:options, withCallback:handler)
                return
                
            default:
                if cachedAsset {
                    DebugLog("Playing back uri '%@' from cache", uri)
                    // See note in playerItemForSource about not being able to support text tracks & caching
                    handler(AVPlayerItem.playerItemWithAsset(cachedAsset))
                    return
                }
            }
            
            let asset:DVURLAsset! = DVURLAsset(URL:url, options:options, networkTimeout:10000)
            asset.loaderDelegate = self
            
            /* More granular code to have control over the DVURLAsset
             let resourceLoaderDelegate = DVAssetLoaderDelegate(url: url)
             resourceLoaderDelegate.delegate = self
             let components = NSURLComponents(url: url, resolvingAgainstBaseURL: false)
             components?.scheme = DVAssetLoaderDelegate.scheme()
             var asset: AVURLAsset? = nil
             if let url = components?.url {
             asset = AVURLAsset(url: url, options: options)
             }
             asset?.resourceLoader.setDelegate(resourceLoaderDelegate, queue: DispatchQueue.main)
             */
            
            handler(AVPlayerItem.playerItemWithAsset(asset))
        })
    }
    
    // MARK: - DVAssetLoaderDelegate
    
    func dvAssetLoaderDelegate(loaderDelegate:DVAssetLoaderDelegate!, didLoadData data:NSData!, forURL url:NSURL!) {
        _videoCache.storeItem(data, forUri:url.absoluteString(), withCallback:{ (success:Bool) in
            DebugLog("Cache data stored successfully 🎉")
        })
    }
    
#endif
    
    override func observeValue(forKeyPath keyPath:String?, of object:Any?, change:[NSKeyValueChangeKey : Any]?, context:UnsafeMutableRawPointer?) {
        
        if (keyPath == readyForDisplayKeyPath) && change?[.newKey] != nil, let onReadyForDisplay = onReadyForDisplay {
            onReadyForDisplay([
                "target": reactTag
            ])
            return
        }
        if object as? AVPlayerItem == _playerItem {
            // When timeMetadata is read the event onTimedMetadata is triggered
            if (keyPath == timedMetadata) {
                let items = change?[.newKey] as? [AVMetadataItem]
                if let items = items, items.count > 0 {
                    var array1: [[String:String?]?] = []
                    for item in items {
                        let value = item.value as? String
                        let identifier = item.identifier?.rawValue
                        
                        if let value = value {
                            array1.append(["value":value, "identifier":identifier])
                        }
                    }
                    
                    onTimedMetadata?([
                        "target": reactTag,
                        "metadata": array1
                    ])
                    
                }
            }
            
            if let _playerItem = _playerItem, (keyPath == statusKeyPath) {
                // Handle player item status change.
                if _playerItem.status == AVPlayerItem.Status.readyToPlay {
                    var duration:Float = Float(CMTimeGetSeconds(_playerItem.asset.duration))
                    
                    if duration.isNaN {
                        duration = 0.0
                    }
                    
                    var width: Float? = nil
                    var height: Float? = nil
                    var orientation = "undefined"
                    
                    if _playerItem.asset.tracks(withMediaType: AVMediaType.video).count > 0 {
                        let videoTrack = _playerItem.asset.tracks(withMediaType: .video)[0]
                        width = Float(videoTrack.naturalSize.width)
                        height = Float(videoTrack.naturalSize.height)
                        let preferredTransform = videoTrack.preferredTransform
                        
                        if (videoTrack.naturalSize.width == preferredTransform.tx
                            && videoTrack.naturalSize.height == preferredTransform.ty)
                            || (preferredTransform.tx == 0 && preferredTransform.ty == 0)
                        {
                            orientation = "landscape"
                        } else {
                            orientation = "portrait"
                        }
                    } else if _playerItem.presentationSize.height != 0.0 {
                        width = Float(_playerItem.presentationSize.width)
                        height = Float(_playerItem.presentationSize.height)
                        orientation = _playerItem.presentationSize.width > _playerItem.presentationSize.height ? "landscape" : "portrait"
                    }
                    
                    if _pendingSeek {
                        setCurrentTime(_pendingSeekTime)
                        _pendingSeek = false
                    }
                    
                    if _videoLoadStarted {
                        onVideoLoad?(["duration": NSNumber(value: duration),
                                      "currentTime": NSNumber(value: Float(CMTimeGetSeconds(_playerItem.currentTime()))),
                                      "canPlayReverse": NSNumber(value: _playerItem.canPlayReverse),
                                      "canPlayFastForward": NSNumber(value: _playerItem.canPlayFastForward),
                                      "canPlaySlowForward": NSNumber(value: _playerItem.canPlaySlowForward),
                                      "canPlaySlowReverse": NSNumber(value: _playerItem.canPlaySlowReverse),
                                      "canStepBackward": NSNumber(value: _playerItem.canStepBackward),
                                      "canStepForward": NSNumber(value: _playerItem.canStepForward),
                                      "naturalSize": [
                                        "width": width != nil ? NSNumber(value: width!) : "undefinded",
                                        "height": width != nil ? NSNumber(value: height!) : "undefinded",
                                        "orientation": orientation
                                      ],
                                      "audioTracks": getAudioTrackInfo(),
                                      "textTracks": getTextTrackInfo(),
                                      "target": reactTag])
                    }
                    _videoLoadStarted = false
                    
                    self.attachListeners()
                    self.applyModifiers()
                } else if _playerItem.status == .failed {
                    onVideoError?(
                        [
                            "error": [
                                "code": NSNumber(value: (_playerItem.error! as NSError).code),
                                "localizedDescription": _playerItem.error?.localizedDescription == nil ? "" : _playerItem.error?.localizedDescription,
                                "localizedFailureReason": ((_playerItem.error! as NSError).localizedFailureReason == nil ? "" : (_playerItem.error! as NSError).localizedFailureReason) ?? "",
                                "localizedRecoverySuggestion": ((_playerItem.error! as NSError).localizedRecoverySuggestion == nil ? "" : (_playerItem.error! as NSError).localizedRecoverySuggestion) ?? "",
                                "domain": (_playerItem.error as! NSError).domain
                            ],
                            "target": reactTag
                        ])
                }
            } else if (keyPath == playbackBufferEmptyKeyPath) {
                _playerBufferEmpty = true
                onVideoBuffer?(["isBuffering": true, "target": reactTag])
            } else if (keyPath == playbackLikelyToKeepUpKeyPath) {
                // Continue playing (or not if paused) after being paused due to hitting an unbuffered zone.
                if (!(_controls || _fullscreenPlayerPresented) || _playerBufferEmpty) && ((_playerItem?.isPlaybackLikelyToKeepUp) != nil) {
                    setPaused(_paused)
                }
                _playerBufferEmpty = false
                onVideoBuffer?(["isBuffering": false, "target": reactTag])
            }
        } else if let _player = _player, object as? AVPlayer == _player {
            if (keyPath == playbackRate) {
                onPlaybackRateChange?(["playbackRate": NSNumber(value: _player.rate),
                                       "target": reactTag])
                if _playbackStalled && _player.rate > 0 {
                    onPlaybackResume?(["playbackRate": NSNumber(value: _player.rate),
                                       "target": reactTag])
                    _playbackStalled = false
                }
            }
            else if (keyPath == externalPlaybackActive) {
                onVideoExternalPlaybackChange?(["isExternalPlaybackActive": NSNumber(value: _player.isExternalPlaybackActive),
                                                "target": reactTag])
            }
        } else if object as? UIView == _playerViewController?.contentOverlayView {
            // when controls==true, this is a hack to reset the rootview when rotation happens in fullscreen
            if (keyPath == "frame") {
                
                let oldRect = (change?[.oldKey] as! NSValue).cgRectValue
                let newRect = (change?[.newKey] as! NSValue).cgRectValue
                
                if !oldRect.equalTo(newRect) {
                    if newRect.equalTo(UIScreen.main.bounds) {
                        NSLog("in fullscreen")
                        
                        self.reactViewController().view.frame = UIScreen.main.bounds
                        self.reactViewController().view.setNeedsLayout()
                    } else {NSLog("not fullscreen")}
                }
                
                return
            }
        }
    }
    
    func attachListeners() {
        // listen for end of file
        NotificationCenter.default.removeObserver(self,
                                                  name:NSNotification.Name.AVPlayerItemDidPlayToEndTime,
                                                  object:_player?.currentItem)
        NotificationCenter.default.addObserver(self,
                                               selector:#selector(playerItemDidReachEnd(notification:)),
                                               name:NSNotification.Name.AVPlayerItemDidPlayToEndTime,
                                               object:_player?.currentItem)
        
        NotificationCenter.default.removeObserver(self,
                                                  name:NSNotification.Name.AVPlayerItemPlaybackStalled,
                                                  object:nil)
        NotificationCenter.default.addObserver(self,
                                               selector:#selector(playbackStalled(notification:)),
                                               name:NSNotification.Name.AVPlayerItemPlaybackStalled,
                                               object:nil)
        
        NotificationCenter.default.removeObserver(self,
                                                  name:NSNotification.Name.AVPlayerItemNewAccessLogEntry,
                                                  object:nil)
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(handleAVPlayerAccess(notification:)),
                                               name:NSNotification.Name.AVPlayerItemNewAccessLogEntry,
                                               object:nil)
        NotificationCenter.default.removeObserver(self,
                                                  name: NSNotification.Name.AVPlayerItemFailedToPlayToEndTime,
                                                  object:nil)
        NotificationCenter.default.addObserver(self,
                                               selector:#selector(didFailToFinishPlaying(notification:)),
                                               name: NSNotification.Name.AVPlayerItemFailedToPlayToEndTime,
                                               object:nil)
        
    }
    
    @objc func handleAVPlayerAccess(notification:NSNotification!) {
        let accessLog:AVPlayerItemAccessLog! = (notification.object as! AVPlayerItem).accessLog()
        let lastEvent:AVPlayerItemAccessLogEvent! = accessLog.events.last
        
        /* TODO: get this working
         if (self.onBandwidthUpdate) {
         self.onBandwidthUpdate(@{@"bitrate": [NSNumber numberWithFloat:lastEvent.observedBitrate]});
         }
         */
    }
    
    @objc func didFailToFinishPlaying(notification:NSNotification!) {
        let error:NSError! = notification.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? NSError
        onVideoError?(
            [
                "error": [
                    "code": NSNumber(value: (error as NSError).code),
                    "localizedDescription": error.localizedDescription ?? "",
                    "localizedFailureReason": (error as NSError).localizedFailureReason ?? "",
                    "localizedRecoverySuggestion": (error as NSError).localizedRecoverySuggestion ?? "",
                    "domain": (error as NSError).domain
                ],
                "target": reactTag
            ])
    }
    
    @objc func playbackStalled(notification:NSNotification!) {
        self.onPlaybackStalled?(["target": reactTag])
        _playbackStalled = true
    }
    
    @objc func playerItemDidReachEnd(notification:NSNotification!) {
        self.onVideoEnd?(["target": reactTag])
        
        if _repeat {
            let item:AVPlayerItem! = notification.object as? AVPlayerItem
            item.seek(to: CMTime.zero)
            self.applyModifiers()
        } else {
            self.removePlayerTimeObserver()
        }
    }
    
    // MARK: - Prop setters
    
    @objc
    func setResizeMode(_ mode: String?) {
        if _controls {
            _playerViewController?.videoGravity = AVLayerVideoGravity(rawValue: mode ?? "")
        } else {
            _playerLayer?.videoGravity = AVLayerVideoGravity(rawValue: mode ?? "")
        }
        _resizeMode = mode
    }
    
    @objc
    func setPlayInBackground(_ playInBackground:Bool) {
        _playInBackground = playInBackground
    }
    
    @objc
    func setPreventsDisplaySleepDuringVideoPlayback(_ preventsDisplaySleepDuringVideoPlayback:Bool) {
        _preventsDisplaySleepDuringVideoPlayback = preventsDisplaySleepDuringVideoPlayback
        self.applyModifiers()
    }
    
    @objc
    func setAllowsExternalPlayback(_ allowsExternalPlayback:Bool) {
        _allowsExternalPlayback = allowsExternalPlayback
        _player?.allowsExternalPlayback = _allowsExternalPlayback
    }
    
    @objc
    func setPlayWhenInactive(_ playWhenInactive:Bool) {
        _playWhenInactive = playWhenInactive
    }
    
    @objc
    func setPictureInPicture(_ pictureInPicture:Bool) {
#if TARGET_OS_IOS
        _pip.setPictureInPicture(pictureInPicture)
#endif
    }
    
    @objc
    func setRestoreUserInterfaceForPIPStopCompletionHandler(_ restore:Bool) {
#if TARGET_OS_IOS
        _pip.setRestoreUserInterfaceForPIPStopCompletionHandler(restore)
#endif
    }
    
    @objc
    func setIgnoreSilentSwitch(_ ignoreSilentSwitch:String!) {
        _ignoreSilentSwitch = ignoreSilentSwitch
        self.applyModifiers()
    }
    
    @objc
    func setMixWithOthers(_ mixWithOthers:String!) {
        _mixWithOthers = mixWithOthers
        self.applyModifiers()
    }
    
    @objc
    func setPaused(_ paused:Bool) {
        if paused {
            _player?.pause()
            _player?.rate = 0.0
        } else {
            let session:AVAudioSession! = AVAudioSession.sharedInstance()
            var category:AVAudioSession.Category? = nil
            var options:AVAudioSession.CategoryOptions? = nil
            
            if (_ignoreSilentSwitch == "ignore") {
                category = AVAudioSession.Category.playback
            } else if (_ignoreSilentSwitch == "obey") {
                category = AVAudioSession.Category.ambient
            }
            
            if (_mixWithOthers == "mix") {
                options = .mixWithOthers
            } else if (_mixWithOthers == "duck") {
                options = .duckOthers
            }
            
            if let category = category, let options = options {
                do {
                    try session.setCategory(category, options: options)
                } catch {
                }
            } else if let category = category, options == nil {
                do {
                    try session.setCategory(category)
                } catch {
                }
            } else if category == nil, let options = options {
                do {
                    try session.setCategory(session.category, options: options)
                } catch {
                }
            }
            
            if #available(iOS 10.0, *), !_automaticallyWaitsToMinimizeStalling {
                _player?.playImmediately(atRate: _rate)
            } else {
                _player?.play()
                _player?.rate = _rate
            }
            _player?.rate = _rate
        }
        
        _paused = paused
    }
    
    func getCurrentTime() -> Float {
        return Float(CMTimeGetSeconds(_playerItem?.currentTime() ?? .zero))
    }
    
    @objc
    func setCurrentTime(_ currentTime:Float) {
        let info:NSDictionary! = [
            "time": NSNumber(value: currentTime),
            "tolerance": NSNumber(value: 100)
        ]
        setSeek(info)
    }
    
    @objc
    func setSeek(_ info:NSDictionary!) {
        let seekTime:NSNumber! = info["time"] as! NSNumber
        let seekTolerance:NSNumber! = info["tolerance"] as! NSNumber
        
        let timeScale:Int = 1000
        
        let item:AVPlayerItem! = _player?.currentItem
        guard item != nil && item.status == AVPlayerItem.Status.readyToPlay else {
            _pendingSeek = true
            _pendingSeekTime = seekTime.floatValue
            return
        }
        
        // TODO check loadedTimeRanges
        let cmSeekTime:CMTime = CMTimeMakeWithSeconds(Float64(seekTime.floatValue), preferredTimescale: Int32(timeScale))
        let current:CMTime = item.currentTime()
        // TODO figure out a good tolerance level
        let tolerance:CMTime = CMTimeMake(value: Int64(seekTolerance.floatValue), timescale: Int32(timeScale))
        let wasPaused:Bool = _paused
        
        guard CMTimeCompare(current, cmSeekTime) != 0 else { return }
        if !wasPaused { _player?.pause() }
        
        _player?.seek(to: cmSeekTime, toleranceBefore:tolerance, toleranceAfter:tolerance, completionHandler:{ [weak self] (finished:Bool) in
            guard let self = self else { return }
            
            if (self._timeObserver == nil) {
                self.addPlayerTimeObserver()
            }
            if !wasPaused {
                self.setPaused(false)
            }
            self.onVideoSeek?(["currentTime": NSNumber(value: Float(CMTimeGetSeconds(item.currentTime()))),
                               "seekTime": seekTime,
                               "target": self.reactTag])
        })
        
        _pendingSeek = false
    }
    
    @objc
    func setRate(_ rate:Float) {
        _rate = rate
        applyModifiers()
    }
    
    @objc
    func setMuted(_ muted:Bool) {
        _muted = muted
        applyModifiers()
    }
    
    @objc
    func setVolume(_ volume:Float) {
        _volume = volume
        applyModifiers()
    }
    
    @objc
    func setMaxBitRate(_ maxBitRate:Float) {
        _maxBitRate = maxBitRate
        _playerItem?.preferredPeakBitRate = Double(maxBitRate)
    }
    
    @objc
    func setPreferredForwardBufferDuration(_ preferredForwardBufferDuration:Float) {
        _preferredForwardBufferDuration = preferredForwardBufferDuration
        if #available(iOS 10.0, *) {
            _playerItem?.preferredForwardBufferDuration = TimeInterval(preferredForwardBufferDuration)
        } else {
            // Fallback on earlier versions
        }
    }
    
    @objc
    func setAutomaticallyWaitsToMinimizeStalling(_ waits:Bool) {
        _automaticallyWaitsToMinimizeStalling = waits
        if #available(iOS 10.0, *) {
            _player?.automaticallyWaitsToMinimizeStalling = waits
        } else {
            // Fallback on earlier versions
        }
    }
    
    
    func applyModifiers() {
        if _muted {
            if !_controls {
                _player?.volume = 0
            }
            _player?.isMuted = true
        } else {
            _player?.volume = _volume
            _player?.isMuted = false
        }
        
        if #available(iOS 12.0, *) {
            _player?.preventsDisplaySleepDuringVideoPlayback = _preventsDisplaySleepDuringVideoPlayback
        } else {
            // Fallback on earlier versions
        }
        
        if let _maxBitRate = _maxBitRate {
            setMaxBitRate(_maxBitRate)
        }
        
        setSelectedAudioTrack(_selectedAudioTrack)
        setSelectedTextTrack(_selectedTextTrack)
        setResizeMode(_resizeMode)
        setRepeat(_repeat)
        setPaused(_paused)
        setControls(_controls)
        setAllowsExternalPlayback(_allowsExternalPlayback)
    }
    
    @objc
    func setRepeat(_ `repeat`: Bool) {
        _repeat = `repeat`
    }
    
    func setMediaSelectionTrackForCharacteristic(characteristic:AVMediaCharacteristic, withCriteria criteria:NSDictionary?) {
        let type:String! = criteria?["type"] as? String
        let group:AVMediaSelectionGroup! = _player?.currentItem?.asset.mediaSelectionGroup(forMediaCharacteristic: characteristic)
        var mediaOption:AVMediaSelectionOption!
        
        if (type == "disabled") {
            // Do nothing. We want to ensure option is nil
        } else if (type == "language") || (type == "title") {
            let value:String! = criteria?["value"] as? String
            for i in 0..<group.options.count {
                let currentOption:AVMediaSelectionOption! = group.options[i]
                var optionValue:String!
                if (type == "language") {
                    optionValue = currentOption.extendedLanguageTag
                } else {
                    optionValue = currentOption.commonMetadata.map(\.value)[0] as? String
                }
                if (value == optionValue) {
                    mediaOption = currentOption
                    break
                }
            }
            //} else if ([type isEqualToString:@"default"]) {
            //  option = group.defaultOption; */
        } else if type == "index" {
            if (criteria?["value"] is NSNumber) {
                let index:Int = (criteria?["value"] as! NSNumber).intValue
                if group.options.count > index {
                    mediaOption = group.options[index]
                }
            }
        } else if let group = group { // default. invalid type or "system"
            _player?.currentItem?.selectMediaOptionAutomatically(in: group)
            return
        }
        
        if let group = group {
            // If a match isn't found, option will be nil and text tracks will be disabled
            _player?.currentItem?.select(mediaOption, in:group)
        }
        
    }
    
    @objc
    func setSelectedAudioTrack(_ selectedAudioTrack:NSDictionary!) {
        _selectedAudioTrack = selectedAudioTrack
        self.setMediaSelectionTrackForCharacteristic(characteristic: AVMediaCharacteristic.audible,
                                                     withCriteria:_selectedAudioTrack)
    }
    
    @objc
    func setSelectedTextTrack(_ selectedTextTrack:NSDictionary!) {
        _selectedTextTrack = selectedTextTrack
        if (_textTracks != nil) { // sideloaded text tracks
            self.setSideloadedText()
        } else { // text tracks included in the HLS playlist
            self.setMediaSelectionTrackForCharacteristic(characteristic: AVMediaCharacteristic.legible,
                                                         withCriteria:_selectedTextTrack)
        }
    }
    
    func setSideloadedText() {
        let type:String! = _selectedTextTrack?["type"] as? String
        let textTracks:[AnyObject]! = self.getTextTrackInfo()
        
        // The first few tracks will be audio & video track
        let firstTextIndex:Int = 0
        for firstTextIndex in 0..<(_player?.currentItem?.tracks.count ?? 0) {
            if _player?.currentItem?.tracks[firstTextIndex].assetTrack?.hasMediaCharacteristic(.legible) ?? false {
                break
            }
        }
        
        var selectedTrackIndex:Int = RCTVideoUnset
        
        if (type == "disabled") {
            // Do nothing. We want to ensure option is nil
        } else if (type == "language") {
            let selectedValue:String! = _selectedTextTrack?["value"] as? String
            for i in 0..<textTracks.count {
                let currentTextTrack:NSDictionary! = textTracks[i] as? NSDictionary
                if (selectedValue == currentTextTrack["language"] as? String) {
                    selectedTrackIndex = i
                    break
                }
            }
        } else if (type == "title") {
            let selectedValue:String! = _selectedTextTrack?["value"] as? String
            for i in 0..<textTracks.count {
                let currentTextTrack:NSDictionary! = textTracks[i] as! NSDictionary
                if (selectedValue == currentTextTrack["title"] as! String) {
                    selectedTrackIndex = i
                    break
                }
            }
        } else if (type == "index") {
            if (_selectedTextTrack?["value"] is NSNumber) {
                let index:Int = (_selectedTextTrack?["value"] as? NSNumber)!.intValue
                if textTracks.count > index {
                    selectedTrackIndex = index
                }
            }
        }
        
        // in the situation that a selected text track is not available (eg. specifies a textTrack not available)
        if (type != "disabled") && selectedTrackIndex == RCTVideoUnset {
            let captioningMediaCharacteristics = MACaptionAppearanceCopyPreferredCaptioningMediaCharacteristics(.user) as! CFArray
            let captionSettings = captioningMediaCharacteristics as? [AnyHashable]
            if ((captionSettings?.contains(AVMediaCharacteristic.transcribesSpokenDialogForAccessibility)) != nil) {
                selectedTrackIndex = 0 // If we can't find a match, use the first available track
                let systemLanguage = NSLocale.preferredLanguages.first
                for i in 0..<textTracks.count {
                    let currentTextTrack = textTracks[i] as? [AnyHashable : Any]
                    if systemLanguage == currentTextTrack?["language"] as? String {
                        selectedTrackIndex = i
                        break
                    }
                }
            }
        }
        
        for i in firstTextIndex..<(_player?.currentItem?.tracks.count ?? 0) {
            var isEnabled = false
            if selectedTrackIndex != RCTVideoUnset {
                isEnabled = i == selectedTrackIndex + firstTextIndex
            }
            _player?.currentItem?.tracks[i].isEnabled = isEnabled
        }
    }
    
    func setStreamingText() {
        let type:String! = _selectedTextTrack?["type"] as! String
        let group:AVMediaSelectionGroup! = _player?.currentItem?.asset.mediaSelectionGroup(forMediaCharacteristic: AVMediaCharacteristic.legible)
        var mediaOption:AVMediaSelectionOption!
        
        if (type == "disabled") {
            // Do nothing. We want to ensure option is nil
        } else if (type == "language") || (type == "title") {
            let value:String! = _selectedTextTrack?["value"] as! String
            for i in 0..<group.options.count {
                let currentOption:AVMediaSelectionOption! = group.options[i]
                var optionValue:String!
                if (type == "language") {
                    optionValue = currentOption.extendedLanguageTag
                } else {
                    optionValue = currentOption.commonMetadata.map(\.value)[0] as! String
                }
                if (value == optionValue) {
                    mediaOption = currentOption
                    break
                }
            }
            //} else if ([type isEqualToString:@"default"]) {
            //  option = group.defaultOption; */
        } else if (type == "index") {
            if (_selectedTextTrack?["value"] is NSNumber) {
                let index:Int = (_selectedTextTrack?["value"] as! NSNumber).intValue
                if group.options.count > index {
                    mediaOption = group.options[index]
                }
            }
        } else { // default. invalid type or "system"
            _player?.currentItem?.selectMediaOptionAutomatically(in: group)
            return
        }
        
        // If a match isn't found, option will be nil and text tracks will be disabled
        _player?.currentItem?.select(mediaOption, in:group)
    }
    
    @objc
    func setTextTracks(_ textTracks:[AnyObject]!) {
        _textTracks = textTracks
        
        // in case textTracks was set after selectedTextTrack
        if (_selectedTextTrack != nil) {setSelectedTextTrack(_selectedTextTrack)}
    }
    
    func getAudioTrackInfo() -> [AnyObject]! {
        let audioTracks:NSMutableArray! = NSMutableArray()
        let group = _player?.currentItem?.asset.mediaSelectionGroup(forMediaCharacteristic: .audible)
        for i in 0..<(group?.options.count ?? 0) {
            let currentOption = group?.options[i]
            var title = ""
            let values = currentOption?.commonMetadata.map(\.value)
            if (values?.count ?? 0) > 0, let value = values?[0] {
                title = value as! String
            }
            let language:String! = currentOption?.extendedLanguageTag ?? ""
            let audioTrack = [
                "index": NSNumber(value: i),
                "title": title,
                "language": language
            ] as [String : Any]
            audioTracks.add(audioTrack)
        }
        return audioTracks as [AnyObject]?
    }
    
    func getTextTrackInfo() -> [AnyObject]! {
        // if sideloaded, textTracks will already be set
        if (_textTracks != nil) {return _textTracks}
        
        // if streaming video, we extract the text tracks
        let textTracks:NSMutableArray! = NSMutableArray()
        let group = _player?.currentItem?.asset.mediaSelectionGroup(forMediaCharacteristic: .legible)
        for i in 0..<(group?.options.count ?? 0) {
            let currentOption = group?.options[i]
            var title = ""
            let values = currentOption?.commonMetadata.map(\.value)
            if (values?.count ?? 0) > 0, let value = values?[0] {
                title = value as! String
            }
            let language:String! = currentOption?.extendedLanguageTag ?? ""
            let textTrack:NSDictionary! = [
                "index": NSNumber(value: i),
                "title": title,
                "language": language
            ]
            textTracks.add(textTrack)
        }
        return textTracks as [AnyObject]?
    }
    
    func getFullscreen() -> Bool {
        return _fullscreenPlayerPresented
    }
    
    @objc
    func setFullscreen(_ fullscreen:Bool) {
        if fullscreen && !_fullscreenPlayerPresented && (_player != nil)
        {
            // Ensure player view controller is not null
            if _playerViewController == nil {
                self.usePlayerViewController()
            }
            
            // Set presentation style to fullscreen
            _playerViewController?.modalPresentationStyle = .fullScreen
            
            // Find the nearest view controller
            var viewController:UIViewController! = self.firstAvailableUIViewController()
            if (viewController == nil) {
                let keyWindow:UIWindow! = UIApplication.shared.keyWindow
                viewController = keyWindow.rootViewController
                if viewController.children.count > 0
                {
                    viewController = viewController.children.last
                }
            }
            if viewController != nil {
                _presentingViewController = viewController
                
                self.onVideoFullscreenPlayerWillPresent?(["target": reactTag])
                
                viewController.present(viewController, animated:true, completion:{
                    self._playerViewController?.showsPlaybackControls = true
                    self._fullscreenPlayerPresented = fullscreen
                    self._playerViewController?.autorotate = self._fullscreenAutorotate
                    
                    self.onVideoFullscreenPlayerDidPresent?(["target": self.reactTag])
                    
                })
            }
        }
        else if  !fullscreen && _fullscreenPlayerPresented
        {
            self.videoPlayerViewControllerWillDismiss(playerViewController: _playerViewController)
            _presentingViewController?.dismiss(animated: true, completion:{
                self.videoPlayerViewControllerDidDismiss(playerViewController: self._playerViewController)
            })
        }
    }
    
    @objc
    func setFullscreenAutorotate(_ autorotate:Bool) {
        _fullscreenAutorotate = autorotate
        if _fullscreenPlayerPresented {
            _playerViewController?.autorotate = autorotate
        }
    }
    
    @objc
    func setFullscreenOrientation(_ orientation:String!) {
        _fullscreenOrientation = orientation
        if _fullscreenPlayerPresented {
            _playerViewController?.preferredOrientation = orientation
        }
    }
    
    func usePlayerViewController() {
        guard _player != nil else { return }
        
        if _playerViewController == nil {
            _playerViewController = self.createPlayerViewController(player: _player, withPlayerItem:_playerItem)
        }
        // to prevent video from being animated when resizeMode is 'cover'
        // resize mode must be set before subview is added
        setResizeMode(_resizeMode)
        
        guard let _playerViewController = _playerViewController else { return }
        
        if _controls {
            let viewController:UIViewController! = self.reactViewController()
            viewController.addChild(_playerViewController)
            self.addSubview(_playerViewController.view)
        }
        
        _playerViewController.addObserver(self, forKeyPath: readyForDisplayKeyPath, options: .new, context: nil)
        
        _playerViewController.contentOverlayView?.addObserver(self, forKeyPath: "frame", options: [.new, .old], context: nil)
    }
    
    func usePlayerLayer() {
        if let _player = _player {
            _playerLayer = AVPlayerLayer(player: _player)
            _playerLayer?.frame = self.bounds
            _playerLayer?.needsDisplayOnBoundsChange = true
            
            // to prevent video from being animated when resizeMode is 'cover'
            // resize mode must be set before layer is added
            setResizeMode(_resizeMode)
            _playerLayer?.addObserver(self, forKeyPath: readyForDisplayKeyPath, options: .new, context: nil)
            _playerLayerObserverSet = true
            
            if let _playerLayer = _playerLayer {
                self.layer.addSublayer(_playerLayer)
            }
            self.layer.needsDisplayOnBoundsChange = true
#if TARGET_OS_IOS
            _pip.setupPipController(_playerLayer)
#endif
        }
    }
    
    @objc
    func setControls(_ controls:Bool) {
        if _controls != controls || ((_playerLayer == nil) && (_playerViewController == nil))
        {
            _controls = controls
            if _controls
            {
                self.removePlayerLayer()
                self.usePlayerViewController()
            }
            else
            {
                _playerViewController?.view.removeFromSuperview()
                _playerViewController = nil
                self.usePlayerLayer()
            }
        }
    }
    
    @objc
    func setProgressUpdateInterval(_ progressUpdateInterval:Float) {
        _progressUpdateInterval = Float64(progressUpdateInterval)
        
        if (_timeObserver != nil) {
            self.removePlayerTimeObserver()
            self.addPlayerTimeObserver()
        }
    }
    
    func removePlayerLayer() {
        if let _loadingRequest = _loadingRequest {
            _loadingRequest.finishLoading()
        }
        _requestingCertificate = false
        _requestingCertificateErrored = false
        _playerLayer?.removeFromSuperlayer()
        if _playerLayerObserverSet {
            _playerLayer?.removeObserver(self, forKeyPath:readyForDisplayKeyPath)
            _playerLayerObserverSet = false
        }
        _playerLayer = nil
    }
    
    // MARK: - RCTVideoPlayerViewControllerDelegate
    
    func videoPlayerViewControllerWillDismiss(playerViewController:AVPlayerViewController!) {
        if _playerViewController == playerViewController && _fullscreenPlayerPresented, let onVideoFullscreenPlayerWillDismiss = onVideoFullscreenPlayerWillDismiss
        {
            do {
                _playerViewController?.contentOverlayView?.removeObserver(self, forKeyPath: "frame")
                _playerViewController?.removeObserver(self, forKeyPath: readyForDisplayKeyPath)
            } catch {
            }
            onVideoFullscreenPlayerWillDismiss(["target": reactTag])
        }
    }
    
    
    func videoPlayerViewControllerDidDismiss(playerViewController:AVPlayerViewController!) {
        if _playerViewController == playerViewController && _fullscreenPlayerPresented
        {
            _fullscreenPlayerPresented = false
            _presentingViewController = nil
            _playerViewController = nil
            self.applyModifiers()
            
            onVideoFullscreenPlayerDidDismiss?(["target": reactTag])
            
        }
    }
    
    @objc
    func setFilter(_ filterName:String!) {
        _filterName = filterName
        
        if !_filterEnabled {
            return
        } else if (_source?.object(forKey: "uri") as! NSString).range(of: "m3u8").location != NSNotFound {
            return // filters don't work for HLS... return
        } else if _playerItem?.asset == nil {
            return
        }
        
        let filter:CIFilter! = CIFilter(name: filterName)
        if #available(iOS 9.0, *), let _playerItem = _playerItem {
            self._playerItem?.videoComposition = AVVideoComposition(
                asset: _playerItem.asset,
                applyingCIFiltersWithHandler: { (request:AVAsynchronousCIImageFilteringRequest) in
                    if filter == nil {
                        request.finish(with: request.sourceImage, context:nil)
                    } else {
                        let image:CIImage! = request.sourceImage.clampedToExtent()
                        filter.setValue(image, forKey:kCIInputImageKey)
                        let output:CIImage! = filter.outputImage?.cropped(to: request.sourceImage.extent)
                        request.finish(with: output, context:nil)
                    }
                })
        } else {
            // Fallback on earlier versions
        }
    }
    
    @objc
    func setFilterEnabled(_ filterEnabled:Bool) {
        _filterEnabled = filterEnabled
    }
    
    // MARK: - React View Management
    
    func insertReactSubview(view:UIView!, atIndex:Int) {
        // We are early in the game and somebody wants to set a subview.
        // That can only be in the context of playerViewController.
        if !_controls && (_playerLayer == nil) && (_playerViewController == nil)
        {
            setControls(true)
        }
        
        if _controls
        {
            view.frame = self.bounds
            _playerViewController?.contentOverlayView?.insertSubview(view, at:atIndex)
        }
        else
        {
            RCTLogError("video cannot have any subviews")
        }
        return
    }
    
    func removeReactSubview(subview:UIView!) {
        if _controls
        {
            subview.removeFromSuperview()
        }
        else
        {
            RCTLog("video cannot have any subviews")
        }
        return
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        if _controls, let _playerViewController = _playerViewController {
            _playerViewController.view.frame = bounds
            
            // also adjust all subviews of contentOverlayView
            for subview in _playerViewController.contentOverlayView?.subviews ?? [] {
                subview.frame = bounds
            }
        } else {
            CATransaction.begin()
            CATransaction.setAnimationDuration(0)
            _playerLayer?.frame = bounds
            CATransaction.commit()
        }
    }
    
    // MARK: - Lifecycle
    
    override func removeFromSuperview() {
        _player?.pause()
        if _playbackRateObserverRegistered {
            _player?.removeObserver(self, forKeyPath:playbackRate, context:nil)
            _playbackRateObserverRegistered = false
        }
        if _isExternalPlaybackActiveObserverRegistered {
            _player?.removeObserver(self, forKeyPath:externalPlaybackActive, context:nil)
            _isExternalPlaybackActiveObserverRegistered = false
        }
        _player = nil
        
        self.removePlayerLayer()
        
        if let _playerViewController = _playerViewController {
            _playerViewController.contentOverlayView?.removeObserver(self, forKeyPath:"frame")
            _playerViewController.removeObserver(self, forKeyPath:readyForDisplayKeyPath)
            _playerViewController.view.removeFromSuperview()
            _playerViewController.rctDelegate = nil
            _playerViewController.player = nil
            self._playerViewController = nil
        }
        
        self.removePlayerTimeObserver()
        self.removePlayerItemObservers()
        
        _eventDispatcher = nil
        NotificationCenter.default.removeObserver(self)
        
        super.removeFromSuperview()
    }
    
    // MARK: - Export
    
    func save(options:NSDictionary!, resolve: @escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) {
        
        let asset:AVAsset! = _playerItem?.asset
        
        if asset != nil {
            
            let exportSession:AVAssetExportSession! = AVAssetExportSession(asset: asset, presetName:AVAssetExportPresetHighestQuality)
            
            if exportSession != nil {
                var path:String! = nil
                let array = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).map(\.path)
                path = generatePathInDirectory(
                    directory: URL(fileURLWithPath: cacheDirectoryPath() ?? "").appendingPathComponent("Videos").path,
                    withExtension: ".mp4")
                let url:NSURL! = NSURL.fileURL(withPath: path) as NSURL
                exportSession.outputFileType = AVFileType.mp4
                exportSession.outputURL = url as URL?
                exportSession.videoComposition = _playerItem?.videoComposition
                exportSession.shouldOptimizeForNetworkUse = true
                exportSession.exportAsynchronously(completionHandler: {
                    
                    switch (exportSession.status) {
                    case .failed:
                        reject("ERROR_COULD_NOT_EXPORT_VIDEO", "Could not export video", exportSession.error)
                        break
                    case .cancelled:
                        reject("ERROR_EXPORT_SESSION_CANCELLED", "Export session was cancelled", exportSession.error)
                        break
                    default:
                        resolve(["uri": url.absoluteString])
                        break
                    }
                    
                })
                
            } else {
                
                reject("ERROR_COULD_NOT_CREATE_EXPORT_SESSION", "Could not create export session", nil)
                
            }
            
        } else {
            
            reject("ERROR_ASSET_NIL", "Asset is nil", nil)
            
        }
    }
    
    func setLicenseResult(license:String!) {
        if let respondData:NSData? = self.base64DataFromBase64String(base64String: license),
           let _loadingRequest = _loadingRequest {
            let dataRequest:AVAssetResourceLoadingDataRequest! = _loadingRequest.dataRequest
            dataRequest.respond(with: respondData as! Data)
            _loadingRequest.finishLoading()
        } else {
            setLicenseResultError(error: "No data from JS license response")
        }
    }
    
    func setLicenseResultError(error:String!) -> Bool {
        if _loadingRequest != nil {
            self.finishLoadingWithError(error: RCTVideoErrorHandler.fromJSPart(error))
        }
        return false
    }
    
    func finishLoadingWithError(error:NSError!) -> Bool {
        if let _loadingRequest = _loadingRequest, let error = error {
            let licenseError:NSError! = error
            _loadingRequest.finishLoading(with: licenseError)
            
            onVideoError?([
                "error": [
                    "code": NSNumber(value: error.code),
                    "localizedDescription": error.localizedDescription == nil ? "" : error.localizedDescription,
                    "localizedFailureReason": ((error as NSError).localizedFailureReason == nil ? "" : (error as NSError).localizedFailureReason) ?? "",
                    "localizedRecoverySuggestion": ((error as NSError).localizedRecoverySuggestion == nil ? "" : (error as NSError).localizedRecoverySuggestion) ?? "",
                    "domain": (error as NSError).domain
                ],
                "target": reactTag
            ])
            
        }
        return false
    }
    
    func ensureDirExists(withPath path: String?) -> Bool {
        var isDir: ObjCBool = false
        var error: Error?
        let exists = FileManager.default.fileExists(atPath: path ?? "", isDirectory: &isDir)
        if !(exists && isDir.boolValue) {
            do {
                try FileManager.default.createDirectory(atPath: path ?? "", withIntermediateDirectories: true, attributes: nil)
            } catch {
            }
            if error != nil {
                return false
            }
        }
        return true
    }
    
    func generatePathInDirectory(directory: String?, withExtension `extension`: String?) -> String? {
        let fileName = UUID().uuidString + (`extension` ?? "")
        ensureDirExists(withPath: directory)
        return URL(fileURLWithPath: directory ?? "").appendingPathComponent(fileName).path
    }
    
    func cacheDirectoryPath() -> String? {
        let array = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).map(\.path)
        return array[0]
    }
    
    // MARK: - AVAssetResourceLoaderDelegate
    
    func resourceLoader(_ resourceLoader:AVAssetResourceLoader!, shouldWaitForRenewalOfRequestedResource renewalRequest:AVAssetResourceRenewalRequest!) -> Bool {
        return self.loadingRequestHandling(loadingRequest: renewalRequest)
    }
    
    func resourceLoader(_ resourceLoader:AVAssetResourceLoader!, shouldWaitForLoadingOfRequestedResource loadingRequest:AVAssetResourceLoadingRequest!) -> Bool {
        return self.loadingRequestHandling(loadingRequest: loadingRequest)
    }
    
    func resourceLoader(_ resourceLoader:AVAssetResourceLoader!, didCancelLoadingRequest loadingRequest:AVAssetResourceLoadingRequest!) {
        NSLog("didCancelLoadingRequest")
    }
    
    func loadingRequestHandling(loadingRequest:AVAssetResourceLoadingRequest!) -> Bool {
        if _requestingCertificate {
            return true
        } else if _requestingCertificateErrored {
            return false
        }
        _loadingRequest = loadingRequest
        
        let url = loadingRequest.request.url
        guard let _drm = _drm else {
            return finishLoadingWithError(error: RCTVideoErrorHandler.noDRMData)
        }
        
        var contentId:String!
        let contentIdOverride:String! = _drm["contentId"] as? String
        if contentIdOverride != nil {
            contentId = contentIdOverride
        } else if (self.onGetLicense != nil) {
            contentId = url?.host
        } else {
            contentId = url?.absoluteString.replacingOccurrences(of: "skd://", with:"")
        }
        
        let drmType:String! = _drm["type"] as? String
        guard drmType == "fairplay" else {
            return finishLoadingWithError(error: RCTVideoErrorHandler.noDRMData)
        }
        
        let certificateStringUrl:String! = _drm["certificateUrl"] as? String
        guard let certificateStringUrl = certificateStringUrl, let certificateURL = URL(string: certificateStringUrl.addingPercentEncoding(withAllowedCharacters: .urlFragmentAllowed) ?? "") else {
            return finishLoadingWithError(error: RCTVideoErrorHandler.noCertificateURL)
        }
        DispatchQueue.global(qos: .default)
        do {
            var certificateData:Data? = try Data(contentsOf: certificateURL)
            // 1255 bytes - same
            if (_drm["base64Certificate"] != nil) {
                certificateData = Data(base64Encoded: certificateData! as Data, options: .ignoreUnknownCharacters)
            }
            
            guard let certificateData = certificateData else {
                finishLoadingWithError(error: RCTVideoErrorHandler.noCertificateData)
                _requestingCertificateErrored = true
                return true
            }
            
            var contentIdData:NSData!
            if self.onGetLicense != nil {
                contentIdData = contentId.data(using: .utf8) as NSData?
            } else {
                contentIdData = NSData(bytes: contentId.cString(using: String.Encoding.utf8), length:contentId.lengthOfBytes(using: String.Encoding.utf8))
                // 48 bytes
            }
            
            let dataRequest:AVAssetResourceLoadingDataRequest! = loadingRequest.dataRequest
            guard dataRequest != nil else {
                finishLoadingWithError(error: RCTVideoErrorHandler.noCertificateData)
                _requestingCertificateErrored = true
                return true
            }
            
            var spcError:NSError! = nil
            var spcData: Data? = nil
            do {
                spcData = try loadingRequest.streamingContentKeyRequestData(forApp: certificateData as Data, contentIdentifier: contentIdData as Data, options: nil)
                // 7148 bytes
            } catch let spcError {
                print("SPC error")
            }
            // Request CKC to the server
            var licenseServer:String! = _drm["licenseServer"] as? String
            if spcError != nil {
                finishLoadingWithError(error: spcError)
                _requestingCertificateErrored = true
            }
            
            guard spcData != nil else {
                finishLoadingWithError(error: RCTVideoErrorHandler.noSPC)
                _requestingCertificateErrored = true
                return true
            }
            
            // js client has a onGetLicense callback and will handle license fetching
            if let onGetLicense = onGetLicense {
                let base64Encoded = spcData?.base64EncodedString(options: [])
                _requestingCertificate = true
                if licenseServer == nil {
                    licenseServer = ""
                }
                onGetLicense(["licenseUrl": licenseServer,
                              "contentId": contentId,
                              "spcBase64": base64Encoded,
                              "target": reactTag])
                
                // license fetching will be handled inside RNV with the given parameters
            } else if licenseServer != nil {
                let request:NSMutableURLRequest! = NSMutableURLRequest()
                request.httpMethod = "POST"
                request.url = NSURL(string: licenseServer) as URL?
                // HEADERS
                let headers = _drm["headers"] as? [AnyHashable : Any]
                if let headers = headers {
                    for key in headers {
                        guard let key = key as? String else {
                            continue
                        }
                        let value = headers[key] as? String
                        request.setValue(value, forHTTPHeaderField: key)
                    }
                }
                
                if (self.onGetLicense != nil) {
                    request.httpBody = spcData
                } else {
                    let spcEncoded = spcData?.base64EncodedString(options: [])
                    let spcUrlEncoded = CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault, spcEncoded as? CFString? as! CFString, nil, "?=&+" as CFString, CFStringBuiltInEncodings.UTF8.rawValue) as? String
                    let post:String! = String(format:"spc=%@&%@", spcUrlEncoded as! CVarArg, contentId)
                    let postData:NSData! = post.data(using: String.Encoding.utf8, allowLossyConversion:true) as NSData?
                    request.httpBody = postData as Data?
                }
                
                let configuration:URLSessionConfiguration! = URLSessionConfiguration.default
                let session:URLSession! = URLSession(configuration: configuration, delegate:self, delegateQueue:nil)
                let postDataTask:URLSessionDataTask! = session.dataTask(with: request as URLRequest, completionHandler:{ [weak self] (data:Data!,response:URLResponse!,error:Error!) in
                    guard let self = self else { return }
                    let httpResponse:HTTPURLResponse! = response as! HTTPURLResponse
                    if error != nil {
                        print("Error getting license from \(url?.absoluteString), HTTP status code \(httpResponse.statusCode)")
                        self.finishLoadingWithError(error: error as NSError?)
                        self._requestingCertificateErrored = true
                    } else {
                        if httpResponse.statusCode != 200 {
                            print("Error getting license from \(url?.absoluteString), HTTP status code \(httpResponse.statusCode)")
                            self.finishLoadingWithError(error: RCTVideoErrorHandler.licenseRequestNotOk(httpResponse.statusCode))
                            self._requestingCertificateErrored = true
                        } else if data != nil {
                            if (self.onGetLicense != nil) {
                                dataRequest.respond(with: data)
                            } else {
                                let decodedData = Data(base64Encoded: data, options: [])
                                if let decodedData = decodedData {
                                    dataRequest.respond(with: decodedData)
                                }
                            }
                            loadingRequest.finishLoading()
                        } else {
                            self.finishLoadingWithError(error: RCTVideoErrorHandler.noDataFromLicenseRequest)
                            self._requestingCertificateErrored = true
                        }
                        
                    }
                })
                postDataTask.resume()
            }
            
        } catch {
        }
        return true
    }
    
    func base64DataFromBase64String(base64String:String!) -> NSData! {
        if base64String != nil {
            // NSData from the Base64 encoded str
            let base64Data:NSData! = NSData.init(base64Encoded:base64String)
            return base64Data
        }
        return nil
    }
    
}


