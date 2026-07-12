package com.synq.backend.domain.ai.rag.chunking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 문단 → 문장 → 강제 분할 순으로 자연스러운 경계를 우선하며 텍스트를 청킹한다.
 */
@Component
public class TextChunker {

	private static final String PARAGRAPH_DELIMITER = "\\n\\s*\\n";
	// 문장 끝(.!?) 뒤의 공백에서 자른다.
	private static final String SENTENCE_DELIMITER = "(?<=[.!?])\\s+";

	private final int targetSize;
	private final int overlap;
	// 새 청크는 "오버랩 꼬리 + 공백 + 원자"로 시작한다.
	private final int maxAtomSize;

	public TextChunker(@Value("${rag.chunking.target-size:800}") int targetSize,
					   @Value("${rag.chunking.overlap:100}") int overlap) {
		if (overlap >= targetSize) {
			throw new IllegalArgumentException("오버랩은 목표 크기보다 작아야 합니다.");
		}
		this.targetSize = targetSize;
		this.overlap = overlap;
		this.maxAtomSize = targetSize - overlap - 1;
	}

	public List<String> chunk(String text) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		return merge(toAtoms(text.strip()));
	}

	/** maxAtomSize 이하의 조각들로 분해한다. */
	private List<String> toAtoms(String text) {
		List<String> atoms = new ArrayList<>();
		for (String paragraph : text.split(PARAGRAPH_DELIMITER)) {
			String trimmed = paragraph.strip();
			if (trimmed.isEmpty()) {
				continue;
			}
			if (trimmed.length() <= maxAtomSize) {
				atoms.add(trimmed);
				continue;
			}
			for (String sentence : trimmed.split(SENTENCE_DELIMITER)) {
				String s = sentence.strip();
				if (s.isEmpty()) {
					continue;
				}
				if (s.length() <= maxAtomSize) {
					atoms.add(s);
				} else {
					atoms.addAll(hardSplit(s));
				}
			}
		}
		return atoms;
	}

	/** 구분자가 없는 초장문을 강제로 자른다. */
	private List<String> hardSplit(String text) {
		List<String> pieces = new ArrayList<>();
		for (int start = 0; start < text.length(); start += maxAtomSize) {
			pieces.add(text.substring(start, Math.min(start + maxAtomSize, text.length())));
		}
		return pieces;
	}

	/** 조각들을 targetSize 이하로 병합한다. 새 청크는 직전 청크의 꼬리로 시작한다. */
	private List<String> merge(List<String> atoms) {
		List<String> chunks = new ArrayList<>();
		StringBuilder current = new StringBuilder();

		for (String atom : atoms) {
			if (current.isEmpty()) {
				current.append(atom);
				continue;
			}
			if (current.length() + 1 + atom.length() <= targetSize) {
				current.append(' ').append(atom);
				continue;
			}
			String completed = current.toString();
			chunks.add(completed);
			// 꼬리와 원자를 그냥 붙이면 "이해다.회의가" 처럼 없던 단어가 생긴다.
			current = new StringBuilder(tailOf(completed)).append(' ').append(atom);
		}
		if (!current.isEmpty()) {
			chunks.add(current.toString());
		}
		return chunks;
	}

	private String tailOf(String chunk) {
		return chunk.length() <= overlap ? chunk : chunk.substring(chunk.length() - overlap);
	}
}
