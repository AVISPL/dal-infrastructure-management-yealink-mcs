/*
 *  Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.yealink.msc.common;

/**
 * LoginInfo class represents information about a login session.
 *
 * @author Harry / Symphony Dev Team<br>
 * @since 1.0.0
 */
public class LoginInfo {
	private long loginDateTime = 0;
	private String token;
	private String access_token;
	private String token_type;
	private Long expires_in;

	/**
	 * Create an instance of LoginInfo
	 */
	public LoginInfo() {
		this.loginDateTime = 0;
	}

	/**
	 * Retrieves {@code {@link #loginDateTime}}
	 *
	 * @return value of {@link #loginDateTime}
	 */
	public long getLoginDateTime() {
		return loginDateTime;
	}

	/**
	 * Sets {@code loginDateTime}
	 *
	 * @param loginDateTime the {@code long} field
	 */
	public void setLoginDateTime(long loginDateTime) {
		this.loginDateTime = loginDateTime;
	}

	/**
	 * Retrieves {@code {@link #token}}
	 *
	 * @return value of {@link #token}
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Sets {@code token}
	 *
	 * @param token the {@code java.lang.String} field
	 */
	public void setToken(String token) {
		this.token = token;
	}

	public String getAccessToken() { return access_token; }
	public void setAccess_token(String access_token) { this.access_token = access_token; }

	public String getTokenType() { return token_type; }
	public void setToken_type(String token_type) { this.token_type = token_type; }

	public Long getExpiresIn() { return expires_in; }
	public void setExpires_in(Long expires_in) { this.expires_in = expires_in; }
}
