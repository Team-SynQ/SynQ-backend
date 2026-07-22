package com.synq.backend.domain.ai.rag.search;

/**
 * 검색된 청크 한 건.
 *
 * referenceMaterialId 와 chunkIndex 를 함께 돌려주는 이유는, 호출자가
 * "이 답변은 어느 문서의 몇 번째 조각에서 나왔는가" 를 사용자에게 보여줄 수 있어야 하기 때문이다.
 *
 * @param similarity 코사인 유사도. 1 에 가까울수록 질의와 가깝다.
 */
public record ChunkMatch(
		Long chunkId,
		Long referenceMaterialId,
		int chunkIndex,
		String content,
		double similarity
) {
}
