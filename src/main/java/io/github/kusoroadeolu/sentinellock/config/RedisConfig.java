package io.github.kusoroadeolu.sentinellock.config;

import io.github.kusoroadeolu.sentinellock.entities.LeaseState;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
import io.github.kusoroadeolu.sentinellock.serializers.ClientSerializer;
import io.github.kusoroadeolu.sentinellock.serializers.LeaseStateSerializer;
import io.github.kusoroadeolu.sentinellock.serializers.SynchronizerSerializer;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.convert.KeyspaceConfiguration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;

import static io.github.kusoroadeolu.sentinellock.utils.Constants.LS_PREFIX;
import static io.github.kusoroadeolu.sentinellock.utils.Constants.SYNC_PREFIX;

@Configuration
@EnableRedisRepositories(keyspaceConfiguration = RedisConfig.MyKeyspaceConfig.class, enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
public class RedisConfig extends KeyspaceConfiguration{

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Synchronizer> synchronizerTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        final var redisSerializer = new SynchronizerSerializer(objectMapper);
        final var redisTemplate = new RedisTemplate<String, Synchronizer>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(redisSerializer);
        redisTemplate.setHashValueSerializer(redisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, LeaseState> leaseStateTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        final var redisSerializer = new LeaseStateSerializer(objectMapper);
        final var redisTemplate = new RedisTemplate<String, LeaseState>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(redisSerializer);
        redisTemplate.setHashValueSerializer(redisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Object> clientTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        final var redisSerializer = new ClientSerializer(objectMapper);
        final var redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(redisSerializer);
        redisTemplate.setHashValueSerializer(redisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    @Bean
    public StringRedisTemplate testTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        final var redisSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);
        final var redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(redisSerializer);
        redisTemplate.setHashValueSerializer(redisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }




    public static class MyKeyspaceConfig extends KeyspaceConfiguration {

        @Override
        @NonNull
        protected Iterable<KeyspaceSettings> initialConfiguration() {
            final var settings  = new ArrayList<KeyspaceSettings>();
            settings.add(new KeyspaceSettings(Synchronizer.class, SYNC_PREFIX));
            settings.add(new KeyspaceSettings(LeaseState.class, LS_PREFIX)); //lock state
            return settings;
        }

    }


}
