package com.synq.backend.domain.project.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectInvitationPropertiesTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void 도메인만_있는_프론트엔드_기본_URL을_허용한다() {
		assertThat(validator.validate(
				new ProjectInvitationProperties("https://synqai.co.kr", 7)))
				.isEmpty();
		assertThat(validator.validate(
				new ProjectInvitationProperties("https://synqai.co.kr/", 7)))
				.isEmpty();
	}

	@Test
	void login_경로가_포함된_프론트엔드_기본_URL을_거부한다() {
		assertThat(validator.validate(
				new ProjectInvitationProperties("https://synqai.co.kr/login", 7)))
				.singleElement()
				.satisfies(violation -> assertThat(violation.getPropertyPath().toString())
						.isEqualTo("frontendBaseUrl"));
	}
}
