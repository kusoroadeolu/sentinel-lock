package io.github.kusoroadeolu.sentinellock.config;

import io.github.kusoroadeolu.sentinellock.entities.ClientId;
import io.github.kusoroadeolu.sentinellock.entities.Lease;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
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

import java.util.Collections;

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
    public RedisTemplate<ClientId, Synchronizer> synchronizerTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        var redisTemplate = new RedisTemplate<ClientId, Synchronizer>();
        modifyRedisTemplate(redisTemplate, redisConnectionFactory, objectMapper);
        return redisTemplate;
    }

    //To keep track of leases
    @Bean
    public RedisTemplate<ClientId, Lease> leaseTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        var redisTemplate = new RedisTemplate<ClientId, Lease>();
        modifyRedisTemplate(redisTemplate, redisConnectionFactory, objectMapper);
        return redisTemplate;
    }

    //To keep track of fencing tokens
    @Bean
    public RedisTemplate<ClientId, Long> fencingTokenTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        var redisTemplate = new RedisTemplate<ClientId, Long>();
        modifyRedisTemplate(redisTemplate, redisConnectionFactory, objectMapper);
        return redisTemplate;
    }

    public static class MyKeyspaceConfig extends KeyspaceConfiguration {

        @Override
        @NonNull
        protected Iterable<KeyspaceSettings> initialConfiguration() {
            return Collections.singleton(new KeyspaceSettings(Synchronizer.class, "sync"));
        }
    }


}
