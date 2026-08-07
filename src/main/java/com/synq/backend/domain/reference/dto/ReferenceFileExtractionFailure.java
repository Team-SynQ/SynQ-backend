package com.synq.backend.domain.reference.dto;

/**
 * 추출에 실패한 파일 한 건. 400 응답의 result 배열 원소다.
 *
 * @param reason FileExtractionFailureReason 의 이름
 */
public record ReferenceFileExtractionFailure(String fileName, String reason) {
}
