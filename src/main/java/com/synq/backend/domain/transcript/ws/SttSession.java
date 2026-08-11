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
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private final SttSessionRegistry registry;

	private volatile boolean firstAudioFrameSeen;
	private volatile boolean streamRejected;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public SttSession(Long meetingId, WebSocketSession browserSession, MeetingTimeline timeline, int startSequence,
					TranscriptSegmentService transcriptSegmentService, SonioxProperties properties,
					ObjectMapper objectMapper, SonioxClientFactory sonioxClientFactory, SttSessionRegistry registry) {
		this.meetingId = meetingId;
		this.browserSession = browserSession;
		this.timeline = timeline;
		this.sequence = new AtomicInteger(startSequence);
		this.transcriptSegmentService = transcriptSegmentService;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.registry = registry;
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
		if (streamRejected) {
			// 헤더 없이 시작된 걸 이미 감지한 스트림. Soniox 로 흘려봐야 디코딩 불가능하므로 버린다.
			return;
		}
		if (!verifyStreamHeader(payload)) {
			return;
		}
		sonioxClient.sendAudio(payload);
	}

	/**
	 * 재연결 시 MediaRecorder 를 재시작하지 않으면 헤더 없는 청크로 스트림이 시작돼
	 * Soniox 가 포맷을 잡지 못한다. 계속 흘려보내는 대신 세션을 끊어 재연결을 강제한다.
	 *
	 * @return 헤더가 정상이거나 이미 확인된 스트림이면 true, 헤더 없이 시작돼 거부했으면 false
	 */
	private boolean verifyStreamHeader(byte[] payload) {
		if (firstAudioFrameSeen) {
			return true;
		}
		firstAudioFrameSeen = true;
		if (startsWithEbmlMagic(payload)) {
			return true;
		}

		streamRejected = true;
		log.warn("webm 헤더 없이 오디오 스트림이 시작됐습니다. 재연결 시 MediaRecorder 재시작이 필요합니다. meetingId={}",
				meetingId);
		send(SttServerMessage.interrupted("MISSING_AUDIO_HEADER"));
		close(false);
		closeBrowserSession();
		return false;
	}

	private void closeBrowserSession() {
		try {
			if (browserSession.isOpen()) {
				browserSession.close(CloseStatus.BAD_DATA.withReason("MISSING_AUDIO_HEADER"));
			}
		} catch (IOException e) {
			log.debug("헤더 오류로 인한 브라우저 WS 종료 중 오류: {}", e.getMessage());
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

	/**
	 * 오디오가 멎어도 Soniox 스트림이 끊기지 않게 유지한다. 스케줄러 스레드에서 호출된다.
	 * 프론트의 일시정지 구현(WS 종료 / 오디오만 중단)을 알 필요 없이 유휴 감지만으로 동작한다.
	 */
	public void keepSonioxStreamAlive() {
		sonioxClient.sendKeepaliveIfIdle(properties.keepaliveIdleMs());
	}

	/**
	 * 브라우저 연결을 붙잡아두기 위한 ping. 스케줄러 스레드에서 호출된다.
	 *
	 * <p>호스트는 오디오를 계속 올려보내지만 그건 클라이언트→서버 방향이라, 서버→클라이언트 방향만
	 * 보는 nginx {@code proxy_read_timeout} 을 리셋하지 못한다. 발화가 없는 구간에는 내려보낼
	 * 전사도 없어 연결이 유휴로 판정돼 끊긴다. ping 은 그 방향으로 흐르고, 브라우저가 자동 응답하는
	 * pong 이 Tomcat 의 읽기 유휴 타이머까지 되돌린다.
	 */
	public void sendPing() {
		SttSessionRegistry.pingQuietly(meetingId, browserSession);
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
		if (!closed.compareAndSet(false, true)) {
			// 이미 다른 스레드(정상 종료/에러 콜백/handler)가 닫는 중이거나 닫았다. 중복 실행 방지.
			return;
		}
		try {
			if (graceful) {
				sonioxClient.closeGracefully(properties.closeFlushTimeoutMs());
			} else {
				sonioxClient.abort();
			}
		} finally {
			// Soniox 쪽 종료가 실패하더라도 이미 확정된 토큰은 반드시 저장한다.
			buffer.flushRemaining().ifPresent(this::persist);
		}
	}

	private void persist(FinalizedSegment segment) {
		try {
			int sequenceIndex = sequence.getAndIncrement();
			int startMs = timeline.toAbsoluteMs(segment.startMs());
			int endMs = timeline.toAbsoluteMs(segment.endMs());

			TranscriptSegment saved = transcriptSegmentService.finalizeSegment(
					meetingId, sequenceIndex, startMs, endMs, segment.content());

			SttServerMessage transcriptMessage =
					SttServerMessage.transcript(saved.getId(), saved.getContent(), startMs, sequenceIndex);
			send(transcriptMessage);
			// 확정 전사만 참여자(수신 전용 구독자)에게 fan-out 한다. 중간 캡션/연결상태는 호스트 자신의
			// 오디오 파이프라인 상태라 지금은 구독자 브로드캐스트 대상이 아니다.
			registry.broadcastToSubscribers(meetingId, transcriptMessage);
		} catch (RuntimeException e) {
			// 한 세그먼트 저장 실패가 스트림 전체를 죽이지 않도록 삼킨다.
			// content 는 회의 발화 원문(민감정보)이라 로그에는 길이만 남긴다.
			log.error("전사 세그먼트 저장에 실패했습니다. meetingId={} contentLength={}",
					meetingId, segment.content().length(), e);
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
