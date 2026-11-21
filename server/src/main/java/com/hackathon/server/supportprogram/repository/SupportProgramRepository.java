package com.hackathon.server.supportprogram.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.supportprogram.entity.SupportProgram;

/** 서울 소상공인 대상 공고만 저장돼 있다(SupportProgramService.isTarget). */
public interface SupportProgramRepository extends JpaRepository<SupportProgram, String> {

	List<SupportProgram> findTop5ByDeadlineGreaterThanEqualOrderByDeadlineAsc(LocalDate today);

	Optional<SupportProgram> findFirstByOrderByCreatedDatetimeDesc();
}
