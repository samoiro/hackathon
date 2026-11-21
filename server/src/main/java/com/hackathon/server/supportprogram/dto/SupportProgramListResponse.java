package com.hackathon.server.supportprogram.dto;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * GET /api/support-programs. 대시보드 "마감 임박 지원사업" 카드.
 * 기업마당 지원사업 공고(공공데이터포털 15157820) 중 서울·전국 소상공인 대상이고 오늘 이후에 마감되는 공고.
 * 원본은 하루 한 번 갱신되고 한 번 받는 데 몇 초 걸려서 서버가 모아 두고 내준다.
 */
@Schema(description = "지원사업 공고 목록")
public record SupportProgramListResponse(
		@Schema(description = "공고 (마감일 빠른 순, 최대 5개). '예산 소진시까지'처럼 마감일이 없는 공고는 뺀다", requiredMode = Schema.RequiredMode.REQUIRED) List<SupportProgramResponse> items,
		@Schema(description = "원본을 확인한 시각 (출처 표시)", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime checkedAt) {
}
