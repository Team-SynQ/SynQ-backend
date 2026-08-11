package com.synq.backend.domain.transcript.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * soniox.* 바인딩. BackendApplication 의 @ConfigurationPropertiesScan 이 등록한다.
 * url/model/audioFormat 은 Soniox 실시간 WS 문서 기준값이며, 실제 응답을 확인하고 확정해야 한다.
 */
@ConfigurationProperties(prefix = "soniox")
public record SonioxProperties(
		String apiKey,
		String url,
		String model,
		String audioFormat,
		List<String> languageHints,
		long segmentIdleTimeoutMs,
		long closeFlushTimeoutMs,
		long keepaliveIdleMs
) {

	/** Soniox 가 오디오/keepalive 없이 버텨주는 상한. 이걸 넘겨 잡으면 keepalive 가 의미를 잃는다. */
	private static final long SONIOX_INACTIVITY_LIMIT_MS = 20_000L;

	public SonioxProperties {
		if (segmentIdleTimeoutMs <= 0) {
			throw new IllegalArgumentException("segmentIdleTimeoutMs 는 1 이상이어야 합니다: " + segmentIdleTimeoutMs);
		}
		if (closeFlushTimeoutMs <= 0) {
			throw new IllegalArgumentException("closeFlushTimeoutMs 는 1 이상이어야 합니다: " + closeFlushTimeoutMs);
		}
		if (keepaliveIdleMs <= 0 || keepaliveIdleMs >= SONIOX_INACTIVITY_LIMIT_MS) {
			throw new IllegalArgumentException(
					"keepaliveIdleMs 는 1 이상 %d 미만이어야 합니다: %d".formatted(SONIOX_INACTIVITY_LIMIT_MS, keepaliveIdleMs));
		}
	}

	public Duration segmentIdleTimeout() {
		return Duration.ofMillis(segmentIdleTimeoutMs);
	}

	public Duration closeFlushTimeout() {
		return Duration.ofMillis(closeFlushTimeoutMs);
	}

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}
}
