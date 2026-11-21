package com.hackathon.server.supply.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.server.supply.dto.SupplyResponse;
import com.hackathon.server.supply.repository.SupplyRepository;

@Service
public class SupplyService {

	private final SupplyRepository repository;

	public SupplyService(SupplyRepository repository) {
		this.repository = repository;
	}

	/** 이름에 query가 들어간 부자재를 전부 이름순으로. query가 없으면 전체. */
	@Transactional(readOnly = true)
	public List<SupplyResponse> search(String query) {
		return repository.findByNameContaining(query == null ? "" : query.strip()).stream()
				// DB collation은 한글을 가나다순으로 정렬하지 않아 코드 포인트 순으로 다시 정렬한다
				.sorted(Comparator.comparing(x -> x.getName()))
				.map(s -> new SupplyResponse(s.getId(), s.getName()))
				.toList();
	}
}
