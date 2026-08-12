package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectJoinRequestStatus;
import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.project.entity.ProjectParticipationRequestPerspective;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProjectParticipationRequestRepositoryTest extends PostgresTestContainer {

	@Autowired
	private ProjectParticipationRequestRepository participationRequestRepository;

	@Autowired
	private ProjectParticipationRequestPerspectiveRepository perspectiveRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 프로젝트와_사용자의_PENDING_요청을_조회한다() {
		User owner = saveUser("repository-request-owner@synq.com");
		User requester = saveUser("repository-request-user@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		ProjectParticipationRequest request = participationRequestRepository.save(
				ProjectParticipationRequest.pending(
						project.getId(),
						requester.getUserId(),
						ProjectJoinSettingSource.DEFAULT,
						Role.DEV_TECH,
						null
				)
		);
		perspectiveRepository.save(ProjectParticipationRequestPerspective.of(request.getId(), Perspective.TECH_RISK));

		assertThat(participationRequestRepository.existsByProjectIdAndUserIdAndStatus(
				project.getId(), requester.getUserId(), ProjectJoinRequestStatus.PENDING)).isTrue();
		assertThat(participationRequestRepository.findByProjectIdAndUserIdAndStatus(
				project.getId(), requester.getUserId(), ProjectJoinRequestStatus.PENDING))
				.contains(request);
		assertThat(perspectiveRepository.findAllByJoinRequestIdOrderByIdAsc(request.getId()))
				.singleElement()
				.extracting(ProjectParticipationRequestPerspective::getPerspective)
				.isEqualTo(Perspective.TECH_RISK);
	}

	@Test
	void 동일_프로젝트와_사용자의_PENDING_요청은_DB에서_중복을_차단한다() {
		User owner = saveUser("repository-unique-owner@synq.com");
		User requester = saveUser("repository-unique-user@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		participationRequestRepository.saveAndFlush(pending(project, requester));

		assertThatThrownBy(() -> participationRequestRepository.saveAndFlush(pending(project, requester)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 프로젝트의_PENDING_요청만_요청_시각과_ID_오름차순으로_조회한다() {
		User owner = saveUser("repository-list-owner@synq.com");
		User firstUser = saveUser("repository-list-first@synq.com");
		User secondUser = saveUser("repository-list-second@synq.com");
		User processedUser = saveUser("repository-list-processed@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		ProjectParticipationRequest first = participationRequestRepository.save(pending(project, firstUser));
		ProjectParticipationRequest second = participationRequestRepository.save(pending(project, secondUser));
		ProjectParticipationRequest processed = participationRequestRepository.save(pending(project, processedUser));
		processed.approve();
		participationRequestRepository.flush();

		List<ProjectParticipationRequest> requests = participationRequestRepository
				.findAllByProjectIdAndStatusOrderByRequestedAtAscIdAsc(
						project.getId(), ProjectJoinRequestStatus.PENDING);

		assertThat(requests).extracting(ProjectParticipationRequest::getId)
				.containsExactly(first.getId(), second.getId());
		assertThat(participationRequestRepository.findByIdAndProjectId(first.getId(), project.getId()))
				.contains(first);
	}

	private ProjectParticipationRequest pending(Project project, User requester) {
		return ProjectParticipationRequest.pending(
				project.getId(),
				requester.getUserId(),
				ProjectJoinSettingSource.DEFAULT,
				Role.DEV_TECH,
				null
		);
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
