package com.hackathon.server.area.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.hackathon.server.area.dto.AreaResponse;
import com.hackathon.server.area.repository.AreaRepository;

@Service
@RequiredArgsConstructor
public class AreaService {

	private final AreaRepository repository;

	/** 좌표가 없으면 이름순 */
	public List<AreaResponse> areas(Double latitude, Double longitude) {
		return repository.findAll(latitude, longitude);
	}
}
