package com.hackathon.server.analysis.dto;

import java.util.List;

/** Python ai-server POST /v1/analyze 응답 (200). 오류 본문은 {"code", "detail"}. */
public record AiAnalyzeResponse(List<Suggestion> suggestions, String model, String generated_at) {

	public record Suggestion(String card_id, String title, String expected_effect, List<String> sub_tasks) {
	}

	public record Error(String code, String detail) {
	}
}
