package com.synq.backend.domain.ai.summary.domain;

import com.synq.backend.domain.ai.summary.domain.GeneratedSummary;
import com.synq.backend.domain.ai.summary.domain.SummaryContext;

public interface SummaryAiClient {
	GeneratedSummary generate(SummaryContext context);
}
