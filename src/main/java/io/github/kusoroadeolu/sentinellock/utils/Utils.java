package io.github.kusoroadeolu.sentinellock.utils;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

public class Utils {
    private Utils(){}

    public static String appendSyncPrefix(String key){
        return Constants.SYNC_PREFIX + key;
    }

    public static String appendLeasePrefix(String key){
        return Constants.LS_PREFIX + key;
    }
}
