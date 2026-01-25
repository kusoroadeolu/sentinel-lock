package io.github.kusoroadeolu.sentinellock.config;

import io.github.kusoroadeolu.sentinellock.entities.SyncKey;
import io.github.kusoroadeolu.sentinellock.entities.Lease;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
import io.github.kusoroadeolu.sentinellock.utils.Constants;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.convert.KeyspaceConfiguration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;


import static io.github.kusoroadeolu.sentinellock.utils.Constants.*;
import static io.github.kusoroadeolu.sentinellock.utils.Utils.modifyRedisTemplate;

@Configuration
@EnableRedisRepositories(keyspaceConfiguration = RedisConfig.MyKeyspaceConfig.class, enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
public class RedisConfig extends KeyspaceConfiguration{

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        return new LettuceConnectionFactory();
    }

    //To keep track of synchronizes
    @Bean
    public RedisTemplate<String, Synchronizer> synchronizerTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        final var redisTemplate = new RedisTemplate<String, Synchronizer>();
        modifyRedisTemplate(redisTemplate, redisConnectionFactory, objectMapper);
        return redisTemplate;
    }

    //To keep track of leases
    @Bean
    public RedisTemplate<String, Lease> leaseTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        final var redisTemplate = new RedisTemplate<String, Lease>();
        modifyRedisTemplate(redisTemplate, redisConnectionFactory, objectMapper);
        return redisTemplate;
    }

    //To keep track of fencing tokens
    @Bean
    public RedisTemplate<String, Long> fencingTokenTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        final var redisTemplate = new RedisTemplate<String, Long>();
        modifyRedisTemplate(redisTemplate, redisConnectionFactory, objectMapper);
        return redisTemplate;
    }

    public static class MyKeyspaceConfig extends KeyspaceConfiguration {

        @Override
        @NonNull
        protected Iterable<KeyspaceSettings> initialConfiguration() {
            var settings  = new ArrayList<KeyspaceSettings>();
            settings.add(new KeyspaceSettings(Synchronizer.class, SYNC_PREFIX));
            settings.add(new KeyspaceSettings(Lease.class, LEASE_PREFIX));
            settings.add(new KeyspaceSettings(Long.class, FT_PREFIX)); //Fencing Token
            return settings;
        }

    }


}
