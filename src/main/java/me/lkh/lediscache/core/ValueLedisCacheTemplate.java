package me.lkh.lediscache.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.lkh.lediscache.core.domain.FromOrigin;
import me.lkh.lediscache.core.domain.ToOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

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
     * @param key
     * @param valueClass
     * @param fromOrigin
     * @return
     * @throws JsonProcessingException
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
     * @param key
     * @param valueClass
     * @return
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
     * @return
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
