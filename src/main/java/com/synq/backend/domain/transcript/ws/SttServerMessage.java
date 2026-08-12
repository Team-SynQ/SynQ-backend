package com.synq.backend.domain.transcript.ws;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 서버 → 클라이언트 WebSocket 메시지. 타입 3종을 하나의 봉투로 표현한다.
 *
 * <p>확정 전사는 utteranceId/sequence 가 채워진 TRANSCRIPT(isFinal=true) 로,
 * 중간 캡션은 둘 다 비운 TRANSCRIPT(isFinal=false) 로 내려간다.
 * 캡션 메시지 모양은 프론트와 합의 후 확정해야 한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SttServerMessage(
		String type,
		Long utteranceId,
		String text,
		Integer startTime,
		Integer sequence,
		Boolean isFinal,
		String status,
		String reason
) {

	private static final String TRANSCRIPT = "TRANSCRIPT";
	private static final String CONNECTION_STATUS = "CONNECTION_STATUS";
	private static final String TRANSCRIPT_INTERRUPTED = "TRANSCRIPT_INTERRUPTED";
	private static final String MEETING_ENDED = "MEETING_ENDED";

	public static SttServerMessage transcript(Long utteranceId, String text, int startTime, int sequence) {
		return new SttServerMessage(TRANSCRIPT, utteranceId, text, startTime, sequence, true, null, null);
	}

	public static SttServerMessage caption(String text) {
		return new SttServerMessage(TRANSCRIPT, null, text, null, null, false, null, null);
	}

	public static SttServerMessage connectionStatus(String status) {
		return new SttServerMessage(CONNECTION_STATUS, null, null, null, null, null, status, null);
	}

	public static SttServerMessage interrupted(String reason) {
		return new SttServerMessage(TRANSCRIPT_INTERRUPTED, null, null, null, null, null, null, reason);
	}

	/** 회의가 종료(정상 /end 또는 진행자 연결 끊김에 의한 강제 종료)됐음을 참여자에게 알린다. */
	public static SttServerMessage meetingEnded() {
		return new SttServerMessage(MEETING_ENDED, null, null, null, null, null, null, null);
	}
}
