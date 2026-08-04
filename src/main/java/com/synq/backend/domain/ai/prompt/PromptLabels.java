package com.synq.backend.domain.ai.prompt;

import java.util.Map;

/**
 * user 도메인의 역할·관점 enum 코드를 프롬프트용 한글 라벨로 바꾼다.
 *
 * <p>한국어 프롬프트에 영문 상수가 섞이면 모델이 오독할 여지가 있어 라벨로 바꾼다.
 * 매핑에 없는 코드는 코드를 그대로 돌려주므로, user 도메인에 enum 이 추가돼도 프롬프트가 깨지지 않는다.
 */
public final class PromptLabels {

	private static final Map<String, String> ROLES = Map.ofEntries(
			Map.entry("PLANNING_OPERATION", "기획·운영"),
			Map.entry("DESIGN_CONTENT", "디자인·콘텐츠"),
			Map.entry("DEV_TECH", "개발·기술"),
			Map.entry("MARKETING_BRANDING", "마케팅·브랜딩"),
			Map.entry("SALES_CUSTOMER", "영업·고객"),
			Map.entry("DATA_RESEARCH", "데이터·리서치"),
			Map.entry("STRATEGY_MANAGEMENT", "전략·경영"),
			Map.entry("ETC", "기타"));

	private static final Map<String, String> PERSPECTIVES = Map.ofEntries(
			Map.entry("SCHEDULE", "일정"),
			Map.entry("SCOPE", "범위"),
			Map.entry("DECISION", "의사결정"),
			Map.entry("UX", "사용자 경험"),
			Map.entry("TECH_RISK", "기술 리스크"),
			Map.entry("COST_PERFORMANCE", "비용·성과"),
			Map.entry("CUSTOMER_REACTION", "고객 반응"),
			Map.entry("OPERATION_ISSUE", "운영 이슈"),
			Map.entry("ACTION_ITEM", "액션 아이템"),
			Map.entry("TEAM_QUESTION", "팀 질문"));

	private PromptLabels() {
	}

	public static String role(String code) {
		return ROLES.getOrDefault(code, code);
	}

	public static String perspective(String code) {
		return PERSPECTIVES.getOrDefault(code, code);
	}
}
