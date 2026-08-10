package com.synq.backend.domain.ai.assistant.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AssistantHintPropertiesTest {

	@ParameterizedTest
	@ValueSource(ints = {-1, -5})
	void searchWindow_가_음수면_기동이_실패한다(int invalid) {
		assertThatThrownBy(() -> new AssistantHintProperties(2, 2, invalid, 3, 3, 0.6))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("검색 질의 윈도우");
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1})
	void focusRepeat_이_1_미만이면_기동이_실패한다(int invalid) {
		assertThatThrownBy(() -> new AssistantHintProperties(2, 2, 1, invalid, 3, 0.6))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("focus-repeat");
	}

	@Test
	void searchWindow_0_과_focusRepeat_1_은_허용한다() {
		assertThatCode(() -> new AssistantHintProperties(2, 2, 0, 1, 3, 0.6))
				.doesNotThrowAnyException();
	}

	@Test
	void 기존_검증은_그대로_동작한다() {
		assertThatThrownBy(() -> new AssistantHintProperties(-1, 2, 1, 3, 3, 0.6))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("윈도우 크기");
		assertThatThrownBy(() -> new AssistantHintProperties(2, 2, 1, 3, 0, 0.6))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("topK");
	}
}
