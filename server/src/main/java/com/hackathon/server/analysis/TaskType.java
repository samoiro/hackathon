package com.hackathon.server.analysis;

/**
 * 추천 할 일 유형. 묶음마다 쓸 수 있는 유형이 정해져 있다 (행사 7개 · 원자재 3개 · 부자재 3개).
 * 카드 키 {category}:{taskType}:{itemId 또는 -}의 가운데 값. 기준: docs/api-dtos.md
 */
public enum TaskType {
	/** 행사 · 근무 배치 */
	STAFFING,
	/** 행사 · 재고 */
	STOCK,
	/** 행사 · 영업시간 */
	HOURS,
	/** 행사 · 주문·픽업 안내 */
	PICKUP,
	/** 행사 · 예약 안내 (서비스업) */
	RESERVATION,
	/** 행사 · 매장 정비 */
	FACILITY,
	/** 행사 · 포장 동선 */
	PACKING,
	/** 원자재 · 단가 확인 */
	PRICE_CHECK,
	/** 원자재 · 미리 주문 */
	BUY_AHEAD,
	/** 원자재 · 발주량 점검 */
	ORDER_PLAN,
	/** 부자재 · 재고 채우기 */
	RESTOCK,
	/** 부자재 · 판매 상품 진열 */
	DISPLAY,
	/** 부자재 · 주문 줄이기 */
	ORDER_LESS
}
