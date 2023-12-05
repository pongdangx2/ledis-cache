package me.lkh.lediscache.core.domain.value;

/**
 * 캐시에 데이터를 저장하기 전 원본 저장소에 데이터를 저장하는 기능을 추상화한 인터페이스
 * @author lee-kh
 * @version 0.1
 */
public interface ToOrigin {
    /**
     * 원본 저장소에 데이터를 저장
     * @return 원본 저장소에 데이터 저장 성공 여부(true: 성공/false:실패)
     */
    boolean saveToOrigin();
}
