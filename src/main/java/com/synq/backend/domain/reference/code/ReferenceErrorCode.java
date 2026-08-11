package com.synq.backend.domain.reference.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReferenceErrorCode implements BaseCode {
	LINK_ADDRESS_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
			"REFERENCE400_1",
			"허용되지 않는 주소입니다."),
	LINK_UNREACHABLE(HttpStatus.BAD_REQUEST,
			"REFERENCE400_2",
			"링크에 접근할 수 없습니다."),
	LINK_UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST,
			"REFERENCE400_3",
			"HTML 또는 텍스트 문서 링크만 등록할 수 있습니다."),
	LINK_TOO_LARGE(HttpStatus.BAD_REQUEST,
			"REFERENCE400_4",
			"링크 문서가 너무 큽니다."),
	INVALID_REFERENCE_FILE(HttpStatus.BAD_REQUEST,
			"REFERENCE400_5",
			"등록할 파일을 확인해 주세요."),
	REFERENCE_FILE_TEXT_EXTRACTION_FAILED(HttpStatus.BAD_REQUEST,
			"REFERENCE400_6",
			"파일에서 텍스트를 추출하지 못했습니다."),
	REFERENCE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN,
			"REFERENCE403_1",
			"참고자료 삭제 권한이 없습니다."),
	REFERENCE_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN,
			"REFERENCE403_2",
			"참고자료 수정 권한이 없습니다."),
	REFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND,
			"REFERENCE404_1",
			"참고자료를 찾을 수 없습니다."),
	REFERENCE_LIMIT_EXCEEDED(HttpStatus.CONFLICT,
			"REFERENCE409_1",
			"프로젝트별 최대 참고자료 수를 초과했습니다."),
	REFERENCE_FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE,
			"REFERENCE413_1",
			"파일 용량 제한을 초과했습니다."),
	UNSUPPORTED_REFERENCE_FILE(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
			"REFERENCE415_1",
			"지원하지 않는 파일 형식입니다."),
	REFERENCE_FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
			"REFERENCE500_1",
			"참고자료 파일 업로드에 실패했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
