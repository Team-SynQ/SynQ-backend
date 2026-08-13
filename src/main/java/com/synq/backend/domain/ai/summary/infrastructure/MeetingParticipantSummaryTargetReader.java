package com.synq.backend.domain.ai.summary.infrastructure;

import com.synq.backend.domain.ai.personalization.MemberProfileReader;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTargetReader;
import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회의 참여자마다 프로젝트에서 선택한 역할·관점을 결합해 개인 요약 대상을 만든다.
 */
@Component
public class MeetingParticipantSummaryTargetReader implements PersonalSummaryTargetReader {

	private final MeetingParticipantRepository meetingParticipantRepository;
	private final MeetingRepository meetingRepository;
	private final MemberProfileReader memberProfileReader;

	public MeetingParticipantSummaryTargetReader(
			MeetingParticipantRepository meetingParticipantRepository,
			MeetingRepository meetingRepository,
			MemberProfileReader memberProfileReader
	) {
		this.meetingParticipantRepository = meetingParticipantRepository;
		this.meetingRepository = meetingRepository;
		this.memberProfileReader = memberProfileReader;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PersonalSummaryTarget> findByMeetingId(Long meetingId) {
		Long projectId = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND))
				.getProjectId();
		Map<Long, PersonalSummaryTarget> targets = new LinkedHashMap<>();
		meetingParticipantRepository.findByMeetingIdOrderByJoinedAtAscIdAsc(meetingId)
				.forEach(participant -> targets.computeIfAbsent(
						participant.getUserId(),
						userId -> createTarget(projectId, userId)
				));
		return List.copyOf(targets.values());
	}

	// 역할 프로필이 없으면 빈 값으로 둔다. 회의 참가 역할(HOST/MEMBER)은 직무 역할이 아니라
	// 프롬프트에 넣으면 개인 요약이 "주최자 관점"이라는 엉뚱한 축으로 생성된다.
	private PersonalSummaryTarget createTarget(Long projectId, Long userId) {
		var profile = memberProfileReader.find(projectId, userId);
		return new PersonalSummaryTarget(
				userId,
				profile.role(),
				profile.detailRole(),
				profile.perspectives()
		);
	}
}
