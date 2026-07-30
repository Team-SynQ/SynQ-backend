package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaMeetingSummaryStore implements MeetingSummaryStore {

	private final MeetingSummaryJpaRepository repository;
	private final EntityManager entityManager;

	public JpaMeetingSummaryStore(MeetingSummaryJpaRepository repository, EntityManager entityManager) {
		this.repository = repository;
		this.entityManager = entityManager;
	}

	@Override
	@Transactional
	public MeetingSummary save(MeetingSummary summary) {
		// 같은 회의의 버전 번호 계산을 직렬화해 다중 서버에서도 (meeting_id, version) 충돌을 막는다.
		entityManager.createNativeQuery("SELECT id FROM meeting WHERE id = :meetingId FOR UPDATE")
				.setParameter("meetingId", summary.meetingId())
				.getSingleResult();
		int nextVersion = repository.findFirstByMeetingIdOrderByVersionDesc(summary.meetingId())
				.map(entity -> entity.getVersion() + 1)
				.orElse(1);
		MeetingSummary versioned = summary.withVersion(nextVersion);
		return repository.save(MeetingSummaryEntity.from(versioned)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MeetingSummary> findLatestByMeetingId(Long meetingId) {
		return repository.findFirstByMeetingIdOrderByVersionDesc(meetingId)
				.map(MeetingSummaryEntity::toDomain);
	}
}
