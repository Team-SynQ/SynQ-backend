package com.synq.backend.domain.reference.file;

/** 파일에서 텍스트를 뽑지 못한 이유. 그대로 400 응답에 실린다. */
public enum FileExtractionFailureReason {
	/** 암호가 걸려 열 수 없다. */
	ENCRYPTED,
	/** 손상됐거나 형식이 깨져 파싱할 수 없다. */
	CORRUPTED,
	/** 파싱은 됐지만 텍스트가 없다. 스캔 PDF, 이미지 위주 PPTX. */
	NO_TEXT_LAYER
}
