package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.configprops.ClientConfigProperties;
import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.Lease;
import io.github.kusoroadeolu.sentinellock.entities.Lease.CompleteLease;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
import io.github.kusoroadeolu.sentinellock.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendSyncPrefix;
import static java.util.Objects.isNull;


@Service
@RequiredArgsConstructor
@Slf4j
public class FencingTokenValidator {
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;
    private final RedisTemplate<String, Object> clientTemplate;
    private final SentinelLockConfigProperties sentinelLockConfigProperties;
    private final ClientConfigProperties clientConfigProperties;
    private final static String LOG_MESSAGE = "Save transaction for fencing token: {} failed due to a race condition";


    public SaveResult validateAndSave(@NonNull Lease lease, @NonNull Object value){
        final var redisOps = this.synchronizerTemplate.opsForValue().getOperations();
        return redisOps.execute(new FencingTokenSessionCallback(lease, value , clientTemplate ,sentinelLockConfigProperties, clientConfigProperties));
    }


    public interface SaveResult {
        default boolean isValid(){
            return false;
        }

        record Success() implements SaveResult {
            public boolean isValid() {
                return true;
            }
        }
        record Error (@NonNull Exception ex) implements SaveResult {}
        enum Invalid implements SaveResult{
            INVALID_LEASE
        }
        enum Failed implements SaveResult{
            FAILED
        }
    }

    private record FencingTokenSessionCallback(@NonNull Lease lease, @NonNull Object object, @NonNull RedisTemplate<String, Object> clientTemplate , @NonNull SentinelLockConfigProperties sentinelLockConfigProperties, ClientConfigProperties clientConfigProperties) implements SessionCallback<SaveResult> {

        @SuppressWarnings("unchecked")
        public @NonNull SaveResult execute(@NonNull RedisOperations ops) throws DataAccessException {
            if (!(lease instanceof CompleteLease(var key, var _ ,var fencingToken))) return SaveResult.Invalid.INVALID_LEASE;
            final var syncKey = appendSyncPrefix(key.key());
            final var syncTtl = this.sentinelLockConfigProperties.syncIdleTtl();

            try {
                ops.watch(syncKey);
                final var synchronizer = (Synchronizer) ops.opsForValue().get(syncKey);
                if (isNull(synchronizer) || synchronizer.currentFencingToken() != fencingToken){
                    ops.unwatch();
                    return SaveResult.Failed.FAILED;
                }

                ops.multi();
                ops.expire(syncKey, Duration.ofMillis(syncTtl)); //A redis action to ensure this transaction isn't empty on success
                final var res = ops.exec();
                if (res.isEmpty()) {
                    log.info(LOG_MESSAGE, fencingToken);
                    return SaveResult.Failed.FAILED;
                }

                clientTemplate.opsForValue().set(Utils.appendClientPrefix(key.key()), Duration.ofMillis(clientConfigProperties.keyTtl()));
                return new SaveResult.Success();
            } catch (DataAccessException e) {
                ops.discard();
                log.error("An error occurred while trying to a perform redis transaction to save a lease for key: {}", syncKey, e);
                return new SaveResult.Error(e);
            }
        }
    }
}
