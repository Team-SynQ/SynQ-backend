package com.synq.backend.domain.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

class HintPromptFactoryTest {

	private final HintPromptFactory factory = new HintPromptFactory();

	@Test
	void 역할과_관점을_한글_라벨로_바꾼다() {
		String prompt = factory.create(input("DEV_TECH", "백엔드", List.of("TECH_RISK", "SCHEDULE")));

		assertThat(prompt).contains("역할: 개발·기술 - 백엔드");
		assertThat(prompt).contains("관점: 기술 리스크, 일정");
	}

	@Test
	void 세부역할이_없으면_역할만_쓴다() {
		String prompt = factory.create(input("PLANNING_OPERATION", "", List.of("UX")));

		assertThat(prompt).contains("역할: 기획·운영\n");
		assertThat(prompt).contains("관점: 사용자 경험");
	}

	@Test
	void 매핑에_없는_코드는_그대로_출력한다() {
		String prompt = factory.create(input("NEW_ROLE", "", List.of("NEW_PERSPECTIVE")));

		assertThat(prompt).contains("역할: NEW_ROLE");
		assertThat(prompt).contains("관점: NEW_PERSPECTIVE");
	}

	@Test
	void 역할과_관점이_비면_미입력으로_쓴다() {
		String prompt = factory.create(input("", "", List.of()));

		assertThat(prompt).contains("역할: (미입력)");
		assertThat(prompt).contains("관점: (미입력)");
	}

	@Test
	void 참고자료가_없으면_없음으로_쓴다() {
		String prompt = factory.create(input("DEV_TECH", "", List.of()));

		assertThat(prompt).contains("[참고자료]\n(없음)");
	}

	@Test
	void 참고자료가_있으면_내용을_나열한다() {
		HintInput input = new HintInput(
				"클릭한 발화", List.of("앞 발화"), List.of("뒤 발화"),
				"DEV_TECH", "백엔드", List.of("TECH_RISK"),
				LiveContextSnapshot.empty(),
				List.of(new ChunkMatch(1L, 10L, 0, "설계 문서 조각", 0.9)));

		String prompt = factory.create(input);

		assertThat(prompt).contains("- 설계 문서 조각");
	}

	@Test
	void 회의_맥락_값이_있으면_그대로_렌더링한다() {
		HintInput input = new HintInput(
				"클릭한 발화", List.of("앞 발화"), List.of("뒤 발화"),
				"DEV_TECH", "백엔드", List.of("TECH_RISK"),
				new LiveContextSnapshot(
						"지금까지 배포 일정을 논의했다",
						"배포 일정",
						List.of("금요일 배포로 결정"),
						List.of("QA 일정 공유"),
						List.of("롤백 기준은?", "담당자는?")),
				List.of());

		String prompt = factory.create(input);

		assertThat(prompt).contains("누적 요약: 지금까지 배포 일정을 논의했다");
		assertThat(prompt).contains("현재 주제: 배포 일정");
		assertThat(prompt).contains("미해결 질문: 롤백 기준은? / 담당자는?");
	}

	@Test
	void 회의_맥락이_비면_미입력과_없음으로_쓴다() {
		String prompt = factory.create(input("DEV_TECH", "", List.of()));

		assertThat(prompt).contains("누적 요약: (미입력)");
		assertThat(prompt).contains("현재 주제: (미입력)");
		assertThat(prompt).contains("미해결 질문: (없음)");
	}

	@Test
	void 클릭한_발화와_앞뒤_맥락을_담는다() {
		HintInput input = new HintInput(
				"클릭한 발화", List.of("앞1", "앞2"), List.of("뒤1"),
				"DEV_TECH", "백엔드", List.of("TECH_RISK"),
				LiveContextSnapshot.empty(), List.of());

		String prompt = factory.create(input);

		assertThat(prompt).contains("클릭한 발화");
		assertThat(prompt).contains("앞1 앞2");
		assertThat(prompt).contains("뒤1");
	}

	private HintInput input(String role, String detailRole, List<String> perspectives) {
		return new HintInput(
				"클릭한 발화", List.of("앞 발화"), List.of("뒤 발화"),
				role, detailRole, perspectives,
				LiveContextSnapshot.empty(), List.of());
	}
}
