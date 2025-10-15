/*
 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common;

/**
 * Enum AggregatedInformation represents various pieces of aggregated information about a device.
 *
 * @author Harry / Symphony Dev Team<br>
 * @since 1.0.0
 */
public enum AggregatedInformation {
	MAC("MAC", "mac", ""),
	SN("MachineID", "sn", ""),
	NAME("Name", "name", ""),
	MODEL_NAME("ModelName", "modelName", ""),
	SITE_NAME("SiteName", "siteName", ""),
	PROGRAM_VERSION("FirmwareVersion", "programVersion", ""),
	LAN_IP("PrivateIP", "lanIp", ""),
	LAST_REPORT_TIME("LastReportTime", "lastReportTime", ""),
	DEVICE_STATUS("DeviceStatus", "deviceStatus", ""),
	;

	private final String name;
	private final String field;
	private final String group;

	/**
	 * Constructor for AggregatedInformation.
	 *
	 * @param name The name representing the system information category.
	 * @param group The group associated with the category.
	 */
	AggregatedInformation(String name, String field, String group) {
		this.name = name;
		this.field = field;
		this.group = group;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #field}
	 *
	 * @return value of {@link #field}
	 */
	public String getField() {
		return field;
	}

	/**
	 * Retrieves {@link #group}
	 *
	 * @return value of {@link #group}
	 */
	public String getGroup() {
		return group;
	}
}
