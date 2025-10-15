/*
 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.metric;

import java.util.concurrent.atomic.AtomicLong;

/**
 * DiagnosisRecord represent about diagnosis information
 *
 * @author Harry / Symphony Dev Team<br>
 * @since 1.0.0
 */
public class DiagnosisRecord {
	private final String deviceId;
	private final String diagnosisId;
	private volatile DiagnosisStatus status;
	private volatile String url;
	private final AtomicLong updatedAt = new AtomicLong();

	/**
	 * Creates a diagnosis record for a device and sets the initial timestamp.
	 *
	 * @param deviceId    target device identifier
	 * @param diagnosisId diagnosis task identifier returned by the API
	 * @param status      initial status (e.g., IN_PROGRESS)
	 */
	public DiagnosisRecord(String deviceId, String diagnosisId, DiagnosisStatus status) {
		this.deviceId = deviceId;
		this.diagnosisId = diagnosisId;
		this.status = status;
		this.updatedAt.set(System.currentTimeMillis());
	}

	/**
	 * Updates the record's status and URL (if any) and refreshes the timestamp.
	 *
	 * @param status new diagnosis status
	 * @param url    capture URL when {@code status == SUCCESS}; otherwise may be {@code null}
	 */
	public void update(DiagnosisStatus status, String url) {
		this.status = status;
		this.url = url;
		this.updatedAt.set(System.currentTimeMillis());
	}

	public String getDeviceId() { return deviceId; }
	public String getDiagnosisId() { return diagnosisId; }
	public DiagnosisStatus getStatus() { return status; }
	public String getUrl() { return url; }
	public long getUpdatedAt() { return updatedAt.get(); }
}
