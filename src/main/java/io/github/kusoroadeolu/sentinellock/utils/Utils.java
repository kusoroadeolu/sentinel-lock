package io.github.kusoroadeolu.sentinellock.utils;

public class Utils {
    private Utils(){}

    public static String appendSyncPrefix(String key){
        return Constants.SYNC_PREFIX + key;
    }

    public static String appendLeasePrefix(String key){
        return Constants.LS_PREFIX + key;
    }
}
