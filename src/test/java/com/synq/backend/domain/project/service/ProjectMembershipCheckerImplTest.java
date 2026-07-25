package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectMembershipCheckerImplTest {

	private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
	private final ProjectMembershipCheckerImpl projectMembershipChecker =
			new ProjectMembershipCheckerImpl(projectMemberRepository);

	@Test
	void 프로젝트_멤버이면_true를_반환한다() {
		when(projectMemberRepository.existsByProjectIdAndUserId(1L, 10L)).thenReturn(true);

		assertThat(projectMembershipChecker.isMember(1L, 10L)).isTrue();
		verify(projectMemberRepository).existsByProjectIdAndUserId(1L, 10L);
	}

	@Test
	void 프로젝트_멤버가_아니면_false를_반환한다() {
		when(projectMemberRepository.existsByProjectIdAndUserId(1L, 10L)).thenReturn(false);

		assertThat(projectMembershipChecker.isMember(1L, 10L)).isFalse();
		verify(projectMemberRepository).existsByProjectIdAndUserId(1L, 10L);
	}
}
