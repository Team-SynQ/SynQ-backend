package com.synq.backend.domain.user.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseCode {
	ROLE_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND,
			"USER404_1",
			"역할·관점 프로필을 찾을 수 없습니다."),
	DETAIL_ROLE_REQUIRED(HttpStatus.BAD_REQUEST,
			"USER400_1",
			"역할이 '기타'인 경우 세부 역할을 입력해야 합니다."),
	TOO_MANY_PERSPECTIVES(HttpStatus.BAD_REQUEST,
			"USER400_2",
			"관심 관점은 최대 3개까지 선택할 수 있습니다."),
	CANNOT_DELETE_DEFAULT_ROLE_PROFILE(HttpStatus.BAD_REQUEST,
			"USER400_3",
			"기본 역할·관점 프로필은 삭제할 수 없습니다. 다른 프로필을 기본으로 설정한 후 삭제해주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
