package me.lkh.lediscache.core.domain;

import java.util.Optional;

public interface FromOrigin {
    <K, V> Optional<V> getDataFromOrigin(K key, Class<V> valueClass);
}
