package com.brentvatne.exoplayer;

import java.io.IOException;
import android.util.Log;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.HttpDataSource.HttpDataSourceException;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy.LoadErrorInfo;
import com.google.android.exoplayer2.C;

public final class ReactExoplayerLoadErrorHandlingPolicy extends DefaultLoadErrorHandlingPolicy {
  private int minLoadRetryCount = Integer.MAX_VALUE;

  public ReactExoplayerLoadErrorHandlingPolicy(int minLoadRetryCount) {
    super(minLoadRetryCount);
    this.minLoadRetryCount = minLoadRetryCount;
  }

  @Override
  public long getRetryDelayMsFor(LoadErrorInfo loadErrorInfo) {
    Log.d("nnn loadErrorInfo", loadErrorInfo.toString());
    Log.d("nnn loadEventInfo", loadErrorInfo.exception.toString());
    Log.d("nnn loadEventInfo", loadErrorInfo.exception.getMessage());
    Log.d("nnn exception", String.valueOf(loadErrorInfo.exception.hashCode()));
    Log.d("nnn exception", String.valueOf(Integer.valueOf(loadErrorInfo.exception.hashCode())));
    if (loadErrorInfo.exception instanceof HttpDataSourceException && String.valueOf(loadErrorInfo.exception.hashCode()) == "Unable to connect") {
      // Capture the error we get when there is no network connectivity and keep retrying it
      return 1000; // Retry every second
    } else if(loadErrorInfo.errorCount < this.minLoadRetryCount) {
      return Math.min((loadErrorInfo.errorCount - 1) * 1000, 5000); // Default timeout handling
    } else {
      return C.TIME_UNSET; // Done retrying and will return the error immediately
    }
  }

  @Override
  public int getMinimumLoadableRetryCount(int dataType) {
    return Integer.MAX_VALUE;
  }
}