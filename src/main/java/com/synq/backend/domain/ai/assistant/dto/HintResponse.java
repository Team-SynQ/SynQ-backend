package com.synq.backend.domain.ai.assistant.dto;

import com.synq.backend.domain.ai.assistant.domain.HintResult;

public record HintResponse(String meaning, String myImpact, String teamQuestion) {

	public static HintResponse from(HintResult result) {
		return new HintResponse(result.meaning(), result.myImpact(), result.teamQuestion());
	}
}
