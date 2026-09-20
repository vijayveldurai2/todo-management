package com.vijay.todo_management.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

// TODO: remove after allowlist task lands — this endpoint is a throwaway connectivity probe.
@RestController
@RequestMapping("/api/_internal")
public class RedisHealthController {

    private static final String HEALTH_KEY = "health:check";
    private static final Duration HEALTH_TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;

    public RedisHealthController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Writes a timestamped value to Redis with a 30-second TTL, reads it back,
     * and confirms round-trip connectivity.
     *
     * <p>Returns 200 {"redis":"connected","value":"<timestamp>"} on success.
     * <p>Returns 503 {"redis":"error","message":"<cause>"} on any failure.
     *
     * TODO: remove after allowlist task lands.
     */
    @GetMapping("/redis-health")
    public ResponseEntity<Map<String, String>> redisHealth() {
        try {
            String value = Instant.now().toString();
            redisTemplate.opsForValue().set(HEALTH_KEY, value, HEALTH_TTL);
            String readBack = redisTemplate.opsForValue().get(HEALTH_KEY);
            return ResponseEntity.ok(Map.of(
                    "redis", "connected",
                    "value", readBack != null ? readBack : "(null)"
            ));
        } catch (Exception ex) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "redis", "error",
                            "message", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()
                    ));
        }
    }
}
