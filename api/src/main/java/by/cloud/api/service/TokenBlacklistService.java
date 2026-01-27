package by.cloud.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24); // TTL токена обычно 24 часа

    public Mono<Boolean> addToBlacklist(String tokenId, Duration ttl) {
        if (tokenId == null || tokenId.isEmpty()) {
            log.warn("Attempted to add null or empty tokenId to blacklist");
            return Mono.just(false);
        }
        
        String key = BLACKLIST_PREFIX + tokenId;
        log.info("Adding token to blacklist. Key: {}, TTL: {} hours", key, ttl.toHours());
        
        return reactiveRedisTemplate.opsForValue()
                .set(key, "blacklisted", ttl)
                .doOnSuccess(result -> {
                    if (result) {
                        log.info("Token successfully added to blacklist: {}", key);
                        System.out.println("Redis key created: " + key);
                    } else {
                        log.warn("Failed to add token to blacklist (result was false): {}", key);
                    }
                })
                .doOnError(error -> {
                    log.error("Error adding token to blacklist: {}", key, error);
                    System.err.println("Redis error: " + error.getMessage());
                })
                .onErrorReturn(false);
    }

    public Mono<Boolean> addToBlacklist(String tokenId) {
        return addToBlacklist(tokenId, DEFAULT_TTL);
    }

    public Mono<Boolean> isBlacklisted(String tokenId) {
        System.out.println("Redis:tokenId: " + tokenId);
        if (tokenId == null || tokenId.isEmpty()) {
            return Mono.just(false);
        }
        
        String key = BLACKLIST_PREFIX + tokenId;
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .map(value -> value != null)
                .defaultIfEmpty(false)
                .doOnNext(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.debug("Token found in blacklist: {}", tokenId);
                    }
                })
                .doOnError(error -> log.error("Error checking token blacklist: {}", tokenId, error))
                .onErrorReturn(false);
    }

    public Mono<Boolean> removeFromBlacklist(String tokenId) {
        if (tokenId == null || tokenId.isEmpty()) {
            return Mono.just(false);
        }
        
        String key = BLACKLIST_PREFIX + tokenId;
        return reactiveRedisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSuccess(result -> {
                    if (result) {
                        log.debug("Token removed from blacklist: {}", tokenId);
                    }
                })
                .doOnError(error -> log.error("Error removing token from blacklist: {}", tokenId, error))
                .onErrorReturn(false);
    }
}

