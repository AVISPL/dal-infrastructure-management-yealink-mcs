/*
 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.constants;

/**
 * YealinkCommand
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 22/10/2024
 * @since 1.0.0
 */
public class YealinkCommand {
	public static final String GET_AUTH = "token";
	public static final String GET_LIST_DEVICES = "dm/listDevices";
	public static final String GET_DEVICES_DETAIL = "dm/devices/%s";
	public static final String GET_NETWORK_INTERFACE = "dm/devices/%s/networkInterfaces";
	public static final String GET_LIST_ACCESSORY = "dm/devices/%s/listParts";
	public static final String GET_DEVICE_COUNT = "dm/statistics/deviceCount?deviceType=%s";
	public static final String REBOOT_URI = "dm/device/reboot";
	public static final String PACKET_CAPTURE_URI = "dm/devices/%s/startPacketCapture";
	public static final String EXPORT_LOG_URI = "dm/devices/%s/exportSyslog";
	public static final String SCREEN_CAPTURE_URI = "dm/devices/%s/captureScreen";
	public static final String STATUS_OF_DIAGNOSIS = "dm/diagnosis/%s/status";
}
