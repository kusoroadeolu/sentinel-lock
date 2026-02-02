package io.github.kusoroadeolu.sentinellock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest
public class RedisTemplateTest {

    @Autowired
    private StringRedisTemplate testTemplate;

    @Test
    void testWatchFailureBehavior() throws Exception {
        var syncKey = "test:watch:sync";
        var testKey = "test:watch:key";


        // Setup initial state
        testTemplate.opsForValue().set(syncKey, "initial");

        var latch1 = new CountDownLatch(1);
        var latch2 = new CountDownLatch(1);
        var resultHolder = new AtomicReference<List<Object>>();

        // Thread 1: WATCH, pause, then try to execute transaction
        var thread1 = new Thread(() -> {
            testTemplate.execute(new SessionCallback<>() {
                @Override
                public Object execute(RedisOperations ops) throws DataAccessException {
                    ops.watch(syncKey);
                    var val = ops.opsForValue().get(syncKey);

                    latch1.countDown(); // Signal thread 2 to interfere

                    try {
                        latch2.await(); // Wait for interference
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    ops.multi();
                    ops.opsForValue().set(testKey, "from-thread-1");
                    var result = ops.exec();

                    resultHolder.set(result);
                    return result;
                }
            });
        });

        // Thread 2: Modify the watched key
        var thread2 = new Thread(() -> {
            try {
                latch1.await(); // Wait for thread 1 to WATCH
                testTemplate.opsForValue().set(syncKey, "modified");
                latch2.countDown(); // Signal thread 1 to continue
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println("exec() returned: " + resultHolder.get());
        System.out.println("Is null: " + (resultHolder.get() == null));
        System.out.println("Is empty: " + (resultHolder.get() != null && resultHolder.get().isEmpty()));
    }

    @Test
    void testWatchWithExpiration() throws Exception {
        testTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations ops) throws DataAccessException {
                var key = "test:expiry";
                ops.opsForValue().set(key, "value", Duration.ofSeconds(1));

                ops.watch(key);
                try {
                    Thread.sleep(1500); // Let it expire
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                ops.multi();
                ops.opsForValue().set("result", "test");
                var res = ops.exec();
                System.out.println("After expiration, exec() returned: " + res);
                System.out.println("Is empty: " + res.isEmpty());
                return res;
            }
        });
    }

    @Test
    void testWatchWithTTLExtension() throws Exception {
        var key = "test:ttl";
        testTemplate.opsForValue().set(key, "value", Duration.ofSeconds(10));

        var latch1 = new CountDownLatch(1);
        var latch2 = new CountDownLatch(1);
        var resultHolder = new AtomicReference<List<Object>>();

        var thread1 = new Thread(() -> {
            testTemplate.execute(new SessionCallback<>() {
                @Override
                public Object execute(RedisOperations ops) throws DataAccessException {
                    ops.watch(key);
                    latch1.countDown();
                    try { latch2.await(); } catch (InterruptedException _) {}

                    ops.multi();
                    ops.opsForValue().set("result", "test");
                    resultHolder.set(ops.exec());
                    return null;
                }
            });
        });

        var thread2 = new Thread(() -> {
            try {
                latch1.await();
                testTemplate.expire(key, Duration.ofSeconds(30));
                latch2.countDown();
            } catch (InterruptedException e) {}
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println("After TTL extension, exec() returned: " + resultHolder.get());
    }

    @Test
    void testRedisKeyCount_usingCountExistingKeys(){
        final var prefix = "prefix:";
        final var key1 = prefix + 1;
        final var key2 = prefix + 2;
        final var key3 = prefix + 3;
        final var key4 = "arandomkeywithnoprefix";
        final var list = List.of(key1, key2, key3, key4);
        final var wildcard = prefix + "*";

        for (String key : list){
            testTemplate.opsForValue().set(key, key, Duration.ofMinutes(5));
        }

        var options = KeyScanOptions
                .scanOptions()
                .count(100)
                .match(wildcard)
                .build();
        int keysProcessed = 0;
        try (var cursor = testTemplate.scan(options)) {
            while (cursor.hasNext()) {
                ++keysProcessed;
                cursor.next();
            }
        }

        Assertions.assertEquals(3, keysProcessed);
    }
}
