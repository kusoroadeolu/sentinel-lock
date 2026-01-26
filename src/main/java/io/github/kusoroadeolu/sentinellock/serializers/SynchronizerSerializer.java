package io.github.kusoroadeolu.sentinellock.serializers;

import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

public record SynchronizerSerializer(ObjectMapper objectMapper) implements RedisSerializer<@NonNull Synchronizer> {
    @Override
    public byte[] serialize(Synchronizer value) throws SerializationException {  // Changed from LeaseState
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize", e);
        }
    }

    @Override
    public Synchronizer deserialize(byte[] bytes) throws SerializationException {  // Changed from LeaseState
        if (bytes == null || bytes.length == 0) return null;
        try {
            return objectMapper.readValue(bytes, Synchronizer.class);  // Changed from LeaseState.class
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize", e);
        }
    }
}
