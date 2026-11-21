package com.hackathon.server.batch.dto;

/** batch_job_history의 read/write/skip 건수. */
public record BatchCounts(long read, long write, long skip) {
}
