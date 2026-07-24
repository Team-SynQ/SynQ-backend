package com.synq.backend.domain.ai.rag.search;

/**
 * 코사인 유사도 임계값의 유효 범위.
 *
 * 범위 밖 값은 에러 없이 조용히 결과를 망가뜨린다. 1 을 넘으면 어떤 청크도 통과하지 못해
 * 항상 0건이고, -1 미만이면 필터가 무의미해진다. NaN 은 비교가 항상 거짓이라 역시 0건이다.
 * 설정과 요청 양쪽이 같은 규칙을 써야 하므로 여기 한 곳에 둔다.
 */
final class SimilarityThreshold {

	private static final double MIN = -1.0;
	private static final double MAX = 1.0;

	private SimilarityThreshold() {
	}

	static boolean isValid(double value) {
		return Double.isFinite(value) && value >= MIN && value <= MAX;
	}

	static String describe(double value) {
		return "minSimilarity 는 %s 이상 %s 이하의 유한한 값이어야 합니다: %s".formatted(MIN, MAX, value);
	}
}
