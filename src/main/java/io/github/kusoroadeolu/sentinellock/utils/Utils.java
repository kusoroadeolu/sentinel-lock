package io.github.kusoroadeolu.sentinellock.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Utils {
    private Utils(){}

    public static String appendSyncPrefix(@NonNull String key){
        return Constants.SYNC_PREFIX + key;
    }

    public static String appendLeasePrefix(@NonNull String key){
        return Constants.LS_PREFIX + key;
    }

    public static boolean isNull(@Nullable final Object o){
        return o == null;
    }
}
