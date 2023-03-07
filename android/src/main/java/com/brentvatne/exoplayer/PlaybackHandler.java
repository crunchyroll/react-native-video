package com.brentvatne.exoplayer;

public interface PlaybackHandler {
    void resumeStream();
    void closeStream();
    void displayLinearAds();
    void handlePopup(String url);
}
