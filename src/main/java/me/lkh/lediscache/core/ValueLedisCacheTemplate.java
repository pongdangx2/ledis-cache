package me.lkh.lediscache.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.lkh.lediscache.core.domain.value.FromOrigin;
import me.lkh.lediscache.core.domain.value.MultipleFromOrigin;
import me.lkh.lediscache.core.domain.value.ToOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;

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
     * 하나의 데이터 조회 - 캐시를 먼저 조회하고 없으면 원본 저장소 조회
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

        ValueOperations<String, String> valueOperation = redisTemplate.opsForValue();
        Optional<String> optionalStringResult = Optional.ofNullable(valueOperation.get(stringKey));

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
     * Redis 캐시에 데이터 저장하고 만료시간을 설정
     * @param key   저장할 key
     * @param value 저장할 value
     * @param toOrigin 원본 데이터 저장후 성공여부 반환
     * @param timeOut 만료시킬 경과 시간
     * @param timeUnit 만료시킬 시간의 단위
     * @return 캐시 저장소와 원본 저장소 모두에 데이터 저장 성공 여부
     * @see me.lkh.lediscache.core.domain.value.ToOrigin
     */
    public boolean setValue(K key, V value, ToOrigin toOrigin, long timeOut, TimeUnit timeUnit){

        if(toOrigin.saveToOrigin()) {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

            try {
                if(timeOut == 0) {
                    valueOperations.set(objectMapper.writeValueAsString(key), objectMapper.writeValueAsString(value));
                } else {
                    valueOperations.set(objectMapper.writeValueAsString(key), objectMapper.writeValueAsString(value), timeOut, timeUnit);
                }
            } catch (JsonProcessingException jsonProcessingException) {
                return false;
            }

        } else {
            return false;
        }
        return true;
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
        return setValue(key, value, toOrigin, 0, null);
    }

    /**
     * 여러 key의 데이터를 조회 - 캐시를 먼저 조회하고 없으면 원본 저장소 조회
     * @param keySet 조회할 key의 Set
     * @param valueClass 조회할 Value의 타입
     * @param multipleFromOrigin 캐시 Miss된 key들을 원본 저장소에서 조회
     * @return 조회한 (key, value) Map
     * @throws JsonProcessingException
     * @see me.lkh.lediscache.core.domain.value.MultipleFromOrigin
     */
    public Map<K, V> getValue(Set<K> keySet, Class<V> valueClass, MultipleFromOrigin multipleFromOrigin) throws JsonProcessingException {

        // 1. redis 캐시 조회
        Map<K, V> result = getValueFromRedis(new ArrayList<>(keySet), valueClass);

        // 2. cache miss 된 key만 따로 보관
        Set<K> originKeySet = new HashSet<>();
        keySet.forEach(key -> {
            if(!result.containsKey(key)){
                originKeySet.add(key);
            }
        });

        // 3. cache miss된 데이터 원본 저장소에서 조회 및 캐시에 저장할 것들 필터링
        Map<String, String> cacheMap = new HashMap<>();
        Map<K, V> originMap = multipleFromOrigin.getMultiDataFromOrigin(originKeySet);
        for(K key: originMap.keySet()){
            V value = originMap.get(key);

            result.put(key, (V) value);
            cacheMap.put(objectMapper.writeValueAsString(key), objectMapper.writeValueAsString(value));
        }

        // 4. MultiSet
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.multiSet(cacheMap);

        return result;
    }

    private Map<K, V> getValueFromRedis(List<K> keyList, Class<V> valueClass) throws JsonProcessingException {
        Map<K, V> result = new HashMap<>();

        // 1. keyList의 element를 String으로 변환
        List<String> stringKeyList = new ArrayList<>();
        for(K key : keyList){
            stringKeyList.add(objectMapper.writeValueAsString(key));
        }

        // 2. redis multiGet
        ValueOperations<String, String> valueOperation = redisTemplate.opsForValue();
        List<String> values = valueOperation.multiGet(stringKeyList);

        // 3. 존재하는 것만 결과에 추가
        int len = keyList.size();
        for(int i = 0; i < len; i++){
            String value = values.get(i);
            if(value != null){
                V tmpValue = objectMapper.readValue(value, valueClass);
                result.put(keyList.get(i), tmpValue);
            }
        }

        return result;
    }

    /**
     * multiSet을 이용해 여러개의 key,value 데이터를 캐시 및 원본 데이터에 저장
     * @param data 저장할 (key, value) Map
     * @param toOrigin 원본 저장소에 데이터를 저장
     * @throws JsonProcessingException
     */
    public void setValue(Map<K, V> data, ToOrigin toOrigin) throws JsonProcessingException {
        setValue(data, toOrigin, 0, null);
    }

    /**
     *
     * multiSet을 이용해 여러개의 key,value 데이터를 캐시 및 원본 데이터에 저장하고 만료시간을 설정
     * @param data 저장할 (key, value) Map
     * @param toOrigin 원본 저장소에 데이터를 저장
     * @param timeOut
     * @param timeUnit
     * @throws JsonProcessingException
     */
    public void setValue(Map<K, V> data, ToOrigin toOrigin, long timeOut, TimeUnit timeUnit) throws JsonProcessingException {

        if(toOrigin.saveToOrigin()){
            Map<String, String> stringMap = new HashMap<>();
            for(K key : data.keySet()){
                stringMap.put(objectMapper.writeValueAsString(key), objectMapper.writeValueAsString(data.get(key)));
            }

            ValueOperations<String, String> valueOperation = redisTemplate.opsForValue();
            valueOperation.multiSet(stringMap);

            if(timeOut != 0) {
                stringMap.keySet().forEach(key -> valueOperation.getAndExpire(key, timeOut, timeUnit));
            }
        }

    }
}
