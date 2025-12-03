/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common.constants;

import java.time.Instant;
import java.time.ZoneId;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * Utility class for the adapter. Which includes helper methods.
 *
 * @author Harry / Symphony Dev Team
 * @since 1.0.0
 */
public class Util {

	private static final ZoneId ZONE_HCM = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final DateTimeFormatter FORMATTER =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZONE_HCM);

	/**
	 * Add addAdvancedControlProperties if advancedControllableProperties different empty
	 *
	 * @param advancedControllableProperties advancedControllableProperties is the list that store all controllable properties
	 * @param stats store all statistics
	 * @param property the property is item advancedControllableProperties
	 * @throws IllegalStateException when exception occur
	 */
	public static void addAdvancedControlProperties(List<AdvancedControllableProperty> advancedControllableProperties, Map<String, String> stats, AdvancedControllableProperty property, String value) {
		if (property != null) {
			advancedControllableProperties.removeIf(controllableProperty -> controllableProperty.getName().equals(property.getName()));

			String propertyValue = StringUtils.isNotNullOrEmpty(value) && !YealinkConstant.NONE.equals(value) ? value : YealinkConstant.EMPTY;
			stats.put(property.getName(), propertyValue);
			advancedControllableProperties.add(property);
		}
	}

	/**
	 * Create a button.
	 *
	 * @param name name of the button
	 * @param label label of the button
	 * @param labelPressed label of the button after pressing it
	 * @param gracePeriod grace period of button
	 * @return This returns the instance of {@link AdvancedControllableProperty} type Button.
	 */
	public static AdvancedControllableProperty createButton(String name, String label, String labelPressed, long gracePeriod) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(gracePeriod);
		return new AdvancedControllableProperty(name, new Date(), button, "N/A");
	}

	/**
	 * capitalize the first character of the string
	 *
	 * @param input input string
	 * @return string after fix
	 */
	public static String uppercaseFirstCharacter(String input) {
		char firstChar = input.charAt(0);
		return Character.toUpperCase(firstChar) + input.substring(1);
	}

	/**
	 * check value is null or empty
	 *
	 * @param value input value
	 * @return value after checking
	 */
	public static String getDefaultValueForNullData(String value) {
		return StringUtils.isNotNullOrEmpty(value) && !"null".equalsIgnoreCase(value) ? uppercaseFirstCharacter(value) : YealinkConstant.NOT_AVAILABLE;
	}

	/**
	 * Builds a standard paged request body for Yealink APIs.
	 * @param skip          zero-based offset; negative values are coerced to 0
	 * @param limit         page size; null uses {@link YealinkConstant#DEFAULT_LIMIT}, otherwise clamped to [1, MAX_LIMIT]
	 * @param autoCount     whether the API should return total count; null is treated as {@code true}
	 * @param extra         optional additional fields (e.g., {@code "filter": {...}}) to include at the root
	 * @param objectMapper  Jackson mapper used to convert extra values to JSON
	 * @return an {@link ObjectNode} ready to send as the request payload
	 */
	public static ObjectNode buildRequestBody(int skip, Integer limit, Boolean autoCount,
			Map<String, ?> extra, ObjectMapper objectMapper){
		if (skip < 0) skip = 0;
		int l = (limit == null) ? YealinkConstant.DEFAULT_LIMIT : Math.max(1, Math.min(limit, YealinkConstant.MAX_LIMIT));
		boolean ac = autoCount == null || autoCount;
		ObjectNode root = objectMapper.createObjectNode();
		root.put("skip", skip);
		root.put("limit", l);
		root.put("autoCount", ac);

		if (extra != null && !extra.isEmpty()) {
			extra.forEach((k, v) -> {
				if (k != null && v != null) root.set(k, objectMapper.valueToTree(v));
			});
		}
		return root;
	}

	/**
	 * Formats a Unix epoch timestamp as a UTC date-time string ("yyyy/MM/dd HH:mm:ss").
	 * @param epochInput epoch timestamp in seconds or milliseconds
	 * @return formatted UTC date-time string (e.g., "2025/10/02 02:33:21")
	 */
	public static String formatEpochUtc(long epochInput) {
		long epochMillis = (epochInput < 10_000_000_000L) ? epochInput * 1000L : epochInput;
		return FORMATTER.format(Instant.ofEpochMilli(epochMillis));
	}

	/**
	 * Uptime is received in seconds, need to normalize it and make it human-readable, like
	 * 1 day(s) 5 hour(s) 12 minute(s) 55 minute(s)
	 * Incoming parameter is may have a decimal point, so in order to safely process this - it's rounded first.
	 * We don't need to add a segment of time if it's 0.
	 *
	 * @param uptimeSeconds value in seconds
	 * @return string value of format 'x day(s) x hour(s) x minute(s) x minute(s)'
	 */
	public static String normalizeUptime(long uptimeSeconds) {
		StringBuilder normalizedUptime = new StringBuilder();

		long seconds = uptimeSeconds % 60;
		long minutes = uptimeSeconds % 3600 / 60;
		long hours = uptimeSeconds % 86400 / 3600;
		long days = uptimeSeconds / 86400;

		if (days > 0) {
			normalizedUptime.append(days).append(" day(s) ");
		}
		if (hours > 0) {
			normalizedUptime.append(hours).append(" hour(s) ");
		}
		if (minutes > 0) {
			normalizedUptime.append(minutes).append(" minute(s) ");
		}
		if (seconds > 0) {
			normalizedUptime.append(seconds).append(" second(s)");
		}
		return normalizedUptime.toString().trim();
	}
}
