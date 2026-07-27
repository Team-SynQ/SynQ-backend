package com.synq.backend.domain.transcript.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.transcript.client.soniox.FinalizedSegment;
import com.synq.backend.domain.transcript.client.soniox.SonioxStreamClient;
import com.synq.backend.domain.transcript.client.soniox.SonioxStreamListener;
import com.synq.backend.domain.transcript.client.soniox.SonioxToken;
import com.synq.backend.domain.transcript.client.soniox.SonioxTokenBuffer;
import com.synq.backend.domain.transcript.config.SonioxProperties;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.service.TranscriptSegmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 회의 하나의 전사 세션. 브라우저 WS ↔ Soniox WS ↔ 토큰 버퍼 ↔ 순번 카운터를 묶는다.
 *
 * <p>브라우저 WS 연결 하나가 세션 하나이고, 세션 하나가 Soniox 스트림 하나다.
 * 호스트가 재연결하면 새 세션이 만들어지고 baseOffset 이 다시 계산되므로,
 * Soniox 타임스탬프가 0 으로 리셋돼도 회의 절대 시각은 계속 증가한다.
 */
public class SttSession implements SonioxStreamListener {

	private static final Logger log = LoggerFactory.getLogger(SttSession.class);

	// webm(Matroska) EBML 헤더 매직넘버. 스트림 첫 프레임에 이게 없으면 디코더 초기화 정보가 없는 것이다.
	private static final byte[] EBML_MAGIC = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};

	private final Long meetingId;
	private final WebSocketSession browserSession;
	private final SonioxStreamClient sonioxClient;
	private final SonioxTokenBuffer buffer = new SonioxTokenBuffer();
	private final MeetingTimeline timeline;
	private final AtomicInteger sequence;
	private final TranscriptSegmentService transcriptSegmentService;
	private final SonioxProperties properties;
	private final ObjectMapper objectMapper;

	private volatile boolean firstAudioFrameSeen;
	private volatile boolean closed;

	public SttSession(Long meetingId, WebSocketSession browserSession, MeetingTimeline timeline, int startSequence,
					TranscriptSegmentService transcriptSegmentService, SonioxProperties properties,
					ObjectMapper objectMapper, SonioxClientFactory sonioxClientFactory) {
		this.meetingId = meetingId;
		this.browserSession = browserSession;
		this.timeline = timeline;
		this.sequence = new AtomicInteger(startSequence);
		this.transcriptSegmentService = transcriptSegmentService;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.sonioxClient = sonioxClientFactory.create(meetingId, this);
	}

	public Long meetingId() {
		return meetingId;
	}

	public String browserSessionId() {
		return browserSession.getId();
	}

	public void start() {
		sonioxClient.connect();
		send(SttServerMessage.connectionStatus("CONNECTED"));
	}

	public void relayAudio(byte[] payload) {
		verifyStreamHeader(payload);
		sonioxClient.sendAudio(payload);
	}

	/**
	 * 재연결 시 MediaRecorder 를 재시작하지 않으면 헤더 없는 청크로 스트림이 시작돼
	 * Soniox 가 포맷을 잡지 못한다. 조용히 전사가 멈추는 것을 막기 위해 첫 프레임을 검사한다.
	 */
	private void verifyStreamHeader(byte[] payload) {
		if (firstAudioFrameSeen) {
			return;
		}
		firstAudioFrameSeen = true;
		if (!startsWithEbmlMagic(payload)) {
			log.warn("webm 헤더 없이 오디오 스트림이 시작됐습니다. 재연결 시 MediaRecorder 재시작이 필요합니다. meetingId={}",
					meetingId);
			send(SttServerMessage.interrupted("MISSING_AUDIO_HEADER"));
		}
	}

	private static boolean startsWithEbmlMagic(byte[] payload) {
		if (payload == null || payload.length < EBML_MAGIC.length) {
			return false;
		}
		for (int i = 0; i < EBML_MAGIC.length; i++) {
			if (payload[i] != EBML_MAGIC[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void onTokens(List<SonioxToken> tokens) {
		SonioxTokenBuffer.BufferUpdate update = buffer.accept(tokens, Instant.now());
		update.finalized().forEach(this::persist);
		if (!update.caption().isEmpty()) {
			send(SttServerMessage.caption(update.caption()));
		}
	}

	/** {@code <end>} 가 오지 않을 때의 안전망. 스케줄러 스레드에서 호출된다. */
	public void flushIfIdle(Instant now) {
		buffer.flushIfIdle(now, properties.segmentIdleTimeout()).ifPresent(segment -> {
			log.info("유휴 타임아웃으로 세그먼트를 강제 확정했습니다. meetingId={} idleTimeoutMs={}",
					meetingId, properties.segmentIdleTimeoutMs());
			persist(segment);
		});
	}

	@Override
	public void onFinished() {
		buffer.flushRemaining().ifPresent(this::persist);
	}

	@Override
	public void onStreamFailure(String reason, Throwable cause) {
		// 이미 누적된 확정 토큰은 살린다.
		buffer.flushRemaining().ifPresent(this::persist);
		send(SttServerMessage.interrupted(reason));
	}

	/**
	 * 회의 종료/연결 종료 시 정리. Soniox 에 빈 프레임을 보내고 남은 토큰을 받은 뒤 닫는다.
	 * 바로 닫으면 종료 직전 발화를 잃는다.
	 */
	public void close(boolean graceful) {
		if (closed) {
			return;
		}
		closed = true;
		if (graceful) {
			sonioxClient.closeGracefully(properties.closeFlushTimeoutMs());
		} else {
			sonioxClient.abort();
		}
		buffer.flushRemaining().ifPresent(this::persist);
	}

	private void persist(FinalizedSegment segment) {
		try {
			int sequenceIndex = sequence.getAndIncrement();
			int startMs = timeline.toAbsoluteMs(segment.startMs());
			int endMs = timeline.toAbsoluteMs(segment.endMs());

			TranscriptSegment saved = transcriptSegmentService.finalizeSegment(
					meetingId, sequenceIndex, startMs, endMs, segment.content());

			send(SttServerMessage.transcript(saved.getId(), saved.getContent(), startMs, sequenceIndex));
		} catch (RuntimeException e) {
			// 한 세그먼트 저장 실패가 스트림 전체를 죽이지 않도록 삼킨다.
			log.error("전사 세그먼트 저장에 실패했습니다. meetingId={} content={}", meetingId, segment.content(), e);
		}
	}

	private void send(SttServerMessage message) {
		if (!browserSession.isOpen()) {
			return;
		}
		try {
			String payload = objectMapper.writeValueAsString(message);
			// WebSocketSession 은 동시 전송에 안전하지 않다. Soniox 리더/스케줄러 스레드가 함께 쓰므로 직렬화한다.
			synchronized (browserSession) {
				browserSession.sendMessage(new TextMessage(payload));
			}
		} catch (IOException | RuntimeException e) {
			log.warn("클라이언트로 전사 메시지를 보내지 못했습니다. meetingId={}", meetingId, e);
		}
	}
}
