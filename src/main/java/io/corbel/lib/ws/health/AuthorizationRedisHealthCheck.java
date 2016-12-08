package io.corbel.lib.ws.health;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import com.codahale.metrics.health.HealthCheck;
import com.google.gson.JsonObject;

/**
 * @author Rubén Carrasco
 *
 */
public class AuthorizationRedisHealthCheck extends HealthCheck {

    private final RedisTemplate<String, JsonObject> redisTemplate;

    public AuthorizationRedisHealthCheck(RedisTemplate<String, JsonObject> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected Result check() throws Exception {
        RedisConnection redis = redisTemplate.getConnectionFactory().getConnection();
        try {
            if (redis.ping().equals("PONG")
                    && redisTemplate.opsForSet().members("fake").size() == 0) {
                return Result.healthy();
            }
            return Result.unhealthy("Redis is down");
        }
        finally {
            redis.close();
        }
    }
}
