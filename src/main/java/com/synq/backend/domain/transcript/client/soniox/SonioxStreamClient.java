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

	// 연결 대기 상한. Soniox 연결이 죽었는데도(open==false 고정) 브라우저가 계속 오디오를 보내면
	// 무한정 쌓여 OOM 으로 이어질 수 있어 상한을 둔다. 1프레임≈1초라 100이면 충분히 넉넉하다.
	private static final int MAX_PENDING_AUDIO_FRAMES = 100;

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
		if (closing) {
			// 종료/실패가 이미 확정된 스트림에는 릴레이 자체를 하지 않는다 (무한 적재 방지).
			return;
		}
		ByteString frame = ByteString.of(payload);
		synchronized (pendingAudio) {
			if (!open) {
				if (pendingAudio.size() >= MAX_PENDING_AUDIO_FRAMES) {
					log.warn("Soniox 연결 대기 큐가 가득 차 오디오 프레임을 폐기합니다. meetingId={}", meetingId);
					return;
				}
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
		if (socket == null || !open) {
			// 아직 config 를 보내기 전(onOpen 이전)이면 Soniox 프로토콜상 흘려보낼 스트림 자체가 없다.
			// 여기서 종료 프레임을 먼저 보내면 이후 onOpen 이 config 를 그 뒤에 보내 순서가 깨진다.
			if (socket != null) {
				socket.cancel();
			}
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
		if (closing) {
			// close() 가 onOpen 보다 먼저 호출된 경쟁 상태. config/오디오를 보내면 이미 끝난 스트림에
			// 뒤늦게 프로토콜 순서를 깨뜨리게 되므로 그냥 취소한다.
			webSocket.cancel();
			return;
		}
		log.info("Soniox 스트림을 열었습니다. meetingId={}", meetingId);
		webSocket.send(configMessage());

		// config 전송 이후에만 오디오가 유효하다. 큐에 쌓아둔 순서를 그대로 흘린다.
		synchronized (pendingAudio) {
			if (closing) {
				pendingAudio.clear();
				webSocket.cancel();
				return;
			}
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
		// 실패한 연결은 재시도하지 않는다. sendAudio() 가 더 이상 오디오를 적재하지 않도록 종결 상태로 못박는다.
		closing = true;
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
