package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.constants;

/**
 * YealinkConstant class used in monitoring
 *
 * @author Harry / Symphony Dev Team<br>
 * @since 1.0.0
 */
public class YealinkConstant {
	public static final String BASE_URL = "v2";
	public static final String HASH = "#";
	public static final String REQUEST_BODY = "{\"grant_type\":\"client_credentials\"}";
	public static final String NONE = "None";
	public static final String NOT_AVAILABLE = "N/A";
	public static final String EMPTY = "";
	public static final String DEVICE_TYPE = "deviceType";
	public static final String DATA = "data";
	public static final String ACCESS_TOKEN = "access_token";
	public static final String TOKEN_TYPE = "token_type";
	public static final String EXPIRES_IN = "expires_in";
	public static final String RESOURCE_ALREADY_EXISTS_CODE = "800003";
	public static final String CANNOT_BE_NULL_CODE = "900400";
	public static final String MODEL_NAME = "modelName";
	public static final String ACCESSORY = "Accessory_";
	public static final String DEVICE_ONLINE = "online";
	public static final String FAILURE_COUNT = "failureCount";
	public static final String ERROR = "errors";
	public static final String REBOOT = "Reboot";
	public static final String PACKET_CAPTURE = "PacketCapture";
	public static final String EXPORT_LOG = "ExportLog";
	public static final String SCREEN_CAPTURE = "ScreenCapture";
	public static final String CONTROL_MANAGEMENT = "Controls";
	public static final String DEFAULT_TYPE_TO_REBOOT = "3";
	public static final int DEFAULT_LIMIT = 20;
	public static final int MAX_LIMIT = 1000;
	public static final String MONITORING_CYCLE_DURATION = "LastMonitoringCycleDuration(s)";
	public static final String ADAPTER_VERSION = "AdapterVersion";
	public static final String MONITORED_DEVICES_TOTAL = "MonitoredDevicesTotal";
	public static final String ADAPTER_BUILD_DATE = "AdapterBuildDate";
	public static final String ADAPTER_UPTIME_MIN = "AdapterUptime(min)";
	public static final String ADAPTER_UPTIME = "AdapterUptime";
}
