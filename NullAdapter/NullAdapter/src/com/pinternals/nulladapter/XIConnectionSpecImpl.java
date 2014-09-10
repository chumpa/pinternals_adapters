package com.pinternals.nulladapter;

import com.sap.aii.af.lib.ra.cci.XIConnectionSpec;
import com.sap.aii.af.lib.trace.Trace;

public class XIConnectionSpecImpl implements XIConnectionSpec {
	private static final Trace TRACE = new Trace(XIConnectionSpecImpl.class.getName());
	private String userName;
	private String password;
	private String channelId;
	private String type;

	public XIConnectionSpecImpl(String userName, String password, String channelId, String type) {
		this.userName = userName;
		this.password = password;
		this.channelId = channelId;
		this.type = type;
	}

	public XIConnectionSpecImpl() {
	}

	public String getUserName() {
		return this.userName;
	}

	public String getPassword() {
		return this.password;
	}

	public String getChannelId() {
		return this.channelId;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
