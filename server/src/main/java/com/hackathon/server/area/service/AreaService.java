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

	public List<AreaResponse> areas() {
		return repository.findAll();
	}
}
