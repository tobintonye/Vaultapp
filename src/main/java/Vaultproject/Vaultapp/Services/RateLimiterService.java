package Vaultproject.Vaultapp.Services;

import java.time.Duration;
import java.util.Collections;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    private final String LUA_SCRIPT = 
        "local window_start = tonumber(ARGV[1]) - tonumber(ARGV[2]) " +
        "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, window_start) " +
        "local current_count = redis.call('ZCARD', KEYS[1]) " +
        "if current_count < tonumber(ARGV[3]) then " +
        "    redis.call('ZADD', KEYS[1], ARGV[1], ARGV[1]) " +
        "    redis.call('EXPIRE', KEYS[1], math.floor(tonumber(ARGV[2]) / 1000)) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        try {
               DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();

        // Execution
        Long result = redisTemplate.execute(
            redisScript,
            Collections.singletonList(key),
            String.valueOf(now),
            String.valueOf(windowMillis),
            String.valueOf(maxRequests)
        );

        return result != null && result == 1;
        
        } catch (Exception e) {
            log.error("Redis connection failed: {}", e.getMessage());
            return true; 
        }
    }
    
}

    /* 
         
    fixed window counter not secured
    private final RedisTemplate<String, Integer> redisTemplate;
    
    public RateLimiterService(RedisTemplate<String, Integer> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequest, Duration window) {
        Integer currentCount = redisTemplate.opsForValue().get(key);
        if(currentCount == null) {
            //create key for the first request
            redisTemplate.opsForValue().set(key, 1, window);
            return true;
        }

        if( currentCount >= maxRequest ) {
            return false;
        }

        redisTemplate.opsForValue().increment(key);
        return true;
    }
         */

