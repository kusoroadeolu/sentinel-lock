package io.github.kusoroadeolu.sentinellock.entities;

import io.github.kusoroadeolu.sentinellock.annotations.Proto;
import org.jspecify.annotations.NonNull;

@Proto
public record SaveRequest(@NonNull Lease lease, @NonNull Object value) {
}
