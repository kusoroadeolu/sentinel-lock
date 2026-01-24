package io.github.kusoroadeolu.sentinellock.configprops;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sentinel-lock")
public record SentinelLockConfigProperties(
        int maxQueuedRegistryClients,
        long minLeaseRequestDuration,
        long maxLeaseRequestDuration,
        long syncIdleTtl
) {
}
