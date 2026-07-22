package com.synq.backend.domain.ai.rag.search;

import java.util.List;

/**
 * 자연어 질의로 청크를 찾는다.
 *
 * 인터페이스로 두는 이유는 두 가지다. 회의 전사 청크 검색기가 추가되면 같은 계약을
 * 구현하고 두 결과를 합치는 구현체를 하나 더 두면 호출자 코드가 바뀌지 않는다.
 * 그리고 3-hint / Chat 을 테스트할 때 실제 임베딩 API 호출 없이 대역으로 교체할 수 있다.
 */
public interface ChunkSearcher {

	/** 유사도 내림차순. 임계값 미만은 제외한다. 결과가 없으면 빈 리스트. */
	List<ChunkMatch> search(ChunkSearchQuery query);
}
