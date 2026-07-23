package com.synq.backend.domain.ai.rag.search;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml 의 ai.rag.search.* 를 바인딩한다.
 * BackendApplication 에 @ConfigurationPropertiesScan 이 있어 별도 등록이 필요 없다.
 *
 * @param minSimilarity 코사인 유사도 임계값. 실제 문서로 돌려보며 조정할 값이다.
 */
@Validated
@ConfigurationProperties(prefix = "ai.rag.search")
public record ChunkSearchProperties(
		@Positive int topK,
		double minSimilarity
) {

	public ChunkSearchProperties {
		// 오타 하나(예: 5)로 전체 검색이 항상 0건이 되는데 서버는 멀쩡히 뜬다. 기동 시점에 막는다.
		if (!SimilarityThreshold.isValid(minSimilarity)) {
			throw new IllegalArgumentException(SimilarityThreshold.describe(minSimilarity));
		}
	}
}
