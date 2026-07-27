package com.synq.backend.domain.transcript.client.soniox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.transcript.config.SonioxProperties;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Soniox 실시간 STT 와의 WebSocket 연결 하나. 회의(=스트림) 하나당 인스턴스 하나다.
 *
 * <p>Soniox 는 공식 Java SDK 가 없어 raw WebSocket 프로토콜로 직접 연동한다.
 * 프로토콜 스키마가 확정되기 전까지 응답 원문을 DEBUG 로 그대로 남긴다.
 */
public class SonioxStreamClient extends WebSocketListener {

	private static final Logger log = LoggerFactory.getLogger(SonioxStreamClient.class);

	private final Long meetingId;
	private final OkHttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final SonioxProperties properties;
	private final SonioxStreamListener listener;

	// onOpen 전에 도착한 오디오를 담아둔다. webm 은 첫 청크에만 헤더가 있어 이걸 흘리면 스트림 전체가 깨진다.
	private final Deque<ByteString> pendingAudio = new ArrayDeque<>();
	private final CountDownLatch finishedLatch = new CountDownLatch(1);

	private volatile WebSocket webSocket;
	private volatile boolean open;
	private volatile boolean closing;

	public SonioxStreamClient(Long meetingId, OkHttpClient httpClient, ObjectMapper objectMapper,
							SonioxProperties properties, SonioxStreamListener listener) {
		this.meetingId = meetingId;
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.properties = properties;
		this.listener = listener;
	}

	public void connect() {
		Request request = new Request.Builder().url(properties.url()).build();
		this.webSocket = httpClient.newWebSocket(request, this);
	}

	/** 브라우저에서 받은 오디오를 그대로 릴레이한다. 서버는 오디오를 저장하지 않는다. */
	public void sendAudio(byte[] payload) {
		ByteString frame = ByteString.of(payload);
		synchronized (pendingAudio) {
			if (!open) {
				pendingAudio.addLast(frame);
				return;
			}
		}
		send(frame);
	}

	/**
	 * 스트림 종료 신호(빈 프레임)를 보내고 Soniox 가 남은 토큰을 flush 할 때까지 기다린다.
	 * 기다리지 않고 바로 닫으면 마지막 세그먼트를 잃는다.
	 */
	public void closeGracefully(long flushTimeoutMs) {
		closing = true;
		WebSocket socket = this.webSocket;
		if (socket == null) {
			return;
		}
		try {
			socket.send(ByteString.EMPTY);
			if (!finishedLatch.await(flushTimeoutMs, TimeUnit.MILLISECONDS)) {
				log.warn("Soniox 종료 응답을 시간 내에 받지 못했습니다. meetingId={}", meetingId);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			socket.close(1000, "meeting ended");
		}
	}

	public void abort() {
		closing = true;
		WebSocket socket = this.webSocket;
		if (socket != null) {
			socket.cancel();
		}
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		log.info("Soniox 스트림을 열었습니다. meetingId={}", meetingId);
		webSocket.send(configMessage());

		// config 전송 이후에만 오디오가 유효하다. 큐에 쌓아둔 순서를 그대로 흘린다.
		synchronized (pendingAudio) {
			open = true;
			while (!pendingAudio.isEmpty()) {
				webSocket.send(pendingAudio.pollFirst());
			}
		}
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		// 프로토콜 확정 전까지 원문을 남긴다. 스키마가 확정되면 이 로그는 내린다.
		log.debug("Soniox 응답. meetingId={} raw={}", meetingId, text);
		try {
			SonioxResponse response = objectMapper.readValue(text, SonioxResponse.class);
			if (response.hasError()) {
				listener.onStreamFailure("Soniox 오류: %s %s".formatted(response.errorCode(), response.errorMessage()), null);
				return;
			}
			if (!response.tokensOrEmpty().isEmpty()) {
				listener.onTokens(response.tokensOrEmpty());
			}
			boolean finished = Boolean.TRUE.equals(response.finished())
					|| response.tokensOrEmpty().stream().anyMatch(SonioxToken::isFinMarker);
			if (finished) {
				listener.onFinished();
				finishedLatch.countDown();
			}
		} catch (Exception e) {
			log.error("Soniox 응답 파싱에 실패했습니다. meetingId={} raw={}", meetingId, text, e);
		}
	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response response) {
		open = false;
		finishedLatch.countDown();
		if (closing) {
			// 정상 종료 과정에서의 소켓 종료는 실패로 보지 않는다.
			log.debug("Soniox 종료 중 소켓이 닫혔습니다. meetingId={}", meetingId);
			return;
		}
		log.error("Soniox 스트림이 실패했습니다. meetingId={}", meetingId, t);
		listener.onStreamFailure("Soniox 연결이 끊어졌습니다.", t);
	}

	@Override
	public void onClosing(WebSocket webSocket, int code, String reason) {
		open = false;
		finishedLatch.countDown();
		webSocket.close(1000, null);
	}

	private void send(ByteString frame) {
		WebSocket socket = this.webSocket;
		if (socket != null && !closing) {
			socket.send(frame);
		}
	}

	private String configMessage() {
		try {
			return objectMapper.writeValueAsString(new SonioxConfigMessage(
					properties.apiKey(),
					properties.model(),
					properties.audioFormat(),
					properties.languageHints(),
					true
			));
		} catch (Exception e) {
			throw new IllegalStateException("Soniox config 메시지 직렬화에 실패했습니다.", e);
		}
	}
}
