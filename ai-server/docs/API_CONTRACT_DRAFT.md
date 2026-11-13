# Java ↔ Python API 계약 초안 (설계 전용)

**구현하지 않은 API의 제안 명세입니다.** 실제 경로 및 필드는 Java 담당자와 합의 후 확정하세요.

## 예정 엔드포인트
`POST /v1/analyze` (본선 당일 구현)

### 요청 예시
```json
{
  "store": {
    "district": "서울 성동구 성수동1가",
    "business_type": "카페",
    "items": ["원두", "우유", "테이크아웃 컵"]
  },
  "events": [],
  "cost_signals": []
}
```

### 응답 예시 (스키마 합의용 가상 데이터)
```json
{
  "summary": "분석 결과가 이곳에 들어갑니다.",
  "actions": [
    {
      "title": "실행할 일",
      "reason": "근거와 연결된 설명",
      "priority": "medium",
      "evidence_ids": []
    }
  ],
  "warnings": [],
  "generated_at": "2026-11-21T00:00:00+09:00"
}
```

## 결정 필요
- 출처 객체 구조 (source_id, URL, 게시일, 확인일)
- 데이터 없을 때 동작 (LLM 호출 여부, 에러 규약)
- 인증 방식 (내부 토큰 또는 네트워크 격리)
- 응답 시간 제한 / 재시도 / 타임아웃
- 기밀/개인정보를 LLM으로 보낼지 여부
- JSON 스키마 및 enum 값의 최종 확정
