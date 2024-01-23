package me.lkh.lediscache.core.domain.value;

import java.util.Map;
import java.util.Set;

/**
 * 캐시에 데이터가 존재하지 않을 경우, 원본데이터를 가져오는 역할을 추상화한 인터페이스
 * @author lee-kh
 * @version 0.1
 */
@FunctionalInterface
public interface MultipleFromOrigin {

    /**
     * 여러 개의 원본 데이터를 조회하여 반환하는 Method
     * @param keySet 캐시 miss된 Key의 집합
     * @param <K> key의 generic
     * @param <V> value의 generic
     * @return keySet에 있는 key와 key 로 조회한 Value의 Map (원본에 미존재 시 반환되는 Map 에는 포함되지 않아야 한다.)
     */
    <K, V> Map<K, V> getMultiDataFromOrigin(Set<K> keySet);

}
