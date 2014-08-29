package com.pinternals.mailclientadapter;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;

public class CCIConnectionMetaData implements ConnectionMetaData {
	private static final XITrace TRACE = new XITrace(CCIConnectionMetaData.class.getName());
	private SPIManagedConnection mc;
	private static final String version = new String("0.1");
	private static final String name = new String("Connection");

	public CCIConnectionMetaData(SPIManagedConnection mc) {
		this.mc = mc;
	}

	public String getEISProductName() throws ResourceException {
		return name;
	}

	public String getEISProductVersion() throws ResourceException {
		return version;
	}

	public String getUserName() throws ResourceException {
		String SIGNATURE = "getUserName()";
		TRACE.entering(SIGNATURE);

		String userName = null;
		if (this.mc.isDestroyed()) {
			throw new ResourceException("ManagedConnection is destroyed");
		}
		PasswordCredential cred = this.mc.getPasswordCredential();
		if (cred != null) {
			userName = cred.getUserName();
		}
		TRACE.entering(SIGNATURE);
		return userName;
	}
}
