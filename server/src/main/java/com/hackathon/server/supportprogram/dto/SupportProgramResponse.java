package com.hackathon.server.supportprogram.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/** 기업마당 공고 한 건. 출처표시·변경금지(공공누리 제3유형)라 제목과 기관명은 원문 그대로 둔다. */
@Schema(description = "지원사업 공고")
public record SupportProgramResponse(
		@Schema(description = "공고 id (pblancId)", example = "PBLN_000000000128062", requiredMode = Schema.RequiredMode.REQUIRED) String id,
		@Schema(description = "공고명 (pblancNm)", example = "2026년 소상공인 법률ㆍ노무 현장 상담 지원사업 모집 공고", requiredMode = Schema.RequiredMode.REQUIRED) String title,
		@Schema(description = "기업마당 원문 링크 (pblancUrl)", requiredMode = Schema.RequiredMode.REQUIRED) String url,
		@Schema(description = "소관기관 (jrsdInsttNm)", example = "중소벤처기업부", requiredMode = Schema.RequiredMode.REQUIRED) String agency,
		@Schema(description = "지원 분야 (pldirSportRealmLclasCodeNm). 모르면 null", example = "경영") String field,
		@Schema(description = "마감일 (오늘이거나 그 뒤)", example = "2026-11-25", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate deadline,
		@Schema(description = "신청 기간 원문 (reqstBeginEndDe)", example = "2026-11-19 ~ 2026-11-25", requiredMode = Schema.RequiredMode.REQUIRED) String applicationPeriod) {
}
