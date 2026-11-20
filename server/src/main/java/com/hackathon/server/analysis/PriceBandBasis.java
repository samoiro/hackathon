package com.hackathon.server.analysis;

/** 가격 단계를 매긴 값. ingredient_price_band.basis와 같다. */
public enum PriceBandBasis {
	/** 조사일 가격 ÷ 평년 가격. 평년 = 최근 5년 같은 날 가격 중 최고·최저를 뺀 3년 평균 */
	NORMAL_RATIO,
	/** 조사일 가격 ÷ 1주 전 평균 가격 − 1. 평년을 못 구한 품목에만 쓴다 */
	WEEKLY_CHANGE
}
