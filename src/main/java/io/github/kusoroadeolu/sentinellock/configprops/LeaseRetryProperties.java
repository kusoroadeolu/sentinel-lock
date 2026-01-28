package io.github.kusoroadeolu.sentinellock.configprops;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:retry-config.yaml")
@ConfigurationProperties("lease")
public record LeaseRetryProperties(int backoffIntervalMs, int maxRetries, int jitterMs) {
}
