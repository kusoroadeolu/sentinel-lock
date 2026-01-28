package io.github.kusoroadeolu.sentinellock.serializers;

import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

import static java.util.Objects.isNull;

public record SynchronizerSerializer(ObjectMapper objectMapper) implements RedisSerializer<@NonNull Synchronizer> {
    @Override
    public byte[] serialize(Synchronizer value) throws SerializationException {
        if (isNull(value)) return null;
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize", e);
        }
    }

    @Override
    public Synchronizer deserialize(byte[] bytes) throws SerializationException {
        if (isNull(bytes) || bytes.length == 0) return null;
        try {
            return objectMapper.readValue(bytes, Synchronizer.class);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize", e);
        }
    }
}
