package io.github.kusoroadeolu.sentinellock.serializers;

import io.github.kusoroadeolu.sentinellock.entities.LeaseState;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

public record LeaseStateSerializer(ObjectMapper objectMapper) implements RedisSerializer<LeaseState> {
    @Override
    public byte[] serialize(LeaseState value) throws SerializationException {  // Changed from Synchronizer
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize", e);
        }
    }

    @Override
    public LeaseState deserialize(byte[] bytes) throws SerializationException {  // Changed from Synchronizer
        if (bytes == null || bytes.length == 0) return null;
        try {
            return objectMapper.readValue(bytes, LeaseState.class);  // Changed from Synchronizer.class
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize", e);
        }
    }
}
