package com.vijay.todo_management.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RedisSessionService {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionService.class);
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String USER_REFRESH_KEY_PREFIX = "user_refresh:";

    public record RefreshTokenData(UUID userId, String device, String ip) {}

    private final RedisTemplate<String, String> redisTemplate;

    public RedisSessionService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(UUID userId, String jti) {
        return SESSION_KEY_PREFIX + userId + ":" + jti;
    }

    /**
     * Creates a new session in Redis using hash structure and sets TTL.
     * key: session:{userId}:{jti} -> {issuedAt, device, ip, lastSeenAt}
     */
    public void createSession(UUID userId, String jti, long ttlSeconds, String device, String ip) {
        if (userId == null || jti == null || jti.isBlank()) {
            return;
        }
        String key = buildKey(userId, jti);
        String now = Instant.now().toString();

        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("issuedAt", now);
        sessionData.put("device", device != null ? device : "unknown");
        sessionData.put("ip", ip != null ? ip : "unknown");
        sessionData.put("lastSeenAt", now);

        try {
            redisTemplate.opsForHash().putAll(key, sessionData);
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
            log.debug("Created Redis session for user {} with jti {}", userId, jti);
        } catch (Exception ex) {
            log.error("Failed to create Redis session for user {}: {}", userId, ex.getMessage());
        }
    }

    /**
     * Touches the session by updating lastSeenAt (and optionally ip) without modifying TTL.
     * Fire-and-forget: logs errors but does not throw.
     */
    public void touchSession(UUID userId, String jti, String ip) {
        if (userId == null || jti == null || jti.isBlank()) {
            return;
        }
        String key = buildKey(userId, jti);
        String now = Instant.now().toString();

        try {
            redisTemplate.opsForHash().put(key, "lastSeenAt", now);
            if (ip != null && !ip.isBlank()) {
                redisTemplate.opsForHash().put(key, "ip", ip);
            }
        } catch (Exception ex) {
            log.warn("Failed to touch Redis session for user {}: {}", userId, ex.getMessage());
        }
    }

    /**
     * Checks whether the session exists in Redis.
     */
    public boolean isValidSession(UUID userId, String jti) {
        if (userId == null || jti == null || jti.isBlank()) {
            return false;
        }
        String key = buildKey(userId, jti);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception ex) {
            log.error("Error verifying Redis session for user {}: {}", userId, ex.getMessage());
            return false;
        }
    }

    /**
     * Deletes a single session key on logout or token rotation.
     */
    public void deleteSession(UUID userId, String jti) {
        if (userId == null || jti == null || jti.isBlank()) {
            return;
        }
        String key = buildKey(userId, jti);
        try {
            redisTemplate.delete(key);
            log.debug("Deleted Redis session {}", key);
        } catch (Exception ex) {
            log.error("Failed to delete Redis session {}: {}", key, ex.getMessage());
        }
    }

    /**
     * Deletes all sessions for a user using non-blocking SCAN.
     */
    public void deleteAllSessions(UUID userId) {
        if (userId == null) {
            return;
        }
        String pattern = SESSION_KEY_PREFIX + userId + ":*";
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            Set<String> keysToDelete = new HashSet<>();

            redisTemplate.execute((RedisCallback<Void>) connection -> {
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                        keysToDelete.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return null;
            });

            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.info("Deleted {} Redis sessions for user {}", keysToDelete.size(), userId);
            }
        } catch (Exception ex) {
            log.error("Failed to delete all Redis sessions for user {}: {}", userId, ex.getMessage());
        }

        // Also clean up all refresh tokens for the user
        String refreshPattern = USER_REFRESH_KEY_PREFIX + userId + ":*";
        try {
            ScanOptions options = ScanOptions.scanOptions().match(refreshPattern).count(100).build();
            Set<String> userRefreshKeys = new HashSet<>();
            Set<String> tokenKeys = new HashSet<>();

            redisTemplate.execute((RedisCallback<Void>) connection -> {
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                        String key = new String(cursor.next(), StandardCharsets.UTF_8);
                        userRefreshKeys.add(key);
                        // Extract tokenHash from user_refresh:{userId}:{tokenHash}
                        int lastColon = key.lastIndexOf(':');
                        if (lastColon != -1 && lastColon + 1 < key.length()) {
                            tokenKeys.add(REFRESH_KEY_PREFIX + key.substring(lastColon + 1));
                        }
                    }
                }
                return null;
            });

            if (!tokenKeys.isEmpty()) {
                redisTemplate.delete(tokenKeys);
            }
            if (!userRefreshKeys.isEmpty()) {
                redisTemplate.delete(userRefreshKeys);
                log.info("Deleted {} Redis refresh tokens for user {}", userRefreshKeys.size(), userId);
            }
        } catch (Exception ex) {
            log.error("Failed to delete all Redis refresh tokens for user {}: {}", userId, ex.getMessage());
        }
    }

    public void createRefreshToken(UUID userId, String rawToken, long ttlSeconds, String device, String ip) {
        if (userId == null || rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = com.vijay.todo_management.util.TokenHasher.sha256(rawToken);
        String refreshKey = REFRESH_KEY_PREFIX + tokenHash;
        String userRefreshKey = USER_REFRESH_KEY_PREFIX + userId + ":" + tokenHash;
        String now = Instant.now().toString();

        Map<String, String> data = new HashMap<>();
        data.put("userId", userId.toString());
        data.put("device", device != null ? device : "unknown");
        data.put("ip", ip != null ? ip : "unknown");
        data.put("createdAt", now);

        try {
            redisTemplate.opsForHash().putAll(refreshKey, data);
            redisTemplate.expire(refreshKey, Duration.ofSeconds(ttlSeconds));

            redisTemplate.opsForValue().set(userRefreshKey, tokenHash, Duration.ofSeconds(ttlSeconds));
            log.debug("Created Redis refresh token for user {}", userId);
        } catch (Exception ex) {
            log.error("Failed to create Redis refresh token for user {}: {}", userId, ex.getMessage());
        }
    }

    public RefreshTokenData validateAndConsumeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        String tokenHash = com.vijay.todo_management.util.TokenHasher.sha256(rawToken);
        String refreshKey = REFRESH_KEY_PREFIX + tokenHash;

        try {
            Map<Object, Object> data = redisTemplate.opsForHash().entries(refreshKey);
            if (data == null || data.isEmpty()) {
                return null;
            }
            String userIdStr = (String) data.get("userId");
            String device = (String) data.get("device");
            String ip = (String) data.get("ip");

            if (userIdStr == null || userIdStr.isBlank()) {
                return null;
            }
            UUID userId = UUID.fromString(userIdStr);

            // Invalidate/consume old refresh token for token rotation
            redisTemplate.delete(refreshKey);
            redisTemplate.delete(USER_REFRESH_KEY_PREFIX + userId + ":" + tokenHash);

            return new RefreshTokenData(userId, device, ip);
        } catch (Exception ex) {
            log.error("Error validating and consuming refresh token: {}", ex.getMessage());
            return null;
        }
    }

    public void deleteRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = com.vijay.todo_management.util.TokenHasher.sha256(rawToken);
        String refreshKey = REFRESH_KEY_PREFIX + tokenHash;

        try {
            Object userIdObj = redisTemplate.opsForHash().get(refreshKey, "userId");
            redisTemplate.delete(refreshKey);
            if (userIdObj instanceof String userIdStr && !userIdStr.isBlank()) {
                redisTemplate.delete(USER_REFRESH_KEY_PREFIX + UUID.fromString(userIdStr) + ":" + tokenHash);
            }
            log.debug("Deleted Redis refresh token {}", tokenHash);
        } catch (Exception ex) {
            log.error("Failed to delete Redis refresh token: {}", ex.getMessage());
        }
    }
}
