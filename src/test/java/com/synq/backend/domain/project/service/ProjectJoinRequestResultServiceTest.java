package com.synq.backend.domain.project.service;

import com.synq.backend.domain.project.dto.ProjectJoinRequestResultResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ProjectJoinRequestResultServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectParticipationRequestRepository participationRequestRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 내_APPROVED_REJECTED_요청만_처리_시각과_ID_역순으로_조회한다() {
		User requester = saveUser("요청자", "result-requester@synq.com");
		User otherUser = saveUser("다른 사용자", "result-other@synq.com");
		Project firstProject = saveProject(requester, "SynQ");
		Project secondProject = saveProject(requester, "UMC");
		Project pendingProject = saveProject(requester, "대기 프로젝트");
		ProjectParticipationRequest approved = savePending(firstProject, requester);
		ProjectParticipationRequest rejected = savePending(secondProject, requester);
		ProjectParticipationRequest pending = savePending(pendingProject, requester);
		ProjectParticipationRequest otherRequest = savePending(firstProject, otherUser);
		approved.approve();
		rejected.reject();
		otherRequest.reject();
		participationRequestRepository.flush();

		LocalDateTime decidedAt = LocalDateTime.of(2026, 8, 13, 14, 10);
		setUpdatedAt(approved.getId(), decidedAt);
		setUpdatedAt(rejected.getId(), decidedAt);
		entityManager.clear();

		List<ProjectJoinRequestResultResponse> responses =
				projectService.findMyJoinRequestResults(requester.getUserId());

		assertThat(responses).hasSize(2);
		assertThat(responses).extracting(ProjectJoinRequestResultResponse::requestId)
				.containsExactly(rejected.getId(), approved.getId());
		assertThat(responses).extracting(ProjectJoinRequestResultResponse::projectId)
				.containsExactly(secondProject.getId(), firstProject.getId());
		assertThat(responses).extracting(ProjectJoinRequestResultResponse::projectTitle)
				.containsExactly("UMC", "SynQ");
		assertThat(responses).extracting(ProjectJoinRequestResultResponse::status)
				.containsExactly("REJECTED", "APPROVED");
		assertThat(responses).extracting(ProjectJoinRequestResultResponse::decidedAt)
				.containsOnly(OffsetDateTime.of(2026, 8, 13, 5, 10, 0, 0, ZoneOffset.UTC));
		assertThat(responses).extracting(ProjectJoinRequestResultResponse::requestId)
				.doesNotContain(pending.getId(), otherRequest.getId());
	}

	@Test
	void 처리된_요청이_없으면_빈_배열을_반환한다() {
		User requester = saveUser("요청자", "result-empty@synq.com");
		Project project = saveProject(requester, "SynQ");
		savePending(project, requester);

		assertThat(projectService.findMyJoinRequestResults(requester.getUserId())).isEmpty();
	}

	@Test
	void 삭제된_프로젝트의_처리_결과는_조회하지_않는다() {
		User requester = saveUser("요청자", "result-deleted@synq.com");
		Project activeProject = saveProject(requester, "활성 프로젝트");
		Project deletedProject = saveProject(requester, "삭제 프로젝트");
		ProjectParticipationRequest activeRequest = savePending(activeProject, requester);
		ProjectParticipationRequest deletedRequest = savePending(deletedProject, requester);
		activeRequest.reject();
		deletedRequest.reject();
		deletedProject.softDelete();
		participationRequestRepository.flush();
		projectRepository.flush();
		entityManager.clear();

		List<ProjectJoinRequestResultResponse> responses =
				projectService.findMyJoinRequestResults(requester.getUserId());

		assertThat(responses).extracting(ProjectJoinRequestResultResponse::requestId)
				.containsExactly(activeRequest.getId());
	}

	@Test
	void 승인과_거절_상태_변경_시_updatedAt이_갱신되어_decidedAt으로_반환된다() {
		User owner = saveUser("소유자", "result-audit-owner@synq.com");
		User requester = saveUser("요청자", "result-audit-requester@synq.com");
		Project approvedProject = saveProject(owner, "승인 프로젝트");
		Project rejectedProject = saveProject(owner, "거절 프로젝트");
		ProjectParticipationRequest approvedRequest = savePending(approvedProject, requester);
		ProjectParticipationRequest rejectedRequest = savePending(rejectedProject, requester);
		participationRequestRepository.flush();
		LocalDateTime previousUpdatedAt = LocalDateTime.of(2020, 1, 1, 0, 0);
		setUpdatedAt(approvedRequest.getId(), previousUpdatedAt);
		setUpdatedAt(rejectedRequest.getId(), previousUpdatedAt);
		entityManager.clear();

		Instant beforeDecision = Instant.now();
		projectService.approveJoinRequest(
				approvedProject.getId(), approvedRequest.getId(), owner.getUserId());
		projectService.rejectJoinRequest(
				rejectedProject.getId(), rejectedRequest.getId(), owner.getUserId());
		participationRequestRepository.flush();
		Instant afterDecision = Instant.now();
		entityManager.clear();

		ProjectParticipationRequest processedApproved = participationRequestRepository
				.findById(approvedRequest.getId()).orElseThrow();
		ProjectParticipationRequest processedRejected = participationRequestRepository
				.findById(rejectedRequest.getId()).orElseThrow();
		List<ProjectJoinRequestResultResponse> responses =
				projectService.findMyJoinRequestResults(requester.getUserId());
		Instant approvedDecisionInstant = findDecisionInstant(approvedRequest.getId());
		Instant rejectedDecisionInstant = findDecisionInstant(rejectedRequest.getId());
		assertThat(processedApproved.getUpdatedAt()).isAfter(previousUpdatedAt);
		assertThat(processedRejected.getUpdatedAt()).isAfter(previousUpdatedAt);
		assertThat(approvedDecisionInstant).isBetween(beforeDecision, afterDecision);
		assertThat(rejectedDecisionInstant).isBetween(beforeDecision, afterDecision);
		assertThat(responses).filteredOn(response -> response.requestId().equals(approvedRequest.getId()))
				.singleElement()
				.extracting(ProjectJoinRequestResultResponse::decidedAt)
				.satisfies(decidedAt -> assertThat(decidedAt.toInstant()).isEqualTo(approvedDecisionInstant));
		assertThat(responses).filteredOn(response -> response.requestId().equals(rejectedRequest.getId()))
				.singleElement()
				.extracting(ProjectJoinRequestResultResponse::decidedAt)
				.satisfies(decidedAt -> assertThat(decidedAt.toInstant()).isEqualTo(rejectedDecisionInstant));
	}

	private Instant findDecisionInstant(Long requestId) {
		return jdbcTemplate.queryForObject(
				"SELECT updated_at AT TIME ZONE 'Asia/Seoul' FROM project_join_request WHERE id = ?",
				(resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class).toInstant(),
				requestId
		);
	}

	private void setUpdatedAt(Long requestId, LocalDateTime updatedAt) {
		jdbcTemplate.update(
				"UPDATE project_join_request SET updated_at = CAST(? AS timestamp) WHERE id = ?",
				updatedAt.toString(),
				requestId
		);
	}

	private Project saveProject(User owner, String title) {
		return projectRepository.save(Project.of(owner.getUserId(), title, null));
	}

	private ProjectParticipationRequest savePending(Project project, User requester) {
		return participationRequestRepository.save(ProjectParticipationRequest.pending(
				project.getId(),
				requester.getUserId(),
				ProjectJoinSettingSource.DEFAULT,
				Role.DEV_TECH,
				null
		));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
