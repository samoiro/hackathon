package com.hackathon.server.supply.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.supply.entity.Supply;

public interface SupplyRepository extends JpaRepository<Supply, Long> {

	List<Supply> findByNameContaining(String name);
}
