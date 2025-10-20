/*
 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.metric;

/**
 * AccessoryEnum represent information accessory site of device
 *
 * @author Harry / Symphony Dev Team<br>
 * @since 1.0.0
 */
public enum Accessory {
	ACCESSORY_ID("ID", "id"),
	MAC("MAC", "mac"),
	SN("SerialNumber", "sn"),
	MODEL_ID("ModelID", "modelId"),
	MODEL_NAME("ModelName", "modelName"),
	CONNECT_WAY("ConnectWay", "connectWay"),
	CONN_STATUS("ConnStatus", "connStatus"),
	LAN_IP("PrivateIP", "lanIp"),
	PROGRAM_VERSION("FirmwareVersion", "programVersion"),
	LAST_REPORT_TIME("LastReportTime", "lastReportTime"),
	;
	private final String name;
	private final String field;

	/**
	 * Constructor for Accessory.
	 *
	 * @param name The name representing the system information category.
	 * @param field The field associated with the category.
	 */
	Accessory(String name, String field) {
		this.name = name;
		this.field = field;
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
}
