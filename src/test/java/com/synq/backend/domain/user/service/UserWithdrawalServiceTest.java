package com.synq.backend.domain.user.service;

import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;
import com.synq.backend.domain.ai.assistant.repository.AiChatMessageRepository;
import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.auth.service.AuthTokenService;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberPerspective;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.project.entity.ProjectParticipationRequestPerspective;
import com.synq.backend.domain.project.repository.ProjectMemberPerspectiveRepository;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestPerspectiveRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.image.ProfileImageStorageClient;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

class UserWithdrawalServiceTest extends PostgresTestContainer {

	@Autowired private UserService userService;
	@Autowired private UserRepository userRepository;
	@Autowired private ProjectRepository projectRepository;
	@Autowired private ProjectMemberRepository projectMemberRepository;
	@Autowired private ProjectMemberPerspectiveRepository projectMemberPerspectiveRepository;
	@Autowired private ProjectParticipationRequestRepository participationRequestRepository;
	@Autowired private ProjectParticipationRequestPerspectiveRepository participationRequestPerspectiveRepository;
	@Autowired private RoleProfileRepository roleProfileRepository;
	@Autowired private RoleProfilePerspectiveRepository roleProfilePerspectiveRepository;
	@Autowired private MeetingRepository meetingRepository;
	@Autowired private MeetingParticipantRepository meetingParticipantRepository;
	@Autowired private ReferenceMaterialRepository referenceMaterialRepository;
	@Autowired private AiChatMessageRepository aiChatMessageRepository;
	@Autowired private JwtProvider jwtProvider;
	@Autowired private TransactionTemplate transactionTemplate;

	@MockitoBean private AccessTokenBlacklistService blacklistService;
	@MockitoBean private AuthTokenService authTokenService;
	@MockitoBean private ProfileImageStorageClient profileImageStorageClient;

	@BeforeEach
	void resetMocks() {
		reset(blacklistService, authTokenService, profileImageStorageClient);
	}

	@Test
	void 일반_회원_탈퇴는_관계_데이터를_정리하고_기존_콘텐츠를_보존한다() {
		String suffix = UUID.randomUUID().toString();
		User owner = userRepository.save(User.ofLocal("소유자", "owner-" + suffix + "@synq.com", "hash"));
		User user = userRepository.save(User.ofSocial("회원", "member@synq.com", Provider.KAKAO, "kakao-" + suffix));
		user.updateProfileImageKey("img/profile/" + user.getUserId() + "/profile.png");
		userRepository.saveAndFlush(user);

		Project project = projectRepository.save(Project.of(owner.getUserId(), "보존 프로젝트", null));
		ProjectMember member = projectMemberRepository.save(
				ProjectMember.of(project.getId(), user.getUserId(), ProjectMemberRole.MEMBER));
		ProjectMemberPerspective memberPerspective = projectMemberPerspectiveRepository.save(
				ProjectMemberPerspective.of(member.getId(), Perspective.TECH_RISK));
		ProjectParticipationRequest joinRequest = participationRequestRepository.save(
				ProjectParticipationRequest.pending(
						project.getId(), user.getUserId(), ProjectJoinSettingSource.PROJECT_CUSTOM,
						Role.DEV_TECH, null));
		ProjectParticipationRequestPerspective requestPerspective = participationRequestPerspectiveRepository.save(
				ProjectParticipationRequestPerspective.of(joinRequest.getId(), Perspective.ACTION_ITEM));
		RoleProfile roleProfile = roleProfileRepository.save(
				RoleProfile.of(user.getUserId(), Role.DEV_TECH, null, true));
		RoleProfilePerspective rolePerspective = roleProfilePerspectiveRepository.save(
				RoleProfilePerspective.of(roleProfile.getId(), Perspective.SCHEDULE));

		Meeting meeting = meetingRepository.save(Meeting.of(project.getId(), "보존 회의"));
		MeetingParticipant participant = meetingParticipantRepository.save(
				MeetingParticipant.of(meeting.getId(), user.getUserId(), ParticipantRole.MEMBER));
		ReferenceMaterial reference = referenceMaterialRepository.save(
				ReferenceMaterial.ofLink(project.getId(), user.getUserId(), "보존 링크",
						"https://example.com/" + suffix, ReferenceStatus.AVAILABLE));
		AiChatMessage aiChat = aiChatMessageRepository.save(
				AiChatMessage.start(meeting.getId(), user.getUserId(), null, UUID.randomUUID(), "보존 질문"));

		String accessToken = jwtProvider.createAccessToken(user.getUserId());
		userService.withdraw(user.getUserId(), accessToken);

		User withdrawn = userRepository.findById(user.getUserId()).orElseThrow();
		assertThat(withdrawn.getName()).isEqualTo("탈퇴한 사용자");
		assertThat(withdrawn.getEmail()).isNull();
		assertThat(withdrawn.getPasswordHash()).isNull();
		assertThat(withdrawn.getProviderId()).startsWith("withdrawn:" + user.getUserId() + ":");
		assertThat(withdrawn.getProfileImageKey()).isNull();
		assertThat(withdrawn.getDeletedAt()).isNotNull();

		assertThat(projectMemberRepository.findById(member.getId())).isEmpty();
		assertThat(projectMemberPerspectiveRepository.findById(memberPerspective.getId())).isEmpty();
		assertThat(participationRequestRepository.findById(joinRequest.getId())).isEmpty();
		assertThat(participationRequestPerspectiveRepository.findById(requestPerspective.getId())).isEmpty();
		assertThat(roleProfileRepository.findById(roleProfile.getId())).isEmpty();
		assertThat(roleProfilePerspectiveRepository.findById(rolePerspective.getId())).isEmpty();

		assertThat(projectRepository.findById(project.getId())).isPresent();
		assertThat(meetingRepository.findById(meeting.getId())).isPresent();
		assertThat(meetingParticipantRepository.findById(participant.getId())).isPresent();
		assertThat(referenceMaterialRepository.findById(reference.getId())).isPresent();
		assertThat(aiChatMessageRepository.findById(aiChat.getId())).isPresent();

		verify(profileImageStorageClient).delete("img/profile/" + user.getUserId() + "/profile.png");
		verify(blacklistService).blacklist(
				org.mockito.ArgumentMatchers.eq(accessToken), org.mockito.ArgumentMatchers.any());
		verify(authTokenService).revoke(user.getUserId());
	}

	@Test
	void 탈퇴한_소셜_식별자로_새_사용자를_가입할_수_있다() {
		String providerId = "google-" + UUID.randomUUID();
		User withdrawn = userRepository.save(User.ofSocial("기존", "old@synq.com", Provider.GOOGLE, providerId));

		userService.withdraw(withdrawn.getUserId(), jwtProvider.createAccessToken(withdrawn.getUserId()));
		User rejoined = userRepository.saveAndFlush(User.ofSocial("재가입", "new@synq.com", Provider.GOOGLE, providerId));

		assertThat(rejoined.getUserId()).isNotEqualTo(withdrawn.getUserId());
		assertThat(userRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId))
				.map(User::getUserId)
				.contains(rejoined.getUserId());
	}

	@Test
	void 탈퇴_트랜잭션이_롤백되면_프로필_이미지를_삭제하지_않는다() {
		String suffix = UUID.randomUUID().toString();
		User user = userRepository.save(User.ofLocal("롤백", "rollback-" + suffix + "@synq.com", "hash"));
		String key = "img/profile/" + user.getUserId() + "/rollback.png";
		user.updateProfileImageKey(key);
		userRepository.saveAndFlush(user);
		String accessToken = jwtProvider.createAccessToken(user.getUserId());

		transactionTemplate.executeWithoutResult(status -> {
			userService.withdraw(user.getUserId(), accessToken);
			status.setRollbackOnly();
		});

		verify(profileImageStorageClient, never()).delete(key);
		User active = userRepository.findById(user.getUserId()).orElseThrow();
		assertThat(active.getDeletedAt()).isNull();
		assertThat(active.getProfileImageKey()).isEqualTo(key);
	}
}
