package com.synq.backend.domain.ai.rag.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkSearchPropertiesTest {

	@ParameterizedTest
	@ValueSource(doubles = {1.5, -3.0, Double.NaN, Double.POSITIVE_INFINITY})
	void 코사인_유사도_범위_밖_설정은_기동을_막는다(double invalid) {
		// 범위 밖 값은 에러 없이 검색을 무력화한다. 설정 오타를 기동 시점에 잡아야 한다.
		assertThatThrownBy(() -> new ChunkSearchProperties(5, invalid))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("minSimilarity");
	}

	@ParameterizedTest
	@ValueSource(doubles = {-1.0, 0.0, 0.5, 1.0})
	void 유효한_임계값은_허용한다(double valid) {
		assertThatCode(() -> new ChunkSearchProperties(5, valid)).doesNotThrowAnyException();
	}

	@Test
	void 설정값을_그대로_노출한다() {
		ChunkSearchProperties properties = new ChunkSearchProperties(5, 0.5);

		assertThat(properties.topK()).isEqualTo(5);
		assertThat(properties.minSimilarity()).isEqualTo(0.5);
	}
}
