package io.github.kusoroadeolu.sentinellock.configprops;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sentinel-lock.client")
public record ClientConfigProperties(long keyTtl) {
}
