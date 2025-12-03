/*
 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.metric;

/**
 * DeviceType represent device type filter when user input from adapter property
 *
 * @author Harry / Symphony Dev Team<br>
 * @since 1.0.0
 */
public enum DeviceType {

	PHONE_DEVICE("PhoneDevice", "1"),
	ROOM_DEVICE("RoomDevice", "3");

	private final String canonical;
	private final String code;

	/**
	 * Creates a {@code DeviceType} with its canonical display name and API code.
	 * @param canonical the canonical display name of the device type (non-null)
	 * @param code      the API code mapped to this device type (non-null)
	 */
	DeviceType(String canonical, String code) {
		this.canonical = canonical;
		this.code = code;
	}

	public String code() { return code; }
	public String canonical() { return canonical; }

	/**
	 * Parses a user-provided string into a {@link DeviceType}.
	 * @param input raw user input, possibly containing different casing or separators
	 * @return the matching {@code DeviceType}, or {@code null} if input is blank
	 * @throws IllegalArgumentException if the input is non-blank but not a supported device type
	 */
	public static DeviceType fromString(String input) {
		if (input == null || input.trim().isEmpty()) return null;
		String norm = input.trim().replaceAll("[^A-Za-z]", "").toLowerCase();
		switch (norm) {
			case "phonedevice":
				return PHONE_DEVICE;
			case "roomdevice":
				return ROOM_DEVICE;
			default:
				return null;
		}
	}
}
