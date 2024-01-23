package me.lkh.lediscache.core.domain.value;

import java.util.Optional;

/**
 * 캐시에 데이터가 존재하지 않을 경우, 원본데이터를 가져오는 역할을 추상화한 인터페이스
 * @author lee-kh
 * @version 0.1
 */
@FunctionalInterface
public interface FromOrigin {

    /**
     * 하나의 원본데이터를 반환하는 Method
     * @param key 조회할 Key
     * @param valueClass 조회할 Value의 타입
     * @param <K> key의 generic
     * @param <V> value의 generic
     * @return 조회한 Value의 Optional
     */
    <K, V> Optional<V> getDataFromOrigin(K key, Class<V> valueClass);

}
