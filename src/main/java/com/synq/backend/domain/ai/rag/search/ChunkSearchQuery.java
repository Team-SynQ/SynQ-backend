package com.synq.backend.domain.ai.rag.search;

/**
 * 청크 검색 입력.
 *
 * @param projectId     검색 스코프. 다른 프로젝트의 청크는 결과에 들어가지 않는다.
 * @param query         자연어 질의
 * @param topK          최대 결과 개수
 * @param minSimilarity 이 값 미만은 제외한다. 코사인 유사도이므로 범위는 [-1, 1] 이다.
 */
public record ChunkSearchQuery(
		Long projectId,
		String query,
		int topK,
		double minSimilarity
) {

	public ChunkSearchQuery {
		if (projectId == null) {
			throw new IllegalArgumentException("검색 스코프인 projectId 는 필수입니다.");
		}
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException("검색 질의가 비어 있습니다.");
		}
		if (topK <= 0) {
			throw new IllegalArgumentException("topK 는 1 이상이어야 합니다: " + topK);
		}
	}
}
