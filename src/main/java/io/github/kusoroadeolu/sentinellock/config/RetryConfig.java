package io.github.kusoroadeolu.sentinellock.config;

import io.github.kusoroadeolu.sentinellock.configprops.LeaseRetryProperties;
import io.github.kusoroadeolu.sentinellock.exceptions.LeaseConflictException;
import io.github.kusoroadeolu.sentinellock.exceptions.LeaseTransactionException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.support.CompositeRetryListener;

import java.time.Duration;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RetryConfig {

    private final LeaseRetryProperties leaseRetryProperties;

    @Bean
    public RetryPolicy leaseRetryPolicy(){
        return RetryPolicy
                .builder()
                .delay(Duration.ofMillis(leaseRetryProperties.backoffIntervalMs()))
                .maxRetries(this.leaseRetryProperties.maxRetries())
                .includes(List.of(LeaseTransactionException.class, LeaseConflictException.class))
                .jitter(Duration.ofMillis(this.leaseRetryProperties.jitterMs()))
                .build();
    }

    @Bean
    public RetryTemplate leaseRetryTemplate(RetryPolicy leaseRetryPolicy){
        final var retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(leaseRetryPolicy);
        retryTemplate.setRetryListener(new CompositeRetryListener());
        return retryTemplate;
    }
}
