package com.hackathon.server.analysis;

import io.swagger.v3.oas.annotations.media.Schema;

/** "172원 (4.1%)". 하락이면 둘 다 음수. */
@Schema(description = "가격 변동")
public record PriceChange(
		@Schema(description = "변동 금액 (원)", example = "172", requiredMode = Schema.RequiredMode.REQUIRED) long amount,
		@Schema(description = "변동률 (%), 소수 첫째 자리", example = "4.1", requiredMode = Schema.RequiredMode.REQUIRED) double percent) {
}
