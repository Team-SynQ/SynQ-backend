package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.dto.ProjectUpdateRequest;
import com.synq.backend.domain.project.dto.ProjectUpdateResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectUpdateServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER가_title만_수정한다() {
		User owner = saveUser("update-title-owner@synq.com");
		Project project = saveProject(owner, "기존 제목", "기존 설명");
		var previousUpdatedAt = project.getUpdatedAt();
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setTitle("  수정 제목  ");

		ProjectUpdateResponse response = projectService.update(project.getId(), owner.getUserId(), request);

		assertThat(response.title()).isEqualTo("수정 제목");
		assertThat(response.description()).isEqualTo("기존 설명");
		assertThat(response.updatedAt()).isEqualTo(project.getUpdatedAt());
		assertThat(response.updatedAt()).isAfter(previousUpdatedAt);
	}

	@Test
	void OWNER가_description만_수정한다() {
		User owner = saveUser("update-description-owner@synq.com");
		Project project = saveProject(owner, "기존 제목", "기존 설명");
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setDescription("수정 설명");

		ProjectUpdateResponse response = projectService.update(project.getId(), owner.getUserId(), request);

		assertThat(response.title()).isEqualTo("기존 제목");
		assertThat(response.description()).isEqualTo("수정 설명");
	}

	@Test
	void OWNER가_title과_description을_모두_수정한다() {
		User owner = saveUser("update-all-owner@synq.com");
		Project project = saveProject(owner, "기존 제목", "기존 설명");
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setTitle("수정 제목");
		request.setDescription("수정 설명");

		ProjectUpdateResponse response = projectService.update(project.getId(), owner.getUserId(), request);

		assertThat(response.title()).isEqualTo("수정 제목");
		assertThat(response.description()).isEqualTo("수정 설명");
	}

	@Test
	void description에_null이_포함되면_설명을_제거한다() {
		User owner = saveUser("update-null-description-owner@synq.com");
		Project project = saveProject(owner, "기존 제목", "기존 설명");
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setDescription(null);

		ProjectUpdateResponse response = projectService.update(project.getId(), owner.getUserId(), request);

		assertThat(response.description()).isNull();
	}

	@Test
	void 수정할_필드가_없으면_400_예외를_발생시킨다() {
		User owner = saveUser("update-empty-owner@synq.com");
		Project project = saveProject(owner, "기존 제목", "기존 설명");

		assertThatThrownBy(() -> projectService.update(
				project.getId(), owner.getUserId(), new ProjectUpdateRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(GeneralErrorCode.BAD_REQUEST));
	}

	@Test
	void OWNER가_아니면_403_예외를_발생시킨다() {
		User owner = saveUser("update-forbidden-owner@synq.com");
		User member = saveUser("update-forbidden-member@synq.com");
		Project project = saveProject(owner, "기존 제목", null);
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setTitle("수정 제목");

		assertThatThrownBy(() -> projectService.update(project.getId(), member.getUserId(), request))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_OWNER));
	}

	@Test
	void 존재하지_않는_프로젝트면_404_예외를_발생시킨다() {
		User owner = saveUser("update-missing-project@synq.com");
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setTitle("수정 제목");

		assertThatThrownBy(() -> projectService.update(Long.MAX_VALUE, owner.getUserId(), request))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void 존재하지_않는_사용자면_기존_USER_NOT_FOUND_예외를_발생시킨다() {
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setTitle("수정 제목");

		assertThatThrownBy(() -> projectService.update(1L, Long.MAX_VALUE, request))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
	}

	private Project saveProject(User owner, String title, String description) {
		return projectRepository.save(Project.of(owner.getUserId(), title, description));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
