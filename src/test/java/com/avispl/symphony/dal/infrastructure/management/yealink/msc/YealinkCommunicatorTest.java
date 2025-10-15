/*
 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.yealink.msc;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.metric.DiagnosisRecord;

/**
 * YealinkCommunicatorTest
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 25/10/2024
 * @since 1.0.0
 */
public class YealinkCommunicatorTest {
	private ExtendedStatistics extendedStatistic;
	private YealinkCommunicator yealinkCommunicator;

	@BeforeEach
	void setUp() throws Exception {
		yealinkCommunicator = new YealinkCommunicator();
		yealinkCommunicator.setHost("---");
		yealinkCommunicator.setLogin("---");
		yealinkCommunicator.setPassword("---");
		yealinkCommunicator.setPort(443);
		yealinkCommunicator.init();
		yealinkCommunicator.connect();
	}

	@AfterEach
	void destroy() throws Exception {
		yealinkCommunicator.disconnect();
		yealinkCommunicator.destroy();
	}

	@Test
	void testLoginSuccess() throws Exception {
		yealinkCommunicator.getMultipleStatistics();
	}

	@Test
	void testGetAggregatorData() throws Exception {
		extendedStatistic = (ExtendedStatistics) yealinkCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistic.getStatistics();
		Assertions.assertEquals("2", stats.get("MonitoredDevicesTotal"));
	}

	@Test
	void testGetAggregatedData() throws Exception {
		yealinkCommunicator.getMultipleStatistics();
		yealinkCommunicator.retrieveMultipleStatistics();
		Thread.sleep(20000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) yealinkCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Thread.sleep(20000);
		List<AggregatedDevice> aggregatedDeviceList = yealinkCommunicator.retrieveMultipleStatistics();
		System.out.println("stats: " + stats);
		System.out.println("aggregatedDeviceList: " + aggregatedDeviceList);
		Assert.assertEquals(2, aggregatedDeviceList.size());
	}


	@Test
	public void testRebootControl() throws Exception {
		yealinkCommunicator.getMultipleStatistics();
		yealinkCommunicator.retrieveMultipleStatistics();
		Thread.sleep(20000);
		yealinkCommunicator.retrieveMultipleStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		controllableProperty.setProperty("Control#Reboot");
		controllableProperty.setValue("1");
		controllableProperty.setDeviceId("6fc78c673fad44c4aaa112b442147f9d");
		yealinkCommunicator.controlProperty(controllableProperty);
		Thread.sleep(20000);
		List<AggregatedDevice> aggregatedDeviceList = yealinkCommunicator.retrieveMultipleStatistics();
		System.out.println("aggregatedDeviceList " + aggregatedDeviceList);
	}


}
