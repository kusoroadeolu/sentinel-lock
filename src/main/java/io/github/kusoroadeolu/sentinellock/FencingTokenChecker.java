package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.Lease;
import io.github.kusoroadeolu.sentinellock.entities.Lease.CompleteLease;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Function;

import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendSyncPrefix;
import static java.util.Objects.isNull;


@Service
@RequiredArgsConstructor
@Slf4j
public class FencingTokenChecker<T> {
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;
    private final SentinelLockConfigProperties configProperties;
    private final static String LOG_MESSAGE = "Save transaction for fencing token: {} failed due to a race condition";

    public SaveResult save(@NonNull Lease lease, @NonNull Runnable action){
        final var redisOps = this.synchronizerTemplate.opsForValue().getOperations();
        return redisOps.execute(new RunnableSessionCallback(lease, action, configProperties));
    }

    public SaveResult save(@NonNull Lease lease, @NonNull T t , @NonNull Function<T, ?> action){
        final var redisOps = this.synchronizerTemplate.opsForValue().getOperations();
        return redisOps.execute(new FunctionSessionCallback<>(lease, action, t, configProperties));
    }

    public interface SaveResult {
        record Success  (@Nullable Object value) implements SaveResult {}
        record Error (@NonNull Exception ex) implements SaveResult {}
        enum Invalid implements SaveResult{
            INVALID
        }
        enum Failed implements SaveResult{
            FAILED
        }
    }

    private record RunnableSessionCallback(@NonNull Lease lease, @NonNull Runnable action, @NonNull SentinelLockConfigProperties configProperties) implements SessionCallback<SaveResult> {

        @SuppressWarnings("unchecked")
        public @NonNull SaveResult execute(@NonNull RedisOperations ops) throws DataAccessException {
            if (!(lease instanceof CompleteLease(var key, var fencingToken))) return SaveResult.Invalid.INVALID;
            final var syncKey = appendSyncPrefix(key.key());
            final var syncTtl = this.configProperties.syncIdleTtl();

            try {
                ops.watch(syncKey);
                final var synchronizer = (Synchronizer) ops.opsForValue().get(syncKey);
                if (isNull(synchronizer) ||synchronizer.currentFencingToken() != fencingToken){
                    ops.unwatch();
                    return SaveResult.Failed.FAILED;
                }

                ops.multi();
                ops.expire(syncKey, Duration.ofMillis(syncTtl)); //A redis action to ensure this transaction isn't empty on success
                action.run();
                final var res = ops.exec();
                log.info("Res: {}", res);
                if (res.isEmpty()) {
                    log.info(LOG_MESSAGE, fencingToken);
                    return SaveResult.Failed.FAILED;
                }
                return new SaveResult.Success(null);
            } catch (DataAccessException e) {
                ops.discard();
                log.error("An error occurred while trying to a perform redis transaction to save a lease for key: {}", syncKey, e);
                return new SaveResult.Error(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private record FunctionSessionCallback<T>(@NonNull Lease lease, @NonNull Function<T, ?> action, @NonNull T t,  @NonNull SentinelLockConfigProperties configProperties) implements SessionCallback<SaveResult> {

        @Override
        public @NonNull SaveResult execute(@NonNull RedisOperations ops) throws DataAccessException {
            if (!(lease instanceof CompleteLease(var key, var fencingToken))) return SaveResult.Invalid.INVALID;
            final var syncKey = appendSyncPrefix(key.key());
            final var syncTtl = this.configProperties.syncIdleTtl();
            try {
                ops.watch(syncKey);
                final var synchronizer = (Synchronizer) ops.opsForValue().get(syncKey);
                if (isNull(synchronizer) || synchronizer.currentFencingToken() != fencingToken){
                    ops.unwatch();
                    return SaveResult.Failed.FAILED;
                }

                ops.multi();
                ops.expire(syncKey, Duration.ofMillis(syncTtl)); //A redis action to ensure this transaction isn't empty on success
                final var o = action.apply(t);
                final var res = ops.exec();
                if (res.isEmpty()){
                    log.info(LOG_MESSAGE, fencingToken);
                    return SaveResult.Failed.FAILED;
                }

                return new SaveResult.Success(o);
            }catch (DataAccessException e){
                ops.discard();
                log.error("An error occurred while trying to a perform redis transaction to save a lease for key: {}", syncKey , e);
                return new SaveResult.Error(e);
            }
        }
    }
}
