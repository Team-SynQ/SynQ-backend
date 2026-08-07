package com.synq.backend.domain.reference.file;

import lombok.Getter;

/**
 * 파일 한 건의 추출 실패.
 *
 * <p>LinkTextExtractor 는 Optional.empty() 로 실패를 알리지만 여기서는 예외를 쓴다.
 * 실패 사유가 400 응답에 실려야 하는데 Optional 로는 담을 수 없기 때문이다.
 */
@Getter
public class FileTextExtractionException extends RuntimeException {

	private final FileExtractionFailureReason reason;

	public FileTextExtractionException(FileExtractionFailureReason reason, Throwable cause) {
		super("파일 텍스트 추출 실패: " + reason, cause);
		this.reason = reason;
	}
}
