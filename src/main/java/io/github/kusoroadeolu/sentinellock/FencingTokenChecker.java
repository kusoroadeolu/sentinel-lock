package io.github.kusoroadeolu.sentinellock;

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

import java.util.function.Function;

import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendSyncPrefix;
import static io.github.kusoroadeolu.sentinellock.utils.Utils.isNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class FencingTokenChecker<T> {
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;

    public void save(@NonNull Lease lease, @NonNull Runnable action){
        final var redisOps = this.synchronizerTemplate.opsForValue().getOperations();
        redisOps.execute(new RunnableSessionCallback(lease, action));
    }

    public void save(@NonNull Lease lease, @NonNull T t , @NonNull Function<T, ?> action){
        final var redisOps = this.synchronizerTemplate.opsForValue().getOperations();
        redisOps.execute(new FunctionSessionCallback(lease, action, t));
    }

    private interface SaveResult {
        record Success  (@Nullable Object value) implements SaveResult {}
        record Error (@NonNull Exception ex) implements SaveResult {}
        enum Invalid implements SaveResult{
            INVALID
        }
        enum Failed implements SaveResult{
            FAILED
        }
    }

    private record RunnableSessionCallback(@NonNull Lease lease, @NonNull Runnable action) implements SessionCallback<Object> {

        @SuppressWarnings("unchecked")
        public SaveResult execute(@NonNull RedisOperations ops) throws DataAccessException {
            if (!(lease instanceof CompleteLease(var key, var fencingToken))) return SaveResult.Invalid.INVALID;
            final var syncKey = appendSyncPrefix(key.key());
            try {
                ops.watch(syncKey);
                final var synchronizer = (Synchronizer) ops.opsForValue().get(syncKey);
                if (isNull(synchronizer) || synchronizer.currentFencingToken() != fencingToken)
                    return SaveResult.Failed.FAILED;
                ops.multi();
                action.run();
                final var res = ops.exec();
                if (isNull(res) || res.isEmpty()) {
                    log.info("Save transaction for fencing token: {} failed due to a race condition", fencingToken);
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
    private class FunctionSessionCallback implements SessionCallback<Object> {
        private final Lease lease;
        private final Function<T, ?> action;
        private final T t;

        public FunctionSessionCallback(@NonNull Lease lease, @NonNull Function<T, ?> action, @NonNull T t) {
            this.lease = lease;
            this.action = action;
            this.t = t;
        }

        @Override
        public SaveResult execute(@NonNull RedisOperations ops) throws DataAccessException {
            if (!(lease instanceof CompleteLease(var key, var fencingToken))) return SaveResult.Invalid.INVALID;
            final var syncKey = appendSyncPrefix(key.key());
            try {
                ops.watch(syncKey);
                final var synchronizer = (Synchronizer) ops.opsForValue().get(syncKey);
                if (isNull(synchronizer) || synchronizer.currentFencingToken() != fencingToken) return SaveResult.Failed.FAILED;
                ops.multi();
                final var o = action.apply(t);
                final var res = ops.exec();
                if (isNull(res) || res.isEmpty()){
                    log.info("Save transaction for fencing token: {} failed due to a race condition", fencingToken);
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
