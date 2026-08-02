package com.synq.backend.domain.reference.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceLinkCreateRequestTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void HTTP와_HTTPS_URL을_허용한다() {
		assertThat(validator.validate(new ReferenceLinkCreateRequest("http://example.com"))).isEmpty();
		assertThat(validator.validate(new ReferenceLinkCreateRequest("https://example.com/path"))).isEmpty();
	}

	@Test
	void URL의_앞뒤_공백을_제거한다() {
		ReferenceLinkCreateRequest request = new ReferenceLinkCreateRequest("  https://example.com/path  ");

		assertThat(request.url()).isEqualTo("https://example.com/path");
		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void null과_빈_문자열과_공백만_있는_URL을_거부한다() {
		assertThat(validator.validate(new ReferenceLinkCreateRequest(null))).isNotEmpty();
		assertThat(validator.validate(new ReferenceLinkCreateRequest(""))).isNotEmpty();
		assertThat(validator.validate(new ReferenceLinkCreateRequest("   "))).isNotEmpty();
	}

	@Test
	void 길이가_2000자인_URL은_허용하고_초과하면_거부한다() {
		String prefix = "https://example.com/";
		String maxLengthUrl = prefix + "a".repeat(2000 - prefix.length());

		assertThat(maxLengthUrl).hasSize(2000);
		assertThat(validator.validate(new ReferenceLinkCreateRequest(maxLengthUrl))).isEmpty();
		assertThat(validator.validate(new ReferenceLinkCreateRequest(maxLengthUrl + "a"))).isNotEmpty();
	}

	@Test
	void HTTP와_HTTPS_외_scheme을_거부한다() {
		assertThat(validator.validate(new ReferenceLinkCreateRequest("ftp://example.com/file"))).isNotEmpty();
		assertThat(validator.validate(new ReferenceLinkCreateRequest("javascript:alert(1)"))).isNotEmpty();
	}

	@Test
	void host가_없는_URL을_거부한다() {
		assertThat(validator.validate(new ReferenceLinkCreateRequest("http:///path"))).isNotEmpty();
	}

	@Test
	void 상대_URL을_거부한다() {
		assertThat(validator.validate(new ReferenceLinkCreateRequest("/documents/example"))).isNotEmpty();
	}

	@Test
	void URI_파싱이_불가능한_URL을_거부한다() {
		assertThat(validator.validate(new ReferenceLinkCreateRequest("https://exa mple.com"))).isNotEmpty();
	}
}
