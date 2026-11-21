package com.hackathon.server.analysis;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** 추천 카드 (순서도 18~19). Java가 칸·사유·준비 기한·강추를 정하고, Python(LLM)이 제목·기대 효과·세부 할 일을 쓴다. */
@Schema(description = "추천 카드")
public record SuggestionResponse(
		@Schema(description = "추천 id", requiredMode = Schema.RequiredMode.REQUIRED) long id,
		@Schema(description = "카드 키 {category}:{taskType}:{itemId 또는 -}. 피드백·해당 없어요 제외에 씀", example = "INGREDIENT:PRICE_CHECK:12", requiredMode = Schema.RequiredMode.REQUIRED) String cardKey,
		@Schema(description = "유형 라벨", requiredMode = Schema.RequiredMode.REQUIRED) SuggestionCategory category,
		@Schema(description = "할 일 유형. 세 묶음 모두 필수이고 묶음마다 쓸 수 있는 유형이 정해져 있다", requiredMode = Schema.RequiredMode.REQUIRED) TaskType taskType,
		@Schema(description = "원자재·부자재 id. EVENT는 null") Long itemId,
		@Schema(description = "할 일 제목 (LLM, 24자 이내)", example = "배추 거래처 단가 확인", requiredMode = Schema.RequiredMode.REQUIRED) String title,
		@Schema(description = "한 줄 사유: 왜 추천했는지 정보형으로. 서버가 대표 근거(evidenceIds 첫 번째)의 숫자로 템플릿을 채운다 (순서도 19)", example = "배추가 4주 전보다 10.9% 올랐어요.", requiredMode = Schema.RequiredMode.REQUIRED) String reason,
		@Schema(description = "준비 기한: 이날까지 하면 좋은 날. 강추 '급한 정도'의 행동 마감과 같다. EVENT는 대표 근거의 시작일(행사·시즌 시작일, 날씨 영향 첫날, 진행 중이면 기준일), SUPPLY는 prepareBy, INGREDIENT는 null('이번 주 발주 전'으로 표시)") LocalDate dueDate,
		@Schema(description = "기대 효과 (LLM, 60자 이내, '~수 있어요.'). reason과 같은 대표 근거에서 나온 효과만, 숫자·매출·손님 수 없이 쓴다. 근거로 말할 효과가 없으면 null이고 화면은 그 줄을 숨긴다", example = "배추값이 오른 만큼 메뉴 원가 부담을 미리 살펴볼 수 있어요.") String expectedEffect,
		@Schema(description = "세부 할 일 (LLM, 2~3개). 강추면 내 준비할 일에 자동 추가하고, 이미 했어요(완료)·해당 없어요(할 일에서 뺌)로 정리한다 (순서도 20)", requiredMode = Schema.RequiredMode.REQUIRED) List<String> subTasks,
		@Schema(description = "강추 여부. 모든 추천 중 근거 점수 상위 3개 (같은 유형 최대 2개). 강추도 해당 없어요로 뺄 수 있다. 기준: ai-server/docs/API_CONTRACT_DRAFT.md 강추 고르기", requiredMode = Schema.RequiredMode.REQUIRED) boolean recommended,
		@Schema(description = "근거 id (E·H·W·R·S + 번호). 첫 번째가 대표 근거로, 카드를 그 행사·품목과 묶어 보여 준다", requiredMode = Schema.RequiredMode.REQUIRED) List<String> evidenceIds) {
}
