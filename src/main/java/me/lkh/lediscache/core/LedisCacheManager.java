package me.lkh.lediscache.core;

import org.springframework.data.redis.core.RedisTemplate;

public class LedisCacheManager<K, V> {

    private final ValueLedisCacheTemplate valueLedisCacheTemplate;

    public LedisCacheManager(RedisTemplate redisTemplate) {
        this.valueLedisCacheTemplate = new ValueLedisCacheTemplate(redisTemplate);
    }

    public ValueLedisCacheTemplate getValueLedisCacheTemplate() {
        return valueLedisCacheTemplate;
    }
}
