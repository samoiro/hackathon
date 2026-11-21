package com.hackathon.server.analysis.dto;

import java.util.List;

/**
 * Python ai-server POST /v1/analyze 요청. 이름은 계약(snake_case) 그대로라 변환 설정 없이 직렬화한다.
 * 기준: ai-server/docs/API_CONTRACT_DRAFT.md, ai-server/app/schemas.py
 */
public record AiAnalyzeRequest(
		String request_id,
		String base_time,
		Store store,
		Search event_search,
		List<Event> events,
		List<Season> season_events,
		Weather weather,
		List<RawMaterial> raw_materials,
		List<Supply> supplies,
		List<Card> cards) {

	public record Store(String gu, String area_label, String industry_category, String industry_name, OperationType operation_type) {
	}

	public record Search(int radius_m, int days) {
	}

	public record Event(String evidence_id, String title, String category, String place, String date_label, EventStatus status,
			int distance_m, int walk_minutes, Boolean is_free, EventTrigger trigger, String time_band, String peak_label) {
	}

	public record Season(String evidence_id, String code, String label, String type, String date_label, long days_left) {
	}

	public record Warning(String kind, String level) {
	}

	public record Weather(String evidence_id, List<WeatherImpact> impacts, List<Warning> warnings, String first_date_label) {
	}

	public record Signal(String text, String demand) {
	}

	public record RawMaterial(String evidence_id, long item_id, String name, String unit, String grade, String base_date,
			Long price, Double change_1w_pct, Double change_4w_pct, Trend trend, Integer consecutive_weeks, String price_stage,
			List<Signal> signals) {
	}

	public record Supply(String evidence_id, long item_id, String name, String kind, String demand, String trigger_label,
			String trigger_evidence_id, String prepare_by_label) {
	}

	public record Card(String card_id, SuggestionCategory category, TaskType task_type, Long item_id, List<String> evidence_ids) {
	}
}
