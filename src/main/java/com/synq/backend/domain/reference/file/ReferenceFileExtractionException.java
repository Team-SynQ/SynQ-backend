package com.synq.backend.domain.reference.file;

import com.synq.backend.domain.reference.dto.ReferenceFileExtractionFailure;
import java.util.List;
import lombok.Getter;

/**
 * 한 요청의 파일 중 하나 이상이 추출에 실패했다.
 *
 * <p>GeneralException 은 BaseCode 만 들고 있어 추가 payload 를 담지 못한다.
 * 5개를 올렸을 때 어느 것이 문제인지 알려주려면 실패 목록을 advice 까지 옮길 통로가 필요하다.
 */
@Getter
public class ReferenceFileExtractionException extends RuntimeException {

	private final List<ReferenceFileExtractionFailure> failures;

	public ReferenceFileExtractionException(List<ReferenceFileExtractionFailure> failures) {
		super("파일에서 텍스트를 추출하지 못했습니다: " + failures);
		this.failures = List.copyOf(failures);
	}
}
