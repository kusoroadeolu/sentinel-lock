package io.github.kusoroadeolu.sentinellock.utils;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

public class Utils {
    private Utils(){}

    public static void modifyRedisTemplate(RedisTemplate<?, ?> redisTemplate, RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJacksonJsonRedisSerializer(objectMapper));
        redisTemplate.setHashValueSerializer(new GenericJacksonJsonRedisSerializer(objectMapper));
        redisTemplate.afterPropertiesSet();
    }
}
