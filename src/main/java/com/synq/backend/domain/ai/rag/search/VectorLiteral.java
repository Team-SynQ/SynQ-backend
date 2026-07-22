package com.synq.backend.domain.ai.rag.search;

/**
 * float[] 을 pgvector 가 이해하는 문자열 리터럴로 바꾼다.
 *
 * <p>네이티브 쿼리에 float[] 을 직접 바인딩하는 것은 JDBC 드라이버 동작에 의존해 깨지기 쉽다.
 * 문자열로 넘기고 SQL 쪽에서 CAST(... AS vector) 하는 편이 예측 가능하다.
 */
public final class VectorLiteral {

	private VectorLiteral() {
	}

	/** 예: {@code [1.0,0.0,0.0]} */
	public static String of(float[] vector) {
		if (vector == null || vector.length == 0) {
			throw new IllegalArgumentException("빈 벡터는 리터럴로 만들 수 없습니다.");
		}

		StringBuilder builder = new StringBuilder(vector.length * 12 + 2);
		builder.append('[');
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				builder.append(',');
			}
			builder.append(vector[i]);
		}
		builder.append(']');
		return builder.toString();
	}
}
