package com.synq.backend.domain.project.service;

import com.synq.backend.domain.meeting.mock.AlwaysMemberProjectMembershipChecker;
import com.synq.backend.domain.meeting.port.ProjectMembershipChecker;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProjectMembershipCheckerProfileTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void local_프로필에서는_Mock_Bean만_등록된다() {
		contextRunner.withPropertyValues("spring.profiles.active=local")
				.run(context -> {
					assertThat(context).hasSingleBean(ProjectMembershipChecker.class);
					assertThat(context.getBean(ProjectMembershipChecker.class))
							.isInstanceOf(AlwaysMemberProjectMembershipChecker.class);
				});
	}

	@Test
	void test_프로필에서는_Mock_Bean만_등록된다() {
		contextRunner.withPropertyValues("spring.profiles.active=test")
				.run(context -> {
					assertThat(context).hasSingleBean(ProjectMembershipChecker.class);
					assertThat(context.getBean(ProjectMembershipChecker.class))
							.isInstanceOf(AlwaysMemberProjectMembershipChecker.class);
				});
	}

	@Test
	void prod_프로필에서는_실제_Bean만_등록된다() {
		contextRunner.withPropertyValues("spring.profiles.active=prod")
				.run(context -> {
					assertThat(context).hasSingleBean(ProjectMembershipChecker.class);
					assertThat(context.getBean(ProjectMembershipChecker.class))
							.isInstanceOf(ProjectMembershipCheckerImpl.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	@Import({
			AlwaysMemberProjectMembershipChecker.class,
			ProjectMembershipCheckerImpl.class
	})
	static class TestConfiguration {

		@Bean
		ProjectMemberRepository projectMemberRepository() {
			return mock(ProjectMemberRepository.class);
		}
	}
}
