package com.hackathon.server.supportprogram.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 기업마당 접수 중인 서울 소상공인 대상 공고 한 건. 일배치가 통째로 바꾸고, created_datetime(수집 시각)은 DB가 채운다. 기준: docs/database-schema.md support_program */
@Entity
@Table(name = "support_program")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportProgram {

	@Id
	private String id;
	private String title;
	private String url;
	private String agencyName;
	private String fieldName;
	private String targetName;
	@JdbcTypeCode(SqlTypes.ARRAY)
	private List<String> hashtags;
	private String applicationPeriod;
	private LocalDate deadline;
	private OffsetDateTime registeredDatetime;
	@Column(insertable = false, updatable = false)
	private OffsetDateTime createdDatetime;

	public SupportProgram(String id, String title, String url, String agencyName, String fieldName, String targetName,
			List<String> hashtags, String applicationPeriod, LocalDate deadline, OffsetDateTime registeredDatetime) {
		this.id = id;
		this.title = title;
		this.url = url;
		this.agencyName = agencyName;
		this.fieldName = fieldName;
		this.targetName = targetName;
		this.hashtags = hashtags;
		this.applicationPeriod = applicationPeriod;
		this.deadline = deadline;
		this.registeredDatetime = registeredDatetime;
	}
}
