/*
 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.metric;

/**
 * DiagnosisStatus of aggregated device
 *
 * @author Harry / Symphony Dev Team<br>
 * @since 1.0.0
 */
public enum DiagnosisStatus {
	IN_PROGRESS("inprogress"),
	SUCCESS("success"),
	FAILURE("failure");

	private final String wireValue;

	/**
	 * @param wireValue lowercase value as returned by the API (e.g., "success")
	 */
	DiagnosisStatus(String wireValue) { this.wireValue = wireValue; }

	public String wireValue() { return wireValue; }

	/**
	 * Parses a wire-format status string into an enum value.
	 * Returns {@link #IN_PROGRESS} for {@code null} or unrecognized input.
	 *
	 * @param s status string from the API (e.g., "success", "failure", "inprogress")
	 * @return corresponding {@link DiagnosisStatus}
	 */
	public static DiagnosisStatus fromWire(String s) {
		if (s == null) return IN_PROGRESS;
		switch (s.toLowerCase()) {
			case "success": return SUCCESS;
			case "failure": return FAILURE;
			case "inprogress":
			default: return IN_PROGRESS;
		}
	}
}
