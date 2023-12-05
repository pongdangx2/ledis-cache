package me.lkh.lediscache.core;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * 각 CacheTemplate instance를 중복생성하지 않고 관리하기 위한 manager
 * @param <K> key의 generic
 * @param <V> value의 generic
 * @author lee-kh
 * @version 0.1
 */
public class LedisCacheManager<K, V> {

    private final ValueLedisCacheTemplate valueLedisCacheTemplate;

    public LedisCacheManager(RedisTemplate redisTemplate) {
        this.valueLedisCacheTemplate = new ValueLedisCacheTemplate(redisTemplate);
    }

    /**
     * ValueLedisCacheTemplate 를 얻기 위한 Method
     * @return 참조중인 ValueLedisCacheTemplate 인스턴스를 반환
     * @see me.lkh.lediscache.core.ValueLedisCacheTemplate
     */
    public ValueLedisCacheTemplate getValueLedisCacheTemplate() {
        return valueLedisCacheTemplate;
    }
}
