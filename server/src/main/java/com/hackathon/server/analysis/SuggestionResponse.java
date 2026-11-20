package com.hackathon.server.analysis;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** 추천 카드 (순서도 18~19). Python 응답을 Java가 검증해 저장한 것. */
@Schema(description = "추천 카드")
public record SuggestionResponse(
		@Schema(description = "추천 id", requiredMode = Schema.RequiredMode.REQUIRED) long id,
		@Schema(description = "카드 키 {category}:{taskType}:{itemId 또는 -}. 피드백·해당 없어요 제외에 씀", example = "INGREDIENT:PRICE_CHECK:12", requiredMode = Schema.RequiredMode.REQUIRED) String cardKey,
		@Schema(description = "유형 라벨", requiredMode = Schema.RequiredMode.REQUIRED) SuggestionCategory category,
		@Schema(description = "행사 할 일 유형. EVENT만 있고 나머지는 null") EventTaskType taskType,
		@Schema(description = "원자재·부자재 id. EVENT는 null") Long itemId,
		@Schema(description = "할 일 제목 (24자 이내)", example = "배추 거래처 단가 확인", requiredMode = Schema.RequiredMode.REQUIRED) String title,
		@Schema(description = "한 줄 사유: 왜 추천했는지 정보형으로 (순서도 19)", example = "배추가 1주 전보다 4.1% 올랐어요.", requiredMode = Schema.RequiredMode.REQUIRED) String reason,
		@Schema(description = "기대 효과 (Figma 시안 문구, 60자 이내). 순서도에는 없어서 쓸지 합의 필요") String expectedEffect,
		@Schema(description = "세부 할 일 (최대 3개). 강추면 내 준비할 일에 자동 추가 (순서도 20)", requiredMode = Schema.RequiredMode.REQUIRED) List<String> subTasks,
		@Schema(description = "강추 여부. 모든 추천 중 근거 점수 상위 3개 (같은 유형 최대 2개). 기준: ai-server/docs/API_CONTRACT_DRAFT.md 강추 고르기", requiredMode = Schema.RequiredMode.REQUIRED) boolean recommended,
		@Schema(description = "근거 id (E·H·W·R·S + 번호)", requiredMode = Schema.RequiredMode.REQUIRED) List<String> evidenceIds) {
}
