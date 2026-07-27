package com.synq.backend.domain.transcript.client.soniox;

import java.util.List;

/** Soniox 스트림에서 올라오는 사건을 세션 쪽으로 전달하는 콜백. */
public interface SonioxStreamListener {

	void onTokens(List<SonioxToken> tokens);

	/** Soniox 가 빈 프레임을 받고 스트림을 정상 종료했음을 알린 시점. */
	void onFinished();

	void onStreamFailure(String reason, Throwable cause);
}
