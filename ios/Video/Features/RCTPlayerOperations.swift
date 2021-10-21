import AVFoundation
import MediaAccessibility

let RCTVideoUnset = -1

/*!
 * Collection of mutating functions
 */
struct RCTPlayerOperations {
    @available(*, unavailable) private init() {}
    
    static func setSideloadedText(player:AVPlayer?, textTracks:[AnyObject]?, selectedTextTrack:NSDictionary?) {
        let type:String! = selectedTextTrack?["type"] as? String
        let textTracks:[AnyObject]! = textTracks ?? RCTVideoUtils.getTextTrackInfo(player)
        
        // The first few tracks will be audio & video track
        let firstTextIndex:Int = 0
        for firstTextIndex in 0..<(player?.currentItem?.tracks.count ?? 0) {
            if player?.currentItem?.tracks[firstTextIndex].assetTrack?.hasMediaCharacteristic(.legible) ?? false {
                break
            }
        }
        
        var selectedTrackIndex:Int = RCTVideoUnset
        
        if (type == "disabled") {
            // Do nothing. We want to ensure option is nil
        } else if (type == "language") {
            let selectedValue:String! = selectedTextTrack?["value"] as? String
            for i in 0..<textTracks.count {
                let currentTextTrack:NSDictionary! = textTracks[i] as? NSDictionary
                if (selectedValue == currentTextTrack["language"] as? String) {
                    selectedTrackIndex = i
                    break
                }
            }
        } else if (type == "title") {
            let selectedValue:String! = selectedTextTrack?["value"] as? String
            for i in 0..<textTracks.count {
                let currentTextTrack:NSDictionary! = textTracks[i] as! NSDictionary
                if (selectedValue == currentTextTrack["title"] as! String) {
                    selectedTrackIndex = i
                    break
                }
            }
        } else if (type == "index") {
            if (selectedTextTrack?["value"] is NSNumber) {
                let index:Int = (selectedTextTrack?["value"] as? NSNumber)!.intValue
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
        
        for i in firstTextIndex..<(player?.currentItem?.tracks.count ?? 0) {
            var isEnabled = false
            if selectedTrackIndex != RCTVideoUnset {
                isEnabled = i == selectedTrackIndex + firstTextIndex
            }
            player?.currentItem?.tracks[i].isEnabled = isEnabled
        }
    }
    
    // UNUSED
    static func setStreamingText(player:AVPlayer?, selectedTextTrack:NSDictionary?) {
        let type:String! = selectedTextTrack?["type"] as! String
        let group:AVMediaSelectionGroup! = player?.currentItem?.asset.mediaSelectionGroup(forMediaCharacteristic: AVMediaCharacteristic.legible)
        var mediaOption:AVMediaSelectionOption!
        
        if (type == "disabled") {
            // Do nothing. We want to ensure option is nil
        } else if (type == "language") || (type == "title") {
            let value:String! = selectedTextTrack?["value"] as! String
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
            if (selectedTextTrack?["value"] is NSNumber) {
                let index:Int = (selectedTextTrack?["value"] as! NSNumber).intValue
                if group.options.count > index {
                    mediaOption = group.options[index]
                }
            }
        } else { // default. invalid type or "system"
            player?.currentItem?.selectMediaOptionAutomatically(in: group)
            return
        }
        
        // If a match isn't found, option will be nil and text tracks will be disabled
        player?.currentItem?.select(mediaOption, in:group)
    }
}
