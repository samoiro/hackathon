package com.hackathon.server.analysis.dto;

/**
 * briefing_analysis_job.input_snapshot. 요청 원문과 1단계에서 확인한 가게 정보(카카오 행정동, 업종·품목 이름).
 *
 * @param adminDongCode 카카오 행정동 코드 10자리 (케이웨더 조회용)
 */
public record AnalysisInput(AnalysisRequest request, StoreSummary store, String adminDongCode) {
}
