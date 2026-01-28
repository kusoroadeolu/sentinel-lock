package io.github.kusoroadeolu.sentinellock.serializers;

import io.github.kusoroadeolu.sentinellock.entities.LeaseState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

import static java.util.Objects.isNull;

public record LeaseStateSerializer(ObjectMapper objectMapper) implements RedisSerializer<LeaseState> {
    @Override
    public byte[] serialize(LeaseState value) throws SerializationException {
        if (isNull(value)) return null;
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize", e);
        }
    }

    @Override
    public LeaseState deserialize(byte[] bytes) throws SerializationException {
        if (isNull(bytes)|| bytes.length == 0) return null;
        try {
            return objectMapper.readValue(bytes, LeaseState.class);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize", e);
        }
    }
}
