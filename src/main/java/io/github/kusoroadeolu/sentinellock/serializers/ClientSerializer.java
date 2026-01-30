package io.github.kusoroadeolu.sentinellock.serializers;

import io.github.kusoroadeolu.sentinellock.entities.LeaseState;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

import static java.util.Objects.isNull;

public record ClientSerializer(ObjectMapper objectMapper) implements RedisSerializer<Object> {
    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (isNull(value)) return null;
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (isNull(bytes)|| bytes.length == 0) return null;
        try {
            return objectMapper.readValue(bytes, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize", e);
        }
    }
}
