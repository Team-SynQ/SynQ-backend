package com.synq.backend.domain.ai.summary.infrastructure;

import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTargetReader;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.user.entity.RoleProfile;
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
						userId -> createTarget(userId, participant.getRole().name())
				));
		return List.copyOf(targets.values());
	}

	private PersonalSummaryTarget createTarget(Long userId, String meetingRole) {
		return roleProfileRepository.findByUserIdAndIsDefaultTrue(userId)
				.map(profile -> new PersonalSummaryTarget(
						userId,
						roleDescription(profile),
						perspectiveRepository.findAllByRoleProfileId(profile.getId()).stream()
								.map(value -> value.getPerspective().name())
								.toList()
				))
				.orElseGet(() -> new PersonalSummaryTarget(userId, meetingRole, List.of()));
	}

	private String roleDescription(RoleProfile profile) {
		if (profile.getDetailRole() == null || profile.getDetailRole().isBlank()) {
			return profile.getRole().name();
		}
		return profile.getRole().name() + " - " + profile.getDetailRole();
	}
}
