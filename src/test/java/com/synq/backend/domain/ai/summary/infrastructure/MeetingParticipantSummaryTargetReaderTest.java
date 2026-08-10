package com.synq.backend.domain.ai.summary.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회의 참가 역할(HOST/MEMBER)은 직무 역할이 아니라 프롬프트에 나가면 안 된다.
 * 폴백 제거가 유지되는지 고정한다.
 */
@Transactional
class MeetingParticipantSummaryTargetReaderTest extends PostgresTestContainer {

	@Autowired
	MeetingParticipantSummaryTargetReader reader;
	@Autowired
	MeetingRepository meetingRepository;
	@Autowired
	MeetingParticipantRepository meetingParticipantRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	RoleProfileRepository roleProfileRepository;
	@Autowired
	RoleProfilePerspectiveRepository perspectiveRepository;

	@Test
	void 역할_프로필이_있으면_역할과_상세역할을_나눠_담는다() {
		Long userId = saveUser();
		Long meetingId = saveMeetingWithHost(userId);
		RoleProfile profile = roleProfileRepository.save(
				RoleProfile.of(userId, Role.DEV_TECH, "백엔드", true));
		perspectiveRepository.save(RoleProfilePerspective.of(profile.getId(), Perspective.TECH_RISK));

		List<PersonalSummaryTarget> targets = reader.findByMeetingId(meetingId);

		assertThat(targets).hasSize(1);
		assertThat(targets.get(0).role()).isEqualTo("DEV_TECH");
		assertThat(targets.get(0).detailRole()).isEqualTo("백엔드");
		assertThat(targets.get(0).perspectives()).containsExactly("TECH_RISK");
		assertThat(targets.get(0).roleDescription()).isEqualTo("DEV_TECH - 백엔드");
	}

	@Test
	void 역할_프로필이_없으면_회의_참가_역할로_채우지_않는다() {
		Long userId = saveUser();
		Long meetingId = saveMeetingWithHost(userId);

		List<PersonalSummaryTarget> targets = reader.findByMeetingId(meetingId);

		assertThat(targets).hasSize(1);
		assertThat(targets.get(0).role()).isEmpty();
		assertThat(targets.get(0).detailRole()).isEmpty();
		assertThat(targets.get(0).perspectives()).isEmpty();
		assertThat(targets.get(0).roleDescription()).isEmpty();
	}

	private Long saveUser() {
		return userRepository.save(
				User.ofLocal("테스트", UUID.randomUUID() + "@synq.com", "password-hash")).getUserId();
	}

	// uq_meeting_project_active 가 프로젝트당 IN_PROGRESS 회의를 하나로 제한한다.
	// 컨테이너를 테스트 클래스끼리 공유하므로 projectId 를 고정하면 다른 테스트가 남긴 회의와 충돌한다.
	private Long saveMeetingWithHost(Long userId) {
		Long meetingId = meetingRepository.save(Meeting.of(userId, "요약 회의")).getId();
		meetingParticipantRepository.save(
				MeetingParticipant.of(meetingId, userId, ParticipantRole.HOST));
		return meetingId;
	}
}
