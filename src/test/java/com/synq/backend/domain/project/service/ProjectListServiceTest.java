package com.synq.backend.domain.project.service;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.project.dto.ProjectListResponse;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ProjectListServiceTest extends PostgresTestContainer {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER와_MEMBER로_참여한_프로젝트를_모두_최근_활동순으로_조회한다() {
		User user = saveUser("project-list@synq.com");
		User otherOwner = saveUser("other-owner@synq.com");
		User otherMember = saveUser("other-member@synq.com");
		Project ownedProject = saveProject(user.getUserId(), user.getUserId(), ProjectMemberRole.OWNER, "소유 프로젝트");
		Project joinedProject = saveProject(
				otherOwner.getUserId(), user.getUserId(), ProjectMemberRole.MEMBER, "참여 프로젝트");
		saveProject(
				otherOwner.getUserId(), otherMember.getUserId(), ProjectMemberRole.MEMBER, "참여하지 않은 프로젝트");
		Meeting recentMeeting = meetingRepository.save(Meeting.of(joinedProject.getId(), "최근 회의"));

		List<ProjectListResponse> responses = projectService.findAll(user.getUserId());

		assertThat(responses).hasSize(2);
		assertThat(responses).extracting(ProjectListResponse::title)
				.containsExactly("참여 프로젝트", "소유 프로젝트")
				.doesNotContain("참여하지 않은 프로젝트");
		assertThat(responses.get(0).projectId()).isEqualTo(joinedProject.getId());
		assertThat(responses.get(0).recentMeetingTitle()).isEqualTo("최근 회의");
		assertThat(responses.get(0).updatedAt()).isEqualTo(recentMeeting.getUpdatedAt());
		assertThat(responses.get(1).projectId()).isEqualTo(ownedProject.getId());
		assertThat(responses.get(1).recentMeetingTitle()).isNull();
	}

	@Test
	void 가장_최근에_시작한_회의_제목을_반환한다() {
		User user = saveUser("recent-meeting@synq.com");
		Project project = saveProject(
				user.getUserId(), user.getUserId(), ProjectMemberRole.OWNER, "회의 프로젝트");
		meetingRepository.save(Meeting.of(project.getId(), "이전 회의"));
		Meeting recentMeeting = meetingRepository.save(Meeting.of(project.getId(), "최근 회의"));

		ProjectListResponse response = projectService.findAll(user.getUserId()).get(0);

		assertThat(response.recentMeetingTitle()).isEqualTo("최근 회의");
		assertThat(response.updatedAt()).isEqualTo(recentMeeting.getUpdatedAt());
	}

	@Test
	void 참여한_프로젝트가_없으면_빈_목록을_반환한다() {
		User user = saveUser("empty-project-list@synq.com");

		assertThat(projectService.findAll(user.getUserId())).isEmpty();
	}

	private Project saveProject(Long ownerId, Long memberId, ProjectMemberRole role, String title) {
		Project project = projectRepository.save(Project.of(ownerId, title, "프로젝트 설명"));
		projectMemberRepository.save(ProjectMember.of(project.getId(), memberId, role));
		return project;
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
