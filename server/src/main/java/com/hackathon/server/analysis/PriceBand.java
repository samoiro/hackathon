package com.hackathon.server.analysis;

/**
 * ingredient_price_band 한 행: 부류별 4주 변동률 분위수 경계. 값 = 조사일 가격 ÷ 4주 전 평균 가격 − 1.
 * 단계 판정(p95 이상이면 상승 심각 … p5 이하면 하락 심각)은 본선에서 구현한다. 기준: docs/database-schema.md ingredient_price_band
 */
public record PriceBand(double p5, double p12_5, double p20, double p80, double p87_5, double p95) {
}
