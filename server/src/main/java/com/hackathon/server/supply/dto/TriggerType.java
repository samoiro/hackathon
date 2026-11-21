package com.hackathon.server.supply.dto;

/** supply_trigger.type. 값은 DB check 제약과 같아야 한다. */
public enum TriggerType {
	WEATHER, EVENT, HOLIDAY, SCHOOL, SEASON
}
