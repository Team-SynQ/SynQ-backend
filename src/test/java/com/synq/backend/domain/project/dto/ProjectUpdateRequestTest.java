package com.synq.backend.domain.project.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectUpdateRequestTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void 빈_객체는_수정_필드가_없는_요청으로_역직렬화한다() throws Exception {
		ProjectUpdateRequest request = objectMapper.readValue("{}", ProjectUpdateRequest.class);

		assertThat(request.hasTitle()).isFalse();
		assertThat(request.hasDescription()).isFalse();
		assertThat(request.isAnyFieldPresent()).isFalse();
	}

	@Test
	void description의_명시적_null을_포함된_필드로_역직렬화한다() throws Exception {
		ProjectUpdateRequest request = objectMapper.readValue(
				"{\"description\":null}", ProjectUpdateRequest.class);

		assertThat(request.hasDescription()).isTrue();
		assertThat(request.description()).isNull();
		assertThat(request.isAnyFieldPresent()).isTrue();
	}

	@Test
	void description의_빈_문자열을_포함된_필드로_역직렬화한다() throws Exception {
		ProjectUpdateRequest request = objectMapper.readValue(
				"{\"description\":\"\"}", ProjectUpdateRequest.class);

		assertThat(request.hasDescription()).isTrue();
		assertThat(request.description()).isEmpty();
		assertThat(request.isAnyFieldPresent()).isTrue();
	}

	@Test
	void title의_명시적_null은_포함됐지만_유효하지_않은_필드로_역직렬화한다() throws Exception {
		ProjectUpdateRequest request = objectMapper.readValue(
				"{\"title\":null}", ProjectUpdateRequest.class);

		assertThat(request.hasTitle()).isTrue();
		assertThat(request.title()).isNull();
		assertThat(request.isTitleValid()).isFalse();
	}

	@Test
	void title의_앞뒤_공백을_제거한다() throws Exception {
		ProjectUpdateRequest request = objectMapper.readValue(
				"{\"title\":\"  SynQ V2  \"}", ProjectUpdateRequest.class);

		assertThat(request.hasTitle()).isTrue();
		assertThat(request.title()).isEqualTo("SynQ V2");
		assertThat(request.isTitleValid()).isTrue();
	}

	@Test
	void 공백으로만_이루어진_title은_공백_제거_후_유효하지_않다() throws Exception {
		ProjectUpdateRequest request = objectMapper.readValue(
				"{\"title\":\"   \"}", ProjectUpdateRequest.class);

		assertThat(request.title()).isEmpty();
		assertThat(request.isTitleValid()).isFalse();
	}
}
