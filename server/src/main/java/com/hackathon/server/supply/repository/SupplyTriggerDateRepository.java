package com.hackathon.server.supply.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.supply.entity.SupplyTriggerDate;

public interface SupplyTriggerDateRepository extends JpaRepository<SupplyTriggerDate, SupplyTriggerDate.Key> {

	List<SupplyTriggerDate> findByTriggerCodeIn(Collection<String> triggerCodes);
}
