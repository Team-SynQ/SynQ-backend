package com.synq.backend.domain.ai.rag.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkerTest {

	private static final int TARGET = 800;
	private static final int OVERLAP = 100;

	private final TextChunker chunker = new TextChunker(TARGET, OVERLAP);

	@Test
	void 빈_텍스트는_청크가_없다() {
		assertThat(chunker.chunk("")).isEmpty();
		assertThat(chunker.chunk("   \n\n  ")).isEmpty();
		assertThat(chunker.chunk(null)).isEmpty();
	}

	@Test
	void 목표_크기보다_짧으면_청크_하나다() {
		List<String> chunks = chunker.chunk("짧은 문서입니다.");

		assertThat(chunks).containsExactly("짧은 문서입니다.");
	}

	@Test
	void 모든_청크는_목표_크기를_넘지_않는다() {
		// 300자 문단 20개 = 6000자
		String text = ("가".repeat(300) + "\n\n").repeat(20);

		List<String> chunks = chunker.chunk(text);

		assertThat(chunks).isNotEmpty();
		assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(TARGET));
	}

	@Test
	void 문단_경계를_우선해서_자른다() {
		// 500자 문단 둘. 합치면 1000자라 800을 넘으므로 서로 다른 청크에 들어가야 한다.
		String first = "가".repeat(500);
		String second = "나".repeat(500);

		List<String> chunks = chunker.chunk(first + "\n\n" + second);

		assertThat(chunks).hasSize(2);
		assertThat(chunks.get(0)).isEqualTo(first);
		assertThat(chunks.get(1)).endsWith(second);
	}

	@Test
	void 두번째_청크는_직전_청크의_끝_100자로_시작한다() {
		String first = "가".repeat(500);
		String second = "나".repeat(500);

		List<String> chunks = chunker.chunk(first + "\n\n" + second);

		String previousTail = chunks.get(0).substring(chunks.get(0).length() - OVERLAP);
		assertThat(chunks.get(1)).startsWith(previousTail);
	}

	@Test
	void 오버랩_꼬리와_다음_내용_사이에_공백이_들어간다() {
		// 공백 없이 붙이면 "이해다.회의가" 처럼 없던 단어가 만들어져 임베딩이 오염된다.
		String first = "가".repeat(500);
		String second = "나".repeat(500);

		List<String> chunks = chunker.chunk(first + "\n\n" + second);

		assertThat(chunks.get(1)).doesNotContain("가나");
		assertThat(chunks.get(1)).contains("가 나");
	}

	@Test
	void 문단이_없는_장문은_문장으로_자른다() {
		// 줄바꿈 없이 100자짜리 문장 30개
		String text = ("나".repeat(98) + ". ").repeat(30);

		List<String> chunks = chunker.chunk(text);

		assertThat(chunks).hasSizeGreaterThan(1);
		assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(TARGET));
	}

	@Test
	void 문장_경계도_없는_초장문은_강제로_자른다() {
		// 구분자가 전혀 없는 2500자
		String text = "다".repeat(2500);

		List<String> chunks = chunker.chunk(text);

		assertThat(chunks).hasSizeGreaterThan(1);
		assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(TARGET));
	}

	@Test
	void 청크는_원문_순서를_유지한다() {
		String text = "가".repeat(500) + "\n\n" + "나".repeat(500) + "\n\n" + "다".repeat(500);

		List<String> chunks = chunker.chunk(text);

		assertThat(chunks.get(0)).contains("가");
		assertThat(chunks.get(chunks.size() - 1)).contains("다");
	}

	@Test
	void 오버랩이_목표_크기_이상이면_생성에_실패한다() {
		assertThatThrownBy(() -> new TextChunker(800, 800))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
