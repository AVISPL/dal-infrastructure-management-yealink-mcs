	/*
	 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
	 */

	package com.avispl.symphony.dal.infrastructure.management.yealink.msc;

	import java.io.IOException;
	import java.util.ArrayList;
	import java.util.Arrays;
	import java.util.Collections;
	import java.util.Date;
	import java.util.HashMap;
	import java.util.HashSet;
	import java.util.List;
	import java.util.Map;
	import java.util.Objects;
	import java.util.Properties;
	import java.util.Set;
	import java.util.UUID;
	import java.util.concurrent.ExecutorService;
	import java.util.concurrent.Executors;
	import java.util.concurrent.TimeUnit;
	import java.util.concurrent.locks.ReentrantLock;
	import java.util.stream.Collectors;

	import org.springframework.http.HttpHeaders;
	import org.springframework.http.HttpMethod;
	import org.springframework.util.CollectionUtils;

	import com.fasterxml.jackson.databind.JsonNode;
	import com.fasterxml.jackson.databind.ObjectMapper;
	import com.fasterxml.jackson.databind.node.ArrayNode;
	import com.fasterxml.jackson.databind.node.ObjectNode;
	import javax.security.auth.login.FailedLoginException;


	import com.avispl.symphony.api.dal.control.Controller;
	import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
	import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
	import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
	import com.avispl.symphony.api.dal.dto.monitor.Statistics;
	import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
	import com.avispl.symphony.api.dal.error.CommandFailureException;
	import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
	import com.avispl.symphony.api.dal.monitor.Monitorable;
	import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
	import com.avispl.symphony.dal.communicator.RestCommunicator;
	import com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.AggregatedInformation;
	import com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.LoginInfo;
	import com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.constants.Util;
	import com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.constants.YealinkCommand;
	import com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.constants.YealinkConstant;
	import com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.metric.Accessory;
	import com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.metric.DeviceType;
	import com.avispl.symphony.dal.util.ControllablePropertyFactory;
	import com.avispl.symphony.dal.util.StringUtils;


	/**
	 * YealinkCommunicator
	 * Supported features are:
	 * Monitoring Aggregator Device:
	 *  <ul>
	 *    <li> Adapter Metadata </li>
	 *  <ul>
	 *
	 * General Info Aggregated Device:
	 * <ul>
	 *   Monitoring device:
	 *   <li> deviceId </li>
	 *   <li> deviceName </li>
	 *   <li> deviceOnline </li>
	 *   <li> DeviceStatus </li>
	 *   <li> FirmwareVersion </li>
	 *   <li> LastReportTime </li>
	 *   <li> MAC </li>
	 *   <li> MachineID </li>
	 *   <li> ModelName </li>
	 *   <li> PrivateIP </li>
	 *   <li> SiteName </li>
	 *   Accessory group
	 *   <ul>
	 *     <li>ConnectionMode</li>
	 *     <li>ConnectStatus</li>
	 *     <li>ID</li>
	 *     <li>LastReportTime</li>
	 *     <li>MAC</li>
	 *     <li>ModelID</li>
	 *     <li>ModelName</li>
	 *     <li>PrivateIP</li>
	 *     <li>ProgramVersion</li>
	 *     <li>SN</li>
	 *   </ul>
	 *   Control group
	 *   <ul>
	 *     <li>ExportLog</li>
	 *     <li>PacketCapture</li>
	 *     <li>Reboot</li>
	 *     <li>ScreenCapture</li>
	 *   </ul>
	 * </ul>
	 *
	 * @author Harry / Symphony Dev Team<br>
	 * @since 1.0.0
	 */
	public class YealinkCommunicator extends RestCommunicator implements Aggregator, Monitorable, Controller {
		/**
		 * How much time last monitoring cycle took to finish
		 */
		private Long lastMonitoringCycleDuration;

		/** Adapter metadata properties - adapter version and build date */
		private final Properties adapterProperties;

		/**
		 * Device adapter instantiation timestamp.
		 */
		private long adapterInitializationTimestamp;

		/**
		 * Indicates whether a device is considered as paused.
		 * True by default so if the system is rebooted and the actual value is lost -> the device won't start stats
		 * collection unless the {@link YealinkCommunicator#retrieveMultipleStatistics()} method is called which will change it
		 * to a correct value
		 */
		private volatile boolean devicePaused = true;

		/**
		 * We don't want the statistics to be collected constantly, because if there's not a big list of devices -
		 * new devices' statistics loop will be launched before the next monitoring iteration. To avoid that -
		 * this variable stores a timestamp which validates it, so when the devices' statistics is done collecting, variable
		 * is set to currentTime + 30s, at the same time, calling {@link #retrieveMultipleStatistics()} and updating the
		 */
		private long nextDevicesCollectionIterationTimestamp;

		/**
		 * This parameter holds timestamp of when we need to stop performing API calls
		 * It used when device stop retrieving statistic. Updated each time of called #retrieveMultipleStatistics
		 */
		private volatile long validRetrieveStatisticsTimestamp;

		/**
		 * Aggregator inactivity timeout. If the {@link YealinkCommunicator#retrieveMultipleStatistics()}  method is not
		 * called during this period of time - device is considered to be paused, thus the Cloud API
		 * is not supposed to be called
		 */
		private static final long retrieveStatisticsTimeOut = 3 * 60 * 1000;

		/**
		 * Enable/disable controllable properties on aggregated devices
		 * */
		private boolean configManagement = false;

		/**
		 * Update the status of the device.
		 * The device is considered as paused if did not receive any retrieveMultipleStatistics()
		 * calls during {@link YealinkCommunicator}
		 */
		private synchronized void updateAggregatorStatus() {
			devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
		}

		/**
		 * Uptime time stamp to valid one
		 */
		private synchronized void updateValidRetrieveStatisticsTimestamp() {
			validRetrieveStatisticsTimestamp = System.currentTimeMillis() + retrieveStatisticsTimeOut;
			updateAggregatorStatus();
		}

		/**
		 * Executor that runs all the async operations, that is posting and
		 */
		private ExecutorService executorService;

		/**
		 * the login info
		 */
		private LoginInfo loginInfo;

		/**
		 * A private field that represents an instance of the YealinkCloudLoader class, which is responsible for loading device data for YealinkCloud
		 */
		private YealinkCloudDataLoader deviceDataLoader;

		/**
		 * A private final ReentrantLock instance used to provide exclusive access to a shared resource
		 * that can be accessed by multiple threads concurrently. This lock allows multiple reentrant
		 * locks on the same shared resource by the same thread.
		 */
		private final ReentrantLock reentrantLock = new ReentrantLock();

		/**
		 * Private variable representing the local extended statistics.
		 */
		private ExtendedStatistics localExtendedStatistics;

		/**
		 * A cache that maps route names to their corresponding values.
		 */
		private final Map<String, String> cacheValue = new HashMap<>();

		/**
		 * List of aggregated device
		 */
		private final List<AggregatedDevice> aggregatedDeviceList = Collections.synchronizedList(new ArrayList<>());

		/**
		 * Cached data
		 */
		private final Map<String, Map<String, String>> cachedMonitoringDevice = Collections.synchronizedMap(new HashMap<>());

		/**
		 * save time get token
		 */
		private Long tokenExpire;

		private static final long TOKEN_SKEW_MS = TimeUnit.SECONDS.toMillis(30);

		/**
		 * Device type filter used in Yealink API requests (sent as {@code filter.deviceType}).
		 */
		private String deviceTypeFiltering = "";

		/**
		 * Retrieves {@link #deviceTypeFiltering}
		 *
		 * @return value of {@link #deviceTypeFiltering}
		 */
		public String getDeviceTypeFiltering() {
			return deviceTypeFiltering;
		}

		/**
		 * Sets {@link #deviceTypeFiltering} value
		 *
		 * @param deviceTypeFiltering new value of {@link #deviceTypeFiltering}
		 */
		public void setDeviceTypeFiltering(String deviceTypeFiltering) {
			DeviceType type = DeviceType.fromString(deviceTypeFiltering);
			if (type == null) {
				this.deviceTypeFiltering = "";
			} else {
				this.deviceTypeFiltering = type.canonical();
			}
		}

		private String packetCaptureDuration = "180";

		/**
		 * Retrieves {@link #packetCaptureDuration}
		 *
		 * @return value of {@link #packetCaptureDuration}
		 */
		public String getPacketCaptureDuration() {
			return packetCaptureDuration;
		}

		/**
		 * Sets {@link #packetCaptureDuration} value
		 *
		 * @param packetCaptureDuration new value of {@link #packetCaptureDuration}
		 */
		public void setPacketCaptureDuration(String packetCaptureDuration) {
			this.packetCaptureDuration = packetCaptureDuration;
		}

		/**
		 * Retrieves {@link #configManagement}
		 *
		 * @return value of {@link #configManagement}
		 */
		public boolean isConfigManagement() {
			return configManagement;
		}

		/**
		 * Sets {@link #configManagement} value
		 *
		 * @param configManagement new value of {@link #configManagement}
		 */
		public void setConfigManagement(boolean configManagement) {
			this.configManagement = configManagement;
		}

		/**
		 * A mapper for reading and writing JSON using Jackson library.
		 * ObjectMapper provides functionality for converting between Java objects and JSON.
		 * It can be used to serialize objects to JSON format, and deserialize JSON data to objects.
		 */
		ObjectMapper objectMapper = new ObjectMapper();

		class YealinkCloudDataLoader implements Runnable {
			private volatile boolean inProgress;
			private volatile boolean dataFetchCompleted = false;

			public YealinkCloudDataLoader() {
				inProgress = true;
			}

			@Override
			public void run() {
				loop:
				while (inProgress) {
					long startCycle = System.currentTimeMillis();
					try {
						try {
							TimeUnit.MILLISECONDS.sleep(500);
						} catch (InterruptedException e) {
							logger.info(String.format("Sleep for 0.5 second was interrupted with error message: %s", e.getMessage()));
						}

						if (!inProgress) {
							break loop;
						}

						// next line will determine whether DT Studio monitoring was paused
						updateAggregatorStatus();
						if (devicePaused) {
							continue loop;
						}
						if (logger.isDebugEnabled()) {
							logger.debug("Fetching other than aggregated device list");
						}

						long currentTimestamp = System.currentTimeMillis();
						if (!dataFetchCompleted && nextDevicesCollectionIterationTimestamp <= currentTimestamp) {
							populateListDevice();
							dataFetchCompleted = true;
						}

						while (nextDevicesCollectionIterationTimestamp > System.currentTimeMillis()) {
							try {
								TimeUnit.MILLISECONDS.sleep(1000);
							} catch (InterruptedException e) {
								logger.info(String.format("Sleep for 1 second was interrupted with error message: %s", e.getMessage()));
							}
						}

						if (!inProgress) {
							break loop;
						}
						nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + 30000;
						lastMonitoringCycleDuration = (System.currentTimeMillis() - startCycle) / 1000;
						logger.debug("Finished collecting devices statistics cycle at " + new Date() + ", total duration: " + lastMonitoringCycleDuration);

						if (logger.isDebugEnabled()) {
							logger.debug("Finished collecting devices statistics cycle at " + new Date());
						}
					} catch (Exception e) {
						logger.error("Unexpected error occurred during main device collection cycle", e);
					}
				}
				logger.debug("Main device collection loop is completed, in progress marker: " + inProgress);
				// Finished collecting
			}

			/**
			 * Triggers main loop to stop
			 */
			public void stop() {
				inProgress = false;
			}
		}

		/**
		 * Configurable property for historical properties, comma separated values kept as set locally
		 */
		private final Set<String> historicalProperties = new HashSet<>();

		/**
		 * Retrieves {@link #historicalProperties}
		 *
		 * @return value of {@link #historicalProperties}
		 */
		public String getHistoricalProperties() {
			return String.join(",", this.historicalProperties);
		}

		/**
		 * Sets {@link #historicalProperties} value
		 *
		 * @param historicalProperties new value of {@link #historicalProperties}
		 */
		public void setHistoricalProperties(String historicalProperties) {
			this.historicalProperties.clear();
			Arrays.asList(historicalProperties.split(",")).forEach(propertyName -> {
				this.historicalProperties.add(propertyName.trim());
			});
		}

		/**
		 * Constructs a new instance of YealinkCommunicator.
		 *
		 * @throws IOException If an I/O error occurs while loading the properties mapping YAML file.
		 */
		public YealinkCommunicator() throws IOException {
			setBaseUri(YealinkConstant.BASE_URL);
			adapterProperties = new Properties();
			adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
			this.setTrustAllCertificates(true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void controlProperty(ControllableProperty cp) {
			reentrantLock.lock();
			String property = cp.getProperty();
			String deviceId = cp.getDeviceId();
			String[] parts = property.split(YealinkConstant.HASH);
			String key = property.contains(YealinkConstant.HASH) ? parts[1] : property;

			try{
				boolean exists = aggregatedDeviceList.stream().anyMatch(d -> d.getDeviceId().equals(deviceId));
				if (!exists) throw new IllegalStateException(String.format("Unable to control property: %s as the device does not exist.", property));

				String request;
				JsonNode response;
				switch (key) {
						case YealinkConstant.REBOOT:
							String code = getDeviceTypeCode();
							if(code == null){
								throw new IllegalArgumentException("deviceType can not be empty");
							}
							ObjectNode payload = objectMapper.createObjectNode();
							ArrayNode idsNode = payload.putArray("deviceIds");
							idsNode.add(cp.getDeviceId().trim());
							payload.put(YealinkConstant.DEVICE_TYPE, getDeviceTypeCode());
							response = doPost(YealinkCommand.REBOOT_URI, payload, JsonNode.class);
							if (response.has(YealinkConstant.ERROR) && !Objects.equals(response.get(YealinkConstant.FAILURE_COUNT).asText(), "0")) {
								throw new RuntimeException(String.valueOf(response.get(YealinkConstant.ERROR).get(0).get("msg")));
							}
							break;
						case YealinkConstant.PACKET_CAPTURE:
							payload = objectMapper.createObjectNode();
							payload.put("networkInterface", getNetWorkInterface(deviceId));
							payload.put("type", 3);
							payload.put("duration", String.valueOf(packetCaptureDuration));
							request = String.format(YealinkCommand.PACKET_CAPTURE_URI, deviceId);
							JsonNode startResp = doPut(request, payload, JsonNode.class);
							if (startResp.has(YealinkConstant.ERROR)) {
								throw new RuntimeException(String.format("Have error: %s", startResp.get(YealinkConstant.ERROR)));
							}
							break;
						case YealinkConstant.EXPORT_LOG:
							request = String.format(YealinkCommand.EXPORT_LOG_URI, deviceId);
							response = doPut(request,new HashMap<>(), JsonNode.class);
							if (response.has(YealinkConstant.ERROR)) {
								throw new RuntimeException(String.format("Have error: %s", response.get(YealinkConstant.ERROR)));
							}
							break;
						case YealinkConstant.SCREEN_CAPTURE:
							request = String.format(YealinkCommand.SCREEN_CAPTURE_URI, deviceId);
							response = doPut(request,new HashMap<>(), JsonNode.class);
							if (response.has(YealinkConstant.ERROR)) {
								throw new RuntimeException(String.format("Have error: %s", response.get(YealinkConstant.ERROR)));
							}
							break;
						default:
							if (logger.isWarnEnabled()) {
								logger.warn(String.format("Unable to execute %s command on device %s: Not Supported", property, deviceId));
							}
							break;
					}
			} catch (IllegalArgumentException | IllegalStateException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("Unable to control property: %s as the device does not exist.", property));
			} finally {
				reentrantLock.unlock();
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void controlProperties(List<ControllableProperty> controllableProperties) {
			if (CollectionUtils.isEmpty(controllableProperties)) {
				throw new IllegalArgumentException("ControllableProperties can not be null or empty");
			}
			for (ControllableProperty p : controllableProperties) {
				try {
					controlProperty(p);
				} catch (Exception e) {
					logger.error(String.format("Error when control property %s", p.getProperty()), e);
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
			return retrieveMultipleStatistics()
					.stream()
					.filter(aggregatedDevice -> list.contains(aggregatedDevice.getDeviceId()))
					.collect(Collectors.toList());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
			if (checkValidApiToken()) {
				throw new ResourceNotReachableException("API Token cannot be null or empty, please enter valid API token in the password and username field.");
			}
			if (executorService == null) {
				executorService = Executors.newFixedThreadPool(1);
				executorService.submit(deviceDataLoader = new YealinkCloudDataLoader());
			}
			nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
			updateValidRetrieveStatisticsTimestamp();
			if (cachedMonitoringDevice.isEmpty()) {
				return Collections.emptyList();
			}
			return cloneAndPopulateAggregatedDeviceList();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<Statistics> getMultipleStatistics() throws Exception {
			reentrantLock.lock();
			try {
				if (loginInfo == null) {
					loginInfo = new LoginInfo();
				}
				if (checkValidApiToken()) {
					throw new ResourceNotReachableException("API Token cannot be null or empty, please enter valid API token in the password and username field.");
				}
				Map<String, String> stats = new HashMap<>();
				Map<String, String> dynamicStatistics = new HashMap<>();
				ExtendedStatistics extendedStatistics = new ExtendedStatistics();
				retrieveMetadata(stats, dynamicStatistics);

				extendedStatistics.setStatistics(stats);
				extendedStatistics.setDynamicStatistics(dynamicStatistics);
				localExtendedStatistics = extendedStatistics;
			} finally {
				reentrantLock.unlock();
			}
			return Collections.singletonList(localExtendedStatistics);
		}

		/**
		 * {@inheritDoc}
		 * set API Key into Header of Request
		 */
		@Override
		protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) {
			headers.set("Content-Type", "application/json");
			headers.set("timestamp", String.valueOf(System.currentTimeMillis()));
			headers.set("nonce", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
			if (uri.contains(YealinkCommand.GET_AUTH)) {
				headers.setBasicAuth(this.getLogin(), this.getPassword());
			} else {
				headers.setBearerAuth(loginInfo.getAccessToken());
			}
			return headers;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void authenticate() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void internalInit() throws Exception {
			if (logger.isDebugEnabled()) {
				logger.debug("Internal init is called.");
			}
			adapterInitializationTimestamp = System.currentTimeMillis();
			executorService = Executors.newFixedThreadPool(1);
			executorService.submit(deviceDataLoader = new YealinkCloudDataLoader());
			super.internalInit();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void internalDestroy() {
			if (logger.isDebugEnabled()) {
				logger.debug("Internal destroy is called.");
			}
			if (deviceDataLoader != null) {
				deviceDataLoader.stop();
				deviceDataLoader = null;
			}
			if (executorService != null) {
				executorService.shutdownNow();
				executorService = null;
			}
			if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
				localExtendedStatistics.getStatistics().clear();
				localExtendedStatistics.getControllableProperties().clear();
			}
			cacheValue.clear();
			loginInfo = null;
			nextDevicesCollectionIterationTimestamp = 0;
			aggregatedDeviceList.clear();
			cachedMonitoringDevice.clear();
			super.internalDestroy();
		}

		/**
		 * Retrieves metadata information and updates the provided statistics and dynamic map.
		 *
		 * @param stats the map where statistics will be stored
		 * @param dynamicStatistics the map where dynamic statistics will be stored
		 */
		private void retrieveMetadata(Map<String, String> stats, Map<String, String> dynamicStatistics) {
			try {
				if (lastMonitoringCycleDuration != null) {
					dynamicStatistics.put(YealinkConstant.MONITORING_CYCLE_DURATION, String.valueOf(lastMonitoringCycleDuration));
				}

				stats.put(YealinkConstant.ADAPTER_VERSION,
						Util.getDefaultValueForNullData(adapterProperties.getProperty("aggregator.version")));
				stats.put(YealinkConstant.ADAPTER_BUILD_DATE,
						Util.getDefaultValueForNullData(adapterProperties.getProperty("aggregator.build.date")));
				long adapterUptime = System.currentTimeMillis() - adapterInitializationTimestamp;
				stats.put(YealinkConstant.ADAPTER_UPTIME_MIN, String.valueOf(adapterUptime / (1000 * 60)));
				stats.put(YealinkConstant.ADAPTER_UPTIME, Util.normalizeUptime(adapterUptime / 1000));

				dynamicStatistics.put(YealinkConstant.MONITORED_DEVICES_TOTAL, getDeviceCount());
			} catch (Exception e) {
				logger.error("Failed to populate metadata information with projectId ", e);
			}
		}

		/**
		 * Check API token validation
		 * If the token expires, we send a request to get a new token
		 *
		 * @return boolean True if valid user information, and vice versa.
		 */
		private boolean checkValidApiToken() throws Exception {
			if (isTokenExpired()) {
				retrieveToken();
			}
			return loginInfo == null || StringUtils.isNullOrEmpty(loginInfo.getAccessToken());
		}

		private boolean isTokenExpired() {
			if (loginInfo == null) return true;
			Long expiresInSec = loginInfo.getExpiresIn();
			if (expiresInSec == null || expiresInSec <= 0) return true;

			long lifetimeMs = TimeUnit.SECONDS.toMillis(expiresInSec);
			long now = System.currentTimeMillis();
			return now >= (tokenExpire + lifetimeMs - TOKEN_SKEW_MS);
		}

		/**
		 * Retrieves an authorization token using the provided credentials.
		 *
		 * @throws FailedLoginException if login fails due to incorrect credentials
		 * @throws ResourceNotReachableException if the endpoint is unreachable
		 */
		private void retrieveToken() throws FailedLoginException {
			if (StringUtils.isNullOrEmpty(this.getLogin()) || StringUtils.isNullOrEmpty(this.getPassword())) {
				throw new FailedLoginException("Username or Password field is empty. Please check device credentials");
			}
			try {
				JsonNode response = doPost(YealinkCommand.GET_AUTH, YealinkConstant.REQUEST_BODY, JsonNode.class);
				if (response.size() == 1) {
					throw new IllegalArgumentException("ClientId and ClientSecret are not correct");
				}
				if (response.has(YealinkConstant.ACCESS_TOKEN)) {
					this.loginInfo.setAccess_token(response.get(YealinkConstant.ACCESS_TOKEN).asText());
					this.loginInfo.setToken_type(response.get(YealinkConstant.TOKEN_TYPE).asText());
					this.loginInfo.setExpires_in(response.get(YealinkConstant.EXPIRES_IN).asLong());
					tokenExpire = System.currentTimeMillis();
					this.loginInfo.setLoginDateTime(System.currentTimeMillis());
				} else {
					loginInfo = null;
				}
			} catch (CommandFailureException | FailedLoginException e) {
				throw new FailedLoginException("Unable to retrieve the authorization token, endpoint not reachable");
			} catch (Exception e) {
				throw new ResourceNotReachableException(e.getMessage(), e);
			}
		}

		/**
		 * Loads devices from Yealink and updates the internal cache with per-device fields.
		 * @throws ResourceNotReachableException if any call to list or detail endpoints fails
		 * or an unexpected error occurs during population.
		 */
		private void populateListDevice() {
			try {
				Map<String, String> filterDeviceType = new HashMap<>();
				Map<String, Object> extraField = new HashMap<>();
				if(!Objects.equals(deviceTypeFiltering, "")) {
					filterDeviceType.put("deviceType", getDeviceTypeCode());
				}
				extraField.put("filter", filterDeviceType);

				ObjectNode body = Util.buildRequestBody(0, 20, true, extraField, objectMapper);
				JsonNode root = this.doPost(YealinkCommand.GET_LIST_DEVICES, body, JsonNode.class);

				JsonNode data = root.path(YealinkConstant.DATA);
				if (data == null || !data.isArray() || data.isEmpty()) {
					return;
				}

				for (JsonNode node : data) {
					String deviceId = node.get("id").asText("");
					if (deviceId.isEmpty()) continue;

					JsonNode detail = this.doGet(String.format(YealinkCommand.GET_DEVICES_DETAIL, deviceId), JsonNode.class);
					if (detail == null || detail.isNull()) continue;

					Map<String, String> mappingValue = new HashMap<>();

					for (AggregatedInformation info : AggregatedInformation.values()) {
						String field = info.getField();
						if (!YealinkConstant.EMPTY.equals(field) && detail.has(field)) {
							mappingValue.put(info.getName(), detail.path(field).asText(YealinkConstant.NOT_AVAILABLE));
						}
					}
					putMapIntoCachedData(deviceId, mappingValue);
				}
			} catch (Exception e) {
				throw new ResourceNotReachableException("Error when retrieving list devices info", e);
			}
		}

		/**
		 * Puts the provided mapping values into the cached monitoring data for the specified device ID.
		 *
		 * @param deviceId The ID of the device.
		 * @param mappingValue The mapping values to be added.
		 */
		private void putMapIntoCachedData(String deviceId, Map<String, String> mappingValue) {
			synchronized (cachedMonitoringDevice) {
				Map<String, String> map = new HashMap<>();
				if (cachedMonitoringDevice.get(deviceId) != null) {
					map = cachedMonitoringDevice.get(deviceId);
				}
				map.putAll(mappingValue);
				cachedMonitoringDevice.put(deviceId, map);
			}
		}

		/**
		 * Returns the API code for the current device type filter.
		 */
		public String getDeviceTypeCode() {
			if (deviceTypeFiltering == null || deviceTypeFiltering.isEmpty()) return null;
			return DeviceType.fromString(deviceTypeFiltering).code();
		}

		/**
		 * Clones and populates a new list of aggregated devices with mapped monitoring properties.
		 *
		 * @return A new list of {@link AggregatedDevice} objects with mapped monitoring properties.
		 */
		private List<AggregatedDevice> cloneAndPopulateAggregatedDeviceList() {
			List<AggregatedDevice> devices = new ArrayList<>();

			cachedMonitoringDevice.forEach((deviceId, cachedData) -> {
				AggregatedDevice aggregatedDevice = new AggregatedDevice();
				String deviceStatus = cachedData.get(AggregatedInformation.DEVICE_STATUS.getName());
				aggregatedDevice.setDeviceId(deviceId);
				aggregatedDevice.setDeviceName(cachedData.get(AggregatedInformation.NAME.getName()));
				aggregatedDevice.setDeviceOnline(YealinkConstant.DEVICE_ONLINE.equalsIgnoreCase(deviceStatus));

				Map<String, String> stats = new HashMap<>();
				List<AdvancedControllableProperty> controls = new ArrayList<>();
				mapMonitorProperty(cachedData, stats);
				mapAccessory(deviceId, stats);
				mapControllableProperty(stats, controls);

				aggregatedDevice.setProperties(stats);
				aggregatedDevice.setTimestamp(System.currentTimeMillis());
				aggregatedDevice.setDynamicStatistics(Collections.emptyMap());
				if (!configManagement) {
					controls.clear();
					controls.add(ControllablePropertyFactory.createText(YealinkConstant.EMPTY,YealinkConstant.EMPTY));
				}
				aggregatedDevice.setControllableProperties(controls);
				devices.add(aggregatedDevice);
			});

			synchronized (aggregatedDeviceList) {
				aggregatedDeviceList.clear();
				aggregatedDeviceList.addAll(devices);
				return new ArrayList<>(aggregatedDeviceList);
			}
		}

		/**
		 * Fetches accessory telemetry for a device and maps it into {@code stats}.
		 * @param deviceId target device identifier
		 * @param stats    destination map to receive accessory fields
		 * @throws ResourceNotReachableException if the accessory list cannot be retrieved or parsed
		 */
		private void mapAccessory(String deviceId, Map<String, String> stats) {
			try{
				ObjectNode body = Util.buildRequestBody(0, 20, true, null, objectMapper);
				JsonNode listAccessory = this.doPost(String.format(YealinkCommand.GET_LIST_ACCESSORY, deviceId), body, JsonNode.class );
				if(listAccessory != null && listAccessory.has(YealinkConstant.DATA) && listAccessory.get(YealinkConstant.DATA).isArray()){
					for (JsonNode item : listAccessory.get(YealinkConstant.DATA)){
						String group = item.get(YealinkConstant.MODEL_NAME).asText();
						for (Accessory accessory : Accessory.values()){
							String nameProperty = accessory.getName();
							String value = item.get(accessory.getField()).asText();
							switch (accessory){
								case LAST_REPORT_TIME:
									long lastReportTime = Long.parseLong(value);
									stats.put(YealinkConstant.ACCESSORY + group + YealinkConstant.HASH + nameProperty, Util.formatEpochUtc(lastReportTime));
									break;
								case CONN_STATUS:
									stats.put(YealinkConstant.ACCESSORY + group + YealinkConstant.HASH + nameProperty, Objects.equals(value, "0") ? "Offline" : "Online");
									break;
								case CONNECT_WAY:
									stats.put(YealinkConstant.ACCESSORY + group + YealinkConstant.HASH + nameProperty, Util.getDefaultValueForNullData(Util.uppercaseFirstCharacter(value.toLowerCase())));
									break;
								default:
									stats.put(YealinkConstant.ACCESSORY + group + YealinkConstant.HASH + nameProperty, Util.getDefaultValueForNullData(value));
									break;
							}
						}
					}
				}
			}catch (Exception e) {
				throw new ResourceNotReachableException("Error when retrieving list accessory info", e);
			}
		}

		/**
		 * Maps monitoring properties from cached values to statistics and advanced control properties.
		 *
		 * @param cachedValue The cached values map containing raw monitoring data.
		 * @param stats The statistics map to store mapped monitoring properties.
		 */
		private void mapMonitorProperty(Map<String, String> cachedValue, Map<String, String> stats) {
			try{
				for (AggregatedInformation item : AggregatedInformation.values()) {
					String name = item.getGroup() + item.getName();
					String value = cachedValue.get(name);
					switch (item) {
						case LAST_REPORT_TIME:
							long lastReportTime = Long.parseLong(value);
							stats.put(name, Util.formatEpochUtc(lastReportTime));
							break;
						case NAME:
							stats.remove(name);
							break;
						default:
							stats.put(name, Util.getDefaultValueForNullData(value));
							break;
					}
				}
			} catch (Exception e) {
				logger.error("Error while populate aggregated device info", e);
			}
		}

		/**
		 * Maps controllable properties to the provided stats and advancedControllableProperties lists.
		 *
		 * @param stats A map containing the statistics to be populated with controllable properties.
		 * @param control A list of AdvancedControllableProperty objects to be populated with controllable properties.
		 */
		private void mapControllableProperty(Map<String, String> stats, List<AdvancedControllableProperty> control) {
			Util.addAdvancedControlProperties(control, stats, Util.createButton(YealinkConstant.CONTROL_MANAGEMENT + YealinkConstant.HASH + YealinkConstant.REBOOT, "Reboot", "Rebooting", TimeUnit.MINUTES.toMillis(3)), YealinkConstant.NONE);
			Util.addAdvancedControlProperties(control, stats, Util.createButton(YealinkConstant.CONTROL_MANAGEMENT + YealinkConstant.HASH + YealinkConstant.PACKET_CAPTURE, "Active", "Activating", 0), YealinkConstant.NONE);
			Util.addAdvancedControlProperties(control, stats, Util.createButton(YealinkConstant.CONTROL_MANAGEMENT + YealinkConstant.HASH + YealinkConstant.EXPORT_LOG, "Export", "Exporting", 0), YealinkConstant.NONE);
			Util.addAdvancedControlProperties(control, stats, Util.createButton(YealinkConstant.CONTROL_MANAGEMENT + YealinkConstant.HASH + YealinkConstant.SCREEN_CAPTURE, "Active", "Activating", 0), YealinkConstant.NONE);
		}

		/**
		 * Retrieves the primary network interface for a device.
		 *
		 * @param deviceId target device id
		 */
		private String getNetWorkInterface(String deviceId) {
			try{
				JsonNode response = this.doGet(String.format(YealinkCommand.GET_NETWORK_INTERFACE, deviceId), JsonNode.class);
				return response.get(0).asText();
			}catch (Exception e){
				logger.error("Can not get network interface");
			}
			return null;
		}

		/**
		 * Retrieves the total device count from Yealink and returns it as a string.
		 *
		 * @return device count as text (value of {@code total})
		 * @throws ResourceNotReachableException if the request fails or {@code total} is missing
		 */
		private String getDeviceCount(){
			try{
				JsonNode res = this.doGet(String.format(YealinkCommand.GET_DEVICE_COUNT, deviceTypeFiltering), JsonNode.class);
				if (res == null || !res.has("total")) {
					throw new ResourceNotReachableException("Missing 'total' in response");
				}
				return res.path("total").asText();
			}catch (Exception e){
				throw new ResourceNotReachableException("Error when retrieving device count", e);
			}
		}
	}
