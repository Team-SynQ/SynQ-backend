package com.synq.backend.domain.ai.summary.application;

import com.synq.backend.domain.ai.summary.domain.GeneratedPersonalSummary;
import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.MeetingSummary;
import com.synq.backend.domain.ai.summary.domain.MeetingSummaryStore;
import com.synq.backend.domain.ai.summary.domain.PersonalSummary;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryStore;
import com.synq.backend.domain.ai.summary.domain.PersonalSummaryTarget;
import com.synq.backend.domain.ai.summary.domain.SummaryJob;
import com.synq.backend.domain.ai.summary.domain.SummaryJobStore;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전체 요약과 개인별 요약을 같은 버전으로 원자적으로 저장한다.
 */
@Component
public class SummaryResultWriter {

	private final MeetingSummaryStore meetingSummaryStore;
	private final PersonalSummaryStore personalSummaryStore;
	private final SummaryJobStore summaryJobStore;

	public SummaryResultWriter(
			MeetingSummaryStore meetingSummaryStore,
			PersonalSummaryStore personalSummaryStore,
			SummaryJobStore summaryJobStore
	) {
		this.meetingSummaryStore = meetingSummaryStore;
		this.personalSummaryStore = personalSummaryStore;
		this.summaryJobStore = summaryJobStore;
	}

	@Transactional
	public boolean saveIfJobProcessing(
			SummaryJob job,
			GeneratedSummary overall,
			List<PersonalGeneration> personalGenerations,
			int failedPersonalSummaryCount
	) {
		// 상태 전이를 먼저 선점해, 만료 처리된 오래된 Job이 결과를 저장하지 못하게 한다.
		if (!summaryJobStore.completeIfProcessing(job.id(), failedPersonalSummaryCount)) {
			return false;
		}
		MeetingSummary savedSummary = meetingSummaryStore.save(
				MeetingSummary.from(job.meetingId(), job.id(), overall)
		);
		personalSummaryStore.saveAll(personalGenerations.stream()
				.map(result -> PersonalSummary.from(
							job.meetingId(),
							job.id(),
						result.target(),
						savedSummary.version(),
						result.content()
					))
					.toList());
		return true;
	}

	public record PersonalGeneration(
			PersonalSummaryTarget target,
			GeneratedPersonalSummary content
	) {
	}
}
