package com.hackathon.server.publicdata.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hackathon.server.publicdata.dto.ListResponse;

/** 공공데이터포털(data.go.kr) 한국농수산식품유통공사 가격 추이 정보 (B552845/priceSequel). */
@Component
public class AgroPriceClient {

	private final DataGoKrClient dataGoKrClient;

	public AgroPriceClient(DataGoKrClient dataGoKrClient) {
		this.dataGoKrClient = dataGoKrClient;
	}

	/** conditions: 컬럼명(se_cd, ctgry_cd, item_cd, vrty_cd, grd_cd) → 값. null 값은 조건에서 제외. */
	public ListResponse priceTrends(String date, Map<String, String> conditions, int page, int size) {
		Map<String, String> query = new LinkedHashMap<>();
		query.put("pageNo", String.valueOf(page));
		query.put("numOfRows", String.valueOf(size));
		query.put("returnType", "JSON");
		query.put("cond[exmn_ymd::EQ]", date);
		conditions.forEach((k, v) -> query.put("cond[" + k + "::EQ]", v));
		return dataGoKrClient.list("/B552845/priceSequel/info", query);
	}
}
