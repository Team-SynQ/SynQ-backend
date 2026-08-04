package com.synq.backend.domain.reference.link;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml 의 reference.link.* 를 바인딩한다.
 * BackendApplication 에 @ConfigurationPropertiesScan 이 있어 별도 등록이 필요 없다.
 *
 * @param preflightCallTimeout 프리플라이트는 트랜잭션 안에서 돌아 DB 커넥션을 점유하므로 본문 fetch 보다 짧다
 * @param allowPrivateNetwork 켜면 SSRF 방어가 통째로 무력화된다. 테스트/로컬 전용이다
 */
@Validated
@ConfigurationProperties(prefix = "reference.link")
public record LinkProperties(
		Duration connectTimeout,
		Duration readTimeout,
		Duration callTimeout,
		Duration preflightCallTimeout,
		@Positive long maxContentBytes,
		@Positive int minTextLength,
		boolean allowPrivateNetwork
) {
}
