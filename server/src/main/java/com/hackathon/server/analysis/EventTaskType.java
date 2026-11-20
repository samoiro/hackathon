package com.hackathon.server.analysis;

/** 행사로 만들 수 있는 할 일 유형. 서버가 업종·운영 방식·행사 시간으로 골라 LLM에 넘기고, LLM은 이 안에서만 쓴다. */
public enum EventTaskType {
	/** 근무 배치 */
	STAFFING,
	/** 영업시간 */
	HOURS,
	/** 주문·픽업 안내 */
	PICKUP,
	/** 포장 동선 */
	PACKING,
	/** 재고 */
	STOCK,
	/** 예약 안내 */
	RESERVATION
}
