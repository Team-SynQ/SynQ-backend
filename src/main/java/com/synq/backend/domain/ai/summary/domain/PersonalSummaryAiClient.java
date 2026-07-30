package com.synq.backend.domain.ai.summary.domain;

public interface PersonalSummaryAiClient {

	GeneratedPersonalSummary generate(
			SummaryContext context,
			GeneratedSummary overallSummary,
			PersonalSummaryTarget target
	);
}
