package me.lkh.lediscache.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.lkh.lediscache.core.domain.value.FromOrigin;
import me.lkh.lediscache.core.domain.value.ToOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

/**
 * Redis String value를 캐싱하는 모듈
 * @param <K> key의 generic
 * @param <V> value의 generic
 * @author lee-kh
 * @version 0.1
 */
public class ValueLedisCacheTemplate<K, V> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    ValueLedisCacheTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 데이터 조회 (Look Aside Cache)
     * @param key 조회할 Key
     * @param valueClass 조회할 Value의 타입
     * @param fromOrigin 캐시 miss 시 원본 데이터를 조회하는 기능을 담당하는 인터페이스
     * @return 조회한 Value의 Optional
     * @throws JsonProcessingException
     * @see me.lkh.lediscache.core.domain.value.FromOrigin
     */
    public Optional<V> getValue(K key, Class<V> valueClass, FromOrigin fromOrigin) throws JsonProcessingException {

        Optional<V> cacheResult = getValueFromRedis(key, valueClass);
        // Cache Hit
        if(cacheResult.isPresent()){
            logger.debug("cache hit");
            return cacheResult;
        }
        // Cache Miss
        else {
            Optional<V> originData = fromOrigin.getDataFromOrigin(key, valueClass);
            // 원본 데이터 미존재
            if(originData.isEmpty()){
                logger.debug("origin not exists");
                return Optional.empty();
            } else {

                // 캐시에 데이터 추가
                if(setValue(key, originData.get(), () -> true)) {
                    logger.debug("save to cache");
                    return originData;
                }
                // 캐시에 데이터 추가 실패
                else {
                    logger.debug("failed to save cache");
                    return Optional.empty();
                }
            }
        }
    }

    /**
     * Redis 에서만 데이터 조회
     * @param key 조회할 Key
     * @param valueClass 조회할 Value의 타입
     * @return 조회한 Value의 Optional
     * @throws JsonProcessingException
     */
    public Optional<V> getValueFromRedis(K key, Class<V> valueClass) throws JsonProcessingException {
        Optional<V> result;
        String stringKey;
        try {
            stringKey = objectMapper.writeValueAsString(key);
        } catch(JsonProcessingException jsonProcessingException){
            return Optional.empty();
        }

        ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
        Optional<String> optionalStringResult = Optional.ofNullable(stringValueOperations.get(stringKey));

        // Cache miss
        if(optionalStringResult.isEmpty()){
            result = Optional.empty();
        }
        // Cahce hit
        else {
            result = Optional.ofNullable(objectMapper.readValue(optionalStringResult.get(), valueClass));
        }

        return result;
    }

    /**
     * Redis 캐시에 데이터 저장
     * @param key   저장할 key
     * @param value 저장할 value
     * @param toOrigin 원본 데이터 저장후 성공여부 반환
     * @return 캐시 저장소와 원본 저장소 모두에 데이터 저장 성공 여부
     * @see me.lkh.lediscache.core.domain.value.ToOrigin
     */
    public boolean setValue(K key, V value, ToOrigin toOrigin){
        if(toOrigin.saveToOrigin()) {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

            try {
                valueOperations.set(objectMapper.writeValueAsString(key), objectMapper.writeValueAsString(value));
            } catch (JsonProcessingException jsonProcessingException) {
                return false;
            }

        } else {
            return false;
        }
        return true;
    }
}
