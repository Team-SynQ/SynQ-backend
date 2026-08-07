package com.synq.backend.domain.reference.file;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param minTextLength 이 길이 미만이면 텍스트 레이어가 없는 것으로 보고 등록을 거절한다
 * @param maxTextChars  추출 상한. 초과분은 버린다. 실패가 아니다
 */
@Validated
@ConfigurationProperties(prefix = "reference.file")
public record FileExtractionProperties(
		@Positive int minTextLength,
		@Positive int maxTextChars
) {
}
