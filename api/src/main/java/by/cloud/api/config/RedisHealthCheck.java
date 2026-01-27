package by.cloud.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Проверка подключения к Redis при старте приложения
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthCheck implements CommandLineRunner {
    
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    
    @Override
    public void run(String... args) {
        log.info("Checking Redis connection...");
        reactiveRedisTemplate.opsForValue()
                .set("health:check", "ok", java.time.Duration.ofSeconds(10))
                .flatMap(result -> {
                    if (result) {
                        log.info("✓ Redis connection successful");
                        return reactiveRedisTemplate.opsForValue().get("health:check");
                    } else {
                        log.error("✗ Redis connection failed - set operation returned false");
                        return Mono.empty();
                    }
                })
                .doOnNext(value -> {
                    if ("ok".equals(value)) {
                        log.info("✓ Redis read operation successful");
                    } else {
                        log.warn("Redis read returned unexpected value: {}", value);
                    }
                })
                .doOnError(error -> {
                    log.error("✗ Redis connection error: {}", error.getMessage(), error);
                    System.err.println("Redis connection failed: " + error.getMessage());
                })
                .subscribe(
                        value -> log.info("Redis health check completed"),
                        error -> log.error("Redis health check failed", error)
                );
    }
}






