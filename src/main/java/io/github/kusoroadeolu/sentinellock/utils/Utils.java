package io.github.kusoroadeolu.sentinellock.utils;

import org.jspecify.annotations.NonNull;

public class Utils {
    private Utils(){}

    public static String appendSyncPrefix(@NonNull String key){
        return Constants.SYNC_PREFIX + key;
    }

    public static String appendLeasePrefix(@NonNull String key){
        return Constants.LS_PREFIX + key;
    }

    public static String appendClientPrefix(@NonNull String key){
        return Constants.CLIENT_PREFIX + key;
    }



}
