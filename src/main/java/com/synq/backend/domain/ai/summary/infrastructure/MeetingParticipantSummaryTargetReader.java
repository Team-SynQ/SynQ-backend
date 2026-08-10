package com.synq.backend.domain.ai.summary.infrastructure;

import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTargetReader;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회의 참여자마다 기본 역할 프로필과 관심 관점을 결합해 개인 요약 대상을 만든다.
 */
@Component
public class MeetingParticipantSummaryTargetReader implements PersonalSummaryTargetReader {

	private final MeetingParticipantRepository meetingParticipantRepository;
	private final RoleProfileRepository roleProfileRepository;
	private final RoleProfilePerspectiveRepository perspectiveRepository;

	public MeetingParticipantSummaryTargetReader(
			MeetingParticipantRepository meetingParticipantRepository,
			RoleProfileRepository roleProfileRepository,
			RoleProfilePerspectiveRepository perspectiveRepository
	) {
		this.meetingParticipantRepository = meetingParticipantRepository;
		this.roleProfileRepository = roleProfileRepository;
		this.perspectiveRepository = perspectiveRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PersonalSummaryTarget> findByMeetingId(Long meetingId) {
		Map<Long, PersonalSummaryTarget> targets = new LinkedHashMap<>();
		meetingParticipantRepository.findByMeetingIdOrderByJoinedAtAscIdAsc(meetingId)
				.forEach(participant -> targets.computeIfAbsent(
						participant.getUserId(),
						this::createTarget
				));
		return List.copyOf(targets.values());
	}

	// 역할 프로필이 없으면 빈 값으로 둔다. 회의 참가 역할(HOST/MEMBER)은 직무 역할이 아니라
	// 프롬프트에 넣으면 개인 요약이 "주최자 관점"이라는 엉뚱한 축으로 생성된다.
	private PersonalSummaryTarget createTarget(Long userId) {
		return roleProfileRepository.findByUserIdAndIsDefaultTrue(userId)
				.map(profile -> new PersonalSummaryTarget(
						userId,
						profile.getRole().name(),
						profile.getDetailRole(),
						perspectiveRepository.findAllByRoleProfileId(profile.getId()).stream()
								.map(value -> value.getPerspective().name())
								.toList()
				))
				.orElseGet(() -> new PersonalSummaryTarget(userId, "", "", List.of()));
	}
}
