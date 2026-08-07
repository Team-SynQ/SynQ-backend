package com.synq.backend.domain.reference.file;

import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import org.springframework.web.multipart.MultipartFile;

/**
 * 검증과 텍스트 추출이 모두 끝난 파일 한 건.
 *
 * @param text 등록 트랜잭션 밖에서 이미 뽑아둔 값. 저장 이후 인덱싱 이벤트에 그대로 실린다
 */
public record ExtractedFile(
		MultipartFile file,
		String name,
		ReferenceFileExtension extension,
		String contentType,
		String text
) {
}
