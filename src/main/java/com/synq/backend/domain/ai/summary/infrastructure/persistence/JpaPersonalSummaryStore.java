package com.synq.backend.domain.ai.summary.infrastructure.persistence;

import com.synq.backend.domain.ai.summary.domain.PersonalSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryStore;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaPersonalSummaryStore implements PersonalSummaryStore {

	private final PersonalSummaryJpaRepository repository;

	public JpaPersonalSummaryStore(PersonalSummaryJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void saveAll(List<PersonalSummary> summaries) {
		repository.saveAll(summaries.stream().map(PersonalSummaryEntity::from).toList());
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<PersonalSummary> findLatestByMeetingIdAndUserId(Long meetingId, Long userId) {
		return repository.findFirstByMeetingIdAndUserIdOrderByVersionDesc(meetingId, userId)
				.map(PersonalSummaryEntity::toDomain);
	}
}
